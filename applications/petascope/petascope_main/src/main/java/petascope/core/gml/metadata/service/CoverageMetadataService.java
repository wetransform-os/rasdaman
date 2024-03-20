/*
 * This file is part of rasdaman community.
 *
 * Rasdaman community is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rasdaman community is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU  General Public License for more details.
 *
 * You should have received a copy of the GNU  General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003 - 2017 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.core.gml.metadata.service;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import petascope.core.gml.metadata.model.CoverageMetadata;
import petascope.core.gml.metadata.model.LocalMetadataChild;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.util.JSONUtil;
import petascope.util.StringUtil;
import petascope.util.TimeUtil;
import petascope.util.XMLUtil;
import petascope.wcps.encodeparameters.model.AreaOfValidity;
import petascope.wcps.encodeparameters.model.AxesMetadata;
import petascope.wcps.encodeparameters.model.AxisMetadata;
import petascope.wcps.exception.processing.InvalidCoverageMetadataToDeserializeException;
import petascope.wcps.metadata.model.Axis;
import petascope.wcps.metadata.model.IrregularAxis;
import petascope.wcps.metadata.model.WcpsCoverageMetadata;


/**
 *
 * Utility class to serialize/deserialize coverage's metadata.
 *
 * @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
public class CoverageMetadataService {

    private static final Logger log = LoggerFactory.getLogger(CoverageMetadataService.class);
    
    private static final XmlMapper xmlMapper = new XmlMapper();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public CoverageMetadataService() {
        
    }  

    
    /**
     * Get the metadata content of WCPS coverage metadata object.
     */
    public String getMetadataContent(WcpsCoverageMetadata wcpsCoverageMetadata) throws PetascopeException {
        
        CoverageMetadata coverageMetadata = wcpsCoverageMetadata.getCoverageMetadata();

        for (Axis axis : wcpsCoverageMetadata.getAxes()) {
            if (axis instanceof IrregularAxis) {
                IrregularAxis irregularAxis = (IrregularAxis) axis;
                if (irregularAxis.getDirectPositionsAreaOfValidityStarts() != null
                    && !irregularAxis.getDirectPositionsAreaOfValidityStarts().isEmpty()) {

                    if (coverageMetadata.getAxesMetadata() == null) {
                        coverageMetadata.setAxesMetadata(new AxesMetadata());
                    }

                    coverageMetadata.getAxesMetadata().getAxesAttributesMap().put(irregularAxis.getLabel(), new AxisMetadata());

                    List<AreaOfValidity> areasOfValidityList = new ArrayList<>();

                    for (int i = 0; i < irregularAxis.getDirectPositionsAreaOfValidityStarts().size(); i++) {
                        BigDecimal coefficientValidityStart = irregularAxis.getDirectPositionsAreaOfValidityStarts().get(i).add(irregularAxis.getCoefficientZeroValueAsNumber());
                        BigDecimal coefficientValidityEnd = irregularAxis.getDirectPositionsAreaOfValidityEnds().get(i).add(irregularAxis.getCoefficientZeroValueAsNumber());

                        String coefficientValidityStartStr = coefficientValidityStart.toPlainString();
                        String coefficientValidityEndStr = coefficientValidityEnd.toPlainString();

                        if (irregularAxis.isTimeAxis()) {
                            coefficientValidityStartStr = StringUtil.stripQuotes(TimeUtil.listValuesToISODateTime(BigDecimal.ZERO, BigDecimal.ZERO, Arrays.asList(coefficientValidityStart),
                                    irregularAxis.getCrsDefinition()).get(0));
                            coefficientValidityEndStr = StringUtil.stripQuotes(TimeUtil.listValuesToISODateTime(BigDecimal.ZERO, BigDecimal.ZERO, Arrays.asList(coefficientValidityEnd),
                                    irregularAxis.getCrsDefinition()).get(0));
                        }

                        AreaOfValidity areaOfValidity = new AreaOfValidity(coefficientValidityStartStr, coefficientValidityEndStr);
                        areasOfValidityList.add(areaOfValidity);
                    }

                    AxisMetadata axisMetadata = coverageMetadata.getAxesMetadata().getAxesAttributesMap().get(irregularAxis.getLabel());
                    axisMetadata.setAreaOfValidityList(areasOfValidityList);
                }
            }
        }
        
        String originalCoverageMetadataStr = wcpsCoverageMetadata.getMetadata();
        String metadataStr = "";
        
        if (coverageMetadata.isIsNotDeserializable()) {
            // In this case, coverage's metadata is not possible to deserialize to CoverageMetadata object
            // then just return what it persisted in database
            metadataStr = originalCoverageMetadataStr;
        } else {            
            if (originalCoverageMetadataStr.isEmpty() || XMLUtil.containsXMLContent(originalCoverageMetadataStr)) {
                // coverage's metadata is in XML
                metadataStr = this.serializeCoverageMetadataInXML(coverageMetadata);
            } else {
                // coverage's metadata is in JSON
                metadataStr = this.serializeCoverageMetadataInJSON(coverageMetadata);
            }
        }
        
        return XMLUtil.unescapeXML(metadataStr);
    }
    
    
    /**
     * Serialize CoverageMetadata object to JSON string to be persisted inside database.
     */
    public String serializeCoverageMetadataInJSON(CoverageMetadata coverageMetadata) throws PetascopeException {
        coverageMetadata.stripEmptyProperties();
        String output = "";
        output = JSONUtil.serializeObjectToJSONString(coverageMetadata);
        
        return output;
    }
    
    /**
     * Serialize CoverageMetadata object to XML string to be persisted inside database.
     */
    public String serializeCoverageMetadataInXML(CoverageMetadata coverageMetadata) throws PetascopeException {
        coverageMetadata.stripEmptyProperties();
        
        String output = "";
        try {
            output = XMLUtil.serializeObjectToXMLString(coverageMetadata);
            // NOTE: don't store the root tag (<CoverageMetadata> generated by Jackson)
            output = output.replace(CoverageMetadata.XML_ROOT_OPEN_TAG, "")
                            .replace(CoverageMetadata.XML_ROOT_CLOSE_TAG, "");
        } catch (JsonProcessingException ex) {
            throw new PetascopeException(ExceptionCode.RuntimeError, 
                    "Cannot serialize CoverageMetadata object to XML string. Reason: " + ex.getMessage(), ex);
        }
        
        return output;
    }
    
    /**
     * Deserialize medata (coverage's local metadata) in string from input GML coverage of UpdateCoverage request
     * to a LocalMetadata object.
     *
     */
    public LocalMetadataChild deserializeLocalMetadata(String metadata) {
        LocalMetadataChild localMetadata = new LocalMetadataChild();
        
        try {
            //find out the type
            if (XMLUtil.containsXMLContent(metadata)) {
                //xml (allow to deserialize "\n" in metadata)
                xmlMapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
                localMetadata = xmlMapper.readValue(METADATA_OPEN_TAG + metadata + METADATA_CLOSE_TAG, LocalMetadataChild.class);
            } else {
                //json (allow to deserialize "\n" in metadata)
                objectMapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
                localMetadata = objectMapper.readValue(metadata, LocalMetadataChild.class);
            }
        } catch (IOException ex) {
            throw new InvalidCoverageMetadataToDeserializeException(ex.getMessage(), ex);
        }
        
        return localMetadata;
    }
     
    /**
     * Deserialize the coverage's metadata in XML/JSON format of gmlcov:metadata element
     * to and object to manipulate.
     * 
     * @param metadata
     * @return 
     */
    public CoverageMetadata deserializeCoverageMetadata(String metadata) {
        // Don't do anything if metadata is empty as it is not valid XML/JSON
        if (metadata == null || metadata.isEmpty()) {
            return new CoverageMetadata();
        }
        
        CoverageMetadata coverageMetadata = new CoverageMetadata();
        //remove the slices and the gmlcov:metadata closing tag if it exists
        // metadata = removeMetadataSlices(metadata).replace("<gmlcov:metadata />", "");
        //convert to object
        try {           
            
            //find out the type
            if (!metadata.startsWith("{") || metadata.startsWith("<")) {            
                // NOTE: if old coverage imported with metadata not in XML or JSON, consider it is XML by adding <metadata/> wrapper element.                
                //the contents that the xmlMapper can read into a map must currently come from inside an outer tag, which is ignored
                //so we are just adding them
                coverageMetadata = xmlMapper.readValue(METADATA_OPEN_TAG + metadata + METADATA_CLOSE_TAG, CoverageMetadata.class);
            } else {
                //json
                coverageMetadata = objectMapper.readValue(metadata, CoverageMetadata.class);
            }
        } catch (IOException ex) {
            throw new InvalidCoverageMetadataToDeserializeException(ex.getMessage(), ex);
        }
        
        return coverageMetadata;
    }    

    private final static String METADATA_OPEN_TAG = "<metadata>";
    private final static String METADATA_CLOSE_TAG = "</metadata>";
}

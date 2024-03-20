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
 * Copyright 2003 - 2018 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.core.gml.cis11;

import nu.xom.Attribute;
import nu.xom.Element;
import petascope.core.gml.cis.AbstractGMLCISGetCoverage;
import petascope.core.gml.cis11.model.rangeset.RangeSetCIS11;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.SecoreException;
import petascope.util.XMLUtil;

import static petascope.core.XMLSymbols.*;

/**
 * Class to build result for WCS GetCoverage request in GML. 
 * e.g: http://schemas.opengis.net/cis/1.1/gml/examples-1.0/exampleRectifiedGridCoverage-2.xml
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
public class GMLCIS11GetCoverage extends AbstractGMLCISGetCoverage {

    private String coverageId;
    private String coverageType;
    private GMLCoreCIS11 gmlCore;
    private RangeSetCIS11 rangeSet;

    public GMLCIS11GetCoverage(String coverageId, String coverageType, GMLCoreCIS11 gmlCore, RangeSetCIS11 rangeSet) {
        this.coverageId = coverageId;
        this.coverageType = coverageType;
        this.gmlCore = gmlCore;
        this.rangeSet = rangeSet;
    }

    public String getCoverageId() {
        return coverageId;
    }

    public void setCoverageId(String coverageId) {
        this.coverageId = coverageId;
    }

    public String getCoverageType() {
        return coverageType;
    }

    public void setCoverageType(String coverageType) {
        this.coverageType = coverageType;
    }

    public GMLCoreCIS11 getGmlCore() {
        return gmlCore;
    }

    public void setGmlCore(GMLCoreCIS11 gmlCore) {
        this.gmlCore = gmlCore;
    }

    public RangeSetCIS11 getRangeSet() {
        return rangeSet;
    }

    public void setRangeSet(RangeSetCIS11 rangeSet) {
        this.rangeSet = rangeSet;
    }

    @Override
    public Element serializeToXMLElement() throws PetascopeException {
        
        // <cis11:GeneralGridCoverage>
        Element coverageTypeElement = new Element(XMLUtil.createXMLLabel(PREFIX_CIS11, this.coverageType), NAMESPACE_CIS_11);
        Attribute attributeId = XMLUtil.createXMLAttribute(NAMESPACE_GML, PREFIX_GML, ATT_ID, this.coverageId);
        coverageTypeElement.addAttribute(attributeId);

        // <ciss11:CoverageFunction>
        Element coverageFunctionElement = this.getGmlCore().getCoverageFunction().serializeToXMLElement();
        coverageFunctionElement.setNamespacePrefix(PREFIX_CIS11);
        coverageFunctionElement.setLocalName(LABEL_COVERAGE_FUNCTION_CIS_111);
        coverageFunctionElement.setNamespaceURI(NAMESPACE_CIS_11);

        coverageTypeElement.appendChild(coverageFunctionElement);
        
        // <cis11:envelope>
        Element envelopeElement = this.getGmlCore().getEnvelope().serializeToXMLElement();
        coverageTypeElement.appendChild(envelopeElement);

        // <cis11:domainSet>
        Element domainSetElement = this.getGmlCore().getDomainSet().serializeToXMLElement();
        coverageTypeElement.appendChild(domainSetElement);

        // <cis11:rangeSet>
        if (this.rangeSet != null) {
            Element rangeSetElement = this.getRangeSet().serializeToXMLElement();
            coverageTypeElement.appendChild(rangeSetElement);
        }
        
        // <cis11:rangeType>
        Element rangeTypeElement = this.getGmlCore().getRangeType().serializeToXMLElement();
        coverageTypeElement.appendChild(rangeTypeElement);


        // <cis11:metadata>
        Element metadataElement;
        try {
            metadataElement = this.getGmlCore().getMetadata().serializeToXMLElement();
        } catch (Exception ex) {
            throw new PetascopeException(ExceptionCode.XmlNotValid, "Cannot serialize coverage's metadata to XML element. Reason: " + ex.getMessage(), ex);
        }
        coverageTypeElement.appendChild(metadataElement);

        
        return coverageTypeElement;
    }
}

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
package petascope.core.gml.cis.model.rangetype;

import nu.xom.Attribute;
import nu.xom.Element;
import petascope.core.XMLSymbols;
import petascope.core.gml.ISerializeToXMElement;
import petascope.util.XMLUtil;
import petascope.util.ras.TypeResolverUtil;

import static petascope.core.XMLSymbols.*;

/**
 * Class to represent Quantity element. e.g:
<swe:Quantity>
    <swe:label>Radiation Dose</swe:label>
    <swe:description>Radiation dose measured by Gamma detector</swe:description>
    <swe:nilValues>
        <swe:NilValues>
                <swe:nilValue reason="http://www.opengis.net/def/nil/OGC/0/BelowDetectionRange">-INF</swe:nilValue>
                <swe:nilValue reason="http://www.opengis.net/def/nil/OGC/0/AboveDetectionRange">INF</swe:nilValue>
        </swe:NilValues>
    </swe:nilValues>
    <swe:uom code="uR"/>
    <swe:constraint>
        <swe:AllowedValues>
           <swe:interval>-180 0</swe:interval>
           <swe:interval>1 180</swe:interval>
        </swe:AllowedValues>
    </swe:constraint> 
<swe:Quantity/>

 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
public class Quantity implements ISerializeToXMElement {
    
    private String label;
    private String description;
    private String definition;
    private NilValues nilValues;
    // NOTE: used only for swe:Quantity
    private String uomCode;
    private Constraint constraint;
    private String dataType;
    private org.rasdaman.domain.cis.Quantity.ObservationType observationType;
    // NOTE: used only for swe:Category
    private String codeSpace;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NilValues getNilValues() {
        return nilValues;
    }

    public void setNilValues(NilValues nilValues) {
        this.nilValues = nilValues;
    }

    public String getUomCode() {
        return uomCode;
    }

    public void setUomCode(String uomCode) {
        this.uomCode = uomCode;
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }


    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public org.rasdaman.domain.cis.Quantity.ObservationType getObservationType() {
        return observationType;
    }

    public void setObservationType(org.rasdaman.domain.cis.Quantity.ObservationType observationType) {
        this.observationType = observationType;
    }

    public String getCodeSpace() {
        return codeSpace;
    }

    public void setCodeSpace(String codeSpace) {
        this.codeSpace = codeSpace;
    }

    /**
     * Create an URL which points to opengis data type based on rasdaman data type,
     * e.g: rasdaman octet -> http://www.opengis.net/def/dataType/OGC/0/signedByte
     */
    private String createOpenGisDataTypeDefinitionURL() {
        String result = this.definition;
        if (result == null || result.isEmpty()) {
            String opengisDataType = TypeResolverUtil.RAS_TYPES_TO_OPENGIS_TYPES.get(this.dataType);
            result = "http://www.opengis.net/def/dataType/OGC/0/" + opengisDataType;
        }

        return result;
    }

    @Override
    public Element serializeToXMLElement() {
        Element quantityElement = new Element(XMLUtil.createXMLLabel(PREFIX_SWE, LABEL_QUANTITY), NAMESPACE_SWE);
        if (this.observationType == org.rasdaman.domain.cis.Quantity.ObservationType.CATEGORIAL) {
            quantityElement = new Element(XMLUtil.createXMLLabel(PREFIX_SWE, LABEL_CATEGORY), NAMESPACE_SWE);
        }

        String dataTypeURL = this.createOpenGisDataTypeDefinitionURL();
        Attribute definitionAttribute = new Attribute(LABEL_DEFINITION, dataTypeURL);
        quantityElement.addAttribute(definitionAttribute);
        
        Element labelElement = new Element(XMLUtil.createXMLLabel(PREFIX_SWE, LABEL_LABEL), NAMESPACE_SWE);
        labelElement.appendChild(this.label);
        
        Element descriptionElement = new Element(XMLUtil.createXMLLabel(PREFIX_SWE, LABEL_DESCRIPTION), NAMESPACE_SWE);
        descriptionElement.appendChild(description);
        
        Element nilValuesElement = null;
        
        if (this.nilValues.getNilValues().size() > 0) {
            nilValuesElement = this.nilValues.serializeToXMLElement(); 
        }

        quantityElement.appendChild(labelElement);
        quantityElement.appendChild(descriptionElement);
        if (nilValuesElement != null) {
            quantityElement.appendChild(nilValuesElement);
        }

        if (this.observationType == org.rasdaman.domain.cis.Quantity.ObservationType.NUMERICAL) {
            if (this.uomCode != null) {
                Element uomElement = new Element(XMLUtil.createXMLLabel(PREFIX_SWE, LABEL_UOM), NAMESPACE_SWE);
                Attribute codeAttribute = new Attribute(LABEL_CODE, this.uomCode);
                uomElement.addAttribute(codeAttribute);
                quantityElement.appendChild(uomElement);
            }
        } else if (this.observationType == org.rasdaman.domain.cis.Quantity.ObservationType.CATEGORIAL) {
            if (this.codeSpace != null) {
                Element codeSpaceElement = new Element(XMLUtil.createXMLLabel(PREFIX_SWE, LABEL_CODE_SPACE), NAMESPACE_SWE);
                Attribute codeAttribute =  new Attribute(PREFIX_XLINK_HREF,
                                                        XMLSymbols.NAMESPACE_XLINK,
                                                        this.codeSpace);
                codeAttribute.setValue(this.codeSpace);
                codeSpaceElement.addAttribute(codeAttribute);
                quantityElement.appendChild(codeSpaceElement);
            }
        }

        return quantityElement;
    }
    
}

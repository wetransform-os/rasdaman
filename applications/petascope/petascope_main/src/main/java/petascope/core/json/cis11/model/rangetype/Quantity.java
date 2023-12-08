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
package petascope.core.json.cis11.model.rangetype;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import static petascope.core.json.cis11.JSONCIS11GetCoverage.TYPE_NAME;

/**
 * Class to represent Quantity element in CIS 1.1. e.g:
 
{
    "type": "QuantityType",
    "name": "band1",
    "description": "Radiation dose measured by Gamma detector",
    "definition": "ogcType:unsignedInt",
    "uom": {
        "type": "UnitReference",
        "code": "10^0"
    }
}

* @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@JsonPropertyOrder({TYPE_NAME})
public class Quantity {

    // swe:Quantity
    public static final String QUANTITY_TYPE = "QuantityType";
    // swe:Category
    public static final String CATEGORY_TYPE = "CategoryType";

    private String name;
    private String description;
    // Data type (e.g: unsignedInt)
    private String definition;
    private NilValues nilValues;
    private UoM uom;
    private org.rasdaman.domain.cis.Quantity.ObservationType observationType;
    private String codeSpace;

    public Quantity() {
        
    }

    public Quantity(String name, String description, String definition, NilValues nilValues, UoM uom,
                    org.rasdaman.domain.cis.Quantity.ObservationType observationType,
                    String codeSpace) {
        this.name = name;
        this.description = description;
        this.definition = definition;
        this.nilValues = nilValues;
        this.observationType = observationType;

        if (observationType == org.rasdaman.domain.cis.Quantity.ObservationType.CATEGORIAL) {
            this.uom = null;
            this.codeSpace = codeSpace;
        } if (observationType == org.rasdaman.domain.cis.Quantity.ObservationType.NUMERICAL) {
            this.codeSpace = null;
            this.uom = uom;
        }

    }
    
    public String getType() {
        String result = QUANTITY_TYPE;
        if (this.observationType == org.rasdaman.domain.cis.Quantity.ObservationType.CATEGORIAL) {
            result = CATEGORY_TYPE;
        }

        return result;
    }    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
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

    public UoM getUom() {
        return uom;
    }

    public void setUom(UoM uom) {
        this.uom = uom;
    }

    public String getCodeSpace() {
        return codeSpace;
    }

    @JsonIgnore
    public org.rasdaman.domain.cis.Quantity.ObservationType getObservationType() {
        return this.observationType;
    }
}

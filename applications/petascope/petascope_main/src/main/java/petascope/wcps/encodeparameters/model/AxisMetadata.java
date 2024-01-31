/*
  *  This file is part of rasdaman community.
  * 
  *  Rasdaman community is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 3 of the License, or
  *  (at your option) any later version.
  * 
  *  Rasdaman community is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *  See the GNU  General Public License for more details.
  * 
  *  You should have received a copy of the GNU  General Public License
  *  along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
  * 
  *  Copyright 2003 - 2024 Peter Baumann / rasdaman GmbH.
  * 
  *  For more information please see <http://www.rasdaman.org>
  *  or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcps.encodeparameters.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A class to represent the axis' metadata inside gmlcov:metadata element
 * @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
public class AxisMetadata {

    private Map<String, String> axesAttributesMap;

    @JsonProperty(AreaOfValidity.AXES_AREAS_OF_VALIDITY)

    // Only for XML (wrapper is the parent element, xmlProperty is for the list of child element names)
    @JacksonXmlElementWrapper(localName = AreaOfValidity.AXES_AREAS_OF_VALIDITY)
    @JacksonXmlProperty(localName = AreaOfValidity.AXES_AREAS_OF_VALIDITY_XML_ONLY_AREA)
    private List<AreaOfValidity> areaOfValidityList;

    public AxisMetadata() {
        this.axesAttributesMap = new LinkedHashMap<>();
    }

    @JsonAnyGetter
    // Unwrap this map
    public Map<String, String> getAxesAttributesMap() {
        return axesAttributesMap;
    }

    @JsonAnySetter
    public void addKeyValue(String key, String values) {
        this.axesAttributesMap.put(key, values);
    }

    public List<AreaOfValidity> getAreaOfValidityList() {
        return areaOfValidityList;
    }

    public void setAreaOfValidityList(List<AreaOfValidity> areaOfValidityList) {
        this.areaOfValidityList = areaOfValidityList;
    }

    @JsonIgnore
    public boolean isEmptyObject() {
        boolean result = this.getAxesAttributesMap().isEmpty() && this.getAreaOfValidityList().isEmpty();
        return result;
    }
}

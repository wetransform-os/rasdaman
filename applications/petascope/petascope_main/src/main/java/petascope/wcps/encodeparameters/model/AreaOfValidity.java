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
 *  Copyright 2003 - 2023 Peter Baumann / rasdaman GmbH.
 *
 *  For more information please see <http://www.rasdaman.org>
 *  or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcps.encodeparameters.model;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class AreaOfValidity {

    public static final String AXES_AREAS_OF_VALIDITY = "areasOfValidity";
    // Used only in XML format
    public static final String AXES_AREAS_OF_VALIDITY_XML_ONLY_AREA = "area";
    public static final String AXES_AREAS_OF_VALIDITY_START = "start";
    public static final String AXES_AREAS_OF_VALIDITY_END = "end";

    private String start;
    private String end;

    public AreaOfValidity(String start, String end) {
        this.start = start;
        this.end = end;
    }

    @JacksonXmlProperty(isAttribute=true)
    // NOTE: with this setting then it serializes as start="..." as attribute of <area> element in XML
    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }
}

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
 * Copyright 2003 - 2023 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

package petascope.openeo.handlers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import petascope.oapi.handlers.model.Link;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileFormat {

    private String title;
    private List<String> gisDataTypes = Arrays.asList("raster");
    private List<FileFormatParameter> parameters;
    private List<Link> links = null;

    public FileFormat(String title, List<FileFormatParameter> parameters, List<Link> links) {
        this.title = title;
        this.parameters = parameters;
        this.links = links;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("gis_data_types")
    public List<String> getGisDataTypes() {
        return gisDataTypes;
    }

    @JsonSerialize(using = FileFormatParametersSerializer.class)
    public List<FileFormatParameter> getParameters() {
        return parameters;
    }

    @JsonProperty("links")
    public List<Link> getLinks() {
        return links;
    }
}



class FileFormatParametersSerializer extends JsonSerializer<List<FileFormatParameter>> {

    @Override
    public void serialize(List<FileFormatParameter> parameters, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        for (FileFormatParameter fileFormatParameter : parameters) {
            jgen.writeObjectField(fileFormatParameter.getParameterName(), fileFormatParameter);
        }
        jgen.writeEndObject();
    }
}
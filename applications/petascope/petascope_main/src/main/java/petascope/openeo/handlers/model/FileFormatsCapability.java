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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import petascope.oapi.handlers.model.Link;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Object represents the response of this endpoint https://openeo.eodc.eu/openeo/1.1.0/file_formats
 */

public class FileFormatsCapability {

    private static List<FileFormat> input = new ArrayList<>();
    private static List<FileFormat> output = new ArrayList<>();

    private static List<Link> links = Arrays.asList(new Link("about", "https://doc.rasdaman.org/04_ql-guide.html#rasql-encode-format-parameters"));

    private static FileFormatParameter fileFormatParameter = new FileFormatParameter("colormap", null,
            "Allows specifying a colormap to colorize single band PNGs.", Arrays.asList("object", "null")
    );

    static {
        input.add(new FileFormat("GTiff", null, null));
        input.add(new FileFormat("netCDF", null, null));
        input.add(new FileFormat("GRIB", null, null));
        input.add(new FileFormat("PNG", null, null));
        input.add(new FileFormat("JPEG", null, null));

        output.add(new FileFormat("GTiff",
                Arrays.asList(fileFormatParameter),
                links));
        output.add(new FileFormat("PNG",
                Arrays.asList(fileFormatParameter),
                links));
        output.add(new FileFormat("JPEG",
                Arrays.asList(fileFormatParameter),
                links));

        output.add(new FileFormat("netCDF", null, null));
        output.add(new FileFormat("CSV", null, null));
        output.add(new FileFormat("JSON", null, null));
    }

    public FileFormatsCapability() {

    }

    @JsonSerialize(using = FileFormatsSerializer.class)
    public List<FileFormat> getInput() {
        return input;
    }

    @JsonSerialize(using = FileFormatsSerializer.class)
    public List<FileFormat> getOutput() {
        return output;
    }
}


class FileFormatsSerializer extends JsonSerializer<List<FileFormat>> {

    @Override
    public void serialize(List<FileFormat> fileFormats, JsonGenerator jgen,
                          SerializerProvider provider) throws IOException {

        jgen.writeStartObject();
        for (FileFormat fileFormat : fileFormats) {
            jgen.writeObjectField(fileFormat.getTitle(), fileFormat);
        }
        jgen.writeEndObject();
    }

}

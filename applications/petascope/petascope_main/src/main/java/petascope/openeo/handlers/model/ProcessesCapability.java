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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import petascope.exceptions.PetascopeException;
import petascope.util.IOUtil;
import petascope.util.JSONUtil;
import petascope.util.ListUtil;
import petascope.wcps.handler.EncodeCoverageHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Object represents the response of this endpoint https://openeo.eodc.eu/openeo/1.1.0/processes
 */

public class ProcessesCapability {

    private static String result;
    private static Map<String, String> processesMap = new LinkedHashMap<>();

    private static org.slf4j.Logger log = LoggerFactory.getLogger(ProcessesCapability.class);

    public ProcessesCapability() throws IOException, PetascopeException {
        getJSONResult();
    }

    public static String getJSONResult() throws IOException, PetascopeException {
        if (result == null) {
            setJSONResult();
        }

        return result;
    }

    /**
     * Get all WCPS operations (processes) defined in separated JSON files and concatenate them
     * as a list of processes
     */
    private static void setJSONResult() throws IOException, PetascopeException {

        String resourceFolder = "openeo/processes";

        List<String> tmps = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(resourceFolder + "/index.txt");
        try (InputStream inputStream = resource.getInputStream()) {
            // Use the inputStream to read the resource content
            String text = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
            String[] lines = text.split("\n");

            for (String line : lines) {
                ClassPathResource resourceTmp = new ClassPathResource(resourceFolder + "/" + line);
                try (InputStream inputStreamTmp = resourceTmp.getInputStream()) {
                    String fileContent = IOUtils.toString(inputStreamTmp, StandardCharsets.UTF_8.name());
                    tmps.add(fileContent);
                    String filename = line.replace(".json", "");

                    processesMap.put(filename, fileContent);
                }

            }
        }

        String tmp = "{ \"processes\": [" + ListUtil.join(tmps, ", ") + "], \"links\": [{\n" +
                "\t\t\"rel\": \"about\",\n" +
                "\t\t\"href\": \"https://doc.rasdaman.org\",\n" +
                "\t\t\"type\": \"text/html\",\n" +
                "\t\t\"title\": \"rasdaman documentation\"\n" +
                "\t}] "
                + "} ";

        result = JSONUtil.prettyPrint(tmp);
    }

    /**
     * Return a pre-defined process by a process id
     */
    public static String getJsonContent(String processId) throws IOException, PetascopeException {
        if (processesMap.isEmpty()) {
            setJSONResult();
        }

        String json = processesMap.get(processId);
        return json;
    }

}

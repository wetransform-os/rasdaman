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
import org.rasdaman.domain.openeo.ProcessGraph;
import org.springframework.core.io.Resource;
import petascope.exceptions.PetascopeException;
import petascope.util.IOUtil;
import petascope.util.JSONUtil;
import petascope.util.ListUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Object represents the response of this endpoint https://openeo.org/documentation/1.0/developers/api/reference.html#tag/User-Defined-Processes/operation/list-custom-processes
 */

public class ProcessGraphsCapability {

    public String getJSONResult(Set<ProcessGraph> processGraphs) throws IOException, PetascopeException {

        List<String> tmps = new ArrayList<>();

        for (ProcessGraph processGraph : processGraphs) {
            tmps.add(processGraph.getContent());
        }

        String tmp = "{ \"processes\": [" + ListUtil.join(tmps, ", ") + "], \"links\": [] } ";

        String result = JSONUtil.prettyPrint(tmp);
        return result;
    }


}

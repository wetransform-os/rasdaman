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
package petascope.wcps.subset_axis.model;

import petascope.util.ListUtil;
import petascope.util.PetascopeDateTime;
import petascope.wcps.result.ParameterResult;

import java.util.ArrayList;
import java.util.List;

/**
e.g. ["2015", "2016", "2017"]
 */
public class ListStringResult extends ParameterResult {

    public ListStringResult(List<String> list) {
        this.list = list;
    }


    public List<String> getList() {
        if (list == null) {
            list = new ArrayList<>();
        }

        return list;
    }

    public String toString() {
        return "<" + ListUtil.join(this.list, ",") + ">";
    }

    private List<String> list;
}

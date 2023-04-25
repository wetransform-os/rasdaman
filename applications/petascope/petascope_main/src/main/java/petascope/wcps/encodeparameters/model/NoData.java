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
package petascope.wcps.encodeparameters.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRawValue;
import org.rasdaman.domain.cis.NilValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petascope.wms.handlers.service.WMSGetMapService;

/**
 *
 * @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
public class NoData {
    private static final String NO_DATA_NAN = "NaN";

    private static Logger log = LoggerFactory.getLogger(NoData.class);
    
    public NoData() {

    }
    
    public NoData(List<NilValue> nilValues) {
        // store the nil values of metadata
        this.nilValues = nilValues;
    }

    @JsonProperty("nodata")
    @JsonRawValue // This is used to serialize NaN as it-is instead of "NaN" which is invalid in rasql
    public List<Double> getNoDataValues() {
        List<Double> results = new ArrayList<>();
        for (NilValue nilValue : this.nilValues) {
            String valueTmp = nilValue.getValue();
            Double value = null;
            if (valueTmp.equalsIgnoreCase(NO_DATA_NAN)) {
                value = Double.NaN;
            } else {
                try {
                    value = Double.valueOf(valueTmp);
                } catch (Exception ex) {
                    log.warn("Null value is not a numeric number. Given: " + value);
                }
            }

            if (value != null) {
                results.add(value);
            }
        }

        return results;
    }

    @JsonIgnore
    public List<NilValue> getNilValues() {
        return this.nilValues;
    }

    @JsonProperty("nodata")
    // NOTE: this is used only when deserializing nodata in extra params from WCPS request
    // e.g. return encode(c, "netcdf", "{ \"nodata\": [0] }")
    public void setNoDataValues(List<Double> nodataValues) {
        for (Double value : nodataValues) {
            this.nilValues.add(new NilValue(value.toString()));
        }
    }

    // this list is used to store the nilValues of metadata
    private List<NilValue> nilValues = new ArrayList<>();
}

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
package petascope.wcps.metadata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import liquibase.pro.packaged.P;
import petascope.core.Pair;
import petascope.util.BigDecimalUtil;

import java.math.BigDecimal;

/**
 * Class for storing numerical slicing
 * over a temporal axis
 * NOTE: A temporal slicing has time granularity
 * e.g. "2023-01" has granularity is month, then min is "2023-01-01T00:00:00:000" and max is "2023-01-31T23:59:59.999"
 *
 */
public class NumericTemporalSlicing extends NumericSubset {

    private Pair<BigDecimal, BigDecimal> timeGranularityBoundsPair;

    public NumericTemporalSlicing() {

    }

    public NumericTemporalSlicing(BigDecimal timeGranularityLowerBound, BigDecimal timeGranularityUpperBound) {
        this.timeGranularityBoundsPair = new Pair<>(timeGranularityLowerBound, timeGranularityUpperBound);
    }

    public Pair<BigDecimal, BigDecimal> getTimeGranularityBoundsPair() {
        return this.timeGranularityBoundsPair;
    }

    public void setTimeGranularityBoundsPair(Pair<BigDecimal, BigDecimal> timeGranularityBoundsPair) {
        this.timeGranularityBoundsPair = timeGranularityBoundsPair;
    }

    @Override
    @JsonIgnore
    public String getStringRepresentation() {
        return timeGranularityBoundsPair.fst.toPlainString() + ":" + getTimeGranularityBoundsPair().snd.toPlainString();
    }

    @Override
    @JsonIgnore
    public String getStringRepresentationInInteger() {
        return timeGranularityBoundsPair.fst.toBigInteger().toString() + ":" + timeGranularityBoundsPair.snd.toBigInteger().toString();
    }

    @Override
    public BigDecimal getLowerLimit() {
        return timeGranularityBoundsPair.fst;
    }

    @Override
    public BigDecimal getUpperLimit() {
        return timeGranularityBoundsPair.snd;
    }

    @Override
    public void setLowerLimit(BigDecimal value) {
        this.timeGranularityBoundsPair = new Pair<>(value, this.timeGranularityBoundsPair.snd);
    }

    @Override
    public void setUpperLimit(BigDecimal value) {
        this.timeGranularityBoundsPair = new Pair<>(this.timeGranularityBoundsPair.fst, value);
    }
}

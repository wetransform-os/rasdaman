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
 * Copyright 2003 - 2020 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import petascope.exceptions.PetascopeException;

import java.time.OffsetDateTime;

import static petascope.util.TimeUtil.*;

/**
 * Class to represent a special wrapper object in petascope
 * to represent datetime with timezone and its granularity
 */
public class PetascopeDateTime {

    private OffsetDateTime dateTimeMin;
    private OffsetDateTime dateTimeMax;

    private Granularity granularity;

    public enum Granularity {
        YEAR,
        MONTH,
        DAY,
        HOUR,
        MINUTE,
        SECOND,
        MILLISECOND
    }

    public PetascopeDateTime() {

    }

    public PetascopeDateTime(OffsetDateTime dateTimeMin, Granularity granularity) {
        this.dateTimeMin = dateTimeMin;
        this.granularity = granularity;
    }

    public PetascopeDateTime(OffsetDateTime dateTimeMin, OffsetDateTime datetimeMax, Granularity granularity) {
        this.dateTimeMin = dateTimeMin;
        this.dateTimeMax = datetimeMax;
        this.granularity = granularity;
    }

    public Granularity getGranularity() {
        return granularity;
    }

    public void setGranularity(Granularity granularity) {
        this.granularity = granularity;
    }

    public OffsetDateTime getDateTimeMin() {
        // with the granularity -> return the min value of this current datetime
        // e.g. 2015-01-01, then min value is 2015-01-01T00:00:00.000
        OffsetDateTime dateTimeMin = this.dateTimeMin;
        return dateTimeMin;
    }

    @JsonIgnore
    public String getDateTimeMinISOFormatStr() {
        String result = TimeUtil.toISOFormat(getDateTimeMin());
        return result;
    }

    public void setDateTimeMin(OffsetDateTime dateTimeMin) {
        this.dateTimeMin = dateTimeMin;
    }

    /**
     * In case the dateTime Max value of a timeInterval is defined by the user -> set it to this value.
     * e.g. OVER $pt ansi("2015-01": "2015-03":"P3M")
     * hence, dateTimeMin = "2015-01-01T00:00:00.000" up to "2015-03-31
     */
    public void setDateTimeMax(OffsetDateTime dateTimeMax) {
        this.dateTimeMax = dateTimeMax;
    }

    public OffsetDateTime getDateTimeMax() {
        if (dateTimeMax != null) {
            return this.dateTimeMax;
        }

        if (dateTimeMin == null) {
            return null;
        }

        // with the granularity -> return the min value of this current datetime
        // e.g. 2015-01-01, then max value is 2015-01-01T23:59:59.999
        OffsetDateTime offsetDateTimeTmp = dateTimeMin;


        if (this.granularity == Granularity.YEAR) {
            offsetDateTimeTmp = dateTimeMin.plusYears(1).plusNanos(MINUS_ONE_MILLISECOND);
        } else if (this.granularity == Granularity.MONTH) {
            offsetDateTimeTmp = dateTimeMin.plusMonths(1).plusNanos(MINUS_ONE_MILLISECOND);
        } else if (this.granularity == Granularity.DAY) {
            offsetDateTimeTmp = dateTimeMin.plusDays(1).plusNanos(MINUS_ONE_MILLISECOND);
        } else if (this.granularity == Granularity.HOUR) {
            offsetDateTimeTmp = dateTimeMin.plusHours(1).plusNanos(MINUS_ONE_MILLISECOND);
        } else if (this.granularity == Granularity.MINUTE) {
            offsetDateTimeTmp = dateTimeMin.plusMinutes(1).plusNanos(MINUS_ONE_MILLISECOND);
        } else if (this.granularity == Granularity.SECOND) {
            offsetDateTimeTmp = dateTimeMin.plusSeconds(1).plusNanos(MINUS_ONE_MILLISECOND);
        }


        OffsetDateTime result = offsetDateTimeTmp;
        if (result.compareTo(this.dateTimeMin) < 0) {
            // in case the datetime is to ms, e.g. 2015-01-01T23:30:30.123, then it cannot be set to 2015-01-01T23:30:30.122
            result = this.dateTimeMin;
        }
        return result;
    }

    @JsonIgnore
    public String getDateTimeMaxISOFormatStr() {
        String result = TimeUtil.toISOFormat(getDateTimeMax());
        return result;
    }

    /**
     * e.g. "2015-01-01T00:30:00.333" is ISO format, but granularity is Day
     * Hence, it prints only "2015-01-01/P1D"
     */
    @JsonIgnore
    public String getDateTimeMinInItsGranularity() throws PetascopeException {
        PetascopeDateTime tempDateTime = calculateDateTimeStringSubset(this.getDateTimeMinISOFormatStr());
        String result = TimeUtil.getDateTimeRepresentationInTargetGranularity(tempDateTime, this.granularity);
        return result;
    }

    @JsonIgnore
    public String getDateTimeMaxInItsGranularity() throws PetascopeException {
        PetascopeDateTime tempDateTime = calculateDateTimeStringSubset(this.getDateTimeMaxISOFormatStr());
        String result = TimeUtil.getDateTimeRepresentationInTargetGranularity(tempDateTime, this.granularity);
        return result;
    }

    @Override
    public String toString() {
        // min:max of this time interval (e.g. "2015-01-01T00:00:00.000" up to "2015-03-31T23:59:59.999"
        // - depends on if the time upper bound is defined by input query or not - used in axis iterator)
        return this.getDateTimeMinISOFormatStr()
                + " TO "
                + this.getDateTimeMaxISOFormatStr();
    }

}

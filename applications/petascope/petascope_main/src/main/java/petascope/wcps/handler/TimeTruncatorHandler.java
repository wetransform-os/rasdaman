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
package petascope.wcps.handler;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.exceptions.PetascopeException;
import petascope.util.PetascopeDateTime;
import petascope.util.StringUtil;
import petascope.util.TimeUtil;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.subset_axis.model.IntervalExpression;
import petascope.wcps.subset_axis.model.ListStringResult;

import java.time.OffsetDateTime;
import java.util.*;

import static petascope.util.TimeUtil.*;

/**
 * Handler for time extractor expression
 * e.g. allyears(domain($c, ansi)) or allyears("2015-01-01":"2018-01-20") -> [("2015", year), ("2016", year), ("2017", year), ("2018", year)]
 * Each element has the granularity value to determine the fine-grained datetime component it should contain.
 *
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TimeTruncatorHandler extends Handler {

    // e.g. months(...) -> month
    private PetascopeDateTime.Granularity granularity;

    public TimeTruncatorHandler() {

    }

    public TimeTruncatorHandler create(Handler timeDimensionIntervalHandler, PetascopeDateTime.Granularity granularity) {
        TimeTruncatorHandler result = new TimeTruncatorHandler();
        result.setChildren(Arrays.asList(timeDimensionIntervalHandler));
        result.granularity = granularity;

        return result;
    }

    @Override
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {
        // e.g. { "2015", "2016", "2017" } from allyears(domain($c, ansi))
        Set<String> truncatedValues = new LinkedHashSet<>();

        // e.g. return "2015-01-01" and "2015-02-03"
        IntervalExpression intervalExpression = (IntervalExpression) this.getFirstChild().handle(serviceRegistries);
        String lowerBound = intervalExpression.getLowerBound();
        String upperBound = intervalExpression.getUpperBound();

        if (intervalExpression.getDirectPositions() == null) {
            // Regular axis or get the number of datetime component from a time interval
            PetascopeDateTime dateTimeLowerBound = TimeUtil.calculateDateTimeStringSubset(lowerBound);
            PetascopeDateTime dateTimeUpperBound = TimeUtil.calculateDateTimeStringSubset(upperBound);

            if (this.granularity == PetascopeDateTime.Granularity.YEAR) {
                truncatedValues = this.getRegularYears(dateTimeLowerBound, dateTimeUpperBound);
            } else if (this.granularity == PetascopeDateTime.Granularity.MONTH) {
                truncatedValues = this.getRegularMonths(dateTimeLowerBound, dateTimeUpperBound);
            } else if (this.granularity == PetascopeDateTime.Granularity.DAY) {
                truncatedValues = this.getRegularDays(dateTimeLowerBound, dateTimeUpperBound);
            } else if (this.granularity == PetascopeDateTime.Granularity.HOUR) {
                truncatedValues = this.getRegularHours(dateTimeLowerBound, dateTimeUpperBound);
            } else if (this.granularity == PetascopeDateTime.Granularity.MINUTE) {
                truncatedValues = this.getRegularMinutes(dateTimeLowerBound, dateTimeUpperBound);
            } else if (this.granularity == PetascopeDateTime.Granularity.SECOND) {
                truncatedValues = this.getRegularSeconds(dateTimeLowerBound, dateTimeUpperBound);
            }
        } else {
            // Irregular axis
            List<PetascopeDateTime> petascopeDateTimes = new ArrayList<>();
            for (String dateTimeValue : intervalExpression.getDirectPositions()) {
                PetascopeDateTime petascopeDateTime = TimeUtil.calculateDateTimeStringSubset(dateTimeValue);
                petascopeDateTimes.add(petascopeDateTime);
            }

            if (this.granularity == PetascopeDateTime.Granularity.YEAR) {
                truncatedValues = this.getIrregularYears(petascopeDateTimes);
            } else if (this.granularity == PetascopeDateTime.Granularity.MONTH) {
                truncatedValues = this.getIrregularMonths(petascopeDateTimes);
            } else if (this.granularity == PetascopeDateTime.Granularity.DAY) {
                truncatedValues = this.getIrregularDays(petascopeDateTimes);
            } else if (this.granularity == PetascopeDateTime.Granularity.HOUR) {
                truncatedValues = this.getIrregularHours(petascopeDateTimes);
            } else if (this.granularity == PetascopeDateTime.Granularity.MINUTE) {
                truncatedValues = this.getIrregularMinutes(petascopeDateTimes);
            } else if (this.granularity == PetascopeDateTime.Granularity.SECOND) {
                truncatedValues = this.getIrregularSeconds(petascopeDateTimes);
            }

        }

        ListStringResult listStringResult = new ListStringResult(new ArrayList<>(truncatedValues));
        return listStringResult;
    }

    // ----- 1. get list of years

    /**
     * e.g. granularity = year, and datetime = "2015-01-01"
     * then return "2015"
     *
     */
    private Set<String> getRegularYears(PetascopeDateTime startDateTime, PetascopeDateTime endDateTime) {
        Set<String> results = new LinkedHashSet<>();
        OffsetDateTime currentDateTime = startDateTime.getDateTimeMin();

        while (currentDateTime.compareTo(endDateTime.getDateTimeMax()) <= 0) {
            results.add(StringUtil.quote(currentDateTime.format(yearDateTimeFormatter)));
            currentDateTime = currentDateTime.plusYears(1);
        }

        return results;
    }

    /**
     * e.g. granularity = year, and datetime = "2015-01-01"
     * then return "2015"
     *
     */
    private Set<String> getIrregularYears(List<PetascopeDateTime> petascopeDateTimes) {
        Set<String> results = new LinkedHashSet<>();
        for (PetascopeDateTime petascopeDateTime : petascopeDateTimes) {
            results.add(StringUtil.quote(petascopeDateTime.getDateTimeMin().format(yearDateTimeFormatter)));
        }

        return results;
    }


    // ----- 2. get list of months

    /**
     * e.g. granularity = month, and datetime = "2015-01-01"
     * then return "01"
     *
     */
    private Set<String> getRegularMonths(PetascopeDateTime startDateTime, PetascopeDateTime endDateTime) {
        Set<String> results = new LinkedHashSet<>();
        OffsetDateTime currentDateTime = startDateTime.getDateTimeMin();

        while (currentDateTime.compareTo(endDateTime.getDateTimeMax()) <= 0) {
            results.add(StringUtil.quote(currentDateTime.format(yearMonthDateTimeFormatter)));

            currentDateTime = currentDateTime.plusMonths(1);
        }

        return results;
    }

    /**
     * e.g. granularity = month, and datetime = "2015-01-01"
     * then return "01"
     *
     */
    private Set<String> getIrregularMonths(List<PetascopeDateTime> petascopeDateTimes) {
        Set<String> results = new LinkedHashSet<>();
        for (PetascopeDateTime petascopeDateTime : petascopeDateTimes) {
            results.add(StringUtil.quote(petascopeDateTime.getDateTimeMin().format(yearMonthDateTimeFormatter)));
        }

        return results;
    }

    // ----- 3. get list of days

    /**
     * e.g. granularity = day, and datetime = "2015-01-01"
     * then return "01"
     *
     */
    private Set<String> getRegularDays(PetascopeDateTime startDateTime, PetascopeDateTime endDateTime) {
        Set<String> results = new LinkedHashSet<>();
        OffsetDateTime currentDateTime = startDateTime.getDateTimeMin();

        while (currentDateTime.compareTo(endDateTime.getDateTimeMax()) <= 0) {
            results.add(StringUtil.quote(currentDateTime.format(yearMonthDayDateTimeFormatter)));

            currentDateTime = currentDateTime.plusDays(1);
        }

        return results;
    }

    /**
     * e.g. granularity = day, and datetime = "2015-01-01"
     * then return "01"
     *
     */
    private Set<String> getIrregularDays(List<PetascopeDateTime> petascopeDateTimes) {
        Set<String> results = new LinkedHashSet<>();
        for (PetascopeDateTime petascopeDateTime : petascopeDateTimes) {
            results.add(StringUtil.quote(petascopeDateTime.getDateTimeMin().format(yearMonthDayDateTimeFormatter)));
        }

        return results;
    }

    // ----- 4. get list of hours

    /**
     * e.g. granularity = hour, and datetime = "2015-01-01T03:00:05"
     * then return "03"
     *
     */
    private Set<String> getRegularHours(PetascopeDateTime startDateTime, PetascopeDateTime endDateTime) {
        Set<String> results = new LinkedHashSet<>();
        OffsetDateTime currentDateTime = startDateTime.getDateTimeMin();

        while (currentDateTime.compareTo(endDateTime.getDateTimeMax()) <= 0) {
            results.add(StringUtil.quote(currentDateTime.format(yearMonthDayHourDateTimeFormatter)));

            currentDateTime = currentDateTime.plusHours(1);
        }

        return results;
    }

    /**
     * e.g. granularity = hour, and datetime = "2015-01-01T03:00:05"
     * then return "03"
     *
     */
    private Set<String> getIrregularHours(List<PetascopeDateTime> petascopeDateTimes) {
        Set<String> results = new LinkedHashSet<>();
        for (PetascopeDateTime petascopeDateTime : petascopeDateTimes) {
            results.add(StringUtil.quote(petascopeDateTime.getDateTimeMin().format(yearMonthDayHourDateTimeFormatter)));
        }

        return results;
    }


    // ----- 5. get list of minutes

    /**
     * e.g. granularity = minute, and datetime = "2015-01-01T03:00:05"
     * then return "00"
     *
     */
    private Set<String> getRegularMinutes(PetascopeDateTime startDateTime, PetascopeDateTime endDateTime) {
        Set<String> results = new LinkedHashSet<>();
        OffsetDateTime currentDateTime = startDateTime.getDateTimeMin();

        while (currentDateTime.compareTo(endDateTime.getDateTimeMax()) <= 0) {
            results.add(StringUtil.quote(currentDateTime.format(yearMonthDayHourMinuteDateTimeFormatter)));

            currentDateTime = currentDateTime.plusMinutes(1);
        }

        return results;
    }

    /**
     * e.g. granularity = minute, and datetime = "2015-01-01T03:00:05"
     * then return "00"
     *
     */
    private Set<String> getIrregularMinutes(List<PetascopeDateTime> petascopeDateTimes) {
        Set<String> results = new LinkedHashSet<>();
        for (PetascopeDateTime petascopeDateTime : petascopeDateTimes) {
            results.add(StringUtil.quote(petascopeDateTime.getDateTimeMin().format(yearMonthDayHourMinuteDateTimeFormatter)));
        }

        return results;
    }

    // ----- 6. get list of seconds

    /**
     * e.g. granularity = second, and datetime = "2015-01-01T03:00:05.333"
     * then return "05.333".
     * NOTE: it needs to contains all the milliseconds as well
     *
     */
    private Set<String> getRegularSeconds(PetascopeDateTime startDateTime, PetascopeDateTime endDateTime) {
        Set<String> results = new LinkedHashSet<>();
        OffsetDateTime currentDateTime = startDateTime.getDateTimeMin();

        while (currentDateTime.compareTo(endDateTime.getDateTimeMax()) <= 0) {
            // e.g. "00.111" (1 ms = 1_000_000 nano seconds)
            results.add(StringUtil.quote(currentDateTime.format(yearMonthDayHourMinuteSecondDateTimeFormatter)));

            currentDateTime = currentDateTime.plusNanos(1_000_000);
        }

        return results;
    }

    /**
     * e.g. granularity = second, and datetime = "2015-01-01T03:00:05"
     * then return "05.333"
     *
     */
    private Set<String> getIrregularSeconds(List<PetascopeDateTime> petascopeDateTimes) {
        Set<String> results = new LinkedHashSet<>();
        for (PetascopeDateTime petascopeDateTime : petascopeDateTimes) {
            // e.g. "00.111" (1 ms = 1_000_000 nano seconds)
            results.add(StringUtil.quote(petascopeDateTime.getDateTimeMin().format(yearMonthDayHourMinuteSecondDateTimeFormatter)));
        }

        return results;
    }


}

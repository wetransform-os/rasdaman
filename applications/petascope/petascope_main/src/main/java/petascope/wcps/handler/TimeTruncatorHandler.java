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
import static petascope.wcps.handler.TimeExtractorHandler.stripTimeComponentUpToGranularity;

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
     *  $c extend from 2022-11-01 through 2023-03-31
     *   allyears($c.domain.date) = < "2022", "2023" >
     *
     */
    private Set<String> getRegularYears(PetascopeDateTime startDateTime, PetascopeDateTime endDateTime) {
        Set<String> results = new LinkedHashSet<>();
        // e.g. "2015-03-01" -> "2015"
        OffsetDateTime currentDateTimeTmp = stripTimeComponentUpToGranularity(startDateTime.getDateTimeMin(), PetascopeDateTime.Granularity.YEAR);
        OffsetDateTime endDateTimeTmp = stripTimeComponentUpToGranularity(endDateTime.getDateTimeMax(), PetascopeDateTime.Granularity.YEAR);

        while (currentDateTimeTmp.compareTo(endDateTimeTmp) <= 0) {
            results.add(StringUtil.addQuotesIfNotExists(currentDateTimeTmp.format(yearDateTimeFormatter)));
            currentDateTimeTmp = currentDateTimeTmp.plusYears(1);
        }

        return results;
    }

    /**
      $c extend from 2022-11-01 through 2023-03-31
     *   allyears($c.domain.date) = < "2022", "2023" >
     *
     */
    private Set<String> getIrregularYears(List<PetascopeDateTime> petascopeDateTimes) {
        Set<String> results = new LinkedHashSet<>();
        for (PetascopeDateTime petascopeDateTime : petascopeDateTimes) {
            results.add(StringUtil.addQuotesIfNotExists(petascopeDateTime.getDateTimeMin().format(yearDateTimeFormatter)));
        }

        return results;
    }


    // ----- 2. get list of months

    /**
     *allmonths($c.domain.date) = < "2022-11", "2022-12", "2023-01", "2023-02", "2023-03" >
     *
     */
    private Set<String> getRegularMonths(PetascopeDateTime startDateTime, PetascopeDateTime endDateTime) {
        Set<String> results = new LinkedHashSet<>();
        OffsetDateTime currentDateTimeTmp = stripTimeComponentUpToGranularity(startDateTime.getDateTimeMin(), PetascopeDateTime.Granularity.MONTH);
        OffsetDateTime endDateTimeTmp = stripTimeComponentUpToGranularity(endDateTime.getDateTimeMax(), PetascopeDateTime.Granularity.MONTH);

        while (currentDateTimeTmp.compareTo(endDateTimeTmp) <= 0) {
            results.add(StringUtil.addQuotesIfNotExists(currentDateTimeTmp.format(yearMonthDateTimeFormatter)));

            currentDateTimeTmp = currentDateTimeTmp.plusMonths(1);
        }

        return results;
    }

    /**
     *allmonths($c.domain.date) = < "2022-11", "2022-12", "2023-01", "2023-02", "2023-03" >
     *
     */
    private Set<String> getIrregularMonths(List<PetascopeDateTime> petascopeDateTimes) {
        Set<String> results = new LinkedHashSet<>();
        for (PetascopeDateTime petascopeDateTime : petascopeDateTimes) {
            results.add(StringUtil.addQuotesIfNotExists(petascopeDateTime.getDateTimeMin().format(yearMonthDateTimeFormatter)));
        }

        return results;
    }

    // ----- 3. get list of days

    /**
     * alldays($c.domain.date) = < "2022-11-01", ..., "2023-03-31" >
     *
     */
    private Set<String> getRegularDays(PetascopeDateTime startDateTime, PetascopeDateTime endDateTime) {
        Set<String> results = new LinkedHashSet<>();
        OffsetDateTime currentDateTimeTmp = stripTimeComponentUpToGranularity(startDateTime.getDateTimeMin(), PetascopeDateTime.Granularity.DAY);
        OffsetDateTime endDateTimeTmp = stripTimeComponentUpToGranularity(endDateTime.getDateTimeMax(), PetascopeDateTime.Granularity.DAY);

        while (currentDateTimeTmp.compareTo(endDateTimeTmp) <= 0) {
            results.add(StringUtil.addQuotesIfNotExists(currentDateTimeTmp.format(yearMonthDayDateTimeFormatter)));

            currentDateTimeTmp = currentDateTimeTmp.plusDays(1);
        }

        return results;
    }

    /**
     * alldays($c.domain.date) = < "2022-11-01", ..., "2023-03-31" >
     *
     */
    private Set<String> getIrregularDays(List<PetascopeDateTime> petascopeDateTimes) {
        Set<String> results = new LinkedHashSet<>();
        for (PetascopeDateTime petascopeDateTime : petascopeDateTimes) {
            results.add(StringUtil.addQuotesIfNotExists(petascopeDateTime.getDateTimeMin().format(yearMonthDayDateTimeFormatter)));
        }

        return results;
    }

    // ----- 4. get list of hours

    /**
     * allhours($c.domain.date) = < "2022-11-01T00", ..., "2023-03-30T23", "2023-03-31T00" >
     *
     */
    private Set<String> getRegularHours(PetascopeDateTime startDateTime, PetascopeDateTime endDateTime) {
        Set<String> results = new LinkedHashSet<>();
        OffsetDateTime currentDateTimeTmp = stripTimeComponentUpToGranularity(startDateTime.getDateTimeMin(), PetascopeDateTime.Granularity.HOUR);
        OffsetDateTime endDateTimeTmp = stripTimeComponentUpToGranularity(endDateTime.getDateTimeMax(), PetascopeDateTime.Granularity.HOUR);

        while (currentDateTimeTmp.compareTo(endDateTimeTmp) <= 0) {
            results.add(StringUtil.quote(currentDateTimeTmp.format(yearMonthDayHourDateTimeFormatter)));

            currentDateTimeTmp = currentDateTimeTmp.plusHours(1);
        }

        return results;
    }

    /**
     * allhours($c.domain.date) = < "2022-11-01T00", ..., "2023-03-30T23", "2023-03-31T00" >
     *
     */
    private Set<String> getIrregularHours(List<PetascopeDateTime> petascopeDateTimes) {
        Set<String> results = new LinkedHashSet<>();
        for (PetascopeDateTime petascopeDateTime : petascopeDateTimes) {
            results.add(StringUtil.addQuotesIfNotExists(petascopeDateTime.getDateTimeMin().format(yearMonthDayHourDateTimeFormatter)));
        }

        return results;
    }


    // ----- 5. get list of minutes

    /**
     * allminutes($c.domain.date) = < "2022-11-01T00:00", ..., "2023-03-30T23:59", "2023-03-31T00:00" >
     *
     */
    private Set<String> getRegularMinutes(PetascopeDateTime startDateTime, PetascopeDateTime endDateTime) {
        Set<String> results = new LinkedHashSet<>();
        OffsetDateTime currentDateTimeTmp = stripTimeComponentUpToGranularity(startDateTime.getDateTimeMin(), PetascopeDateTime.Granularity.MINUTE);
        OffsetDateTime endDateTimeTmp = stripTimeComponentUpToGranularity(endDateTime.getDateTimeMax(), PetascopeDateTime.Granularity.MINUTE);

        while (currentDateTimeTmp.compareTo(endDateTimeTmp) <= 0) {
            results.add(StringUtil.addQuotesIfNotExists(currentDateTimeTmp.format(yearMonthDayHourMinuteDateTimeFormatter)));

            currentDateTimeTmp = currentDateTimeTmp.plusMinutes(1);
        }

        return results;
    }

    /**
     * allminutes($c.domain.date) = < "2022-11-01T00:00", ..., "2023-03-30T23:59", "2023-03-31T00:00" >
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
     * allseconds($c.domain.date) = < "2022-11-01T00:00:00.000", .., "2023-03-30T23:59.999", "2023-03-31T00:00.000" >
     * NOTE: it needs to contains all the milliseconds as well
     *
     */
    private Set<String> getRegularSeconds(PetascopeDateTime startDateTime, PetascopeDateTime endDateTime) {
        Set<String> results = new LinkedHashSet<>();
        OffsetDateTime currentDateTimeTmp = stripTimeComponentUpToGranularity(startDateTime.getDateTimeMin(), PetascopeDateTime.Granularity.SECOND);
        OffsetDateTime endDateTimeTmp = stripTimeComponentUpToGranularity(endDateTime.getDateTimeMax(), PetascopeDateTime.Granularity.SECOND);

        while (currentDateTimeTmp.compareTo(endDateTimeTmp) <= 0) {
            // e.g. "00.111" (1 ms = 1_000_000 nano seconds)
            results.add(StringUtil.addQuotesIfNotExists(currentDateTimeTmp.format(yearMonthDayHourMinuteSecondDateTimeFormatter)));

            currentDateTimeTmp = currentDateTimeTmp.plusNanos(1_000_000);
        }

        return results;
    }

    /**
     * allseconds($c.domain.date) = < "2022-11-01T00:00:00.000", .., "2023-03-30T23:59.999", "2023-03-31T00:00.000" >
     *
     */
    private Set<String> getIrregularSeconds(List<PetascopeDateTime> petascopeDateTimes) {
        Set<String> results = new LinkedHashSet<>();
        for (PetascopeDateTime petascopeDateTime : petascopeDateTimes) {
            // e.g. "00.111" (1 ms = 1_000_000 nano seconds)
            results.add(StringUtil.addQuotesIfNotExists(petascopeDateTime.getDateTimeMin().format(yearMonthDayHourMinuteSecondDateTimeFormatter)));
        }

        return results;
    }


}

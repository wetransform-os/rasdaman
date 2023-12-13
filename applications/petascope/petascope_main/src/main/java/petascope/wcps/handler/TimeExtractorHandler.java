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
import petascope.util.ListUtil;
import petascope.util.PetascopeDateTime;
import petascope.util.TimeUtil;
import petascope.wcps.result.VisitorResult;
import petascope.wcps.subset_axis.model.IntervalExpression;
import petascope.wcps.subset_axis.model.ListStringResult;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Handler for time extractor expression
 * e.g. years(domain($c, ansi)) or years("2015-01-01":"2018-01-20") -> [2015, 2016, 2017, 2018]
 *
 * NOTE: days( [ "2022-01" : "2023-12" ] ) results in {1..31} without duplicate numbers
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TimeExtractorHandler extends Handler {

    // e.g. months(...) -> month
    private PetascopeDateTime.Granularity granularity;
    private static final int MAX_DAYS_PER_MONTH = 31;
    private static final int MAX_HOURS_PER_MONTH = 24;
    private static final int MAX_MILLISECONDS_PER_MINUTE = 60000;
    private static final int MAX_NANOSECONDS_PER_SECOND = 1_000_000;


    public TimeExtractorHandler() {

    }

    public TimeExtractorHandler create(Handler timeDimensionIntervalHandler, PetascopeDateTime.Granularity granularity) {
        TimeExtractorHandler result = new TimeExtractorHandler();
        result.setChildren(Arrays.asList(timeDimensionIntervalHandler));
        result.granularity = granularity;

        return result;
    }

    @Override
    public VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException {
        // e.g. { "2015", "2016", "2017" } from years(domain($c, ansi))
        Set<String> extractedValues = new LinkedHashSet<>();

        // e.g. return "2015-01-01" and "2015-02-03"
        IntervalExpression intervalExpression = (IntervalExpression) this.getFirstChild().handle(serviceRegistries);
        String lowerBound = intervalExpression.getLowerBound();
        String upperBound = intervalExpression.getUpperBound();

        if (intervalExpression.getDirectPositions() == null) {
            // Regular axis or get the number of datetime component from a time interval
            PetascopeDateTime dateTimeLowerBound = TimeUtil.calculateDateTimeStringSubset(lowerBound);
            PetascopeDateTime dateTimeUpperBound = TimeUtil.calculateDateTimeStringSubset(upperBound);

            if (this.granularity == PetascopeDateTime.Granularity.YEAR) {
                extractedValues = this.getRegularYears(dateTimeLowerBound, dateTimeUpperBound);
            } else if (this.granularity == PetascopeDateTime.Granularity.MONTH) {
                extractedValues = this.getRegularMonths(dateTimeLowerBound, dateTimeUpperBound);
            } else if (this.granularity == PetascopeDateTime.Granularity.DAY) {
                extractedValues = this.getRegularDays(dateTimeLowerBound, dateTimeUpperBound);
            } else if (this.granularity == PetascopeDateTime.Granularity.HOUR) {
                extractedValues = this.getRegularHours(dateTimeLowerBound, dateTimeUpperBound);
            } else if (this.granularity == PetascopeDateTime.Granularity.MINUTE) {
                extractedValues = this.getRegularMinutes(dateTimeLowerBound, dateTimeUpperBound);
            } else if (this.granularity == PetascopeDateTime.Granularity.SECOND) {
                extractedValues = this.getRegularSeconds(dateTimeLowerBound, dateTimeUpperBound);
            }
        } else {
            // Irregular axis
            List<PetascopeDateTime> petascopeDateTimes = new ArrayList<>();
            for (String dateTimeValue : intervalExpression.getDirectPositions()) {
                PetascopeDateTime petascopeDateTime = TimeUtil.calculateDateTimeStringSubset(dateTimeValue);
                petascopeDateTimes.add(petascopeDateTime);
            }

            if (this.granularity == PetascopeDateTime.Granularity.YEAR) {
                extractedValues = this.getIrregularYears(petascopeDateTimes);
            } else if (this.granularity == PetascopeDateTime.Granularity.MONTH) {
                extractedValues = this.getIrregularMonths(petascopeDateTimes);
            } else if (this.granularity == PetascopeDateTime.Granularity.DAY) {
                extractedValues = this.getIrregularDays(petascopeDateTimes);
            } else if (this.granularity == PetascopeDateTime.Granularity.HOUR) {
                extractedValues = this.getIrregularHours(petascopeDateTimes);
            } else if (this.granularity == PetascopeDateTime.Granularity.MINUTE) {
                extractedValues = this.getIrregularMinutes(petascopeDateTimes);
            } else if (this.granularity == PetascopeDateTime.Granularity.SECOND) {
                extractedValues = this.getIrregularSeconds(petascopeDateTimes);
            }

        }

        ListStringResult listStringResult = new ListStringResult(new ArrayList<>(extractedValues));
        // NOTE: it needs to be sorted in ascending order (e.g. list of extracted hours = {"30", "50", "10"}, then change to {"10", "30", "50"}
        ListUtil.sortStringValues(listStringResult.getList());
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
            results.add(String.valueOf(currentDateTime.getYear()));
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
            results.add(String.valueOf(petascopeDateTime.getDateTimeMin().getYear()));
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
            int value = currentDateTime.getMonth().getValue();
            String tmp = TimeUtil.fillLeadingZero(value);
            results.add(tmp);

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
            int value = petascopeDateTime.getDateTimeMin().getMonth().getValue();
            String tmp = TimeUtil.fillLeadingZero(value);
            results.add(tmp);
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
            int value = currentDateTime.getDayOfMonth();
            String tmp = TimeUtil.fillLeadingZero(value);
            results.add(tmp);

            currentDateTime = currentDateTime.plusDays(1);

            if (results.size() == MAX_DAYS_PER_MONTH) {
                // A month contains max 31 days
                break;
            }
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
            int value = petascopeDateTime.getDateTimeMin().getDayOfMonth();
            String tmp = TimeUtil.fillLeadingZero(value);
            results.add(tmp);

            if (results.size() == MAX_DAYS_PER_MONTH) {
                // A month contains max 31 days
                break;
            }
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
            int value = currentDateTime.getHour();
            String tmp = TimeUtil.fillLeadingZero(value);
            results.add(tmp);

            currentDateTime = currentDateTime.plusHours(1);

            if (results.size() == 24) {
                // A day contains max 24 hours (00...23)
                break;
            }
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
            int value = petascopeDateTime.getDateTimeMin().getHour();
            String tmp = TimeUtil.fillLeadingZero(value);
            results.add(tmp);

            if (results.size() == 24) {
                // A day contains max 24 hours (00...23)
                break;
            }
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
            int value = currentDateTime.getMinute();
            String tmp = TimeUtil.fillLeadingZero(value);
            results.add(tmp);

            currentDateTime = currentDateTime.plusMinutes(1);

            if (results.size() == 60) {
                // An hour contains max 60 minutes (00...59)
                break;
            }
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
            int value = petascopeDateTime.getDateTimeMin().getMinute();
            String tmp = TimeUtil.fillLeadingZero(value);
            results.add(tmp);

            if (results.size() == 24) {
                // A day contains max 24 hours (00...23)
                break;
            }
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
            int value = currentDateTime.getSecond();
            String tmp = TimeUtil.fillLeadingZero(value);
            tmp = tmp + "." + TimeUtil.fillLeadingTwoZeros(currentDateTime.getNano() / MAX_NANOSECONDS_PER_SECOND);
            results.add(tmp);

            currentDateTime = currentDateTime.plusNanos(MAX_NANOSECONDS_PER_SECOND);

            if (results.size() == MAX_MILLISECONDS_PER_MINUTE) {
                // A minute contains max 60 seconds and a second contains max 1000 milliseconds
                break;
            }
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
            int value = petascopeDateTime.getDateTimeMin().getSecond();
            String tmp = TimeUtil.fillLeadingZero(value);
            tmp = tmp + "." + TimeUtil.fillLeadingTwoZeros(petascopeDateTime.getDateTimeMin().getNano() / MAX_NANOSECONDS_PER_SECOND);
            results.add(tmp);

            if (results.size() == MAX_MILLISECONDS_PER_MINUTE) {
                // A minute contains max 60 seconds and a second contains max 1000 milliseconds
                break;
            }
        }

        return results;
    }

}

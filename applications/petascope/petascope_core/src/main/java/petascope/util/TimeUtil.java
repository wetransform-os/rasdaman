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
 * Copyright 2003 - 2014 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petascope.core.CrsDefinition;
import petascope.core.Pair;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.WCSException;

/**
 * Class for handling timestamps formatting and elaborations. It relies on the
 * Joda-Time library (http://www.joda.org/joda-time/).
 *
 * @author <a href="mailto:p.campalani@jacobs-university.de">Piero Campalani</a>
 */
public class TimeUtil {

    // Standard codes for supported temporal Unit of Measures (which provide ISO8601 interface to the user)
    // http://unitsofmeasure.org/ucum.html
    public static final String UCUM = "UCUM";
    
    public static final String UCUM_MILLIS = "ms";
    public static final String UCUM_SECOND = "s";
    public static final String UCUM_MINUTE = "min";
    public static final String UCUM_HOUR = "h";
    public static final String UCUM_DAY = "d";
    public static final String UCUM_WEEK = "wk";
    public static final String UCUM_MEAN_JULIAN_MONTH = "mo_j";
    public static final String UCUM_MEAN_GREGORIAN_MONTH = "mo_g";
    public static final String UCUM_SYNODAL_MONTH = "mo_s";
    public static final String UCUM_MONTH = "mo";
    public static final String UCUM_MEAN_JULIAN_YEAR = "a_j";
    public static final String UCUM_MEAN_GREGORIAN_YEAR = "a_g";
    public static final String UCUM_TROPICAL_YEAR = "a_t";
    public static final String UCUM_YEAR = "a";

    // Milliseconds associated to each supported temporal UoM:
    public static final Long MILLIS_MILLIS = 1L;
    public static final Long MILLIS_SECOND = 1000L;
    public static final Long MILLIS_MINUTE = MILLIS_SECOND * 60;
    public static final Long MILLIS_HOUR = MILLIS_MINUTE * 60;
    public static final Long MILLIS_DAY = MILLIS_HOUR * 24;
    public static final Long MILLIS_WEEK = MILLIS_DAY * 7;
    public static final Long MILLIS_MEAN_JULIAN_YEAR = (long) (MILLIS_DAY * 365.25);
    public static final Long MILLIS_MEAN_GREGORIAN_YEAR = (long) (MILLIS_DAY * 365.2425);
    public static final Long MILLIS_TROPICAL_YEAR = (long) (MILLIS_DAY * 365.24219);
    public static final Long MILLIS_YEAR = MILLIS_MEAN_JULIAN_YEAR;
    public static final Long MILLIS_MEAN_JULIAN_MONTH = MILLIS_MEAN_JULIAN_YEAR / 12;
    public static final Long MILLIS_MEAN_GREGORIAN_MONTH = MILLIS_MEAN_GREGORIAN_YEAR / 12;
    public static final Long MILLIS_SYNODAL_MONTH = (long) (MILLIS_DAY * 29.53059);
    public static final Long MILLIS_MONTH = MILLIS_MEAN_JULIAN_MONTH;

    // NOTE: time can have fraction of seconds as well (joda time: http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html)
    public static final String ISO_8061_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z";

    // -- Calendar functionalities

    private static final Pattern TIME_INTERVAL_PATTERN = Pattern.compile("(\".*?\"):(\".*?\")");

    public static final String PERIODICITY_KEYWORD = "P";
    public static final String TIME_DELIMITER_KEYWORD = "T";
    // e.g. T01GMT+7 or T02+07:00 or T03:05-05:00
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d\\d)(:\\d\\d)?(:\\d\\d)?(.\\d\\d\\d)?[+-a-zA-Z\\\\0-9_]*");
    public static final java.time.format.DateTimeFormatter yearDateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy");
    public static final java.time.format.DateTimeFormatter yearMonthDateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");
    public static final java.time.format.DateTimeFormatter yearMonthDayDateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final java.time.format.DateTimeFormatter yearMonthDayHourDateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");
    public static final java.time.format.DateTimeFormatter yearMonthDayHourMinuteDateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    public static final java.time.format.DateTimeFormatter yearMonthDayHourMinuteSecondDateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public static final java.time.format.DateTimeFormatter ISO_DATETIME_WITH_TIME_OFFSET_FORMAT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX");

    // And -1 ms, e.g. 2015-01-01T01:01:00 returns  2015-01-01T01:00:59.999
    public static int MINUS_ONE_MILLISECOND = -1_000_000;

    public static String DURATION_ONE_MILLISECOND_REPRESENTATION = "1.001S";

    public static final String INTERNAL_DELIMITER_CHARACTER = "|";
    // e.g. "2013-01-01/P1M" here even last time component is 1 day, but granularity is actually 1 month
    public static String TIME_AND_GRANULARITY_DELIMITER = "/";

    // Logger
    private static Logger log = LoggerFactory.getLogger(TimeUtil.class);

    /**
     * Registry of Temporal UoMs which support ISO timestamp conversion.
     */
    private static final Map<String, Long> timeUomsRegistry = new HashMap<String, Long>();

    /**
     * ISO datetime formatter. Valid formats and other ISO formatters:
     * http://www.joda.org/joda-time/apidocs/org/joda/time/format/ISODateTimeFormat.html
     *
     * The default chronology in Joda-Time is ISO (ISO8601). For other Joda-Time
     * Chronologies see
     * http://www.joda.org/joda-time/apidocs/org/joda/time/Chronology.html. For
     * a description of the ISO8601 calendar system see
     * http://www.joda.org/joda-time/cal_iso.html.
     *
     * "For most applications, the Chronology can be ignored as it will default
     * to the ISOChronology. This is suitable for most uses. You would change it
     * if you need accurate dates before October 15, 1582, or whenever the
     * Julian calendar ceased in the territory you're interested in). You'd also
     * change it if you need a specific calendar like the Coptic calendar
     * illustrated earlier". (Joda-Time)
     */
    private static final DateTimeFormatter isoFmt;

    static {
        //Create an ISO formatter with optional time and UTC default time zone
        isoFmt = ISODateTimeFormat.dateOptionalTimeParser().withZone(DateTimeZone.UTC);
        // NOTE: if zone is not assigned, the default time zone of the JVM will be picked: no-no.

        // Fill the registry of time UoMs:
        timeUomsRegistry.put(UCUM_MILLIS, MILLIS_MILLIS);
        timeUomsRegistry.put(UCUM_SECOND, MILLIS_SECOND);
        timeUomsRegistry.put(UCUM_MINUTE, MILLIS_MINUTE);
        timeUomsRegistry.put(UCUM_HOUR, MILLIS_HOUR);
        timeUomsRegistry.put(UCUM_DAY, MILLIS_DAY);
        timeUomsRegistry.put(UCUM_WEEK, MILLIS_WEEK);
        timeUomsRegistry.put(UCUM_MEAN_JULIAN_MONTH, MILLIS_MEAN_JULIAN_MONTH);
        timeUomsRegistry.put(UCUM_MEAN_GREGORIAN_MONTH, MILLIS_MEAN_GREGORIAN_MONTH);
        timeUomsRegistry.put(UCUM_SYNODAL_MONTH, MILLIS_SYNODAL_MONTH);
        timeUomsRegistry.put(UCUM_MONTH, MILLIS_MONTH);
        timeUomsRegistry.put(UCUM_MEAN_JULIAN_YEAR, MILLIS_MEAN_JULIAN_YEAR);
        timeUomsRegistry.put(UCUM_MEAN_GREGORIAN_YEAR, MILLIS_MEAN_GREGORIAN_YEAR);
        timeUomsRegistry.put(UCUM_TROPICAL_YEAR, MILLIS_TROPICAL_YEAR);
        timeUomsRegistry.put(UCUM_YEAR, MILLIS_YEAR);
    }

    /**
     * Check if the input timestamp is in an understandable format.
     *
     * @param timestamp Timestamp string requested by the client
     * @return True if valid ISO timestamp.
     */
    public static boolean isValidTimestamp(String timestamp) {
        boolean isValid = true;
        try {
            // date time format valid by ISO 8601, e.g:  YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
            DateTime dt = isoFmt.parseDateTime(fix(timestamp));
        } catch (IllegalArgumentException ex) {
            log.debug(timestamp + " format is invalid or unsupported: " + ex.getMessage());
            isValid = false;
        }
        return isValid;
    }

    /**
     * Verifies that the two input timestamps define a valid interval.
     *
     * @param timestampLo timestamp
     * @param timestampHi timestamp
     * @return True if Lo is lower or equal than Hi
     * @throws petascope.exceptions.WCSException
     */
    public static boolean isOrderedTimeSubset(String timestampLo, String timestampHi) throws WCSException {

        DateTime dtLo = isoFmt.parseDateTime(fix(timestampLo));
        DateTime dtHi = isoFmt.parseDateTime(fix(timestampHi));
        Duration millis;
        try {
            millis = new Duration(dtLo, dtHi);
        } catch (ArithmeticException ex) {
            log.error("Error while computing milliseconds between " + dtLo + " and " + dtHi + ".");
            throw new WCSException(ExceptionCode.InternalComponentError,
                    "Error while computing milliseconds between " + dtLo + " and " + dtHi + ". Reason: " + ex.getMessage(), ex);
        }
        return (millis.getMillis() >= 0);
    }
    
    /**
     * Check if time is within a time interval in ISO format
     * e.g: "2015-01-02" is within ("2015-01-01":"2015-03-04")
     */
    public static boolean isInTimeInterval(String inputTime, String timeLowerBound, String timeUpperBound) {
        DateTime dateTime = isoFmt.parseDateTime(fix(inputTime));
        DateTime dateTimeLowerBound = isoFmt.parseDateTime(fix(timeLowerBound));
        DateTime dateTimeUpperBound = isoFmt.parseDateTime(fix(timeUpperBound));
        
        return dateTime.equals(dateTimeLowerBound) 
              || dateTime.equals(dateTimeUpperBound) 
              || dateTime.isAfter(dateTimeLowerBound) && dateTime.isBefore(dateTimeUpperBound);
    }

    /**
     * Count how many temporal units (i.e. offset vectors) fit inside a time
     * interval.
     *
     * @param timestampLo Lower ISO timestamp
     * @param timestampHi Upper ISO timestamp
     * @param timeResolution Temporal UoM of the CRS
     * @param timeVector Length of the offset vector along time [TIME(n) :=
     * timeEpoch + n*(timeResolution*timeVector)]
     * @return How many time units (with fractional resolution) fit into the
     * time interval [timestampHi-timestampLo]
     * @throws petascope.exceptions.PetascopeException
     */
    public static BigDecimal countOffsets(String timestampLo, String timestampHi, String timeResolution, BigDecimal timeVector)
            throws PetascopeException {

        // local variables
        BigDecimal fractionalTimeSteps;

        DateTime dtLo = isoFmt.parseDateTime(fix(timestampLo));
        DateTime dtHi = isoFmt.parseDateTime(fix(timestampHi));

        // Determine milliseconds between these two datetimes
        Duration millis;
        try {
            millis = new Duration(dtLo, dtHi);
        } catch (ArithmeticException ex) {
            throw new PetascopeException(ExceptionCode.InternalComponentError,
                    "Error while computing milliseconds between " + dtLo + " and " + dtHi + ". Reason: " + ex.getMessage(), ex);
        }

        // Compute how many vectors of distance are there between dtLo and dtHi along this time CRS
        // NOTE: not necessarily an integer number of vectors will fit in the interval, clearly.
        // Formula:
        //               fractionalTimeSteps := milliseconds(lo,hi) / [milliseconds(offset_vector)]
        // WHERE milliseconds(offset_vector) := milliseconds(timeResolution) * vector_length
        Long vectorMillis = (new BigDecimal(getMillis(timeResolution)).multiply(timeVector)).longValue();
        fractionalTimeSteps = BigDecimalUtil.divide(new BigDecimal(millis.getMillis()), new BigDecimal(vectorMillis));

        // log.debug("Computed " + fractionalTimeSteps + " offset-vectors between " + dtLo + " and " + dtHi + ".");
        return fractionalTimeSteps;
    }

    /**
     * Removes an epsilon to the input timestamp.
     *
     * When translating to cell indexes, there is a particular case:
     *
     * 9h00 10h00 11h00 12h00 13h00 o---------o---------o---------o---------o
     * cell0 cell1 cell2 cell3
     *
     * E.g. subset=t(10h00:18h00) cellLo = cellMin + countPixels( 9h00:10h00) =
     * cellMin + 1 cellHi = cellLo + countPixels(10h00:13h00) = cellLo + 3 -->
     * overflow
     *
     * Whereas subset=t(10h01:18h00) would work fine: things work when excluding
     * the maximum, i.e. < instead of <=.
     *
     * @param timestamp ISO:8601 timestamp
     * @return 1 nanosecond before timestamp.
     */
    public static String minusEpsilon(String timestamp) {
        DateTime dt = isoFmt.parseDateTime(fix(timestamp));
        DateTime dtEps = dt.minusMillis(1);
        return dtEps.toString();
    }

    /**
     * Add a certain time duration to a timestamp.
     *
     * @param timestamp The starting time instant.
     * @param coefficient The coefficient of the second addend.
     * @param timeResolution The time resolution to be added to timestamp ("c"
     * times).
     * @return The ISO representation of timestamp+c*timeResolution.
     * @throws PetascopeException
     */
    public static String plus(String timestamp, Double coefficient, String timeResolution) throws PetascopeException {
        DateTime dt = isoFmt.parseDateTime(fix(timestamp));
        DateTime outDt = dt.plus((long) (getMillis(timeResolution) * coefficient));
        return outDt.toString();
    }

    /**
     * Retrieves the time instant of a numeric time coordinate.
     *
     * @param numCoordinate
     * @param datumOrigin
     * @param timeResolution
     * @return The time instant correspondent to the input time coordinate.
     * @throws PetascopeException
     */
    public static String coordinate2timestamp(Double numCoordinate, String datumOrigin, String timeResolution) throws PetascopeException {
        return TimeUtil.plus(datumOrigin, numCoordinate, timeResolution);
    }

    /**
     * Get the number of milliseconds from the UOM of CRS
     *
     * @param crsDefinition
     * @return
     * @throws petascope.exceptions.PetascopeException
     */
    public static Long getMillis(CrsDefinition crsDefinition) throws PetascopeException {
        String ucumAbbreviation = crsDefinition.getAxes().get(0).getUoM();
        return getMillis(ucumAbbreviation);
    }
    
    
    /**
     * Converts the time coefficient to ISO datetime stamp
     * yyyy-MM-dd'T'HH:mm:ssZ (e.g: 2008-01-01T00:00:00Z)
     *
     * @param firstPoint the value of first coefficient from origin (e.g: 2008-01-01 is 148654 from AnsiDate origin: 1601-01-01)
     * @param coeff a time point
     * @param crsDefinition contains information of Time CRS     
     * @return string list of time coefficients (days, seconds) to ISO datetime
     * stamp
     * @throws petascope.exceptions.PetascopeException
     */
    public static String valueToISODateTime(BigDecimal firstPoint, BigDecimal coeff, CrsDefinition crsDefinition) throws PetascopeException {        
        // Get the UOM in milliseconds (e.g: ansidate uom: d is 86 400 000 millis, unixtime uom: seconds is 1000 millis)
        Long milliSeconds = TimeUtil.getMillis(crsDefinition);
        DateTime dateTime = new DateTime(crsDefinition.getDatumOrigin());

        // formula: (firstPoint + Time Coefficients) * UOM in milliSeconds
        long duration = ((firstPoint.add(coeff)).multiply(new BigDecimal(milliSeconds))).setScale(0, RoundingMode.HALF_UP).longValue();
        DateTime dt = dateTime.plus(duration);

        // Then convert the added date to ISO 8601 datetime (Z means UTC)
        // and we add the qoute to make XML parser easier as it is time value
        String isoDateTime = "\"" + dt.toString(DateTimeFormat.forPattern(TimeUtil.ISO_8061_FORMAT).withZoneUTC()) + "\"";        
        return isoDateTime;
    }

    /**
     * Converts the time coefficients to ISO datetime stamp
     * yyyy-MM-dd'T'HH:mm:ssZ (e.g: 2008-01-01T00:00:00Z)
     *
     * @param firstPoint the value of first coefficient from origin (e.g: 2008-01-01 is 148654 from AnsiDate origin: 1601-01-01)
     * @param coeffs time coefficients from time axis (NOTE: added with the
     * SubsetLow of start date)
     * @param crsDefinition contains information of Time CRS     
     * @return string list of time coefficients (days, seconds) to ISO datetime
     * stamp
     * @throws petascope.exceptions.PetascopeException
     */
    public static List<String> listValuesToISODateTime(BigDecimal firstPoint, List<BigDecimal> coeffs, CrsDefinition crsDefinition) throws PetascopeException {
        List<String> isoDateTimes = new ArrayList<>();

        // Get the UOM in milliseconds (e.g: ansidate uom: d is 86 400 000 millis, unixtime uom: seconds is 1000 millis)
        Long milliSeconds = TimeUtil.getMillis(crsDefinition);
        DateTime dateTime = new DateTime(crsDefinition.getDatumOrigin());

        // NOTE: When doing a trim subset on an irregular axis (time) then the coefficients of the result are not corresponding to the new subsetted time interval.
        // (i.e: first coefficient should be 0 when not doing subset, but it is a value > 0), therefore need to substract for first coefficient to normalize the list of 
        // subsetted coefficients or it will return wrong values for time axis.
        BigDecimal firstCoefficient = coeffs.get(0);
        for (BigDecimal coeff : coeffs) {
            BigDecimal tempCoeff = coeff.subtract(firstCoefficient);
            // formula: (firstPoint + Time Coefficients) * UOM in milliSeconds
            long duration = ((firstPoint.add(tempCoeff)).multiply(new BigDecimal(milliSeconds))).setScale(0, RoundingMode.HALF_UP).longValue();
            DateTime dt = dateTime.plus(duration);

            // Then convert the added date to ISO 8601 datetime (Z means UTC)
            // and we add the qoute to make XML parser easier as it is time value
            isoDateTimes.add("\"" + dt.toString(DateTimeFormat.forPattern(TimeUtil.ISO_8061_FORMAT).withZoneUTC()) + "\"");
        }
        return isoDateTimes;
    }
    
    /**
     * Parse a datetime string by a valid datetime pattern.
     * e.g: "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ISO 8601 datetime format
     */
    public static DateTime parseDateTimeByPattern(String inputDateTime) throws PetascopeException {
        DateTime dateTime = isoFmt.parseDateTime(fix(inputDateTime));
        return dateTime;
    }

    // --- Calendar functionalities



    /**
     * In case subsetDimension has datetime concatenating string subsets WITH Periodicity [AND with TIME ZONE conversion],
     * then, this value must be calculated properly before passing to SubsetExpressionHandler
     * For example:
     * 2015-01-02T02:03:05.000Z P-1D TO TZ +02:00 (Z is UTC = GMT+0)
     * will be calculated as
     * 2015-01-02T02:03:05.000Z (because P-1D)
     * then
     * 2015-01-02T04:03:05.000Z (because TO TZ +02:00)
     *
     * @param inputDateTimeBound e.g. "2015-01-01" or "2015-01-01T00:00Z" or "2015-01-01T03:05:07.222+05:00 P-2YT1D"
     *                      or "2015-01-06-07 PT-3H TO TZ UTC+05:00"
     *                      Formula: DATETIME [PERIODCITY (e.g. P2M)] [TO TZ TIMEOFFSET / ZONENAME (e.g. PST)]
     */
    public static PetascopeDateTime calculateDateTimeStringSubset(String inputDateTimeBound) throws PetascopeException {
        inputDateTimeBound = StringUtil.stripQuotes(inputDateTimeBound).replaceAll("\\s+", " ");

        // e.g. +05:00
        String targetTimeZone = null;

        // e.g. "2012-01-01/P1M" -> "2012-01-01"
        String[] parts = inputDateTimeBound.split(TimeUtil.TIME_AND_GRANULARITY_DELIMITER);

        // e.g. P1M from "2012-01-01/P1M"
        PetascopeDateTime.Granularity originalDateTimeGranularity = null;
        if (parts.length > 1) {
            // e.g. "P1M P-1D" with P1M is the original granularity and P-1D as the durationShifting
            String secondPart = parts[1].split(" ")[0];
            if (!secondPart.startsWith(PERIODICITY_KEYWORD)) {
                throw new PetascopeException(ExceptionCode.InvalidRequest,
                        "Datetime value is not valid. Given: " + StringUtil.addQuotesIfNotExists(inputDateTimeBound));
            }
            String[] tmps = secondPart.split(PERIODICITY_KEYWORD);
            String granularityStr = PERIODICITY_KEYWORD + tmps[1];
            originalDateTimeGranularity = getGranularityByPeriodicity(granularityStr);

            // Then, strip this original granularity string from the input time bound
            // e.g. "2015-01/P1M P-1D" or "2015-01/P1MP-1D" -> "2015-01P-1D"
            inputDateTimeBound = inputDateTimeBound.replace(TimeUtil.TIME_AND_GRANULARITY_DELIMITER + granularityStr, "");
        }

        String[] tmps = inputDateTimeBound.split(PERIODICITY_KEYWORD);
        String dateTimeBound = tmps[0].trim();
        List<String> durationShifts = new ArrayList<>();
        if (tmps.length > 1) {
            // e.g. "2015-01-01 P1M P-3D" -> ["P1M", "P-3D"]
            for (int i = 1; i < tmps.length; i++) {
                String isoPeriodicityShift = PERIODICITY_KEYWORD + tmps[i];
                durationShifts.add(isoPeriodicityShift);
            }
        }

        // e.g. in case 2015/P1Y . "-01-01" then the datetime's granularity is actually day not year because it is just string concatenation
        // not shifting by periodicity
        tmps = inputDateTimeBound.split(" ");
        if (tmps.length > 1 && !tmps[0].equals(PERIODICITY_KEYWORD)) {
            dateTimeBound = StringUtil.removeWhiteSpaces(dateTimeBound);
            originalDateTimeGranularity = null;
        }

        if (originalDateTimeGranularity == null) {
            originalDateTimeGranularity = TimeUtil.getGranularity(dateTimeBound);
        }


        PetascopeDateTime result = null;
        if (durationShifts.size() > 0) {
            String currentDateTimeBound = dateTimeBound;

            for (String durationShift : durationShifts) {
                result = calculatePetascopeDateTimeByPeriodicityAndTimeZone(currentDateTimeBound, durationShift,
                        targetTimeZone, originalDateTimeGranularity);
                currentDateTimeBound = result.getDateTimeMinISOFormatStr();
            }
        } else {
            // no duration shift declared
            result = calculatePetascopeDateTimeByPeriodicityAndTimeZone(dateTimeBound, null,
                                                    targetTimeZone, originalDateTimeGranularity);
        }

        return result;
    }

    /**
     * @param dateTimeBound e.g. "2015-01-0100:00:00+03:00"
     * @param isoPeriodicity e.g. "P1Y", then dateTimeBound + one year or "PT-1D", then dateTimeBound - 1 day
     * @param targetZoneName e.g. "-05:00" (offset) or "PST" (ZoneId)
     */
    public static PetascopeDateTime calculatePetascopeDateTimeByPeriodicityAndTimeZone(String dateTimeBound, String isoPeriodicity, String targetZoneName, PetascopeDateTime.Granularity originalGranularity) throws PetascopeException {

        if (isoPeriodicity != null) {
            isoPeriodicity = StringUtil.stripQuotes(isoPeriodicity);
        }

        java.time.format.DateTimeFormatter datetimeFormatter = new DateTimeFormatterBuilder()
                                                                        .appendValue(ChronoField.YEAR, 4)
                                                                        .appendPattern("[['-']MM[['-']dd[['T']HH[[':']mm[[':']ss['.'SSS]]]]]][Z][z][O][VV]")
                                                                        .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                                                                        .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                                                                        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                                                                        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                                                                        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                                                                        .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                                                                        .toFormatter();
        dateTimeBound = StringUtil.stripQuotes(dateTimeBound);

        TemporalAccessor accessor = datetimeFormatter.parse(dateTimeBound);
        ZoneId zone = accessor.query(TemporalQueries.zone());
        ZoneOffset offset = accessor.query(TemporalQueries.offset());
        LocalDateTime localDateTime = LocalDateTime.from(accessor);

        ZonedDateTime sourceZonedDateTime = null;

        if (offset != null) {
            // NOTE: in this case there is offset in the bound, e.g. +05:00
            sourceZonedDateTime = ZonedDateTime.of(localDateTime, offset);
        } else if (zone != null) {
            // this chooses the earlier time in an overlap
            sourceZonedDateTime = ZonedDateTime.of(localDateTime, zone);
        } else {
            // default is UTC (GMT+0)
            sourceZonedDateTime = ZonedDateTime.of(localDateTime, ZoneOffset.UTC);
        }

        // ------------ add / subtract by periodicity if needed

        if (isoPeriodicity != null) {

            // for P year / month / day
            Period period = null;

            // for PT hour / minute / second
            java.time.Duration duration = null;

            String isoPeriodicityTmp = isoPeriodicity.replace(PERIODICITY_KEYWORD, "");

            if (isoPeriodicityTmp.startsWith(TIME_DELIMITER_KEYWORD)) {
                // e.g. duration: T1H (1 hour) -> PT1H
                duration = parseDuration(PERIODICITY_KEYWORD + isoPeriodicityTmp);
            } else if (!isoPeriodicityTmp.startsWith(TIME_DELIMITER_KEYWORD) && !isoPeriodicityTmp.contains(TIME_DELIMITER_KEYWORD)) {
                // e.g. period: 1Y (1 year) -> P1Y
                period = parsePeriod(PERIODICITY_KEYWORD + isoPeriodicityTmp);
            } else {
                // e.g 1DT1H (1 day and 1 hour)
                // 1D -> P1D (period)
                // T1H -> PT1H (duration)
                String[] parts = isoPeriodicityTmp.split(TIME_DELIMITER_KEYWORD);
                period = parsePeriod(PERIODICITY_KEYWORD + parts[0]);
                duration = parseDuration(PERIODICITY_KEYWORD + TIME_DELIMITER_KEYWORD + parts[1]);
            }

            if (period != null) {
                sourceZonedDateTime = sourceZonedDateTime.plus(period);
            }
            if (duration != null) {
                sourceZonedDateTime = sourceZonedDateTime.plus(duration);
            }
        }

        // ------------ timezone conversion if needed

        ZoneId targetZoneId = null;
        if (targetZoneName == null) {
            // default is UTC time
            targetZoneId = ZoneId.of(ZoneOffset.UTC.getId());
        } else {
            targetZoneId = ZoneId.of(targetZoneName);
        }

        // The result of offset datetime is e.g. "2015-01-01T00:03:00.002Z+05:00" with targetZoneId="+05:00"
        OffsetDateTime offsetDateTime = sourceZonedDateTime.withZoneSameInstant(targetZoneId).toOffsetDateTime();
        PetascopeDateTime result = new PetascopeDateTime(offsetDateTime, originalGranularity);
        return result;

    }

    /**
     * e.g. parse P1D
     */
    private static java.time.Period parsePeriod(String str) throws PetascopeException {
        str = str.trim();

        // e.g. duration: T1H (1 hour) -> PT1H
        try {
            java.time.Period per = java.time.Period.parse(str);
            return per;
        } catch (Exception ex) {
            throw new PetascopeException(ExceptionCode.InvalidRequest, "Cannot parse period from the periodicity input value. Given: " + str, ex);
        }
    }

    /**
     * e.g. parse PT1H
     */
    private static java.time.Duration parseDuration(String str) throws PetascopeException {
        // e.g. duration: T1H (1 hour) -> PT1H
        try {
            java.time.Duration duration = java.time.Duration.parse(str);
            return duration;
        } catch (Exception ex) {
            throw new PetascopeException(ExceptionCode.InvalidRequest, "Cannot parse duration from the periodicity input value. Given: " + str, ex);
        }
    }

    /**
     * Given a valid datetime string, then get its granularity
     * e.g. 2015 -> year, 2015-01 -> month, 2015-01-02 -> day 2015-01-02T03 -> hour
     */
    public static PetascopeDateTime.Granularity getGranularity(String dateTimeBound) {
        dateTimeBound = dateTimeBound.trim();
        PetascopeDateTime.Granularity result = PetascopeDateTime.Granularity.YEAR;

        // e.g. "2023-01 P-1D" = "2023-01" only
        dateTimeBound = dateTimeBound.split(PERIODICITY_KEYWORD)[0];

        String[] parts = dateTimeBound.split(TIME_DELIMITER_KEYWORD);
        String dateTmp = parts[0];
        if (parts.length == 1) {
            // Only year or year-month or year-month-day
            String[] dateParts = dateTmp.split("-");
            if (dateParts.length == 2) {
                // e.g. 2015-01
                result = PetascopeDateTime.Granularity.MONTH;
            } else if (dateParts.length == 3) {
                // e.g. 2015-01-02
                result = PetascopeDateTime.Granularity.DAY;
            }
        } else {
            // It has year-month-day and time
            // time is e.g. 2015-01
            String timeTmp = parts[1];
            Matcher matcher = TIME_PATTERN.matcher(timeTmp);
            int count = 0;
            while (matcher.find()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    if (matcher.group(i) != null) {
                        count++;
                    } else {
                        break;
                    }
                }
            }

            if (count == 1) {
                // e.g. T05GMT+7
                result = PetascopeDateTime.Granularity.HOUR;
            } else if (count == 2) {
                // e.g. T05:02GMT+7
                result = PetascopeDateTime.Granularity.MINUTE;
            } else if (count == 3) {
                // e.g. T05:01:02GMT+7
                result = PetascopeDateTime.Granularity.SECOND;
            } else if (count == 4) {
                // e.g. T05:01:02.222GMT+7
                result = PetascopeDateTime.Granularity.MILLISECOND;
            }
        }

        return result;
    }

    /**
     * e.g. Granularity is 1 year -> P1Y
     * or it is hour -> PT1H
     */
    public static String getPeriodicityRepresentation(PetascopeDateTime.Granularity granularity) throws PetascopeException {
        if (granularity == PetascopeDateTime.Granularity.YEAR
           || granularity == PetascopeDateTime.Granularity.MONTH
           || granularity == PetascopeDateTime.Granularity.DAY) {
            // e.g. P1D
            return PERIODICITY_KEYWORD + "1" + granularity.toString().charAt(0);
        } else if (granularity == PetascopeDateTime.Granularity.HOUR
                || granularity == PetascopeDateTime.Granularity.MINUTE
                || granularity == PetascopeDateTime.Granularity.SECOND) {
            // e.g. PT1H
            return PERIODICITY_KEYWORD + TIME_DELIMITER_KEYWORD + "1" + granularity.toString().charAt(0);
        } else if (granularity == PetascopeDateTime.Granularity.MILLISECOND) {
            // e.g. 1 ms = PT1.001S in java Duration representation
            return PERIODICITY_KEYWORD + TIME_DELIMITER_KEYWORD
                    + DURATION_ONE_MILLISECOND_REPRESENTATION;
        }

        throw new PetascopeException(ExceptionCode.NoApplicableCode, "Unsupported granularity: " + granularity);
    }

    /**
     * e.g. P1YT1H -> Hour
     */
    public static PetascopeDateTime.Granularity getGranularityByPeriodicity(String periodicity) throws PetascopeException {
        periodicity = StringUtil.stripQuotes(periodicity).trim();

        String lastCharacter = String.valueOf(periodicity.charAt(periodicity.length() - 1)).toUpperCase();
        if (lastCharacter.equals("Y")) {
            return PetascopeDateTime.Granularity.YEAR;
        } else if (lastCharacter.equals("M") && !periodicity.contains(TIME_DELIMITER_KEYWORD)) {
            return PetascopeDateTime.Granularity.MONTH;
        } else if (lastCharacter.equals("D")) {
            return PetascopeDateTime.Granularity.DAY;
        } else if (lastCharacter.equals("H")) {
            return PetascopeDateTime.Granularity.HOUR;
        } else if (lastCharacter.equals("M")) {
            return PetascopeDateTime.Granularity.MINUTE;
        } else if (lastCharacter.equals("S")) {
            return PetascopeDateTime.Granularity.SECOND;
        }

        throw new PetascopeException(ExceptionCode.NoApplicableCode, "Cannot determine granularity of input periodicity. Given: " + periodicity);
    }

    /**
     *
     * e.g. Granularity is month -> P1M
     */
    public static String getPeriodicityByGranularity(PetascopeDateTime.Granularity granularity) throws PetascopeException {
        if (granularity == PetascopeDateTime.Granularity.YEAR) {
            return PERIODICITY_KEYWORD + "1Y";
        } else if (granularity == PetascopeDateTime.Granularity.MONTH) {
            return PERIODICITY_KEYWORD + "1M";
        } else if (granularity == PetascopeDateTime.Granularity.DAY) {
            return PERIODICITY_KEYWORD + "1D";
        } else if (granularity == PetascopeDateTime.Granularity.HOUR) {
            return PERIODICITY_KEYWORD + TIME_DELIMITER_KEYWORD + "1H";
        } else if (granularity == PetascopeDateTime.Granularity.MINUTE) {
            return PERIODICITY_KEYWORD + TIME_DELIMITER_KEYWORD + "1M";
        } else if (granularity == PetascopeDateTime.Granularity.SECOND || granularity == PetascopeDateTime.Granularity.MILLISECOND) {
            return PERIODICITY_KEYWORD + TIME_DELIMITER_KEYWORD + "1S";
        }

        throw new PetascopeException(ExceptionCode.NoApplicableCode, "Cannot retrieve periodicity string by input granularity. Given: " + granularity);
    }


    /**
     * Return a pair of lowerBound:upperBound
     * from "datetime":"datetime" string
     *
     */
    public static Pair<String, String> parseBounds(String timeIntervalStr) throws PetascopeException {
        timeIntervalStr = timeIntervalStr.trim();

        Pair<String, String> result = null;
        Matcher matcher = TIME_INTERVAL_PATTERN.matcher(timeIntervalStr);
        while(matcher.find()) {
            String lowerBound = matcher.group(1);
            String upperBound = matcher.group(2);

            result = new Pair<>(lowerBound, upperBound);
            return result;
        }

        throw new PetascopeException(ExceptionCode.InvalidRequest, "Input string is not time interval. Given: " + timeIntervalStr);
    }

    /**
     *
     * e.g. DateTime is "2015-01" (granularity is month), but now to print with granularity is hour then the result is
     * "2015-01-01T00"
     * or DateTime is "2015-01-01T10:20:30.002", and input granularity is hour -> "2015-01-01T10"
     */
    public static String getDateTimeRepresentationInTargetGranularity(PetascopeDateTime petascopeDateTime, PetascopeDateTime.Granularity targetGranularity) throws PetascopeException {
        java.time.format.DateTimeFormatter dateTimeFormatter = null;
        if (targetGranularity == PetascopeDateTime.Granularity.YEAR) {
            dateTimeFormatter = yearDateTimeFormatter;
        } else if (targetGranularity == PetascopeDateTime.Granularity.MONTH) {
            dateTimeFormatter = yearMonthDateTimeFormatter;
        } else if (targetGranularity == PetascopeDateTime.Granularity.DAY) {
            dateTimeFormatter = yearMonthDayDateTimeFormatter;
        } else if (targetGranularity == PetascopeDateTime.Granularity.HOUR) {
            dateTimeFormatter = yearMonthDayHourDateTimeFormatter;
        } else if (targetGranularity == PetascopeDateTime.Granularity.MINUTE) {
            dateTimeFormatter = yearMonthDayHourMinuteDateTimeFormatter;
        } else if (targetGranularity == PetascopeDateTime.Granularity.SECOND || targetGranularity == PetascopeDateTime.Granularity.MILLISECOND) {
            dateTimeFormatter = yearMonthDayHourMinuteSecondDateTimeFormatter;
        }

        if (dateTimeFormatter == null) {
            throw new PetascopeException(ExceptionCode.NoApplicableCode, "There is no datetime formatter for target granularity. Given: " + targetGranularity);
        }

        // NOTE: e.g. "2022-12-31"/P1M from the output of "2023-01". "P-1D"
        String result = petascopeDateTime.getDateTimeMin().format(dateTimeFormatter)
                        + TIME_AND_GRANULARITY_DELIMITER + getPeriodicityByGranularity(targetGranularity);
        return result;
    }

    /**
     * e.g. "2015-01/P1D" -> "2015-01
     *
     */
    public static String getDateTimeWithoutGranularitySuffix(String dateTimeStr) {
        String result = dateTimeStr.split(TIME_AND_GRANULARITY_DELIMITER)[0];
        return result;
    }

    /**
     *
     * @param dateTimeStr e.g. "2015-01-01T02:00"
     * @param targetGranularity e.g. P1D
     * @return "2015-01-01"
     */
    public static PetascopeDateTime getDateTimeInTargetGranularity(String dateTimeStr, PetascopeDateTime.Granularity targetGranularity) throws PetascopeException {
        PetascopeDateTime tempDateTime = calculateDateTimeStringSubset(dateTimeStr);
        PetascopeDateTime result = calculateDateTimeStringSubset(getDateTimeRepresentationInTargetGranularity(tempDateTime, targetGranularity));
        return result;
    }

    /**
     * OVER $pt t("2015-01":"2015-11":"P3M") - granularity of "2015-01" and "2015-11" is P1M, but it is modified by P3M
     * @param startTime e.g. "2015-01"
     * @param endTime e.g. "2015-11"
     * @param timeStep e.g. "P3M"
     * @return list of time values:
     * [ "2015-01-01T00:00:000" up to "2015-03-31T23:59:59.999",
     *   "2015-04-01T00:00:000" up to "2015-06-30T23:59:59.999",
     *   "2015-07-01T00:00:000" up to "2015-09-30T23:59:59.999",
     *   "2015-10-01T00:00:000" up to "2015-11-30T23:59:59.999" NOT "2015-10-01T00:00:000" up to "2015-12-31T23:59:59.999"m
     *   because the upper bound is "2015-11" (granularity is 1 month)
     * ]
     */
    public static List<PetascopeDateTime> getListOfDateTimes(PetascopeDateTime startTime, PetascopeDateTime endTime, String timeStep) throws PetascopeException {
        if (startTime.getDateTimeMin().compareTo(endTime.getDateTimeMax()) > 0) {
            throw new PetascopeException(ExceptionCode.InvalidRequest,
                                                    "Start time cannot be greater than the end time. " +
                                                    "Given: " + StringUtil.enquoteIfNotEnquotedAlready(startTime.getDateTimeMin().toString())
                                                    + " and "
                                                    + StringUtil.enquoteIfNotEnquotedAlready(endTime.getDateTimeMin().toString())
                                                    + " respectively.");
        }

        List<PetascopeDateTime> results = new ArrayList<>();

        // e.g. startTime = "2015-01-01", "2015-04-01", "2015-07-01", "2015-10-01"
        PetascopeDateTime timeIteration = startTime;
        results.add(timeIteration);

        while (timeIteration.getDateTimeMin().compareTo(endTime.getDateTimeMax()) < 0) {
            // Then, calculate the maxTime with timeStep (e.g. P3M) of each iteration, then subtract by 1 ms
            // here, it is timeMax = timeMin + timeStep (e.g. P3M) - 1 ms
            String targetTimeZone = null;
            timeIteration = calculatePetascopeDateTimeByPeriodicityAndTimeZone(timeIteration.getDateTimeMinISOFormatStr(), timeStep,
                                                                                targetTimeZone, timeIteration.getGranularity());
            if (timeIteration.getDateTimeMin().compareTo(endTime.getDateTimeMax()) > 0) {
                break;
            }
            timeIteration.setGranularity(startTime.getGranularity());
            results.add(timeIteration);
        }

        return results;
    }

    /**
     * Get ISO Datetime format with offset time string (e.g. +02:00)
     */
    public static String toISOFormat(OffsetDateTime offsetDateTime) {
        String result = StringUtil.addQuotesIfNotExists(ISO_DATETIME_WITH_TIME_OFFSET_FORMAT.format(offsetDateTime));
        return result;
    }

    /**
     * e.g. 3 -> 03
     */
    public static String fillLeadingZero(int value) {
        String result = value < 10 ? "0" + value : String.valueOf(value);
        return result;
    }

    /**
     * e.g. 3 -> 003
     */
    public static String fillLeadingTwoZeros(int value) {
        if (value < 10) {
            return "00" + value;
        } else if (value < 100) {
            return "0" + (value);
        }

        return String.valueOf(value);
    }

    /**
     * Get the number of milliseconds fitting in the provided UoM (UCUM
     * abbreviation).
     *
     * @param ucumAbbreviation See c/s symbols in
     * http://unitsofmeasure.org/ucum.html
     * @return How many milliseconds [ms] are in the specified duration.
     * @throws PetascopeException
     */
    private static Long getMillis(String ucumAbbreviation) throws PetascopeException {
        Long millis = timeUomsRegistry.get(ucumAbbreviation);
        if (null == millis) {
            throw new PetascopeException(ExceptionCode.InvalidCoverageConfiguration,
                    "Unsupported temporal Unit of Measure [" + ucumAbbreviation + "].");
        }
        return millis;
    }

    /**
     * Remove quotes from a timestamp, if present.
     *
     * @param timestamp ISO:8601 timestamp (possibly quoted)
     * @return String PSQL/DateTime compatible timestamp
     */
    private static String fix(String timestamp) {
        String unquotedTimestamp;

        unquotedTimestamp = timestamp.replaceAll("%22", "");
        unquotedTimestamp = unquotedTimestamp.replaceAll("'", "");
        unquotedTimestamp = unquotedTimestamp.replaceAll("\"", "");

        return unquotedTimestamp;
    }
}

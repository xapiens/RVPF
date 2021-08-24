/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DateTimeOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.time.ZonedDateTime;

import java.util.Optional;
import java.util.TimeZone;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.Require;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Date-time operations.
 */
public final class DateTimeOperations
    extends SimpleOperations
{
    /** {@inheritDoc}
     */
    @Override
    public void execute(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Task.ExecuteException, Stack.AccessException
    {
        final Stack stack = task.getStack();

        switch ((_Code) reference.getCode()) {
            case ADD_ELAPSED_ELAPSED: {
                _doAddElapsedElapsed(stack);

                break;
            }
            case ADD_ELAPSED_DATE_TIME: {
                _doAddElapsedDateTime(stack);

                break;
            }
            case DATE_TIME_DAY: {
                _doDateTimeDay(stack);

                break;
            }
            case DEC_DAY: {
                _doDecDay(stack);

                break;
            }
            case INC_DAY: {
                _doIncDay(stack);

                break;
            }
            case SUB_DAYS_DATE_TIME: {
                _doSubDaysDateTime(stack);

                break;
            }
            case ADD_DAYS_DATE_TIME: {
                _doAddDaysDateTime(stack);

                break;
            }
            case DATE_TIME_DIM: {
                _doDateTimeDim(stack);

                break;
            }
            case DATE_TIME_DOW: {
                _doDateTimeDow(stack);

                break;
            }
            case DATE_TIME_HOUR: {
                _doDateTimeHour(stack);

                break;
            }
            case DEC_HOUR: {
                _doDecHour(stack);

                break;
            }
            case INC_HOUR: {
                _doIncHour(stack);

                break;
            }
            case FLOOR_HOUR: {
                _doFloorHour(stack);

                break;
            }
            case ROUND_HOUR: {
                _doRoundHour(stack);

                break;
            }
            case ELAPSED_HOURS: {
                _doElapsedHours(stack);

                break;
            }
            case HOURS_ELAPSED: {
                _doHoursElapsed(stack);

                break;
            }
            case DATE_TIME_HOURS: {
                _doDateTimeHours(stack);

                break;
            }
            case MIDNIGHT: {
                _doMidnight(stack);

                break;
            }
            case DATE_TIME_MILLI: {
                _doDateTimeMilli(stack);

                break;
            }
            case DEC_MILLI: {
                _doDecMilli(stack);

                break;
            }
            case INC_MILLI: {
                _doIncMilli(stack);

                break;
            }
            case FLOOR_MILLI: {
                _doFloorMilli(stack);

                break;
            }
            case ROUND_MILLI: {
                _doRoundMilli(stack);

                break;
            }
            case ELAPSED_MILLIS: {
                _doElapsedMillis(stack);

                break;
            }
            case MILLIS_ELAPSED: {
                _doMillisElapsed(stack);

                break;
            }
            case DATE_TIME_MILLIS: {
                _doDateTimeMillis(stack);

                break;
            }
            case DATE_TIME_MINUTE: {
                _doDateTimeMinute(stack);

                break;
            }
            case DEC_MINUTE: {
                _doDecMinute(stack);

                break;
            }
            case INC_MINUTE: {
                _doIncMinute(stack);

                break;
            }
            case FLOOR_MINUTE: {
                _doFloorMinute(stack);

                break;
            }
            case ROUND_MINUTE: {
                _doRoundMinute(stack);

                break;
            }
            case ELAPSED_MINUTES: {
                _doElapsedMinutes(stack);

                break;
            }
            case MINUTES_ELAPSED: {
                _doMinutesElapsed(stack);

                break;
            }
            case DATE_TIME_MINUTES: {
                _doDateTimeMinutes(stack);

                break;
            }
            case ELAPSED_DATE_TIME: {
                _doElapsedDateTime(stack);

                break;
            }
            case LONG_DATE_TIME: {
                _doLongDateTime(stack);

                break;
            }
            case STRING_DATE_TIME: {
                _doStringDateTime(stack);

                break;
            }
            case DATE_TIME_MONTH: {
                _doDateTimeMonth(stack);

                break;
            }
            case DEC_MONTH: {
                _doDecMonth(stack);

                break;
            }
            case INC_MONTH: {
                _doIncMonth(stack);

                break;
            }
            case SUB_MONTHS_DATE_TIME: {
                _doSubMonthsDateTime(stack);

                break;
            }
            case ADD_MONTHS_DATE_TIME: {
                _doAddMonthsDateTime(stack);

                break;
            }
            case NOON: {
                _doNoon(stack);

                break;
            }
            case NOW: {
                _doNow(stack);

                break;
            }
            case ELAPSED_RAW: {
                _doElapsedRaw(stack);

                break;
            }
            case DATE_TIME_RAW: {
                _doDateTimeRaw(stack);

                break;
            }
            case DATE_TIME_SECOND: {
                _doDateTimeSecond(stack);

                break;
            }
            case DEC_SECOND: {
                _doDecSecond(stack);

                break;
            }
            case INC_SECOND: {
                _doIncSecond(stack);

                break;
            }
            case FLOOR_SECOND: {
                _doFloorSecond(stack);

                break;
            }
            case ROUND_SECOND: {
                _doRoundSecond(stack);

                break;
            }
            case ELAPSED_SECONDS: {
                _doElapsedSeconds(stack);

                break;
            }
            case DATE_TIME_SECONDS: {
                _doDateTimeSeconds(stack);

                break;
            }
            case SECONDS_ELAPSED: {
                _doSecondsElapsed(stack);

                break;
            }
            case ELAPSED_SPLIT: {
                _doElapsedSplit(stack);

                break;
            }
            case DATE_TIME_SPLIT: {
                _doDateTimeSplit(stack);

                break;
            }
            case FIELDS_JOIN: {
                _doFieldsJoin(stack);

                break;
            }
            case SUB_ELAPSED_ELAPSED: {
                _doSubElapsedElapsed(stack);

                break;
            }
            case SUB_ELAPSED_DATE_TIME: {
                _doSubElapsedDateTime(stack);

                break;
            }
            case SUB_DATE_TIME_DATE_TIME: {
                _doSubDateTimeDateTime(stack);

                break;
            }
            case TODAY: {
                _doToday(stack);

                break;
            }
            case TOMORROW: {
                _doTomorrow(stack);

                break;
            }
            case DATE_TIME_YEAR: {
                _doDateTimeYear(stack);

                break;
            }
            case DEC_YEAR: {
                _doDecYear(stack);

                break;
            }
            case INC_YEAR: {
                _doIncYear(stack);

                break;
            }
            case SUB_YEARS_DATE_TIME: {
                _doSubYearsDateTime(stack);

                break;
            }
            case ADD_YEARS_DATE_TIME: {
                _doAddYearsDateTime(stack);

                break;
            }
            case YESTERDAY: {
                _doYesterday(stack);

                break;
            }
            case TZ: {
                _doTZ(stack);

                break;
            }
            default: {
                Require.failure();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void setUp()
        throws Operation.OverloadException
    {
        register("-", _Code.SUB_ELAPSED_ELAPSED, BOTH_ELAPSED);
        register("-", _Code.SUB_ELAPSED_DATE_TIME, TOP_ELAPSED_DATE_TIME);
        register("-", _Code.SUB_DATE_TIME_DATE_TIME, BOTH_DATE_TIME);
        register("+", _Code.ADD_ELAPSED_ELAPSED, BOTH_ELAPSED);
        register("+", _Code.ADD_ELAPSED_DATE_TIME, TOP_ELAPSED_DATE_TIME);
        register("day", _Code.DATE_TIME_DAY, TOP_DATE_TIME);
        register("--day", _Code.DEC_DAY, TOP_DATE_TIME);
        register("++day", _Code.INC_DAY, TOP_DATE_TIME);
        register("-days", _Code.SUB_DAYS_DATE_TIME, TOP_LONG_DATE_TIME);
        register("+days", _Code.ADD_DAYS_DATE_TIME, TOP_LONG_DATE_TIME);
        register("dim", _Code.DATE_TIME_DIM, TOP_DATE_TIME);
        register("dow", _Code.DATE_TIME_DOW, TOP_DATE_TIME);
        register("hour", _Code.DATE_TIME_HOUR, TOP_DATE_TIME);
        register("--hour", _Code.DEC_HOUR, TOP_DATE_TIME);
        register("++hour", _Code.INC_HOUR, TOP_DATE_TIME);
        register("_hour", _Code.FLOOR_HOUR, TOP_DATE_TIME);
        register("~hour", _Code.ROUND_HOUR, TOP_DATE_TIME);
        register("hours", _Code.ELAPSED_HOURS, TOP_ELAPSED);
        register("hours", _Code.HOURS_ELAPSED, Filter.TOP_NUMBER);
        register("hours", _Code.DATE_TIME_HOURS, TOP_DATE_TIME);
        register("midnight", _Code.MIDNIGHT, TOP_DATE_TIME);
        register("milli", _Code.DATE_TIME_MILLI, TOP_DATE_TIME);
        register("--milli", _Code.DEC_MILLI, TOP_DATE_TIME);
        register("++milli", _Code.INC_MILLI, TOP_DATE_TIME);
        register("_milli", _Code.FLOOR_MILLI, TOP_DATE_TIME);
        register("~milli", _Code.ROUND_MILLI, TOP_DATE_TIME);
        register("millis", _Code.ELAPSED_MILLIS, TOP_ELAPSED);
        register("millis", _Code.MILLIS_ELAPSED, Filter.TOP_NUMBER);
        register("millis", _Code.DATE_TIME_MILLI, TOP_DATE_TIME);
        register("minute", _Code.DATE_TIME_MINUTE, TOP_DATE_TIME);
        register("--minute", _Code.DEC_MINUTE, TOP_DATE_TIME);
        register("++minute", _Code.INC_MINUTE, TOP_DATE_TIME);
        register("_minute", _Code.FLOOR_MINUTE, TOP_DATE_TIME);
        register("~minute", _Code.ROUND_MINUTE, TOP_DATE_TIME);
        register("minutes", _Code.ELAPSED_MINUTES, TOP_ELAPSED);
        register("minutes", _Code.MINUTES_ELAPSED, Filter.TOP_LONG);
        register("minutes", _Code.DATE_TIME_MINUTES, TOP_DATE_TIME);
        register("mjd", _Code.ELAPSED_DATE_TIME, TOP_ELAPSED);
        register("mjd", _Code.LONG_DATE_TIME, Filter.TOP_LONG);
        register("mjd", _Code.STRING_DATE_TIME, Filter.TOP_STRING);
        register("month", _Code.DATE_TIME_MONTH, TOP_DATE_TIME);
        register("--month", _Code.DEC_MONTH, TOP_DATE_TIME);
        register("++month", _Code.INC_MONTH, TOP_DATE_TIME);
        register("-months", _Code.SUB_MONTHS_DATE_TIME, TOP_LONG_DATE_TIME);
        register("+months", _Code.ADD_MONTHS_DATE_TIME, TOP_LONG_DATE_TIME);
        register("noon", _Code.NOON, TOP_DATE_TIME);
        register("now", _Code.NOW);
        register("raw", _Code.ELAPSED_RAW, TOP_ELAPSED);
        register("raw", _Code.DATE_TIME_RAW, TOP_DATE_TIME);
        register("second", _Code.DATE_TIME_SECOND, TOP_DATE_TIME);
        register("--second", _Code.DEC_SECOND, TOP_DATE_TIME);
        register("++second", _Code.INC_SECOND, TOP_DATE_TIME);
        register("_second", _Code.FLOOR_SECOND, TOP_DATE_TIME);
        register("~second", _Code.ROUND_SECOND, TOP_DATE_TIME);
        register("seconds", _Code.ELAPSED_SECONDS, TOP_ELAPSED);
        register("seconds", _Code.DATE_TIME_SECONDS, TOP_DATE_TIME);
        register("seconds", _Code.SECONDS_ELAPSED, Filter.TOP_NUMBER);
        register("split", _Code.ELAPSED_SPLIT, TOP_ELAPSED);
        register("split", _Code.DATE_TIME_SPLIT, TOP_DATE_TIME);
        register("join", _Code.FIELDS_JOIN, TOP_FIELDS);
        register("today", _Code.TODAY);
        register("tomorrow", _Code.TOMORROW);
        register("year", _Code.DATE_TIME_YEAR, TOP_DATE_TIME);
        register("--year", _Code.DEC_YEAR, TOP_DATE_TIME);
        register("++year", _Code.INC_YEAR, TOP_DATE_TIME);
        register("-years", _Code.SUB_YEARS_DATE_TIME, TOP_LONG_DATE_TIME);
        register("+years", _Code.ADD_YEARS_DATE_TIME, TOP_LONG_DATE_TIME);
        register("yesterday", _Code.YESTERDAY);
        register("tz", _Code.TZ);
    }

    private static void _doAddDaysDateTime(
            final Stack stack)
        throws Stack.AccessException
    {
        final int days = stack.popIntValue();
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .plusYears(days);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doAddElapsedDateTime(
            final Stack stack)
        throws Stack.AccessException
    {
        final ElapsedTime elapsed = _popElapsedValue(stack);
        final DateTime stamp = _popDateTimeValue(stack);

        stack.push(stamp.after(elapsed));
    }

    private static void _doAddElapsedElapsed(
            final Stack stack)
        throws Stack.AccessException
    {
        final ElapsedTime y = _popElapsedValue(stack);
        final ElapsedTime x = _popElapsedValue(stack);

        stack.push(x.add(y));
    }

    private static void _doAddMonthsDateTime(
            final Stack stack)
        throws Stack.AccessException
    {
        final int months = stack.popIntValue();
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .plusYears(months);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doAddYearsDateTime(
            final Stack stack)
        throws Stack.AccessException
    {
        final int years = stack.popIntValue();
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .plusYears(years);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doDateTimeDay(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popZonedDateTime(stack).getDayOfMonth());
    }

    private static void _doDateTimeDim(
            final Stack stack)
        throws Stack.AccessException
    {
        stack
            .push(
                _getDateTimeContext(stack)
                    .getDaysInMonth(_popDateTimeValue(stack)));
    }

    private static void _doDateTimeDow(
            final Stack stack)
        throws Stack.AccessException
    {
        stack
            .push(
                _getDateTimeContext(stack)
                    .getDayOfWeek(_popDateTimeValue(stack)));
    }

    private static void _doDateTimeHour(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popZonedDateTime(stack).getHour());
    }

    private static void _doDateTimeHours(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).scaled(ElapsedTime.HOUR));
    }

    private static void _doDateTimeMilli(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popZonedDateTime(stack).getNano() / 1_000_000);
    }

    private static void _doDateTimeMillis(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).scaled(ElapsedTime.MILLI));
    }

    private static void _doDateTimeMinute(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popZonedDateTime(stack).getMinute());
    }

    private static void _doDateTimeMinutes(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).scaled(ElapsedTime.MINUTE));
    }

    private static void _doDateTimeMonth(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popZonedDateTime(stack).getMonthValue());
    }

    private static void _doDateTimeRaw(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).toRaw());
    }

    private static void _doDateTimeSecond(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack);

        stack
            .push(zonedDateTime.getSecond()
                  + (((double) zonedDateTime.getNano()) / 1_000_000_000));
    }

    private static void _doDateTimeSeconds(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).scaled(ElapsedTime.SECOND));
    }

    private static void _doDateTimeSplit(
            final Stack stack)
        throws Stack.AccessException
    {
        final DateTime.Fields fields = _getDateTimeContext(stack)
            .toFields(_popDateTimeValue(stack));

        stack.push(fields.year);
        stack.push(fields.month);
        stack.push(fields.day);
        stack.push(fields.hour);
        stack.push(fields.minute);
        stack.push(fields.second + (((double) fields.nano) / 1_000_000_000));
    }

    private static void _doDateTimeYear(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popZonedDateTime(stack).getYear());
    }

    private static void _doDecDay(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .minusDays(1);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doDecHour(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .minusHours(1);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doDecMilli(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .minusNanos(1_000_000);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doDecMinute(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .minusMinutes(1);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doDecMonth(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .minusMonths(1);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doDecSecond(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .minusSeconds(1);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doDecYear(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .minusYears(1);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doElapsedDateTime(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(DateTime.MJD_EPOCH.after(_popElapsedValue(stack)));
    }

    private static void _doElapsedHours(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popElapsedValue(stack).scaled(ElapsedTime.HOUR.toRaw()));
    }

    private static void _doElapsedMillis(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popElapsedValue(stack).scaled(ElapsedTime.MILLI.toRaw()));
    }

    private static void _doElapsedMinutes(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popElapsedValue(stack).scaled(ElapsedTime.MINUTE.toRaw()));
    }

    private static void _doElapsedRaw(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popElapsedValue(stack).toRaw());
    }

    private static void _doElapsedSeconds(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popElapsedValue(stack).scaled(ElapsedTime.SECOND.toRaw()));
    }

    private static void _doElapsedSplit(
            final Stack stack)
        throws Stack.AccessException
    {
        final ElapsedTime elapsed = _popElapsedValue(stack);

        stack.push((long) elapsed.scaled(24 * ElapsedTime.HOUR.toRaw()));
        stack.push((long) elapsed.scaled(ElapsedTime.HOUR.toRaw()));
        stack.push((long) elapsed.scaled(ElapsedTime.MINUTE.toRaw()));
        stack.push(elapsed.scaled(ElapsedTime.SECOND.toRaw()));
    }

    private static void _doFieldsJoin(
            final Stack stack)
        throws Stack.AccessException
    {
        final DateTime.Fields fields = new DateTime.Fields();
        final double seconds = stack.popDoubleValue();

        fields.second = (int) seconds;
        fields.nano = (int) ((seconds - fields.second) * 1_000_000_000);
        fields.minute = stack.popIntValue();
        fields.hour = stack.popIntValue();
        fields.day = stack.popIntValue();
        fields.month = stack.popIntValue();
        fields.year = stack.popIntValue();
        stack.push(_getDateTimeContext(stack).fromFields(fields));
    }

    private static void _doFloorHour(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).floored(ElapsedTime.HOUR));
    }

    private static void _doFloorMilli(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).floored(ElapsedTime.MILLI));
    }

    private static void _doFloorMinute(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).floored(ElapsedTime.MINUTE));
    }

    private static void _doFloorSecond(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).floored(ElapsedTime.SECOND));
    }

    private static void _doHoursElapsed(
            final Stack stack)
        throws Stack.AccessException
    {
        stack
            .push(
                ElapsedTime
                    .fromRaw((long) (stack.popDoubleValue()
                    * ElapsedTime.HOUR.toRaw())));
    }

    private static void _doIncDay(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .plusDays(1);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doIncHour(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .plusHours(1);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doIncMilli(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .plusNanos(1_000_000);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doIncMinute(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .plusMinutes(1);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doIncMonth(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .plusMonths(1);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doIncSecond(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .plusSeconds(1);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doIncYear(
            final Stack stack)
        throws Stack.AccessException
    {
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .plusYears(1);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doLongDateTime(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(DateTime.fromRaw(stack.popLongValue()));
    }

    private static void _doMidnight(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).midnight());
    }

    private static void _doMillisElapsed(
            final Stack stack)
        throws Stack.AccessException
    {
        stack
            .push(
                ElapsedTime
                    .fromRaw((long) (stack.popDoubleValue()
                    * ElapsedTime.MILLI.toRaw())));
    }

    private static void _doMinutesElapsed(
            final Stack stack)
        throws Stack.AccessException
    {
        stack
            .push(
                ElapsedTime
                    .fromRaw((long) (stack.popDoubleValue()
                    * ElapsedTime.MINUTE.toRaw())));
    }

    private static void _doNoon(final Stack stack)
    {
        final DateTime.Context dateTimeContext = _getDateTimeContext(stack);
        final ZonedDateTime zonedDateTime = dateTimeContext
            .toZonedDateTime(dateTimeContext.midnight(DateTime.now()));

        stack.push(DateTime.fromZonedDateTime(zonedDateTime.withHour(12)));
    }

    private static void _doNow(final Stack stack)
    {
        stack.push(DateTime.now());
    }

    private static void _doRoundHour(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).rounded(ElapsedTime.HOUR));
    }

    private static void _doRoundMilli(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).rounded(ElapsedTime.MILLI));
    }

    private static void _doRoundMinute(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).rounded(ElapsedTime.MINUTE));
    }

    private static void _doRoundSecond(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(_popDateTimeValue(stack).rounded(ElapsedTime.SECOND));
    }

    private static void _doSecondsElapsed(
            final Stack stack)
        throws Stack.AccessException
    {
        stack
            .push(
                ElapsedTime
                    .fromRaw((long) (stack.popDoubleValue()
                    * ElapsedTime.SECOND.toRaw())));
    }

    private static void _doStringDateTime(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final String string = stack.popStringValue();

        try {
            stack.push(_getDateTimeContext(stack).fromString(string));
        } catch (final IllegalArgumentException exception) {
            fail(ProcessorMessages.DATE_TIME_FORMAT, string);
        }
    }

    private static void _doSubDateTimeDateTime(
            final Stack stack)
        throws Stack.AccessException
    {
        final DateTime y = _popDateTimeValue(stack);
        final DateTime x = _popDateTimeValue(stack);

        stack.push(x.sub(y));
    }

    private static void _doSubDaysDateTime(
            final Stack stack)
        throws Stack.AccessException
    {
        final int days = stack.popIntValue();
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .minusYears(days);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doSubElapsedDateTime(
            final Stack stack)
        throws Stack.AccessException
    {
        final ElapsedTime y = _popElapsedValue(stack);
        final DateTime x = _popDateTimeValue(stack);

        stack.push(x.before(y));
    }

    private static void _doSubElapsedElapsed(
            final Stack stack)
        throws Stack.AccessException
    {
        final ElapsedTime y = _popElapsedValue(stack);
        final ElapsedTime x = _popElapsedValue(stack);

        stack.push(x.sub(y));
    }

    private static void _doSubMonthsDateTime(
            final Stack stack)
        throws Stack.AccessException
    {
        final int months = stack.popIntValue();
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .minusYears(months);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doSubYearsDateTime(
            final Stack stack)
        throws Stack.AccessException
    {
        final int years = stack.popIntValue();
        final ZonedDateTime zonedDateTime = _popZonedDateTime(stack)
            .minusYears(years);

        stack.push(DateTime.fromZonedDateTime(zonedDateTime));
    }

    private static void _doTZ(final Stack stack)
        throws Stack.AccessException
    {
        final Optional<TimeZone> timeZone;

        if (stack.peek() != null) {
            timeZone = Optional
                .of(TimeZone.getTimeZone(stack.popStringValue()));
        } else {
            stack.pop();
            timeZone = Optional.empty();
        }

        stack.getTask().getContext().setTimeZone(timeZone);
    }

    private static void _doToday(final Stack stack)
    {
        final DateTime.Context dateTimeContext = _getDateTimeContext(stack);

        stack.push(dateTimeContext.midnight(DateTime.now()));
    }

    private static void _doTomorrow(final Stack stack)
    {
        final DateTime.Context dateTimeContext = _getDateTimeContext(stack);
        final ZonedDateTime zonedDateTime = dateTimeContext
            .toZonedDateTime(dateTimeContext.midnight(DateTime.now()));

        stack.push(DateTime.fromZonedDateTime(zonedDateTime.plusDays(1)));
    }

    private static void _doYesterday(final Stack stack)
    {
        final DateTime.Context dateTimeContext = _getDateTimeContext(stack);
        final ZonedDateTime zonedDateTime = dateTimeContext
            .toZonedDateTime(dateTimeContext.midnight(DateTime.now()));

        stack.push(DateTime.fromZonedDateTime(zonedDateTime.minusDays(1)));
    }

    private static DateTime.Context _getDateTimeContext(final Stack stack)
    {
        return stack.getTask().getContext().getDateTimeContext();
    }

    private static DateTime _popDateTimeValue(
            final Stack stack)
        throws Stack.AccessException
    {
        return (DateTime) stack.pop(DateTime.class);
    }

    private static ElapsedTime _popElapsedValue(
            final Stack stack)
        throws Stack.AccessException
    {
        return (ElapsedTime) stack.pop(ElapsedTime.class);
    }

    private static ZonedDateTime _popZonedDateTime(
            final Stack stack)
        throws Stack.AccessException
    {
        return _getDateTimeContext(stack)
            .toZonedDateTime((DateTime) stack.pop(DateTime.class));
    }

    /** Both must be an ElapsedTime. */
    public static final Filter BOTH_ELAPSED = new Filter()
        .is(0, ElapsedTime.class)
        .is(1, ElapsedTime.class)
        .and();

    /** Both must be DateTimes. */
    public static final Filter BOTH_DATE_TIME = new Filter()
        .is(0, DateTime.class)
        .is(1, DateTime.class)
        .and();

    /** Top must be a DateTime. */
    public static final Filter TOP_DATE_TIME = new Filter()
        .is(0, DateTime.class);

    /** Top must be an ElapsedTime. */
    public static final Filter TOP_ELAPSED = new Filter()
        .is(0, ElapsedTime.class);

    /** Top must be DateTime fields. */
    public static final Filter TOP_FIELDS = new Filter()
        .is(0, Number.class)
        .isLong(1)
        .and()
        .isLong(2)
        .and()
        .isLong(3)
        .and()
        .isLong(4)
        .and()
        .isLong(5)
        .and();

    /** Top must be an ElapsedTime over a DateTime. */
    public static final Filter TOP_ELAPSED_DATE_TIME = new Filter()
        .is(0, ElapsedTime.class)
        .is(1, DateTime.class)
        .and();

    /** Top must be an Integer over a DateTime. */
    public static final Filter TOP_LONG_DATE_TIME = new Filter()
        .isLong(0)
        .is(1, DateTime.class)
        .and();

    /**
     * Code.
     */
    private enum _Code
    {
        ADD_ELAPSED_ELAPSED,
        ADD_ELAPSED_DATE_TIME,
        DATE_TIME_DAY,
        DEC_DAY,
        INC_DAY,
        SUB_DAYS_DATE_TIME,
        ADD_DAYS_DATE_TIME,
        DATE_TIME_DIM,
        DATE_TIME_DOW,
        DATE_TIME_HOUR,
        DEC_HOUR,
        INC_HOUR,
        FLOOR_HOUR,
        ROUND_HOUR,
        ELAPSED_HOURS,
        HOURS_ELAPSED,
        DATE_TIME_HOURS,
        MIDNIGHT,
        DATE_TIME_MILLI,
        DEC_MILLI,
        INC_MILLI,
        FLOOR_MILLI,
        ROUND_MILLI,
        DATE_TIME_MILLIS,
        ELAPSED_MILLIS,
        MILLIS_ELAPSED,
        DATE_TIME_MINUTE,
        DEC_MINUTE,
        INC_MINUTE,
        FLOOR_MINUTE,
        ROUND_MINUTE,
        ELAPSED_MINUTES,
        DATE_TIME_MINUTES,
        MINUTES_ELAPSED,
        ELAPSED_DATE_TIME,
        LONG_DATE_TIME,
        DATE_TIME_MONTH,
        DEC_MONTH,
        INC_MONTH,
        SUB_MONTHS_DATE_TIME,
        ADD_MONTHS_DATE_TIME,
        NOON,
        NOW,
        ELAPSED_RAW,
        DATE_TIME_RAW,
        STRING_DATE_TIME,
        DATE_TIME_SECOND,
        DEC_SECOND,
        INC_SECOND,
        FLOOR_SECOND,
        ROUND_SECOND,
        DATE_TIME_SECONDS,
        ELAPSED_SECONDS,
        SECONDS_ELAPSED,
        ELAPSED_SPLIT,
        DATE_TIME_SPLIT,
        FIELDS_JOIN,
        SUB_ELAPSED_ELAPSED,
        SUB_ELAPSED_DATE_TIME,
        SUB_DATE_TIME_DATE_TIME,
        TODAY,
        TOMORROW,
        DATE_TIME_YEAR,
        DEC_YEAR,
        INC_YEAR,
        SUB_YEARS_DATE_TIME,
        ADD_YEARS_DATE_TIME,
        YESTERDAY,
        TZ
    }
}

/* This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA
 */

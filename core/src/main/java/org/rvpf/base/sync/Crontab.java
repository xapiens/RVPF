/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Crontab.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.sync;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.DateTime;
import org.rvpf.base.tool.Require;

/**
 * Crontab.
 */
@Immutable
public final class Crontab
{
    /**
     * Constructs an instance.
     *
     * @param minutes
     * @param hours
     * @param days
     * @param months
     * @param daysOfWeek
     */
    private Crontab(
            final String entry,
            final boolean[] minutes,
            final boolean[] hours,
            final boolean[] days,
            final boolean[] months,
            final boolean[] daysOfWeek)
    {
        _entry = entry;
        _minutes = minutes;
        _hours = hours;
        _days = days;
        _months = months;
        _daysOfWeek = daysOfWeek;
    }

    /**
     * Parses a crontab entry.
     *
     * @param entry The crontab entry.
     *
     * @return A filled crontab object.
     *
     * @throws BadItemException When there is a bad item in the entry.
     */
    @Nonnull
    @CheckReturnValue
    public static Crontab parse(
            @Nonnull final String entry)
        throws BadItemException
    {
        final String[] lists = _CRONTAB_SPLITTER.split(entry.trim());
        final boolean[] minutes = _parseList(
            _getList(lists, _MINUTES_INDEX),
            0,
            _MINUTES_IN_HOUR - 1,
            false);
        final boolean[] hours = _parseList(
            _getList(lists, _HOURS_INDEX),
            0,
            _HOURS_IN_DAY - 1,
            false);
        final boolean[] days = _parseList(
            _getList(lists, _DAYS_INDEX),
            1,
            _DAYS_IN_MONTH,
            false);
        final boolean[] months = _parseList(
            _getList(lists, _MONTHS_INDEX),
            1,
            _MONTHS_IN_YEAR,
            false);
        final boolean[] daysOfWeek = _parseList(
            _getList(lists, _DAYS_OF_WEEK_INDEX),
            0,
            _DAYS_IN_WEEK - 1,
            true);

        if ((lists.length <= _DAYS_OF_WEEK_INDEX)
                || lists[_DAYS_OF_WEEK_INDEX].equals(_ALL_VALUES)) {
            Arrays.fill(daysOfWeek, false);
        } else if (lists[_DAYS_INDEX].equals(_ALL_VALUES)) {
            Arrays.fill(days, false);
        }

        return new Crontab(entry, minutes, hours, days, months, daysOfWeek);
    }

    /**
     * Reads in an external representation of a crontab.
     *
     * <p>This is a helper method for
     * {@link java.io.Externalizable Externalizable} objects containing a
     * Crontab.</p>
     *
     * @param input The external representation.
     *
     * @return The crontab.
     *
     * @throws IOException When an I/O error occurs.
     */
    @Nonnull
    @CheckReturnValue
    public static Crontab readExternal(
            @Nonnull final ObjectInput input)
        throws IOException
    {
        final String entry = input.readUTF();
        final boolean[] minutes = _readBooleans(_MINUTES_IN_HOUR, input);
        final boolean[] hours = _readBooleans(_HOURS_IN_DAY, input);
        final boolean[] days = _readBooleans(_DAYS_IN_MONTH, input);
        final boolean[] months = _readBooleans(_MONTHS_IN_YEAR, input);
        final boolean[] daysOfWeek = _readBooleans(_DAYS_IN_WEEK, input);

        return new Crontab(entry, minutes, hours, days, months, daysOfWeek);
    }

    /**
     * Writes out an external representation of a crontab.
     *
     * <p>This is a helper method for
     * {@link java.io.Externalizable Externalizable} objects containing a
     * crontab.</p>
     *
     * @param crontab The crontab.
     * @param output The external representation.
     *
     * @throws IOException When an I/O error occurs.
     */
    public static void writeExternal(
            @Nonnull final Crontab crontab,
            @Nonnull final ObjectOutput output)
        throws IOException
    {
        output.writeUTF(crontab._entry);
        _writeBooleans(crontab._minutes, output);
        _writeBooleans(crontab._hours, output);
        _writeBooleans(crontab._days, output);
        _writeBooleans(crontab._months, output);
        _writeBooleans(crontab._daysOfWeek, output);
    }

    /**
     * Changes to the next (or previous) time.
     *
     * @param dateTime The current date-time.
     * @param zoneId The zone id.
     * @param forward True for next (otherwise previous).
     *
     * @return The new date-time.
     */
    public DateTime change(
            @Nonnull final DateTime dateTime,
            @Nonnull final ZoneId zoneId,
            final boolean forward)
    {
        final _Pad pad = new _Pad(dateTime, zoneId, forward);

        if (!_settle(pad)) {
            _changeMinute(pad);
        }

        return pad.getDateTime();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof Crontab)) {
            return false;
        }

        final Crontab otherCrontab = (Crontab) other;

        return Arrays.equals(_days, otherCrontab._days)
               && Arrays.equals(_daysOfWeek, otherCrontab._daysOfWeek)
               && Arrays.equals(_hours, otherCrontab._hours)
               && Arrays.equals(_minutes, otherCrontab._minutes)
               && Arrays.equals(_months, otherCrontab._months);
    }

    /**
     * Gets the days.
     *
     * @return The days.
     */
    @Nonnull
    @CheckReturnValue
    public boolean[] getDays()
    {
        return _days;
    }

    /**
     * Gets the days of week.
     *
     * @return The days of week.
     */
    @Nonnull
    @CheckReturnValue
    public boolean[] getDaysOfWeek()
    {
        return _daysOfWeek;
    }

    /**
     * Gets the entry.
     *
     * @return The entry.
     */
    @Nonnull
    @CheckReturnValue
    public String getEntry()
    {
        return _entry;
    }

    /**
     * Gets the hours.
     *
     * @return The hours.
     */
    @Nonnull
    @CheckReturnValue
    public boolean[] getHours()
    {
        return _hours;
    }

    /**
     * Gets the minutes.
     *
     * @return The minutes.
     */
    @Nonnull
    @CheckReturnValue
    public boolean[] getMinutes()
    {
        return _minutes;
    }

    /**
     * Gets the months.
     *
     * @return The months.
     */
    @Nonnull
    @CheckReturnValue
    public boolean[] getMonths()
    {
        return _months;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(_days, _daysOfWeek, _hours, _minutes, _months);
    }

    /**
     * Asks if the date-time is in schedule.
     *
     * @param dateTime The current date-time.
     * @param zoneId The zone id.
     *
     * @return True if the date-time is in schedule.
     */
    @CheckReturnValue
    public boolean isInSchedule(
            @Nonnull final DateTime dateTime,
            @Nonnull final ZoneId zoneId)
    {
        final DateTime.Fields dateTimeFields = new DateTime.Context(zoneId)
            .toFields(dateTime);

        return (dateTimeFields.second == 0)
               && (dateTimeFields.nano == 0)
               && _minutes[dateTimeFields.minute]
               && _hours[dateTimeFields.hour]
               && _months[dateTimeFields.month - 1]
               && _isDaySet(
                   LocalDate.of(
                           dateTimeFields.year,
                                   dateTimeFields.month,
                                   dateTimeFields.day));
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getClass()
            .getSimpleName() + "@" + Integer.toHexString(
                System.identityHashCode(this));
    }

    private static void _changeYear(final _Pad pad)
    {
        pad.changeYear();
    }

    private static String _getList(final String[] lists, final int index)
    {
        if (index < lists.length) {
            return lists[index];
        }

        return _ALL_VALUES;
    }

    private static void _parseItem(
            final String item,
            final int origin,
            final boolean[] mask,
            final boolean wrap)
        throws BadItemException
    {
        final Matcher matcher = _ITEM_PATTERN.matcher(item);
        final int length = mask.length;
        int begin = origin;
        int end = (begin + length) - 1;
        int step = 1;

        if (!matcher.matches()) {
            throw new BadItemException(item);
        }

        if (!"*".equals(matcher.group(_RANGE_GROUP))) {
            begin = Integer.parseInt(matcher.group(_BEGIN_GROUP));

            if (matcher.group(_END_GROUP) != null) {
                end = Integer.parseInt(matcher.group(_END_GROUP));
            } else if (matcher.group(_STEP_GROUP) == null) {
                end = begin;
            }
        }

        if (matcher.group(_STEP_GROUP) != null) {
            step = Integer.parseInt(matcher.group(_STEP_GROUP));
        }

        begin -= origin;

        if (wrap && (begin == length)) {
            begin = 0;
        }

        end -= origin;

        if ((begin > end)
                || (begin < 0)
                || ((end == length) && !wrap)
                || (end > length)
                || (step <= 0)
                || (step >= length)) {
            throw new BadItemException(item);
        }

        for (int i = begin; i <= end; i += step) {
            mask[(i < length)? i: 0] = true;
        }
    }

    private static boolean[] _parseList(final String list, final int origin, final int limit, final boolean wrap) throws BadItemException
    {
        final String[] items = _LIST_SPLITTER.split(list);
        final boolean[] mask = new boolean[(1 + limit) - origin];

        for (String item: items) {
            if (item.isEmpty()) {
                item = _ALL_VALUES;
            }

            _parseItem(item, origin, mask, wrap);
        }

        return mask;
    }

    private static boolean[] _readBooleans(
            final int length,
            final ObjectInput input)
        throws IOException
    {
        final boolean[] items = new boolean[length];

        for (int i = 0; i < length; ++i) {
            items[i] = input.readBoolean();
        }

        return items;
    }

    private static void _writeBooleans(
            final boolean[] items,
            final ObjectOutput output)
        throws IOException
    {
        for (final boolean item: items) {
            output.writeBoolean(item);
        }
    }

    private void _changeDay(final _Pad pad)
    {
        final int daysInMonth = pad.getDaysInMonth();
        int day = pad.getDay();
        int dayOfWeek = pad.getDayOfWeek();

        Require.success(daysInMonth <= _days.length);

        if (pad.isForward()) {
            while (++day < daysInMonth) {
                if (++dayOfWeek >= _daysOfWeek.length) {
                    dayOfWeek = 0;
                }

                pad.changeDay();

                if (_isDaySet(pad.getLocalDate())) {
                    _settleMonth(pad);

                    return;
                }
            }
        } else {
            while (--day >= 0) {
                if (--dayOfWeek < 0) {
                    dayOfWeek = _daysOfWeek.length - 1;
                }

                pad.changeDay();

                if (_days[day] || _daysOfWeek[dayOfWeek]) {
                    _settleMonth(pad);

                    return;
                }
            }
        }

        _changeMonth(pad);
        pad.resetDay();
        _settleDay(pad);
    }

    private void _changeHour(final _Pad pad)
    {
        int hour = pad.getHour();

        if (pad.isForward()) {
            while (++hour < _hours.length) {
                pad.changeHour();

                if (_hours[hour]) {
                    _settleDay(pad);

                    return;
                }
            }
        } else {
            while (--hour >= 0) {
                pad.changeHour();

                if (_hours[hour]) {
                    _settleDay(pad);

                    return;
                }
            }
        }

        _changeDay(pad);
        pad.resetHour();
        _settleHour(pad);
    }

    private void _changeMinute(final _Pad pad)
    {
        final int hour = pad.getHour();
        int minute = pad.getMinute();

        if (pad.isForward()) {
            while (++minute < _minutes.length) {
                pad.changeMinute();

                if (pad.getHour() != hour) {
                    pad.resetMinute();
                    _settleHour(pad);
                    _settleMinute(pad);

                    return;
                }

                if (_minutes[minute]) {
                    _settleHour(pad);

                    return;
                }
            }
        } else {
            while (--minute >= 0) {
                pad.changeMinute();

                if (pad.getHour() != hour) {
                    _settleHour(pad);
                    _settleMinute(pad);

                    return;
                }

                if (_minutes[minute]) {
                    _settleHour(pad);

                    return;
                }
            }
        }

        pad.resetMinute();
        _changeHour(pad);
        _settleMinute(pad);
    }

    private void _changeMonth(final _Pad pad)
    {
        int month = pad.getMonth();

        if (pad.isForward()) {
            while (++month <= _months.length) {
                pad.changeMonth();

                if (_months[month - 1]) {
                    return;
                }
            }
        } else {
            while (--month > 0) {
                pad.changeMonth();

                if (_months[month - 1]) {
                    return;
                }
            }
        }

        _changeYear(pad);
        pad.resetMonth();
        _settleMonth(pad);
    }

    private boolean _isDaySet(final LocalDate localDate)
    {
        final int dayOfMonth = localDate.getDayOfMonth();

        if (_days[dayOfMonth - 1]
                || _daysOfWeek[localDate.getDayOfWeek().getValue() % 7]) {
            return true;
        }

        if (dayOfMonth == localDate.lengthOfMonth()) {
            for (int i = dayOfMonth; i < _days.length; ++i) {
                if (_days[i]) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean _settle(final _Pad pad)
    {
        boolean settled = false;

        if (((pad.getNanosecond() > 0) || (pad.getSecond() > 0))) {
            pad.setSecond(0);

            if (pad.isForward()) {
                pad.changeMinute();
            }

            settled = true;
        }

        settled |= _settleMonth(pad);
        settled |= _settleDay(pad);
        settled |= _settleHour(pad);
        settled |= _settleMinute(pad);

        return settled;
    }

    private boolean _settleDay(final _Pad pad)
    {
        if (_isDaySet(pad.getLocalDate())) {
            return false;
        }

        _changeDay(pad);

        return true;
    }

    private boolean _settleHour(final _Pad pad)
    {
        if (_hours[pad.getHour()]) {
            return false;
        }

        _changeHour(pad);

        return true;
    }

    private boolean _settleMinute(final _Pad pad)
    {
        if (_minutes[pad.getMinute()]) {
            return false;
        }

        _changeMinute(pad);

        return true;
    }

    private boolean _settleMonth(final _Pad pad)
    {
        if (_months[pad.getMonth() - 1]) {
            return false;
        }

        _changeMonth(pad);

        return true;
    }

    private static final String _ALL_VALUES = "*";
    private static final int _BEGIN_GROUP = 2;
    private static final Pattern _CRONTAB_SPLITTER = Pattern.compile("\\s");
    private static final int _DAYS_INDEX = 2;
    private static final int _DAYS_IN_MONTH = 31;
    private static final int _DAYS_IN_WEEK = 7;
    private static final int _DAYS_OF_WEEK_INDEX = 4;
    private static final int _END_GROUP = 3;
    private static final int _HOURS_INDEX = 1;
    private static final int _HOURS_IN_DAY = 24;
    private static final Pattern _ITEM_PATTERN = Pattern
        .compile("(\\*+|(\\d++)(?:-(\\d++))?)(?:/(\\d+))?");
    private static final Pattern _LIST_SPLITTER = Pattern.compile(",");
    private static final int _MINUTES_INDEX = 0;
    private static final int _MINUTES_IN_HOUR = 60;
    private static final int _MONTHS_INDEX = 3;
    private static final int _MONTHS_IN_YEAR = 12;
    private static final int _RANGE_GROUP = 1;
    private static final int _STEP_GROUP = 4;

    private final boolean[] _days;
    private final boolean[] _daysOfWeek;
    private final String _entry;
    private final boolean[] _hours;
    private final boolean[] _minutes;
    private final boolean[] _months;

    /**
     * Bad item exception.
     */
    public static final class BadItemException
        extends Exception
    {
        /**
         * Creates an instance.
         *
         * @param item The 'crontab' item.
         */
        public BadItemException(final String item)
        {
            super(item);
        }

        private static final long serialVersionUID = 1L;
    }


    /**
     * Pad.
     */
    private static final class _Pad
    {
        /**
         * Constructs an instance.
         *
         * @param reference The reference date-time.
         * @param zoneId The zone id.
         * @param forward True for next (otherwise previous).
         */
        _Pad(
                final DateTime reference,
                final ZoneId zoneId,
                final boolean forward)
        {
            _currentDateTime = ZonedDateTime
                .ofInstant(reference.toInstant(), zoneId);
            _forward = forward;
        }

        /**
         * Changes the current day.
         */
        void changeDay()
        {
            _currentDateTime = _currentDateTime.plusDays(_forward? +1: -1);
        }

        /**
         * Changes the current hour.
         */
        void changeHour()
        {
            _currentDateTime = _currentDateTime.plusHours(_forward? +1: -1);
        }

        /**
         * Changes the current minute.
         */
        void changeMinute()
        {
            _currentDateTime = _currentDateTime.plusMinutes(_forward? +1: -1);
        }

        /**
         * Changes the current month.
         */
        void changeMonth()
        {
            _currentDateTime = _currentDateTime.plusMonths(_forward? +1: -1);
        }

        /**
         * Changes the current year.
         */
        void changeYear()
        {
            _currentDateTime = _currentDateTime.plusYears(_forward? +1: -1);
        }

        /**
         * Gets the current date-time.
         *
         * @return The current date-time.
         */
        DateTime getDateTime()
        {
            return DateTime.fromInstant(_currentDateTime.toInstant());
        }

        /**
         * Gets the day in the current month.
         *
         * @return The day in the current month.
         */
        int getDay()
        {
            return _currentDateTime.getDayOfMonth();
        }

        /**
         * Gets the day in the current week.
         *
         * @return The day in the current week (Sunday is 0).
         */
        int getDayOfWeek()
        {
            return _currentDateTime.getDayOfWeek().getValue() % 7;
        }

        /**
         * Gets the number of days in the current month.
         *
         * @return The number of days in the current month.
         */
        int getDaysInMonth()
        {
            return _currentDateTime.toLocalDate().lengthOfMonth();
        }

        /**
         * Gets the current hour.
         *
         * @return The current hour.
         */
        int getHour()
        {
            return _currentDateTime.getHour();
        }

        /**
         * Gets the local date.
         *
         * @return The local date.
         */
        LocalDate getLocalDate()
        {
            return _currentDateTime.toLocalDate();
        }

        /**
         * Gets the current minute.
         *
         * @return The current minute.
         */
        int getMinute()
        {
            return _currentDateTime.getMinute();
        }

        /**
         * Gets the current month.
         *
         * @return The current month.
         */
        int getMonth()
        {
            return _currentDateTime.getMonthValue();
        }

        /**
         * Gets the current nanosecond.
         *
         * @return The current nanosecond.
         */
        int getNanosecond()
        {
            return _currentDateTime.getNano();
        }

        /**
         * Gets the second.
         *
         * @return The second.
         */
        int getSecond()
        {
            return _currentDateTime.getSecond();
        }

        /**
         * Gets the forward indicator.
         *
         * @return The forward indicator.
         */
        boolean isForward()
        {
            return _forward;
        }

        /**
         * Resets the current day.
         */
        void resetDay()
        {
            _currentDateTime = _currentDateTime
                .withDayOfMonth(_forward? 1: getDaysInMonth());
        }

        /**
         * Resets the current hour.
         */
        void resetHour()
        {
            _currentDateTime = _currentDateTime
                .withHour(_forward? 0: _HOURS_IN_DAY - 1);
        }

        /**
         * Resets the current minute.
         */
        void resetMinute()
        {
            _currentDateTime = _currentDateTime
                .withMinute(_forward? 0: _MINUTES_IN_HOUR - 1);
        }

        /**
         * Resets the current month.
         */
        void resetMonth()
        {
            _currentDateTime = _currentDateTime
                .withMonth(_forward? 1: _MONTHS_IN_YEAR);
        }

        /**
         * Sets the current second.
         *
         * @param second The new current second.
         */
        void setSecond(final int second)
        {
            _currentDateTime = _currentDateTime.withSecond(second);
            _currentDateTime = _currentDateTime.withNano(0);
        }

        private ZonedDateTime _currentDateTime;
        private final boolean _forward;
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

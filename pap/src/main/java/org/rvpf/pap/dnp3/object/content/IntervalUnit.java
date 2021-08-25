/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: IntervalUnit.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.content;

import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.pap.EnumCode;

/**
 * IntervalUnit.
 */
public enum IntervalUnit
    implements EnumCode
{
    NONE(0, "Action Not Repeated"),
    MILLISECONDS(1, "Milliseconds"),
    SECONDS(2, "Seconds"),
    MINUTES(3, "Minutes"),
    HOURS(4, "Hours"),
    DAYS(5, "Days"),
    WEEKS(6, "Weeks"),
    MONTHS(7, "Months"),
    MONTHS_SAME_DOW_FROM_SOM(8,
            "Months on Same Day of Week from Start of Month"),
    MONTHS_SAME_DOW_FROM_EOM(9, "Months on Same Day of Week from End of Month"),
    SEASONS(10, "Seasons");

    /**
     * Constructs an instance.
     *
     * @param code The code.
     * @param title The title
     */
    IntervalUnit(final int code, @Nonnull final String title)
    {
        _code = code;
        _title = title;
    }

    /**
     * Gets the instance for a code.
     *
     * @param code The code.
     *
     * @return The instance (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<IntervalUnit> instance(final int code)
    {
        return Optional.ofNullable(_CODE_MAP.get(Integer.valueOf(code)));
    }

    /** {@inheritDoc}
     */
    @Override
    public int getCode()
    {
        return _code;
    }

    /**
     * Gets the title.
     *
     * @return The title.
     */
    @Nonnull
    @CheckReturnValue
    public String getTitle()
    {
        return _title;
    }

    private static final Map<Integer, IntervalUnit> _CODE_MAP =
        MapFactory.codeMap(
            values());

    private final int _code;
    private final String _title;
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

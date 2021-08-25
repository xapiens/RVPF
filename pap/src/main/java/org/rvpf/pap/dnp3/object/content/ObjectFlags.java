/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ObjectFlags.java 3976 2019-05-11 17:26:10Z SFB $
 */

package org.rvpf.pap.dnp3.object.content;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Object flags.
 */
public final class ObjectFlags
{
    /**
     * Gets the chatter filter flag.
     *
     * @return The chatter filter flag.
     */
    @CheckReturnValue
    public boolean getChatterFilter()
    {
        return _getFlag(_CHATTER_FILTER_MASK);
    }

    /**
     * Gets the comm lost flag.
     *
     * @return The comm lost flag.
     */
    @CheckReturnValue
    public boolean getCommLost()
    {
        return _getFlag(_COMM_LOST_MASK);
    }

    /**
     * Gets the discontinuity flag.
     *
     * @return The discontinuity flag.
     */
    @CheckReturnValue
    public boolean getDiscontinuity()
    {
        return _getFlag(_DISCONTINUITY_MASK);
    }

    /**
     * Gets the double-bit state.
     *
     * @return The double-bit state.
     */
    @Nonnull
    @CheckReturnValue
    public DoubleBitState getDoubleBitState()
    {
        return DoubleBitState
            .instance(
                (_flags & _DOUBLE_BIT_STATE_MASK) >> _DOUBLE_BIT_STATE_SHIFT);
    }

    /**
     * Gets the flags.
     *
     * @return The flags.
     */
    @CheckReturnValue
    public byte getFlags()
    {
        return _flags;
    }

    /**
     * Gets the local forced flag.
     *
     * @return The local forced flag.
     */
    @CheckReturnValue
    public boolean getLocalForced()
    {
        return _getFlag(_LOCAL_FORCED_MASK);
    }

    /**
     * Gets the online flag.
     *
     * @return The online flag.
     */
    @CheckReturnValue
    public boolean getOnline()
    {
        return _getFlag(_ONLINE_MASK);
    }

    /**
     * Gets the over range flag.
     *
     * @return The over range flag.
     */
    @CheckReturnValue
    public boolean getOverRange()
    {
        return _getFlag(_OVER_RANGE_MASK);
    }

    /**
     * Gets the reference err flag.
     *
     * @return The reference err flag.
     */
    @CheckReturnValue
    public boolean getReferenceErr()
    {
        return _getFlag(_REFERENCE_ERR_MASK);
    }

    /**
     * Gets the remote forced flag.
     *
     * @return The remote forced flag.
     */
    @CheckReturnValue
    public boolean getRemoteForced()
    {
        return _getFlag(_REMOTE_FORCED_MASK);
    }

    /**
     * Gets the restart flag.
     *
     * @return The restart flag.
     */
    @CheckReturnValue
    public boolean getRestart()
    {
        return _getFlag(_RESTART_MASK);
    }

    /**
     * Gets the state flag.
     *
     * @return The state flag.
     */
    @CheckReturnValue
    public boolean getState()
    {
        return _getFlag(_STATE_MASK);
    }

    /**
     * Asks if the flags are at their default value.
     *
     * @return True if the flags are at their default value.
     */
    @CheckReturnValue
    public boolean isDefault()
    {
        return (_flags & ~(_STATE_MASK | _DOUBLE_BIT_STATE_MASK))
               == _ONLINE_MASK;
    }

    /**
     * Sets the chatter filter flag.
     *
     * @param chatterFilter The chatter filter flag.
     */
    public void setChatterFilter(final boolean chatterFilter)
    {
        _setFlag(_CHATTER_FILTER_MASK, chatterFilter);
    }

    /**
     * Sets the comm lost flag.
     *
     * @param commLost The comm lost flag.
     */
    public void setCommLost(final boolean commLost)
    {
        _setFlag(_COMM_LOST_MASK, commLost);
    }

    /**
     * Sets the discontinuity flag.
     *
     * @param discontinuity The discontinuity flag.
     */
    public void setDiscontinuity(final boolean discontinuity)
    {
        _setFlag(_DISCONTINUITY_MASK, discontinuity);
    }

    /**
     * Sets the double-bit state.
     *
     * @param state The state.
     */
    public void setDoubleBitState(@Nonnull final DoubleBitState state)
    {
        _flags &= _DOUBLE_BIT_STATE_MASK;
        _flags |= state.ordinal() << _DOUBLE_BIT_STATE_SHIFT;
    }

    /**
     * Sets the flags.
     *
     * @param flags The flags.
     */
    public void setFlags(final byte flags)
    {
        _flags = flags;
    }

    /**
     * Sets the flags.
     *
     * @param objectFlags An other instance (empty resets).
     */
    public void setFlags(@Nonnull final Optional<ObjectFlags> objectFlags)
    {
        setFlags(
            objectFlags.isPresent()
            ? objectFlags.get().getFlags(): _ONLINE_MASK);
    }

    /**
     * Sets the local forced flag.
     *
     * @param localForced The local forced flag.
     */
    public void setLocalForced(final boolean localForced)
    {
        _setFlag(_LOCAL_FORCED_MASK, localForced);
    }

    /**
     * Sets the online flag.
     *
     * @param online The online flag.
     */
    public void setOnline(final boolean online)
    {
        _setFlag(_ONLINE_MASK, online);
    }

    /**
     * Sets the over range flag.
     *
     * @param overRange The over range flag.
     */
    public void setOverRange(final boolean overRange)
    {
        _setFlag(_OVER_RANGE_MASK, overRange);
    }

    /**
     * Sets the reference err flag.
     *
     * @param referenceErr The reference err flag.
     */
    public void setReferenceErr(final boolean referenceErr)
    {
        _setFlag(_REFERENCE_ERR_MASK, referenceErr);
    }

    /**
     * Sets the remote forced flag.
     *
     * @param remoteForced The remote forced flag.
     */
    public void setRemoteForced(final boolean remoteForced)
    {
        _setFlag(_REMOTE_FORCED_MASK, remoteForced);
    }

    /**
     * Sets the restart flag.
     *
     * @param restart The restart flag.
     */
    public void setRestart(final boolean restart)
    {
        _setFlag(_RESTART_MASK, restart);
    }

    /**
     * Sets the state flag.
     *
     * @param state The state flag.
     */
    public void setState(final boolean state)
    {
        _setFlag(_STATE_MASK, state);
    }

    private boolean _getFlag(final byte mask)
    {
        return (_flags & mask) != 0;
    }

    private void _setFlag(final byte mask, final boolean state)
    {
        _flags &= ~mask;

        if (state) {
            _flags |= mask;
        }
    }

    /** The number of bytes needed to represent the flags. */
    public static final int BYTES = Byte.BYTES;

    /**  */

    private static final byte _CHATTER_FILTER_MASK = 0x20;
    private static final byte _COMM_LOST_MASK = 0x02;
    private static final byte _DISCONTINUITY_MASK = 0x40;
    private static final byte _DOUBLE_BIT_STATE_MASK = (byte) 0xC0;
    private static final byte _DOUBLE_BIT_STATE_SHIFT = 6;
    private static final byte _LOCAL_FORCED_MASK = 0x10;
    private static final byte _ONLINE_MASK = 0x01;
    private static final byte _OVER_RANGE_MASK = 0x20;
    private static final byte _REFERENCE_ERR_MASK = 0x40;
    private static final byte _REMOTE_FORCED_MASK = 0x08;
    private static final byte _RESTART_MASK = 0x02;
    private static final byte _STATE_MASK = (byte) 0x80;

    private byte _flags = _ONLINE_MASK;
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

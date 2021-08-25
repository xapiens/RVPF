/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: InternalIndications.java 4085 2019-06-16 15:17:12Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.pap.dnp3.object.content.InternalIndication;

/**
 * Internal indications.
 */
public final class InternalIndications
{
    /**
     * Constructs an instance.
     *
     * @param internalIndications The internal indications.
     */
    public InternalIndications(
            @Nonnull final InternalIndication... internalIndications)
    {
        for (final InternalIndication internalIndication: internalIndications) {
            _internalIndications |= internalIndication.getMask();
        }
    }

    /**
     * Constructs an instance.
     *
     * @param internalIndications The internal indication bits.
     */
    public InternalIndications(@Nonnull final short internalIndications)
    {
        _internalIndications = internalIndications;
    }

    /**
     * Adds internal indications.
     *
     * @param internalIndications The added internal indications.
     */
    public void add(@Nonnull final InternalIndications internalIndications)
    {
        _internalIndications |= internalIndications.getInternalIndications();
    }

    /**
     * Gets the internal indications.
     *
     * @return The internal indications.
     */
    @CheckReturnValue
    public short getInternalIndications()
    {
        return _internalIndications;
    }

    /**
     * Asks if this fragment has the already executing indicator.
     *
     * @return True if it has the already executing indicator.
     */
    @CheckReturnValue
    public boolean hasAlreadyExecuting()
    {
        return _getInternalIndication(InternalIndication.ALREADY_EXECUTING);
    }

    /**
     * Asks if this fragment has the broadcast indicator.
     *
     * @return True if it has the broadcast indicator.
     */
    @CheckReturnValue
    public boolean hasBroadcast()
    {
        return _getInternalIndication(InternalIndication.BROADCAST);
    }

    /**
     * Asks if this fragment indicates class 1 events.
     *
     * @return True if it indicates class 1 events.
     */
    @CheckReturnValue
    public boolean hasClass1Events()
    {
        return _getInternalIndication(InternalIndication.CLASS_1_EVENTS);
    }

    /**
     * Asks if this fragment indicates class 2 events.
     *
     * @return True if it indicates class 2 events.
     */
    @CheckReturnValue
    public boolean hasClass2Events()
    {
        return _getInternalIndication(InternalIndication.CLASS_2_EVENTS);
    }

    /**
     * Asks if this fragment indicates class 3 events.
     *
     * @return True if it indicates class 3 events.
     */
    @CheckReturnValue
    public boolean hasClass3Events()
    {
        return _getInternalIndication(InternalIndication.CLASS_3_EVENTS);
    }

    /**
     * Asks if this fragment has the config corrupt indicator.
     *
     * @return True if it has the config corrupt indicator.
     */
    @CheckReturnValue
    public boolean hasConfigCorrupt()
    {
        return _getInternalIndication(InternalIndication.CONFIG_CORRUPT);
    }

    /**
     * Asks if this fragment has the device restart indicator.
     *
     * @return True if it has the device restart indicator.
     */
    @CheckReturnValue
    public boolean hasDeviceRestart()
    {
        return _getInternalIndication(InternalIndication.DEVICE_RESTART);
    }

    /**
     * Asks if this fragment has the device trouble indicator.
     *
     * @return True if it has the device trouble indicator.
     */
    @CheckReturnValue
    public boolean hasDeviceTrouble()
    {
        return _getInternalIndication(InternalIndication.DEVICE_TROUBLE);
    }

    /**
     * Asks if this fragment has the event buffer overflow indicator.
     *
     * @return True if it has the event buffer overflow indicator.
     */
    @CheckReturnValue
    public boolean hasEventBufferOverflow()
    {
        return _getInternalIndication(InternalIndication.EVENT_BUFFER_OVERFLOW);
    }

    /**
     * Asks if this fragment has the local control indicator.
     *
     * @return True if it has the local control indicator.
     */
    @CheckReturnValue
    public boolean hasLocalControl()
    {
        return _getInternalIndication(InternalIndication.LOCAL_CONTROL);
    }

    /**
     * Asks if this fragment has the need time indicator.
     *
     * @return True if it has the need time indicator.
     */
    @CheckReturnValue
    public boolean hasNeedTime()
    {
        return _getInternalIndication(InternalIndication.NEED_TIME);
    }

    /**
     * Asks if this fragment has the function code not supported indicator.
     *
     * @return True if it has the function code not supported indicator.
     */
    @CheckReturnValue
    public boolean hasNoFuncCodeSupport()
    {
        return _getInternalIndication(InternalIndication.NO_FUNC_CODE_SUPPORT);
    }

    /**
     * Asks if this fragment has the object unknown indicator.
     *
     * @return True if it has the object unknown indicator.
     */
    @CheckReturnValue
    public boolean hasObjectUnknown()
    {
        return _getInternalIndication(InternalIndication.OBJECT_UNKNOWN);
    }

    /**
     * Asks if this fragment has the parameter error indicator.
     *
     * @return True if it has the parameter error indicator.
     */
    @CheckReturnValue
    public boolean hasParameterError()
    {
        return _getInternalIndication(InternalIndication.PARAMETER_ERROR);
    }

    /**
     * Sets internal indications.
     *
     * @param internalIndications The added internal indications.
     */
    public void set(@Nonnull final InternalIndications internalIndications)
    {
        _internalIndications = internalIndications.getInternalIndications();
    }

    /**
     * Sets the value of an internal indication.
     *
     * @param internalIndication The internal indication.
     * @param set True to set, false to clear.
     */
    public void set(
            @Nonnull final InternalIndication internalIndication,
            final boolean set)
    {
        if (set) {
            _internalIndications |= internalIndication.getMask();
        } else {
            _internalIndications &= ~internalIndication.getMask();
        }
    }

    /**
     * Sets the already executing indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setAlreadyExecuting(final boolean state)
    {
        _setInternalIndication(InternalIndication.ALREADY_EXECUTING, state);
    }

    /**
     * Sets the broadcast indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setBroadcast(final boolean state)
    {
        _setInternalIndication(InternalIndication.BROADCAST, state);
    }

    /**
     * Sets the class 1 events indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setClass1Events(final boolean state)
    {
        _setInternalIndication(InternalIndication.CLASS_1_EVENTS, state);
    }

    /**
     * Sets the class 2 events indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setClass2Events(final boolean state)
    {
        _setInternalIndication(InternalIndication.CLASS_2_EVENTS, state);
    }

    /**
     * Sets the class 3 events indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setClass3Events(final boolean state)
    {
        _setInternalIndication(InternalIndication.CLASS_3_EVENTS, state);
    }

    /**
     * Sets the config corrupt indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setConfigCorrupt(final boolean state)
    {
        _setInternalIndication(InternalIndication.CONFIG_CORRUPT, state);
    }

    /**
     * Sets the device restart indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setDeviceRestart(final boolean state)
    {
        _setInternalIndication(InternalIndication.DEVICE_RESTART, state);
    }

    /**
     * Sets the device trouble indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setDeviceTrouble(final boolean state)
    {
        _setInternalIndication(InternalIndication.DEVICE_TROUBLE, state);
    }

    /**
     * Sets the event buffer overflow indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setEventBufferOverflow(final boolean state)
    {
        _setInternalIndication(InternalIndication.EVENT_BUFFER_OVERFLOW, state);
    }

    /**
     * Sets the local control indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setLocalControl(final boolean state)
    {
        _setInternalIndication(InternalIndication.LOCAL_CONTROL, state);
    }

    /**
     * Sets the need time indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setNeedTime(final boolean state)
    {
        _setInternalIndication(InternalIndication.NEED_TIME, state);
    }

    /**
     * Sets the function code not supported indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setNoFuncCodeSupport(final boolean state)
    {
        _setInternalIndication(InternalIndication.NO_FUNC_CODE_SUPPORT, state);
    }

    /**
     * Sets the object unknown indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setObjectUnknown(final boolean state)
    {
        _setInternalIndication(InternalIndication.OBJECT_UNKNOWN, state);
    }

    /**
     * Sets the parameter error indicator for this fragment.
     *
     * @param state The state of the indicator.
     */
    public void setParameterError(final boolean state)
    {
        _setInternalIndication(InternalIndication.PARAMETER_ERROR, state);
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        if (_internalIndications == 0) {
            return "";
        }

        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < InternalIndication.getSize(); ++i) {
            if ((_internalIndications & (1 << i)) != 0) {
                builder.append(',');
                builder.append(InternalIndication.instance(i));
            }
        }

        return builder.toString();
    }

    private boolean _getInternalIndication(
            final InternalIndication internalIndication)
    {
        return (_internalIndications & internalIndication.getMask()) != 0;
    }

    private void _setInternalIndication(
            final InternalIndication internalIndication,
            final boolean state)
    {
        final int mask = internalIndication.getMask();

        if (state) {
            _internalIndications |= mask;
        } else {
            _internalIndications &= ~mask;
        }
    }

    private short _internalIndications;
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

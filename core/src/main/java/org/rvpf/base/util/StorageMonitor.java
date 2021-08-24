/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StorageMonitor.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.util;

import java.io.File;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongSupplier;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedValues;

/**
 * Storage monitor.
 */
public final class StorageMonitor
{
    /**
     * Constructs an instance.
     *
     * @param logger A logger.
     */
    public StorageMonitor(@Nonnull final Logger logger)
    {
        this(logger, Optional.empty(), Optional.empty());
    }

    /**
     * Constructs an instance.
     *
     * <p>When the transition handler predicate is called, it returns false to
     * let the storage monitor log a message (also logged if the handler is
     * null).</p>
     *
     * @param logger A logger.
     * @param onWarn Called on 'warn' transition.
     * @param onAlert Called on 'alert' transition.
     */
    public StorageMonitor(
            @Nonnull final Logger logger,
            @Nonnull final Optional<StateTransitionHandler> onWarn,
            @Nonnull final Optional<StateTransitionHandler> onAlert)
    {
        _logger = Require.notNull(logger);
        _onWarn = onWarn;
        _onAlert = onAlert;
    }

    /**
     * Checks.
     *
     * @return True unless on alert.
     */
    @CheckReturnValue
    public boolean check()
    {
        Require.success(_READY_STATES.contains(_state));

        if ((_state != State.INITIAL)
                && (_freeAlert == 0)
                && (_freeWarn == 0)) {
            return true;
        }

        final long usableSpace = _usableSpaceSupplier.getAsLong();

        if (_available >= _freeWarn) {
            if (usableSpace < _freeWarn) {
                final boolean warned;

                if (_onWarn.isPresent()) {
                    warned = _onWarn.get().onTransition(true);
                } else {
                    warned = false;
                }

                if (!warned) {
                    _logger
                        .warn(
                            BaseMessages.STORAGE_UNDER_WARN,
                            Integer.valueOf((int) (_freeWarn / 1_000_000)));
                }

                _state = State.WARNED;
            }
        }

        if (_available >= _freeAlert) {
            if (usableSpace < _freeAlert) {
                final boolean alerted;

                if (_onAlert.isPresent()) {
                    alerted = _onAlert.get().onTransition(true);
                } else {
                    alerted = false;
                }

                if (!alerted) {
                    _logger
                        .warn(
                            BaseMessages.STORAGE_UNDER_ALERT,
                            Integer.valueOf((int) (_freeAlert / 1_000_000)));
                }

                _state = State.ALERTED;
            }
        } else if (usableSpace >= _freeAlert) {
            final boolean alerted;

            if (_onAlert.isPresent()) {
                alerted = _onAlert.get().onTransition(false);
            } else {
                alerted = false;
            }

            if (!alerted) {
                _logger
                    .info(
                        BaseMessages.STORAGE_NOT_UNDER_ALERT,
                        Integer.valueOf((int) (_freeAlert / 1_000_000)));
            }

            _state = State.WARNED;
        }

        if ((_state == State.WARNED) && (usableSpace >= _freeWarn)) {
            final boolean warned;

            if (_onWarn.isPresent()) {
                warned = _onWarn.get().onTransition(false);
            } else {
                warned = false;
            }

            if (!warned) {
                _logger
                    .info(
                        BaseMessages.STORAGE_NOT_UNDER_WARN,
                        Integer.valueOf((int) (_freeWarn / 1_000_000)));
            }

            _state = State.NORMAL;
        }

        _available = usableSpace;

        if (_state == State.INITIAL) {
            _state = State.NORMAL;
        }

        return usableSpace >= _freeAlert;
    }

    /**
     * Gets the available space size in bytes from the last check.
     *
     * @return The available space size in bytes.
     */
    @CheckReturnValue
    public long getAvailable()
    {
        return (_CHECKED_STATES
            .contains(_state))? _available: _usableSpaceSupplier.getAsLong();
    }

    /**
     * Gets the free space size in million bytes from the last check.
     *
     * @return The free space size in million bytes.
     */
    @CheckReturnValue
    public int getFree()
    {
        return (int) (getAvailable() / 1_000_000);
    }

    /**
     * Gets the state.
     *
     * @return The state.
     */
    @CheckReturnValue
    public State getState()
    {
        return _state;
    }

    /**
     * Sets up this.
     *
     * @param storageProperties The storage properties.
     * @param defaultRootDir The default root directory.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp(
            @Nonnull final KeyedValues storageProperties,
            @Nonnull final File defaultRootDir)
    {
        Require.notNull(defaultRootDir);

        if (_state != State.UNAVAILABLE) {
            tearDown();
        }

        _state = State.UNSTABLE;

        _freeWarn = storageProperties
            .getInt(FREE_WARN_PROPERTY, 0) * 1_000_000L;
        _freeAlert = storageProperties
            .getInt(FREE_ALERT_PROPERTY, 0) * 1_000_000L;

        final String rootDirString = storageProperties
            .getString(ROOT_DIR_PROPERTY, Optional.of(""))
            .get();

        _rootDir = rootDirString
            .isEmpty()? defaultRootDir: new File(rootDirString);
        _state = State.INITIAL;

        return true;
    }

    /**
     * Sets the usable space supplier.
     *
     * <p>Used by storage monitor tests.</p>
     *
     * @param usableSpaceSupplier The usable space supplier.
     */
    public void setUsableSpaceSupplier(
            @Nonnull final LongSupplier usableSpaceSupplier)
    {
        _usableSpaceSupplier = Require.notNull(usableSpaceSupplier);
    }

    /**
     * Tears down what as been set up.
     */
    public void tearDown()
    {
        _rootDir = null;
        _freeAlert = 0;
        _freeWarn = 0;
        _available = Long.MAX_VALUE;
        _state = State.UNAVAILABLE;
    }

    private long _getUsableSpace()
    {
        Require.success(_READY_STATES.contains(_state));

        return _rootDir.getUsableSpace();
    }

    /** Free alert property. */
    public static final String FREE_ALERT_PROPERTY = "free.alert";

    /** Free warn property. */
    public static final String FREE_WARN_PROPERTY = "free.warn";

    /** Root dir property. */
    public static final String ROOT_DIR_PROPERTY = "root.dir";

    /**  */

    private static final Set<State> _CHECKED_STATES = EnumSet
        .range(State.NORMAL, State.ALERTED);
    private static final Set<State> _READY_STATES = EnumSet
        .range(State.INITIAL, State.ALERTED);

    private long _available = Long.MAX_VALUE;
    private long _freeAlert;
    private long _freeWarn;
    private final Logger _logger;
    private final Optional<StateTransitionHandler> _onAlert;
    private final Optional<StateTransitionHandler> _onWarn;
    private File _rootDir;
    private volatile State _state = State.UNAVAILABLE;
    private LongSupplier _usableSpaceSupplier = this::_getUsableSpace;

    /**
     * State.
     */
    public enum State
    {
        UNAVAILABLE,
        UNSTABLE,
        INITIAL,
        NORMAL,
        WARNED,
        ALERTED
    }

    /**
     * State transition handler.
     */
    public interface StateTransitionHandler
    {
        /**
         * Called on some state transitions.
         *
         * @param on True on transition to the associated state or higher.
         *
         * @return True if the handling is complete.
         */
        boolean onTransition(boolean on);
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

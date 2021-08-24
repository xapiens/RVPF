/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AlertContext.java 4043 2019-06-02 15:05:37Z SFB $
 */

package org.rvpf.http.alert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.UUID;
import org.rvpf.base.alert.Event;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.http.HTTPMessages;
import org.rvpf.http.HTTPModule;
import org.rvpf.service.Service;

/**
 * Alert context.
 */
final class AlertContext
    extends HTTPModule.Context
{
    /**
     * Creates an instance.
     *
     * @param alertModule The context owner.
     */
    AlertContext(@Nonnull final AlertModule alertModule)
    {
        _alertModule = alertModule;
    }

    /**
     * Gets the events list.
     *
     * @return The events list.
     */
    @Nonnull
    @CheckReturnValue
    synchronized Collection<Event> getEvents()
    {
        return new ArrayList<Event>(_events.values());
    }

    /**
     * Gets the events received after a specified time.
     *
     * @param after The specified time.
     *
     * @return The events list.
     */
    @Nonnull
    @CheckReturnValue
    synchronized Collection<Event> getEvents(@Nonnull final DateTime after)
    {
        final Collection<Event> eventsAfter = _events
            .tailMap(after, false)
            .values();
        final Collection<Event> events = new ArrayList<Event>(
            1 + eventsAfter.size());

        if (_events.isEmpty() || after.isBefore(_events.firstKey())) {
            events.add(_alertModule.createEvent(LOST_EVENTS_EVENT));
        }

        events.addAll(eventsAfter);

        return events;
    }

    /**
     * Gets the last event for each service.
     *
     * @return The collection of events.
     */
    @Nonnull
    @CheckReturnValue
    synchronized Collection<Event> getServiceEvents()
    {
        return new ArrayList<Event>(_serviceMap.values());
    }

    /**
     * Gets a time stamp after the last one supplied and not before now.
     *
     * @return The new time stamp.
     */
    @Nonnull
    @CheckReturnValue
    synchronized DateTime getStamp()
    {
        DateTime stamp = DateTime.now();

        if (stamp.isNotAfter(_stamp)) {
            stamp = _stamp.after();
        }

        _stamp = stamp;

        return stamp;
    }

    /**
     * Returns the next signal.
     *
     * @return The next signal or empty.
     */
    @Nonnull
    @CheckReturnValue
    synchronized Optional<Signal> nextSignal()
    {
        return (!_signals
            .isEmpty())? Optional.of(_signals.removeFirst()): Optional.empty();
    }

    /**
     * Acts on an event.
     *
     * @param event The event.
     */
    synchronized void onEvent(@Nonnull final Event event)
    {
        if (event.getSourceUUID().isPresent()) {
            _serviceMap.put(event.getSourceUUID().get(), event);
        }

        final int eventsLimit = _eventsLimit;

        while (_events.size() >= eventsLimit) {
            _events.remove(_events.firstKey());
        }

        _events.put(getStamp(), event);
        notifyAll();
    }

    /**
     * Queues a signal.
     *
     * @param name The signal's name.
     * @param info Optional additional informations.
     */
    void queueSignal(
            @Nonnull final String name,
            @Nonnull final Optional<String> info)
    {
        synchronized (this) {
            _signals.addLast(new Signal(name, info));
        }

        _alertModule.callbackForPendingActions();
    }

    /**
     * Sets the restart enabled indicator.
     *
     * @param restartEnabled The restart enabled indicator.
     *
     * @return The previous value of the indicator.
     */
    boolean setRestartEnabled(final boolean restartEnabled)
    {
        return _alertModule.setRestartEnabled(restartEnabled);
    }

    /**
     * Sets up this.
     *
     * @param contextProperties The context properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull final KeyedGroups contextProperties)
    {
        final KeyedGroups alertProperties = contextProperties
            .getGroup(ALERT_PROPERTIES);

        _restraintInterval = alertProperties
            .getElapsed(
                RESTRAINT_PROPERTY,
                Optional.of(DEFAULT_RESTRAINT),
                Optional.of(DEFAULT_RESTRAINT))
            .orElse(null);

        _restraint = null;
        useRestraint();
        _LOGGER.debug(HTTPMessages.RESTRAINT, _restraintInterval);
        _eventsLimit = alertProperties
            .getInt(EVENTS_LIMIT_PROPERTY, DEFAULT_EVENTS_LIMIT);
        _LOGGER.debug(HTTPMessages.EVENTS_LIMIT, String.valueOf(_eventsLimit));

        queueSignal(
            Service.PING_SIGNAL,
            Optional.empty());    // Starts with a Ping!

        return true;
    }

    /**
     * Insures that the signals are used with restraint.
     *
     * @return True if a signal can be sent now.
     */
    synchronized boolean useRestraint()
    {
        if ((_restraint != null) && DateTime.now().isBefore(_restraint)) {
            return false;
        }

        _restraint = DateTime.now().after(_restraintInterval);

        return true;
    }

    /**
     * Waits for a new event.
     *
     * @param after The time reference.
     * @param timeout The maximum time to wait in milliseconds.
     */
    synchronized void waitForEvent(
            @Nonnull final DateTime after,
            final long timeout)
    {
        final long startMillis = System.currentTimeMillis();

        while (_events.isEmpty() || _events.lastKey().isNotAfter(after)) {
            final long elapsedMillis = System.currentTimeMillis() - startMillis;

            if ((elapsedMillis < 0) || (elapsedMillis >= timeout)) {
                break;
            }

            try {
                wait(timeout - elapsedMillis);
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();    // Keep interrupt status.

                break;
            }
        }
    }

    /** Alert properties. */
    public static final String ALERT_PROPERTIES = "alert";

    /** Default events limit. */
    public static final int DEFAULT_EVENTS_LIMIT = 1000;

    /** Default restraint. */
    public static final ElapsedTime DEFAULT_RESTRAINT = ElapsedTime
        .fromMillis(60000);

    /** Specifies the number of events to keep as history. */
    public static final String EVENTS_LIMIT_PROPERTY = "events.limit";

    /** Lost events event. */
    public static final String LOST_EVENTS_EVENT = "LostEvents";

    /**
     * Specifies a restraint period on startup or after a signal other than
     * 'Ping'. This periods limits alert triggers only to the 'Ping' signal.
     */
    public static final String RESTRAINT_PROPERTY = "restraint";
    private static final Logger _LOGGER = Logger
        .getInstance(AlertContext.class);

    private final AlertModule _alertModule;
    private final NavigableMap<DateTime, Event> _events = new TreeMap<>();
    private volatile int _eventsLimit;
    private volatile DateTime _restraint;
    private volatile ElapsedTime _restraintInterval;
    private final Map<UUID, Event> _serviceMap = new HashMap<>();
    private final LinkedList<Signal> _signals = new LinkedList<>();
    private DateTime _stamp = DateTime.now();

    /**
     * Signal.
     */
    static class Signal
    {
        /**
         * Constructs an instance.
         *
         * @param name The signal's name.
         * @param info Optional additional informations.
         */
        Signal(@Nonnull final String name, @Nonnull final Optional<String> info)
        {
            _name = name;
            _info = info.orElse(null);
        }

        /**
         * Gets the additional informations.
         *
         * @return The optional additional informations.
         */
        @Nonnull
        @CheckReturnValue
        Optional<String> getInfo()
        {
            return Optional.ofNullable(_info);
        }

        /**
         * Gets the signal's name.
         *
         * @return The signal's name.
         */
        @Nonnull
        @CheckReturnValue
        String getName()
        {
            return _name;
        }

        private final String _info;
        private final String _name;
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

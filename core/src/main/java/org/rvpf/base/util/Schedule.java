/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Schedule.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;

/**
 * Schedule.
 *
 * @param <E> The event implementation.
 */
public class Schedule<E extends Schedule.Event>
    extends AbstractSet<E>
    implements Iterator<E>
{
    /**
     * Constructs an instance.
     *
     * @param future True to schedule only in the future.
     */
    public Schedule(final boolean future)
    {
        _future = future;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean add(final E event)
    {
        final boolean added;

        synchronized (_mutex) {
            added = _events.add(event);
            _mutex.notifyAll();
        }

        return added;
    }

    /**
     * Advances to the next event.
     */
    public final void advance()
    {
        synchronized (_mutex) {
            Require.failure(_busy);

            if (_returnedEvent != null) {
                _events.remove(_returnedEvent);
                _returnedEvent.advance(_future);
                _events.add(_returnedEvent);
                _returnedEvent = null;
            }
        }
    }

    /**
     * Cancels an event.
     *
     * @param event The event.
     */
    public final void cancel(@Nonnull final E event)
    {
        final long eventId = event.getId();
        final Iterator<E> iterator = _events.iterator();

        while (iterator.hasNext()) {
            final E scheduledEvent = iterator.next();

            if (scheduledEvent.getId() == eventId) {
                iterator.remove();

                return;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void clear()
    {
        synchronized (_mutex) {
            _events.clear();
            _mutex.notifyAll();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean contains(final Object object)
    {
        synchronized (_events) {
            return _events.contains(object);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean containsAll(final Collection<?> collection)
    {
        synchronized (_mutex) {
            return _events.containsAll(collection);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean equals(final Object other)
    {
        return this == other;
    }

    /**
     * Gets the delay before the first event is due.
     *
     * @return A number of milliseconds (negative if overdue).
     *
     * @throws Require.FailureException When empty.
     */
    @CheckReturnValue
    public final long getDelay()
    {
        final Optional<E> firstEvent = peek();

        Require.success(firstEvent.isPresent());

        return firstEvent.get().getDelay();
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean hasNext()
    {
        synchronized (_mutex) {
            return !isEmpty();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final int hashCode()
    {
        return System.identityHashCode(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isEmpty()
    {
        synchronized (_mutex) {
            return _events.isEmpty();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final Iterator<E> iterator()
    {
        synchronized (_mutex) {
            Require.failure(_busy);
        }

        return this;
    }

    /** {@inheritDoc}
     */
    @Override
    public final E next()
    {
        synchronized (_mutex) {
            try {
                return next(true).get();
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();

                return null;
            }
        }
    }

    /**
     * Peeks at the first event.
     *
     * @return The first event (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<E> peek()
    {
        synchronized (_mutex) {
            return isEmpty()? Optional.empty(): Optional.of(_events.first());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void remove()
    {
        synchronized (_mutex) {
            Require.failure(_busy);

            if (_returnedEvent != null) {
                _events.remove(_returnedEvent);
                _returnedEvent = null;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean remove(final Object object)
    {
        final boolean removed;

        synchronized (_mutex) {
            removed = _events.remove(object);

            if (removed && (_returnedEvent != null)) {
                if (_returnedEvent.compareTo((Event) object) == 0) {
                    Require.failure(_busy);

                    _returnedEvent = null;
                }
            }
        }

        return removed;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean removeAll(final Collection<?> collection)
    {
        boolean modified = false;

        for (final Object object: collection) {
            modified |= remove(object);
        }

        return modified;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean retainAll(final Collection<?> collection)
    {
        boolean modified = false;

        synchronized (_mutex) {
            final Iterator<E> iterator = _events.iterator();

            while (iterator.hasNext()) {
                final E event = iterator.next();

                if (!collection.contains(event)) {
                    iterator.remove();

                    if (event == _returnedEvent) {
                        Require.failure(_busy);

                        _returnedEvent = null;
                    }

                    modified = true;
                }
            }
        }

        return modified;
    }

    /** {@inheritDoc}
     */
    @Override
    public final int size()
    {
        synchronized (_mutex) {
            return _events.size();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final Object[] toArray()
    {
        synchronized (_mutex) {
            return _events.toArray();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final <T> T[] toArray(final T[] array)
    {
        synchronized (_mutex) {
            return _events.toArray(array);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final String toString()
    {
        synchronized (_mutex) {
            return _events.toString();
        }
    }

    /**
     * Cancels wait.
     */
    protected void cancelWait()
    {
        synchronized (_mutex) {
            _cancelWait = true;
            _mutex.notifyAll();
        }
    }

    /**
     * Gets the events.
     *
     * @return The events.
     */
    @Nonnull
    @CheckReturnValue
    protected NavigableSet<E> getEvents()
    {
        return _events;
    }

    /**
     * Gets the mutex.
     *
     * @return The mutex.
     */
    @Nonnull
    @CheckReturnValue
    protected final Object getMutex()
    {
        return _mutex;
    }

    /**
     * Returns the next event.
     *
     * <p>Note: must be called while synchronized on {@link #getMutex()}. </p>
     *
     * @param wait True to wait.
     *
     * @return The next event (empty if none without wait)
     *
     * @throws InterruptedException When interrupted.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<E> next(
            final boolean wait)
        throws InterruptedException
    {
        advance();

        _busy = true;

        for (;;) {
            final E firstEvent = peek().orElse(null);

            if (firstEvent == null) {
                throw new NoSuchElementException();
            }

            final long delay = firstEvent.getDelay();

            if (delay <= 0) {
                _returnedEvent = firstEvent;

                break;
            }

            if (wait) {
                _cancelWait = false;
                _mutex.wait(delay);

                if (_cancelWait) {
                    _returnedEvent = null;

                    break;
                }

                continue;
            }

            break;
        }

        _busy = false;

        return Optional.ofNullable(_returnedEvent);
    }

    /**
     * Rolls back the latest call to {@link #next(boolean)}.
     */
    protected final void rollback()
    {
        _returnedEvent = null;
    }

    /**
     * Asks if past events should be skipped.
     *
     * @return True if past events should be skipped.
     */
    boolean _isSkipPastEvents()
    {
        return _future;
    }

    private boolean _busy;
    private volatile boolean _cancelWait;
    private final NavigableSet<E> _events = new TreeSet<>();
    private final boolean _future;
    private final Object _mutex = new Object();
    private E _returnedEvent;

    /**
     * Event.
     */
    public interface Event
        extends Comparable<Event>
    {
        /**
         * Advances.
         *
         * @param future True to advance in the future.
         */
        void advance(boolean future);

        /**
         * Gets the delay before the event is due.
         *
         * @return A number of milliseconds (negative if overdue).
         */
        @CheckReturnValue
        long getDelay();

        /**
         * Gets the event id.
         *
         * @return The vent id.
         */
        @CheckReturnValue
        long getId();

        /**
         * Gets the stamp.
         *
         * @return The stamp.
         */
        @Nonnull
        @CheckReturnValue
        DateTime getStamp();

        /**
         * Abstract.
         */
        abstract class Abstract
            implements Event
        {
            /**
             * Constructs an instance.
             *
             * @param startStamp The start stamp.
             * @param sync A sync instance.
             */
            Abstract(
                    @Nonnull final DateTime startStamp,
                    @Nonnull final Sync sync)
            {
                _id = _nextId.getAndIncrement();
                _stamp = Require.notNull(startStamp);
                _sync = sync;

                if (!_sync.isInSync(startStamp)) {
                    _stamp = _sync.getNextStamp().orElse(null);
                }
            }

            /** {@inheritDoc}
             */
            @Override
            public final void advance(final boolean future)
            {
                _stamp = _sync.getNextStamp().orElse(null);

                if (future && (_stamp != null)) {
                    final DateTime now = DateTime.now();

                    if (_stamp.isNotAfter(now)) {
                        _stamp = _sync.getNextStamp(now).orElse(null);
                    }
                }
            }

            /** {@inheritDoc}
             */
            @Override
            public int compareTo(final Event otherEvent)
            {
                int comparison = getStamp().compareTo(otherEvent.getStamp());

                if (comparison == 0) {
                    comparison = Long.compare(getId(), otherEvent.getId());
                }

                return comparison;
            }

            /** {@inheritDoc}
             */
            @Override
            public boolean equals(final Object object)
            {
                if ((object == null) || (object.getClass() != getClass())) {
                    return false;
                }

                return compareTo((Event) object) == 0;
            }

            /** {@inheritDoc}
             */
            @Override
            public long getDelay()
            {
                return getStamp().toMillis() - System.currentTimeMillis();
            }

            /** {@inheritDoc}
             */
            @Override
            public long getId()
            {
                return _id;
            }

            /** {@inheritDoc}
             */
            @Override
            public final DateTime getStamp()
            {
                return _stamp;
            }

            /** {@inheritDoc}
             */
            @Override
            public int hashCode()
            {
                return getStamp().hashCode();
            }

            private static final AtomicLong _nextId = new AtomicLong(1);

            private final long _id;
            private DateTime _stamp;
            private final Sync _sync;
        }
    }


    /**
     * Point event.
     */
    public static class PointEvent
        extends Event.Abstract
    {
        /**
         * Constructs an instance.
         *
         * @param point The optional point.
         * @param startStamp The start stamp.
         * @param sync A sync instance.
         */
        public PointEvent(
                @Nonnull final Optional<Point> point,
                @Nonnull final DateTime startStamp,
                @Nonnull final Sync sync)
        {
            super(startStamp, sync);

            _point = point;
        }

        /**
         * Gets the point.
         *
         * @return The point.
         */
        @Nonnull
        @CheckReturnValue
        public final Optional<Point> getPoint()
        {
            return _point;
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            final StringBuilder stringBuilder = new StringBuilder(
                getClass().getSimpleName());

            stringBuilder.append(" #");
            stringBuilder.append(getId());
            stringBuilder.append(" at ");
            stringBuilder.append(getStamp());
            stringBuilder.append(" for '");
            stringBuilder.append(getPoint());
            stringBuilder.append("'");

            return stringBuilder.toString();
        }

        private final Optional<Point> _point;
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

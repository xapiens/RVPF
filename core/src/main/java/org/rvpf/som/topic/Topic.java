/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Topic.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.som.topic;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.som.TopicInfo;
import org.rvpf.base.util.container.KeyedValues;

/**
 * Topic.
 */
interface Topic
{
    /**
     * Closes.
     */
    void close();

    /**
     * Gets the info.
     *
     * @return The info.
     */
    @Nonnull
    @CheckReturnValue
    TopicInfo getInfo();

    /**
     * Returns a new publisher.
     *
     * @return A new publisher.
     */
    @Nonnull
    @CheckReturnValue
    Publisher newPublisher();

    /**
     * Return a new subscriber.
     *
     * @return The new subscriber.
     */
    @Nonnull
    @CheckReturnValue
    Subscriber newSubscriber();

    /**
     * Sets up this topic.
     *
     * @param somProperties The SOM properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull KeyedValues somProperties);

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Publisher.
     */
    public interface Publisher
    {
        /**
         * Closes.
         */
        void close();

        /**
         * Sends messages.
         *
         * @param messages The messages.
         *
         * @throws ServiceClosedException When the service is closed.
         */
        void send(@Nonnull Serializable[] messages)
            throws ServiceClosedException;
    }


    /**
     * Subscriber.
     */
    public interface Subscriber
    {
        /**
         * Closes this subscriber.
         */
        void close();

        /**
         * Receives messages.
         *
         * @param limit The maximum number of messages.
         * @param timeout A time limit in millis to wait for the first message
         *                (negative for infinite).
         *
         * @return The messages.
         *
         * @throws ServiceClosedException When the service is closed.
         */
        @Nonnull
        @CheckReturnValue
        Serializable[] receive(
                int limit,
                long timeout)
            throws ServiceClosedException;
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MessagingSupport.java 4034 2019-05-28 19:57:11Z SFB $
 */

package org.rvpf.tests;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.tests.service.ServiceTests;

/**
 * Messaging support.
 */
public interface MessagingSupport
{
    /**
     * Creates a client publisher.
     *
     * @param topicProperties The topic properties.
     *
     * @return The publisher.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Publisher createClientPublisher(
            @Nonnull KeyedGroups topicProperties)
        throws Exception;

    /**
     * Creates a client publisher.
     *
     * @param name The topic name.
     *
     * @return The publisher.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Publisher createClientPublisher(@Nonnull String name)
        throws Exception;

    /**
     * Creates a client receiver.
     *
     * @param queueProperties The queue properties.
     *
     * @return The receiver.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Receiver createClientReceiver(
            @Nonnull KeyedGroups queueProperties)
        throws Exception;

    /**
     * Creates a client receiver.
     *
     * @param name The queue name.
     *
     * @return The receiver.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Receiver createClientReceiver(@Nonnull String name)
        throws Exception;

    /**
     * Creates a client sender.
     *
     * @param queueProperties The queue properties.
     *
     * @return The sender.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Sender createClientSender(
            @Nonnull KeyedGroups queueProperties)
        throws Exception;

    /**
     * Creates a client sender.
     *
     * @param name The queue name.
     *
     * @return The sender.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Sender createClientSender(@Nonnull String name)
        throws Exception;

    /**
     * Creates a client subscriber.
     *
     * @param topicProperties The topic properties.
     *
     * @return The subscriber.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Subscriber createClientSubscriber(
            @Nonnull KeyedGroups topicProperties)
        throws Exception;

    /**
     * Creates a client subscriber.
     *
     * @param name The topic name.
     *
     * @return The subscriber.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Subscriber createClientSubscriber(@Nonnull String name)
        throws Exception;

    /**
     * Creates a server publisher.
     *
     * @param topicProperties The topic properties.
     *
     * @return The publisher.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Publisher createServerPublisher(
            @Nonnull KeyedGroups topicProperties)
        throws Exception;

    /**
     * Creates a server publisher.
     *
     * @param name The topic name.
     *
     * @return The publisher.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Publisher createServerPublisher(@Nonnull String name)
        throws Exception;

    /**
     * Creates a server receiver.
     *
     * @param queueProperties The queue properties.
     *
     * @return The receiver.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Receiver createServerReceiver(
            @Nonnull KeyedGroups queueProperties)
        throws Exception;

    /**
     * Creates a server receiver.
     *
     * @param name The queue name.
     *
     * @return The receiver.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Receiver createServerReceiver(@Nonnull String name)
        throws Exception;

    /**
     * Creates a server sender.
     *
     * @param queueProperties The queue properties.
     *
     * @return The sender.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Sender createServerSender(
            @Nonnull KeyedGroups queueProperties)
        throws Exception;

    /**
     * Creates a server sender.
     *
     * @param name The queue name.
     *
     * @return The sender.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Sender createServerSender(@Nonnull String name)
        throws Exception;

    /**
     * Creates a server subscriber.
     *
     * @param topicProperties The topic properties.
     *
     * @return The subscriber.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Subscriber createServerSubscriber(
            @Nonnull KeyedGroups topicProperties)
        throws Exception;

    /**
     * Creates a server subscriber.
     *
     * @param name The topic name.
     *
     * @return The subscriber.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Subscriber createServerSubscriber(@Nonnull String name)
        throws Exception;

    /**
     * Sets up messaging support.
     *
     * @param config The configuration.
     * @param client The client.
     */
    void setUp(@Nonnull Config config, @Nonnull ServiceTests client);

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Publisher.
     */
    interface Publisher
    {
        /**
         * Closes this.
         *
         * @throws Exception On failure.
         */
        void close()
            throws Exception;

        /**
         * Opens this.
         *
         * @throws Exception On failure.
         */
        void open()
            throws Exception;

        /**
         * Sends a message.
         *
         * @param content The content of the message.
         *
         * @throws Exception On failure.
         */
        void send(@Nonnull Serializable content)
            throws Exception;
    }


    /**
     * Receiver.
     */
    interface Receiver
    {
        /**
         * Closes this.
         *
         * @throws Exception On failure.
         */
        void close()
            throws Exception;

        /**
         * Commits uncommitted messages.
         *
         * @throws Exception On failure.
         */
        void commit()
            throws Exception;

        /**
         * Opens this.
         *
         * @throws Exception On failure.
         */
        void open()
            throws Exception;

        /**
         * Purges the queue.
         *
         * @return The number of messages purged.
         *
         * @throws Exception On failure.
         */
        long purge()
            throws Exception;

        /**
         * Receives the content of a message.
         *
         * @param timeout A time limit in millis.
         *
         * @return The content of the message.
         *
         * @throws Exception On failure.
         */
        @Nullable
        @CheckReturnValue
        Serializable receive(long timeout)
            throws Exception;
    }


    /**
     * Sender.
     */
    interface Sender
    {
        /**
         * Closes this.
         *
         * @throws Exception On failure.
         */
        void close()
            throws Exception;

        /**
         * Commits uncommitted messages.
         *
         * @throws Exception On failure.
         */
        void commit()
            throws Exception;

        /**
         * Opens this.
         *
         * @throws Exception On failure.
         */
        void open()
            throws Exception;

        /**
         * Sends a message.
         *
         * @param content The content of the message.
         *
         * @throws Exception On failure.
         */
        void send(@Nonnull Serializable content)
            throws Exception;
    }


    /**
     * Subscriber.
     */
    interface Subscriber
    {
        /**
         * Closes this.
         *
         * @throws Exception On failure.
         */
        void close()
            throws Exception;

        /**
         * Opens this.
         *
         * @throws Exception On failure.
         */
        void open()
            throws Exception;

        /**
         * Receives the content of a message.
         *
         * @param timeout A time limit in millis.
         *
         * @return The content of the message.
         *
         * @throws Exception On failure.
         */
        @Nullable
        @CheckReturnValue
        Serializable receive(long timeout)
            throws Exception;
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

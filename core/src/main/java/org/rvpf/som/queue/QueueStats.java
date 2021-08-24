/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QueueStats.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.som.queue;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.Stats;
import org.rvpf.base.StatsOwner;
import org.rvpf.service.ServiceMessages;
import org.rvpf.som.SOMStats;

/**
 * Queue stats.
 */
@ThreadSafe
public final class QueueStats
    extends SOMStats
{
    /**
     * Constructs an instance.
     *
     * @param statsOwner The stats owner.
     */
    QueueStats(@Nonnull final StatsOwner statsOwner)
    {
        super(statsOwner);
    }

    /** {@inheritDoc}
     */
    @Override
    public void buildText()
    {
        if ((getTransactionsKept() + getTransactionsRecovered()
                + getTransactionsDropped()) > 0) {
            addLine(
                ServiceMessages.QUEUE_TRANSACTIONS_KEPT,
                String.valueOf(getTransactionsKept()),
                String.valueOf(getTransactionsRecovered()),
                String.valueOf(getTransactionsDropped()));
            addLine(
                ServiceMessages.QUEUE_MESSAGES_KEPT,
                String.valueOf(getMessagesRecovered()));
        }

        addLine(
            ServiceMessages.QUEUE_RECEIVER_SESSIONS,
            String.valueOf(getReceiverSessionsOpened()),
            String.valueOf(getReceiverSessionsClosed()));
        addLine(
            ServiceMessages.QUEUE_SENDER_SESSIONS,
            String.valueOf(getSenderSessionsOpened()),
            String.valueOf(getSenderSessionsClosed()));

        addLine(
            ServiceMessages.QUEUE_RECEIVED_STATS,
            String.valueOf(getTransactionsReceived()),
            String.valueOf(getMessagesReceived()));
        addLine(
            ServiceMessages.QUEUE_SENT_STATS,
            String.valueOf(getTransactionsActive()),
            String.valueOf(getMessagesActive()));

        super.buildText();
    }

    /** {@inheritDoc}
     */
    @Override
    public Stats clone()
    {
        final QueueStats clone = (QueueStats) super.clone();

        clone._messagesActive = new AtomicInteger(getMessagesActive());
        clone._messagesReceived = new AtomicInteger(getMessagesReceived());
        clone._messagesRecovered = new AtomicInteger(getMessagesRecovered());
        clone._receiverSessionsClosed = new AtomicInteger(
            getReceiverSessionsClosed());
        clone._receiverSessionsOpened = new AtomicInteger(
            getReceiverSessionsOpened());
        clone._senderSessionsClosed = new AtomicInteger(
            getSenderSessionsClosed());
        clone._senderSessionsOpened = new AtomicInteger(
            getSenderSessionsOpened());
        clone._transactionsActive = new AtomicInteger(getTransactionsActive());
        clone._transactionsDropped = new AtomicInteger(
            getTransactionsDropped());
        clone._transactionsKept = new AtomicInteger(getTransactionsKept());
        clone._transactionsReceived = new AtomicInteger(
            getTransactionsReceived());
        clone._transactionsRecovered = new AtomicInteger(
            getTransactionsRecovered());

        return clone;
    }

    /**
     * Gets the number of active messages.
     *
     * @return The number of active messages.
     */
    @CheckReturnValue
    public int getMessagesActive()
    {
        return _messagesActive.get();
    }

    /**
     * Gets the number of active messages.
     *
     * @return The number of active messages.
     */
    @CheckReturnValue
    public int getMessagesReceived()
    {
        return _messagesReceived.get();
    }

    /**
     * Gets the number of messages recovered.
     *
     * @return The number of messages recovered.
     */
    @CheckReturnValue
    public int getMessagesRecovered()
    {
        return _messagesRecovered.get();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getObjectType()
    {
        return OBJECT_TYPE;
    }

    /**
     * Gets the number of receiver sessions closed.
     *
     * @return The number of receiver sessions closed.
     */
    @CheckReturnValue
    public int getReceiverSessionsClosed()
    {
        return _receiverSessionsClosed.get();
    }

    /**
     * Gets the number of receiver sessions opened.
     *
     * @return The number of receiver sessions opened.
     */
    @CheckReturnValue
    public int getReceiverSessionsOpened()
    {
        return _receiverSessionsOpened.get();
    }

    /**
     * Gets the number of sender sessions closed.
     *
     * @return The number of sender sessions closed.
     */
    @CheckReturnValue
    public int getSenderSessionsClosed()
    {
        return _senderSessionsClosed.get();
    }

    /**
     * Gets the number of sender sessions opened.
     *
     * @return The number of sender sessions opened.
     */
    @CheckReturnValue
    public int getSenderSessionsOpened()
    {
        return _senderSessionsOpened.get();
    }

    /**
     * Gets the number of active transactions.
     *
     * @return The number of active transactions.
     */
    @CheckReturnValue
    public int getTransactionsActive()
    {
        return _transactionsActive.get();
    }

    /**
     * Gets the number of transactions dropped.
     *
     * @return The number of transactions dropped.
     */
    @CheckReturnValue
    public int getTransactionsDropped()
    {
        return _transactionsDropped.get();
    }

    /**
     * Gets the number of transactions kept.
     *
     * @return The number of transactions kept.
     */
    @CheckReturnValue
    public int getTransactionsKept()
    {
        return _transactionsKept.get();
    }

    /**
     * Gets the number of transactions received.
     *
     * @return The number of transactions received.
     */
    @CheckReturnValue
    public int getTransactionsReceived()
    {
        return _transactionsReceived.get();
    }

    /**
     * Gets the number of transactions recovered.
     *
     * @return The number of transactions recovered.
     */
    @CheckReturnValue
    public int getTransactionsRecovered()
    {
        return _transactionsRecovered.get();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void substract(final Stats snapshot)
    {
        final QueueStats stats = (QueueStats) snapshot;

        _messagesRecovered.addAndGet(-stats.getMessagesRecovered());
        _receiverSessionsClosed.addAndGet(-stats.getReceiverSessionsClosed());
        _receiverSessionsOpened.addAndGet(-stats.getReceiverSessionsOpened());
        _senderSessionsClosed.addAndGet(-stats.getSenderSessionsClosed());
        _senderSessionsOpened.addAndGet(-stats.getSenderSessionsOpened());
        _transactionsDropped.addAndGet(-stats.getTransactionsDropped());
        _transactionsKept.addAndGet(-stats.getTransactionsKept());
        _transactionsReceived.addAndGet(-stats.getTransactionsReceived());
        _transactionsRecovered.addAndGet(-stats.getTransactionsRecovered());

        super.substract(snapshot);
    }

    /**
     * Called when a receiver session is closed.
     */
    void receiverSessionClosed()
    {
        _receiverSessionsClosed.incrementAndGet();
        updated();
    }

    /**
     * Called when a receiver session is opened.
     */
    void receiverSessionOpened()
    {
        _receiverSessionsOpened.incrementAndGet();
        updated();
    }

    /**
     * Called when the queue content recovery is completed.
     *
     * @param transactionsKept The number of transactions kept.
     * @param transactionsRecovered The number of transactions recovered.
     * @param transactionsDropped The number of transactions dropped.
     * @param messagesRecovered The number of messages recovered.
     */
    void recovered(
            final int transactionsKept,
            final int transactionsRecovered,
            final int transactionsDropped,
            final int messagesRecovered)
    {
        _transactionsKept.addAndGet(transactionsKept);
        _transactionsRecovered.addAndGet(transactionsRecovered);
        _transactionsDropped.addAndGet(transactionsDropped);
        _messagesRecovered.addAndGet(messagesRecovered);

        _transactionsActive.set(transactionsKept + transactionsRecovered);
        _messagesActive.set(messagesRecovered);
    }

    /**
     * Called when a sender session is closed.
     */
    void senderSessionClosed()
    {
        _senderSessionsClosed.incrementAndGet();
        updated();
    }

    /**
     * Called when a sender session is opened.
     */
    void senderSessionOpened()
    {
        _senderSessionsOpened.incrementAndGet();
        updated();
    }

    /**
     * Called when a transaction is received.
     *
     * @param messages The number of messages in the transaction.
     */
    void transactionReceived(final int messages)
    {
        _transactionsReceived.incrementAndGet();
        _messagesReceived.addAndGet(messages);

        _transactionsActive.incrementAndGet();
        _messagesActive.addAndGet(messages);

        updated();
    }

    /**
     * Called when transactions are sent.
     *
     * @param transactions The number of transactions.
     * @param messages The number of messages.
     */
    void transactionsSent(final int transactions, final int messages)
    {
        _transactionsActive.addAndGet(-transactions);
        _messagesActive.addAndGet(-messages);
        updated();
    }

    public static final String OBJECT_TYPE = "SOMQueue";
    private static final long serialVersionUID = 1L;

    private AtomicInteger _messagesActive = new AtomicInteger();
    private AtomicInteger _messagesReceived = new AtomicInteger();
    private AtomicInteger _messagesRecovered = new AtomicInteger();
    private AtomicInteger _receiverSessionsClosed = new AtomicInteger();
    private AtomicInteger _receiverSessionsOpened = new AtomicInteger();
    private AtomicInteger _senderSessionsClosed = new AtomicInteger();
    private AtomicInteger _senderSessionsOpened = new AtomicInteger();
    private AtomicInteger _transactionsActive = new AtomicInteger();
    private AtomicInteger _transactionsDropped = new AtomicInteger();
    private AtomicInteger _transactionsKept = new AtomicInteger();
    private AtomicInteger _transactionsReceived = new AtomicInteger();
    private AtomicInteger _transactionsRecovered = new AtomicInteger();
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

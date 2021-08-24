/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMReplicator.java 4057 2019-06-04 18:44:38Z SFB $
 */

package org.rvpf.store.server.som;

import java.io.Serializable;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.ReplicatedValue;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.som.SOMFactory;
import org.rvpf.service.som.SOMSender;
import org.rvpf.store.server.Replicator;

/**
 * SOM replicator.
 */
public final class SOMReplicator
    extends Replicator.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public synchronized boolean setUpImpl()
    {
        if (!isEnabled()) {
            return true;
        }

        final SOMFactory factory = new SOMFactory(getService().getConfig());

        for (final Partner partner: getPartners()) {
            final KeyedGroups queueProperties = ((SOMPartner) partner)
                .getQueueProperties();
            final SOMFactory.Queue factoryQueue = factory
                .createQueue(queueProperties);
            final SOMSender sender = factoryQueue.createSender(false);

            if (sender == null) {
                return false;
            }

            if (sender.isRemote()) {
                getThisLogger()
                    .warn(
                        ServiceMessages.REMOTE_SERVICE_WARNING,
                        sender.toString());
            }

            ((SOMPartner) partner).setSender(sender);
        }

        return super.setUpImpl();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void close(final Partner partner)
    {
        final Optional<SOMSender> sender = ((SOMPartner) partner).getSender();

        if (sender.isPresent()) {
            sender.get().tearDown();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void commit(final Partner partner)
        throws InterruptedException
    {
        final SOMSender sender = ((SOMPartner) partner).getSender().get();

        if (!sender.commit()) {
            getThisLogger().warn(ServiceMessages.SERVICE_CLOSED);

            throw new InterruptedException();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected SOMPartner newPartner(final KeyedGroups partnerProperties)
    {
        final KeyedGroups queueProperties = partnerProperties
            .getGroup(PARTNER_QUEUE_PROPERTIES);

        if (queueProperties.isMissing()) {
            getThisLogger()
                .error(
                    ServiceMessages.MISSING_PROPERTIES_IN,
                    PARTNER_QUEUE_PROPERTIES,
                    partnerProperties.getName().orElse(null));
        }

        return new SOMPartner(
            partnerProperties.getString(STORE_NAME_PROPERTY),
            queueProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean open(final Partner partner)
    {
        return ((SOMPartner) partner).getSender().get().open();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void replicate(
            final ReplicatedValue replicatedValue,
            final Partner partner)
        throws InterruptedException
    {
        final SOMSender sender = ((SOMPartner) partner).getSender().get();

        if (!sender.send(new Serializable[] {replicatedValue}, false)) {
            getThisLogger().warn(ServiceMessages.SERVICE_CLOSED);

            throw new InterruptedException();
        }
    }

    /** Partner queue properties. */
    public static final String PARTNER_QUEUE_PROPERTIES = "queue";

    /**
     * SOM partner.
     */
    private static final class SOMPartner
        extends Partner
    {
        /**
         * Constructs an instance.
         *
         * @param storeName The optional store name.
         * @param queueProperties The queue properties.
         */
        SOMPartner(
                @Nonnull final Optional<String> storeName,
                @Nonnull final KeyedGroups queueProperties)
        {
            super(storeName);

            _queueProperties = queueProperties;
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _sender.toString();
        }

        /**
         * Gets the queue properties.
         *
         * @return The queue properties.
         */
        @Nonnull
        @CheckReturnValue
        KeyedGroups getQueueProperties()
        {
            return _queueProperties;
        }

        /**
         * Gets the sender.
         *
         * @return The optional sender.
         */
        @Nonnull
        @CheckReturnValue
        Optional<SOMSender> getSender()
        {
            return Optional.ofNullable(_sender);
        }

        /**
         * Sets the sender.
         *
         * @param sender The sender.
         */
        void setSender(@Nonnull final SOMSender sender)
        {
            _sender = sender;
        }

        private final KeyedGroups _queueProperties;
        private SOMSender _sender;
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

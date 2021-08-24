/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RemoteEngineProxy.java 4018 2019-05-23 13:32:33Z SFB $
 */

package org.rvpf.processor.engine.executor.remote;

import java.io.Serializable;

import java.rmi.RemoteException;

import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Params;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.SessionClientContext;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.rmi.SessionProxy;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.LoginInfo;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.config.Config;

/**
 * Remote engine proxy.
 */
public class RemoteEngineProxy
    extends SessionProxy
    implements RemoteEngineSession
{
    /**
     * Constructs an instance.
     *
     * @param clientName A descriptive name for the client.
     * @param loginInfo The optional login informations.
     * @param context The session client context.
     * @param listener The optional listener.
     * @param autoconnect The autoconnect indicator.
     */
    RemoteEngineProxy(
            @Nonnull final String clientName,
            @Nonnull final Optional<LoginInfo> loginInfo,
            @Nonnull final SessionClientContext context,
            @Nonnull final Optional<Listener> listener,
            final boolean autoconnect)
    {
        super(clientName, loginInfo, context, listener, autoconnect);
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /** {@inheritDoc}
     */
    @Override
    public void disposeContext(
            final Serializable context)
        throws SessionException
    {
        try {
            getServerSession().disposeContext(Require.notNull(context));
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public List<PointValue> execute(
            ResultValue resultValue,
            String[] params,
            final Serializable context)
        throws SessionException
    {
        // Prepares for call to a server having no metadata.

        resultValue = resultValue.morph(Optional.empty());

        if (params.length == 0) {
            params = null;
        }

        // Executes on the remote server.

        try {
            return getServerSession().execute(resultValue, params, context);
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable newContext(
            final Params params,
            final Logger logger)
        throws SessionException
    {
        try {
            return getServerSession().newContext(params, logger);
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final String name,
            final Params params,
            final Config config,
            final Logger logger)
        throws SessionException
    {
        try {
            return getServerSession().setUp(name, params, null, logger);
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        try {
            getServerSession().tearDown();
        } catch (final Exception exception) {
            final SessionException sessionException = sessionException(
                exception);

            getThisLogger()
                .debug(
                    sessionException,
                    BaseMessages.VERBATIM,
                    exception.getMessage());
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected Session createSession()
        throws RemoteException, SessionException
    {
        return ((RemoteEngineFactory) getFactory())
            .createEngineSession(getContextUUID(), getClientName());
    }

    private RemoteEngineSession getServerSession()
        throws SessionException
    {
        return (RemoteEngineSession) getSession();
    }

    /**
     * Builder.
     */
    public static final class Builder
        extends SessionProxy.Builder
    {
        /** {@inheritDoc}
         */
        @Override
        public RemoteEngineProxy build()
        {
            if (!setUp()) {
                return null;
            }

            return new RemoteEngineProxy(
                getClientName(),
                getLoginInfo(),
                getContext(),
                getListener(),
                isAutoconnect());
        }
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RemoteEngineSessionImpl.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.processor.engine.executor.remote;

import java.io.Serializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.Params;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.config.Config;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.executor.EngineExecutor;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.rmi.ExportedSessionImpl;
import org.rvpf.service.rmi.SessionFactory;

/**
 * Remote engine session implementation.
 */
public final class RemoteEngineSessionImpl
    extends ExportedSessionImpl
    implements RemoteEngineSession
{
    RemoteEngineSessionImpl(
            @Nonnull final SessionFactory sessionFactory,
            @Nonnull final ConnectionMode connectionMode,
            @Nonnull final String clientName)
    {
        super(clientName, sessionFactory, connectionMode);
    }

    /** {@inheritDoc}
     */
    @Override
    public void disposeContext(Serializable context)
    {
        try {
            context = Require
                .notNull(
                    _contexts.remove(Require.notNull(context)),
                    ProcessorMessages.UNKNOWN_CONTEXT);
            _executor.disposeContext(context);
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public List<PointValue> execute(
            final ResultValue resultValue,
            String[] params,
            Serializable context)
        throws ServiceNotAvailableException, InterruptedException
    {
        try {
            securityCheck(RemoteEngineFactoryImpl.EXECUTE_ROLE);

            if (params == null) {
                params = _EMPTY_STRING_ARRAY;
            }

            context = Require
                .notNull(
                    _contexts.get(context),
                    ProcessorMessages.UNKNOWN_CONTEXT);

            return _executor.execute(resultValue, params, context);
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable newContext(Params params, final Logger logger)
    {
        try {
            Require.notNull(_executor, ProcessorMessages.SET_UP_NOT_COMPLETED);

            final Integer contextID;

            params = getSessionFactory().getConfig().substitute(params);
            contextID = Integer.valueOf(_nextContextID++);
            _contexts.put(contextID, _executor.newContext(params, logger));

            return contextID;
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final String name,
            Params params,
            final Config config,
            final Logger logger)
        throws SessionException
    {
        try {
            securityCheck(RemoteEngineFactoryImpl.EXECUTE_ROLE);

            params = getSessionFactory().getConfig().substitute(params);
            _logger = logger;

            final Optional<ClassDef> classDef = params
                .getClassDef(ENGINE_EXECUTOR_PARAM, Optional.empty());

            if (!classDef.isPresent()) {
                _logger
                    .error(
                        BaseMessages.MISSING_PARAMETER,
                        ENGINE_EXECUTOR_PARAM);

                return false;
            }

            _executor = classDef.get().createInstance(EngineExecutor.class);

            if (_executor == null) {
                return false;
            }

            return _executor
                .setUp(name, params, getSessionFactory().getConfig(), _logger);
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        try {
            if (_executor != null) {
                for (final Serializable context: _contexts.values()) {
                    _executor.disposeContext(context);
                }

                _contexts.clear();

                _executor.tearDown();
                _executor = null;
            }

            _logger = null;
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** EngineExecutor implementation parameter. */
    public static final String ENGINE_EXECUTOR_PARAM = "EngineExecutor";
    private static final String[] _EMPTY_STRING_ARRAY = new String[0];

    private final Map<Integer, Serializable> _contexts = new HashMap<Integer,
        Serializable>();
    private EngineExecutor _executor;
    private Logger _logger;
    private int _nextContextID;
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

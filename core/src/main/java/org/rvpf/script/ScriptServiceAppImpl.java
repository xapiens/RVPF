/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ScriptServiceAppImpl.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.script;

import java.io.File;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.service.app.ServiceAppImpl;

/**
 * Script service application implementation.
 */
public final class ScriptServiceAppImpl
    extends ServiceAppImpl
    implements ServiceThread.Target
{
    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        if (_runFile != null) {
            if (!_eval(_runFile, (_runText != null) || (_stopText != null))) {
                getService().fail();
            }
        }

        if (_runText != null) {
            if (!_eval(_runText, _stopText != null)) {
                getService().fail();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Service service)
    {
        if (!super.setUp(service)) {
            return false;
        }

        final Config config = service.getConfig();
        final KeyedGroups serviceProperties = config
            .getPropertiesGroup(SERVICE_PROPERTIES);
        final Optional<String> engineName = serviceProperties
            .getString(ENGINE_NAME_PROPERTY);
        final Optional<String> startFileName = serviceProperties
            .getString(START_FILE_PROPERTY);
        final Optional<String> runFileName = serviceProperties
            .getString(RUN_FILE_PROPERTY);

        _startFile = startFileName
            .isPresent()? new File(startFileName.get()): null;
        _startText = serviceProperties
            .getString(START_TEXT_PROPERTY)
            .orElse(null);
        _runFile = runFileName.isPresent()? new File(runFileName.get()): null;
        _runText = serviceProperties.getString(RUN_TEXT_PROPERTY).orElse(null);
        _stopText = serviceProperties
            .getString(STOP_TEXT_PROPERTY)
            .orElse(null);

        if (getThisLogger().isDebugEnabled()) {
            if (_startFile != null) {
                getThisLogger()
                    .debug(
                        ServiceMessages.SCRIPT_START_FILE,
                        _startFile.getAbsolutePath());
            }

            if (_startText != null) {
                getThisLogger()
                    .debug(ServiceMessages.SCRIPT_START_TEXT, _startText);
            }

            if (_runFile != null) {
                getThisLogger()
                    .debug(
                        ServiceMessages.SCRIPT_RUN_FILE,
                        _runFile.getAbsolutePath());
            }

            if (_runText != null) {
                getThisLogger()
                    .debug(ServiceMessages.SCRIPT_RUN_TEXT, _runText);
            }

            if (_stopText != null) {
                getThisLogger()
                    .debug(ServiceMessages.SCRIPT_STOP_TEXT, _stopText);
            }
        }

        if ((_startFile != null) && !_startFile.exists()) {
            getThisLogger()
                .error(
                    ServiceMessages.SCRIPT_START_FILE_UNKNOWN,
                    startFileName);

            return false;
        }

        if ((_runFile != null) && !_runFile.exists()) {
            getThisLogger()
                .error(ServiceMessages.SCRIPT_RUN_FILE_UNKNOWN, runFileName);

            return false;
        }

        _engineDriver = ScriptEngineDriver.newInstance(engineName);

        if (_engineDriver == null) {
            return false;
        }

        _engineDriver.bind(CONFIG_ATTRIBUTE, config);
        _engineDriver
            .bind(
                ENGINE_VERSION_PROPERTY,
                _engineDriver.getEngineFactory().getEngineVersion());

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        final String serviceName = getService().getServiceName();

        _engineDriver.start("Script driver for [" + serviceName + "]");

        if (_startFile != null) {
            if (!_eval(_startFile, true)) {
                getService().fail();
            }
        }

        if (_startText != null) {
            if (!_eval(_startText, true)) {
                getService().fail();
            }
        }

        if ((_runFile != null) || (_runText != null)) {
            final ServiceThread thread = new ServiceThread(
                this,
                "Script service");

            if (_thread.compareAndSet(null, thread)) {
                getThisLogger()
                    .debug(ServiceMessages.STARTING_THREAD, thread.getName());
                thread.start();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        if (_engineDriver.isRunning()) {
            if (_stopText != null) {
                _eval(_stopText, true);
            }

            _engineDriver.stop();
        }

        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            thread.interrupt();
            Require
                .ignored(
                    thread
                        .join(getThisLogger(), getService().getJoinTimeout()));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _engineDriver = null;

        super.tearDown();
    }

    private boolean _eval(
            @Nonnull final Object scriptObject,
            final boolean wait)
    {
        final ScriptEngineDriver engineDriver = _engineDriver;

        if (engineDriver == null) {
            return false;
        }

        final _EvalTask task = new _EvalTask(scriptObject, wait);

        if (!engineDriver.submit(task)) {
            if (Thread.currentThread().isInterrupted()) {
                getThisLogger()
                    .debug(
                        ServiceMessages.SCRIPT_EVAL_INTERRUPTED,
                        scriptObject);
            }

            return false;
        }

        return true;
    }

    /** Config attribute. */
    public static final String CONFIG_ATTRIBUTE = "CONFIG";

    /** Default interrupt delay (millis). */
    public static final int DEFAULT_INTERRUPT_DELAY = 60000;

    /** Engine name property. */
    public static final String ENGINE_NAME_PROPERTY = "engine.name";

    /** Engine version property. */
    public static final String ENGINE_VERSION_PROPERTY = "engine.version";

    /** Run file property. */
    public static final String RUN_FILE_PROPERTY = "file.run";

    /** Run text property. */
    public static final String RUN_TEXT_PROPERTY = "text.run";

    /** Script service. */
    public static final String SERVICE_PROPERTIES = "script.service";

    /** Start file property. */
    public static final String START_FILE_PROPERTY = "file.start";

    /** Start text property. */
    public static final String START_TEXT_PROPERTY = "text.start";

    /** Stop text property. */
    public static final String STOP_TEXT_PROPERTY = "text.stop";

    private volatile ScriptEngineDriver _engineDriver;
    private File _runFile;
    private String _runText;
    private File _startFile;
    private String _startText;
    private String _stopText;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();

    private static final class _EvalTask
        extends ScriptEngineDriver.EvalTask
    {
        /**
         * Constructs an instance.
         *
         * @param scriptObject
         * @param wait True for synchronous execution.
         */
        _EvalTask(@Nonnull final Object scriptObject, final boolean wait)
        {
            super(scriptObject);

            _wait = wait;
        }

        /** {@inheritDoc}
         */
        @Override
        protected boolean isAsync()
        {
            return !_wait;
        }

        private final boolean _wait;
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

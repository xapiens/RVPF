/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ScriptSink.java 4057 2019-06-04 18:44:38Z SFB $
 */

package org.rvpf.store.server.sink;

import java.io.File;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.exception.ServiceNotReadyException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.script.ScriptEngineDriver;
import org.rvpf.service.ServiceMessages;
import org.rvpf.store.server.StoreMessages;

/**
 * Script sink.
 */
public final class ScriptSink
    extends SinkModule.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        if (_engineDriver.isRunning()) {
            if (_stopText != null) {
                _eval(_stopText, null);
            }

            _engineDriver.stop();
        }

        super.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean delete(final VersionedValue versionedValue)
    {
        return (_delete != null)? (_eval(
            _delete,
            versionedValue) > 0): super.delete(versionedValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public void open()
        throws ServiceNotReadyException
    {
        super.open();

        _engineDriver
            .start("Script driver for ["
                   + getSinkAppImpl().getService().getServiceName() + "]");

        if (_startFile != null) {
            if (_eval(_startFile, null) < 0) {
                throw new ServiceNotReadyException();
            }
        }

        if (_startText != null) {
            if (_eval(_startText, null) < 0) {
                throw new ServiceNotReadyException();
            }
        }

        _delete = _compile(_deleteText);
        _update = _compile(_updateText);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final SinkServiceAppImpl sinkAppImpl)
    {
        if (!super.setUp(sinkAppImpl)) {
            return false;
        }

        final KeyedGroups scriptProperties = getSinkAppImpl()
            .getServerProperties()
            .getGroup(SCRIPT_PROPERTIES);

        if (scriptProperties.isMissing()) {
            getThisLogger()
                .error(ServiceMessages.MISSING_PROPERTIES, SCRIPT_PROPERTIES);

            return false;
        }

        final Optional<String> engineName = scriptProperties
            .getString(ENGINE_NAME_PROPERTY);

        _engineDriver = ScriptEngineDriver.newInstance(engineName);

        if (_engineDriver == null) {
            return false;
        }

        _engineDriver
            .bind(
                METADATA_ATTRIBUTE,
                getSinkAppImpl().getService().getMetadata());
        _engineDriver
            .bind(
                ENGINE_VERSION_PROPERTY,
                _engineDriver.getEngineFactory().getEngineVersion());

        final Optional<String> startFileName = scriptProperties
            .getString(START_FILE_PROPERTY);

        _startFile = startFileName
            .isPresent()? new File(startFileName.get()): null;
        _startText = scriptProperties
            .getString(START_TEXT_PROPERTY)
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
        }

        if ((_startFile != null) && !_startFile.exists()) {
            getThisLogger()
                .error(
                    ServiceMessages.SCRIPT_START_FILE_UNKNOWN,
                    startFileName);

            return false;
        }

        _deleteText = scriptProperties
            .getString(DELETE_TEXT_PROPERTY)
            .orElse(null);
        _updateText = scriptProperties
            .getString(UPDATE_TEXT_PROPERTY)
            .orElse(null);

        _stopText = scriptProperties.getString(STOP_TEXT_PROPERTY).orElse(null);

        if (getThisLogger().isDebugEnabled()) {
            if (_deleteText != null) {
                getThisLogger()
                    .debug(StoreMessages.SCRIPT_DELETE_TEXT, _deleteText);
            }

            if (_updateText != null) {
                getThisLogger()
                    .debug(StoreMessages.SCRIPT_UPDATE_TEXT, _updateText);
            }

            if (_stopText != null) {
                getThisLogger()
                    .debug(ServiceMessages.SCRIPT_STOP_TEXT, _stopText);
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean update(final VersionedValue versionedValue)
    {
        return (_update != null)? (_eval(
            _update,
            versionedValue) > 0): super.update(versionedValue);
    }

    private Object _compile(final Object scriptObject)
    {
        final ScriptEngineDriver.CompileTask task =
            new ScriptEngineDriver.CompileTask(
                scriptObject);

        if (!_engineDriver.submit(task)) {
            if (Thread.currentThread().isInterrupted()) {
                getThisLogger()
                    .warn(ServiceMessages.SCRIPT_COMPILE_INTERRUPTED);
            }

            return null;
        }

        return task.getCompiled().orElse(null);
    }

    private int _eval(
            final Object scriptObject,
            final VersionedValue versionedValue)
    {
        final _EvalTask task = new _EvalTask(scriptObject, versionedValue);

        if (!_engineDriver.submit(task)) {
            if (Thread.currentThread().isInterrupted()) {
                getThisLogger()
                    .debug(
                        ServiceMessages.SCRIPT_EVAL_INTERRUPTED,
                        scriptObject);
            }

            return -1;
        }

        return (versionedValue != null)? task.getUpdateCount(): 0;
    }

    /** Delete text property. */
    public static final String DELETE_TEXT_PROPERTY = "text.delete";

    /** Engine name property. */
    public static final String ENGINE_NAME_PROPERTY = "engine.name";

    /** Engine version property. */
    public static final String ENGINE_VERSION_PROPERTY = "engine.version";

    /** Metadata attribute. */
    public static final String METADATA_ATTRIBUTE = "METADATA";

    /** The script properties group. */
    public static final String SCRIPT_PROPERTIES = "script";

    /** Start file property. */
    public static final String START_FILE_PROPERTY = "file.start";

    /** Start text property. */
    public static final String START_TEXT_PROPERTY = "text.start";

    /** Stop text property. */
    public static final String STOP_TEXT_PROPERTY = "text.stop";

    /** Update argument attribute. */
    public static final String UPDATE_ARG_ATTRIBUTE = "UPDATE_ARG";

    /** Update count attribute. */
    public static final String UPDATE_COUNT_ATTRIBUTE = "UPDATE_COUNT";

    /** Update text property. */
    public static final String UPDATE_TEXT_PROPERTY = "text.update";

    private Object _delete;
    private String _deleteText;
    private ScriptEngineDriver _engineDriver;
    private File _startFile;
    private String _startText;
    private String _stopText;
    private Object _update;
    private String _updateText;

    private static final class _EvalTask
        extends ScriptEngineDriver.EvalTask
    {
        /**
         * Constructs an instance.
         *
         * @param scriptObject A script object.
         * @param versionedValue An update value.
         */
        _EvalTask(
                @Nonnull final Object scriptObject,
                @Nullable final VersionedValue versionedValue)
        {
            super(scriptObject);

            _updateValue = versionedValue;
        }

        /** {@inheritDoc}
         */
        @Override
        protected void execute(final ScriptEngineDriver driver)
        {
            if (_updateValue != null) {
                driver.bind(UPDATE_ARG_ATTRIBUTE, _updateValue);
            }

            super.execute(driver);

            if (_updateValue != null) {
                _updateCount = driver.bound(UPDATE_COUNT_ATTRIBUTE);
                driver.unbind(UPDATE_ARG_ATTRIBUTE);
            }
        }

        /**
         * Gets the update count.
         *
         * @return The update count.
         */
        @CheckReturnValue
        int getUpdateCount()
        {
            if (_updateCount.isPresent()) {
                final Object updateCount = _updateCount.get();

                if (updateCount instanceof Number) {
                    return ((Number) updateCount).intValue();
                }

                try {
                    return Integer.parseInt(updateCount.toString());
                } catch (final NumberFormatException exception) {
                    _getLogger().warn(StoreMessages.UPDATE_COUNT_BAD);

                    return 0;
                }
            }

            _getLogger().warn(StoreMessages.UPDATE_COUNT_UNKNOWN);
            Thread.dumpStack();

            return 0;
        }

        private Logger _getLogger()
        {
            return Logger.getInstance(getClass());
        }

        private Optional<Object> _updateCount = Optional.empty();
        private final VersionedValue _updateValue;
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

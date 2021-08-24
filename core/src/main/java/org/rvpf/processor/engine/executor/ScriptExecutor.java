/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ScriptExecutor.java 4056 2019-06-04 15:21:02Z SFB $
 */

package org.rvpf.processor.engine.executor;

import java.io.File;
import java.io.Serializable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Params;
import org.rvpf.base.exception.ServiceNotReadyException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.script.ScriptEngineDriver;
import org.rvpf.service.ServiceMessages;

/**
 * Script executor.
 */
public class ScriptExecutor
    implements EngineExecutor
{
    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        if (_engineDriver != null) {
            if (_stopText != null) {
                _eval(_stopText, Optional.empty());
            }

            _engineDriver.stop();
            _engineDriver = null;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void disposeContext(final Serializable context)
    {
        if (_engineDriver != null) {
            _eval(((_Context) context).stopText, Optional.empty());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public List<PointValue> execute(
            final ResultValue resultValue,
            final String[] params,
            final Serializable context)
        throws InterruptedException, ServiceNotReadyException
    {
        final _Context scriptContext = (_Context) context;

        final Map<String, Object> bindings = new HashMap<String, Object>();
        final List<PointValue> results = new LinkedList<PointValue>();

        results.add(resultValue);
        bindings.put(RESULT_ATTRIBUTE, resultValue);
        bindings.put(RESULTS_ATTRIBUTE, results);
        bindings.put(INPUTS_ATTRIBUTE, resultValue.getInputValues());
        bindings
            .put(
                TRANSFORM_PARAMS_ATTRIBUTE,
                Arrays.asList(scriptContext.params));
        bindings.put(POINT_PARAMS_ATTRIBUTE, Arrays.asList(params));

        _activateContext(scriptContext);

        if (!_eval(scriptContext.apply, Optional.of(bindings))) {
            throw new ServiceNotReadyException();
        }

        return results;
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable newContext(final Params params, final Logger logger)
    {
        final _Context context = new _Context();

        context.startFile = params.getString(START_FILE_PARAM).orElse(null);
        context.startText = params.getString(START_TEXT_PARAM).orElse(null);

        if (logger.isDebugEnabled()) {
            if (context.startFile != null) {
                logger
                    .debug(
                        ServiceMessages.SCRIPT_START_FILE,
                        context.startFile);
            }

            if (context.startText != null) {
                logger
                    .debug(
                        ServiceMessages.SCRIPT_START_TEXT,
                        context.startText);
            }
        }

        context.applyText = params.getString(APPLY_TEXT_PARAM).orElse(null);

        if (context.applyText == null) {
            logger.error(ProcessorMessages.SCRIPT_APPLY_TEXT_MISSING);

            return null;
        }

        context.stopText = params.getString(STOP_TEXT_PARAM).orElse(null);

        if (context.stopText != null) {
            logger.debug(ServiceMessages.SCRIPT_STOP_TEXT, context.stopText);
        }

        context.params = params.getStrings(Transform.PARAM_PARAM);

        context.logger = logger;

        return context;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final String name,
            final Params params,
            final Config config,
            final Logger logger)
    {
        _logger = logger;
        _name = name;
        _engineName = params.getString(ENGINE_NAME_PARAM).orElse(null);

        final Optional<String> startFileName = params
            .getString(START_FILE_PARAM);

        _startFile = startFileName
            .isPresent()? new File(startFileName.get()): null;
        _startText = params.getString(START_TEXT_PARAM).orElse(null);

        if (_logger.isDebugEnabled()) {
            if (_startFile != null) {
                _logger
                    .debug(
                        ServiceMessages.SCRIPT_START_FILE,
                        _startFile.getAbsolutePath());
            }

            if (_startText != null) {
                _logger.debug(ServiceMessages.SCRIPT_START_TEXT, _startText);
            }
        }

        _stopText = params.getString(STOP_TEXT_PARAM).orElse(null);

        if (_stopText != null) {
            _logger.debug(ServiceMessages.SCRIPT_STOP_TEXT, _stopText);
        }

        _metadata = (config instanceof Metadata)? (Metadata) config: null;

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        close();
    }

    private void _activateContext(
            final _Context context)
        throws ServiceNotReadyException
    {
        if (context.apply == null) {
            if (_engineDriver == null) {
                _engineDriver = ScriptEngineDriver
                    .newInstance(Optional.ofNullable(_engineName));

                if (_engineDriver == null) {
                    throw new ServiceNotReadyException();
                }

                _engineDriver.bind(METADATA_ATTRIBUTE, _metadata);
                _engineDriver
                    .bind(
                        ENGINE_VERSION_PROPERTY,
                        _engineDriver.getEngineFactory().getEngineVersion());

                if ((_startFile != null) && !_startFile.exists()) {
                    _logger
                        .error(
                            ServiceMessages.SCRIPT_START_FILE_UNKNOWN,
                            _startFile.getAbsolutePath());

                    throw new ServiceNotReadyException();
                }

                _engineDriver
                    .start("Script driver for the '" + _name + "' engine");

                if (!_eval(_startFile, Optional.empty())) {
                    _logger.error(ProcessorMessages.SCRIPT_START_FILE_FAILED);

                    throw new ServiceNotReadyException();
                }

                if (!_eval(_startText, Optional.empty())) {
                    _logger.error(ProcessorMessages.SCRIPT_START_TEXT_FAILED);

                    throw new ServiceNotReadyException();
                }
            }

            final File startFile = (context.startFile != null)? new File(
                context.startFile): null;

            if ((startFile != null) && !startFile.exists()) {
                context.logger
                    .error(
                        ServiceMessages.SCRIPT_START_FILE_UNKNOWN,
                        startFile.getAbsolutePath());

                throw new ServiceNotReadyException();
            }

            if (!_eval(startFile, Optional.empty())) {
                context.logger
                    .error(ProcessorMessages.SCRIPT_START_FILE_FAILED);

                throw new ServiceNotReadyException();
            }

            if (!_eval(context.startText, Optional.empty())) {
                context.logger
                    .error(ProcessorMessages.SCRIPT_START_TEXT_FAILED);

                throw new ServiceNotReadyException();
            }

            context.apply = _compile(context.applyText);
        }
    }

    private Object _compile(final Object scriptObject)
    {
        final ScriptEngineDriver.CompileTask task =
            new ScriptEngineDriver.CompileTask(
                scriptObject);

        if (!_engineDriver.submit(task)) {
            if (Thread.currentThread().isInterrupted()) {
                _logger
                    .warn(
                        ServiceMessages.SCRIPT_COMPILE_INTERRUPTED,
                        scriptObject);
            }

            return null;
        }

        return task.getCompiled().orElse(null);
    }

    private boolean _eval(
            final Object scriptObject,
            final Optional<Map<String, Object>> bindings)
    {
        if (scriptObject != null) {
            final _EvalTask task = new _EvalTask(scriptObject, bindings);

            if (!_engineDriver.submit(task)) {
                if (Thread.currentThread().isInterrupted()) {
                    _logger
                        .debug(
                            ServiceMessages.SCRIPT_EVAL_INTERRUPTED,
                            scriptObject);
                }

                return false;
            }
        }

        return true;
    }

    /** The script text used to apply the transform. */
    public static final String APPLY_TEXT_PARAM = "ApplyText";

    /** The name of the script engine. */
    public static final String ENGINE_NAME_PARAM = "ScriptEngineName";

    /** Engine version property. */
    public static final String ENGINE_VERSION_PROPERTY = "engine.version";

    /** Inputs attribute. */
    public static final String INPUTS_ATTRIBUTE = "INPUTS";

    /** Metadata attribute. */
    public static final String METADATA_ATTRIBUTE = "METADATA";

    /** Point params attribute. */
    public static final String POINT_PARAMS_ATTRIBUTE = "POINT_PARAMS";

    /** Results attribute. */
    public static final String RESULTS_ATTRIBUTE = "RESULTS";

    /** Result attribute. */
    public static final String RESULT_ATTRIBUTE = "RESULT";

    /** The script file used to start the engine. */
    public static final String START_FILE_PARAM = "StartFile";

    /** The script text used to start the engine. */
    public static final String START_TEXT_PARAM = "StartText";

    /** The script text used to stop the engine. */
    public static final String STOP_TEXT_PARAM = "StopText";

    /** Transform params attribute. */
    public static final String TRANSFORM_PARAMS_ATTRIBUTE = "TRANSFORM_PARAMS";
    private static final long serialVersionUID = 1L;

    private ScriptEngineDriver _engineDriver;
    private String _engineName;
    private Logger _logger;
    private Metadata _metadata;
    private String _name;
    private File _startFile;
    private String _startText;
    private String _stopText;

    /**
     * Context.
     */
    private static final class _Context
        implements Serializable
    {
        /**
         * Constructs an instance.
         */
        _Context() {}

        private static final long serialVersionUID = 1L;

        transient Object apply;
        String applyText;
        Logger logger;
        String[] params;
        String startFile;
        String startText;
        String stopText;
    }


    /**
     * Eval task.
     */
    private static final class _EvalTask
        extends ScriptEngineDriver.EvalTask
    {
        /**
         * Constructs an instance.
         *
         * @param scriptObject A script object.
         * @param bindings An optional update value.
         */
        _EvalTask(
                @Nonnull final Object scriptObject,
                @Nonnull final Optional<Map<String, Object>> bindings)
        {
            super(scriptObject);

            _bindings = bindings.orElse(null);
        }

        /** {@inheritDoc}
         */
        @Override
        protected void execute(
                final ScriptEngineDriver driver)
            throws ScriptEngineDriver.ExecuteException
        {
            if (_bindings != null) {
                driver.bind(_bindings);
            }

            super.execute(driver);

            if (_bindings != null) {
                driver.unbind(_bindings);
            }
        }

        private final Map<String, Object> _bindings;
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

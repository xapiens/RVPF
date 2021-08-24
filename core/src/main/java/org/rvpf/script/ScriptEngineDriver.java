/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ScriptEngineDriver.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;

import java.nio.charset.StandardCharsets;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ThreadExecutor;

/**
 * Script engine driver.
 */
public final class ScriptEngineDriver
{
    /**
     * Constructs an instance.
     *
     * @param engine The script engine.
     */
    private ScriptEngineDriver(final ScriptEngine engine)
    {
        final ScriptEngineFactory engineFactory = engine.getFactory();

        _LOGGER
            .info(
                ServiceMessages.SCRIPT_ENGINE,
                engineFactory.getEngineName(),
                engineFactory.getNames(),
                engineFactory.getEngineVersion());
        _LOGGER
            .info(
                ServiceMessages.SCRIPT_LANGUAGE,
                engineFactory.getLanguageName(),
                engineFactory.getLanguageVersion());

        _engine = engine;
    }

    /**
     * Returns a new instance.
     *
     * @param optionalEngineName The optional engine name.
     *
     * @return The new instance (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public static ScriptEngineDriver newInstance(
            @Nonnull final Optional<String> optionalEngineName)
    {
        final String engineName = optionalEngineName
            .orElse(DEFAULT_ENGINE_NAME);
        final ScriptEngineManager engineManager = new ScriptEngineManager();
        final ScriptEngine engine = engineManager.getEngineByName(engineName);

        if (engine == null) {
            _LOGGER.error(ServiceMessages.SCRIPT_ENGINE_UNKNOWN, engineName);

            return null;
        }

        return new ScriptEngineDriver(engine);
    }

    /**
     * Binds all from additional bindings.
     *
     * @param bindings The bindings to be added.
     */
    public synchronized void bind(@Nonnull final Map<String, Object> bindings)
    {
        _bindings.putAll(bindings);
    }

    /**
     * Binds a key to a value.
     *
     * @param key The key.
     * @param value The value.
     */
    public synchronized void bind(
            @Nonnull final String key,
            @Nonnull final Object value)
    {
        _bindings.put(key, value);
    }

    /**
     * Returns the bound value for a key.
     *
     * @param key The key.
     *
     * @return The bound value (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    public synchronized Optional<Object> bound(@Nonnull final String key)
    {
        return Optional.ofNullable(_bindings.get(key));
    }

    /**
     * Gets the script engine.
     *
     * @return The script engine.
     */
    @Nonnull
    @CheckReturnValue
    public ScriptEngineFactory getEngineFactory()
    {
        return _engine.getFactory();
    }

    /**
     * Asks if running.
     *
     * @return True if running.
     */
    @CheckReturnValue
    public boolean isRunning()
    {
        return !_executor.isShutdown();
    }

    /**
     * Starts the background thread.
     *
     * @param threadName The name of the thread.
     */
    public void start(@Nonnull final String threadName)
    {
        _executor.reset(Optional.of(threadName), true);
        _executor.startThread();
    }

    /**
     * Stops the background thread.
     */
    public void stop()
    {
        _executor.shutdownNow();
    }

    /**
     * Submits a task.
     *
     * @param task The task.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean submit(@Nonnull final Task task)
    {
        synchronized (this) {
            if (_asyncTask != null) {
                if (_asyncTask.cancel(true)) {
                    _LOGGER.warn(ServiceMessages.SCRIPT_CANCELLED);
                }

                _asyncTask = null;
            }
        }

        final Future<?> future;

        try {
            future = _executor
                .submit(() -> task.execute(ScriptEngineDriver.this));
        } catch (final RejectedExecutionException exception) {
            return false;
        }

        if (task.isAsync()) {
            synchronized (this) {
                _asyncTask = future;
            }
        } else {
            try {
                future.get();
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();

                return false;
            } catch (final ExecutionException exception) {
                throw new RuntimeException(exception);
            }
        }

        return true;
    }

    /**
     * Unbinds all from additional bindings.
     *
     * @param bindings The bindings to be removed.
     */
    public synchronized void unbind(@Nonnull final Map<String, Object> bindings)
    {
        _bindings.clear();
    }

    /**
     * Unbinds a key.
     *
     * @param key The key.
     */
    public synchronized void unbind(@Nonnull final String key)
    {
        _bindings.remove(key);
    }

    /**
     * Compiles an object.
     *
     * @param scriptObject
     *
     * @return The compiled script object (empty when not compilable).
     *
     * @throws ScriptException On script exception.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Object> _compile(
            @Nonnull Object scriptObject)
        throws ScriptException
    {
        if (!(_engine instanceof Compilable)) {
            return Optional.empty();
        }

        if (scriptObject instanceof File) {
            scriptObject = _newScriptReader((File) scriptObject);
        }

        final Object compiled;

        if (scriptObject instanceof Reader) {
            compiled = ((Compilable) _engine).compile((Reader) scriptObject);
        } else {
            compiled = ((Compilable) _engine)
                .compile(String.valueOf(scriptObject));
        }

        return Optional.of(compiled);
    }

    /**
     * Evaluates an object.
     *
     * @param scriptObject The script object.
     *
     * @throws ScriptException On script exception.
     */
    void _eval(@Nonnull Object scriptObject)
        throws ScriptException
    {
        if (scriptObject instanceof CompiledScript) {
            ((CompiledScript) scriptObject).eval();
        } else {
            if (scriptObject instanceof File) {
                scriptObject = _newScriptReader((File) scriptObject);
            }

            if (scriptObject instanceof Reader) {
                _engine.eval((Reader) scriptObject);
            } else {
                _engine.eval(String.valueOf(scriptObject));
            }
        }
    }

    void _updateContext()
    {
        ScriptContext context = _engine.getContext();

        if (context == null) {
            context = new SimpleScriptContext();
            _engine.setContext(context);
        }

        Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);

        if (bindings == null) {
            bindings = _engine.createBindings();
            context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        }

        synchronized (this) {
            bindings.putAll(_bindings);
            _bindings = bindings;
        }
    }

    private static Reader _newScriptReader(final File scriptFile)
    {
        try {
            return new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(scriptFile),
                    StandardCharsets.UTF_8));
        } catch (final FileNotFoundException exception) {
            throw new RuntimeException(exception);
        }
    }

    /** Default engine name. */
    public static final String DEFAULT_ENGINE_NAME = "ECMAScript";
    private static final Logger _LOGGER = Logger
        .getInstance(ScriptEngineDriver.class);

    private Future<?> _asyncTask;
    private Bindings _bindings = new SimpleBindings();
    private final ScriptEngine _engine;
    private final _ScriptThreadExecutor _executor = new _ScriptThreadExecutor();

    /**
     * Compile task.
     */
    public static class CompileTask
        extends Task
    {
        /**
         * Constructs an instance.
         *
         * @param scriptObject A script object.
         */
        public CompileTask(@Nonnull final Object scriptObject)
        {
            _scriptObject = Require.notNull(scriptObject);
        }

        /**
         * Gets the compiled object.
         *
         * @return The compiled object.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<Object> getCompiled()
        {
            return _compiled;
        }

        /** {@inheritDoc}
         */
        @Override
        protected void execute(final ScriptEngineDriver driver)
        {
            try {
                _compiled = driver._compile(_scriptObject);
            } catch (final ScriptException exception) {
                throw new ExecuteException(exception);
            }
        }

        private Optional<Object> _compiled = Optional.empty();
        private final Object _scriptObject;
    }


    /**
     * Eval task.
     */
    public static class EvalTask
        extends Task
    {
        /**
         * Constructs an instance.
         *
         * @param scriptObject A script object.
         */
        public EvalTask(@Nonnull final Object scriptObject)
        {
            _scriptObject = Require.notNull(scriptObject);
        }

        /** {@inheritDoc}
         */
        @Override
        protected void execute(final ScriptEngineDriver driver)
        {
            try {
                driver._eval(_scriptObject);
            } catch (final ScriptException exception) {
                throw new ExecuteException(exception);
            }
        }

        private final Object _scriptObject;
    }


    /**
     * Execute exception.
     */
    public static final class ExecuteException
        extends RuntimeException
    {
        /**
         * Constructs an instance.
         *
         * @param cause The exception's cause.
         */
        ExecuteException(@Nonnull final Throwable cause)
        {
            super(cause);
        }

        private static final long serialVersionUID = 1L;
    }


    /**
     * Task.
     */
    public abstract static class Task
    {
        /**
         * Executes.
         *
         * @param driver The script engine driver.
         */
        protected void execute(@Nonnull final ScriptEngineDriver driver) {}

        /**
         * Asks if this task is asynchrounous.
         *
         * @return True if this task is asynchrounous.
         */
        @CheckReturnValue
        protected boolean isAsync()
        {
            return false;
        }
    }


    /**
     * _Script thread executor.
     */
    private class _ScriptThreadExecutor
        extends ThreadExecutor
    {
        /**
         * Constructs an instance.
         */
        _ScriptThreadExecutor()
        {
            super(Optional.empty());
        }

        /** {@inheritDoc}
         */
        @Override
        public void run()
        {
            _updateContext();

            super.run();
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

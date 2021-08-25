/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Launcher.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.jnlp.launcher;

import java.io.File;
import java.io.FileNotFoundException;

import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The Launcher class implements a subset of the JNLP specification for
 * applications running as background services.
 *
 * <h1>Limitations</h1>
 *
 * <ul>
 *   <li>Offline mode is always enabled.</li>
 *   <li>No sandbox environment.</li>
 *   <li>Resources must always be signed.</li>
 *   <li>Update check is done only at launch time, with a timeout.</li>
 *   <li>JRE update is not supported.</li>
 *   <li>Only the 'application-desc' application descriptor is supported.</li>
 *   <li>The application always runs inside the same VM.</li>
 *   <li>Download is always 'eager'.</li>
 *   <li>Version-base download is not supported.</li>
 *   <li>Extension resources are not supported.</li>
 * </ul>
 */
@ThreadSafe
public final class Launcher
    implements Runnable, Thread.UncaughtExceptionHandler
{
    /**
     * Constructs an instance.
     */
    private Launcher() {}

    /**
     * Main program entry.
     *
     * <p>The first argument must be the URL of the JNLP file. The remaining
     * arguments will be supplied to the application's main before those
     * specified in the application descriptor.</p>
     *
     * @param args The launcher's arguments.
     */
    public static void main(@Nonnull final String[] args)
    {
        final Launcher launcher = new Launcher();

        Thread.setDefaultUncaughtExceptionHandler(launcher);

        try {
            launcher._launch(args);
        } catch (final Throwable throwable) {
            Thread
                .getDefaultUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), throwable);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        try {
            final URL loaderJar = _getJarURL(
                LOADER_JAR_PROPERTY,
                DEFAULT_LOADER_JAR);

            Thread
                .currentThread()
                .setContextClassLoader(
                    new URLClassLoader(new URL[] {loaderJar}));

            final Loader loader = (Loader) Class
                .forName(
                    System
                        .getProperty(
                                LOADER_CLASS_PROPERTY,
                                        DEFAULT_LOADER_CLASS),
                    true,
                    Thread.currentThread().getContextClassLoader())
                .getConstructor()
                .newInstance();

            if (loader.loadJNLP(_jnlpURL)) {
                _urls = loader.getURLs();
                _mainClassName = loader.getMainClassName();
                _arguments = loader.getArguments();
                _properties = loader.getProperties();
                _success = true;
            }
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void uncaughtException(
            final Thread thread,
            final Throwable throwable)
    {
        final Logger logger = Logger.getLogger(getClass().getName());

        logger
            .log(
                Level.SEVERE,
                "Exception in thread \"" + thread.getName() + "\": ",
                throwable);
    }

    private static URL _getJarURL(
            final String jarProperty,
            final String defaultJar)
        throws Exception
    {
        final String name = System.getProperty(jarProperty, defaultJar);
        final File file = new File(name);

        if (!file.isFile()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }

        return file.toURI().toURL();
    }

    private void _launch(final String[] args)
        throws Exception
    {
        _jnlpURL = (args.length > 0)? Optional
            .of(new URL(args[0])): Optional.empty();

        final Thread thread = new Thread(this);

        thread.start();
        thread.join();

        if (_success) {
            final ClassLoader classLoader = new URLClassLoader(_urls);

            Thread.currentThread().setContextClassLoader(classLoader);

            final Class<?> mainClass = Class
                .forName(
                    _mainClassName,
                    true,
                    Thread.currentThread().getContextClassLoader());
            final Method mainMethod = mainClass
                .getMethod("main", String[].class);
            final String[] jnlpArgs = _arguments;
            final String[] arguments;

            arguments = new String[(args.length - 1) + jnlpArgs.length];
            System.arraycopy(args, 1, arguments, 0, args.length - 1);
            System
                .arraycopy(
                    jnlpArgs,
                    0,
                    arguments,
                    args.length - 1,
                    jnlpArgs.length);

            for (final Map.Entry<String, String> entry:
                    _properties.entrySet()) {
                System.setProperty(entry.getKey(), entry.getValue());
            }

            mainMethod.invoke(null, new Object[] {arguments});
        }
    }

    /** Default loader class property. */
    public static final String DEFAULT_LOADER_CLASS =
        "org.rvpf.jnlp.loader.LoaderImpl";

    /** Default loader Jar property. */
    public static final String DEFAULT_LOADER_JAR = "lib/rvpf-jnlp-loader.jar";

    /** Loader class property. */
    public static final String LOADER_CLASS_PROPERTY = "rvpf.jnlp.loader.class";

    /** Loader Jar property. */
    public static final String LOADER_JAR_PROPERTY = "rvpf.jnlp.loader.jar";

    private String[] _arguments;
    private Optional<URL> _jnlpURL;
    private String _mainClassName;
    private Map<String, String> _properties;
    private boolean _success;
    private URL[] _urls;
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

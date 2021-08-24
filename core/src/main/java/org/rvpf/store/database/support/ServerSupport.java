/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServerSupport.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.database.support;

import java.io.File;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.store.server.the.sql.dialect.DialectSupport;

/**
 * Server support.
 */
public interface ServerSupport
{
    /**
     * Gets the connection URL.
     *
     * @return The connection URL.
     */
    @Nonnull
    @CheckReturnValue
    String getConnectionURL();

    /**
     * Gets a dialect support object.
     *
     * <p>Used by tests procedures.</p>
     *
     * @return The dialect support object.
     */
    @Nonnull
    @CheckReturnValue
    DialectSupport getDialectSupport();

    /**
     * Sets up the database server.
     *
     * @param supportProperties The support configuration properties.
     * @param dataDir The database data directory.
     *
     * @return True on success.
     *
     * @throws Exception When appropriate.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull KeyedGroups supportProperties,
            @Nonnull File dataDir)
        throws Exception;

    /**
     * Starts the database server.
     */
    void start();

    /**
     * Stops the database server.
     */
    void stop();

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Abstract server support.
     */
    @NotThreadSafe
    abstract class Abstract
        implements ServerSupport
    {
        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(
                final KeyedGroups supportProperties,
                final File dataDir)
        {
            Require.notNull(supportProperties);
            Require.notNull(dataDir);

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public synchronized void tearDown()
        {
            if (_properties != null) {
                final Iterable<String> keys = new ArrayList<String>(
                    _properties.keySet());

                for (final String key: keys) {
                    final String value = _properties.get(key);

                    if (value != null) {
                        System.setProperty(key, value);
                    } else if (System.getProperty(key) != null) {
                        System.clearProperty(key);
                    }

                    _properties.remove(key);
                }

                _properties = null;
            }
        }

        /**
         * Gets a method for a class.
         *
         * @param objectClass The object class.
         * @param methodName The method name.
         * @param parameterTypes The parameter types.
         *
         * @return The method.
         */
        @Nonnull
        @CheckReturnValue
        protected static Method getMethod(
                @Nonnull final Class<?> objectClass,
                @Nonnull final String methodName,
                final Class<?>... parameterTypes)
        {
            try {
                return objectClass.getMethod(methodName, parameterTypes);
            } catch (final NoSuchMethodException|SecurityException exception) {
                throw new RuntimeException(exception);
            }
        }

        /**
         * Gets a method for an object instance.
         *
         * @param instance The object instance.
         * @param methodName The method name.
         * @param parameterTypes The parameter types.
         *
         * @return The method.
         */
        @Nonnull
        @CheckReturnValue
        protected static Method getMethod(
                @Nonnull final Object instance,
                @Nonnull final String methodName,
                final Class<?>... parameterTypes)
        {
            return getMethod(instance.getClass(), methodName, parameterTypes);
        }

        /**
         * Invokes a static method.
         *
         * @param method The static method.
         * @param args The arguments for the method.
         *
         * @return The optional result object.
         */
        @Nonnull
        protected static Optional<Object> invoke(
                @Nonnull final Method method,
                @Nonnull final Object... args)
        {
            try {
                return Optional.ofNullable(method.invoke(null, args));
            } catch (final IllegalAccessException|IllegalArgumentException
                     |InvocationTargetException exception) {
                throw new RuntimeException(exception);
            }
        }

        /**
         * Invokes a method on an object instance.
         *
         * @param instance The object instance.
         * @param method The method.
         * @param args The arguments for the method.
         *
         * @return The optional result object.
         */
        @Nonnull
        protected static Optional<Object> invoke(
                @Nonnull final Object instance,
                @Nonnull final Method method,
                @Nonnull final Object... args)
        {
            try {
                return Optional
                    .ofNullable(method.invoke(Require.notNull(instance), args));
            } catch (final IllegalAccessException|IllegalArgumentException
                     |InvocationTargetException exception) {
                throw new RuntimeException(exception);
            }
        }

        /**
         * Sets a system property.
         *
         * @param key The property key.
         * @param value The optional property value.
         */
        protected static synchronized void setSystemProperty(
                @Nonnull final String key,
                @Nonnull final Optional<String> value)
        {
            final String previousValue;

            if (value.isPresent()) {
                previousValue = System.setProperty(key, value.get());
            } else {
                previousValue = System.clearProperty(key);
            }

            if (_properties == null) {
                _properties = new HashMap<String, String>();
            }

            if (!_properties.containsKey(key)) {
                _properties.put(key, previousValue);
            }
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        protected final Logger getThisLogger()
        {
            return _logger;
        }

        private static Map<String, String> _properties;

        private final Logger _logger = Logger.getInstance(getClass());
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

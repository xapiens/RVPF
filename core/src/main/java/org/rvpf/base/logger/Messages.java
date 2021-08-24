/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Messages.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.logger;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.BaseMessages;

/**
 * Provides just in time cached access to message resources.
 *
 * <p>Message resources are represented by Enum types implementing
 * {@link Entry}. Each entry implements caching using {@link #getString} and a
 * private field in a {@link #toString} override. The
 * {@link Entry#getBundleName} method needs to be defined to supply bundle
 * identification.</p>
 */
@Immutable
public final class Messages
{
    /**
     * No instances.
     */
    private Messages() {}

    /**
     * Gets a string for an entry.
     *
     * @param entry The entry.
     *
     * @return The string.
     */
    @Nonnull
    @CheckReturnValue
    public static String getString(final Entry entry)
    {
        ResourceBundle bundle = _BUNDLES.get(entry.getClass());
        String string;

        if (bundle == null) {
            bundle = _getBundle(entry);
            _BUNDLES.put(entry.getClass(), bundle);
        }

        try {
            string = (String) bundle.getObject(entry.name());
        } catch (final MissingResourceException exception) {
            if (entry.getClass() != BaseMessages.class) {
                Logger
                    .getInstance(entry.getClass())
                    .warn(
                        BaseMessages.MESSAGE_KEY_NOT_FOUND,
                        entry.name(),
                        entry.getBundleName());
            } else {    // Avoids a possible recursive loop.
                Logger
                    .getInstance(entry.getClass())
                    .log(
                        Logger.LogLevel.WARN,
                        _KEY_NOT_FOUND,
                        entry.name(),
                        entry.getBundleName());
            }

            string = entry.name();
        }

        return string;
    }

    private static ResourceBundle _getBundle(final Entry entry)
    {
        final String bundleName = entry.getBundleName();
        ResourceBundle bundle;

        try {    // Tries to get the bundle from the ServiceClassLoader.
            bundle = ResourceBundle
                .getBundle(
                    bundleName,
                    Locale.getDefault(),
                    Thread.currentThread().getContextClassLoader());
        } catch (final MissingResourceException exception) {
            bundle = null;
        }

        if (bundle == null) {
            try {    // Tries to get the bundle from the jar holding this class.
                bundle = ResourceBundle
                    .getBundle(
                        bundleName,
                        Locale.getDefault(),
                        Messages.class.getClassLoader());
            } catch (final MissingResourceException exception) {
                // The bundle will stay null.
            }
        }

        if (entry.getClass() != BaseMessages.class) {
            if (bundle != null) {
                _LOGGER.debug(BaseMessages.LOADED_MESSAGES, bundleName);
            } else {
                _LOGGER.warn(BaseMessages.UNKNOWN_BUNDLE, bundleName);
                bundle = _EMPTY_BUNDLE;
            }
        } else if (bundle == null) {    // Avoids a recursive loop.
            _LOGGER.log(Logger.LogLevel.WARN, _UNKNOWN_BUNDLE, bundleName);
            bundle = _EMPTY_BUNDLE;
        }

        return bundle;
    }

    private static final Map<Class<?>, ResourceBundle> _BUNDLES =
        new ConcurrentHashMap<>();
    private static final String _KEY_NOT_FOUND =
        "Message key ''{0}'' not found in ''{1}''";
    private static final Logger _LOGGER = Logger.getInstance(Messages.class);
    private static final String _UNKNOWN_BUNDLE =
        "Failed to locate a messages bundle: {0}";
    private static final ResourceBundle _EMPTY_BUNDLE = new ResourceBundle()
    {
        /** {@inheritDoc}
         */
        @Override
        public Enumeration<String> getKeys()
        {
            return new Enumeration<String>()
            {
                @Override
                public boolean hasMoreElements()
                {
                    return false;
                }

                @Override
                public String nextElement()
                {
                    return null;
                }
            };
        }

        /** {@inheritDoc}
         */
        @Override
        protected Object handleGetObject(final String key)
        {
            return null;
        }
    };

    /**
     * Entry.
     */
    public interface Entry
    {
        /**
         * Gets the messages resource bundle name.
         *
         * @return The messages resource bundle name.
         */
        @Nonnull
        @CheckReturnValue
        String getBundleName();

        /**
         * Returns the entry name.
         *
         * @return The entry name.
         */
        @Nonnull
        @CheckReturnValue
        String name();
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

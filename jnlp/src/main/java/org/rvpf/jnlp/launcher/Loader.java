/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Loader.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.jnlp.launcher;

import java.net.URL;

import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Loader for a JNLP file and its resources.
 *
 * <p>This interface allows a dedicated class loader to be used for the actual
 * loader implementation and avoids interference with the classes loaded for the
 * application.</p>
 */
public interface Loader
{
    /**
     * Gets the application's arguments.
     *
     * @return The application's arguments.
     */
    @Nonnull
    @CheckReturnValue
    String[] getArguments();

    /**
     * Gets the name of the application's main class.
     *
     * @return The name of the main class.
     */
    @Nonnull
    @CheckReturnValue
    String getMainClassName();

    /**
     * Gets the properties.
     *
     * @return The properties.
     */
    @Nonnull
    @CheckReturnValue
    Map<String, String> getProperties();

    /**
     * Gets the class loader URLs for the application.
     *
     * @return The class loader URLs.
     */
    @Nonnull
    @CheckReturnValue
    URL[] getURLs();

    /**
     * Loads a JNLP file.
     *
     * <p>This method must be the first one called. The others will simply
     * return values prepared while loading the JNLP file.</p>
     *
     * @param jnlpURL The URL for the JNLP file.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean loadJNLP(@Nonnull final Optional<URL> jnlpURL);
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClassDef.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class definition.
 *
 * <p>Class definitions wrap the handling of run time references to classes.</p>
 */
public interface ClassDef
{
    /**
     * Creates an instance of the class.
     *
     * <p>Uses the class returned by {@link #getInstanceClass()}, thus
     * triggering the class loading if needed. If either the class loading or
     * the instance creation fails, an error message is logged and a null value
     * is returned.</p>
     *
     * @param expectedClass The expected class.
     * @param <T> The type of the returned value.
     *
     * @return The instance (null on failure).
     */
    @Nullable
    @CheckReturnValue
    <T> T createInstance(@Nonnull Class<T> expectedClass);

    /**
     * Gets the referenced class name.
     *
     * @return The referenced class name.
     */
    @Nonnull
    @CheckReturnValue
    String getClassName();

    /**
     * Gets the class of instances created by this class definition.
     *
     * <p>If the class loading fails, an error message is logged and a null
     * value is returned.</p>
     *
     * @return The requested class (null on failure).
     */
    @Nullable
    @CheckReturnValue
    Class<?> getInstanceClass();

    /**
     * Gets the class of instances created by this class definition.
     *
     * <p>If the class loading fails, an error message is logged and a null
     * value is returned.</p>
     *
     * @param classLoader The optional class loader to use.
     *
     * @return The requested class (null on failure).
     */
    @Nullable
    @CheckReturnValue
    Class<?> getInstanceClass(@Nonnull final Optional<ClassLoader> classLoader);

    /**
     * Gets the package member name for the class.
     *
     * @return The class name without the package specification.
     */
    @Nonnull
    @CheckReturnValue
    String getMember();

    /**
     * Gets the package name for the class.
     *
     * @return The name of the package containing the class.
     */
    @Nonnull
    @CheckReturnValue
    String getPackageName();

    /**
     * Asks if the class has been loaded.
     *
     * @return True if the class has been loaded.
     */
    @CheckReturnValue
    boolean isLoaded();

    /**
     * Loader.
     */
    interface Loader
    {
        /**
         * Asks if a class has been loaded.
         *
         * @param className The class name.
         *
         * @return True if the class has been loaded.
         */
        @CheckReturnValue
        boolean isLoaded(@Nonnull String className);
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

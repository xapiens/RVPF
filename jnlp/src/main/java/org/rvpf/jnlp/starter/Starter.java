/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Starter.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.jnlp.starter;

import java.lang.reflect.Method;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Starter.
 *
 * <p>This class is expected to be put in a signed Jar with a copy of the JNLP
 * file in JNLP-INF/APPLICATION.JNLP and application configuration files in the
 * appropriate directories.</p>
 */
@ThreadSafe
public final class Starter
{
    /**
     * No instances.
     */
    private Starter() {}

    /**
     * Main program entry.
     *
     * <p>The name of the real Main-Class is taken from the value of the
     * {@value #STARTED_CLASS_PROPERTY} property. If it is absent, the first
     * argument is consumed as this name; the arguments left, if any, will be
     * supplied to the 'main' method of the class.</p>
     *
     * @param args The application arguments.
     */
    public static void main(@Nonnull final String[] args)
    {
        final String[] mainArgs;
        String mainClassName = System.getProperty(STARTED_CLASS_PROPERTY);

        if (mainClassName != null) {
            mainArgs = args;
        } else {
            if (args.length < 1) {
                throw new IllegalArgumentException();
            }

            mainClassName = args[0];
            mainArgs = new String[args.length - 1];
            System.arraycopy(args, 1, mainArgs, 0, mainArgs.length);
        }

        try {
            final Class<?> mainClass = Class.forName(mainClassName);
            final Method mainMethod = mainClass
                .getMethod("main", String[].class);

            mainMethod.invoke(null, new Object[] {mainArgs, });
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /** Started class property. */
    public static final String STARTED_CLASS_PROPERTY =
        "rvpf.jnlp.started.class";
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UncaughtExceptionHandler.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.util;

import javax.annotation.concurrent.Immutable;
import org.rvpf.base.logger.Logger;

/** Uncaught exception handler.
 */
@Immutable
public class UncaughtExceptionHandler
    implements Thread.UncaughtExceptionHandler
{
    /** {@inheritDoc}
     */
    @Override
    public void uncaughtException(
            final Thread thread,
            final Throwable throwable)
    {
        final Thread.UncaughtExceptionHandler exceptionHandler =
            thread.getUncaughtExceptionHandler();

        if (exceptionHandler != thread.getThreadGroup()) {
            exceptionHandler.uncaughtException(thread, throwable);
        } else {
            try {
                if (!Logger.isShutDown()) {
                    Logger.getInstance(getClass())
                        .uncaughtException(thread, throwable);
                    return;
                }
                return;
            } catch (final Throwable ignored) {
                // Will print on System.err.
            }
            System.err.print(
                "Exception in thread \""
                + thread.getName() + "\": ");
            throwable.printStackTrace(System.err);
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

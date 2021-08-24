/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PostInstallAction.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.tool;

import javax.annotation.Nonnull;

import javax.swing.JFrame;

/**
 * Post-install action.
 */
public interface PostInstallAction
{
    /**
     * Called after the normal installation steps.
     *
     * @param frame The parent frame (for dialogs).
     * @param installPath The install path.
     *
     * @throws ActionFailureException When appropriate.
     */
    void onInstallDone(
            @Nonnull JFrame frame,
            @Nonnull String installPath)
        throws ActionFailureException;

    /**
     * Action failure exception.
     */
    final class ActionFailureException
        extends Exception
    {
        /**
         * Constructs an instance.
         *
         * @param message The explanatory message.
         */
        public ActionFailureException(@Nonnull final String message)
        {
            super(message);
        }

        private static final long serialVersionUID = 1L;
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

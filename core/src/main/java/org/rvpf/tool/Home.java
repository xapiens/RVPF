/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Home.java 3885 2019-02-05 20:22:42Z SFB $
 */

package org.rvpf.tool;

import java.io.File;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.annotation.Nonnull;

/**
 * Home Preference.
 *
 * <p>This class is an installation and configuration helper. It is expected to
 * be used as a command line program to set or get the home directory of a
 * subsystem.</p>
 *
 * <p>This implementation uses the Java Preferences API to store and retrieve
 * its informations.</p>
 */
public final class Home
{
    /**
     * Constructs an instance.
     */
    private Home() {}

    /**
     * Main Program.
     *
     * <p>Usages (command line arguments):</p>
     *
     * <ul>
     *   <li>set &lt;application-node&gt; &lt;application-directory&gt;</li>
     *   <li>get &lt;aplication-node&gt; [&lt;output-prefix&gt;]</li>
     * </ul>
     *
     * @param args Command line arguments.
     */
    public static void main(@Nonnull final String[] args)
    {
        if (args.length > 1) {
            final String actionName = args[0];
            final String nodePath = args[1].replace('\\', '/');

            if ("get".equals(actionName) && (args.length <= 3)) {
                _doGetHome(nodePath, (args.length > 2)? args[2]: "");
            } else if ("set".equals(actionName) && (args.length == 3)) {
                _doSetHome(nodePath, args[2]);
            } else {
                _usage();
            }
        } else {
            _usage();
        }
    }

    private static void _doGetHome(final String nodePath, final String prefix)
    {
        final Preferences userRoot = Preferences.userRoot();
        final String home;

        try {
            if (userRoot.nodeExists(nodePath)) {
                home = userRoot.node(nodePath).get(_HOME_KEY, "");
            } else {
                home = "";
            }
        } catch (final BackingStoreException exception) {
            throw new RuntimeException(exception);
        }

        System.out.print(prefix + home);
    }

    private static void _doSetHome(
            final String nodePath,
            final String directoryPath)
    {
        final Preferences applicationNode = Preferences
            .userRoot()
            .node(nodePath);
        final File file = new File(directoryPath);

        if (!file.isDirectory()) {
            _usage();
        }

        applicationNode.put(_HOME_KEY, file.getAbsolutePath());
    }

    private static void _usage()
    {
        System.out.println("Usages:");
        System.out
            .println("         set <application-node> <application-directory>");
        System.out.println("         get <aplication-node> [<output-prefix>]");
        System.exit(1);
    }

    private static final String _HOME_KEY = "home";
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

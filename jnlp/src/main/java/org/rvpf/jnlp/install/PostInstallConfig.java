/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PostInstallConfig.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.jnlp.install;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.annotation.concurrent.ThreadSafe;

import javax.swing.JFrame;

import org.rvpf.base.tool.Require;
import org.rvpf.tool.PostInstallAction;

/**
 * Post-install configuration.
 */
@ThreadSafe
public class PostInstallConfig
    implements PostInstallAction
{
    /** {@inheritDoc}
     */
    @Override
    public void onInstallDone(
            final JFrame frame,
            final String installPath)
        throws ActionFailureException
    {
        final File rootDir = new File(installPath).getParentFile();

        Require.success(rootDir.isDirectory());

        _installConfig(rootDir);

        _runConfig(rootDir);
    }

    private static void _runConfig(
            final File rootDir)
        throws ActionFailureException
    {
        final String command = System
            .getProperty(CONFIG_EXEC_PROPERTY, "")
            .trim();

        if (command.isEmpty()) {
            return;
        }

        try {
            final Process process = Runtime
                .getRuntime()
                .exec(command, null, rootDir);
            final int status;

            process.waitFor();
            status = process.exitValue();

            if (status != 0) {
                throw new ActionFailureException(
                    "Command \"" + command + "\" completed with status '"
                    + status + "'");
            }
        } catch (final IOException exception) {
            throw new ActionFailureException(
                "Failed to execute \"" + command + "\": "
                + exception.getMessage());
        } catch (final InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void _installConfig(
            final File rootDir)
        throws ActionFailureException
    {
        final String jarName = System
            .getProperty(CONFIG_JAR_PROPERTY, "")
            .trim();

        if (jarName.isEmpty()) {
            return;
        }

        final InputStream inputStream = getClass()
            .getClassLoader()
            .getResourceAsStream(jarName);

        if (inputStream == null) {
            throw new ActionFailureException(
                "Jar resource '" + jarName + "' not found");
        }

        try (final JarInputStream jarStream = new JarInputStream(
                inputStream,
                        false)) {
            final byte[] buffer = new byte[4096];

            for (; ; jarStream.closeEntry()) {
                final JarEntry jarEntry = jarStream.getNextJarEntry();

                if (jarEntry == null) {
                    break;
                }

                final String entryName = jarEntry.getName();

                if (entryName.startsWith("META-INF/")) {
                    continue;
                }

                if (entryName.endsWith("/")) {
                    final File jarFile = new File(rootDir, entryName);

                    if (!jarFile.mkdirs()) {
                        if (!jarFile.isDirectory()) {
                            throw new ActionFailureException(
                                "Failed to created  directory '" + jarFile
                                + "' for '" + jarName + "'");
                        }
                    }

                    continue;
                }

                try (final OutputStream outputStream = new FileOutputStream(
                        new File(rootDir, entryName))) {
                    for (;;) {
                        final int read = jarStream.read(buffer);

                        if (read < 0) {
                            break;
                        }

                        outputStream.write(buffer, 0, read);
                    }
                }
            }
        } catch (final IOException exception) {
            throw new ActionFailureException(
                "Failed to access '" + jarName + "': "
                + exception.getMessage());
        }
    }

    /** Config exec property. */
    public static final String CONFIG_EXEC_PROPERTY = "rvpf.jnlp.config.exec";

    /** Config jar property. */
    public static final String CONFIG_JAR_PROPERTY = "rvpf.jnlp.config.jar";
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

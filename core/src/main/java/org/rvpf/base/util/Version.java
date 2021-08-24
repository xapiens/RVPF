/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Version.java 3983 2019-05-14 11:11:45Z SFB $
 */

package org.rvpf.base.util;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Logger.LogLevel;
import org.rvpf.base.tool.ValueConverter;

/**
 * Abstract version.
 *
 * <p>Subclasses provide the implementation version specified in the manifest of
 * the jar from which they were loaded.</p>
 */
@Immutable
public abstract class Version
{
    /**
     * Gets the date-time of the container of a resource.
     *
     * @param resourcePath The resource path.
     *
     * @return The optional date-time.
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<DateTime> getContainerDateTime(
            @Nonnull final String resourcePath)
    {
        final URL url = _getThisCLassLoader().getResource(resourcePath);
        final long millis;

        if (url != null) {
            final URLConnection connection;

            try {
                connection = url.openConnection();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);    // Should not happen.
            }

            millis = connection.getLastModified();
        } else {
            millis = 0;
        }

        return Optional
            .ofNullable((millis > 0)? DateTime.fromMillis(millis): null);
    }

    /**
     * Gets the file of the container of a resource.
     *
     * @param resourcePath The resource path.
     *
     * @return The optional file (empty if path protocol is not 'file').
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<File> getContainerFile(
            @Nonnull final String resourcePath)
    {
        URL url = _getThisCLassLoader().getResource(resourcePath);

        if ("jar".equalsIgnoreCase(url.getProtocol())) {
            final String path = url.getPath();
            final int index = path.indexOf('!');

            try {
                url = new URL((index >= 0)? path.substring(0, index): path);
            } catch (final MalformedURLException exception) {
                throw new RuntimeException(exception);    // Should not happen.
            }
        } else if (!ResourceFileFactory.FILE_PROTOCOL
            .equalsIgnoreCase(url.getProtocol())) {
            url = null;
        }

        try {
            return Optional
                .ofNullable((url != null)? new File(url.toURI()): null);
        } catch (final URISyntaxException exception) {
            throw new RuntimeException(exception);    // Should not happen.
        }
    }

    /**
     * Gets the manifest from the jar containing an object class.
     *
     * @param objectClass The object class.
     *
     * @return The manifest (empty on failure).
     */
    @Nonnull
    @CheckReturnValue
    public static Manifest getManifest(@Nonnull final Class<?> objectClass)
    {
        Manifest manifest;

        try {
            final URL url = objectClass
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();
            final JarFile jarFile = new JarFile(url.getPath());

            manifest = jarFile.getManifest();
            jarFile.close();
        } catch (final Exception exception) {
            manifest = null;
        }

        return (manifest != null)? manifest: new Manifest();
    }

    /**
     * Gets an attribute from a jar manifest.
     *
     * @param manifest The manifest.
     * @param attributeName The attribute name.
     *
     * @return The attribute value ({@link #PROBLEM_INDICATOR} when missing).
     */
    @Nonnull
    @CheckReturnValue
    public static String getManifestAttribute(
            @Nonnull final Manifest manifest,
            @Nonnull final String attributeName)
    {
        final Attributes attributes = manifest.getMainAttributes();
        final String attribute = attributes.getValue(attributeName);

        return (attribute != null)? attribute: PROBLEM_INDICATOR;
    }

    /**
     * Gets the date-time of the container of this class.
     *
     * @return The optional date-time.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<DateTime> getContainerDateTime()
    {
        return getContainerDateTime(_getThisResourcePath());
    }

    /**
     * Gets the file of the container of this class.
     *
     * @return The optional file (empty if resource path protocol is not 'file').
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<File> getContainerFile()
    {
        return getContainerFile(_getThisResourcePath());
    }

    /**
     * Gets the implementation ident.
     *
     * @return The implementation title and version.
     */
    @Nonnull
    @CheckReturnValue
    public final String getImplementationIdent()
    {
        final String version = getImplementationVersion();

        return String
            .valueOf(
                (version == UNKNOWN_INDICATOR)? getContainerDateTime()
                    .orElse(null): new StringBuilder(getImplementationTitle())
                            .append(' ')
                            .append(version));
    }

    /**
     * Gets the implementation title.
     *
     * @return The implementation title or "*" if unavailable.
     */
    @Nonnull
    @CheckReturnValue
    public final String getImplementationTitle()
    {
        final String title = getClass().getPackage().getImplementationTitle();

        return (title != null)? title: UNKNOWN_INDICATOR;
    }

    /**
     * Gets the implementation version text.
     *
     * @return The version text or "*" if unavailable.
     */
    @Nonnull
    @CheckReturnValue
    public final String getImplementationVersion()
    {
        final String version = getClass()
            .getPackage()
            .getImplementationVersion();

        return (version != null)? version: UNKNOWN_INDICATOR;
    }

    /**
     * Logs the implementation ident.
     *
     * @param quiet True for quiet logging (DEBUG instead of INFO).
     */
    public void logImplementationIdent(final boolean quiet)
    {
        final Logger logger = Logger.getInstance(getClass());
        final LogLevel logLevel = quiet? LogLevel.DEBUG: LogLevel.INFO;

        logger
            .log(
                logLevel,
                BaseMessages.IMPLEMENTATION,
                getImplementationIdent());
    }

    /**
     * Logs system informations.
     *
     * @param identification The caller's identification.
     */
    public void logSystemInfo(@Nonnull final String identification)
    {
        logSystemInfo(identification, true);
    }

    /**
     * Logs system informations.
     *
     * @param identification The caller's identification.
     * @param quiet True for quiet logging (DEBUG instead of INFO).
     */
    public void logSystemInfo(
            @Nonnull final String identification,
            final boolean quiet)
    {
        final Logger logger = Logger.getInstance(getClass());
        final LogLevel logLevel = quiet? LogLevel.DEBUG: LogLevel.INFO;
        final String javaVersion = System.getProperty("java.version");
        final String javaVendor = System.getProperty("java.vendor");
        final String vmName = System.getProperty("java.vm.name");
        final String vmVersion = System.getProperty("java.vm.version");
        final String vmInfo = System.getProperty("java.vm.info");
        final String vmVendor = System.getProperty("java.vm.vendor");
        final String osName = System.getProperty("os.name");
        final String osVersion = System.getProperty("os.version");
        final String osArch = System.getProperty("os.arch");
        final String userDir = System.getProperty("user.dir");
        String osPatchLevel = System.getProperty("sun.os.patch.level", "");

        if (osPatchLevel.length() > 0) {
            osPatchLevel = "unknown"
                .equals(osPatchLevel)? "": (" " + osPatchLevel);
        }

        logger.log(logLevel, BaseMessages.SYSTEM_INFO, identification);
        logger
            .log(logLevel, BaseMessages.JAVA_VERSION, javaVersion, javaVendor);
        logger
            .log(
                logLevel,
                BaseMessages.JAVA_VM,
                vmName,
                vmVersion,
                vmInfo,
                vmVendor);
        logger
            .log(
                logLevel,
                BaseMessages.OPERATING_SYSTEM,
                osName,
                osVersion,
                osPatchLevel,
                osArch);
        logger.log(logLevel, BaseMessages.BASE_DIRECTORY, userDir);

        if (logger.isDebugEnabled()) {
            final String javaHome = System.getProperty("java.home");
            final String bootClassPath = System
                .getProperty("sun.boot.class.path");
            final String bootLibraryPath = System
                .getProperty("sun.boot.library.path");
            final String classPath = System.getProperty("java.class.path");
            final String libraryPath = System.getProperty("java.library.path");
            final String fileEncoding = System.getProperty("file.encoding");
            final String userHome = System.getProperty("user.home");
            final String userName = System.getProperty("user.name");
            final String userCountry = System.getProperty("user.country");
            final String userLanguage = System.getProperty("user.language");

            logger.debug(BaseMessages.JAVA_HOME, javaHome);
                        
            
            if (bootClassPath != null) {
                logger.debug(BaseMessages.BOOT_CLASS_PATH, bootClassPath);
            }

            if (bootLibraryPath != null) {
                logger.debug(BaseMessages.BOOT_LIBRARY_PATH, bootLibraryPath);
                logger.log(logLevel, BaseMessages.BOOT_LIBRARY_PATH, bootLibraryPath);
            }

            logger.debug(BaseMessages.CLASS_PATH, classPath);
            logger.debug(BaseMessages.LIBRARY_PATH, libraryPath);
            logger.debug(BaseMessages.USER_HOME, userName, userHome);
            logger
                .debug(
                    BaseMessages.COUNTRY_ETC,
                    userCountry,
                    userLanguage,
                    TimeZone.getDefault().getDisplayName());
            logger
                .debug(
                    BaseMessages.DEFAULT_LOCALE,
                    Locale.getDefault(),
                    fileEncoding);
        }

        final Optional<byte[]> node = UUID.getNode();

        if (node.isPresent()) {
            final StringBuilder stringBuilder = new StringBuilder();

            for (final byte value: node.get()) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append('-');
                }

                final String hex = Integer.toHexString(value & 0xFF);

                if (hex.length() < 2) {
                    stringBuilder.append('0');
                }

                stringBuilder.append(hex);
            }

            logger.log(logLevel, BaseMessages.MAC_ADDRESS, stringBuilder);
        }

        final long maxMemory = ValueConverter
            .roundToMebibytes(Runtime.getRuntime().maxMemory());

        logger
            .log(logLevel, BaseMessages.HEAP_LIMIT, String.valueOf(maxMemory));

        logImplementationIdent(quiet);

        Logger.logBackEnd();
    }

    private static ClassLoader _getThisCLassLoader()
    {
        final ClassLoader classLoader = Thread
            .currentThread()
            .getContextClassLoader();

        return (classLoader != null)? classLoader: ClassLoader
            .getSystemClassLoader();
    }

    private String _getThisResourcePath()
    {
        return getClass().getName().replace('.', '/') + ".class";
    }

    /** Application name attribute. */
    public static final String APPLICATION_NAME_ATTRIBUTE = "Application-Name";

    /** Bundle version attribute. */
    public static final String BUNDLE_VERSION_ATTRIBUTE = "Bundle-Version";

    /** Problem indicator. */
    public static final String PROBLEM_INDICATOR = "?";

    /** Specification version attribute. */
    public static final String SPECIFICATION_VERSION_ATTRIBUTE =
        "Specification-Version";

    /** Unknown indicator. */
    public static final String UNKNOWN_INDICATOR = "*";
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

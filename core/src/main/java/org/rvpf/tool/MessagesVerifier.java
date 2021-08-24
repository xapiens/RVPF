/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MessagesVerifier.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.text.MessageFormat;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.ResourceFileFactory;

/**
 * Messages verifier.
 */
public final class MessagesVerifier
{
    private MessagesVerifier(final String packageName)
    {
        _packageName = packageName;
    }

    /**
     * Main entry.
     *
     * @param args Standard main arguments.
     */
    public static void main(@Nonnull final String[] args)
    {
        _LOGGER.reset();

        final MessagesVerifier verifier = new MessagesVerifier(
            (args.length > 0)? args[0]: _RVPF_PACKAGE);

        verifier._verifyMessages();

        System.exit(_LOGGER.hasLogged(Logger.LogLevel.WARN)? 1: 0);
    }

    private static List<String> _readMessagesNames(final File resource)
    {
        final List<String> names = new LinkedList<String>();

        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(resource),
                    StandardCharsets.UTF_8))) {
            for (;;) {
                String line = reader.readLine();

                if (line == null) {
                    break;
                }

                line = line.trim();

                if ((line.length() > 0)
                        && !line.startsWith("#")
                        && !line.startsWith("!")) {
                    final Matcher matcher = _MESSAGE_PATTERN.matcher(line);

                    if (matcher.matches()) {
                        names.add(matcher.group(1));

                        try {
                            new MessageFormat(matcher.group(2), Locale.ROOT);
                        } catch (final IllegalArgumentException exception) {
                            _LOGGER
                                .warn(
                                    ToolsMessages.BAD_MESSAGE,
                                    resource,
                                    matcher.group(1),
                                    exception.getMessage());
                        }
                    } else {
                        _LOGGER.warn(ToolsMessages.BAD_LINE, resource, line);
                    }
                }
            }
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        return names;
    }

    private static File _relativeFile(final File absoluteFile)
    {
        final File currentDirectory = new File(".")
            .getAbsoluteFile()
            .getParentFile();
        final String currentPath = currentDirectory.getPath();
        final String absolutePath = absoluteFile.getPath();
        int index;

        for (index = 0; index < currentPath.length(); ++index) {
            if (absolutePath.charAt(index) != currentPath.charAt(index)) {
                break;
            }
        }

        final File rootDirectory = new File(currentPath.substring(0, index));

        if (!rootDirectory.isDirectory()) {
            return absoluteFile;
        }

        if (!currentDirectory.equals(rootDirectory)) {
            return null;
        }

        String relativePath = absolutePath.substring(index);

        if (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }

        return new File(relativePath);
    }

    @SuppressWarnings(
    {
        "unchecked", "rawtypes"
    })
    private static void _verifyMessagesResource(
            final File resource,
            final Class<?> messagesClass)
    {
        final File relativeResource = _relativeFile(resource);

        if (relativeResource == null) {
            return;
        }

        _LOGGER
            .debug(ToolsMessages.VERIFYING_MESSAGES_RESOURCE, relativeResource);

        final List<String> names = _readMessagesNames(resource);
        final Set<String> namesSet = new HashSet<>();
        String previousName = "";

        for (final String name: names) {
            if (!namesSet.add(name)) {
                _LOGGER.warn(ToolsMessages.DUPLICATE_MESSAGE_NAME, name);
            }

            if (name.compareToIgnoreCase(previousName) <= 0) {
                _LOGGER
                    .warn(
                        ToolsMessages.MESSAGE_NAME_ORDER,
                        name,
                        relativeResource);
            }

            previousName = name;

            try {
                Enum.valueOf((Class) messagesClass, name);
            } catch (final IllegalArgumentException exception) {
                _LOGGER
                    .warn(
                        ToolsMessages.MESSAGE_NAME_NOT_DEFINED,
                        name,
                        relativeResource,
                        messagesClass.getName());
            }
        }

        final boolean isBase = resource.getName().indexOf('_') < 0;
        final Object[] entries = messagesClass.getEnumConstants();
        int missingNames = 0;

        for (final Object object: entries) {
            final Messages.Entry entry = (Messages.Entry) object;

            if (!namesSet.contains(entry.name())) {
                final Message message = new Message(
                    ToolsMessages.MESSAGE_NAME_MISSING,
                    entry.name(),
                    relativeResource);

                if (isBase) {
                    _LOGGER.warn(message);
                } else {
                    _LOGGER.debug(message);
                }

                ++missingNames;
            }
        }

        if ((missingNames > 0) && !isBase) {
            _LOGGER
                .info(
                    ToolsMessages.MISSING_MESSAGE_NAMES,
                    relativeResource,
                    String.valueOf(missingNames));
        }
    }

    private List<File> _getRootDirectories()
    {
        final List<File> roots = new LinkedList<>();
        final Enumeration<URL> urls;

        try {
            urls = Thread
                .currentThread()
                .getContextClassLoader()
                .getResources(_packageName.replace('.', '/'));
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        while (urls.hasMoreElements()) {
            final URL url = urls.nextElement();

            if (ResourceFileFactory.FILE_PROTOCOL
                .equalsIgnoreCase(url.getProtocol())) {
                final File file = new File(url.getFile());

                Require.success(file.isDirectory());
                roots.add(file);
            }
        }

        return roots;
    }

    private void _loadMessagesClasses()
    {
        for (final File directory: _getRootDirectories()) {
            if (_relativeFile(directory) != null) {
                _loadMessagesClasses(directory, _packageName);
            }
        }
    }

    private void _loadMessagesClasses(final File directory, final String prefix)
    {
        final String[] entries = Require.notNull(directory.list());

        for (final String entry: entries) {
            if (entry.indexOf('.') >= 0) {
                if (entry.endsWith(_MESSAGES_CLASS)) {
                    try {
                        final Class<?> entryClass = Class
                            .forName(
                                prefix + '.' + entry.substring(
                                    0,
                                    entry.length() - _CLASS.length()),
                                false,
                                Thread.currentThread().getContextClassLoader());

                        if (Messages.Entry.class.isAssignableFrom(entryClass)) {
                            Class
                                .forName(
                                    entryClass.getName(),
                                    true,
                                    Thread
                                        .currentThread()
                                        .getContextClassLoader());
                            _messagesClasses.add(entryClass);
                        }
                    } catch (final ClassNotFoundException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            } else {
                final File subdirectory = new File(directory, entry);

                if (subdirectory.isDirectory()) {
                    _loadMessagesClasses(subdirectory, prefix + '.' + entry);
                }
            }
        }
    }

    private void _scanClasses(final File directory)
    {
        final File[] files = Require.notNull(directory.listFiles());

        for (final File file: files) {
            if (file.isDirectory()) {
                _scanClasses(file);
            } else if (file.getName().endsWith(_CLASS)) {
                final ClassConstantsLoader loader;

                try {
                    loader = new ClassConstantsLoader(file);
                } catch (final ClassFormatError exception) {
                    _LOGGER
                        .warn(
                            ToolsMessages.FORMAT_ERROR,
                            file,
                            exception.getMessage());

                    continue;
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }

                if (_entryNamesMap.containsKey(loader.getClassName())) {
                    continue;
                }

                for (final String className: loader.getClassNames()) {
                    final Set<String> entryNames = _entryNamesMap
                        .get(className);

                    if ((entryNames != null) && !entryNames.isEmpty()) {
                        for (final String fieldName:
                                loader.getFieldNames(className)) {
                            entryNames.remove(fieldName);
                        }
                    }
                }
            }
        }
    }

    private void _verifyMessages()
    {
        _loadMessagesClasses();
        _verifyMessagesClasses();
        _verifyMessagesResources();
        _verifyMessagesReferences();

        if (_LOGGER.hasLogged(Logger.LogLevel.WARN)) {
            _LOGGER.info(ToolsMessages.MESSAGES_VERIFICATION_FAILED);
        } else {
            _LOGGER.info(ToolsMessages.MESSAGES_VERIFICATION_SUCCESSFUL);
        }
    }

    private void _verifyMessagesClasses()
    {
        _LOGGER.info(ToolsMessages.VERIFYING_MESSAGES_CLASSES);

        for (final Class<?> messagesClass: _messagesClasses) {
            final String className = messagesClass.getName();
            final Object[] entries = messagesClass.getEnumConstants();

            if (entries.length > 0) {
                _LOGGER
                    .debug(ToolsMessages.VERIFYING_MESSAGES_CLASS, className);

                final Set<String> entryNames = new LinkedHashSet<>();
                String previousName = "";

                for (final Object object: entries) {
                    final Messages.Entry entry = (Messages.Entry) object;

                    if (entry.name().compareToIgnoreCase(previousName) <= 0) {
                        _LOGGER
                            .warn(
                                ToolsMessages.MESSAGE_ENTRY_ORDER,
                                entry.name(),
                                className);
                    }

                    previousName = entry.name();
                    entryNames.add(previousName);
                }

                _entryNamesMap.put(className, entryNames);
            } else {
                _LOGGER.info(ToolsMessages.CLASS_NO_ENTRIES, className);
            }
        }
    }

    private void _verifyMessagesReferences()
    {
        _LOGGER.info(ToolsMessages.SCANNING_MESSAGE_REFERENCES);

        for (final File directory: _getRootDirectories()) {
            final File relativeDirectory = _relativeFile(directory);

            if (relativeDirectory != null) {
                _LOGGER
                    .debug(
                        ToolsMessages.SCANNING_MESSAGE_REFERENCES_UNDER,
                        relativeDirectory);
                _scanClasses(directory);
            }
        }

        for (final Map.Entry<String, Set<String>> entry:
                _entryNamesMap.entrySet()) {
            for (final String entryName: entry.getValue()) {
                _LOGGER
                    .warn(
                        ToolsMessages.MESSAGE_ENTRY_UNREFERENCED,
                        entry.getKey(),
                        entryName);
            }
        }
    }

    private void _verifyMessagesResources()
    {
        _LOGGER.info(ToolsMessages.VERIFYING_MESSAGES_RESOURCES);

        for (final Class<?> messagesClass: _messagesClasses) {
            final Object[] entries = messagesClass.getEnumConstants();

            if (entries.length > 0) {
                final String bundleName = ((Messages.Entry) entries[0])
                    .getBundleName();
                final ClassLoader classLoader = Thread
                    .currentThread()
                    .getContextClassLoader();
                final URL bundleURL = classLoader
                    .getResource(bundleName.replace('.', '/') + ".properties");

                if (bundleURL == null) {
                    _LOGGER
                        .error(
                            ToolsMessages.MESSAGE_RESOURCE_NOT_FOUND,
                            bundleName);

                    continue;
                }

                final File bundleBase = new File(bundleURL.getFile());
                final File directory = bundleBase.getParentFile();
                final String baseName = bundleBase.getName();
                final int periodIndex = baseName.indexOf('.');
                final String namePrefix = baseName.substring(0, periodIndex);
                final String nameSuffix = baseName.substring(periodIndex);
                final FileFilter fileFilter = new FileFilter()
                {
                    @Override
                    public boolean accept(final File file)
                    {
                        if (!file.isDirectory()) {
                            final String fileName = file.getName();

                            if (fileName.startsWith(namePrefix)
                                    && fileName.endsWith(nameSuffix)) {
                                return (fileName.length()
                                        == (namePrefix.length()
                                            + nameSuffix.length()))
                                       || (fileName.charAt(
                                           namePrefix.length()) == '_');
                            }
                        }

                        return false;
                    }
                };

                if ((directory != null) && directory.exists()) {
                    final File[] resources = Require
                        .notNull(directory.listFiles(fileFilter));

                    for (final File resource: resources) {
                        _verifyMessagesResource(resource, messagesClass);
                    }
                }
            }
        }
    }

    private static final String _CLASS = ".class";
    private static final Logger _LOGGER = Logger
        .getInstance(MessagesVerifier.class);
    private static final String _MESSAGES_CLASS = "Messages" + _CLASS;
    private static final Pattern _MESSAGE_PATTERN = Pattern
        .compile("([A-Z][A-Z0-9_]*+)\\s*+[:=]?+\\s*+(.+)");
    private static final String _RVPF_PACKAGE = "org.rvpf";

    private final Map<String, Set<String>> _entryNamesMap =
        new LinkedHashMap<>();
    private final List<Class<?>> _messagesClasses = new LinkedList<>();
    private final String _packageName;
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

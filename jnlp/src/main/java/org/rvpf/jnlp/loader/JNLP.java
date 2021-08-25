/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JNLP.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.jnlp.loader;

import java.io.File;

import java.net.URL;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Require;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;

/**
 * JNLP: gets and verifies the contents of a JNLP file.
 */
public final class JNLP
{
    /**
     * Constructs an instance.
     */
    private JNLP() {}

    /**
     * Loads the content specified by the JNLP file.
     *
     * @param jnlpURL The URL for the JNLP file.
     *
     * @return An instance representing the content of the JNLP file.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    static JNLP load(@Nonnull final URL jnlpURL)
        throws Exception
    {
        final JNLP jnlp = new JNLP();

        final CacheManager cacheManager = new CacheManager();

        try {
            jnlp._load(jnlpURL, cacheManager);
        } finally {
            cacheManager.close();
        }

        return jnlp;
    }

    /**
     * Gets the arguments.
     *
     * @return The arguments.
     */
    @Nonnull
    @CheckReturnValue
    String[] getArguments()
    {
        return _arguments.toArray(new String[_arguments.size()]);
    }

    /**
     * Gets the jars.
     *
     * @return The jars.
     */
    @Nonnull
    @CheckReturnValue
    List<File> getJars()
    {
        return _jars;
    }

    /**
     * Gets the main class name.
     *
     * @return The main class name.
     */
    @Nonnull
    @CheckReturnValue
    String getMainClassName()
    {
        Require.notNull(_mainClassName);

        return _mainClassName;
    }

    /**
     * Gets the properties.
     *
     * @return The properties.
     */
    @Nonnull
    @CheckReturnValue
    Map<String, String> getProperties()
    {
        return _properties;
    }

    private static String _getAttribute(
            final XMLElement element,
            final String name)
    {
        final Optional<String> value = element
            .getAttributeValue(name, Optional.empty());

        return (value
            .isPresent())? JNLPProperties
                .getInstance()
                .substitute(value.get(), false): null;
    }

    private static String _getText(final XMLElement element)
    {
        return JNLPProperties
            .getInstance()
            .substitute(element.getText(), false);
    }

    private File _fetchResources(
            final XMLElement root,
            final CacheManager cacheManager)
        throws Exception
    {
        final Optional<String> dirProperty = JNLPProperties
            .getInstance()
            .getStringValue(EXTRACT_DIR_PROPERTY, Optional.empty());
        final File extractDir = dirProperty
            .isPresent()? new File(dirProperty.get()): null;
        String href = _getAttribute(root, _CODEBASE_ATTRIBUTE);

        if ((href != null) && !href.endsWith("/")) {
            href += '/';
        }

        final URL codeBase = (href != null)? new URL(href): null;
        File mainJar = null;

        for (final XMLElement resources: root.getChildren(_RESOURCES_ELEMENT)) {
            for (final XMLElement resource: resources.getChildren()) {
                final String name = resource.getName();

                if (_JAR_ELEMENT.equals(name)) {
                    href = _getAttribute(resource, _HREF_ATTRIBUTE);

                    if (href != null) {
                        final URL url = new URL(codeBase, href);
                        final File jar = cacheManager.getFile(url);

                        if (!Verifier
                            .getInstance()
                            .verifyJar(
                                jar,
                                cacheManager.isNew(jar),
                                Optional.empty())) {
                            throw new JNLPException(
                                Message.format(JNLPMessages.JAR_FAILED, url));
                        }

                        _jars.addLast(jar);

                        if (_TRUE_VALUE
                            .equals(_getAttribute(resource, _MAIN_ATTRIBUTE))) {
                            if (mainJar != null) {
                                throw new JNLPException(
                                    Message.format(JNLPMessages.ONE_MAIN_JAR));
                            }

                            mainJar = jar;
                        }
                    }
                } else if (_NATIVELIB_ELEMENT.equals(name)) {
                    href = _getAttribute(resource, _HREF_ATTRIBUTE);

                    if ((href != null) && (extractDir != null)) {
                        final File jar = cacheManager
                            .getFile(new URL(codeBase, href));

                        if (!Verifier
                            .getInstance()
                            .verifyJar(
                                jar,
                                cacheManager.isNew(jar),
                                Optional.of(extractDir))) {
                            throw new JNLPException(
                                Message.format(JNLPMessages.JAR_FAILED));
                        }
                    }
                } else if (_PROPERTY_ELEMENT.equals(name)) {
                    _properties
                        .put(
                            _getAttribute(resource, _NAME_ATTRIBUTE),
                            _getAttribute(resource, _VALUE_ATTRIBUTE));
                }
            }
        }

        if (_jars.isEmpty()) {
            throw new JNLPException(Message.format(JNLPMessages.NO_JAR));
        }

        return (mainJar != null)? mainJar: _jars.getFirst();
    }

    private void _load(
            final URL jnlpURL,
            final CacheManager cacheManager)
        throws Exception
    {
        final File jnlpFile = cacheManager.getFile(jnlpURL);
        final XMLElement root = new XMLDocument()
            .parse(jnlpFile, Optional.empty());
        final File mainJar = _fetchResources(root, cacheManager);
        final Optional<XMLElement> optionalApplication = root
            .getFirstChild(_APPLICATION_DESC_ELEMENT);
        String mainClassName = null;

        if (optionalApplication.isPresent()) {
            final XMLElement application = optionalApplication.get();

            mainClassName = _getAttribute(application, _MAIN_CLASS_ATTRIBUTE);

            for (final XMLElement argument:
                    application.getChildren(_ARGUMENT_ELEMENT)) {
                _arguments.addLast(_getText(argument));
            }
        }

        final JarFile jarFile = new JarFile(mainJar);

        if (!Verifier.verifyJNLP(jnlpFile, jarFile)) {
            throw new JNLPException(
                Message.format(
                    JNLPMessages.JNLP_FAILED,
                    jnlpFile.getName(),
                    mainJar.getName()));
        }

        if (mainClassName == null) {
            final Manifest manifest = jarFile.getManifest();
            final Attributes attributes = (manifest != null)? manifest
                .getMainAttributes(): null;

            mainClassName = (attributes != null)? attributes
                .getValue(Attributes.Name.MAIN_CLASS): null;
        }

        jarFile.close();

        if (mainClassName == null) {
            throw new JNLPException(Message.format(JNLPMessages.NO_MAIN_CLASS));
        }

        _mainClassName = mainClassName;
    }

    /** Extract directory property. */
    public static final String EXTRACT_DIR_PROPERTY = "extract.dir";

    /**  */

    private static final String _APPLICATION_DESC_ELEMENT = "application-desc";
    private static final String _ARGUMENT_ELEMENT = "argument";
    private static final String _CODEBASE_ATTRIBUTE = "codebase";
    private static final String _HREF_ATTRIBUTE = "href";
    private static final String _JAR_ELEMENT = "jar";
    private static final String _MAIN_ATTRIBUTE = "main";
    private static final String _MAIN_CLASS_ATTRIBUTE = "main-class";
    private static final String _NAME_ATTRIBUTE = "name";
    private static final String _NATIVELIB_ELEMENT = "nativelib";
    private static final String _PROPERTY_ELEMENT = "property";
    private static final String _RESOURCES_ELEMENT = "resources";
    private static final String _TRUE_VALUE = "true";
    private static final String _VALUE_ATTRIBUTE = "value";

    private final LinkedList<String> _arguments = new LinkedList<String>();
    private final LinkedList<File> _jars = new LinkedList<File>();
    private String _mainClassName;
    private final Map<String, String> _properties = new HashMap<String,
        String>();
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

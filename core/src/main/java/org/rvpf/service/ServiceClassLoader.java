/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceClassLoader.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.UUID;
import org.rvpf.base.alert.Alert;
import org.rvpf.base.alert.Fatal;
import org.rvpf.base.alert.Info;
import org.rvpf.base.alert.Warning;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.util.ResourceFileFactory;
import org.rvpf.config.Config;
import org.rvpf.config.UndefinedEntityException;
import org.rvpf.config.entity.ClassLibEntity;

/**
 * Service class loader.
 *
 * <p>Adds class libraries support. Their configuration may specify that they
 * should be copied in a local cache for greater availability.</p>
 *
 * <p>This implementation uses two levels of class loaders by inserting a
 * slightly modified {@link URLClassLoader} between this and the original
 * parent.</p>
 */
public final class ServiceClassLoader
    extends ClassLoader
    implements ClassDef.Loader, Alert.Dispatcher
{
    /**
     * Constructs an instance.
     *
     * @param parent The class loader parent.
     */
    private ServiceClassLoader(final _ParentClassLoader parent)
    {
        super(parent);

        _LOGGER
            .debug(
                ServiceMessages.CLASS_LOADER_INSTANTIATED,
                parent.getParent());
    }

    /**
     * Gets a service class loader.
     *
     * <p>If the current thread context class loader is already a service class
     * loader, it will be returned; otherwise a new instance is returned.</p>
     *
     * @return The service class loader.
     */
    @Nonnull
    @CheckReturnValue
    public static ServiceClassLoader getInstance()
    {
        return getInstance(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Gets a service class loader.
     *
     * <p>If the supplied parent is already a service class loader, it will be
     * returned; otherwise a new instance is returned.</p>
     *
     * @param parent The parent class loader.
     *
     * @return The service class loader.
     */
    @Nonnull
    @CheckReturnValue
    public static ServiceClassLoader getInstance(
            @Nonnull final ClassLoader parent)
    {
        final ServiceClassLoader instance;

        if (parent instanceof ServiceClassLoader) {
            instance = (ServiceClassLoader) parent;
        } else {
            _ParentClassLoader child;

            synchronized (_CHILDREN) {
                final Reference<_ParentClassLoader> reference = _CHILDREN
                    .get(parent);

                child = (reference != null)? reference.get(): null;

                if (child == null) {
                    child = new _ParentClassLoader(parent);
                    _CHILDREN
                        .put(
                            parent,
                            new WeakReference<_ParentClassLoader>(child));
                }
            }

            instance = new ServiceClassLoader(child);
        }

        return instance;
    }

    /**
     * Hides the current service class loader instance.
     *
     * @return The optional service class loader instance.
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<ServiceClassLoader> hideInstance()
    {
        if (Thread.currentThread().getContextClassLoader()
                instanceof ServiceClassLoader) {
            final ServiceClassLoader serviceClassLoader =
                (ServiceClassLoader) Thread
                    .currentThread()
                    .getContextClassLoader();

            Thread
                .currentThread()
                .setContextClassLoader(
                    serviceClassLoader.getParent().getParent());

            return Optional.of(serviceClassLoader);
        }

        return Optional.empty();
    }

    /**
     * Restores a service class loader instance.
     *
     * @param serviceClassLoader The optional service class loader instance.
     */
    public static void restoreInstance(
            @Nonnull final Optional<ServiceClassLoader> serviceClassLoader)
    {
        if (serviceClassLoader.isPresent()) {
            serviceClassLoader.get().activate();
        }
    }

    /**
     * Activates this.
     */
    public void activate()
    {
        Thread.currentThread().setContextClassLoader(this);
    }

    /**
     * Adds URLs from a class library to the class path.
     *
     * <p>Redundant additions of the same class library are ignored. Also,
     * multiple references to the same URL are filtered.</p>
     *
     * @param classLib The class library holding the URLs.
     *
     * @throws UndefinedEntityException When the interface is undefined.
     */
    public synchronized void addFromClassLib(
            @Nonnull final ClassLibEntity classLib)
        throws UndefinedEntityException
    {
        if (!classLib.isDefined()) {
            throw new UndefinedEntityException(classLib);
        }

        if (classLib.isAdded()) {
            return;
        }

        final List<URL> urls = new LinkedList<URL>();

        for (final URI location: classLib.getLocations()) {
            _addFromLocation(location, urls);
        }

        for (URL url: urls) {
            if (classLib.isCached(false)) {
                url = _cacheURL(url, classLib.getUUID().get());

                if (url != null) {
                    _addURL(url);

                    break;
                }
            } else {
                _addURL(url);
            }
        }

        classLib.setAdded(true);

        addFromClassLibs(classLib.getClassLibs());
    }

    /**
     * Adds URLs from class libraries to the class path.
     *
     * <p>Redundant additions of the same class library are ignored. Also,
     * multiple references to the same URL are filtered.</p>
     *
     * @param classLibs The class libraries holding the URLs.
     *
     * @throws UndefinedEntityException When the interface is undefined.
     */
    public synchronized void addFromClassLibs(
            @Nonnull final List<ClassLibEntity> classLibs)
        throws UndefinedEntityException
    {
        for (final ClassLibEntity classLib: classLibs) {
            addFromClassLib(classLib);
        }
    }

    /**
     * Adds URLs from a location to the class path.
     *
     * <p>Multiple references to the same URL are filtered.</p>
     *
     * @param location The location.
     */
    public synchronized void addFromLocation(@Nonnull final String location)
    {
        try {
            addFromLocation(new URI(location));
        } catch (final URISyntaxException exception) {
            _LOGGER.warn(ServiceMessages.BAD_LOCATION, location);
        }
    }

    /**
     * Adds URLs from a location to the class path.
     *
     * <p>Multiple references to the same URL are filtered.</p>
     *
     * @param location The location.
     */
    public synchronized void addFromLocation(@Nonnull final URI location)
    {
        final List<URL> urls = new LinkedList<URL>();

        _addFromLocation(location, urls);

        for (URL url: urls) {
            _addURL(url);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void dispatchAlert(
            final Logger.LogLevel alertLevel,
            final String alertName,
            final String info)
    {
        final Optional<Config> config = getConfig();

        if ((config.isPresent()) && config.get().hasService()) {
            final Service service = config.get().getService();
            final String serviceName = service.getServiceName();
            final Optional<String> entityName = service.getEntityName();
            final UUID sourceUUID = service
                .getOptionalSourceUUID()
                .orElse(null);
            final Alert alert;

            switch (alertLevel) {
                case FATAL: {
                    alert = new Fatal(
                        alertName,
                        Optional.of(serviceName),
                        entityName,
                        Optional.ofNullable(sourceUUID),
                        Optional.ofNullable(info));

                    break;
                }
                case ERROR: {
                    alert = new org.rvpf.base.alert.Error(
                        alertName,
                        Optional.of(serviceName),
                        entityName,
                        Optional.ofNullable(sourceUUID),
                        Optional.ofNullable(info));

                    break;
                }
                case WARN: {
                    alert = new Warning(
                        alertName,
                        Optional.of(serviceName),
                        entityName,
                        Optional.ofNullable(sourceUUID),
                        Optional.ofNullable(info));

                    break;
                }
                default: {
                    alert = new Info(
                        alertName,
                        Optional.of(serviceName),
                        entityName,
                        Optional.ofNullable(sourceUUID),
                        Optional.ofNullable(info));

                    break;
                }
            }

            service.sendAlert(alert);
        }
    }

    /**
     * Forgets the current config object if it is the one assumed as current.
     *
     * @param config The config object assumed as current.
     */
    public synchronized void forgetConfig(@Nonnull final Config config)
    {
        if (_config == Require.notNull(config)) {
            _config = null;
        }
    }

    /**
     * Gets the configuration context.
     *
     * @return The optional configuration context.
     */
    @Nonnull
    @CheckReturnValue
    public synchronized Optional<Config> getConfig()
    {
        return Optional.ofNullable(_config);
    }

    /**
     * Gets the search path of URLs for loading classes and resources.
     *
     * @return The search path of URLs for loading classes and resources.
     */
    @Nonnull
    @CheckReturnValue
    public URL[] getURLs()
    {
        return ((URLClassLoader) getParent()).getURLs();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isLoaded(final String className)
    {
        return findLoadedClass(className) != null;
    }

    /**
     * Sets the context to use for getting properties.
     *
     * @param config The config object.
     */
    public synchronized void useConfig(@Nonnull final Config config)
    {
        _config = config;
    }

    /**
     * Gets the children.
     *
     * @return The children.
     */
    @Nonnull
    @CheckReturnValue
    static Map<ClassLoader, Reference<_ParentClassLoader>> getChildren()
    {
        return _CHILDREN;
    }

    private void _addFromLocation(final URI location, final List<URL> urls)
    {
        final URI absoluteURI = location
            .isAbsolute()? location: _masterURI().resolve(location);
        final URL wildURL;

        try {
            wildURL = absoluteURI.toURL();
        } catch (final MalformedURLException exception) {
            _LOGGER.warn(ServiceMessages.BAD_URL, location);

            return;
        }

        // Converts wild URL to List<URL>.

        if (ResourceFileFactory.FILE_PROTOCOL.equalsIgnoreCase(
                wildURL.getProtocol())
                && wildURL.getFile().toLowerCase(
                    Locale.ROOT).endsWith(".jar")) {
            final File wildFile = new File(wildURL.getFile());
            final String wildName = wildFile.getName();
            final boolean isWild = (wildName.indexOf('*') >= 0)
                    || (wildName.indexOf('?') >= 0);
            final File wildDirectory = wildFile.getParentFile();

            if (isWild && (wildDirectory != null)) {
                final Pattern wildPattern = ValueConverter
                    .wildToPattern(wildName);
                final File[] files = Require
                    .notNull(
                        wildDirectory
                            .listFiles(
                                    new FilenameFilter()
                        {
                            @Override
                            public boolean accept(
                                    final File directory,
                                            final String name)
                            {
                                return wildPattern.matcher(name).matches();
                            }
                        }));

                if (files.length == 0) {
                    _LOGGER
                        .warn(
                            ServiceMessages.NO_MATCH_FOR_LIB_JAR,
                            wildName,
                            wildDirectory);
                }

                for (final File file: files) {
                    try {
                        urls.add(file.toURI().toURL());
                    } catch (final MalformedURLException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            } else {
                urls.add(wildURL);
            }
        } else {
            urls.add(wildURL);
        }
    }

    private void _addURL(final URL url)
    {
        if (ResourceFileFactory.FILE_PROTOCOL
            .equalsIgnoreCase(url.getProtocol())) {
            if (!(new File(URI.create(url.toString())).exists())) {
                _LOGGER.warn(ServiceMessages.FILE_NOT_FOUND, url);

                return;
            }
        }

        final _ParentClassLoader parent = (_ParentClassLoader) getParent();

        synchronized (parent) {
            for (final URL parentURL: parent.getURLs()) {
                if ((parentURL != null)
                        && url.toExternalForm().equals(
                            parentURL.toExternalForm())) {
                    return;    // Avoids duplicate URLs.
                }
            }

            parent.addURL(url);
            _LOGGER.debug(ServiceMessages.ADDED_TO_CLASSPATH, url);
        }
    }

    private URL _cacheURL(URL url, final UUID uuid)
    {
        if (ResourceFileFactory.FILE_PROTOCOL.equalsIgnoreCase(
                url.getProtocol())
                && !url.getFile().toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return url;
        }

        if ((_cache == null) && (_config != null)) {
            _cache = _config
                .getCacheDir(
                    CACHE_SECTION_NAME,
                    Optional.empty(),
                    Optional.empty());
        }

        if (_cache != null) {
            final String fileName = uuid.toName() + ".jar";
            final File cachedFile = new File(_cache, fileName);

            try {
                final URI uri = url.toURI();

                if (uri.getPath().endsWith("/")) {
                    url = new URI(
                        uri.getScheme(),
                        uri.getHost(),
                        uri.getPath() + fileName,
                        uri.getFragment())
                        .toURL();
                }

                final URLConnection connection = url.openConnection();
                final long after = cachedFile.lastModified();

                if (after > 0) {
                    connection.setIfModifiedSince(after);
                }

                connection.connect();

                final long lastModified = connection.getLastModified();

                if (lastModified > after) {
                    final File temporaryFile = new File(
                        _cache,
                        uuid.toName() + ".tmp");
                    final InputStream connectionStream = connection
                        .getInputStream();
                    final OutputStream fileStream = new FileOutputStream(
                        temporaryFile);
                    final byte[] buffer = new byte[_STREAM_BUFFER_SIZE];

                    for (;;) {
                        final int length = connectionStream.read(buffer);

                        if (length < 0) {
                            break;
                        }

                        fileStream.write(buffer, 0, length);
                    }

                    fileStream.close();
                    connectionStream.close();

                    if (cachedFile.exists() && !cachedFile.delete()) {
                        _LOGGER
                            .warn(BaseMessages.FILE_DELETE_FAILED, cachedFile);

                        return null;
                    }

                    if (!temporaryFile.renameTo(cachedFile)) {
                        _LOGGER
                            .warn(
                                BaseMessages.FILE_RENAME_FAILED,
                                temporaryFile,
                                cachedFile);

                        return null;
                    }

                    if (!cachedFile
                        .setLastModified(
                            ((lastModified + 999) / 1000) * 1000)) {
                        _LOGGER
                            .warn(
                                ServiceMessages.SET_FILE_MODIFIED_FAILED,
                                cachedFile);
                    }
                }
            } catch (final Exception exception) {
                _LOGGER
                    .warn(
                        ServiceMessages.JAR_LOAD_FAILED,
                        url,
                        exception.getMessage());
            }

            if (cachedFile.exists()) {
                try {
                    url = cachedFile.toURI().toURL();
                } catch (final MalformedURLException exception) {
                    throw new RuntimeException(exception);    // Should not happen.
                }
            } else {
                url = null;
            }
        } else {
            url = null;
        }

        return url;
    }

    private URI _masterURI()
    {
        if (_masterURI == null) {
            if (_config != null) {
                final Optional<String> serverProperty = _config
                    .getStringValue(CLASSLIB_SERVER_PROPERTY);

                if (serverProperty.isPresent()) {
                    final URI serverURI;

                    try {
                        serverURI = new URI(serverProperty.get());

                        if (serverURI.getPath().endsWith("/")) {
                            _masterURI = serverURI;
                        } else {
                            _masterURI = new URI(
                                serverURI.getScheme(),
                                serverURI.getHost(),
                                serverURI.getPath() + "/",
                                serverURI.getFragment());
                        }
                    } catch (final URISyntaxException exception) {
                        _LOGGER
                            .warn(
                                ServiceMessages.CLASS_LIB_ADDRESS_BAD,
                                serverProperty.get(),
                                exception.getMessage());
                    }
                }

                if (_masterURI == null) {
                    final Optional<String> dirProperty = _config
                        .getStringValue(CLASSLIB_DIR_PROPERTY);

                    if (dirProperty.isPresent()) {
                        final File dir = new File(dirProperty.get());

                        if (dir.isDirectory()) {
                            _masterURI = dir.toURI();
                        } else {
                            _LOGGER
                                .warn(
                                    ServiceMessages.CLASS_LIB_DIR_NOT_FOUND,
                                    dirProperty.get(),
                                    dir.getAbsolutePath());
                            _masterURI = Config.CWD_URI;
                        }
                    } else {
                        _masterURI = Config.CWD_URI;
                    }
                }
            } else {
                _masterURI = Config.CWD_URI;
            }
        }

        return _masterURI;
    }

    /** Cache section name for jars. */
    public static final String CACHE_SECTION_NAME = "jars";

    /** Contains the path to the ClassLib jars directory. */
    public static final String CLASSLIB_DIR_PROPERTY = "classlib.dir";

    /** The URL to the ClassLib server. */
    public static final String CLASSLIB_SERVER_PROPERTY = "classlib.server";

    /**  */

    static final Logger _LOGGER = Logger.getInstance(ServiceClassLoader.class);

    /**  */

    private static final int _STREAM_BUFFER_SIZE = 8192;
    private static final Map<ClassLoader, Reference<_ParentClassLoader>> _CHILDREN =
        new IdentityHashMap<>();

    private File _cache;
    private Config _config;
    private URI _masterURI;

    /**
     * Parent class loader.
     */
    private static final class _ParentClassLoader
        extends URLClassLoader
    {
        /**
         * Constructs an instance.
         *
         * @param parent The parent of this class loader.
         */
        _ParentClassLoader(final ClassLoader parent)
        {
            super(_EMPTY_URL_ARRAY, parent);
        }

        /** {@inheritDoc}
         */
        @Override
        public Class<?> loadClass(
                final String name)
            throws ClassNotFoundException
        {
            try {
                return super.loadClass(name);
            } catch (final NoClassDefFoundError exception) {
                _LOGGER.debug(ServiceMessages.CLASS_LOAD_FAILED, name);

                throw exception;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        protected void addURL(final URL url)
        {
            super.addURL(url);
        }

        /** {@inheritDoc}
         */
        @Override
        protected void finalize()
            throws Throwable
        {
            final Map<ClassLoader, Reference<_ParentClassLoader>> children =
                getChildren();

            synchronized (children) {
                final ClassLoader parent = getParent();
                final Reference<_ParentClassLoader> reference = children
                    .get(parent);
                final _ParentClassLoader child = (reference != null)? reference
                    .get(): null;

                if ((child == null) || (child == this)) {
                    children.remove(parent);
                }
            }

            super.finalize();
        }

        private static final URL[] _EMPTY_URL_ARRAY = new URL[0];
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

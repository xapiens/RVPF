/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Config.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Entity;
import org.rvpf.base.Params;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.util.ResourceFileFactory;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.config.entity.ClassLibEntity;
import org.rvpf.document.loader.DocumentPropertiesMap;
import org.rvpf.document.loader.DocumentStream;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceClassLoader;
import org.rvpf.service.ServiceContext;
import org.rvpf.service.ServiceMessages;

/**
 * Config.
 *
 * <p>Keeps configuration informations for a service (MBean). Also supplies
 * convenience methods to handle service context informations.</p>
 *
 * <p>Although instances are not thread safe, the service class loader methods
 * are.</p>
 */
@NotThreadSafe
public class Config
{
    /**
     * Constructs an instance.
     *
     * @param serviceName The service name (may be empty).
     */
    public Config(@Nonnull final String serviceName)
    {
        this(
            serviceName,
            new ConfigProperties(
                ServiceMessages.CONFIG_PROPERTIES_TYPE,
                Optional.of(serviceName)));

        fetchSystemProperties();
        _fetchResourceProperties();
    }

    /**
     * Constructs an instance.
     *
     * @param serviceName The service name (may be empty).
     * @param properties The properties.
     */
    public Config(
            @Nonnull final String serviceName,
            @Nonnull final ConfigProperties properties)
    {
        _serviceName = Require.notNull(serviceName);
        _properties = Require.notNull(properties);
    }

    /**
     * Returns a data directory.
     *
     * @param parentDir An optional parent directory.
     * @param serviceProperties The service properties.
     * @param dataDirProperty The data directory property.
     * @param defaultDirName The default directory name.
     *
     * @return The data directory.
     */
    @Nonnull
    @CheckReturnValue
    public static final File dataDir(
            @Nonnull final Optional<File> parentDir,
            @Nonnull final KeyedGroups serviceProperties,
            @Nonnull final String dataDirProperty,
            @Nonnull final String defaultDirName)
    {
        final String pathString = serviceProperties
            .getString(dataDirProperty, Optional.of(defaultDirName))
            .get();
        Path path;

        try {
            path = Paths.get(pathString);
        } catch (final InvalidPathException exception) {
            Logger
                .getInstance(Config.class)
                .warn(
                    ServiceMessages.INVALID_PATH_IN,
                    pathString,
                    dataDirProperty);
            path = Paths.get(defaultDirName);
        }

        if (parentDir.isPresent()) {
            path = parentDir.get().toPath().resolve(path);
        }

        return path.toFile();
    }

    /**
     * Adds a class definition entity.
     *
     * @param classDef The class definition entity.
     */
    public final void addClassDefEntity(@Nonnull final ClassDefEntity classDef)
    {
        final Optional<String> name = classDef.getName();

        Require.success(name.isPresent());

        if (_classDefsByName == null) {
            _classDefsByName = new HashMap<String, ClassDefEntity>();
        }

        _classDefsByName.put(name.get().toUpperCase(Locale.ROOT), classDef);
    }

    /**
     * Adds a class library entity.
     *
     * @param classLib The class library entity.
     */
    public final void addClassLibEntity(@Nonnull final ClassLibEntity classLib)
    {
        final Optional<String> name = classLib.getName();

        if (name.isPresent()) {
            if (_classLibsByName == null) {
                _classLibsByName = new HashMap<String, ClassLibEntity>();
            }

            _classLibsByName.put(name.get().toUpperCase(Locale.ROOT), classLib);
        }
    }

    /**
     * Adds properties for a Service.
     *
     * <p>If the Service already exists, the new informations are appended to
     * those already present.</p>
     *
     * @param serviceContext The properties.
     */
    public final void addServiceContext(
            @Nonnull final ServiceContext serviceContext)
    {
        final Set<String> aliases = serviceContext.getServiceAliases();
        final String serviceKey = serviceContext
            .getServiceName()
            .toUpperCase(Locale.ROOT);
        ServiceContext registeredContext;

        registeredContext = _serviceContexts.get(serviceKey);

        if (registeredContext == null) {
            _serviceContexts.put(serviceKey, serviceContext);

            if (_servicesByAlias.containsKey(serviceKey)) {
                getThisLogger()
                    .warn(
                        ServiceMessages.SERVICE_NAME_HIDES_ALIAS,
                        serviceContext.getServiceName());
            }
        } else {
            registeredContext.add(serviceContext);
        }

        if (aliases != null) {
            for (final String alias: aliases) {
                final String aliasKey = alias.toUpperCase(Locale.ROOT);

                registeredContext = _servicesByAlias.get(aliasKey);

                if (registeredContext != null) {
                    if (registeredContext
                        .getServiceName()
                        .equalsIgnoreCase(serviceContext.getServiceName())) {
                        getThisLogger()
                            .warn(
                                ServiceMessages.SERVICE_ALIAS_COLLIDES,
                                alias);
                    }
                } else {
                    if (_serviceContexts.get(aliasKey) != null) {
                        getThisLogger()
                            .warn(ServiceMessages.SERVICE_ALIAS_HIDDEN, alias);
                    }

                    _servicesByAlias.put(aliasKey, serviceContext);
                }
            }
        }
    }

    /**
     * Asks if this contains a properties group.
     *
     * @param key The properties group key (name).
     *
     * @return True if the properties group is present.
     */
    @CheckReturnValue
    public final boolean containsProperties(@Nonnull final String key)
    {
        return _properties.containsGroupKey(key);
    }

    /**
     * Asks if this contains a property.
     *
     * @param key The property key (name).
     *
     * @return True if the property is present.
     */
    @CheckReturnValue
    public final boolean containsProperty(@Nonnull final String key)
    {
        return _properties.containsValueKey(key);
    }

    /**
     * Creates a URL.
     *
     * <p>If the context is null, if the 'config.dir' property is defined, it is
     * used as context, otherwise, the document is searched as a resource on the
     * classpath; if not found, the context is set to the current directory.</p>
     *
     * @param from The document source.
     * @param context The optional current directory context.
     *
     * @return A new URL (null when malformed).
     */
    public final URL createURL(
            @Nonnull final String from,
            @Nonnull Optional<URL> context)
    {
        URL fromURL = null;

        try {
            if (!context.isPresent()) {
                final Optional<String> dir = getConfigDir();

                if (dir.isPresent()) {
                    context = Optional.of(new File(dir.get()).toURI().toURL());
                } else {
                    fromURL = Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource(from);

                    if (fromURL == null) {
                        context = Optional.of(CWD_URI.toURL());
                    }
                }
            }

            if (fromURL == null) {
                fromURL = new URL(context.orElse(null), from);
            }
        } catch (final MalformedURLException exception) {
            // Returns null.
        }

        return fromURL;
    }

    /**
     * Fetches system properties.
     *
     * <p>All system properties having a name beginning with
     * '{@value #SYSTEM_PROPERTY_PREFIX}' are added with the
     * '{@value #SYSTEM_PROPERTY_PREFIX}' prefix removed. These properties have
     * precedence over any other.</p>
     */
    public final void fetchSystemProperties()
    {
        Require.notPresent(_properties.getOverrider());

        final ConfigProperties systemProperties = new ConfigProperties(
            ServiceMessages.SYSTEM_PROPERTIES_TYPE);

        for (final Map.Entry<?, ?> entry: System.getProperties().entrySet()) {
            final String name = (String) entry.getKey();

            if (name.startsWith(SYSTEM_PROPERTY_PREFIX)) {
                systemProperties
                    .add(
                        name.substring(SYSTEM_PROPERTY_PREFIX.length()),
                        entry.getValue());
            }
        }

        _properties.setOverrider(systemProperties);
    }

    /**
     * Gets a boolean value for a key, defaulting to false.
     *
     * <p>If the key is not found or its value is unrecognized, false will be
     * returned.</p>
     *
     * @param key The name of the value.
     *
     * @return The requested value or false.
     */
    @CheckReturnValue
    public final boolean getBooleanValue(@Nonnull final String key)
    {
        return _properties.getBoolean(key, false);
    }

    /**
     * Gets a boolean value, providing a default.
     *
     * <p>If the key is not found or its value is unrecognized the default will
     * be returned.</p>
     *
     * @param key The key for the value.
     * @param defaultValue The default value.
     *
     * @return The requested value or the provided default.
     */
    @CheckReturnValue
    public final boolean getBooleanValue(
            @Nonnull final String key,
            final boolean defaultValue)
    {
        return _properties.getBoolean(key, defaultValue);
    }

    /**
     * Gets the cache directory.
     *
     * <p>The section name is used as the name of a subdirectory of the cache
     * directory specified in this configuration. When supplied, the UUID is
     * used to generate a name for a further subdirectory level.</p>
     *
     * <p>When the resulting directory is absent, it is created and
     * initialized.</p>
     *
     * @param section The name of the cache section.
     * @param uuid An optional UUID as subsection.
     * @param ident An optional service identification.
     *
     * @return The cache directory (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public final File getCacheDir(
            @Nonnull final String section,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final Optional<String> ident)
    {
        final File cacheRoot = dataDir(
            Optional.of(getDataDir()),
            getProperties(),
            CACHE_DIR_PROPERTY,
            DEFAULT_CACHE_DIR);
        File cacheDir;

        cacheDir = new File(cacheRoot, section);

        if (uuid.isPresent()) {
            cacheDir = new File(cacheDir, uuid.get().toName());
        }

        final String cacheDirPath = cacheDir.getAbsolutePath();

        getThisLogger().debug(ServiceMessages.CACHE_DIR, cacheDirPath);

        if (!cacheDir.isDirectory()) {
            if (cacheDir.mkdirs()) {
                final String serviceName = getServiceName();

                try {
                    if (uuid.isPresent()) {
                        createFile(
                            cacheDir,
                            UUID_FILENAME,
                            uuid.get().toString());
                    }

                    if (ident.isPresent()) {
                        createFile(cacheDir, ident.get(), serviceName);
                    } else if (!serviceName.isEmpty()) {
                        createFile(cacheDir, SERVICE_FILENAME, serviceName);
                    }

                    getThisLogger()
                        .info(ServiceMessages.CACHE_CREATED, cacheDirPath);
                } catch (final IOException exception) {
                    getThisLogger()
                        .warn(
                            ServiceMessages.CACHE_INIT_FAILED,
                            cacheDirPath,
                            exception.getMessage());
                    cacheDir = null;
                }
            } else {
                getThisLogger()
                    .warn(ServiceMessages.CACHE_CREATION_FAILED, cacheDirPath);
                cacheDir = null;
            }
        }

        return cacheDir;
    }

    /**
     * Gets a ClassDef for a key, providing a default.
     *
     * @param key The key for the ClassDef.
     * @param defaultClassDef The optional default for the key.
     *
     * @return The requested ClassDef or the provided default.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<? extends ClassDef> getClassDef(
            @Nonnull final String key,
            @Nonnull final Optional<ClassDef> defaultClassDef)
    {
        Optional<? extends ClassDef> classDef = getClassDefEntity(key);

        if (!classDef.isPresent()) {
            final Object object = _properties.getObject(key);

            if (object instanceof ClassDef) {
                classDef = Optional.of((ClassDef) object);
            } else if (object instanceof String) {
                classDef = getClassDefEntity((String) object);
            }

            if (!classDef.isPresent()) {
                classDef = _properties.getClassDef(key, defaultClassDef);
            }
        }

        return classDef;
    }

    /**
     * Gets a class definition entity.
     *
     * @param name The name of the class definition entity.
     *
     * @return The optional class definition entity.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ClassDefEntity> getClassDefEntity(
            @Nonnull final String name)
    {
        return (_classDefsByName != null)? Optional
            .ofNullable(
                _classDefsByName
                    .get(name.toUpperCase(Locale.ROOT))): Optional.empty();
    }

    /**
     * Gets the ClassDef array for the specified key.
     *
     * @param key The key for the requested ClassDef array.
     *
     * @return The ClassDef array (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public final ClassDef[] getClassDefs(@Nonnull final String key)
    {
        return _properties.getClassDefs(key);
    }

    /**
     * Gets a class library entity.
     *
     * @param name The name of the class library entity.
     *
     * @return The optional class library entity.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ClassLibEntity> getClassLibEntity(
            @Nonnull final String name)
    {
        return (_classLibsByName != null)? Optional
            .ofNullable(
                _classLibsByName
                    .get(name.toUpperCase(Locale.ROOT))): Optional.empty();
    }

    /**
     * Gets the service class loader.
     *
     * <p>If the service class loader has not been set before, an instance is
     * requested from its class.</p>
     *
     * @return The service class loader.
     */
    @Nonnull
    @CheckReturnValue
    public final synchronized ServiceClassLoader getClassLoader()
    {
        if (_classLoader == null) {
            setClassLoader(Optional.empty());
        }

        return _classLoader;
    }

    /**
     * Gets the 'config.dir' property.
     *
     * @return The optional 'config.dir' property.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getConfigDir()
    {
        return getStringValue(CONFIG_DIR_PROPERTY);
    }

    /**
     * Gets the data directory.
     *
     * @return The data directory.
     */
    @Nonnull
    @CheckReturnValue
    public final File getDataDir()
    {
        if (_dataDir == null) {
            _dataDir = dataDir(
                Optional.empty(),
                getProperties(),
                SERVICE_DATA_DIR_PROPERTY,
                DEFAULT_SERVICE_DATA_DIR);
            getThisLogger()
                .debug(ServiceMessages.DATA_DIR, _dataDir.getAbsolutePath());
        }

        return _dataDir;
    }

    /**
     * Gets a double value, providing a default.
     *
     * <p>If the key is not found or its value is unrecognized the default will
     * be returned.</p>
     *
     * @param key The key for the value.
     * @param defaultValue The default value.
     *
     * @return The requested value or the provided default.
     */
    @CheckReturnValue
    public final double getDoubleValue(
            final String key,
            final double defaultValue)
    {
        return _properties.getDouble(key, defaultValue);
    }

    /**
     * Gets an elapsed time value, providing a default.
     *
     * @param key The key for the value.
     * @param defaultValue The optional default value.
     * @param emptyValue The optional assumed value for empty.
     *
     * @return The requested value, empty, or the provided default.
     *
     * @see org.rvpf.base.util.container.KeyedValues#getElapsed KeyedValues
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ElapsedTime> getElapsedValue(
            @Nonnull final String key,
            @Nonnull final Optional<ElapsedTime> defaultValue,
            @Nonnull final Optional<ElapsedTime> emptyValue)
    {
        return _properties.getElapsed(key, defaultValue, emptyValue);
    }

    /**
     * Gets the entities.
     *
     * @return The optional entities.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<HashMap<String, Entity>> getEntities()
    {
        return _state.getEntities();
    }

    /**
     * Gets a File instance for a path.
     *
     * <p>If the path is relative, it will use either the config URL as base if
     * it is a file, or the current directory.</p>
     *
     * @param path The path.
     *
     * @return The File instance.
     */
    @Nonnull
    @CheckReturnValue
    public final File getFile(final String path)
    {
        File file = new File(path);

        if (!file.isAbsolute()) {
            final URL url = getURL();
            URI uri;

            try {
                uri = url.toURI();

                if (ResourceFileFactory.FILE_PROTOCOL
                    .equalsIgnoreCase(uri.getScheme())) {
                    uri = new File(uri).getParentFile().toURI();
                } else {
                    uri = CWD_URI;
                }
            } catch (final URISyntaxException exception) {
                uri = CWD_URI;
            }

            file = new File(new File(uri), path);
        }

        return file;
    }

    /**
     * Gets an int value, providing a default.
     *
     * <p>If the key is not found or its value is unrecognized the default will
     * be returned.</p>
     *
     * @param key The key for the value.
     * @param defaultValue The default value.
     *
     * @return The requested value or the provided default.
     */
    @CheckReturnValue
    public final int getIntValue(
            @Nonnull final String key,
            final int defaultValue)
    {
        return _properties.getInt(key, defaultValue);
    }

    /**
     * Gets a password value.
     *
     * @param key The key for the value.
     *
     * @return The optional password value.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<char[]> getPasswordValue(@Nonnull final String key)
    {
        return _properties.getPassword(key);
    }

    /**
     * Gets the values.
     *
     * @return The values.
     */
    @Nonnull
    @CheckReturnValue
    public final ConfigProperties getProperties()
    {
        return _properties;
    }

    /**
     * Gets a group of properties.
     *
     * @param key The name of the group.
     *
     * @return The properties.
     */
    @Nonnull
    @CheckReturnValue
    public final KeyedGroups getPropertiesGroup(@Nonnull final String key)
    {
        return _properties.getGroup(Require.notNull(key));
    }

    /**
     * Gets the groups of configuration properties for a specified key.
     *
     * @param key The name of the groups.
     *
     * @return An array of configuration property groups (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public final KeyedGroups[] getPropertiesGroups(@Nonnull final String key)
    {
        return _properties.getGroups(key);
    }

    /**
     * Gets the root element name of the document used to load this config.
     *
     * @return The root element name.
     */
    @Nonnull
    @CheckReturnValue
    public final String getRootName()
    {
        return Require.notNull(_rootName);
    }

    /**
     * Gets the service owning this config.
     *
     * @return The service.
     */
    @Nonnull
    @CheckReturnValue
    public final Service getService()
    {
        return Require.notNull(_service);
    }

    /**
     * Gets a service context.
     *
     * <p>The service aliases will be searched when the key does not correspond
     * to a service definition.</p>
     *
     * @param key The name of the service.
     *
     * @return The service context (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ServiceContext> getServiceContext(
            @Nonnull final String key)
    {
        final String serviceKey = key.toUpperCase(Locale.ROOT);
        ServiceContext serviceContext;

        serviceContext = _serviceContexts.get(serviceKey);

        if (serviceContext == null) {
            serviceContext = _servicesByAlias.get(serviceKey);
        }

        return Optional.ofNullable(serviceContext);
    }

    /**
     * Gets the service contexts.
     *
     * @return The service contexts.
     */
    @Nonnull
    @CheckReturnValue
    public final Collection<ServiceContext> getServiceContexts()
    {
        return _serviceContexts.values();
    }

    /**
     * Gets the service name.
     *
     * @return The service name.
     */
    @Nonnull
    @CheckReturnValue
    public final String getServiceName()
    {
        return _serviceName;
    }

    /**
     * Gets a service name.
     *
     * @param key Either an alias or the name itself.
     *
     * @return The service name (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getServiceName(final String key)
    {
        final Optional<ServiceContext> serviceContext = getServiceContext(key);

        return (serviceContext
            .isPresent())? Optional
                .of(serviceContext.get().getServiceName()): Optional.empty();
    }

    /**
     * Gets the service properties.
     *
     * @return The service properties.
     */
    @Nonnull
    @CheckReturnValue
    public final ConfigProperties getServiceProperties()
    {
        final ConfigProperties overrider = _properties.getOverrider().get();

        return overrider.getOverriden().get();
    }

    /**
     * Gets the service UUID.
     *
     * @return The optional service UUID.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<UUID> getServiceUUID()
    {
        return (_service != null)? _service.getServiceUUID(): Optional.empty();
    }

    /**
     * Gets the last modified stamp.
     *
     * @return The optional last modified stamp.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<DateTime> getStamp()
    {
        return Optional.ofNullable(_stamp);
    }

    /**
     * Gets a string value.
     *
     * @param key The key for the value.
     *
     * @return The requested value or empty.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getStringValue(@Nonnull final String key)
    {
        return getStringValue(key, Optional.empty());
    }

    /**
     * Gets a string value, providing a default.
     *
     * @param key The key for the value.
     * @param defaultValue The default value.
     *
     * @return The requested value or the provided default.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getStringValue(
            @Nonnull final String key,
            @Nonnull final Optional<String> defaultValue)
    {
        return _properties.getString(key, defaultValue);
    }

    /**
     * Gets the string values for the specified key.
     *
     * @param key The key for the values.
     *
     * @return An array of values (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public final String[] getStringValues(@Nonnull final String key)
    {
        return _properties.getStrings(Require.notNull(key));
    }

    /**
     * Gets the URL from which this Config was loaded.
     *
     * @return The URL.
     */
    @Nonnull
    @CheckReturnValue
    public URL getURL()
    {
        return Require.notNull(_url);
    }

    /**
     * Asks if this configuration is linked to a service.
     *
     * @return True if this configuration is linked to a service.
     */
    @CheckReturnValue
    public final boolean hasService()
    {
        return _service != null;
    }

    /**
     * Asks if the supplied URL is already included.
     *
     * <p>A subsequent call with an URL which is equal to the supplied URL will
     * return true. This is used while loading the documents to avoid multiple
     * inclusions of the same document.</p>
     *
     * @param url The supplied URL.
     *
     * @return True if already included.
     */
    @CheckReturnValue
    public boolean isIncluded(@Nonnull final URL url)
    {
        return !_state.include(url);
    }

    /**
     * Keeps the entities.
     *
     * <p>Keeps alive the entities, even if they are not referenced.</p>
     *
     * @param entities The object containing references to all the entities.
     */
    public final void keepEntities(
            @Nonnull final Optional<HashMap<String, Entity>> entities)
    {
        _state.setEntities(entities);
    }

    /**
     * Registers a ClassLib.
     *
     * @param classLibName The name of the ClassLib.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean registerClassLib(@Nonnull final String classLibName)
    {
        final Optional<ClassLibEntity> classLib = getClassLibEntity(
            classLibName);

        if (!classLib.isPresent()) {
            getThisLogger()
                .warn(ServiceMessages.CLASS_LIB_UNKNOWN, classLibName);

            return false;
        }

        if (_classLoader == null) {
            registerClassLoader();
        }

        try {
            _classLoader.addFromClassLib(classLib.get());
        } catch (final UndefinedEntityException exception) {
            getThisLogger().warn(BaseMessages.VERBATIM, exception.getMessage());

            return false;
        }

        return true;
    }

    /**
     * Registers the class loader.
     */
    public final void registerClassLoader()
    {
        getClassLoader().activate();
    }

    /**
     * Restores the state.
     *
     * <p>Used to cancel state modifications caused by the repeated loading of
     * child documents (metadata).</p>
     *
     * @param state The saved state.
     */
    public final void restoreState(@Nonnull final Object state)
    {
        _state = new _State((_State) state);
    }

    /**
     * Returns the entities that were kept.
     *
     * @return The optional entities.
     */
    @Nonnull
    public final Optional<HashMap<String, Entity>> returnEntities()
    {
        final Optional<HashMap<String, Entity>> entities = _state.getEntities();

        _state.setEntities(Optional.empty());

        return entities;
    }

    /**
     * Saves the state.
     *
     * @return A copy of the state.
     */
    @Nonnull
    @CheckReturnValue
    public final Object saveState()
    {
        return new _State(_state);
    }

    /**
     * Sets the service class loader.
     *
     * <p>When the service class loader is not supplied, an instance is
     * requested from its class.</p>
     *
     * @param classLoader The service class loader (may be empty).
     */
    public final synchronized void setClassLoader(
            @Nonnull final Optional<ServiceClassLoader> classLoader)
    {
        _classLoader = classLoader.orElse(ServiceClassLoader.getInstance());
        _classLoader.useConfig(this);
    }

    /**
     * Sets the 'config.dir' property.
     *
     * @param dir The new value for the 'config.dir' property.
     */
    public final void setConfigDir(@Nonnull final String dir)
    {
        _properties.setValue(CONFIG_DIR_PROPERTY, dir);
    }

    /**
     * Sets the root element name of the document used to load this Config.
     *
     * @param rootName The root element name.
     */
    public final void setRootName(@Nonnull final String rootName)
    {
        _rootName = rootName;
    }

    /**
     * Sets the service owning this Config.
     *
     * @param service The service.
     */
    public final void setService(@Nonnull final Service service)
    {
        _service = service;
    }

    /**
     * Sets the service properties.
     *
     * @param properties The service properties.
     */
    public final void setServiceProperties(
            @Nonnull final ConfigProperties properties)
    {
        final ConfigProperties overrider = _properties.getOverrider().get();

        overrider.setOverriden(properties);
    }

    /**
     * Sets the URL from which this Config was loaded.
     *
     * @param url The URL.
     */
    public void setURL(@Nonnull final URL url)
    {
        _url = url;
    }

    /**
     * Sets up the service.
     *
     * <p>Establishes the set of properties that are specific to the current
     * service.</p>
     */
    public final void setUpService()
    {
        setServiceProperties(getServiceContext(getServiceName()).get());
    }

    /**
     * Substitutes markers in parameter string values.
     *
     * @param params The original parameters.
     *
     * @return Cloned parameters with markers substituted.
     */
    @Nonnull
    @CheckReturnValue
    public final Params substitute(@Nonnull final Params params)
    {
        final Params clonedParams = params.copy();

        for (final Map.Entry<String, List<Object>> entry:
                clonedParams.getValuesEntries()) {
            for (final ListIterator<Object> iterator =
                    entry.getValue().listIterator();
                    iterator.hasNext(); ) {
                final Object value = iterator.next();

                if (value instanceof String) {
                    iterator
                        .set(getProperties().substitute((String) value, false));
                }
            }
        }

        clonedParams.freeze();

        return clonedParams;
    }

    /**
     * Substitutes markers in a given text.
     *
     * <p>A substitution marker would be a '${x}' property reference.</p>
     *
     * <p>Note: should be called only while substitutions are enabled.</p>
     *
     * @param text The text possibly containing substitution markers.
     * @param deferred True if substitution of '$${x}' should be deferred.
     *
     * @return The text with markers substituted.
     */
    @Nonnull
    @CheckReturnValue
    public final String substitute(
            @Nonnull final String text,
            final boolean deferred)
    {
        return getProperties().substitute(text, deferred);
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        final ServiceClassLoader classLoader = _classLoader;

        if (classLoader != null) {
            _classLoader = null;
            classLoader.forgetConfig(this);
        }

        _serviceContexts.clear();
        _servicesByAlias.clear();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getClass()
            .getName() + "@" + Integer.toHexString(
                System.identityHashCode(this));
    }

    /**
     * Updates the last modified stamp.
     *
     * @param stamp The last modified stamp.
     */
    public final void updateStamp(@Nonnull final Optional<DateTime> stamp)
    {
        if ((_stamp == null)
                || (stamp.isPresent() && stamp.get().isAfter(_stamp))) {
            _stamp = stamp.orElse(null);
        }
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return _logger;
    }

    private static void createFile(
            final File directory,
            final String filename,
            final String content)
        throws IOException
    {
        final Writer writer = new OutputStreamWriter(
            new FileOutputStream(new File(directory, filename)),
            StandardCharsets.UTF_8);

        writer.write(content);
        writer.close();
    }

    /**
     * Fetches resource properties.
     *
     * <p>The system property '{@value #RVPF_PROPERTIES}' names resource files
     * containing configuration properties (defaults to
     * '{@value #RVPF_PROPERTIES}'). A configuration property loaded from a
     * resource file has precedence on the same property loaded from a
     * subsequent resource file.</p>
     */
    private void _fetchResourceProperties()
    {
        final String resourceName = System
            .getProperty(RVPF_PROPERTIES, RVPF_PROPERTIES);
        final Optional<ConfigProperties> systemProperties = _properties
            .getOverrider();
        final boolean substitutionDeferred = systemProperties
            .isPresent()? systemProperties
                .get()
                .getBoolean(SUBSTITUTION_DEFERRED_PROPERTY): false;
        boolean substitutionEnabled = systemProperties
            .isPresent()? systemProperties
                .get()
                .getBoolean(SUBSTITUTION_ENABLED_PROPERTY): false;

        try {
            final Enumeration<URL> resources = Thread
                .currentThread()
                .getContextClassLoader()
                .getResources(resourceName);

            if (!resources.hasMoreElements()) {
                getThisLogger()
                    .info(ServiceMessages.NO_RESOURCES, resourceName);
            }

            for (final Enumeration<URL> i = resources; i.hasMoreElements(); ) {
                final URL url = i.nextElement();

                getThisLogger().debug(ServiceMessages.LOADING_PROPERTIES, url);

                final DocumentPropertiesMap properties = DocumentPropertiesMap
                    .fetch(DocumentStream.create(url), Optional.empty());

                updateStamp(properties.getStamp());

                for (final Map.Entry<String, String> entry:
                        properties.entrySet()) {
                    final String key = entry.getKey();

                    if (!systemProperties.get().containsValueKey(key)) {
                        String value = entry.getValue();

                        if (substitutionEnabled && (value != null)) {
                            value = systemProperties
                                .get()
                                .substitute(value, substitutionDeferred);
                        }

                        if (value != null) {
                            systemProperties.get().setValue(key, value);
                        } else {
                            systemProperties.get().removeValue(key);
                        }

                        if (key.equals(SUBSTITUTION_ENABLED_PROPERTY)) {
                            substitutionEnabled = ValueConverter
                                .convertToBoolean(
                                    systemProperties.get().getType(),
                                    key,
                                    Optional.ofNullable(value),
                                    false);
                        }
                    }
                }
            }
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /** Contains the path to the cache directory. */
    public static final String CACHE_DIR_PROPERTY = "cache.dir";

    /**
     * This property is automagically created when the configuration file
     * loading is initiated. It contains the path to the configuration
     * directory.
     */
    public static final String CONFIG_DIR_PROPERTY = "config.dir";

    /** Current working directory URI. */
    public static final URI CWD_URI = new File(System.getProperty("user.dir"))
        .toURI();

    /** Default cache directory. */
    public static final String DEFAULT_CACHE_DIR = "cache";

    /** Default service data directory path. */
    public static final String DEFAULT_SERVICE_DATA_DIR = "data";

    /** Holds the path to the initial properties text. */
    public static final String RVPF_PROPERTIES = "rvpf.properties";

    /** Specifies the service data directory path. */
    public static final String SERVICE_DATA_DIR_PROPERTY = "service.data.dir";

    /** Service file name. */
    public static final String SERVICE_FILENAME = "service";

    /** Asks to defer substitution when a marker is of the form '$${x}'. */
    public static final String SUBSTITUTION_DEFERRED_PROPERTY =
        "substitution.deferred";

    /**
     * Enables or disables the marker substitution mode where a marker is of
     * the form '${x}'. The 'x' would be the name of a property.
     */
    public static final String SUBSTITUTION_ENABLED_PROPERTY =
        "substitution.enabled";

    /** The system property prefix for RVPF specific properties. */
    public static final String SYSTEM_PROPERTY_PREFIX = "rvpf.";

    /** UUID file name. */
    public static final String UUID_FILENAME = "UUID";

    private Map<String, ClassDefEntity> _classDefsByName;
    private Map<String, ClassLibEntity> _classLibsByName;
    private volatile ServiceClassLoader _classLoader;
    private volatile File _dataDir;
    private final Logger _logger = Logger.getInstance(getClass());
    private final ConfigProperties _properties;
    private String _rootName;
    private Service _service;
    private final Map<String, ServiceContext> _serviceContexts =
        new HashMap<String, ServiceContext>();
    private final String _serviceName;
    private final Map<String, ServiceContext> _servicesByAlias =
        new HashMap<String, ServiceContext>();
    private DateTime _stamp;
    private _State _state = new _State();
    private URL _url;

    /**
     * Config tate.
     */
    private static final class _State
    {
        /**
         * Constructs an instance.
         */
        _State()
        {
            _included = new HashSet<String>();
        }

        /**
         * Constructs an instance.
         *
         * @param state An input state.
         */
        _State(final _State state)
        {
            _entities = new HashMap<String, Entity>(state._entities);
            _included = new HashSet<String>(state._included);
        }

        /**
         * Gets the entities.
         *
         * @return The optional entities.
         */
        Optional<HashMap<String, Entity>> getEntities()
        {
            return Optional.ofNullable(_entities);
        }

        /**
         * Includes an URL.
         *
         * @param url The URL.
         *
         * @return True if added.
         */
        boolean include(final URL url)
        {
            return _included.add(url.toExternalForm());
        }

        /**
         * Sets The entities.
         *
         * @param entities The entities.
         */
        void setEntities(final Optional<HashMap<String, Entity>> entities)
        {
            _entities = entities.orElse(null);
        }

        private HashMap<String, Entity> _entities;
        private final Set<String> _included;
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

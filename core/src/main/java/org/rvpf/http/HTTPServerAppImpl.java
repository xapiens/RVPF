/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: HTTPServerAppImpl.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.http;

import java.io.IOException;

import java.net.URL;
import java.net.URLConnection;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.net.ssl.SSLException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.alert.Event;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.security.ServerSecurityContext;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.service.metadata.MetadataService;
import org.rvpf.service.metadata.app.MetadataServiceAppImpl;

/**
 * HTTP server application implementation.
 */
public final class HTTPServerAppImpl
    extends MetadataServiceAppImpl
    implements ServiceThread.Target
{
    /** {@inheritDoc}
     */
    @Override
    public boolean onEvent(final Event event)
    {
        for (final HTTPModule module: _eventActionsCallbacks) {
            module.doEventActions(event);
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onSignal(final Signal signal)
    {
        for (final HTTPModule module: _signalActionsCallbacks) {
            module.doSignalActions(signal);
        }

        return _needsMetadata
               || !MetadataService.REFRESH_METADATA_SIGNAL.equalsIgnoreCase(
                   signal.getName());
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws InterruptedException
    {
        for (;;) {
            final HTTPModule[] modulesToCall;

            synchronized (_mutex) {
                while (_pendingActionsCallbacks.isEmpty()) {
                    _mutex.wait();
                }

                modulesToCall = _pendingActionsCallbacks
                    .toArray(new HTTPModule[_pendingActionsCallbacks.size()]);
                _pendingActionsCallbacks.clear();
            }

            for (final HTTPModule moduleToCall: modulesToCall) {
                moduleToCall.doPendingActions();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final MetadataService service)
    {
        if (!super.setUp(service)) {
            return false;
        }

        final Config config = service.getConfig();

        final ClassDef classDef = config
            .getClassDef(
                config
                    .getStringValue(
                            HTTP_SUPPORT_CLASS_PROPERTY,
                                    Optional.of(DEFAULT_HTTP_SUPPORT_CLASS))
                    .orElse(null),
                Optional.empty())
            .get();

        _support = classDef.createInstance(HTTPSupport.class);

        if (_support == null) {
            return false;
        }

        final KeyedGroups serverProperties = config
            .getPropertiesGroup(SERVER_PROPERTIES);

        if (!_setUpListeners(serverProperties)) {
            return false;
        }

        if (!_setUpRealms(serverProperties)) {
            return false;
        }

        if (!_setUpContexts(serverProperties)) {
            return false;
        }

        return _setUpServer();
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        try {
            _support.startServer();
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }

        final ServiceThread thread = new ServiceThread(this, "HTTP server");

        if (_thread.compareAndSet(null, thread)) {
            getThisLogger()
                .debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            Require
                .ignored(
                    thread.interruptAndJoin(getThisLogger(), getJoinTimeout()));

            _stopServer();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        // Clears the callback sets.

        synchronized (_mutex) {
            _eventActionsCallbacks.clear();
            _signalActionsCallbacks.clear();
            _pendingActionsCallbacks.clear();
        }

        // Tears down each module.

        for (final HTTPModule module: _modules) {
            module.tearDown();
        }

        _modules.clear();

        // Tears down the HTTP server.

        if (_support != null) {
            if (_support.isServerStarted()) {
                _stopServer();
            }

            try {
                _support.destroyServer();
            } catch (final Exception exception) {
                getThisLogger()
                    .warn(HTTPMessages.SERVER_DESTROY_FAILED, exception);
            }

            _support = null;
        }

        super.tearDown();
    }

    /**
     * Requests a callback to a server module for event actions.
     *
     * <p>The callback will happen on each event.</p>
     *
     * @param serverModule The server module to call back.
     */
    void callbackForEventActions(@Nonnull final HTTPModule serverModule)
    {
        synchronized (_mutex) {
            _eventActionsCallbacks.add(serverModule);
            _mutex.notifyAll();
        }
    }

    /**
     * Requests a callback to a server module for pending actions.
     *
     * <p>The callback will happen on each event.</p>
     *
     * @param serverModule The server module to call back.
     */
    void callbackForPendingActions(@Nonnull final HTTPModule serverModule)
    {
        synchronized (_mutex) {
            _pendingActionsCallbacks.add(serverModule);
            _mutex.notifyAll();
        }
    }

    /**
     * Requests a callback to a server module for signal actions.
     *
     * <p>The callback will happen on each event.</p>
     *
     * @param serverModule The server module to call back.
     */
    void callbackForSignalActions(@Nonnull final HTTPModule serverModule)
    {
        synchronized (_mutex) {
            _signalActionsCallbacks.add(serverModule);
            _mutex.notifyAll();
        }
    }

    /**
     * Gets the listener count.
     *
     * @return The listener count.
     */
    @CheckReturnValue
    int getListenerCount()
    {
        return _support.getListenerCount();
    }

    /**
     * Gets the listener host.
     *
     * @param index The listener index.
     *
     * @return The optional listener host.
     */
    Optional<String> getListenerHost(final int index)
    {
        return _support.getListenerAddress(index);
    }

    /**
     * Gets the listener port.
     *
     * @param index The listener index.
     *
     * @return The listener port.
     */
    int getListenerPort(final int index)
    {
        return _support.getListenerPort(index);
    }

    private boolean _setUpContexts(final KeyedGroups serverProperties)
    {
        final KeyedGroups[] groups = serverProperties
            .getGroups(CONTEXT_PROPERTIES);

        if (groups.length == 0) {
            getThisLogger().error(HTTPMessages.NO_CONTEXT);

            return false;
        }

        getService().saveConfigState();

        for (final KeyedGroups contextProperties: groups) {
            final Optional<ClassDef> classDef = contextProperties
                .getClassDef(MODULE_CLASS_PROPERTY, Optional.empty());
            final HTTPModule module;
            String path = contextProperties
                .getString(PATH_PROPERTY)
                .orElse(null);

            if (classDef.isPresent()) {
                module = classDef.get().createInstance(HTTPModule.class);

                if (module == null) {
                    return false;
                }

                if (path == null) {
                    path = module.getDefaultPath();
                }
            } else {
                module = null;
            }

            if (path == null) {
                getThisLogger().error(HTTPMessages.NO_CONTEXT_PATH);

                return false;
            }

            String resource = contextProperties
                .getString(RESOURCE_PROPERTY)
                .orElse(null);

            if ((resource == null) && !classDef.isPresent()) {
                getThisLogger().error(HTTPMessages.NO_CONTEXT_CONTENT, path);

                return false;
            }

            if (resource != null) {
                final Config config = getService().getConfig();
                final URL resourceURL = config
                    .createURL(resource, Optional.of(config.getURL()));

                if (resourceURL == null) {
                    getThisLogger()
                        .error(HTTPMessages.BAD_RESOURCE_URL, resource);

                    return false;
                }

                try {
                    final URLConnection connection = resourceURL
                        .openConnection();

                    connection.setDoInput(false);
                    connection.connect();
                } catch (final IOException exception) {
                    getThisLogger()
                        .warn(
                            HTTPMessages.RESOURCE_ACCESS,
                            resourceURL,
                            exception.getMessage());
                }

                resource = resourceURL.toString();
            }

            final Map<String, String> servlets = new LinkedHashMap<>();

            if (module != null) {
                getService().starting();    // Extends the start up time.

                if (!module.setUp(this, servlets, contextProperties)) {
                    module.tearDown();

                    return false;
                }

                getService().restoreConfigState();

                _needsMetadata |= module.needsMetadata();
                _modules.add(module);

                getThisLogger()
                    .info(
                        HTTPMessages.CONTEXT_LOADED_MODULE,
                        path,
                        module.getClass().getName());
            }

            final String[] roles = contextProperties.getStrings(ROLE_PROPERTY);
            final String authenticator;
            boolean confidential = contextProperties
                .getBoolean(CONFIDENTIAL_PROPERTY);

            if (confidential || (roles.length > 0)) {
                authenticator = contextProperties
                    .getString(
                        AUTHENTICATOR_PROPERTY,
                        Optional.of(HTTPSupport.DIGEST_AUTHENTICATOR))
                    .get();

                if (HTTPSupport.BASIC_AUTHENTICATOR
                    .equalsIgnoreCase(authenticator)) {
                    confidential = true;
                }

                getThisLogger()
                    .debug(HTTPMessages.AUTHENTICATION, authenticator);
            } else {
                authenticator = null;
            }

            final ServletContextListener contextListener = (!servlets
                .isEmpty())? new ServletContextListener()
            {
                /** {@inheritDoc}
                 */
                @Override
                public void contextDestroyed(final ServletContextEvent event) {}

                /** {@inheritDoc}
                 */
                @Override
                public void contextInitialized(final ServletContextEvent event)
                {
                    Require.notNull(module);
                    module.prepareServletContext(event.getServletContext());
                }
            }: null;

            final Optional<String> realmName = contextProperties
                .getString(REALM_PROPERTY);

            if (!_support
                .setUpContext(
                    path,
                    Optional.ofNullable(resource),
                    servlets,
                    realmName,
                    Optional.ofNullable(authenticator),
                    confidential,
                    roles,
                    Optional.ofNullable(contextListener))) {
                return false;
            }
        }

        return true;
    }

    private boolean _setUpListeners(final KeyedGroups serverProperties)
    {
        KeyedGroups[] groups = serverProperties.getGroups(LISTENER_PROPERTIES);

        if (groups.length == 0) {    // Use a listener with default values.
            final KeyedGroups group = new KeyedGroups();

            group.freeze();
            groups = new KeyedGroups[] {group, };
        }

        for (final KeyedGroups listenerProperties: groups) {
            final ServerSecurityContext securityContext =
                new ServerSecurityContext(
                    getThisLogger());
            final KeyedGroups securityProperties = listenerProperties
                .getGroup(SecurityContext.SECURITY_PROPERTIES);

            if (!securityContext
                .setUp(KeyedGroups.MISSING_KEYED_GROUP, securityProperties)) {
                return false;
            }

            final boolean secure = securityContext.isCertified()
                    || securityContext.isSecure()
                    || (!securityProperties.isEmpty());
            final String addressString = listenerProperties
                .getString(ADDRESS_PROPERTY, Optional.of(DEFAULT_ADDRESS))
                .get()
                .trim();

            if (secure) {
                final int port = listenerProperties
                    .getInt(PORT_PROPERTY, DEFAULT_HTTPS_PORT);

                getThisLogger()
                    .debug(
                        HTTPMessages.ADDING_LISTENER,
                        String.valueOf(port),
                        Integer.valueOf(0),
                        null);

                try {
                    securityContext.checkForSecureOperation();
                } catch (final SSLException exception) {
                    getThisLogger()
                        .error(BaseMessages.VERBATIM, exception.getMessage());

                    return false;
                }

                if (!_support
                    .setUpListener(
                        addressString,
                        port,
                        0,
                        Optional.of(securityContext))) {
                    return false;
                }
            } else {
                // A confidential (HTTPS) port may be associated with this
                // non confidential (HTTP) listener.
                final int confidential = listenerProperties
                    .getInt(CONFIDENTIAL_PROPERTY, 0);
                final int port = listenerProperties
                    .getInt(PORT_PROPERTY, DEFAULT_HTTP_PORT);

                getThisLogger()
                    .debug(
                        HTTPMessages.ADDING_LISTENER,
                        String.valueOf(port),
                        Integer.valueOf(confidential),
                        String.valueOf(confidential));

                if (!_support
                    .setUpListener(
                        addressString,
                        port,
                        confidential,
                        Optional.empty())) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean _setUpRealms(final KeyedGroups serverProperties)
    {
        final KeyedGroups[] groups = serverProperties
            .getGroups(REALM_PROPERTIES);

        getThisLogger().debug(HTTPMessages.ADDING_REALMS);

        for (KeyedGroups realmProperties: groups) {
            final String name = realmProperties
                .getString(NAME_PROPERTY)
                .orElse(null);

            if ((name == null) || name.isEmpty()) {
                getThisLogger().error(HTTPMessages.NO_REALM_NAME);

                return false;
            }

            final SecurityContext securityContext = new SecurityContext(
                getThisLogger());

            if (!securityContext
                .setUp(
                    getConfigProperties(),
                    realmProperties
                        .getGroup(SecurityContext.SECURITY_PROPERTIES))) {
                return false;
            }

            final Optional<String> path = realmProperties
                .getString(PATH_PROPERTY);

            if (!path.isPresent()) {
                realmProperties = realmProperties.copy();
                realmProperties
                    .setValue(
                        SecurityContext.PATH_PROPERTY,
                        securityContext
                            .getRealmProperties()
                            .getString(PATH_PROPERTY)
                            .orElse(null));
            }

            if (!_support.setUpRealm(name, realmProperties, securityContext)) {
                return false;
            }
        }

        return true;
    }

    private boolean _setUpServer()
    {
        return _support.setUpServer(this);
    }

    private void _stopServer()
    {
        if (_stopping.compareAndSet(false, true)) {
            getThisLogger().debug(HTTPMessages.STOPPING_SERVER);

            try {
                _support.stopServer();
                getThisLogger().debug(HTTPMessages.SERVER_STOPPED);
            } catch (final InterruptedException exception) {
                getThisLogger().warn(ServiceMessages.INTERRUPTED);
                Thread.currentThread().interrupt();
            } catch (final Exception exception) {
                getThisLogger()
                    .warn(HTTPMessages.SERVER_STOP_FAILED, exception);
            }
        }
    }

    /** Specifies on which network address to listen. */
    public static final String ADDRESS_PROPERTY = "address";

    /**
     * The Authenticator required for this context. It must be one of 'Basic',
     * 'Digest' and 'Certificate'. The default is 'Digest'.
     */
    public static final String AUTHENTICATOR_PROPERTY = "authenticator";

    /** Specifies if the communication channel must be confidential. */
    public static final String CONFIDENTIAL_PROPERTY = "confidential";

    /** Specifies the path to the user-password-roles configuration file. */
    public static final String CONFIG_PROPERTY = "config";

    /** Properties used to define an HTTP server context. */
    public static final String CONTEXT_PROPERTIES = "context";

    /** Default address. */
    public static final String DEFAULT_ADDRESS = Inet.LOCAL_HOST;

    /** Default HTTPS port. */
    public static final int DEFAULT_HTTPS_PORT = 443;

    /** Default HTTP port. */
    public static final int DEFAULT_HTTP_PORT = 80;

    /** Default HTTP support. */
    public static final String DEFAULT_HTTP_SUPPORT_CLASS = "HTTPJettySupport";

    /** HTTP server support class. */
    public static final String HTTP_SUPPORT_CLASS_PROPERTY =
        "http.support.class";

    /** Properties used to define an HTTP server listener. */
    public static final String LISTENER_PROPERTIES = "listener";

    /** The class of the module to activate for the context. */
    public static final String MODULE_CLASS_PROPERTY = "module.class";

    /** Specifies the name of the realm. */
    public static final String NAME_PROPERTY = "name";

    /** The relative path pointing to the context. */
    public static final String PATH_PROPERTY = "path";

    /** Specifies on which network port to listen. */
    public static final String PORT_PROPERTY = "port";

    /** Properties used to define an HTTP server realm. */
    public static final String REALM_PROPERTIES = "realm";

    /** The realm used for the context. */
    public static final String REALM_PROPERTY = "realm";

    /** The path to a resource directory. */
    public static final String RESOURCE_PROPERTY = "resource";

    /** The security role needed to access the context. */
    public static final String ROLE_PROPERTY = "role";

    /** Server properties. */
    public static final String SERVER_PROPERTIES = "http.server";

    private final Set<HTTPModule> _eventActionsCallbacks =
        new LinkedHashSet<>();
    private final Collection<HTTPModule> _modules = new LinkedList<>();
    private final Object _mutex = new Object();
    private volatile boolean _needsMetadata;
    private final Set<HTTPModule> _pendingActionsCallbacks =
        new LinkedHashSet<>();
    private final Set<HTTPModule> _signalActionsCallbacks =
        new LinkedHashSet<>();
    private final AtomicBoolean _stopping = new AtomicBoolean();
    private volatile HTTPSupport _support;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
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

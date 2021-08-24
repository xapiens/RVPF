/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TomcatHTTPSupport.java 3990 2019-05-15 20:45:55Z SFB $
 */

package org.rvpf.ext.tomcat;

import java.beans.PropertyChangeListener;

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.catalina.Context;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.authenticator.DigestAuthenticator;
import org.apache.catalina.authenticator.SSLAuthenticator;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.scan.StandardJarScanner;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.KeyStoreConfig;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.security.ServerSecurityContext;
import org.rvpf.base.security.TrustStoreConfig;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.ext.ExtMessages;
import org.rvpf.http.HTTPServerAppImpl;
import org.rvpf.http.HTTPSupport;

/**
 * Tomcat HTTP support.
 */
public class TomcatHTTPSupport
    implements HTTPSupport
{
    /**
     * Constructs an instance.
     */
    public TomcatHTTPSupport()
    {
        _server = new _Server();
        _server.setBaseDir(".");

        TomcatURLStreamHandlerFactory.disable();
    }

    /** {@inheritDoc}
     */
    @Override
    public void destroyServer()
        throws Exception
    {
        _server.destroy();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getListenerAddress(final int index)
    {
        final Object address = _server
            ._getConnectors()[index]
            .getAttribute("address");

        return (address != null)? Optional
            .of(address.toString()): Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public int getListenerCount()
    {
        return _server._getConnectors().length;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getListenerPort(final int index)
    {
        return _server._getConnectors()[index].getLocalPort();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isServerStarted()
    {
        return _started.get();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUpContext(
            final String path,
            final Optional<String> resource,
            final Map<String, String> servlets,
            final Optional<String> realmName,
            final Optional<String> optionalAuthenticatorName,
            final boolean confidential,
            final String[] roles,
            final Optional<EventListener> eventListener)
    {
        final String contextPath = path.isEmpty()? path: ("/" + path);

        _LOGGER.debug(ExtMessages.ADDING_CONTEXT, contextPath);

        final StandardContext context = (StandardContext) _server
            .addContext(contextPath, _DEFAULT_DOC_BASE);

        context.setLoader(new _Loader());

        if (eventListener != null) {
            context.addApplicationLifecycleListener(eventListener);
        }

        final SecurityCollection collection = new SecurityCollection();

        for (final Map.Entry<String, String> entry: servlets.entrySet()) {
            final String servletClassName = entry.getValue();
            final String servletPath = entry.getKey();
            final String servletName = String.valueOf(++_servletName);

            Tomcat.addServlet(context, servletName, servletClassName);
            context.addServletMapping(servletPath, servletName);
            collection.addPattern(servletPath);
        }

        if (roles.length > 0) {
            final TomcatRealm realm;

            if (realmName.isPresent()) {
                realm = _realms.get(realmName.get());
            } else if (_realms.size() == 1) {
                realm = _realms.values().toArray(new TomcatRealm[1])[0];
            } else if (_realms.isEmpty()) {
                _LOGGER.error(ExtMessages.NO_REALM);

                return false;
            } else {
                _LOGGER.error(ExtMessages.NO_CONTEXT_REALM);

                return false;
            }

            if (realm == null) {
                _LOGGER
                    .error(
                        ExtMessages.CONTEXT_REALM_CONFIG,
                        realmName.orElse(null));

                return false;
            }

            context.setRealm(realm);

            final SecurityConstraint constraint = new SecurityConstraint();

            for (final String role: roles) {
                _LOGGER.debug(ExtMessages.ROLE, role);
                constraint.addAuthRole(role);
            }

            constraint.addCollection(collection);
            context.addConstraint(constraint);

            final String authenticatorName = optionalAuthenticatorName
                .orElse(null);
            final LoginConfig loginConfig = new LoginConfig();
            final AuthenticatorBase authenticator;

            if (DIGEST_AUTHENTICATOR.equalsIgnoreCase(authenticatorName)) {
                loginConfig.setAuthMethod("DIGEST");
                authenticator = new DigestAuthenticator();
            } else if (CERTIFICATE_AUTHENTICATOR
                .equalsIgnoreCase(authenticatorName)) {
                loginConfig.setAuthMethod("CLIENT-CERT");
                authenticator = new SSLAuthenticator();
            } else if (BASIC_AUTHENTICATOR
                .equalsIgnoreCase(authenticatorName)) {
                loginConfig.setAuthMethod("BASIC");
                authenticator = new BasicAuthenticator();
            } else {
                _LOGGER
                    .error(
                        ExtMessages.AUTHENTICATOR_UNKNOWN,
                        authenticatorName);

                return false;
            }

            loginConfig.setRealmName(realmName.orElse(null));
            context.setLoginConfig(loginConfig);
            context.getPipeline().addValve(authenticator);
        }

        final StandardJarScanner jarScanner = (StandardJarScanner) context
            .getJarScanner();

        jarScanner.setScanClassPath(false);

        if (resource.isPresent()) {
            if (servlets.isEmpty()) {
                final Wrapper servlet = Tomcat
                    .addServlet(
                        context,
                        "default",
                        DefaultServlet.class.getName());

                servlet.setLoadOnStartup(1);
                servlet.setOverridable(true);
                context.addServletMapping("/", "default");
            }

            try {
                context.setDocBase(new URL(resource.get()).getPath());
            } catch (final MalformedURLException exception) {
                throw new InternalError(exception);    // Should not happen.
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUpListener(
            final String address,
            final int port,
            final int confidentialPort,
            final Optional<ServerSecurityContext> optionalSecurityContext)
    {
        final Connector connector = new Connector();

        if (optionalSecurityContext.isPresent()) {
            final ServerSecurityContext securityContext =
                optionalSecurityContext
                    .get();
            final AbstractHttp11JsseProtocol<?> protocol =
                (AbstractHttp11JsseProtocol<?>) connector
                    .getProtocolHandler();

            connector.setSecure(true);
            connector.setScheme("https");

            final KeyStoreConfig keyStoreConfig = securityContext
                .getKeyStoreConfig();

            protocol.setKeyAlias(keyStoreConfig.getKeyIdent().orElse(null));
            protocol.setKeyPass(_password(keyStoreConfig.getKeyPassword()));
            protocol.setKeystoreFile(keyStoreConfig.getPath().get());
            protocol.setKeystorePass(_password(keyStoreConfig.getPassword()));
            protocol
                .setKeystoreProvider(keyStoreConfig.getProvider().orElse(null));
            protocol.setKeystoreType(keyStoreConfig.getType().orElse(null));

            final TrustStoreConfig trustStoreConfig = securityContext
                .getTrustStoreConfig();

            protocol.setTruststoreFile(trustStoreConfig.getPath().get());
            protocol
                .setTruststorePass(_password(trustStoreConfig.getPassword()));
            protocol
                .setTruststoreProvider(
                    trustStoreConfig.getProvider().orElse(null));
            protocol.setTruststoreType(trustStoreConfig.getType().orElse(null));

            protocol
                .setClientAuth(
                    Boolean.valueOf(securityContext.isCertified()).toString());
            protocol.setSslProtocol("TLS");
            protocol.setSSLEnabled(true);
        } else {
            if (confidentialPort > 0) {
                connector.setRedirectPort(confidentialPort);
            }
        }

        connector.setPort(port);

        if (!address.isEmpty()) {
            connector.setAttribute("address", address);
        }

        _server._addConnector(connector);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUpRealm(
            final String name,
            final KeyedGroups realmProperties,
            final SecurityContext securityContext)
    {
        if (_realms.containsKey(name)) {
            _LOGGER.error(ExtMessages.DUPLICATE_REALM, name);

            return false;
        }

        final TomcatRealm realm = new TomcatRealm(name);

        if (!realm.setUp(realmProperties, securityContext)) {
            return false;
        }

        _LOGGER.debug(ExtMessages.REALM_CONFIG, name, realmProperties);

        _realms.put(name, realm);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUpServer(final HTTPServerAppImpl httpServerAppImpl)
    {
        final String workDir = new File(
            httpServerAppImpl.getDataDir(),
            _HTTP_DATA_DIR)
            .getAbsolutePath();

        ((StandardHost) _server.getHost()).setWorkDir(workDir);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void startServer()
        throws Exception
    {
        if (_started.compareAndSet(false, true)) {
            _server.start();
        } else {
            throw new InternalError();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stopServer()
        throws Exception
    {
        if (_started.compareAndSet(true, false)) {
            _server.stop();
        }
    }

    private static String _password(final Optional<char[]> password)
    {
        return password.isPresent()? String.valueOf(password.get()): null;
    }

    private static final String _HTTP_DATA_DIR = "http";
    private static final Logger _LOGGER = Logger
        .getInstance(TomcatHTTPSupport.class);
    private static final String _DEFAULT_DOC_BASE = new File("tmp")
        .getAbsolutePath();

    private final Map<String, TomcatRealm> _realms = new HashMap<>();
    private final _Server _server;
    private int _servletName;
    private final AtomicBoolean _started = new AtomicBoolean();

    public static final class ChildClassLoader
        extends URLClassLoader
    {
        /**
         * Constructs an instance.
         *
         * @param parent The parent class loader.
         */
        ChildClassLoader(final ClassLoader parent)
        {
            super(_EMPTY_URLS, parent);
        }

        public void setClearReferencesHttpClientKeepAliveThread(
                final boolean clearReferencesHttpClientKeepAliveThread) {}

        public void setClearReferencesStatic(
                final boolean clearReferencesStatic) {}

        public void setClearReferencesStopThreads(
                final boolean clearReferencesStopThreads) {}

        public void setClearReferencesStopTimerThreads(
                final boolean clearReferencesStopTimerThreads) {}

        private static final URL[] _EMPTY_URLS = new URL[0];
    }


    /**
     * Loader.
     */
    private static final class _Loader
        implements Loader
    {
        /**
         * Constructs an instance.
         */
        _Loader() {}

        /** {@inheritDoc}
         */
        @Override
        public void addPropertyChangeListener(
                final PropertyChangeListener listener) {}

        /** {@inheritDoc}
         */
        @Override
        public void backgroundProcess() {}

        /** {@inheritDoc}
         */
        @Override
        public ClassLoader getClassLoader()
        {
            return new ChildClassLoader(
                Thread.currentThread().getContextClassLoader());
        }

        /** {@inheritDoc}
         */
        @Override
        public Context getContext()
        {
            return _context;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean getDelegate()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean getReloadable()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean modified()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public void removePropertyChangeListener(
                final PropertyChangeListener listener) {}

        /** {@inheritDoc}
         */
        @Override
        public void setContext(final Context context)
        {
            _context = context;
        }

        /** {@inheritDoc}
         */
        @Override
        public void setDelegate(final boolean delegate) {}

        /** {@inheritDoc}
         */
        @Override
        public void setReloadable(final boolean reloadable) {}

        private Context _context;
    }


    /**
     * Tomcat server.
     */
    private static final class _Server
        extends Tomcat
    {
        /**
         * Constructs an instance.
         */
        _Server() {}

        /** {@inheritDoc}
         */
        @Override
        public Connector getConnector()
        {
            return null;
        }

        /**
         * Adds a connector.
         *
         * @param newConnector The new connector.
         */
        void _addConnector(final Connector newConnector)
        {
            getService().addConnector(newConnector);
        }

        /**
         * Gets the connectors.
         *
         * @return The connectors.
         */
        Connector[] _getConnectors()
        {
            return getService().findConnectors();
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

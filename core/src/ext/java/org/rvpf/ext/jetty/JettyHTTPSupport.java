/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JettyHTTPSupport.java 3986 2019-05-15 18:52:38Z SFB $
 */

package org.rvpf.ext.jetty;

import java.util.EventListener;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.ClientCertAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.Identity;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.security.ServerSecurityContext;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.ext.ExtMessages;
import org.rvpf.http.HTTPServerAppImpl;
import org.rvpf.http.HTTPSupport;

/**
 * HTTP Support using Jetty.
 */
public final class JettyHTTPSupport
    implements HTTPSupport
{
    /**
     * Constructs an instance.
     */
    public JettyHTTPSupport()
    {
        _server = new Server();
        _server.setHandler(new ContextHandlerCollection());
        _server.setThreadPool(new ExecutorThreadPool());
    }

    /** {@inheritDoc}
     */
    @Override
    public void destroyServer()
    {
        _server.destroy();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getListenerAddress(final int index)
    {
        return Optional.ofNullable(_server.getConnectors()[index].getHost());
    }

    /** {@inheritDoc}
     */
    @Override
    public int getListenerCount()
    {
        return _server.getConnectors().length;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getListenerPort(final int index)
    {
        return _server.getConnectors()[index].getLocalPort();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isServerStarted()
    {
        return _server.isStarted();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUpContext(
            final String path,
            final Optional<String> resource,
            final Map<String, String> servlets,
            final Optional<String> realmName,
            final Optional<String> optionalAuthenticator,
            final boolean confidential,
            final String[] roles,
            final Optional<EventListener> eventListener)
    {
        final String contextPath = "/" + path;

        _LOGGER.debug(ExtMessages.ADDING_CONTEXT, contextPath);

        final ServletContextHandler servletContext = new ServletContextHandler(
            (HandlerContainer) _server.getHandler(),
            contextPath,
            (confidential || (roles.length > 0))
            ? ServletContextHandler.SECURITY: 0);
        final ConstraintSecurityHandler securityHandler =
            (ConstraintSecurityHandler) servletContext
                .getSecurityHandler();

        if (securityHandler != null) {
            final JettyRealm realm;

            if (realmName.isPresent()) {
                realm = _realms.get(realmName.get());
            } else if (_realms.size() == 1) {
                realm = _realms.values().toArray(new JettyRealm[1])[0];
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

            final String authenticator = optionalAuthenticator.orElse(null);

            if (DIGEST_AUTHENTICATOR.equalsIgnoreCase(authenticator)) {
                securityHandler.setLoginService(realm);
                securityHandler.setAuthenticator(new DigestAuthenticator());
            } else if (CERTIFICATE_AUTHENTICATOR
                .equalsIgnoreCase(authenticator)) {
                final LoginService loginService;

                loginService = new _CertificateRealm(realm);
                securityHandler.setLoginService(loginService);
                securityHandler.setAuthenticator(new ClientCertAuthenticator());
            } else if (BASIC_AUTHENTICATOR.equalsIgnoreCase(authenticator)) {
                securityHandler.setLoginService(realm);
                securityHandler.setAuthenticator(new BasicAuthenticator());
            } else {
                _LOGGER.error(ExtMessages.AUTHENTICATOR_UNKNOWN, authenticator);

                return false;
            }

            final Constraint constraint = new Constraint();

            constraint.setAuthenticate(true);

            if (confidential) {
                _LOGGER.debug(ExtMessages.CONFIDENTIAL);
                constraint.setDataConstraint(Constraint.DC_CONFIDENTIAL);
            }

            if (roles.length > 0) {
                constraint.setRoles(roles);

                for (final String role: roles) {
                    _LOGGER.debug(ExtMessages.ROLE, role);
                }
            } else {
                constraint.setRoles(new String[] {Constraint.ANY_ROLE});
            }

            final ConstraintMapping constraintMapping = new ConstraintMapping();

            constraintMapping.setPathSpec("/");
            constraintMapping.setConstraint(constraint);
            securityHandler
                .setConstraintMappings(
                    new ConstraintMapping[] {constraintMapping});
        }

        for (final Map.Entry<String, String> entry: servlets.entrySet()) {
            servletContext.addServlet(entry.getValue(), entry.getKey());
        }

        if (resource.isPresent()) {
            servletContext.setResourceBase(resource.get());
            _LOGGER
                .info(
                    ExtMessages.CONTEXT_RESOURCE,
                    servletContext.getContextPath(),
                    servletContext.getResourceBase());
            servletContext.addServlet(DefaultServlet.class.getName(), "/");
        }

        if (eventListener.isPresent()) {
            servletContext.addEventListener(eventListener.get());
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
        final SocketConnector socketListener;

        if (optionalSecurityContext.isPresent()) {
            final ServerSecurityContext securityContext =
                optionalSecurityContext
                    .get();
            final SslContextFactory contextFactory = new SslContextFactory();
            final SslSocketConnector secureListener = new SslSocketConnector(
                contextFactory);
            final SSLContext sslContext;

            contextFactory
                .setCertAlias(
                    securityContext
                        .getKeyStoreConfig()
                        .getKeyIdent()
                        .orElse(null));

            try {
                sslContext = securityContext.createSSLContext();
            } catch (final SSLException exception) {
                throw new RuntimeException(exception);
            }

            contextFactory.setSslContext(sslContext);
            contextFactory.setNeedClientAuth(securityContext.isCertified());
            socketListener = secureListener;
        } else {
            socketListener = new SocketConnector();

            if (confidentialPort > 0) {
                socketListener.setConfidentialPort(confidentialPort);
            }
        }

        socketListener.setPort(port);

        socketListener.setHost(address.isEmpty()? null: address);
        _server.addConnector(socketListener);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUpRealm(
            final String realmName,
            final KeyedGroups realmProperties,
            final SecurityContext securityContext)
    {
        if (_realms.containsKey(realmName)) {
            _LOGGER.error(ExtMessages.DUPLICATE_REALM, realmName);

            return false;
        }

        final JettyRealm realm;

        realm = new JettyRealm(realmName);

        if (!realm.setUp(realmProperties, securityContext)) {
            return false;
        }

        _realms.put(realmName, realm);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUpServer(final HTTPServerAppImpl httpServerAppImpl)
    {
        final HandlerCollection handlerCollection = (HandlerCollection) _server
            .getHandler();

        handlerCollection.addHandler(new DefaultHandler());

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void startServer()
        throws Exception
    {
        _server.start();
    }

    /** {@inheritDoc}
     */
    @Override
    public void stopServer()
        throws Exception
    {
        _server.stop();
    }

    private static void _setSystemProperty(final String key, final String value)
    {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }

    private static final Logger _LOGGER = Logger
        .getInstance(JettyHTTPSupport.class);

    static {
        // Traps Jetty logging.
        _setSystemProperty(
            "org.eclipse.jetty.util.log.class",
            JettyLogger.class.getName());
    }

    private final Map<String, JettyRealm> _realms = new HashMap<>();
    private final Server _server;

    /**
     * Certificate realm.
     */
    private static final class _CertificateRealm
        implements LoginService
    {
        /**
         * Constructs an instance.
         *
         * @param realm The original realm.
         */
        _CertificateRealm(final JettyRealm realm)
        {
            _realm = realm;
        }

        /** {@inheritDoc}
         */
        @Override
        public IdentityService getIdentityService()
        {
            return _realm.getIdentityService();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getName()
        {
            return _realm.getName();
        }

        /** {@inheritDoc}
         */
        @Override
        public UserIdentity login(final String cn, final Object credentials)
        {
            final Matcher matcher = _CN_PATTERN.matcher(cn);

            if (!matcher.matches()) {
                return null;
            }

            final String identifier = matcher
                .group(1)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_');
            final Identity identity = _realm
                .getIdentity(Optional.of(identifier));

            return _realm.getUserIdentity(identity);
        }

        /** {@inheritDoc}
         */
        @Override
        public void logout(final UserIdentity userIdentity)
        {
            _realm.logout(userIdentity);
        }

        /** {@inheritDoc}
         */
        @Override
        public void setIdentityService(final IdentityService identityService)
        {
            _realm.setIdentityService(identityService);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean validate(final UserIdentity userIdentity)
        {
            return _realm.validate(userIdentity);
        }

        private static final Pattern _CN_PATTERN = Pattern
            .compile("CN=(.+?)(?:,.*)?", Pattern.CASE_INSENSITIVE);

        private final JettyRealm _realm;
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

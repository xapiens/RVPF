/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataCache.java 4055 2019-06-04 13:05:05Z SFB $
 */

package org.rvpf.document.loader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;

import java.nio.charset.StandardCharsets;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.security.Crypt;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.util.UnicodeStreamReader;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.config.Config;
import org.rvpf.http.HTTPMessages;
import org.rvpf.http.metadata.MetadataServerModule;
import org.rvpf.http.metadata.MetadataServlet;
import org.rvpf.service.ServiceMessages;

/**
 * Metadata cache.
 */
final class MetadataCache
{
    /**
     * Constructs an instance.
     */
    MetadataCache() {}

    /**
     * Gets the from string.
     *
     * @return The from string.
     */
    @CheckReturnValue
    String getFrom()
    {
        return _from;
    }

    /**
     * Gets the reader.
     *
     * @return The reader.
     */
    @CheckReturnValue
    Reader getReader()
    {
        return _reader;
    }

    /**
     * Refreshes.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean refresh()
    {
        final File requestFile = new File(_cacheDir, REQUEST_FILE_NAME);
        final File responseFile = new File(_cacheDir, RESPONSE_FILE_NAME);
        final String savedRequest;
        final String request;

        if (responseFile.exists() && requestFile.exists()) {
            savedRequest = _getSavedRequest(requestFile);
        } else {
            savedRequest = null;
        }

        if (savedRequest != null) {
            request = savedRequest;
        } else {
            request = _filter
                .getXML(Optional.ofNullable(_domain), Optional.empty());
            _deleteFile(requestFile);
            _deleteFile(responseFile);
        }

        _sendRequest(request, _serverURL, responseFile, requestFile);

        if (responseFile.exists()) {
            _from = responseFile.toURI().toASCIIString();

            if (_verify || _decrypt) {
                final Serializable serializable = _crypt
                    .load(
                        responseFile,
                        _verify,
                        _verifyKeyIdents,
                        _decrypt,
                        _decryptKeyIdents);

                if (serializable == null) {
                    return false;
                }

                _reader = new StringReader(serializable.toString());
            } else {
                try {
                    _reader = new UnicodeStreamReader(responseFile);
                } catch (final FileNotFoundException exception) {
                    throw new InternalError(exception);    // Should not happen.
                }
            }
        } else {
            _from = null;
        }

        return _from != null;
    }

    /**
     * Sets up this.
     *
     * @param config The configuration.
     * @param uuid The reference UUID.
     * @param filter A metadata filter.
     *
     * @return The new instance.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull final Config config,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final MetadataFilter filter)
    {
        final KeyedGroups configProperties = config.getProperties();
        final KeyedGroups metadataProperties = configProperties
            .getGroup(MetadataDocumentLoader.METADATA_PROPERTIES);

        _serverURL = _getServerURL(
            metadataProperties.getString(SERVER_PROPERTY).orElse(null));

        if (_serverURL == null) {
            return false;
        }

        if (!uuid.isPresent()) {
            _LOGGER.warn(ServiceMessages.METADATA_NEEDS_UUID);

            return false;
        }

        _uuid = uuid.get();
        _filter = filter;
        _domain = metadataProperties.getString(DOMAIN_PROPERTY).orElse(null);
        _cacheDir = config
            .getCacheDir(
                CACHE_SECTION_NAME,
                Optional.of(_uuid),
                _filter.getClientIdent());

        if (_cacheDir == null) {
            return false;
        }

        _verify = metadataProperties.getBoolean(Crypt.VERIFY_PROPERTY);

        if (_verify) {
            _LOGGER.debug(HTTPMessages.WILL_VERIFY);
            _verifyKeyIdents = metadataProperties
                .getStrings(Crypt.VERIFY_KEY_PROPERTY);

            for (final String keyIdent: _verifyKeyIdents) {
                _LOGGER.debug(HTTPMessages.VERIFICATION_KEY, keyIdent);
            }
        }

        _decrypt = metadataProperties.getBoolean(Crypt.DECRYPT_PROPERTY);

        if (_decrypt) {
            _LOGGER.debug(HTTPMessages.WILL_DECRYPT);
            _decryptKeyIdents = metadataProperties
                .getStrings(Crypt.DECRYPT_KEY_PROPERTY);

            for (final String keyIdent: _decryptKeyIdents) {
                _LOGGER.debug(HTTPMessages.DECRYPTION_KEY, keyIdent);
            }
        }

        if (_verify || _decrypt) {
            final SecurityContext securityContext = new SecurityContext(
                _LOGGER);
            final KeyedGroups securityProperties = metadataProperties
                .getGroup(SecurityContext.SECURITY_PROPERTIES);

            if (securityProperties.isMissing()) {
                _LOGGER
                    .warn(
                        ServiceMessages.MISSING_PROPERTIES,
                        SecurityContext.SECURITY_PROPERTIES);

                return false;
            }

            if (!securityContext.setUp(configProperties, securityProperties)) {
                return false;
            }

            _crypt = new Crypt();

            if (!_crypt
                .setUp(
                    securityContext.getCryptProperties(),
                    Optional.empty())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tears down what has been set up.
     */
    void tearDown()
    {
        _tearDown();
    }

    private static void _deleteFile(final File file)
    {
        if (file.exists()) {
            if (!file.delete()) {
                _LOGGER
                    .warn(
                        BaseMessages.FILE_DELETE_FAILED,
                        file.getAbsolutePath());
            }
        }
    }

    private static URL _getServerURL(final String serverAddress)
    {
        final URL serverURL;

        if (serverAddress != null) {
            final String trimmedAddress = serverAddress.trim();

            if (trimmedAddress.length() > 0) {
                try {
                    final String serverHost;
                    URI serverURI;

                    serverURI = new URI(
                        serverAddress.contains(
                            "/")? serverAddress: ("//" + serverAddress));

                    if (serverURI.getRawPath().length() <= 1) {
                        serverURI = serverURI.resolve(DEFAULT_SERVER_PATH);
                    }

                    serverHost = serverURI.getHost();

                    if (serverHost != null) {
                        serverURI = Inet.substituteURI(serverURI).orElse(null);

                        if (serverURI == null) {
                            throw new Exception(
                                Message.format(
                                    BaseMessages.UNKNOWN_HOST,
                                    serverHost));
                        }
                    }

                    serverURL = DEFAULT_SERVER_URI.resolve(serverURI).toURL();
                    _LOGGER.debug(ServiceMessages.METADATA_SERVER, serverURL);
                } catch (final Exception exception) {
                    _LOGGER
                        .warn(
                            ServiceMessages.BAD_ADDRESS_,
                            serverAddress,
                            exception.getMessage());

                    return null;
                }
            } else {
                serverURL = null;
            }
        } else {
            serverURL = null;
        }

        return serverURL;
    }

    private String _getSavedRequest(final File requestFile)
    {
        final String request;

        try {
            final BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(requestFile),
                    StandardCharsets.UTF_8));
            final StringBuilder stringBuilder = new StringBuilder();

            for (;;) {
                final String line = reader.readLine();

                if (line == null) {
                    break;
                }

                stringBuilder.append(line);
                stringBuilder.append('\n');
            }

            reader.close();

            request = stringBuilder.toString();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        final XMLDocument document = new XMLDocument();

        try {
            document.parse(new StringReader(request));
        } catch (final XMLDocument.ParseException exception) {
            throw new RuntimeException(exception.getCause());
        }

        final Optional<String> afterAttribute = document
            .getRootElement()
            .getAttributeValue(AFTER_ATTRIBUTE, Optional.empty());
        final DateTime after = afterAttribute
            .isPresent()? DateTime.now().valueOf(afterAttribute.get()): null;

        return request
            .equals(
                _filter
                    .getXML(
                            Optional.ofNullable(_domain),
                                    Optional.ofNullable(after)))? request: null;
    }

    private void _sendRequest(
            final String request,
            final URL serverURL,
            final File responseFile,
            final File requestFile)
    {
        final HttpURLConnection connection;
        final int responseCode;
        final String stamp;

        try {
            connection = (HttpURLConnection) serverURL.openConnection();
        } catch (final IOException exception) {
            _LOGGER
                .warn(
                    ServiceMessages.CONNECTION_OPEN_FAILED,
                    serverURL,
                    exception.getMessage());

            return;
        }

        connection.setDoOutput(true);
        connection.setUseCaches(false);

        try {
            connection.setRequestMethod("POST");
        } catch (final ProtocolException exception) {
            throw new RuntimeException(exception);    // Should not happen.
        }

        connection.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
        connection
            .setRequestProperty(
                "Content-Length",
                String.valueOf(request.length()));

        try {
            connection.connect();
        } catch (final IOException exception) {
            _LOGGER
                .warn(
                    ServiceMessages.CONNECTION_FAILED,
                    serverURL,
                    exception.getMessage());

            return;
        }

        try {
            final Writer writer = new OutputStreamWriter(
                connection.getOutputStream(),
                StandardCharsets.UTF_8);

            writer.write(request);
            writer.close();
        } catch (final IOException exception) {
            _LOGGER
                .warn(
                    ServiceMessages.UPLOAD_FAILED,
                    serverURL,
                    exception.getMessage());

            return;
        }

        try {
            responseCode = connection.getResponseCode();
        } catch (final IOException exception) {
            _LOGGER
                .warn(
                    ServiceMessages.RESPONSE_GET_FAILED,
                    serverURL,
                    exception.getMessage());

            return;
        }

        if ((responseCode == HttpURLConnection.HTTP_NO_CONTENT)
                && responseFile.exists()) {
            _LOGGER.info(ServiceMessages.METADATA_CACHE_OK);

            return;    // No changes.
        }

        if (responseCode == HttpURLConnection.HTTP_GONE) {
            _LOGGER.warn(ServiceMessages.NO_GOOD_METADATA);

            return;
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            try {
                _LOGGER
                    .warn(
                        ServiceMessages.UNEXPECTED_RESPONSE,
                        String.valueOf(responseCode),
                        URLDecoder
                            .decode(
                                    connection.getResponseMessage(),
                                            StandardCharsets.UTF_8.name()));

                return;
            } catch (final Exception exception) {
                throw new RuntimeException(exception);    // Should not happen.
            }
        }

        try {
            final BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    connection.getInputStream(),
                    StandardCharsets.UTF_8));
            final File temporaryFile = new File(
                responseFile.getParentFile(),
                TEMPORARY_FILE_NAME);
            final Writer writer = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(temporaryFile),
                    StandardCharsets.UTF_8));

            for (;;) {
                final String line = reader.readLine();

                if (line == null) {
                    break;
                }

                writer.write(line);
                writer.write('\n');
            }

            writer.close();
            reader.close();

            _deleteFile(responseFile);

            if (!temporaryFile.renameTo(responseFile)) {
                _LOGGER
                    .warn(
                        BaseMessages.FILE_RENAME_FAILED,
                        temporaryFile,
                        responseFile);

                return;
            }

            _LOGGER.info(ServiceMessages.METADATA_CACHE_REFRESHED);
        } catch (final IOException exception) {
            _LOGGER
                .warn(
                    ServiceMessages.RESPONSE_READ_FAILED,
                    serverURL,
                    exception.getMessage());

            return;
        }

        stamp = connection
            .getHeaderField(MetadataServlet.METADATA_STAMP_HEADER);

        if (stamp != null) {
            try {
                final Writer writer = new OutputStreamWriter(
                    new FileOutputStream(requestFile),
                    StandardCharsets.UTF_8);

                writer
                    .write(
                        _filter
                            .getXML(
                                    Optional.ofNullable(_domain),
                                            Optional
                                                    .of(
                                                            DateTime
                                                                    .now()
                                                                    .valueOf(stamp))));
                writer.close();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }
        } else {
            _LOGGER
                .warn(
                    ServiceMessages.MISSING_HEADER,
                    MetadataServlet.METADATA_STAMP_HEADER);
        }

        connection.disconnect();
    }

    private void _tearDown()
    {
        if (_crypt != null) {
            _crypt.tearDown();
        }
    }

    /** After attribute. */
    public static final String AFTER_ATTRIBUTE = MetadataFilter.AFTER_ATTRIBUTE;

    /** Cache section name. */
    public static final String CACHE_SECTION_NAME = "metadata";

    /** Default server path. */
    public static final String DEFAULT_SERVER_PATH = "/"
        + MetadataServerModule.DEFAULT_PATH + MetadataServerModule.GET_PATH;

    /** Default server URI. */
    public static final URI DEFAULT_SERVER_URI = URI
        .create("http://" + Inet.LOCAL_HOST + "/");

    /** The application domain to request from the Medatata server. */
    public static final String DOMAIN_PROPERTY = "metadata.domain";

    /** Request file name. */
    public static final String REQUEST_FILE_NAME = "next-request.xml";

    /** Response file name. */
    public static final String RESPONSE_FILE_NAME = "previous-response.xml";

    /** The URL to the Medatata server. */
    public static final String SERVER_PROPERTY = "server";

    /** Temporary file name. */
    public static final String TEMPORARY_FILE_NAME = "response.tmp";
    private static final Logger _LOGGER = Logger
        .getInstance(MetadataCache.class);

    private File _cacheDir;
    private Crypt _crypt;
    private boolean _decrypt;
    private String[] _decryptKeyIdents;
    private String _domain;
    private MetadataFilter _filter;
    private String _from;
    private Reader _reader;
    private URL _serverURL;
    private UUID _uuid;
    private boolean _verify;
    private String[] _verifyKeyIdents;
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataServerModule.java 4055 2019-06-04 13:05:05Z SFB $
 */

package org.rvpf.http.metadata;

import java.io.File;

import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletContext;

import org.rvpf.base.alert.Event;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.config.ConfigProperties;
import org.rvpf.document.loader.MetadataDocumentLoader;
import org.rvpf.document.version.VersionControl;
import org.rvpf.http.HTTPMessages;
import org.rvpf.http.HTTPModule;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.metadata.MetadataServiceImpl;

/**
 * Metadata server module.
 */
public final class MetadataServerModule
    extends HTTPModule.Abstract
{
    /**
     * Constructs an instance.
     */
    public MetadataServerModule()
    {
        _metadataHolder = new MetadataContext();
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void doEventActions(final Event event)
    {
        if (VersionControl.DOCUMENT_UPDATED_EVENT.equalsIgnoreCase(
                event.getName())
                || VersionControl.DOCUMENT_RESTORED_EVENT.equalsIgnoreCase(
                    event.getName())) {
            final Optional<String> info = event.getInfo();
            final String revision;

            if (info.isPresent()) {
                final String infoString = info.get().toString().trim();

                revision = (infoString
                    .length() > 0)? ValueConverter
                        .splitFields(infoString, 2)[0]: null;
            } else {
                revision = null;
            }

            _revision = revision;
            _reload = true;
            callbackForPendingActions();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void doPendingActions()
    {
        try {
            if (_reload) {
                _reloadMetadata(_revision);
                _refresh = false;
                _reload = false;
                _revision = null;
            } else if (_refresh) {
                _loadMetadata();
                _refresh = false;
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void doSignalActions(final Signal signal)
    {
        if (RELOAD_METADATA_SIGNAL.equalsIgnoreCase(signal.getName())) {
            _revision = null;
            _reload = true;
            callbackForPendingActions();
        } else if (MetadataServiceImpl.REFRESH_METADATA_SIGNAL
            .equalsIgnoreCase(signal.getName())) {
            if (!signal.getInfo().isPresent()) {
                _refresh = true;
                callbackForPendingActions();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public String getDefaultPath()
    {
        return DEFAULT_PATH;
    }

    /** {@inheritDoc}
     */
    @Override
    public void prepareServletContext(final ServletContext servletContext)
    {
        servletContext
            .setAttribute(METADATA_CONTEXT_ATTRIBUTE, _metadataHolder);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final KeyedGroups contextProperties)
    {
        if (!getConfig().getBooleanValue(CONFIG_PROCESSOR_PROPERTY)) {
            getThisLogger().error(HTTPMessages.PROCESSOR_JAR_REQUIRED);

            return false;
        }

        if (!getConfig().getBooleanValue(CONFIG_STORE_PROPERTY)) {
            getThisLogger().error(HTTPMessages.STORE_JAR_REQUIRED);

            return false;
        }

        final KeyedGroups metadataProperties = contextProperties
            .getGroup(METADATA_PROPERTIES);

        if (!_metadataHolder
            .setUp(getConfig().getProperties(), metadataProperties)) {
            return false;
        }

        final KeyedGroups versionProperties = getConfig()
            .getPropertiesGroup(VersionControl.DOCUMENT_VERSION_PROPERTIES);
        final Optional<String> workspace = versionProperties
            .isMissing()? Optional
                .empty(): versionProperties
                    .getString(VersionControl.WORKSPACE_PROPERTY);

        if (workspace.isPresent()) {
            final File workspaceDirectory = getConfig()
                .getFile(workspace.get());

            if (workspaceDirectory.isDirectory()) {
                String metadataPath = metadataProperties
                    .getString(METADATA_PATH_PROPERTY)
                    .orElse(null);

                if ((metadataPath == null)
                        || new File(metadataPath).isAbsolute()) {
                    metadataPath = MetadataDocumentLoader.DEFAULT_PATH;
                }

                _metadataFile = new File(workspaceDirectory, metadataPath);

                getThisLogger()
                    .info(
                        HTTPMessages.METADATA_FILE,
                        _metadataFile.getAbsolutePath());
            }
        }

        try {
            _loadMetadata();
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();

            return false;
        }

        _triggerRefresh = contextProperties
            .getBoolean(TRIGGER_REFRESH_PROPERTY);

        callbackForEventActions();
        callbackForSignalActions();

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _metadataHolder.tearDown();

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void addServlets(final Map<String, String> servlets)
    {
        servlets.put(GET_PATH, MetadataServlet.class.getName());
    }

    private boolean _loadMetadata()
        throws InterruptedException
    {
        getThisLogger().info(HTTPMessages.LOADING_METADATA);

        final ConfigProperties configProperties = getConfig()
            .getProperties()
            .copy();

        configProperties.setValue(Config.SUBSTITUTION_DEFERRED_PROPERTY, "1");
        configProperties.freeze();

        final String from;

        if (_metadataFile != null) {
            from = _metadataFile.toURI().toString();
        } else {
            final KeyedGroups metadataProperties = configProperties
                .getGroup(MetadataDocumentLoader.METADATA_PROPERTIES);

            from = metadataProperties
                .getString(
                    MetadataDocumentLoader.PATH_PROPERTY,
                    Optional.of(MetadataDocumentLoader.DEFAULT_PATH))
                .get();
        }

        final Config config = new Config("", configProperties);

        Metadata metadata = MetadataDocumentLoader
            .fetchMetadata(
                new MetadataServerFilter(),
                Optional.of(config),
                Optional.empty(),
                Optional.of(from));

        if (metadata != null) {
            if (!metadata.validatePointsRelationships()) {
                metadata = null;
            }
        }

        if (metadata != null) {
            metadata.cleanUp();
            _metadataHolder.setMetadata(metadata);
            getThisLogger()
                .info(HTTPMessages.GOOD_METADATA, metadata.getStamp().get());
        } else {
            getThisLogger().warn(HTTPMessages.BAD_METADATA);
        }

        return metadata != null;
    }

    private void _reloadMetadata(
            final String revision)
        throws InterruptedException
    {
        if (_loadMetadata()) {
            getService()
                .sendEvent(
                    VersionControl.GOOD_DOCUMENT_EVENT,
                    Optional.ofNullable(revision));

            if (_triggerRefresh) {
                getService()
                    .sendSignal(
                        MetadataServiceImpl.REFRESH_METADATA_SIGNAL,
                        Optional.of((revision != null)? revision: ""));
            }
        } else {
            getService()
                .sendEvent(
                    VersionControl.BAD_DOCUMENT_EVENT,
                    Optional.ofNullable(revision));
        }

        getService().restoreConfigState();
    }

    /** The shared processor configuration indicator property. */
    public static final String CONFIG_PROCESSOR_PROPERTY = "config.processor";

    /** The shared store configuration indicator property. */
    public static final String CONFIG_STORE_PROPERTY = "config.store";

    /** Default path. */
    public static final String DEFAULT_PATH = "metadata";

    /** Get path. */
    public static final String GET_PATH = "/get";

    /** Metadata context attribute. */
    public static final String METADATA_CONTEXT_ATTRIBUTE = "metadata.context";

    /**
     * The Metadata file path relative to the document version workspace
     * directory.
     */
    public static final String METADATA_PATH_PROPERTY = "metadata";

    /** Properties holding the metadata configuration. */
    public static final String METADATA_PROPERTIES = "metadata";

    /** Reload metadata signal. */
    public static final String RELOAD_METADATA_SIGNAL = "ReloadMetadata";

    /**
     * When true, a successful metadata reload by the Metadata Server module
     * will trigger a 'RefreshMetadata' signal.
     */
    public static final String TRIGGER_REFRESH_PROPERTY = "trigger.refresh";

    private File _metadataFile;
    private final MetadataContext _metadataHolder;
    private boolean _refresh;
    private boolean _reload;
    private String _revision;
    private volatile boolean _triggerRefresh;
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

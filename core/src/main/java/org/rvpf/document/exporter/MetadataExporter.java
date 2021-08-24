/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataExporter.java 3949 2019-05-03 15:35:40Z SFB $
 */

package org.rvpf.document.exporter;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.document.loader.MetadataDocumentLoader;
import org.rvpf.document.loader.MetadataElementLoader;
import org.rvpf.metadata.Metadata;

/**
 * Metadata exporter.
 */
public final class MetadataExporter
    extends ConfigExporter
{
    private MetadataExporter(
            final Metadata metadata,
            final Optional<Set<String>> usages,
            final Optional<Set<String>> langs,
            final boolean secure)
    {
        super(metadata, MetadataDocumentLoader.METADATA_ROOT, secure);

        getDocument().setDocTypeStrings(MetadataDocumentLoader.DOCTYPE_STRINGS);

        _metadata = metadata;
        _usages = usages;
        _langs = langs;
    }

    /**
     * Exports the metadata.
     *
     * @param metadata The metadata.
     * @param usages The optional attributes usages to include.
     * @param langs The optional languages to include for information texts.
     * @param secure True if the destination is secure.
     *
     * @return The metadata as an XML document.
     */
    @Nonnull
    @CheckReturnValue
    public static XMLDocument export(
            @Nonnull final Metadata metadata,
            @Nonnull final Optional<Set<String>> usages,
            @Nonnull final Optional<Set<String>> langs,
            final boolean secure)
    {
        final MetadataExporter exporter = new MetadataExporter(
            metadata,
            usages,
            langs,
            secure);

        exporter.addExporters();

        return exporter.export();
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getElementName()
    {
        return MetadataDocumentLoader.METADATA_ROOT;
    }

    /** {@inheritDoc}
     */
    @Override
    void addExporters()
    {
        super.addExporters();

        new ParamDefExporter(this);
        new PermissionsExporter(this);
        new OriginExporter(this);
        new StoreExporter(this);
        new ContentExporter(this);
        new SyncExporter(this);
        new BehaviorExporter(this);
        new EngineExporter(this);
        new TransformExporter(this);
        new PointExporter(this);
        new GroupExporter(this);
    }

    /** {@inheritDoc}
     */
    @Override
    XMLDocument export()
    {
        final XMLDocument document = super.export();
        final AttributesExporter attributesExporter = new AttributesExporter(
            this);

        attributesExporter.export(_metadata.getAttributes(), getRootElement());

        if (_metadata.getDomain().length() > 0) {
            document
                .getRootElement()
                .setAttribute(
                    MetadataElementLoader.DOMAIN_ATTRIBUTE,
                    Optional.of(_metadata.getDomain()));
        }

        return document;
    }

    /** {@inheritDoc}
     */
    @Override
    Optional<Set<String>> getLangs()
    {
        return _langs;
    }

    /** {@inheritDoc}
     */
    @Override
    Optional<Set<String>> getUsages()
    {
        return _usages;
    }

    /** {@inheritDoc}
     */
    @Override
    void registerReferences()
    {
        super.registerReferences();

        _registerReferences(_metadata.getOriginEntities());
        _registerReferences(_metadata.getStoreEntities());
        _registerReferences(_metadata.getContentEntities());
        _registerReferences(_metadata.getSyncEntities());
        _registerReferences(_metadata.getEngineEntities());
        _registerReferences(_metadata.getTransformEntities());
        _registerReferences(_metadata.getGroupEntities());
        _registerReferences(_metadata.getPointsCollection());
    }

    private void _registerReferences(
            final Collection<? extends Entity> entities)
    {
        for (final Entity entity: entities) {
            registerReference(Optional.of(entity));
        }
    }

    private final Optional<Set<String>> _langs;
    private final Metadata _metadata;
    private final Optional<Set<String>> _usages;
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

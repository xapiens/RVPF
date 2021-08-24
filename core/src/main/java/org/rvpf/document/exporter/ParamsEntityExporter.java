/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ParamsEntityExporter.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.document.exporter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.Params;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.document.loader.DocumentElement;
import org.rvpf.document.loader.MetadataElementLoader;
import org.rvpf.metadata.entity.ParamDefEntity;
import org.rvpf.metadata.entity.ParamsEntity;

/**
 * Params entity exporter.
 */
public abstract class ParamsEntityExporter
    extends MetadataEntityExporter
{
    /**
     * Creates an instance.
     *
     * @param owner The exporter owning this.
     * @param entityClass The class of the entities exported by this.
     */
    protected ParamsEntityExporter(
            @Nonnull final MetadataExporter owner,
            @Nonnull final Class<? extends Entity> entityClass)
    {
        super(owner, entityClass);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void export(final Entity entity, final XMLElement element)
    {
        super.export(entity, element);

        final ParamsEntity paramsEntity = (ParamsEntity) entity;

        export(paramsEntity.getParams(), element);
    }

    /**
     * Exports Params into the supplied Element.
     *
     * @param params The Params to export.
     * @param element The target Element.
     */
    @SuppressWarnings("unchecked")
    protected final void export(
            @Nonnull final Params params,
            @Nonnull final XMLElement element)
    {
        final boolean secure = getOwner().isSecure();

        for (final String paramName: params.getValuesKeys()) {
            final Object[] entryValues = params.getValues(paramName);
            final String defPrefix = ParamDefEntity.ENTITY_PREFIX + element.getName();
            final Optional<ParamDefEntity> paramDef =
                (Optional<ParamDefEntity>) getEntity(
                    paramName,
                    defPrefix);
            final boolean hidden = (paramDef.isPresent()? paramDef
                .get()
                .isHidden(): false)
                    || params.areValuesHidden(paramName);

            if (hidden && !secure && !areDeferredSubstitutions(entryValues)) {
                continue;
            }

            final XMLElement paramElement = createElement(
                MetadataElementLoader.PARAM_ELEMENT);

            if (paramDef.isPresent()) {
                paramElement
                    .setAttribute(
                        MetadataElementLoader.DEF_ATTRIBUTE,
                        reference(paramDef));
            } else {
                paramElement
                    .setAttribute(
                        DocumentElement.NAME_ATTRIBUTE,
                        Optional.of(paramName));
            }

            if (hidden) {
                paramElement
                    .setAttribute(MetadataElementLoader.HIDDEN_ATTRIBUTE, true);
            }

            if (entryValues.length > 1) {
                for (final Object entryValue: entryValues) {
                    final XMLElement valueElement = createElement(
                        MetadataElementLoader.VALUE_ELEMENT);

                    if (entryValue instanceof ClassDefEntity) {
                        valueElement
                            .setAttribute(
                                MetadataElementLoader.CLASS_DEF_REFERENCE,
                                reference(
                                    Optional.of((ClassDefEntity) entryValue)));
                    } else {
                        valueElement
                            .setAttribute(
                                MetadataElementLoader.VALUE_ATTRIBUTE,
                                Optional.of((String) entryValue));
                    }

                    paramElement.addChild(valueElement);
                }
            } else if (entryValues[0] instanceof ClassDefEntity) {
                paramElement
                    .setAttribute(
                        MetadataElementLoader.CLASS_DEF_REFERENCE,
                        reference(
                            Optional.of((ClassDefEntity) entryValues[0])));
            } else if (((String) entryValues[0]).length() > 0) {
                paramElement
                    .setAttribute(
                        MetadataElementLoader.VALUE_ATTRIBUTE,
                        Optional.of((String) entryValues[0]));
            }

            element.addChild(paramElement);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void registerReferences(final Entity entity)
    {
        final ParamsEntity paramsEntity = (ParamsEntity) entity;

        super.registerReferences(entity);

        registerReferences(paramsEntity.getParams(), getElementName());
    }

    /**
     * Registers parameter references to their definitions.
     *
     * @param params The parameters.
     * @param elementName The name of the element holding the parameters.
     */
    protected final void registerReferences(
            @Nonnull final Params params,
            @Nonnull final String elementName)
    {
        for (final Map.Entry<String, List<Object>> paramEntry:
                params.getValuesEntries()) {
            final String paramName = paramEntry.getKey();
            final String defPrefix = ParamDefEntity.ENTITY_PREFIX + elementName;

            registerReference(getEntity(paramName, defPrefix));

            for (final Object paramValue: paramEntry.getValue()) {
                if (paramValue instanceof ClassDefEntity) {
                    registerReference(Optional.of((ClassDefEntity) paramValue));
                }
            }
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

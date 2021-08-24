/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PropertiesDefLoader.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.config.entity.PropertiesDefEntity;
import org.rvpf.config.entity.PropertyDefEntity;
import org.rvpf.service.ServiceMessages;

/**
 * PropertiesDef loader.
 */
final class PropertiesDefLoader
    extends ConfigElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        final DocumentElement propertiesDefElement = getElement();
        final Optional<String> nameAttribute = propertiesDefElement
            .getNameAttribute();
        final PropertiesDefEntity oldPropertiesDefEntity = nameAttribute
            .isPresent()? (PropertiesDefEntity) getEntity(
                nameAttribute.get(),
                PropertiesDefEntity.ENTITY_PREFIX,
                true)
                .orElse(null): null;
        final PropertiesDefEntity newPropertiesDefEntity =
            _preparePropertiesDef(
                propertiesDefElement,
                oldPropertiesDefEntity);

        if (newPropertiesDefEntity != oldPropertiesDefEntity) {
            newPropertiesDefEntity.setUUID(propertiesDefElement.getUUID());
            putEntity(newPropertiesDefEntity);
        }
    }

    private PropertiesDefEntity _preparePropertiesDef(
            final DocumentElement propertiesDefElement,
            PropertiesDefEntity propertiesDefEntity)
        throws ValidationException
    {
        if (propertiesDefEntity == null) {
            propertiesDefEntity = new PropertiesDefEntity();
        }

        final Optional<String> extendsAttribute = propertiesDefElement
            .getAttributeValue(EXTENDS_ATTRIBUTE, Optional.empty());
        final PropertiesDefEntity extended = extendsAttribute
            .isPresent()? (PropertiesDefEntity) getEntity(
                extendsAttribute.get(),
                PropertiesDefEntity.ENTITY_PREFIX,
                true)
                .orElse(null): null;

        if (extended != null) {
            propertiesDefEntity.setHidden(extended.isHidden());
            propertiesDefEntity.setMultiple(extended.isMultiple());
            propertiesDefEntity.setValidated(extended.isValidated());

            for (final PropertyDefEntity extendedPropertyDef:
                    extended.getPropertyDefs()) {
                propertiesDefEntity.addPropertyDef(extendedPropertyDef);
            }

            for (final PropertiesDefEntity extendedPropertiesDef:
                    extended.getPropertiesDefs()) {
                propertiesDefEntity.addPropertiesDef(extendedPropertiesDef);
            }
        }

        propertiesDefEntity.setName(propertiesDefElement.getNameAttribute());
        propertiesDefEntity
            .setHidden(
                propertiesDefElement
                    .getAttributeValue(
                            HIDDEN_ATTRIBUTE,
                                    propertiesDefEntity.isHidden()));
        propertiesDefEntity
            .setMultiple(
                propertiesDefElement
                    .getAttributeValue(
                            MULTIPLE_ATTRIBUTE,
                                    propertiesDefEntity.isMultiple()));
        propertiesDefEntity
            .setValidated(
                propertiesDefElement
                    .getAttributeValue(
                            VALIDATED_ATTRIBUTE,
                                    propertiesDefEntity.isValidated()));

        for (final DocumentElement child: propertiesDefElement.getChildren()) {
            if (child.isEnabled()
                    && (!child.getAttributeValue(
                        DocumentElement.LANG_ATTRIBUTE,
                        Optional.empty()).isPresent())) {
                if (PROPERTY_DEF_ENTITY.equals(child.getName())) {
                    if (!propertiesDefEntity
                        .addPropertyDef(
                            PropertyDefLoader.preparePropertyDef(child))) {
                        throw new ValidationException(
                            ServiceMessages.PROPERTY_DEF_MULTIPLE,
                            child.getNameAttribute());
                    }
                } else if (PROPERTIES_DEF_ENTITY.equals(child.getName())) {
                    final String childNameAttribute = child
                        .getNameAttribute()
                        .orElse(null);
                    final PropertiesDefEntity oldPropertiesDef =
                        (childNameAttribute != null)? propertiesDefEntity
                            .getPropertiesDef(childNameAttribute)
                            .orElse(null): null;
                    final PropertiesDefEntity newPropertiesDef =
                        _preparePropertiesDef(
                            child,
                            oldPropertiesDef);

                    if (newPropertiesDef != oldPropertiesDef) {
                        propertiesDefEntity.addPropertiesDef(newPropertiesDef);
                    }
                }
            }
        }

        return propertiesDefEntity;
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

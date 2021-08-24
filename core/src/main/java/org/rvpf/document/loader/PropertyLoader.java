/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PropertyLoader.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.config.ConfigProperties;
import org.rvpf.config.entity.PropertiesDefEntity;
import org.rvpf.config.entity.PropertyDefEntity;
import org.rvpf.service.ServiceMessages;

/**
 * Property loader.
 */
final class PropertyLoader
    extends ConfigElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        final DocumentElement propertyElement = getElement();

        if (propertyElement.getAttributeValue(SYSTEM_ATTRIBUTE, false)) {
            final Optional<String> name = propertyElement.getNameAttribute();

            if (!name.isPresent()) {
                throw new ValidationException(
                    ServiceMessages.PROPERTY_NAME_MISSING);
            }

            putValues(
                name.get(),
                propertyElement,
                getConfig().getProperties(),
                Optional.empty(),
                DocumentSystemProperties.getInstance());
        } else {
            addPropertyValues(
                this,
                propertyElement,
                getConfig().getProperties(),
                Optional.empty(),
                ((ConfigDocumentLoader) getDocumentLoader())
                    .isValidationEnabled());
        }
    }

    /**
     * Adds the Property values specified in the Element.
     *
     * @param loader The current Entry.
     * @param element An XML Element.
     * @param context Where to get and put values.
     * @param propertiesDef The optional container.
     * @param validate Specifies if the property should be validated.
     *
     * @throws ValidationException When appropriate.
     */
    static void addPropertyValues(
            @Nonnull final ConfigElementLoader loader,
            @Nonnull final DocumentElement element,
            @Nonnull final ConfigProperties context,
            @Nonnull final Optional<PropertiesDefEntity> propertiesDef,
            final boolean validate)
        throws ValidationException
    {
        final String name = element.getNameAttribute().orElse(null);

        if (name == null) {
            throw new ValidationException(
                ServiceMessages.PROPERTY_NAME_MISSING);
        }

        loader.putValues(name, element, context, Optional.empty(), context);

        if (validate && element.getAttributeValue(VALIDATED_ATTRIBUTE, true)) {
            final PropertyDefEntity propertyDef;
            String def = element
                .getAttributeValue(DEF_ATTRIBUTE, Optional.empty())
                .orElse(null);

            if (def == null) {
                def = name;
            }

            if (propertiesDef.isPresent()) {
                propertyDef = propertiesDef
                    .get()
                    .getPropertyDef(def)
                    .orElse(null);
            } else {
                propertyDef = (PropertyDefEntity) loader
                    .getDocumentLoader()
                    .getEntity(def, PropertyDefEntity.ENTITY_PREFIX)
                    .orElse(null);
            }

            if (propertyDef == null) {
                if (propertiesDef.isPresent()) {
                    loader
                        .getLogger()
                        .warn(
                            ServiceMessages.PROPERTY_DEF_MISSING_IN,
                            name,
                            propertiesDef.get().getName().orElse(null));
                } else {
                    loader
                        .getLogger()
                        .warn(ServiceMessages.PROPERTY_DEF_MISSING, name);
                }
            } else if (context.isMultiple(name)) {
                if (!propertyDef.isMultiple()) {
                    if (propertiesDef.isPresent()) {
                        loader
                            .getLogger()
                            .warn(
                                ServiceMessages.PROPERTY_MULTIPLE_IN,
                                name,
                                propertiesDef.get().getName().orElse(null));
                    } else {
                        loader
                            .getLogger()
                            .warn(ServiceMessages.PROPERTY_MULTIPLE, name);
                    }
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

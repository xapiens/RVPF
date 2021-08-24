/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClassLibLoader.java 3956 2019-05-06 11:17:05Z SFB $
 */

package org.rvpf.document.loader;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.config.UndefinedEntityException;
import org.rvpf.config.entity.ClassLibEntity;
import org.rvpf.service.ServiceMessages;

/**
 * ClassLib loader.
 */
final class ClassLibLoader
    extends ConfigElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        final DocumentElement classLibElement = getElement();
        final ClassLibEntity.Builder classLibBuilder = ClassLibEntity
            .newBuilder();

        classLibBuilder
            .setUUID(classLibElement.getUUID().orElse(null))
            .setName(classLibElement.getNameAttribute().orElse(null));

        _addLocation(
            classLibElement
                .getAttributeValue(LOCATION_ATTRIBUTE, Optional.of(""))
                .get(),
            classLibBuilder);

        for (final DocumentElement locationElement:
                classLibElement.getChildren(LOCATION_ELEMENT)) {
            if (locationElement.isEnabled()) {
                _addLocation(
                    locationElement
                        .getAttributeValue(LOCATION_ATTRIBUTE, Optional.of(""))
                        .get(),
                    classLibBuilder);
                _addLocation(locationElement.getText(), classLibBuilder);
            }
        }

        if (classLibElement
            .getAttributeValue(CACHED_ATTRIBUTE, Optional.empty())
            .isPresent()) {
            final boolean cached = classLibElement
                .getAttributeValue(CACHED_ATTRIBUTE, false);

            classLibBuilder.setCached(Optional.of(Boolean.valueOf(cached)));

            if (classLibBuilder.isCached(false)
                    && !classLibElement.getUUID().isPresent()) {
                getLogger()
                    .warn(
                        ServiceMessages.CLASS_LIB_ATTRIBUTES,
                        CACHED_ATTRIBUTE,
                        DocumentElement.UUID_ATTRIBUTE);
                classLibBuilder.setCached(Optional.empty());
            }
        }

        for (final DocumentElement referenceElement:
                classLibElement.getChildren(CLASS_LIB_REFERENCE)) {
            if (referenceElement.isEnabled()) {
                classLibBuilder
                    .addClassLib(
                        getClassLibEntity(
                            referenceElement
                                    .getAttributeValue(CLASS_LIB_REFERENCE)));
            }
        }

        final ClassLibEntity classLib = classLibBuilder.build();

        if (!classLib.getUUID().isPresent()
                && !classLib.getName().isPresent()
                && !classLibElement.getId().isPresent()) {
            try {
                getConfig().getClassLoader().addFromClassLib(classLib);
            } catch (final UndefinedEntityException exception) {
                throw new ValidationException(
                    BaseMessages.VERBATIM,
                    exception.getMessage());
            }
        } else {
            getConfig().addClassLibEntity(classLib);
            putEntity(classLib);
        }
    }

    private void _addLocation(
            String location,
            final ClassLibEntity.Builder classLibBuilder)
    {
        location = location.trim();
        location = location.replace("\\\\", "/");
        location = location.replace("\\", "/");
        location = location.replace(":////", ":///");
        location = location.replace("file:///..", "..");

        if (location.length() > 0) {
            final URI uri;

            try {
                uri = new URI(location);
            } catch (final URISyntaxException exception) {
                getLogger().warn(ServiceMessages.BAD_LOCATION, location);

                return;
            }

            classLibBuilder.addLocation(uri);
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

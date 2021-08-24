/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PermissionsLoader.java 4103 2019-07-01 13:31:25Z SFB $
 */

package org.rvpf.document.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

import org.rvpf.base.UUID;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.metadata.Permissions;
import org.rvpf.metadata.entity.PermissionsEntity;
import org.rvpf.service.ServiceMessages;

/**
 * Permissions loader.
 */
final class PermissionsLoader
    extends MetadataElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        final DocumentElement permissionsElement = getElement();
        final Optional<String> name = permissionsElement.getNameAttribute();
        final Optional<UUID> uuid = permissionsElement.getUUID();
        PermissionsEntity permissions = name
            .isPresent()? (PermissionsEntity) getEntity(
                name.get(),
                PermissionsEntity.ENTITY_PREFIX,
                true)
                .orElse(null): null;

        if ((permissions == null) && uuid.isPresent()) {
            permissions = (PermissionsEntity) getEntity(
                uuid.get().toRawString(),
                PermissionsEntity.ENTITY_PREFIX,
                true)
                .orElse(null);
        }

        if (permissions != null) {    // Keeps the original definition.
            putEntity(permissionsElement.getId().get(), permissions);

            return;
        }

        final PermissionsEntity.Builder permissionsBuilder = PermissionsEntity
            .newBuilder();

        permissionsBuilder.setUUID(uuid.orElse(null)).setName(name);
        permissions = permissionsBuilder.build();

        final Optional<String> extendsAttribute = permissionsElement
            .getAttributeValue(EXTENDS_ATTRIBUTE, Optional.empty());
        final PermissionsEntity parent = extendsAttribute
            .isPresent()? (PermissionsEntity) getEntity(
                extendsAttribute.get(),
                PermissionsEntity.ENTITY_PREFIX,
                true)
                .orElse(null): null;

        if (parent != null) {
            parent.adopt(permissions);
        }

        for (final DocumentElement permissionElement:
                permissionsElement.getChildren(PERMISSIONS_ELEMENT)) {
            if (!permissionElement.isEnabled()) {
                continue;
            }

            final String role = permissionElement
                .getAttributeValue(ROLE_ATTRIBUTE, Optional.of(""))
                .get()
                .trim();
            final String allow = permissionElement
                .getAttributeValue(ALLOW_ATTRIBUTE, Optional.of(""))
                .get()
                .trim();
            final String deny = permissionElement
                .getAttributeValue(DENY_ATTRIBUTE, Optional.of(""))
                .get()
                .trim();

            for (final Permissions.Action action: _splitActions(allow)) {
                permissions.allow(role, action);
            }

            for (final Permissions.Action action: _splitActions(deny)) {
                permissions.deny(role, action);
            }
        }

        putEntity(permissions);
    }

    private static Collection<Permissions.Action> _splitActions(
            final String values)
        throws ValidationException
    {
        final String[] fields = ValueConverter.splitFields(values);
        final Collection<Permissions.Action> actions =
            new ArrayList<Permissions.Action>(
                fields.length);

        for (final String name: fields) {
            if (name.length() > 0) {
                try {
                    actions
                        .add(
                            Enum
                                .valueOf(
                                        Permissions.Action.class,
                                                name.toUpperCase(Locale.ROOT)));
                } catch (final IllegalArgumentException exception) {
                    throw new ValidationException(
                        ServiceMessages.PERMISSION_ACTION_UNKNOWN,
                        name);
                }
            }
        }

        return actions;
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

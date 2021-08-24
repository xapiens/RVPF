/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PermissionsExporter.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.document.exporter;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.ConfigElementLoader;
import org.rvpf.document.loader.MetadataElementLoader;
import org.rvpf.metadata.Permissions;
import org.rvpf.metadata.entity.PermissionsEntity;

/**
 * Permissions exporter.
 */
final class PermissionsExporter
    extends DefEntityExporter
{
    /**
     * Constructs an instance.
     *
     * @param owner The exporter owning this.
     */
    PermissionsExporter(@Nonnull final ConfigExporter owner)
    {
        super(owner, PermissionsEntity.class);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void export(final Entity entity, final XMLElement element)
    {
        super.export(entity, element);

        for (final Map.Entry<String, Set<Permissions.Action>> permissionEntry:
                ((PermissionsEntity) entity).getPermissions().entrySet()) {
            final StringBuilder actionsStringBuilder = new StringBuilder();

            for (final Permissions.Action action: permissionEntry.getValue()) {
                if (actionsStringBuilder.length() > 0) {
                    actionsStringBuilder.append(',');
                }

                actionsStringBuilder.append(action.name());
            }

            if (actionsStringBuilder.length() > 0) {
                final XMLElement permissionsElement = createElement(
                    MetadataElementLoader.PERMISSIONS_ELEMENT);
                final String role = permissionEntry.getKey();

                if (role.length() > 0) {
                    permissionsElement
                        .setAttribute(
                            ConfigElementLoader.ROLE_ATTRIBUTE,
                            Optional.of(role));
                }

                permissionsElement
                    .setAttribute(
                        ConfigElementLoader.ALLOW_ATTRIBUTE,
                        Optional.of(actionsStringBuilder.toString()));

                element.addChild(permissionsElement);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getElementName()
    {
        return MetadataElementLoader.PERMISSIONS_ENTITY;
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceLoader.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.service.ServiceContext;
import org.rvpf.service.ServiceMessages;

/**
 * Service loader.
 */
final class ServiceLoader
    extends ConfigElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        final DocumentElement serviceElement = getElement();       
        if (!(getDocumentLoader().getClass() == ConfigDocumentLoader.class)) {
            throw new ValidationException(
                ServiceMessages.CONFIG_ELEMENT,
                serviceElement.getName());
        }

        final ServiceContext serviceContext = new ServiceContext();

        serviceContext
            .setServiceName(
                serviceElement
                    .getAttributeValue(DocumentElement.NAME_ATTRIBUTE));

        for (final DocumentElement aliasElement:
                serviceElement.getChildren(ALIAS_ELEMENT)) {
            if (aliasElement.isEnabled()) {
                serviceContext
                    .addServiceAlias(
                        aliasElement
                            .getAttributeValue(DocumentElement.NAME_ATTRIBUTE));
            }
        }

        if (serviceContext
            .getServiceName()
            .equals(getConfig().getServiceName())) {
            PropertiesLoader
                .prepareGroup(
                    serviceContext,
                    this,
                    serviceElement,
                    getConfig().getProperties(),
                    Optional.empty(),
                    ((ConfigDocumentLoader) getDocumentLoader())
                        .isValidationEnabled());

            for (final DocumentElement classLibElement:
                    serviceElement.getChildren(CLASS_LIB_REFERENCE)) {
                if (classLibElement.isEnabled()) {
                    serviceContext
                        .addClassLib(
                            getClassLibEntity(
                                classLibElement
                                        .getAttributeValue(
                                                CLASS_LIB_REFERENCE)));
                }
            }
        }

        getConfig().addServiceContext(serviceContext);
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

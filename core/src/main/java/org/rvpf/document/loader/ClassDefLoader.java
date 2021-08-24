/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClassDefLoader.java 3956 2019-05-06 11:17:05Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.config.entity.ClassLibEntity;

/**
 * ClassDef loader.
 */
final class ClassDefLoader
    extends ConfigElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        final DocumentElement classDefElement = getElement();
        final ClassDefEntity.Builder classDefBuilder = ClassDefEntity
            .newBuilder();
        final Optional<String> name = classDefElement.getNameAttribute();
        final String packageName = classDefElement
            .getAttributeValue(PACKAGE_ATTRIBUTE, Optional.of("java.lang"))
            .orElse(null);
        final String member = classDefElement
            .getAttributeValue(
                MEMBER_ATTRIBUTE,
                name.isPresent()? name: Optional.of("Object"))
            .orElse(null);

        classDefBuilder.setName(name.get());
        classDefBuilder.setImpl(new ClassDefImpl(packageName, member));

        final Optional<String> referenceAttribute = classDefElement
            .getAttributeValue(CLASS_LIB_REFERENCE, Optional.empty());
        final ClassLibEntity classLib = referenceAttribute
            .isPresent()? getClassLibEntity(referenceAttribute.get()): null;

        if (classLib != null) {
            classDefBuilder.setClassLib(classLib);
        }

        for (final DocumentElement implementsElement:
                classDefElement.getChildren(IMPLEMENTS_ELEMENT)) {
            if (implementsElement.isEnabled()) {
                classDefBuilder
                    .addImplemented(
                        getClassDefEntity(
                            implementsElement
                                    .getAttributeValue(CLASS_DEF_REFERENCE)));
            }
        }

        final ClassDefEntity classDef = classDefBuilder.build();

        getConfig().addClassDefEntity(classDef);
        putEntity(
            classDef,
            classDefElement.getAttributeValue(SUPERSEDE_ATTRIBUTE, false));
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3ObjectsValidation.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.pap.dnp3;

import java.util.Optional;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.dnp3.object.GroupCategory;
import org.rvpf.pap.dnp3.object.ObjectGroup;
import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectVariation;
import org.rvpf.tests.Tests;

import org.testng.annotations.Test;

/**
 * DNP3 objects validation.
 */
public final class DNP3ObjectsValidation
    extends Tests
{
    /**
     * Validates group codes.
     */
    @Test
    public static void validateGroups()
    {
        for (final GroupCategory objectCategory: GroupCategory.values()) {
            for (final ObjectGroup objectGroup:
                    objectCategory.getObjectGroups()) {
                Require
                    .success(
                        objectGroup.getCode()
                        >= objectCategory.getFromGroupCode());
                Require
                    .success(
                        objectGroup.getCode()
                        <= objectCategory.getToGroupCode());
                Require.same(objectGroup.getCategory(), objectCategory);
            }
        }
    }

    /**
     * Validates variation codes.
     */
    @Test
    public static void validateVariations()
    {
        for (final GroupCategory objectCategory: GroupCategory.values()) {
            for (final ObjectGroup objectGroup:
                    objectCategory.getObjectGroups()) {
                for (final ObjectVariation objectVariation:
                        GroupCategory
                            .objectVariations(
                                    Optional.ofNullable(objectGroup))) {
                    Require.same(objectVariation.getObjectGroup(), objectGroup);

                    final ObjectInstance objectInstance = GroupCategory
                        .newObjectInstance(objectVariation);
                    final String objectString = "Object '" + objectInstance
                        + "'";

                    Require
                        .same(
                            objectInstance.getObjectVariation(),
                            objectVariation,
                            objectString);

                    Require
                        .equal(
                            objectInstance.getClass().getSimpleName(),
                            "G" + objectInstance.getGroupCode() + "V"
                            + objectInstance.getVariationCode(),
                            objectString);
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

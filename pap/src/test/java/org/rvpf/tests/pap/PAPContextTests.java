/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPContextTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.pap;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.Entity;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.pap.PAPContext;
import org.rvpf.pap.PAPMessages;
import org.rvpf.tests.Tests;

import org.testng.annotations.Test;

/**
 * PAP context tests.
 */
public abstract class PAPContextTests
    extends Tests
{
    /**
     * Constructs an instance.
     *
     * @param usage The attributes usage.
     */
    protected PAPContextTests(@Nonnull final String usage)
    {
        _attributesUsage = usage;
    }

    /**
     * Should require a known origin for a point.
     */
    @Test(priority = 120)
    public void shouldRequireKnownOriginForPoint()
    {
        final Point point;
        final boolean pointAccepted;

        // Given a point with an origin unknown to the context,
        point = newPoint(Optional.of(newOriginEntity()));

        // when adding the point,
        expectLogs(PAPMessages.UNKNOWN_ORIGIN);
        pointAccepted = getServerContext()
            .addRemotePoint(point, getAttributes(point).orElse(null));

        // then it should be rejected.
        Require.failure(pointAccepted, "point accepted");
        requireLogs(PAPMessages.UNKNOWN_ORIGIN);
    }

    /**
     * Should require an origin for a point.
     */
    @Test(priority = 110)
    public void shouldRequireOriginForPoint()
    {
        final Point point;
        final boolean pointAccepted;

        // Given a point without origin,
        point = newPoint(Optional.empty());

        // when adding the point,
        expectLogs(PAPMessages.MISSING_ORIGIN);
        pointAccepted = getServerContext()
            .addRemotePoint(point, getAttributes(point).orElse(null));

        // then it should be rejected.
        Require.failure(pointAccepted, "point accepted");
        requireLogs(PAPMessages.MISSING_ORIGIN);
    }

    /**
     * Gets the usage attributes for an entity.
     *
     * @param entity The entity.
     *
     * @return The optional attributes.
     */
    @Nonnull
    @CheckReturnValue
    protected Optional<Attributes> getAttributes(final Entity entity)
    {
        return entity.getAttributes(_attributesUsage);
    }

    /**
     * Gets the context.
     *
     * @return The context.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract PAPContext getServerContext();

    /**
     * Returns a new origin.
     *
     * @return The new origin.
     */
    @Nonnull
    @CheckReturnValue
    protected OriginEntity newOriginEntity()
    {
        final OriginEntity.Builder originBuilder = OriginEntity.newBuilder();
        final KeyedGroups attributes = new KeyedGroups();

        attributes.setValue(_attributesUsage, new Attributes(_attributesUsage));

        originBuilder.setName("CONTEXT_TESTS_" + ++_originIndex);
        originBuilder.setAttributes(Optional.of(attributes));

        return originBuilder.build();
    }

    /**
     * Returns a new point for an origin.
     *
     * @param origin The optional origin.
     *
     * @return The point.
     */
    @Nonnull
    @CheckReturnValue
    protected Point newPoint(@Nonnull final Optional<OriginEntity> origin)
    {
        final PointEntity point = new PointEntity.Definition();
        final KeyedGroups attributes = new KeyedGroups();

        point.setName(Optional.of("CONTEXT_TESTS_" + ++_pointIndex));
        point.setOriginEntity(origin);
        attributes.setValue(_attributesUsage, new Attributes(_attributesUsage));
        point.setAttributes(Optional.of(attributes));

        return point;
    }

    private final String _attributesUsage;
    private int _originIndex;
    private int _pointIndex;
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CIPContext.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.pap.cip;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Traces;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAPContext;

/**
 * CIP context.
 */
public abstract class CIPContext
    extends PAPContext
{
    /**
     * Constructs an instance.
     *
     * @param metadata The optional metadata.
     * @param traces The optional traces.
     */
    CIPContext(
            @Nonnull final Optional<Metadata> metadata,
            @Nonnull final Optional<Traces> traces)
    {
        super(new CIPSupport(), metadata, traces);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean addRemoteOrigin(
            final Origin remoteOrigin,
            final Attributes originAttributes)
    {
        final CIPProxy remoteProxy = newRemoteProxy(remoteOrigin);

        if (!remoteProxy.setUp(originAttributes)) {
            return false;
        }

        registerRemoteProxy(remoteProxy);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean addRemotePoint(
            final Point remotePoint,
            final Attributes pointAttributes)
    {
        final CIPProxy remoteProxy = (CIPProxy) getRemoteProxy(remotePoint)
            .orElse(null);

        if (remoteProxy == null) {
            return false;
        }

        registerRemotePoint(remotePoint);

        return true;
    }

    /**
     * Gets the tag for a point.
     *
     * <p>Looks first for the {@value CIP#TAG_ATTRIBUTE} in {@value CIP#ATTRIBUTES_USAGE}
     * attributes, then for the {@value Point#TAG_PARAM} param, and finally gets
     * the point name.</p>
     *
     * @param point The point.
     *
     * @return The tag.
     */
    @Nonnull
    @CheckReturnValue
    public String getTag(@Nonnull final Point point)
    {
        final Optional<Attributes> pointAttributes = point
            .getAttributes(CIP.ATTRIBUTES_USAGE);
        String tag = (pointAttributes
            .isPresent())? pointAttributes
                .get()
                .getString(CIP.TAG_ATTRIBUTE)
                .orElse(null): null;

        if (tag == null) {
            tag = point
                .getParams()
                .getString(Point.TAG_PARAM, point.getName())
                .get();
        }

        return tag;
    }

    /** {@inheritDoc}
     */
    @Override
    protected abstract CIPProxy newRemoteProxy(Origin remoteOrigin);
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

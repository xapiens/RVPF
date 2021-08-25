/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPUpdatesListener.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.store.server.pap;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.PAPProxy;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.UpdatesListener;

/**
 * PAP updates listener.
 */
public abstract class PAPUpdatesListener
    extends UpdatesListener.Abstract
    implements PAPProxy.Responder
{
    /** {@inheritDoc}
     */
    @Override
    public PointValue[] select(
            final Point[] points)
        throws InterruptedException, ServiceNotAvailableException
    {
        final StoreValuesQuery[] storeQueries =
            new StoreValuesQuery[points.length];
        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder();

        for (int i = 0; i < points.length; ++i) {
            storeQueries[i] = storeQueryBuilder.setPoint(points[i]).build();
        }

        final StoreValues[] response = getServer()
            .select(storeQueries, Optional.empty());
        final PointValue[] pointValues = new PointValue[response.length];

        Require.success(response.length == storeQueries.length);

        for (int i = 0; i < response.length; ++i) {
            pointValues[i] = response[i].getPointValue().orElse(null);
        }

        return pointValues;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final StoreServiceAppImpl storeAppImpl,
            final KeyedGroups listenerProperties)
    {
        if (!super.setUp(storeAppImpl, listenerProperties)) {
            return false;
        }

        if (!_traces
            .setUp(
                storeAppImpl.getDataDir(),
                storeAppImpl
                    .getConfigProperties()
                    .getGroup(Traces.TRACES_PROPERTIES),
                storeAppImpl.getSourceUUID(),
                listenerProperties.getString(TRACES_PROPERTY))) {
            return false;
        }

        return true;
    }

    /**
     * Gets the traces.
     *
     * @return The traces.
     */
    @Nonnull
    @CheckReturnValue
    protected Traces getTraces()
    {
        return _traces;
    }

    /** Traces subdirectory property. */
    public static final String TRACES_PROPERTY = "traces";

    private final Traces _traces = new Traces();
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

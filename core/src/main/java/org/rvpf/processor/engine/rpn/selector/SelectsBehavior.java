/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.processor.engine.rpn.selector;

import javax.annotation.CheckReturnValue;

import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.processor.behavior.PrimaryBehavior;

/**
 * Selects behavior.
 */
public abstract class SelectsBehavior
    extends PrimaryBehavior
{
    /**
     * Constructs an instance.
     */
    protected SelectsBehavior() {}

    /** {@inheritDoc}
     */
    @Override
    public boolean isInputRequired()
    {
        return true;
    }

    /**
     * Asks if the interval is start/stop.
     *
     * @return A true value if it is start/stop.
     */
    @CheckReturnValue
    public boolean isStartStop()
    {
        return _startStop;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        if (getRelation().isPresent()) {
            _startStop = StartStopContent.class
                .isInstance(getInputPoint().getContent().orElse(null));
        }

        return true;
    }

    private boolean _startStop;
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

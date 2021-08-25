/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.processor.engine.pap.captor;

import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.engine.rpn.operation.Operations;
import org.rvpf.processor.engine.rpn.selector.SelectedBehavior;
import org.rvpf.processor.engine.rpn.selector.SelectorEngine;
import org.rvpf.processor.engine.rpn.selector.SelectsBehavior;

/**
 * Captor engine.
 *
 * <p>This engine is used to capture ephemeral datalogger data.</p>
 *
 * <p>It is assumed that inputs happen in chronological order.</p>
 */
public final class CaptorEngine
    extends SelectorEngine
{
    /** {@inheritDoc}
     */
    @Override
    protected Class<? extends SelectedBehavior> getSelectedBehaviorClass()
    {
        return CapturedBehavior.class;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Class<? extends SelectsBehavior> getSelectsBehaviorClass()
    {
        return CapturesBehavior.class;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Operations newOperations()
    {
        return new CaptorOperations();
    }

    /** {@inheritDoc}
     */
    @Override
    protected Transform newTransform()
    {
        return new CaptorTransform(this);
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RecalcTriggerBehavior.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.behavior;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.RecalcTrigger;
import org.rvpf.base.value.ResultValue;

/**
 * Recalc trigger behavior.
 */
public class RecalcTriggerBehavior
    extends AbstractBehavior
{
    /**
     * Constructs an instance.
     *
     * @param recalcTrigger The associated recalc trigger.
     */
    public RecalcTriggerBehavior(final RecalcTrigger recalcTrigger)
    {
        _recalcTrigger = recalcTrigger;
    }

    /** {@inheritDoc}
     */
    @Override
    public Point getResultPoint()
    {
        return _recalcTrigger.getPoint().get();
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultValue newResultValue(final Optional<DateTime> stamp)
    {
        Require.success(_recalcTrigger.getStamp().equals(stamp.orElse(null)));

        return super.newResultValue(stamp);
    }

    private final RecalcTrigger _recalcTrigger;
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

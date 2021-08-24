/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JiTArchiver.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.server.archiver;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotAvailableException;

/**
 * Just in time archiver.
 */
public class JiTArchiver
    extends Archiver.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public void archive(final Point point)
        throws ServiceNotAvailableException
    {
        final Optional<PointReference> pointReference = getPointReference(
            point);

        if (pointReference.isPresent()) {
            archive(pointReference.get(), DateTime.now());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void commit()
    {
        commitAttic();
    }

    /** {@inheritDoc}
     */
    @Override
    public void start() {}

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        updateStats();
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

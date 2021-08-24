/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AbstractContent.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.content;

import java.io.Serializable;
import javax.annotation.Nonnull;
import org.rvpf.base.BaseMessages;
import org.rvpf.base.Content;
import org.rvpf.base.Point;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Proxied;

/** Content converter.
 *
 * <p>Base class for content converters. It supplies minimal behavior.</p>
 */
public abstract class AbstractContent
    extends Proxied.Abstract
    implements Content
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable decode(final PointValue pointValue)
    {
        return pointValue.getValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable denormalize(final NormalizedValue normalizedValue)
    {
        return normalizedValue.getValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable encode(final PointValue pointValue)
    {
        return decode(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public Content getInstance(final Point point)
    {
        return this;
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable normalize(final PointValue pointValue)
    {
        return pointValue.getValue();
    }

    /** Warns about the bad format of a value.
     *
     * @param pointValue The associated point value.
     */
    protected final void warnBadValue(@Nonnull final PointValue pointValue)
    {
        getThisLogger().warn(BaseMessages.BAD_VALUE_NULLED, pointValue);
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

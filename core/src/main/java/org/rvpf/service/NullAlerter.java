/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: NullAlerter.java 3967 2019-05-08 20:37:41Z SFB $
 */

package org.rvpf.service;

import java.util.Optional;

import org.rvpf.base.alert.Alert;

/**
 * Null alerter.
 */
public final class NullAlerter
    extends Alerter.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public boolean isEmbedded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isRunning()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isStealth()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doSend(final Alert alert)
    {
        notifyListeners(Optional.of(alert));
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean doSetUp()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doTearDown() {}

    /** {@inheritDoc}
     */
    @Override
    protected SharedContext sharedContext()
    {
        return _SHARED_CONTEXT;
    }

    private static final SharedContext _SHARED_CONTEXT = new SharedContext();
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

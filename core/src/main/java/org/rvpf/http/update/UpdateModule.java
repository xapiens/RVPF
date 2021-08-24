/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UpdateModule.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http.update;

import java.util.Map;

import javax.servlet.ServletContext;

import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.http.HTTPModule;

/**
 * Update module.
 */
public final class UpdateModule
    extends HTTPModule.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public String getDefaultPath()
    {
        return DEFAULT_PATH;
    }

    /** {@inheritDoc}
     */
    @Override
    public void prepareServletContext(final ServletContext servletContext)
    {
        servletContext.setAttribute(UPDATE_CONTEXT_ATTRIBUTE, _updateContextr);
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_updateContextr != null) {
            _updateContextr.tearDown();
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void addServlets(final Map<String, String> servlets)
    {
        servlets.put(ACCEPT_PATH, UpdateServlet.class.getName());
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups contextProperties)
    {
        _updateContextr = new UpdateContext();

        return _updateContextr.setUp(getConfig(), contextProperties);
    }

    /** Accept path. */
    public static final String ACCEPT_PATH = "/accept";

    /** Default path. */
    public static final String DEFAULT_PATH = "update";

    /** Update context attribute. */
    public static final String UPDATE_CONTEXT_ATTRIBUTE = "update.context";

    private UpdateContext _updateContextr;
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

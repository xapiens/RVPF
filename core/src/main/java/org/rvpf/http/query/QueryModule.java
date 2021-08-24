/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QueryModule.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.http.query;

import java.util.Map;
import javax.servlet.ServletContext;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.http.HTTPModule;

/** Query module.
 */
public final class QueryModule
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
        servletContext.setAttribute(QUERY_REQUESTER_ATTRIBUTE, _requester);
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_requester != null) {
            _requester.tearDown();
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void addServlets(final Map<String, String> servlets)
    {
        servlets.put(INFO_PATH, InfoServlet.class.getName());
        servlets.put(VALUES_PATH, ValuesServlet.class.getName());
        servlets.put(VALUES_PATH + COUNT_PATH, ValuesServlet.class.getName());
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups contextProperties)
    {
        _requester = new QueryContext();

        return _requester.setUp(getConfig(), contextProperties);
    }

    /** Count path. */
    public static final String COUNT_PATH = "/count";

    /** Default path. */
    public static final String DEFAULT_PATH = "query";

    /** Info path. */
    public static final String INFO_PATH = "/info";

    /** Values path. */
    public static final String VALUES_PATH = "/values";

    /** Query requester attribute. */
    public static final String QUERY_REQUESTER_ATTRIBUTE = "query.requester";

    private QueryContext _requester;
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

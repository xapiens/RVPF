/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: NoticeModule.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http.notice;

import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletContext;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.http.HTTPModule;

/**
 * Notice module.
 */
public final class NoticeModule
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
    public boolean needsMetadata()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void prepareServletContext(final ServletContext servletContext)
    {
        servletContext.setAttribute(NOTICE_CONTEXT_ATTRIBUTE, _context);
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_context != null) {
            _context.tearDown();
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void addServlets(final Map<String, String> servlets)
    {
        servlets.put(ACCEPT_PATH, NoticeServlet.class.getName());
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups contextProperties)
    {
        final class _MetadataFilter
            extends MetadataFilter
        {
            /**
             * Constructs an instance.
             */
            public _MetadataFilter()
            {
                super(false);
            }

            /** {@inheritDoc}
             */
            @Override
            public boolean areContentsNeeded()
            {
                return true;
            }

            /** {@inheritDoc}
             */
            @Override
            public boolean areEntitiesKept()
            {
                return true;
            }

            /** {@inheritDoc}
             */
            @Override
            public Optional<String> getClientIdent()
            {
                return Optional.of(NOTICE_SENDER_IDENT);
            }
        }

        if (!loadMetadata(new _MetadataFilter())) {
            return false;
        }

        final ClassDef classDef = contextProperties
            .getClassDef(NOTIFIER_CLASS_ATTRIBUTE, DEFAULT_NOTIFIER_CLASS);

        _context = classDef.createInstance(NoticeContext.class);

        if ((_context == null)
                || !_context.setUp(getMetadata(), contextProperties)) {
            return false;
        }

        getMetadata().cleanUp();

        return true;
    }

    /** Accept path. */
    public static final String ACCEPT_PATH = "/accept";

    /** Default notifier. */
    public static final ClassDef DEFAULT_NOTIFIER_CLASS = new ClassDefImpl(
        SOMNotifier.class);

    /** Default path. */
    public static final String DEFAULT_PATH = "notice";

    /** Notice context attribute. */
    public static final String NOTICE_CONTEXT_ATTRIBUTE = "notice.context";

    /** The notifier Class. */
    public static final String NOTIFIER_CLASS_ATTRIBUTE = "notifier.class";
    static final String NOTICE_SENDER_IDENT = "NoticeSender";

    private NoticeContext _context;
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

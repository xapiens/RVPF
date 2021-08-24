/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ContainerContent.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.content;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.Content;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;

/**
 * Container content converter.
 */
public abstract class ContainerContent
    extends AbstractContent
{
    /**
     * Gets the first level content.
     *
     * @return The first level content.
     */
    public final Content getContent()
    {
        return _content;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean setUp(
            final Metadata metadata,
            final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        if (_content == null) {
            _content = getParams()
                .getClassDef(CONTENT_PARAM, _DEFAULT_CONTENT)
                .createInstance(Content.class);

            if (_content == null) {
                return false;
            }

            if (_content instanceof AbstractContent) {
                if (!((AbstractContent) _content)
                    .setUp(metadata, proxyEntity)) {
                    return false;
                }
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public final void tearDown()
    {
        if (_content instanceof AbstractContent) {
            ((AbstractContent) _content).tearDown();
        }

        _content = null;

        super.tearDown();
    }

    /** Content param. */
    public static final String CONTENT_PARAM = "Content";

    /**  */

    private static final ClassDef _DEFAULT_CONTENT = new ClassDefImpl(
        UnspecifiedContent.class);

    private Content _content;
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

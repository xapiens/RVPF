/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DocumentSystemProperties.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.document.loader;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.util.container.KeyedValues;

/**
 * System Properties.
 *
 * <p>Wraps system properties as {@link KeyedValues}.</p>
 */
@NotThreadSafe
final class DocumentSystemProperties
    extends KeyedValues
{
    /**
     * Constructs an instance.
     */
    private DocumentSystemProperties()
    {
        super(BaseMessages.SYSTEM_TYPE.toString());
    }

    /** {@inheritDoc}
     */
    @Override
    public void add(@Nonnull final String key, @Nonnull final Object value)
    {
        checkNotFrozen();

        System.setProperty(key, value.toString());
    }

    /** {@inheritDoc}
     */
    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean containsValueKey(final String key)
    {
        return System.getProperties().containsKey(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public DocumentSystemProperties copy()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public DocumentSystemProperties freeze()
    {
        super.freeze();

        return this;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isMultiple(final String key)
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public void setValue(final String key, final Object value)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setValuesHidden(final String key)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance.
     */
    @Nonnull
    @CheckReturnValue
    static DocumentSystemProperties getInstance()
    {
        return _INSTANCE;
    }

    @SuppressWarnings("static-method")
    private Object readResolve()
    {
        return getInstance();
    }

    private static final DocumentSystemProperties _INSTANCE =
        new DocumentSystemProperties();
    private static final long serialVersionUID = 1L;
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

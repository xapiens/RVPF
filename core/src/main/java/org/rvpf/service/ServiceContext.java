/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceContext.java 4103 2019-07-01 13:31:25Z SFB $
 */

package org.rvpf.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.tool.Require;
import org.rvpf.config.ConfigProperties;
import org.rvpf.config.entity.ClassLibEntity;

/**
 * Service Context.
 */
@NotThreadSafe
public final class ServiceContext
    extends ConfigProperties
{
    /**
     * Constructs a ServiceContext.
     */
    public ServiceContext()
    {
        super(ServiceMessages.SERVICE_TYPE);
    }

    /** {@inheritDoc}
     */
    @Override
    public void add(@Nonnull final String key, @Nonnull final Object value)
    {
        super.add(key, value);
    }

    /**
     * Adds a class library.
     *
     * @param classLib The class library.
     */
    public void addClassLib(@Nonnull final ClassLibEntity classLib)
    {
        checkNotFrozen();

        _classLibs.add(classLib);
    }

    /**
     * Adds a service alias.
     *
     * @param alias The service alias.
     */
    public void addServiceAlias(@Nonnull final String alias)
    {
        checkNotFrozen();

        if (_serviceAliases == null) {
            _serviceAliases = new HashSet<>();
        }

        _serviceAliases.add(alias);
    }

    /** {@inheritDoc}
     */
    @Override
    public ServiceContext copy()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public ServiceContext freeze()
    {
        super.freeze();

        return this;
    }

    /**
     * Gets the class libraries.
     *
     * @return The class libraries.
     */
    @Nonnull
    @CheckReturnValue
    public List<ClassLibEntity> getClassLibs()
    {
        return _classLibs;
    }

    /**
     * Gets the service aliases.
     *
     * @return Returns the service aliases.
     */
    @Nonnull
    @CheckReturnValue
    public Set<String> getServiceAliases()
    {
        return (_serviceAliases != null)? _serviceAliases: Collections
            .<String>emptySet();
    }

    /**
     * Gets the service name.
     *
     * @return Returns the service name.
     */
    @Nonnull
    @CheckReturnValue
    public String getServiceName()
    {
        return Require.notNull(_serviceName);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setOverriden(final ConfigProperties overriden)
    {
        super.setOverriden(overriden);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setOverrider(final ConfigProperties overrider)
    {
        super.setOverrider(overrider);
    }

    /**
     * Sets the service name.
     *
     * @param serviceName The service name.
     */
    public void setServiceName(@Nonnull final String serviceName)
    {
        _serviceName = Require.notNull(serviceName);
    }

    /** {@inheritDoc}
     */
    @Override
    @Nonnull
    public void setValue(@Nonnull final String key, final Object value)
    {
        super.setValue(key, value);
    }

    private static final long serialVersionUID = 1L;

    private final List<ClassLibEntity> _classLibs = new LinkedList<>();
    private Set<String> _serviceAliases;
    private String _serviceName;
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

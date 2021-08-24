/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CryptFilter.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.forwarder.filter;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.security.Crypt;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.forwarder.ForwarderModule;

/**
 * Crypt filter.
 */
public abstract class CryptFilter
    extends ForwarderFilter.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final ForwarderModule forwarderModule,
            final KeyedGroups filterProperties)
    {
        if (!super.setUp(forwarderModule, filterProperties)) {
            return false;
        }

        final Config config = getModule().getConfig();
        final SecurityContext securityContext = new SecurityContext(
            getThisLogger());
        final KeyedGroups securityProperties = filterProperties
            .getGroup(SecurityContext.SECURITY_PROPERTIES);

        if (!securityContext
            .setUp(config.getProperties(), securityProperties)) {
            return false;
        }

        if (!_crypt
            .setUp(securityContext.getCryptProperties(), Optional.empty())) {
            return false;
        }

        _strict = filterProperties.getBoolean(STRICT_PROPERTY);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _crypt.tearDown();

        super.tearDown();
    }

    /**
     * Handles a crypt exception.
     *
     * @param cryptResult The crypt result.
     */
    protected void cryptException(@Nonnull final Crypt.Result cryptResult)
    {
        final Exception exception = cryptResult.getException();

        if (exception.getCause() != null) {
            throw new RuntimeException(exception.getCause());
        }

        getThisLogger().warn(BaseMessages.VERBATIM, exception.getMessage());
    }

    /**
     * Gets the crypt instance.
     *
     * @return The crypt instance.
     */
    @CheckReturnValue
    protected Crypt getCrypt()
    {
        return _crypt;
    }

    /**
     * Asks if in strict mode.
     *
     * @return True if in strict mode.
     */
    @CheckReturnValue
    protected boolean isStrict()
    {
        return _strict;
    }

    /** Strict property. */
    public static final String STRICT_PROPERTY = "strict";

    private final Crypt _crypt = new Crypt();
    private boolean _strict;
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

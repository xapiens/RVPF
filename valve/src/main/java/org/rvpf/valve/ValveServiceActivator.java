/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValveServiceActivator.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.valve;

import java.net.SocketAddress;

import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.util.Version;
import org.rvpf.service.ServiceActivatorBase;
import org.rvpf.service.ServiceMessages;

/**
 * Valve service activator.
 *
 * <p>This service intercepts connections to the TCP/IP port of an other
 * application to make it available only while a specified control port is
 * active.</p>
 *
 * <p>This may be used to disallow access to an historian service until a
 * notifier (Store) service has established event listening with that historian.
 * </p>
 *
 * @see ValveServiceImpl
 */
public final class ValveServiceActivator
    extends ServiceActivatorBase
    implements ValveServiceActivatorMBean
{
    /**
     * Allows operation in stand alone mode.
     *
     * <p>As a program, it expects one optional argument: the environment
     * properties file specification (defaults to "rvpf-valve.properties").</p>
     *
     * @param args The program arguments.
     */
    public static void main(@Nonnull final String[] args)
    {
        new ValveServiceActivator().run(args);
    }

    /** {@inheritDoc}
     */
    @Override
    public void create()
        throws Exception
    {
        super.create();

        created();
    }

    /** {@inheritDoc}
     */
    @Override
    public void destroy()
    {
        super.destroy();

        destroyed();
    }

    /**
     * Gets the controlled addresses.
     *
     * <p>For framework tests.</p>
     *
     * @return The controlled addresses.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<SocketAddress> getControlledAddresses()
    {
        return _valveServiceImpl.getControlledAddresses();
    }

    /**
     * Gets the direct addresses.
     *
     * <p>For framework tests.</p>
     *
     * @return The direct addresses.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<SocketAddress> getDirectAddresses()
    {
        return _valveServiceImpl.getDirectAddresses();
    }

    /**
     * Gets the environment properties.
     *
     * <p>For framework tests.</p>
     *
     * @return The environment properties.
     */
    @Nonnull
    @CheckReturnValue
    public Properties getEnv()
    {
        return _valveServiceImpl.getEnv();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getEnvPath()
    {
        String envPath = _envPath;

        if (envPath == null) {
            envPath = ValveServiceImpl.envPath();
        }

        return envPath;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<ValveStats> getStats()
    {
        return Optional.ofNullable(_valveServiceImpl.getStats());
    }

    /** {@inheritDoc}
     */
    @Override
    public Version getVersion()
    {
        return new ValveVersion();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isPaused()
    {
        return _valveServiceImpl.isPaused();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isStarted()
    {
        final ValveServiceImpl valveServiceImpl = _valveServiceImpl;

        return (valveServiceImpl != null) && valveServiceImpl.isOpened();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isStopped()
    {
        final ValveServiceImpl valveServiceImpl = _valveServiceImpl;

        return (valveServiceImpl == null) || valveServiceImpl.isClosed();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setEnvPath(final String envPath)
    {
        _envPath = envPath;

        if (_valveServiceImpl != null) {
            _valveServiceImpl.setEnvPath(getEnvPath());
        }

        getThisLogger().debug(ValveMessages.PROPERTIES_FILE_PATH, _envPath);
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
        throws Exception
    {
        super.start();

        if (isStarted()) {
            return;
        }

        _valveServiceImpl = new ValveServiceImpl(this);
        _valveServiceImpl.setEnvPath(getEnvPath());
        _valveServiceImpl.putProperties(getProperties());
        _valveServiceImpl.setServiceName(getObjectName().toString());

        if (!_valveServiceImpl.open()) {
            _valveServiceImpl.close();
            _valveServiceImpl = null;
        }

        getThisLogger().info(ServiceMessages.SERVICE_STARTED, getObjectName());

        started();
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        super.stop();

        if (isStopped()) {
            return;
        }

        _valveServiceImpl.close();
        _valveServiceImpl = null;

        getThisLogger().info(ServiceMessages.SERVICE_STOPPED, getObjectName());

        stopped();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateStats()
    {
        _valveServiceImpl.updateStats();
    }

    /**
     * Waits for control.
     *
     * <p>For unit tests.</p>
     *
     * @param control The value to wait for.
     *
     * @throws InterruptedException When interrupted.
     */
    public void waitForControl(
            final boolean control)
        throws InterruptedException
    {
        _valveServiceImpl.waitForControl(control);
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean acceptMainArg(final String arg)
    {
        if (super.acceptMainArg(arg)) {
            return true;
        }

        if (_envPath == null) {
            setEnvPath(arg);

            return true;
        }

        return false;
    }

    private String _envPath;
    private ValveServiceImpl _valveServiceImpl;
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

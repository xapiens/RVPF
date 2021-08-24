/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ForwarderServiceAppImpl.java 4042 2019-06-02 13:28:46Z SFB $
 */

package org.rvpf.forwarder;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Message;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.forwarder.input.InputModule;
import org.rvpf.forwarder.output.OutputModule;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.app.ServiceAppImpl;
import org.rvpf.service.metadata.MetadataService;

/**
 * Forwarder service application implementation.
 */
public final class ForwarderServiceAppImpl
    extends ServiceAppImpl
{
    /**
     * Gets the outputs size.
     *
     * @return The outputs size.
     */
    public int getOutputsSize()
    {
        return _outputs.size();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onSignal(final Signal signal)
    {
        if (MetadataService.REFRESH_METADATA_SIGNAL
            .equalsIgnoreCase(signal.getName())) {
            if (_needsMetadata) {
                synchronized (_outputs) {
                    for (final ForwarderModule module: _outputs) {
                        if (!module.onMetadataRefreshed()) {
                            return true;
                        }
                    }
                }

                synchronized (_inputs) {
                    for (final ForwarderModule module: _inputs) {
                        if (!module.onMetadataRefreshed()) {
                            return true;
                        }
                    }
                }
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * Output messages.
     *
     * @param messages The messages.
     *
     * @return True on success.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    @CheckReturnValue
    public boolean output(
            @Nonnull final Serializable[] messages)
        throws InterruptedException, ServiceNotAvailableException
    {
        final int batchLimit = _batchLimit;

        synchronized (_outputs) {
            for (int start = 0; start < messages.length; start += batchLimit) {
                final int size = Math.min(messages.length - start, batchLimit);
                final Serializable[] batch = new Serializable[size];

                System.arraycopy(messages, start, batch, 0, size);

                for (final OutputModule outputModule: _outputs) {
                    if (!outputModule.output(batch)) {
                        return false;
                    }
                }

                for (final OutputModule outputModule: _outputs) {
                    if (!outputModule.commit()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Service service)
    {
        if (!super.setUp(service)) {
            return false;
        }

        final Config config = service.getConfig();
        final KeyedGroups[] modulesGroup = config
            .getPropertiesGroups(MODULE_PROPERTIES);

        if (modulesGroup.length == 0) {
            getThisLogger().warn(ForwarderMessages.NO_MODULES);
        }

        _batchLimit = Integer.MAX_VALUE;
        service.saveConfigState();

        boolean outputIsReliable = true;

        for (final KeyedGroups moduleProperties: modulesGroup) {
            final Optional<ClassDef> classDef = moduleProperties
                .getClassDef(MODULE_CLASS_PROPERTY, Optional.empty());

            if (!classDef.isPresent()) {
                getThisLogger()
                    .error(
                        ForwarderMessages.MODULE_PROPERTY_MISSING,
                        MODULE_CLASS_PROPERTY);

                return false;
            }

            final ForwarderModule module = classDef
                .get()
                .createInstance(ForwarderModule.class);

            if (module == null) {
                return false;
            }

            service.starting();    // Extends the start up time.

            if (!module.setUp(this, moduleProperties)) {
                module.tearDown();

                return false;
            }

            service.restoreConfigState();

            _needsMetadata |= module.needsMetadata();

            if (module instanceof OutputModule) {
                outputIsReliable &= module.isReliable();
                _batchLimit = Math
                    .min(_batchLimit, module.getBatchControl().getLimit());
                _outputs.add((OutputModule) module);
            } else {
                _inputs.add((InputModule) module);
            }

            if (getThisLogger().isDebugEnabled()) {
                getThisLogger()
                    .debug(
                        () -> new Message(
                            ServiceMessages.MODULE_LOADED,
                            module.getClass().getName()));
            }
        }

        if (!outputIsReliable && (getOutputsSize() > 1)) {
            getThisLogger().error(ForwarderMessages.OUTPUT_UNRELIABLE_SIBLING);

            return false;
        }

        if (_batchLimit < Integer.MAX_VALUE) {
            getThisLogger()
                .info(
                    ForwarderMessages.OUTPUT_BATCH_LIMIT,
                    String.valueOf(_batchLimit));
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        try {
            synchronized (_outputs) {
                for (final ForwarderModule module: _outputs) {
                    module.start();
                }
            }

            synchronized (_inputs) {
                for (final ForwarderModule module: _inputs) {
                    module.start();
                }
            }
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        synchronized (_inputs) {
            for (final ForwarderModule module: _inputs) {
                module.stop();
            }
        }

        synchronized (_outputs) {
            for (final ForwarderModule module: _outputs) {
                module.stop();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        synchronized (_inputs) {
            for (final ForwarderModule module: _inputs) {
                module.tearDown();
            }

            _inputs.clear();
        }

        synchronized (_outputs) {
            for (final ForwarderModule module: _outputs) {
                module.tearDown();
            }

            _outputs.clear();
        }

        super.tearDown();
    }

    /** The class of the module to activate. */
    public static final String MODULE_CLASS_PROPERTY = "module.class";

    /** Properties used to define a forwarder module. */
    public static final String MODULE_PROPERTIES = "forwarder.module";

    private int _batchLimit;
    private final List<InputModule> _inputs = new LinkedList<>();
    private boolean _needsMetadata;
    private final List<OutputModule> _outputs = new LinkedList<>();
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

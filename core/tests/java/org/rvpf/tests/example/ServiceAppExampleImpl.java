/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceAppExampleImpl.java 4095 2019-06-24 17:44:43Z SFB $
 */

/**
 * Related Values Processing Framework.
 *
 * $Id: ServiceAppExampleImpl.java 4095 2019-06-24 17:44:43Z SFB $
 */
package org.rvpf.tests.example;

import org.rvpf.base.alert.Alert;
import org.rvpf.base.alert.Event;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.logger.StringLogger;
import org.rvpf.base.tool.Require;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.metadata.MetadataService;
import org.rvpf.service.metadata.app.MetadataServiceAppImpl;

/**
 * Service application example implementation.
 */
public final class ServiceAppExampleImpl
    extends MetadataServiceAppImpl
{
    /** {@inheritDoc}
     */
    @Override
    public boolean onAlert(final Alert alert)
    {
        _LOGGER.debug("On alert: {0}", alert);

        return super.onAlert(alert);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onEvent(final Event event)
    {
        _LOGGER.debug("On event: {0}", event);

        return super.onEvent(event);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewMetadata(final Metadata metadata)
    {
        _LOGGER.debug("New metadata");

        return super.onNewMetadata(metadata);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onServicesNotReady()
    {
        _LOGGER.debug("Services not ready");

        super.onServicesNotReady();
    }

    /** {@inheritDoc}
     */
    @Override
    public void onServicesReady()
    {
        _LOGGER.debug("Services ready");

        super.onServicesReady();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onSignal(final Signal signal)
    {
        _LOGGER.debug("On signal: {0}", signal);

        return super.onSignal(signal);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final MetadataService service)
    {
        if (!super.setUp(service)) {
            _LOGGER.debug("Set up metadata service failed");

            return false;
        }

        Require.failure(getProperties().isMissing());
        Require
            .equal(
                getProperties().getString(_TEST_PROPERTY_NAME).get(),
                _TEST_PROPERTY_VALUE);

        _LOGGER.debug("Set up metadata service succeeded");

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        _LOGGER.debug("Starting");

        super.start();
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        _LOGGER.debug("Stopping");

        super.stop();
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _LOGGER.debug("Tear down");

        super.tearDown();
    }

    private static final String _TEST_PROPERTY_NAME = "tests.app.specific";
    private static final String _TEST_PROPERTY_VALUE = "Test";
    private static final StringLogger _LOGGER = StringLogger
        .getInstance(ServiceAppExampleImpl.class);
}

//End.




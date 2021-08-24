/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AlertModule.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http.alert;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import javax.servlet.ServletContext;

import org.rvpf.base.alert.Event;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.http.HTTPModule;

/**
 * Alert module.
 */
public final class AlertModule
    extends HTTPModule.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public void doEventActions(final Event event)
    {
        _alertContext.onEvent(event);
    }

    /** {@inheritDoc}
     */
    @Override
    public void doPendingActions()
    {
        super.doPendingActions();

        for (;;) {
            final AlertContext.Signal signal = _alertContext
                .nextSignal()
                .orElse(null);

            if (signal == null) {
                break;
            }

            getService().sendSignal(signal.getName(), signal.getInfo());
        }
    }

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
        servletContext.setAttribute(ALERT_CONTEXT_ATTRIBUTE, _alertContext);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void addServlets(final Map<String, String> servlets)
    {
        servlets.put(EVENTS_PATH, EventsServlet.class.getName());
        servlets.put(STATUS_PATH, StatusServlet.class.getName());
        servlets.put(TRIGGER_PATH, TriggerServlet.class.getName());
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups contextProperties)
    {
        _alertContext = new AlertContext(this);

        if (!_alertContext.setUp(contextProperties)) {
            return false;
        }

        callbackForEventActions();

        return true;
    }

    /**
     * Creates an event.
     *
     * @param name The name of the event.
     *
     * @return The event.
     */
    Event createEvent(@Nonnull final String name)
    {
        return new Event(
            name,
            Optional.of(getService().getServiceName()),
            getService().getEntityName(),
            Optional.of(getService().getSourceUUID()),
            Optional.empty());
    }

    /** Alert context attribute. */
    public static final String ALERT_CONTEXT_ATTRIBUTE = "alert.context";

    /** Default path. */
    public static final String DEFAULT_PATH = "alert";

    /** Events path. */
    public static final String EVENTS_PATH = "/events";

    /** Status path. */
    public static final String STATUS_PATH = "/status";

    /** Trigger path. */
    public static final String TRIGGER_PATH = "/trigger";

    private AlertContext _alertContext;
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RPNModule.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.http.rpn;

import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletContext;

import org.rvpf.base.DateTime;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.http.HTTPMessages;
import org.rvpf.http.HTTPModule;
import org.rvpf.metadata.entity.EngineEntity;
import org.rvpf.metadata.processor.Engine;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.RPNEngine;
import org.rvpf.processor.engine.rpn.RPNExecutor;

/**
 * RPN module.
 */
public final class RPNModule
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
        servletContext
            .setAttribute(
                RPN_CONTEXT_ATTRIBUTE,
                new RPNContext(getMetadata(), _executor));
    }

    /** {@inheritDoc}
     */
    @Override
    protected void addServlets(final Map<String, String> servlets)
    {
        servlets.put(EXECUTE_PATH, RPNServlet.class.getName());
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups contextProperties)
    {
        final String engineName = contextProperties
            .getString(ENGINE_PROPERTY, Optional.of(DEFAULT_ENGINE))
            .get();

        if (!loadMetadata(new RPNMetadataFilter(engineName))) {
            return false;
        }

        final EngineEntity engineEntity = getMetadata()
            .getEngineEntity(Optional.of(engineName))
            .orElse(null);

        if (engineEntity == null) {
            getThisLogger()
                .error(ProcessorMessages.ENGINE_NOT_FOUND, engineName);

            return false;
        }

        if (!engineEntity.setUp(getMetadata())) {
            getThisLogger().error(HTTPMessages.ENGINE_CONFIG, engineName);

            return false;
        }

        final Optional<? extends Engine> engine = engineEntity.getEngine();

        if (!engine.isPresent() || !(engine.get() instanceof RPNEngine)) {
            getThisLogger()
                .error(
                    ProcessorMessages.ENGINE_CLASS,
                    engineName,
                    RPNEngine.class.getName());

            return false;
        }

        _executor = new RPNExecutor(
            (RPNEngine) engine.get(),
            DateTime.getTimeZone());

        getMetadata().cleanUp();

        return true;
    }

    /** Default engine. */
    public static final String DEFAULT_ENGINE = "RPN";

    /** Default path. */
    public static final String DEFAULT_PATH = "rpn";

    /** Specifies the engine name. */
    public static final String ENGINE_PROPERTY = "engine";

    /** Execute path. */
    public static final String EXECUTE_PATH = "/execute";

    /** RPN context attribute. */
    public static final String RPN_CONTEXT_ATTRIBUTE = "rpn.context";

    private RPNExecutor _executor;
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

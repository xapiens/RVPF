/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LocalExecutorEngine.java 4042 2019-06-02 13:28:46Z SFB $
 */

package org.rvpf.processor.engine.executor;

import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.behavior.Synchronized;
import org.rvpf.processor.engine.AbstractEngine;

/**
 * Local executor engine.
 */
public final class LocalExecutorEngine
    extends AbstractEngine
{
    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        _executor.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public Transform createTransform(final TransformEntity proxyEntity)
    {
        final LocalExecutorTransform transform = new LocalExecutorTransform(
            _executor);

        if (!transform.setUp(getMetadata(), proxyEntity)) {
            return null;
        }

        return transform;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        final Optional<ClassDef> classDef = getParams()
            .getClassDef(ENGINE_EXECUTOR_PARAM, Optional.empty());

        if (!classDef.isPresent()) {
            getThisLogger()
                .error(BaseMessages.MISSING_PARAMETER, ENGINE_EXECUTOR_PARAM);

            return false;
        }

        _executor = classDef.get().createInstance(EngineExecutor.class);

        if (_executor == null) {
            return false;
        }

        return _executor
            .setUp(getName(), getParams(), metadata, getThisLogger());
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_executor != null) {
            _executor.tearDown();
            _executor = null;
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<ClassDef> defaultBehavior()
    {
        return _DEFAULT_BEHAVIOR;
    }

    private static final Optional<ClassDef> _DEFAULT_BEHAVIOR = Optional
        .of(new ClassDefImpl(Synchronized.class));

    private EngineExecutor _executor;
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

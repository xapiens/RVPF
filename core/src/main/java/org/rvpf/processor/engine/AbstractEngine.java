/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AbstractEngine.java 4042 2019-06-02 13:28:46Z SFB $
 */

package org.rvpf.processor.engine;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.PointRelation;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.processor.Behavior;
import org.rvpf.metadata.processor.Engine;

/**
 * Abstract engine.
 */
public abstract class AbstractEngine
    extends Proxied.Abstract
    implements Engine
{
    /** {@inheritDoc}
     */
    @Override
    public void close() {}

    /** {@inheritDoc}
     */
    @Override
    public Optional<BehaviorEntity> getDefaultBehavior(
            final PointRelation relation)
    {
        if (_defaultBehavior == null) {
            return Optional.empty();
        }

        final Class<?> instanceClass = _defaultBehavior.getInstanceClass();

        return (instanceClass != null)? getDefaultBehavior(
            instanceClass): Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        _defaultBehavior = getParams()
            .getClassDef(DEFAULT_BEHAVIOR_PARAM, defaultBehavior())
            .orElse(null);

        return true;
    }

    /**
     * Returns the engine specific default behavior.
     *
     * @return The optional default behavior class def.
     */
    @Nonnull
    @CheckReturnValue
    protected Optional<ClassDef> defaultBehavior()
    {
        return Optional.empty();
    }

    /**
     * Gets a default behavior entity wrapping the specified class.
     *
     * <p>If this is the first time that the class has been requested, an
     * instance will be created.</p>
     *
     * @param behaviorClass The wrapped class of the requested behavior entity.
     *                      Must implement {@link Behavior}.
     *
     * @return The optional behavior entity.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<BehaviorEntity> getDefaultBehavior(
            @Nonnull final Class<?> behaviorClass)
    {
        Optional<BehaviorEntity> behaviorEntity = getMetadata()
            .getDefaultBehavior(behaviorClass);

        if (!behaviorEntity.isPresent()) {
            Behavior behaviorInstance = null;

            try {
                behaviorInstance = (Behavior) behaviorClass
                    .getConstructor()
                    .newInstance();
            } catch (final Exception exception) {
                getThisLogger()
                    .error(
                        BaseMessages.INSTANTIATION_FAILED,
                        behaviorClass.getName(),
                        exception.getMessage());
            }

            if (behaviorInstance != null) {
                final BehaviorEntity.Builder behaviorBuilder = BehaviorEntity
                    .newBuilder();
                final ClassDefEntity.Builder classDefBuilder = ClassDefEntity
                    .newBuilder()
                    .setImpl(new ClassDefImpl(behaviorClass));

                classDefBuilder.setName(behaviorClass.getName());

                behaviorBuilder
                    .setGenerated(true)
                    .setClassDef(Optional.of(classDefBuilder.build()))
                    .setInstance(behaviorInstance);

                behaviorEntity = Optional.of(behaviorBuilder.build());

                if (behaviorInstance
                    .setUp(getMetadata(), behaviorEntity.get())) {
                    getMetadata()
                        .setDefaultBehavior(
                            behaviorClass,
                            behaviorEntity.get());
                } else {
                    behaviorEntity = Optional.empty();
                }
            }
        }

        return behaviorEntity;
    }

    /** Specifies the default behavior. */
    public static final String DEFAULT_BEHAVIOR_PARAM = "DefaultBehavior";

    private ClassDef _defaultBehavior;
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

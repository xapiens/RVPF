/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.processor.engine.rpn.selector;

import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.PointRelation;
import org.rvpf.base.tool.Require;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.metadata.processor.Behavior;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.behavior.Retriggers;
import org.rvpf.processor.engine.rpn.RPNEngine;
import org.rvpf.processor.engine.rpn.operation.Operation;
import org.rvpf.processor.engine.rpn.operation.Operations;
import org.rvpf.processor.engine.rpn.selector.summarizer.SummarizesBehavior;

/**
 * Selector engine.
 *
 * <p>Implements an RPN engine acting on selected values of an input point
 * (the last one) controlled by an other input point (the first one).</p>
 */
public abstract class SelectorEngine
    extends RPNEngine
{
    /** {@inheritDoc}
     */
    @Override
    public final Transform createTransform(final TransformEntity proxyEntity)
    {
        final Transform transform = newTransform();

        if (!transform.setUp(getMetadata(), proxyEntity)) {
            return null;
        }

        return transform;
    }

    /** {@inheritDoc}
     *
     * <p>The first relation selects and the last one is selected. Each
     * intermediate relation retriggers.</p>
     */
    @Override
    public Optional<BehaviorEntity> getDefaultBehavior(
            final PointRelation relation)
    {
        final List<? extends PointRelation> relations = relation
            .getResultPoint()
            .getInputs();
        final int position = relations.indexOf(relation);
        final Class<? extends Behavior> behaviorClass;

        Require.success(position >= 0);

        if (position == 0) {
            behaviorClass = getSelectsBehaviorClass();
        } else if (position == (relations.size() - 1)) {
            behaviorClass = getSelectedBehaviorClass();
        } else {
            return super.getDefaultBehavior(relation);
        }

        return getDefaultBehavior(behaviorClass);
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean setUp(
            final Metadata metadata,
            final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        try {
            register(newOperations());
        } catch (final Operation.OverloadException exception) {
            return false;
        }

        if (metadata
            .getBooleanValue(SummarizesBehavior.REVERSE_INTERVAL_PROPERTY)) {
            getThisLogger().info(ProcessorMessages.REVERSE_INTERVAL);
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected final Optional<ClassDef> defaultBehavior()
    {
        return _DEFAULT_BEHAVIOR;
    }

    /**
     * Gets the selected behavior class.
     *
     * @return The selected behavior class.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Class<? extends SelectedBehavior> getSelectedBehaviorClass();

    /**
     * Gets the selects behavior class.
     *
     * @return The selects behavior class.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Class<? extends SelectsBehavior> getSelectsBehaviorClass();

    /**
     * Returns new operations.
     *
     * @return The operations.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Operations newOperations();

    /**
     * Returns a new transform.
     *
     * @return The transform.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Transform newTransform();

    private static final Optional<ClassDef> _DEFAULT_BEHAVIOR = Optional
        .of(new ClassDefImpl(Retriggers.class));
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

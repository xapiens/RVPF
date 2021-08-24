/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ControlTransform.java 4056 2019-06-04 15:21:02Z SFB $
 */

package org.rvpf.processor.engine.control;

import java.io.Serializable;

import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.content.BooleanContent;
import org.rvpf.document.version.VersionControl;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.Behavior;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.AbstractTransform;

/**
 * Control transform.
 */
public final class ControlTransform
    extends AbstractTransform
{
    /** {@inheritDoc}
     */
    @Override
    public Optional<PointValue> applyTo(
            final ResultValue resultValue,
            final Batch batch)
    {
        final PointValue inputValue = resultValue.getInputValues().get(0);
        final NormalizedValue normalizedValue = inputValue.normalized();

        if (_action == UPDATE_DOCUMENT_ACTION) {
            final Serializable value = normalizedValue.getValue();

            batch
                .queueSignal(
                    VersionControl.UPDATE_DOCUMENT_SIGNAL,
                    (value != null)? Optional
                        .of(value.toString()): Optional.empty());
        } else if (_action == UPDATE_CUTOFF_CONTROL_ACTION) {
            batch.setCutoff(Optional.ofNullable(normalizedValue));
        } else if (_action == UPDATE_FILTER_CONTROL_ACTION) {
            final Boolean updatesFiltered = (Boolean) normalizedValue
                .getValue();

            if (updatesFiltered != null) {
                batch.setUpdatesFiltered(updatesFiltered.booleanValue());
            }
        }

        if (inputValue.getPoint().get() == resultValue.getPoint().get()) {
            return Optional.of(Batch.DISABLED_UPDATE);
        }

        resultValue.copyValueFrom(normalizedValue.encoded());

        return Optional.of(resultValue);
    }

    /**
     * Gets the action name.
     *
     * @return The action name.
     */
    @Nonnull
    @CheckReturnValue
    public String getAction()
    {
        return Require.notNull(_action);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<? extends Transform> getInstance(final Point point)
    {
        final List<? extends PointRelation> inputs = point.getInputs();

        if (inputs.size() != 1) {
            getThisLogger()
                .error(
                    ProcessorMessages.TRANSFORM_SINGLE_INPUT,
                    getName(),
                    point);

            return Optional.empty();
        }

        final Behavior behavior = ((PointInput) inputs.get(0))
            .getPrimaryBehavior()
            .orElse(null);

        if (!ControlsBehavior.class.isInstance(behavior)) {
            getThisLogger()
                .error(
                    ProcessorMessages.TRANSFORM_BEHAVIOR_1,
                    getName(),
                    ControlsBehavior.class.getName(),
                    point);

            return Optional.empty();
        }

        if (_action == UPDATE_FILTER_CONTROL_ACTION) {
            if (!(point.getContent().orElse(null) instanceof BooleanContent)) {
                getThisLogger()
                    .error(
                        ProcessorMessages.CONTROL_LOGICAL,
                        UPDATE_FILTER_CONTROL_ACTION);
            }
        }

        return Optional.of(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        final String action;

        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        action = getParams().getString(ACTION_PARAM).orElse(null);

        if (action == null) {
            getThisLogger().error(BaseMessages.MISSING_PARAMETER, ACTION_PARAM);

            return false;
        }

        if (UPDATE_DOCUMENT_ACTION.equalsIgnoreCase(action)) {
            _action = UPDATE_DOCUMENT_ACTION;
        } else if (UPDATE_CUTOFF_CONTROL_ACTION.equalsIgnoreCase(action)) {
            _action = UPDATE_CUTOFF_CONTROL_ACTION;
        } else if (UPDATE_FILTER_CONTROL_ACTION.equalsIgnoreCase(action)) {
            _action = UPDATE_FILTER_CONTROL_ACTION;
        } else {
            getThisLogger().error(ProcessorMessages.UNKNOWN_ACTION, action);

            return false;
        }

        return true;
    }

    /** Update cutoff control action. */
    public static final String UPDATE_CUTOFF_CONTROL_ACTION =
        "UpdateCutoffControl";

    /** Update document action. */
    public static final String UPDATE_DOCUMENT_ACTION = "UpdateDocument";

    /** Update filter control action. */
    public static final String UPDATE_FILTER_CONTROL_ACTION =
        "UpdateFilterControl";

    private String _action;
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

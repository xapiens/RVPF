/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ReplicatedBehavior.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.processor.engine.replicator;

import org.rvpf.base.DateTime;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.behavior.PrimaryBehavior;

/**
 * Replicated behavior.
 */
public class ReplicatedBehavior
    extends PrimaryBehavior
{
    /** {@inheritDoc}
     */
    @Override
    public final boolean isResultFetched(
            final PointValue noticeValue,
            final ResultValue resultValue)
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean prepareSelect(
            final ResultValue resultValue,
            final Batch batch)
    {
        switch (batch.getLookUpPass()) {
            case 1: {
                final StoreValuesQuery.Builder storeValuesQueryBuilder =
                    StoreValuesQuery
                        .newBuilder()
                        .setPoint(resultValue.getPoint().get());
                final DateTime stamp = resultValue.getStamp();

                storeValuesQueryBuilder.setAt(stamp);
                batch.addStoreValuesQuery(storeValuesQueryBuilder.build());

                return true;
            }
            default: {
                return true;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean select(final ResultValue resultValue, final Batch batch)
    {
        final DateTime stamp = resultValue.getStamp();
        final BatchValuesQuery.Builder batchValuesQueryBuilder =
            BatchValuesQuery
                .newBuilder()
                .setPoint(resultValue.getPoint());
        final PointValue pointValue;

        batchValuesQueryBuilder.setAt(stamp);
        pointValue = batch.getPointValue(batchValuesQueryBuilder.build());

        resultValue.setValue(pointValue.isPresent()? pointValue: null);

        return true;
    }

    /** {@inheritDoc}
     *
     * <p>Since we are replicating, it would be reasonable for the notice and
     * the result to refer to the same point definition. In order to be able to
     * get the result value from its locally configured Store, we ask the Batch
     * to forget about the notice, then set up a result with the notice as
     * input.</p>
     */
    @Override
    public void trigger(final PointValue noticeValue, final Batch batch)
    {
        final ResultValue result;

        if (getInputPoint() == getResultPoint()) {
            batch.forgetInputValue(noticeValue);
        }

        result = batch.setUpResultValue(noticeValue.getStamp(), this);

        result.addInputValue(noticeValue);
    }

    /** {@inheritDoc}
     *
     * <p>Because of its special interaction with the Batch, this Behavior is
     * intolerant of others on the same relation.</p>
     *
     * <p>If the input replicates itself, it is not allowed to have other
     * dependents on the same Processor.</p>
     */
    @Override
    protected boolean doValidate()
    {
        if (!validateTransform(ReplicatorTransform.class)) {
            return false;
        }

        if (getNext().isPresent()) {
            getThisLogger()
                .error(ProcessorMessages.BEHAVIOR_INCOMPATIBLE_ANY, getName());

            return false;
        }

        if ((getInputPoint() == getResultPoint()) && !validateNoResults()) {
            return false;
        }

        return validateNotSynchronized();
    }
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

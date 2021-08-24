/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Transform.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.metadata.processor;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Proxied;

/**
 * Transform.
 *
 * <p>The transform protocol is used to apply a program created by an engine to
 * the computation of a result value needing update.</p>
 */
public interface Transform
    extends Proxied
{
    /**
     * Applies the program to provide the specified result value.
     *
     * <p>When this method is called, the result value must have been provided
     * with all its needed input values.</p>
     *
     * <p>Note: the returned value is not required to be an instance of the
     * ResultValue class.</p>
     *
     * @param resultValue The result value needing update.
     * @param batch The current batch.
     *
     * @return The resulting point value or empty.
     *
     * @throws InterruptedException When interrupted.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    @Nonnull
    @CheckReturnValue
    Optional<PointValue> applyTo(
            @Nonnull ResultValue resultValue,
            @Nonnull Batch batch)
        throws InterruptedException, ServiceNotAvailableException;

    /**
     * Gets an appropriate instance of this transform for the point.
     *
     * @param point The point.
     *
     * @return A transform instance or empty.
     */
    @Nonnull
    @CheckReturnValue
    Optional<? extends Transform> getInstance(@Nonnull Point point);

    /**
     * Asks if a null result should cause a removal in the store.
     *
     * @param point The point.
     *
     * @return True if a null should cause removal.
     */
    @CheckReturnValue
    boolean isNullRemoves(@Nonnull Point point);

    /**
     * Sets up this instance.
     *
     * <p>This is called after the set up of the points relationships.</p>
     *
     * @param point The point for the instance.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull Point point);

    /**
     * Asks if this transform needs the fetched result value.
     *
     * @return True if the result is used.
     */
    @CheckReturnValue
    boolean usesFetchedResult();

    /** Identifies the action requested. */
    String ACTION_PARAM = "Action";

    /**
     * Specifies the time interval relative to the next Sync interval which can
     * be trimmed to resynchronize the result.
     */
    String CEILING_INTERVAL_PARAM = "CeilingInterval";

    /**
     * Specifies the ratio of the offset relative to the next Sync interval
     * which can be trimmed to resynchronize the result.
     */
    String CEILING_RATIO_PARAM = "CeilingRatio";

    /**
     * Specifies that the failure to produce a result should return a null
     * value.
     */
    String FAIL_RETURNS_NULL_PARAM = Point.FAIL_RETURNS_NULL_PARAM;

    /** The final program to execute. */
    String FINAL_PROGRAM_PARAM = "FinalProgram";

    /**
     * Specifies the time interval relative to the previous Sync interval which
     * can be trimmed to resynchronize the result.
     */
    String FLOOR_INTERVAL_PARAM = "FloorInterval";

    /**
     * Specifies the ratio of the offset relative to the previous Sync interval
     * which can be trimmed to resynchronize the result.
     */
    String FLOOR_RATIO_PARAM = "FloorRatio";

    /** The initial program to execute. */
    String INITIAL_PROGRAM_PARAM = "InitialProgram";

    /** Specifies if a null result should cause a removal from the store. */
    String NULL_REMOVES_PARAM = Point.NULL_REMOVES_PARAM;

    /** Specifies values which will be made available to the process. */
    String PARAM_PARAM = Point.PARAM_PARAM;

    /** Instructions to compute the result from its inputs. */
    String PROGRAM_PARAM = "Program";

    /** Program to execute for each step of the summarized value. */
    String STEP_PROGRAM_PARAM = "StepProgram";
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

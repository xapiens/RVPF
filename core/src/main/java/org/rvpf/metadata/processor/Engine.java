/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Engine.java 3956 2019-05-06 11:17:05Z SFB $
 */

package org.rvpf.metadata.processor;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.PointRelation;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.TransformEntity;

/**
 * Engine.
 * <p>
 * An engine is used to create a transform program able to compute a result
 * given the appropriate inputs.
 * </p>
 * <p>
 * It also holds services and values shared by its transforms.
 * </p>
 */
public interface Engine
    extends Proxied
{
    /**
     * Closes this engine.
     */
    void close();

    /**
     * Creates a transform object.
     *
     * @param proxyEntity The proxy (transform entity) for the transform.
     *
     * @return The transform (null on failure).
     */
    @Nullable
    @CheckReturnValue
    Transform createTransform(@Nonnull TransformEntity proxyEntity);

    /**
     * Gets the default (primary) behavior.
     *
     * @param relation The relation needing a primary behavior.
     *
     * @return The optional default behavior.
     */
    @Nonnull
    @CheckReturnValue
    Optional<BehaviorEntity> getDefaultBehavior(PointRelation relation);

    /** Specifies the RMI URI for the server binding. */
    String BINDING_PARAM = "Binding";

    /** EngineExecutor implementation parameter. */
    String ENGINE_EXECUTOR_PARAM = "EngineExecutor";

    /** Limits the number of iterations for each loop operation. */
    String LOOP_LIMIT_PARAM = "LoopLimit";

    /** Defines a macro instruction. */
    String MACRO_PARAM = "Macro";

    /** Specifies an extension module. */
    String MODULE_PARAM = "Module";

    /** Specifies the name for the RMI server. */
    String NAME_PARAM = "Name";

    /** Specifies the password for authentication to the server. */
    String PASSWORD_PARAM = "Password";

    /** Specifies the security properties for connection to the server. */
    String SECURITY_PARAM = "Security";

    /** Specifies the user for identification to the server. */
    String USER_PARAM = "User";

    /** Defines an additional word. */
    String WORD_PARAM = "Word";
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

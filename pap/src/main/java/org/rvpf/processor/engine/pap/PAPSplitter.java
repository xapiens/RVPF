/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPSplitter.java 4022 2019-05-24 17:08:04Z SFB $
 */

package org.rvpf.processor.engine.pap;

import java.io.Serializable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.PAP;
import org.rvpf.pap.PAPMessages;
import org.rvpf.service.ServiceMessages;

/**
 * PAP splitter.
 */
public abstract class PAPSplitter
{
    /**
     * Sets up a plan for a point.
     *
     * @param point The point.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public abstract boolean setUp(@Nonnull Point point);

    /**
     * Splits the values of a point value.
     *
     * @param pointValue The point value to split.
     *
     * @return An optional splitted object (empty when no plan).
     */
    @Nonnull
    @CheckReturnValue
    public abstract Optional<Splitted> split(@Nonnull PointValue pointValue);

    /**
     * Gets a result position.
     *
     * @param resultRelation The result relation.
     *
     * @return The result position (negative on failure).
     */
    @CheckReturnValue
    protected final int getResultPosition(
            @Nonnull final PointRelation resultRelation)
    {
        final Params resultRelationParams = resultRelation.getParams();
        final String positionKey;

        if (resultRelationParams.containsValueKey(PAP.INDEX_PARAM)) {
            if (resultRelationParams.containsValueKey(PAP.OFFSET_PARAM)) {
                getThisLogger()
                    .warn(
                        ServiceMessages.PARAM_CONFLICT,
                        PAP.INDEX_PARAM,
                        PAP.OFFSET_PARAM);

                return -1;
            }

            positionKey = PAP.INDEX_PARAM;
        } else if (resultRelationParams.containsValueKey(PAP.OFFSET_PARAM)) {
            positionKey = PAP.OFFSET_PARAM;
        } else {
            getThisLogger()
                .warn(
                    PAPMessages.MISSING_POSITION_PARAMETER,
                    resultRelation.getResultPoint());

            return -1;
        }

        final int index = resultRelationParams.getInt(positionKey, -1);

        if (index < 0) {
            getThisLogger()
                .warn(
                    PAPMessages.BAD_PARAMETER_VALUE,
                    positionKey,
                    String.valueOf(index));
        }

        return index;
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return _logger;
    }

    private final Logger _logger = Logger.getInstance(getClass());

    /**
     * Splitted values.
     */
    public static final class Splitted
        implements Serializable
    {
        /**
         * Constructs an instance.
         */
        public Splitted() {}

        /**
         * Gets a value for a point.
         *
         * @param point The point.
         *
         * @return The optional value.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<Serializable> get(@Nonnull final Point point)
        {
            return _splitted.get(point.getUUID().get());
        }

        /**
         * Puts a value for a point.
         *
         * @param point The point.
         * @param value The optional value.
         */
        public void put(
                @Nonnull final Point point,
                @Nonnull final Optional<Serializable> value)
        {
            _splitted.put(point.getUUID().get(), value);
        }

        /**
         * Returns the size.
         *
         * @return The size.
         */
        @CheckReturnValue
        public int size()
        {
            return _splitted.size();
        }

        private static final long serialVersionUID = 1L;

        private final Map<UUID, Optional<Serializable>> _splitted =
            new LinkedHashMap<>();
    }


    /**
     * Detail.
     */
    protected abstract static class Detail
    {
        /**
         * Constructs an instance.
         *
         * @param point The point.
         * @param position The position in the tuple.
         * @param bit The bit in the tuple value (negative if none).
         */
        protected Detail(
                @Nonnull final Point point,
                final int position,
                final int bit)
        {
            _point = point;
            _position = position;
            _bit = bit;
        }

        /**
         * Gets the bit.
         *
         * @return The bit.
         */
        @CheckReturnValue
        public int getBit()
        {
            return _bit;
        }

        /**
         * Gets the point.
         *
         * @return The point.
         */
        @Nonnull
        @CheckReturnValue
        public Point getPoint()
        {
            return _point;
        }

        /**
         * Gets the position.
         *
         * @return The position.
         */
        @CheckReturnValue
        public int getPosition()
        {
            return _position;
        }

        private final int _bit;
        private final Point _point;
        private final int _position;
    }


    /**
     * Plan.
     */
    protected abstract static class Plan
    {
        /**
         * Constructs an instance.
         *
         * @param details Plan details.
         */
        protected Plan(@Nonnull final Detail[] details)
        {
            _details = details;
        }

        /**
         * Gets the details.
         *
         * @return The details.
         */
        @Nonnull
        @CheckReturnValue
        protected Detail[] getDetails()
        {
            return _details;
        }

        private final Detail[] _details;
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

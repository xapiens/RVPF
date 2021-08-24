/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RPNExecutor.java 4040 2019-05-31 18:55:08Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;

/**
 * RPN Executor.
 */
public class RPNExecutor
{
    /**
     * Constructs an instance.
     *
     * @param engine The RPN engine.
     * @param timeZone The time zone.
     */
    public RPNExecutor(
            @Nonnull final RPNEngine engine,
            @Nonnull final TimeZone timeZone)
    {
        _engine = engine;
        _timeZone = timeZone;
    }

    /**
     * Returns a result from a result value and a point value.
     *
     * @param resultValue The result value.
     * @param optionalPointValue The optional point value
     *
     * @return The optional resulting value.
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<PointValue> returnResult(
            @Nonnull final ResultValue resultValue,
            @Nonnull final Optional<PointValue> optionalPointValue)
    {
        PointValue pointValue;

        if (optionalPointValue.isPresent()) {
            pointValue = optionalPointValue.get().encoded();
            pointValue = pointValue
                .morph(
                    resultValue.getPoint(),
                    pointValue.hasStamp()? Optional.empty(): resultValue
                        .hasStamp()? Optional
                                .of(resultValue.getStamp()): Optional.empty());
        } else {
            pointValue = null;
        }

        return Optional.ofNullable(pointValue);
    }

    /**
     * Executes the program compiled from the source on the resultValue's
     * inputs.
     *
     * @param source The source program.
     * @param macros The macro instruction definitions.
     * @param words The word definitions.
     * @param resultValue The ResultValue.
     * @param logger The Logger.
     *
     * @return The ResultValue (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public PointValue execute(
            @Nonnull final String source,
            @Nonnull final String[] macros,
            @Nonnull final String[] words,
            @Nonnull final ResultValue resultValue,
            @Nonnull final Logger logger)
    {
        final Task.Context context = new Task.Context(
            resultValue,
            false,
            Optional.empty(),
            Optional.of(_timeZone),
            logger);
        final Optional<Map<String, MacroDef>> macroDefs;
        final Map<String, Program> wordDefs;
        final Program program;

        try {
            macroDefs = _engine.getMacroDefs(macros);
            wordDefs = _engine
                .getWordDefs(words, macroDefs, Optional.empty(), logger);
        } catch (final Compiler.CompileException exception) {
            logger.error(BaseMessages.VERBATIM, exception.getMessage());

            return null;
        }

        program = _engine
            .compile(source, macroDefs, Optional.ofNullable(wordDefs), logger);

        if (program == null) {
            return null;
        }

        resultValue.setValue(null);

        return returnResult(resultValue, new Task(context).execute(program))
            .orElse(null);
    }

    private final RPNEngine _engine;
    private final TimeZone _timeZone;
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

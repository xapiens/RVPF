/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RPNTransform.java 4056 2019-06-04 15:21:02Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Point;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.processor.engine.AbstractTransform;

/**
 * RPN transform.
 *
 * <p>Instances of this class apply the RPN program from its
 * '{@value org.rvpf.metadata.processor.Transform#PROGRAM_PARAM}' parameter.
 * This program is compiled on startup, using the macro and word definitions
 * supplied by the'{@value org.rvpf.processor.engine.rpn.RPNEngine#MACRO_PARAM}'
 * and'{@value org.rvpf.processor.engine.rpn.RPNEngine#WORD_PARAM}' parameters
 * for itself and its engine.</p>
 *
 * @see RPNEngine
 */
public final class RPNTransform
    extends AbstractTransform
{
    /**
     * Constructs a RPN transform.
     *
     * @param engine The RPN engine.
     */
    public RPNTransform(@Nonnull final RPNEngine engine)
    {
        _engine = engine;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<PointValue> applyTo(
            final ResultValue resultValue,
            final Batch batch)
    {
        final Task.Context context = new Task.Context(
            resultValue,
            _failReturnsNull,
            Optional.of(batch),
            Optional.empty(),
            getThisLogger());
        final Optional<PointValue> pointValue = new Task(context)
            .execute(_program);

        return RPNExecutor.returnResult(resultValue, pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        final Optional<String> source = proxyEntity
            .getParams()
            .getString(PROGRAM_PARAM);

        if (!source.isPresent()) {
            getThisLogger()
                .error(BaseMessages.MISSING_PARAMETER, PROGRAM_PARAM);

            return false;
        }

        final Optional<Map<String, MacroDef>> macroDefs;
        final Optional<Map<String, Program>> wordDefs;

        try {
            macroDefs = _engine.getMacroDefs(proxyEntity);
            wordDefs = _engine.getWordDefs(proxyEntity, macroDefs);
        } catch (final Compiler.CompileException exception) {
            getThisLogger()
                .error(BaseMessages.VERBATIM, exception.getMessage());

            return false;
        }

        _program = _engine
            .compile(source.get().trim(), macroDefs, wordDefs, getThisLogger());

        if (_program == null) {
            return false;
        }

        _failReturnsNull = getParams()
            .getBoolean(Point.FAIL_RETURNS_NULL_PARAM);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean usesFetchedResult()
    {
        return true;
    }

    private final RPNEngine _engine;
    private boolean _failReturnsNull;
    private Program _program;
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

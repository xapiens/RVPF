/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RPNEngine.java 4028 2019-05-26 18:11:34Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.PointRelation;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.behavior.PrimaryBehavior;
import org.rvpf.processor.behavior.Retriggers;
import org.rvpf.processor.behavior.Synchronized;
import org.rvpf.processor.engine.AbstractEngine;
import org.rvpf.processor.engine.rpn.operation.Operation;
import org.rvpf.processor.engine.rpn.operation.Operations;

/**
 * RPN engine.
 *
 * <p>Implements an RPN (Reverse Polish Notation) computing engine accepting a
 * Forth-like syntax.</p>
 *
 * <p>The operations available to the transform programs are supported by the
 * modules supplied to this engine by the '{@value #MODULE_PARAM}'
 * parameter.</p>
 *
 * @see RPNTransform
 * @see Compiler
 */
public class RPNEngine
    extends AbstractEngine
{
    /**
     * Compile a Program from a source String.
     *
     * @param source The source code.
     * @param macroDefs The optional macro instruction definitions.
     * @param wordDefs Optional additional word definitions.
     * @param logger The Logger.
     *
     * @return The Transform Program (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public final Program compile(
            @Nonnull final String source,
            @Nonnull final Optional<Map<String, MacroDef>> macroDefs,
            @Nonnull final Optional<Map<String, Program>> wordDefs,
            @Nonnull final Logger logger)
    {
        return _compiler.compile(source.trim(), macroDefs, wordDefs, logger);
    }

    /** {@inheritDoc}
     */
    @Override
    public Transform createTransform(final TransformEntity proxyEntity)
    {
        final RPNTransform transform = new RPNTransform(this);

        if (!transform.setUp(getMetadata(), proxyEntity)) {
            return null;
        }

        return transform;
    }

    /** {@inheritDoc}
     *
     * <p>On self-reference (when the input point is the same as the result
     * point), the default behavior becomes {@link Retriggers} unless the
     * {@value org.rvpf.processor.behavior.PrimaryBehavior#SELECT_SYNC_POSITION_PARAM}
     * is specified.</p>
     */
    @Override
    public Optional<BehaviorEntity> getDefaultBehavior(
            final PointRelation relation)
    {
        return ((relation.getInputPoint() == relation.getResultPoint())
                && (relation.getParams().getInt(
                    PrimaryBehavior.SELECT_SYNC_POSITION_PARAM,
                    0) >= 0))? getDefaultBehavior(
                            Retriggers.class): super
                                    .getDefaultBehavior(relation);
    }

    /**
     * Gets the macro instruction definitions.
     *
     * <p>On name collision, the definitions of the transform will override
     * those of the engine.</p>
     *
     * @param proxyEntity The proxy (transform entity) for the transform.
     *
     * @return The optional macro defs.
     *
     * @throws Compiler.CompileException On macro compilation exception.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Map<String, MacroDef>> getMacroDefs(
            @Nonnull final ProxyEntity proxyEntity)
        throws Compiler.CompileException
    {
        final String[] transformMacros = proxyEntity
            .getParams()
            .getStrings(MACRO_PARAM);
        final Optional<Map<String, MacroDef>> macroDefs;

        if (_macroDefs.isPresent()) {
            if (transformMacros.length > 0) {
                macroDefs = Optional
                    .of(new HashMap<String, MacroDef>(_macroDefs.get()));
                macroDefs.get().putAll(getMacroDefs(transformMacros).get());
            } else {
                macroDefs = _macroDefs;
            }
        } else {
            macroDefs = getMacroDefs(transformMacros);
        }

        return macroDefs;
    }

    /**
     * Gets the additional word definitions.
     *
     * <p>On name collision, the definitions of the transform will override
     * those of the engine.</p>
     *
     * @param proxyEntity The proxy (transform entity) for the transform.
     * @param macroDefs The optional macro instruction definitions.
     *
     * @return The optional word definitions.
     *
     * @throws Compiler.CompileException On word compilation exception.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Map<String, Program>> getWordDefs(
            @Nonnull final ProxyEntity proxyEntity,
            @Nonnull final Optional<Map<String, MacroDef>> macroDefs)
        throws Compiler.CompileException
    {
        final String[] transformWords = proxyEntity
            .getParams()
            .getStrings(WORD_PARAM);
        final Optional<Map<String, Program>> wordDefs;

        if (_macroDefs != null) {
            if (transformWords.length > 0) {
                wordDefs = Optional
                    .of(new HashMap<String, Program>(_wordDefs.get()));
                wordDefs
                    .get()
                    .putAll(
                        getWordDefs(
                            transformWords,
                            macroDefs,
                            _wordDefs,
                            getThisLogger()));
            } else {
                wordDefs = _wordDefs;
            }
        } else {
            wordDefs = Optional
                .of(
                    getWordDefs(
                        transformWords,
                        macroDefs,
                        _wordDefs,
                        getThisLogger()));
        }

        return wordDefs;
    }

    /**
     * Registers an Operations module.
     *
     * @param module The Operations module.
     *
     * @throws Operation.OverloadException From Operations set up.
     */
    public final void register(
            @Nonnull final Operations module)
        throws Operation.OverloadException
    {
        module.setUp(_compiler.getRegistrations(), getParams());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        final ClassDef[] modules = proxyEntity
            .getParams()
            .getClassDefs(MODULE_PARAM);

        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        try {
            _compiler = new Compiler(getParams());

            for (final ClassDef classDef: modules) {
                final Operations module = classDef
                    .createInstance(Operations.class);

                if (module == null) {
                    return false;
                }

                register(module);
            }
        } catch (final Operation.OverloadException exception) {
            return false;
        }

        try {
            _macroDefs = getMacroDefs(getParams().getStrings(MACRO_PARAM));
            _wordDefs = Optional
                .of(
                    getWordDefs(
                        getParams().getStrings(WORD_PARAM),
                        _macroDefs,
                        Optional.empty(),
                        getThisLogger()));
        } catch (final Compiler.CompileException exception) {
            getThisLogger()
                .error(BaseMessages.VERBATIM, exception.getMessage());

            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<ClassDef> defaultBehavior()
    {
        return _DEFAULT_BEHAVIOR;
    }

    /**
     * Gets macro def instances from their definition texts.
     *
     * <p>Will return null if the texts array is null or contains nothing.</p>
     *
     * @param macros The macro instruction definition texts.
     *
     * @return The optional macro defs.
     *
     * @throws Compiler.CompileException On macro compilation exception.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Map<String, MacroDef>> getMacroDefs(
            @Nonnull final String[] macros)
        throws Compiler.CompileException
    {
        if (macros.length == 0) {
            return Optional.empty();
        }

        final Map<String, MacroDef> macroDefs = new HashMap<String, MacroDef>(
            KeyedValues.hashCapacity(macros.length));

        for (final String macro: macros) {
            final MacroDef macroDef = new MacroDef(macro);

            macroDefs.put(macroDef.getKey(), macroDef);
        }

        return Optional.of(macroDefs);
    }

    /**
     * Gets the word definitions from their definition texts.
     *
     * @param words The word definition texts.
     * @param macroDefs Optional macro instruction definitions.
     * @param wordDefs Optional additional word definitions.
     * @param logger The Logger.
     *
     * @return The word definitions (may be empty).
     *
     * @throws Compiler.CompileException On program compilation exception.
     */
    @Nonnull
    @CheckReturnValue
    Map<String, Program> getWordDefs(
            @Nonnull final String[] words,
            @Nonnull final Optional<Map<String, MacroDef>> macroDefs,
            @Nonnull Optional<Map<String, Program>> wordDefs,
            @Nonnull final Logger logger)
        throws Compiler.CompileException
    {
        final Map<String, Program> defs = new HashMap<String, Program>(
            KeyedValues.hashCapacity(words.length));

        wordDefs = Optional
            .of(
                new HashMap<String, Program>(
                    wordDefs.isPresent()? wordDefs.get(): defs));

        for (String word: words) {
            word = word.trim();

            if (word.isEmpty()) {
                throw new Compiler.CompileException(
                    ProcessorMessages.EMPTY_WORD);
            }

            final int spaceIndex = word.indexOf(' ');
            final String name = ((spaceIndex > 0)? word
                .substring(0, spaceIndex): word)
                .toUpperCase(Locale.ROOT);

            if (_compiler.getRegistrations().get(name) != null) {
                throw new Compiler.CompileException(
                    ProcessorMessages.REGISTERED_WORD_NAME,
                    name);
            }

            final String source = (spaceIndex > 0)? word
                .substring(spaceIndex)
                .trim(): "";
            final Program def = compile(source, macroDefs, wordDefs, logger);

            if (def == null) {
                throw new Compiler.CompileException(
                    ProcessorMessages.WORD_COMPILATION_FAILED,
                    name);
            }

            if ((defs.put(name, def) != null)
                    || (wordDefs.get().put(name, def) != null)) {
                getThisLogger()
                    .warn(ProcessorMessages.MULTIPLE_DEF_RPN_WORD, name);
            }
        }

        return defs;
    }

    /** Default loop limit. */
    public static final int DEFAULT_LOOP_LIMIT = 999;

    /**  */

    private static final Optional<ClassDef> _DEFAULT_BEHAVIOR = Optional
        .of(new ClassDefImpl(Synchronized.class));

    private Compiler _compiler;
    private Optional<Map<String, MacroDef>> _macroDefs = Optional.empty();
    private Optional<Map<String, Program>> _wordDefs = Optional.empty();
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

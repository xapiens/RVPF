/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Compiler.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Params;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.tool.Require;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.operation.BasicOperations;
import org.rvpf.processor.engine.rpn.operation.OperandOperation;
import org.rvpf.processor.engine.rpn.operation.Operation;
import org.rvpf.processor.engine.rpn.operation.OperationReference;
import org.rvpf.processor.engine.rpn.operation.Operations;

/**
 * Compiles RPN programs for an RPN engine.
 *
 * <h1>EBNF</h1>
 *
 * <ul>
 *   <li>words = word, {spaces, word};</li>
 *   <li>word = visible-ascii, {visible-ascii};</li>
 *   <li>spaces = space, {[forth-comment | c-comment], space};</li>
 *   <li>c-comment = '/', '*', {any - ('*', '/')}, '*', '/';</li>
 *   <li>forth-comment = '(', space, {any - ')' }, ')';</li>
 *   <li>word-def= word-name, spaces, program;</li>
 *   <li>inline-word-def= ':', spaces, word-def, spaces, ';';</li>
 *   <li>program = {inline-word-def, spaces}, [';', spaces], words;</li>
 * </ul>
 *
 * @see RPNEngine
 * @see MacroDef
 */
public final class Compiler
{
    /**
     * Creates a new compiler.
     *
     * @param params The engine's parameters.
     *
     * @throws Operation.OverloadException From {@link Operations#setUp}.
     */
    Compiler(@Nonnull final Params params)
        throws Operation.OverloadException
    {
        final BasicOperations operations = new BasicOperations();

        _call = new OperandOperation(
            "CALL",
            operations,
            BasicOperations.CALL_CODE);
        _constantValue = new OperandOperation(
            "CONSTANT_VALUE",
            operations,
            BasicOperations.CONSTANT_VALUE_CODE);
        _dupInputStore = new OperandOperation(
            "DUP_INPUT_STORE",
            operations,
            BasicOperations.DUP_INPUT_STORE_CODE);
        _dupMemoryStore = new OperandOperation(
            "DUP_MEMORY_STORE",
            operations,
            BasicOperations.DUP_MEMORY_STORE_CODE);
        _inputPoint = new OperandOperation(
            "INPUT_POINT",
            operations,
            BasicOperations.INPUT_POINT_CODE);
        _inputPresent = new OperandOperation(
            "INPUT_PRESENT",
            operations,
            BasicOperations.INPUT_VALUE_Q_CODE);
        _inputRequired = new OperandOperation(
            "INPUT_REQUIRED",
            operations,
            BasicOperations.INPUT_VALUE_REQ_CODE);
        _inputStamp = new OperandOperation(
            "INPUT_STAMP",
            operations,
            BasicOperations.INPUT_STAMP_CODE);
        _inputState = new OperandOperation(
            "INPUT_STATE",
            operations,
            BasicOperations.INPUT_STATE_CODE);
        _inputStore = new OperandOperation(
            "INPUT_STORE",
            operations,
            BasicOperations.INPUT_VALUE_STORE_CODE);
        _inputValue = new OperandOperation(
            "INPUT_VALUE",
            operations,
            BasicOperations.INPUT_VALUE_CODE);
        _memoryRequired = new OperandOperation(
            "MEMORY_REQUIRED",
            operations,
            BasicOperations.MEMORY_REQ_CODE);
        _memoryStore = new OperandOperation(
            "MEMORY_STORE",
            operations,
            BasicOperations.MEMORY_STORE_CODE);
        _memoryValue = new OperandOperation(
            "MEMORY_VALUE",
            operations,
            BasicOperations.MEMORY_CODE);
        _paramRequired = new OperandOperation(
            "PARAM_REQUIRED",
            operations,
            BasicOperations.PARAM_REQ_CODE);
        _paramValue = new OperandOperation(
            "PARAM_VALUE",
            operations,
            BasicOperations.PARAM_CODE);

        operations.setUp(_registrations, params);

        _loopLimit = params
            .getInt(RPNEngine.LOOP_LIMIT_PARAM, RPNEngine.DEFAULT_LOOP_LIMIT);
    }

    /**
     * Gets the operand.
     *
     * @return The operand.
     */
    @CheckReturnValue
    public Serializable getOperand()
    {
        return _operand;
    }

    /**
     * Returns the next operation reference.
     *
     * @return The next operation reference (may be empty).
     *
     * @throws CompileException On compilation problem.
     *
     * @see #peekReference
     */
    @Nonnull
    public Optional<OperationReference> nextReference()
        throws CompileException
    {
        final Optional<OperationReference> reference = peekReference();

        _nextReference = null;

        if (reference.isPresent()) {
            reference.get().setUp(this);
        }

        return reference;
    }

    /**
     * Peeks at the next operation reference.
     *
     * <p>The returned reference has not been set up yet. It can be queried for
     * its position in the source and its operation. All calls to
     * {@link #nextReference} or {@link #peekReference} must be made during the
     * reference set up.</p>
     *
     * @return The next operation reference (may be empty).
     *
     * @throws CompileException On compilation problem.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<OperationReference> peekReference()
        throws CompileException
    {
        Require
            .success(
                !_busy,
                "Reference building allowed only during setUp call");

        if (_nextReference == null) {
            _nextReference = _buildReference();
        }

        return Optional.ofNullable(_nextReference);
    }

    /**
     * Compiles a program.
     *
     * @param source The source for the program.
     * @param macroDefs Optional macro instruction definitions.
     * @param wordDefs Optional additional word definitions.
     * @param logger The error logger.
     *
     * @return The compiled Program (null on failure).
     */
    @Nullable
    @CheckReturnValue
    Program compile(
            @Nonnull final String source,
            @Nonnull final Optional<Map<String, MacroDef>> macroDefs,
            @Nonnull final Optional<Map<String, Program>> wordDefs,
            @Nonnull final Logger logger)
    {
        final Program compiledProgram;

        _wordDefs = wordDefs;

        try {
            _tokenizer = new Tokenizer()
                .initialize(source, macroDefs, _loopLimit);
            _compileWordDefs();
            _compileWords();

            if (_tokenizer.peek().isPresent()) {
                throw new CompileException(
                    ProcessorMessages.MISPLACED_END_WORD,
                    _END_DEF);
            }
        } catch (final CompileException exception) {
            logger.error(BaseMessages.VERBATIM, exception.getMessage());
            _program = null;
        }

        compiledProgram = _program;

        _program = null;
        _tokenizer = null;
        _operand = null;
        _wordDefs = Optional.empty();

        return compiledProgram;
    }

    /**
     * Gets the operation registrations.
     *
     * @return The operation registrations.
     */
    @Nonnull
    @CheckReturnValue
    Map<String, Operation> getRegistrations()
    {
        return _registrations;
    }

    private OperationReference _buildOtherReference(
            final Token.OtherName token)
        throws CompileException
    {
        final String word = token.toString().toUpperCase(Locale.ROOT);
        final Operation operation = _registrations.get(word);
        final OperationReference reference;

        if (operation != null) {
            reference = _newReference(operation);
        } else {
            _operand = _wordDefs.isPresent()? _wordDefs.get().get(word): null;

            if (_operand != null) {
                reference = _newReference(_call);
            } else {
                throw new CompileException(
                    ProcessorMessages.UNKNOWN_RPN_WORD,
                    word);
            }
        }

        return reference;
    }

    private OperationReference _buildReference()
        throws CompileException
    {
        if (_program == null) {
            return null;
        }

        final Token token = _tokenizer.nextToken().orElse(null);

        if (token == null) {
            return null;
        }

        final OperationReference reference;

        _busy = true;    // Protects against recursive calls.

        try {
            if (token.isVariableActionName()) {
                reference = _buildVariableActionReference(
                    (Token.VariableActionName) token);
            } else if (token.isOtherName()) {
                if (_END_DEF.equals(token)) {
                    return null;
                }

                reference = _buildOtherReference((Token.OtherName) token);
            } else if (token.isConstant()) {
                _operand = ((Token.Constant) token).getValue();
                reference = _newReference(_constantValue);
            } else {
                throw new InternalError("Unexpected Token category");
            }
        } finally {
            _busy = false;
        }

        return reference;
    }

    private OperationReference _buildVariableActionReference(
            final Token.VariableActionName token)
        throws CompileException
    {
        final Integer identifier = token.getIdentifier();
        final int index = identifier.intValue() - 1;
        final char type = token.getType();
        final char action = token.getAction();
        final boolean dup = token.getDup();
        final OperationReference reference;

        _operand = identifier;

        if ((index < 0) || (index >= _loopLimit)) {
            reference = null;
        } else if (type == Tokenizer.INPUT_TYPE) {
            if (dup && (action != Tokenizer.STORE_ACTION)) {
                reference = null;
            } else if (action == Tokenizer.VALUE_ACTION) {
                reference = _newReference(_inputValue);
            } else if (action == Tokenizer.REQUIRED_ACTION) {
                reference = _newReference(_inputRequired);
            } else if (action == Tokenizer.STAMP_ACTION) {
                reference = _newReference(_inputStamp);
            } else if (action == Tokenizer.STATE_ACTION) {
                reference = _newReference(_inputState);
            } else if (action == Tokenizer.PRESENT_ACTION) {
                reference = _newReference(_inputPresent);
            } else if (action == Tokenizer.STORE_ACTION) {
                reference = _newReference(dup? _dupInputStore: _inputStore);
            } else if (action == Tokenizer.POINT_ACTION) {
                reference = _newReference(_inputPoint);
            } else {
                reference = null;
            }
        } else if (type == Tokenizer.MEMORY_TYPE) {
            if (dup && (action != Tokenizer.STORE_ACTION)) {
                reference = null;
            } else if (action == Tokenizer.VALUE_ACTION) {
                reference = _newReference(_memoryValue);
            } else if (action == Tokenizer.REQUIRED_ACTION) {
                reference = _newReference(_memoryRequired);
            } else if (action == Tokenizer.STORE_ACTION) {
                reference = _newReference(dup? _dupMemoryStore: _memoryStore);
            } else {
                reference = null;
            }
        } else if (type == Tokenizer.PARAM_TYPE) {
            if (dup) {
                reference = null;
            } else if (action == Tokenizer.VALUE_ACTION) {
                reference = _newReference(_paramValue);
            } else if (action == Tokenizer.REQUIRED_ACTION) {
                reference = _newReference(_paramRequired);
            } else {
                reference = null;
            }
        } else {
            reference = null;
        }

        if (reference == null) {
            throw new CompileException(ProcessorMessages.VARIABLE_FORM, token);
        }

        return reference;
    }

    private void _compileWordDefs()
        throws CompileException
    {
        Map<String, Program> wordDefs = null;

        while (_BEGIN_DEF.equals(_tokenizer.peek().orElse(null))) {
            _tokenizer.nextToken();

            final Optional<Token> token = _tokenizer.nextToken();

            if (!token.isPresent() || !token.get().isOtherName()) {
                throw new CompileException(ProcessorMessages.MISSING_WORD_NAME);
            }

            final String name = token.get().toString().toUpperCase(Locale.ROOT);

            if (_registrations.get(name) != null) {
                throw new CompileException(
                    ProcessorMessages.REGISTERED_WORD_NAME,
                    name);
            }

            _compileWords();

            if (wordDefs == null) {
                wordDefs = new HashMap<>(_wordDefs.get());
            }

            wordDefs.put(name, _program);
            _program = null;
        }

        if (wordDefs != null) {
            _wordDefs = Optional.of(wordDefs);
        }

        if (_END_DEF.equals(_tokenizer.peek().orElse(null))) {
            _tokenizer.nextToken();
        }
    }

    private void _compileWords()
        throws CompileException
    {
        _program = new Program();

        for (;;) {
            final Optional<OperationReference> reference = nextReference();

            if (!reference.isPresent()) {
                _program.freeze();

                break;
            }

            _program.add(reference.get());
        }
    }

    private OperationReference _newReference(
            final Operation operation)
        throws CompileException
    {
        final OperationReference reference = operation.newReference(this);

        reference.setPosition(_tokenizer.getPosition());

        return reference;
    }

    private static final Token.OtherName _BEGIN_DEF = new Token.OtherName(":");
    private static final Token.OtherName _END_DEF = new Token.OtherName(";");

    private boolean _busy;
    private final Operation _call;
    private final Operation _constantValue;
    private final Operation _dupInputStore;
    private final Operation _dupMemoryStore;
    private final Operation _inputPoint;
    private final Operation _inputPresent;
    private final Operation _inputRequired;
    private final Operation _inputStamp;
    private final Operation _inputState;
    private final Operation _inputStore;
    private final Operation _inputValue;
    private final int _loopLimit;
    private final Operation _memoryRequired;
    private final Operation _memoryStore;
    private final Operation _memoryValue;
    private OperationReference _nextReference;
    private Serializable _operand;
    private final Operation _paramRequired;
    private final Operation _paramValue;
    private Program _program;
    private final Map<String, Operation> _registrations = new HashMap<>();
    private Tokenizer _tokenizer;
    private Optional<Map<String, Program>> _wordDefs = Optional.empty();

    /**
     * Compile Exception.
     */
    public static final class CompileException
        extends Exception
    {
        /**
         * Constructs an instance.
         *
         * @param entry The messages entry.
         * @param params The message parameters.
         */
        public CompileException(
                @Nonnull final Messages.Entry entry,
                @Nonnull final Object... params)
        {
            super(Message.format(entry, params));
        }

        private static final long serialVersionUID = 1L;
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

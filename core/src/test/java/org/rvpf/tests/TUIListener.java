/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TUIListener.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.tests;

import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Coder;
import org.rvpf.base.tool.Require;
import org.rvpf.tests.FrameworkTests.Test;

import org.testng.ISuite;
import org.testng.ITestResult;

/**
 * TUI listener.
 */
final class TUIListener
    extends AbstractListener
{
    /**
     * Constructs an instance.
     *
     * @param tests The tests.
     * @param verbose Asks for verbose output.
     * @param haltOnFailure Halt on failure.
     */
    TUIListener(
            @Nonnull final Test tests,
            final boolean verbose,
            final boolean haltOnFailure)
    {
        super(tests, haltOnFailure);

        _verbose = verbose;

        Charset charset = Charset.defaultCharset();

        if ((charset != null) && "windows-1252".equals(charset.name())) {
            charset = Charset.forName("IBM850");
        } else {
            final String term = System.getenv("TERM");

            if ("cygwin".equals(term)) {
                charset = Charset.forName("IBM850");
            } else if ((charset != null) && "US-ASCII".equals(charset.name())) {
                charset = Charset.forName("ISO-8859-1");
            } else if (charset == null) {
                charset = StandardCharsets.UTF_8;
            }
        }

        _coder = new Coder(charset);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onConfigurationFailure(final ITestResult result)
    {
        if (!isSkip(result.getThrowable())) {
            super.onConfigurationFailure(result);

            onFailure();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void onFinish(final ISuite suite)
    {
        super.onFinish(suite);

        if (!_verbose) {
            _println();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestFailedButWithinSuccessPercentage(final ITestResult result)
    {
        super.onTestFailedButWithinSuccessPercentage(result);

        _outputResult(result, '?', TestsMessages.TEST_QUALIFIED_RESULT);
        onFailure();
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestFailure(final ITestResult result)
    {
        super.onTestFailure(result);

        _outputResult(result, '!', TestsMessages.TEST_FAILED_RESULT);
        onFailure();
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestSkipped(final ITestResult result)
    {
        super.onTestSkipped(result);

        _outputIdent(result);
        _outputResult(result, '-', TestsMessages.TEST_SKIPPED_RESULT);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestStart(final ITestResult result)
    {
        super.onTestStart(result);

        _outputIdent(result);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestSuccess(final ITestResult result)
    {
        super.onTestSuccess(result);

        _outputResult(result, '=', TestsMessages.TEST_SUCCEEDED_RESULT);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void onFailure()
    {
        final boolean hasFailure = hasFailure();

        try {
            super.onFailure();
        } catch (final FrameworkTests.FailureException exception) {
            if (!hasFailure) {
                if (!_verbose) {
                    _println();
                }

                _println(
                    Message
                        .format(
                            TestsMessages.HALT_ON_FAILURE,
                            exception.getMessage()));
            }

            throw exception;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    void waitForClose()
    {
        _println(
            new Message(
                TestsMessages.TESTS_TIME,
                String.valueOf(getElapsedTime())));
    }

    private static void _println()
    {
        System.out.println();
    }

    private void _outputIdent(final ITestResult result)
    {
        if (_verbose && !_identified) {
            _print(getTestName(result) + ": ");
            _identified = true;
        }
    }

    private void _outputResult(
            final ITestResult result,
            final char briefStatus,
            final Object verboseStatus)
    {
        if (_verbose) {
            _println(verboseStatus);
            _identified = false;
        } else {
            if (_dots >= _DOTS_WIDTH) {
                _println();
                _dots = 0;
            }

            _print(String.valueOf(briefStatus));
            ++_dots;
        }
    }

    private void _print(final Object object)
    {
        final byte[] encoded = Require
            .notNull(_coder.encode(object.toString()));

        try {
            System.out.write(encoded);
        } catch (final IOException exception) {
            throw new RuntimeException(exception);    // Should not happen.
        }
    }

    private void _println(final Object object)
    {
        _print(object);
        _println();
    }

    private static final int _DOTS_WIDTH = 47;

    private final Coder _coder;
    private int _dots;
    private boolean _identified;
    private final boolean _verbose;
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

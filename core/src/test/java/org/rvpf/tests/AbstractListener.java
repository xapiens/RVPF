/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AbstractListener.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests;

import java.util.Arrays;
import java.util.Locale;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Logger.LogLevel;
import org.rvpf.base.tool.Require;
import org.rvpf.tests.FrameworkTests.Test;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.TestListenerAdapter;

/**
 * Abstract listener.
 */
abstract class AbstractListener
    extends TestListenerAdapter
    implements ISuiteListener
{
    /**
     * Constructs an abstract listener.
     *
     * @param tests The tests.
     * @param haltOnFailure Halt on failure.
     */
    AbstractListener(@Nonnull final Test tests, final boolean haltOnFailure)
    {
        _tests = Require.notNull(tests);
        _haltOnFailure = haltOnFailure;
        _startTime = System.currentTimeMillis();
    }

    /** {@inheritDoc}
     */
    @Override
    public void onConfigurationFailure(final ITestResult result)
    {
        getThisLogger()
            .warn(
                result.getThrowable(),
                TestsMessages.METHOD_MESSAGE,
                _CONFIGURATION_TYPE,
                result.getMethod(),
                _FAILED_STATE);

        super.onConfigurationFailure(result);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onFinish(final ISuite suite)
    {
        if (_suiteName != null) {    // Protects against redundant call.
            Require.success(suite.getName().equals(_suiteName));
            getThisLogger().info(TestsMessages.SUITE_ENDS, _suiteName);
            _suiteName = null;
            _context = null;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void onFinish(final ITestContext context)
    {
        Require.success(context.getName().equals(_testName));

        getThisLogger().log(LogLevel.INFO, _TEST_MARKER);

        getThisLogger().info(TestsMessages.TEST_ENDS, _testName);
        _testName = null;
        _context = _suiteName;

        super.onFinish(context);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onStart(final ISuite suite)
    {
        if (_suiteName == null) {    // Protects against redundant call.
            _suiteName = suite.getName();
            getThisLogger().info(TestsMessages.SUITE_BEGINS, _suiteName);
            _context = _suiteName;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void onStart(final ITestContext context)
    {
        _testName = context.getName();
        getThisLogger().info(TestsMessages.TEST_BEGINS, _testName);

        getThisLogger().log(LogLevel.INFO, _TEST_MARKER);

        _context = _testName;

        super.onStart(context);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestFailedButWithinSuccessPercentage(final ITestResult result)
    {
        getThisLogger().log(LogLevel.INFO, _QUALIFIED_MARKER);

        getThisLogger()
            .info(
                result.getThrowable(),
                TestsMessages.METHOD_MESSAGE,
                _TEST_TYPE,
                result.getMethod(),
                _QUALIFIED_STATE);

        super.onTestFailedButWithinSuccessPercentage(result);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestFailure(final ITestResult result)
    {
        getThisLogger().log(LogLevel.INFO, _FAILED_MARKER);

        getThisLogger()
            .warn(
                result.getThrowable(),
                TestsMessages.METHOD_MESSAGE,
                _TEST_TYPE,
                result.getMethod(),
                _FAILED_STATE);

        super.onTestFailure(result);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestSkipped(final ITestResult result)
    {
        Require.success(isSkip(result.getThrowable()));
        getThisLogger()
            .info(
                TestsMessages.METHOD_MESSAGE,
                _TEST_TYPE,
                result.getMethod(),
                _SKIPPED_STATE);

        getThisLogger().log(LogLevel.INFO, _SKIPPED_MARKER);

        super.onTestSkipped(result);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestStart(final ITestResult result)
    {
        getThisLogger()
            .info(
                TestsMessages.METHOD_MESSAGE,
                _TEST_TYPE,
                result.getMethod(),
                _BEGINS_STATE);

        getThisLogger().log(LogLevel.INFO, _METHOD_MARKER);

        super.onTestStart(result);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestSuccess(final ITestResult result)
    {
        getThisLogger().log(LogLevel.INFO, _SUCCESS_MARKER);

        getThisLogger()
            .info(
                TestsMessages.METHOD_MESSAGE,
                _TEST_TYPE,
                result.getMethod(),
                _SUCCEEDED_STATE);

        super.onTestSuccess(result);
    }

    /**
     * Gets the elapsed time.
     *
     * @return The elapsed time.
     */
    @CheckReturnValue
    protected float getElapsedTime()
    {
        return (System.currentTimeMillis() - _startTime) / 1000.0f;
    }

    /**
     * Gets the test name for a test result.
     *
     * @param result The test result.
     *
     * @return The test name.
     */
    @Nonnull
    @CheckReturnValue
    protected String getTestName(@Nonnull final ITestResult result)
    {
        final StringBuilder stringBuilder = new StringBuilder();
        final Object instance = result.getInstance();

        if (instance instanceof ITest) {
            stringBuilder.append(((ITest) instance).getTestName());
        } else {
            final ITestNGMethod method = result.getMethod();
            final Class<?> methodClass = (method != null)? method
                .getConstructorOrMethod()
                .getDeclaringClass(): null;

            stringBuilder.append(_testName);
            stringBuilder.append(':');
            stringBuilder
                .append(
                    (methodClass != null)? methodClass.getSimpleName(): "?");
            stringBuilder.append(':');
            stringBuilder.append((method != null)? method.getMethodName(): "?");
        }

        return stringBuilder.toString();
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
        Logger logger = _logger;

        if (logger == null) {
            logger = Logger.getInstance(getClass());
            _logger = logger;
        }

        return logger;
    }

    /**
     * Asks if there has been at least one failure.
     *
     * @return True if there has been at least one failure.
     */
    @CheckReturnValue
    protected boolean hasFailure()
    {
        return _tests.hasFailure();
    }

    /**
     * Asks if the throwable asks for a skip.
     *
     * @param throwable The throwable.
     *
     * @return True if the throwable asks for a skip.
     */
    @CheckReturnValue
    protected boolean isSkip(@Nonnull final Throwable throwable)
    {
        if ((throwable instanceof SkipException)
                && ((SkipException) throwable).isSkip()) {
            getThisLogger().info(BaseMessages.VERBATIM, throwable.getMessage());

            return true;
        }

        return false;
    }

    /**
     * Called on failure.
     */
    protected void onFailure()
    {
        _tests.onFailure();

        if (_haltOnFailure) {
            throw new FrameworkTests.FailureException(_context);
        }
    }

    /**
     * Waits for a close event.
     *
     * @throws InterruptedException When interrupted.
     */
    void waitForClose()
        throws InterruptedException {}

    private static final String _BEGINS_STATE = TestsMessages.TEST_BEGINS_STATE
        .toString();
    private static final String _CONFIGURATION_TYPE =
        TestsMessages.CONFIGURATION
            .toString();
    private static final String _FAILED_MARKER;
    private static final String _FAILED_STATE = TestsMessages.TEST_FAILED_RESULT
        .toString()
        .toLowerCase(Locale.getDefault());
    private static final String _METHOD_MARKER;
    private static final String _QUALIFIED_MARKER;
    private static final String _QUALIFIED_STATE =
        TestsMessages.TEST_QUALIFIED_RESULT
            .toString()
            .toLowerCase(Locale.getDefault());
    private static final String _SKIPPED_MARKER;
    private static final String _SKIPPED_STATE =
        TestsMessages.TEST_SKIPPED_RESULT
            .toString()
            .toLowerCase(Locale.getDefault());
    private static final String _SUCCEEDED_STATE =
        TestsMessages.TEST_SUCCEEDED_RESULT
            .toString()
            .toLowerCase(Locale.getDefault());
    private static final String _SUCCESS_MARKER;
    private static final String _TEST_MARKER;
    private static final String _TEST_TYPE = TestsMessages.TEST.toString();

    static {
        final char[] marker = new char[128];

        Arrays.fill(marker, '*');
        _TEST_MARKER = new String(marker);

        Arrays.fill(marker, '+');
        _METHOD_MARKER = new String(marker);

        Arrays.fill(marker, '=');
        _SUCCESS_MARKER = new String(marker);

        Arrays.fill(marker, '-');
        _SKIPPED_MARKER = new String(marker);

        Arrays.fill(marker, '?');
        _QUALIFIED_MARKER = new String(marker);

        Arrays.fill(marker, '!');
        _FAILED_MARKER = new String(marker);
    }

    private String _context;
    private final boolean _haltOnFailure;
    private volatile Logger _logger;
    private final long _startTime;
    private String _suiteName;
    private String _testName;
    private final Test _tests;
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: GUIListener.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.tests;

import javax.annotation.Nonnull;

import org.rvpf.tests.FrameworkTests.Test;

import org.testng.ISuite;
import org.testng.ITestResult;

/**
 * GUI listener.
 */
final class GUIListener
    extends AbstractListener
{
    /**
     * Constructs an instance.
     *
     * @param tests The tests.
     * @param haltOnFailure Halt on failure.
     */
    GUIListener(@Nonnull final Test tests, final boolean haltOnFailure)
    {
        super(tests, haltOnFailure);

        _dialog = new GUIListenerFrame(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onConfigurationFailure(final ITestResult result)
    {
        if (!isSkip(result.getThrowable())) {
            super.onConfigurationFailure(result);

            _dialog.onConfigurationFailure();
            onFailure();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void onStart(final ISuite suite)
    {
        super.onStart(suite);

        _dialog.onSuiteStart(suite.getName());
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestFailedButWithinSuccessPercentage(final ITestResult result)
    {
        super.onTestFailedButWithinSuccessPercentage(result);

        _dialog.onTestResult(State.QUALIFIED, getElapsedTime());
        onFailure();
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestFailure(final ITestResult result)
    {
        super.onTestFailure(result);

        _dialog.onTestResult(State.FAILED, getElapsedTime());
        onFailure();
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestSkipped(final ITestResult result)
    {
        super.onTestSkipped(result);

        _dialog.onTestStart(getTestName(result));
        _dialog.onTestResult(State.SKIPPED, getElapsedTime());
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestStart(final ITestResult result)
    {
        super.onTestStart(result);

        _dialog.onTestStart(getTestName(result));
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTestSuccess(final ITestResult result)
    {
        super.onTestSuccess(result);

        _dialog.onTestResult(State.SUCCEEDED, getElapsedTime());
    }

    /** {@inheritDoc}
     *
     * @throws InterruptedException
     */
    @Override
    public synchronized void waitForClose()
        throws InterruptedException
    {
        _done = true;

        _dialog.onTestsDone();

        while (!_closed) {
            wait();
        }

        _dialog = null;
    }

    /**
     * Informs that the dialog is closed.
     */
    synchronized void closed()
    {
        if (!_closed) {
            _closed = true;
            notifyAll();

            if (!_done) {
                System.exit(-1);
            }
        }
    }

    private boolean _closed;
    private GUIListenerFrame _dialog;
    private boolean _done;

    /**
     * States.
     */
    enum State
    {
        SUCCEEDED,
        QUALIFIED,
        SKIPPED,
        FAILED;
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TestsMessages.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.tests;

import org.rvpf.base.logger.Messages;

/**
 * Core tests messages.
 */
public enum TestsMessages
    implements Messages.Entry
{
    CONFIGURATION,
    END,
    EXIT_ON_EXCEPTION,
    HALT_ON_FAILURE,
    KNOWN_TESTS,
    METHOD_MESSAGE,
    NO_TEST_FOUND,
    RECEIVED_EXPECTED_ALERT,
    RECEIVED_EXPECTED_EVENT,
    RECEIVED_EXPECTED_SIGNAL,
    RECEIVED_NULL_ALERT,
    SERVICE_STOP_FAILED,
    SUITE_BEGINS,
    SUITE_ENDS,
    SYSTEM_PROPERTY_CLEARED,
    SYSTEM_PROPERTY_RESTORED,
    SYSTEM_PROPERTY_SET,
    TEST,
    TEST_BEGINS,
    TEST_BEGINS_STATE,
    TEST_ENDS,
    TEST_FAILED,
    TEST_FAILED_RESULT,
    TEST_NG_EXCEPTION,
    TEST_NG_VERSION,
    TEST_NOT_FOUND,
    TEST_QUALIFIED,
    TEST_QUALIFIED_RESULT,
    TEST_SKIPPED,
    TEST_SKIPPED_RESULT,
    TEST_SUCCEEDED,
    TEST_SUCCEEDED_RESULT,
    TEST_UNKNOWN_RESULT,
    TESTS_CANCEL,
    TESTS_DONE,
    TESTS_ELAPSED,
    TESTS_TIME,
    TESTS_TIMEOUT,
    TESTS_TOTAL,
    UNSUPPORTED_LANGUAGE,
    VALUE_CLASS;

    /** {@inheritDoc}
     */
    @Override
    public String getBundleName()
    {
        return _BUNDLE_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized String toString()
    {
        if (_string == null) {
            _string = Messages.getString(this);
        }

        return _string;
    }

    private static final String _BUNDLE_NAME = "org.rvpf.tests.messages.tests";

    private String _string;
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

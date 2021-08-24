/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CoreTestsMessages.java 4025 2019-05-25 17:07:14Z SFB $
 */

package org.rvpf.tests.core;

import org.rvpf.base.logger.Messages;

/**
 * Core tests messages.
 */
public enum CoreTestsMessages
    implements Messages.Entry
{
    ADDING_VALUES,
    ALWAYS_TRIGGER_TEST,
    BREAKPOINT_OPPORTUNITY,
    CASTING_CONTEXT,
    CASTING_DIRECTOR,
    CONNECTING_TO_STORE,
    CONNECTION,
    CONNECTION_URL,
    CRYPT_SECURE,
    DATABASE_DIALECT,
    DELETING_VALUES,
    DRIVER_CLASS,
    DRIVER_VERSION,
    EXPECTED,
    EXPORTED,
    GENERATING_METADATA,
    INSERTING_VALUES,
    LOADING_VALUES,
    METADATA_MISMATCH,
    MIDNIGHT_TRIGGER_TEST,
    NATIVE_NOT_FOUND,
    NOT_REALLY_MIDNIGHT,
    NOW,
    POINT_UNKNOWN,
    PULLING_ALL,
    PULLING_ALL_NO_DELETED,
    PULLING_PURGED,
    PURGED_VALUES,
    PURGING_ALL,
    QUERYING_ALL,
    QUERYING_COUNT,
    QUERYING_EACH,
    QUERYING_LAST,
    QUERYING_POLATED,
    QUERYING_PURGED,
    READ,
    RECEIVED,
    REQUEST,
    RESPONSE,
    SCENARIO_CONTEXT,
    SCENARIO_NOTICES,
    SCENE_CONTEXT,
    SCRIPT_CONTEXT,
    SENDING_VALUES,
    SENT_VALUES,
    SKIPPED_NO_COUNT,
    SKIPPED_NO_PULL,
    SKIPPED_SNAPSHOT,
    STARTING_STORE,
    STORE_WITHOUT_ARCHIVER,
    SUPPORTS_PULL,
    TIME,
    UPDATING_FOURTH,
    USES_MAC,
    VALUES_KEPT,
    VERSION_CONTROL_NOT_ENABLED,
    XML;

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

    private static final String _BUNDLE_NAME = "org.rvpf.tests.messages.core";

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

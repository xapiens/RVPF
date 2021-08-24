/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: HTTPMessages.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http;

import org.rvpf.base.logger.Messages;

/**
 * HTTP messages.
 */
public enum HTTPMessages
    implements Messages.Entry
{
    ADDING_LISTENER,
    ADDING_REALMS,
    ATTRIBUTE_CONFLICT,
    AUTHENTICATION,
    BAD_METADATA,
    BAD_MODULE_UUID,
    BAD_RESOURCE_URL,
    BAD_UUID,
    CONTENT_CONFIG,
    CONTENT_INAPPROPRIATE,
    CONTENT_UNKNOWN,
    CONTEXT_LOADED_MODULE,
    DECRYPTION_KEY,
    DOCUMENT_PARSE_FAILED,
    ENCRYPTION_KEY,
    ENGINE_CONFIG,
    EVENTS_LIMIT,
    GOOD_METADATA,
    LOADING_METADATA,
    MESSAGE,
    METADATA_FILE,
    MISSING_ELEMENT,
    MISSING_ELEMENT_ATTRIBUTE,
    MISSING_POINT_REFERENCE,
    MISSING_REQUEST_PARAMETER,
    NO_CONTEXT,
    NO_CONTEXT_CONTENT,
    NO_CONTEXT_PATH,
    NO_REALM_NAME,
    PROCESSOR_JAR_REQUIRED,
    QUEUED_SIGNAL,
    REQUEST_RECEIVED,
    RESOURCE_ACCESS,
    RESTRAINT,
    RESTRAINT_REFUSED,
    SERVER_DESTROY_FAILED,
    SERVER_STOP_FAILED,
    SERVER_STOPPED,
    SIGNING_KEY,
    STOPPING_SERVER,
    STORE_JAR_REQUIRED,
    UNIT_UNKNOWN,
    UNRECOGNIZED_BOOLEAN,
    UNRECOGNIZED_INTEGER,
    UNRECOGNIZED_SYNC_TYPE,
    VERIFICATION_KEY,
    WILL_DECRYPT,
    WILL_ENCRYPT,
    WILL_SIGN,
    WILL_VERIFY;

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

    private static final String _BUNDLE_NAME = "org.rvpf.messages.http";

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

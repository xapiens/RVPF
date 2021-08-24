/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ForwarderMessages.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.forwarder;

import org.rvpf.base.logger.Messages;

/**
 * Forwarder messages.
 */
public enum ForwarderMessages
    implements Messages.Entry
{
    ADD_ALERT_LISTENER_FAILED,
    ADDRESS,
    ALERT_DROPPED,
    AUTHENTICATION_FAILED,
    AUTHORIZED,
    AUTHORIZED_ROLE,
    BAD_CONNECTION,
    BAD_FILTER_UUID,
    BAD_MODULE_UUID,
    BATCH_SIZE_LIMIT,
    BATCH_TIMEOUT,
    BATCH_WAIT,
    CLIENT,
    COMPRESSED_SUFFIX,
    CONFIRM,
    CONFIRM_ALWAYS,
    CONFIRM_MARKED,
    CONFIRM_NEVER,
    CONFIRM_REPLICATED,
    CONNECTION_ACCEPTED,
    CONNECTION_CANCELLED,
    CONNECTION_CLOSE_FAILED,
    CONNECTION_CLOSED,
    CONNECTION_COMPLETED,
    CONNECTION_FAILED,
    CONNECTION_FAILED_SLEEPING,
    CONNECTION_LOST,
    CONTROL_DIRECTORY,
    CONTROL_DIRECTORY_CREATE_FAILED,
    CONTROL_DIRECTORY_CREATED,
    DECODED_VALUE,
    DECRYPTION_KEY,
    DELETED_OLD_DONE_FILE,
    DELIVER_NOT_SUPPORTED,
    DONE_FILE_PREFIX,
    DONE_FILE_SUFFIX,
    DROPPED_NOT_POINT_VALUE,
    DROPPED_UNKNOWN_POINT,
    ENCRYPTION_KEY,
    FAILED_TO_RESPOND,
    FILE_PROCESSED,
    FILTER_PROPERTY_MISSING,
    FILTER_UUID,
    FLUSH_BATCH_LIMIT,
    FLUSH_CLOSE,
    FLUSH_REQUEST,
    FLUSH_TIME_LIMIT,
    FLUSHED_MESSAGES,
    FLUSHED_NOTHING,
    FLUSHING_MESSAGES,
    GET_STREAM_FAILED,
    INPUT_DIRECTORY,
    INPUT_DIRECTORY_CREATE_FAILED,
    INPUT_DIRECTORY_CREATED,
    INPUT_DIRECTORY_OVERFLOW,
    INPUT_FILE_ADDED,
    INPUT_FILE_DISAPPEARED,
    INPUT_FILE_PREFIX,
    INPUT_FILE_SUFFIX,
    INPUT_STORE,
    LISTENING_ON_PORT,
    LOGIN_NEEDED,
    MARK_MISSING,
    MARK_READ_FAILED,
    MESSAGE_CLASS_DROPPED,
    MESSAGE_FROM,
    MESSAGE_TO,
    MODULE_PROPERTY_MISSING,
    MODULE_UUID,
    NO_BATCH_LIMIT,
    NO_MODULES,
    NOT_AUTHORIZED,
    NOTICE_DROPPED,
    NOTICE_REJECTED,
    OUTPUT_BATCH_LIMIT,
    OUTPUT_UNRELIABLE_SIBLING,
    PROCESSING_FILE,
    PULL_NOR_DELIVER,
    PULL_NOT_SUPPORTED,
    PURGED_SEM_FILE,
    QUEUED_MESSAGES,
    REPLICATES,
    RESYNCHRONIZES,
    SCAN_INTERVAL,
    SEM_DIR,
    SEM_DIR_MISSING,
    SEM_FILE_PREFIX,
    SEM_FILE_SUFFIX,
    SEM_MATCH_ENABLED,
    SEM_PURGE_ENABLED,
    SIGNING_KEY,
    SUBSCRIBE_FAILED,
    SUBSCRIBE_UNSUPPORTED,
    UNSUBSCRIBE_CLOSE_FAILED,
    USER_ATTRIBUTE,
    VERIFICATION_FAILED,
    VERIFICATION_KEY;

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

    private static final String _BUNDLE_NAME = "org.rvpf.messages.forwarder";

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

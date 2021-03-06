/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreMessages.java 4024 2019-05-25 15:42:43Z SFB $
 */

package org.rvpf.store.server;

import org.rvpf.base.logger.Messages;

/**
 * Store messages.
 */
public enum StoreMessages
    implements Messages.Entry
{
    ABORTING,
    ALREADY_CONNECTED,
    ARCHIVED_VALUES,
    ARCHIVER_ATTIC_DISABLED,
    ARCHIVER_CLEAN_UP,
    ARCHIVER_QUEUE,
    ARCHIVER_SCHEDULE,
    ARCHIVER_SET_UP,
    ATTIC_CLASS,
    BACK_END,
    BACK_END_LIMIT,
    BACKUP_COMPLETED,
    BACKUP_SCHEDULE,
    BACKUP_STARTED,
    BAD_DRIVER_CLASS,
    BDB_DIR,
    BDB_DIR_CREATED,
    BDB_DIR_CREATION_FAILED,
    BOUND_BY_SERVER,
    BOUND_FROM_METADATA,
    CONNECT_FAILED,
    CONNECTED_DATABASE,
    CONNECTION_OPEN_FAILED,
    CUSTOM_DRIVER_PROPERTY,
    DATA_SOURCE_CLOSED,
    DATABASE_DATA_DIR,
    DELETE_FAILED,
    DELETE_NOTICES,
    DELETED,
    DELIVERED,
    DIALECT_SELECTED,
    DISCONNECTED,
    DROP_DELETED,
    DUPLICATE_PARTNER_NAME,
    ENV,
    EXCEPTION_ON_COUNT,
    EXCEPTION_ON_DELETE,
    EXCEPTION_ON_DELIVER,
    EXCEPTION_ON_READ,
    EXCEPTION_ON_SUBSCRIBE,
    EXCEPTION_ON_UNSUBSCRIBE,
    EXCEPTION_ON_WRITE,
    FAILED_CONNECT,
    FAILED_FORGET,
    FAILED_GET_CONTEXT,
    FAILED_LOAD_LIBRARY,
    FAILED_RESOLVE,
    FAILED_SUBSCRIBE,
    FAILED_UNSUBSCRIBE,
    FORGOTTEN_VALUES,
    IGNORED,
    IGNORED_VALUE,
    IMPERSONATE_BEGINS,
    IMPERSONATE_CONTINUES,
    IMPERSONATE_ENDS,
    IMPERSONATING_NOT_AUTHENTICATED,
    IMPLEMENTATION_LIBRARY,
    INCONSISTENT_CHARSET,
    INCONSISTENT_ENV,
    INSTANCE_LOG_LEVEL,
    KEEP_SINK_PROCESS,
    KEEP_SINK_PROCESS_TEXT,
    LISTENING_UPDATES,
    LOG_LEVEL_UNKNOWN,
    LOST_UPDATE_QUEUE,
    MARKED_VALUE,
    NO_CODE_FOR_QUALITY,
    NO_NAME_FOR_QUALITY,
    NO_POINT_FOR_POLATE,
    NO_STORE,
    NO_SUBSCRIPTIONS,
    NOTICE_QUEUED,
    NOTICES_BATCHES,
    NOTICES_COMMITTED,
    NOTICES_FILTERED,
    NOTICES_SENT,
    NOTICES_STATS,
    NOTICES_UNCOMMITTED,
    NOTIFICATION_TIME,
    NOTIFIER_CLASS,
    NOTIFIER_STARTED,
    NOTIFIER_STOPPED,
    NULL_REMOVES,
    PARTNER_NOT_SPECIFIED,
    POINT_ACTION_UNAUTHORIZED,
    POINT_ALREADY_SUBSCRIBED,
    POINT_NOT_IN_STORE,
    POINT_UNKNOWN,
    POINT_UNNAMED,
    POINT_UPDATE_IGNORED,
    POINT_UUID_INCOMPATIBLE,
    POINT_WAS_NOT_SUBSCRIBED,
    POLATED_INTERVAL,
    POLATED_SYNC,
    POLATOR,
    POLL_INTERVAL,
    POLL_INTERVAL_TEXT,
    PREEMPTING_LIBRARY,
    PULL_DISABLED,
    PULL_SLEEP,
    PULL_SLEEP_TEXT,
    QUERIES_DONE,
    QUERIES_IGNORED,
    QUERIES_RECEIVED,
    QUERY_ANSWER,
    QUERY_DONE,
    QUERY_IS_NOT_PULL,
    QUERY_RECEIVED,
    QUERY_UNAUTHORIZED,
    REASON_DELETED,
    REASON_NULL,
    REASON_SYNC,
    REASON_UNAUTHORIZED,
    RECOVERING_BACKUP_FILES,
    REFRESHING_METADATA,
    REPLICATES_BATCHES,
    REPLICATES_SENT,
    REPLICATES_STATS,
    REPLICATION_TIME,
    RESPONDER_KEEP,
    RESPONSE_TIME,
    RESTORE_COMPLETED,
    RESTORE_STARTED,
    RETRYING_CONNECT,
    ROW_DELETED,
    ROW_INSERTED,
    ROW_REPLACED,
    ROWS_DELETED,
    SCRIPT_DELETE_TEXT,
    SCRIPT_UPDATE_TEXT,
    SELECT_STATEMENTS_LIMIT,
    SENT_REPLICATE,
    SENT_REPLICATE_AS,
    SERVER_ARGS,
    SERVER_PORT,
    SERVER_SUPPORT,
    SERVER_URL,
    SESSIONS_STATS,
    SHARED_CONNECTION,
    SINK,
    SNAPSHOT_MODE,
    SQL,
    SQL_FOR,
    STATE_GROUP_GLOBAL_NAMES,
    STATE_GROUP_LOAD_FAILED,
    STATE_GROUP_UNDEFINED,
    STATE_GROUPS,
    STORAGE_CREATE_FAILED,
    STORAGE_CREATED,
    STORE_DATA_DIR,
    STORE_SET_UP,
    STORE_WILL_NOTIFY,
    SUPPORTED_VALUE_TYPES,
    SUPPORTS_CONNECTIONS,
    SUPPORTS_COUNT,
    SUPPORTS_DELETE,
    SUPPORTS_DELIVER,
    SUPPORTS_PULL,
    SUPPORTS_SUBSCRIBE,
    SUPPORTS_THREADS,
    UNBOUND,
    UNEXPECTED_DELIVERY,
    UNKNOWN,
    UPDATE_COUNT_BAD,
    UPDATE_COUNT_UNKNOWN,
    UPDATE_FAILED,
    UPDATE_NOTICES,
    UPDATE_RECEIVED,
    UPDATE_TIME,
    UPDATED,
    UPDATER_DELETED,
    UPDATER_HIDDEN,
    UPDATER_IGNORED,
    UPDATER_UPDATED,
    UPDATES_BATCHES,
    UPDATES_IGNORED,
    UPDATES_LISTENER,
    UPDATES_RECEIVED,
    UPDATES_STATS,
    USING_DRIVER,
    USING_JDBC_DRIVER,
    USING_SQL_FOR,
    USING_URL,
    VALUES_DELETED,
    VALUES_REMOVED,
    VALUES_SENT,
    VALUES_UPDATED,
    VERSIONS_IN_FUTURE,
    WILL_REPLICATE;

    /** {@inheritDoc}
     */
    @Override
    public String getBundleName()
    {
        return BUNDLE_NAME;
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

    private static final String BUNDLE_NAME = "org.rvpf.messages.store";

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

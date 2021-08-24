/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CStoreVector.h 3961 2019-05-06 20:14:59Z SFB $
 */
#ifndef RVPF_C_STORE_VECTOR_H
#define RVPF_C_STORE_VECTOR_H

#include "CStoreImpl.h"

#include <stdlib.h>
#include <string.h>

// Private CStore implementation forward declarations.

static c_store_code_t CStore_connect(c_store_t cStore);

static c_store_code_t CStore_delete(
    c_store_t cStore,
    size_t count,
    c_store_handle_t *server_handles,
    c_store_stamp_t *time_stamps,
    c_store_code_t *status_codes);

static c_store_code_t CStore_deliver(
    c_store_t cStore,
    size_t limit,
    c_store_millis_t timeout,
    size_t *count,
    c_store_value_t ***values);

static c_store_code_t CStore_disconnect(c_store_t cStore);

static void CStore_dispose(c_store_t cStore);

static c_store_code_t CStore_exchangeHandles(
    c_store_t cStore,
    size_t count,
    char **tag_strings,
    c_store_handle_t *client_handles,
    c_store_handle_t *server_handles,
    c_store_code_t *status_codes);

static void CStore_freeValues(
    c_store_t cStore,
    size_t count,
    c_store_value_t **values);

static c_store_code_t CStore_getQualityCode(
    c_store_t cStore,
    char *qualityName,
    c_store_quality_t *qualityCode);

static char *CStore_getQualityName(
    c_store_t cStore,
    c_store_quality_t qualityCode);

static c_store_code_t CStore_getStateCode(
    c_store_t cStore,
    c_store_handle_t server_handle,
    char *stateyName,
    c_store_quality_t *stateCode);

static char *CStore_getStateName(
    c_store_t cStore,
    c_store_handle_t server_handle,
    c_store_quality_t stateCode);

static c_store_code_t CStore_interrupt(c_store_t cStore);

static c_store_code_t CStore_putEnv(c_store_t cStore, char *entry);

static c_store_code_t CStore_count(
    c_store_t cStore,
    c_store_handle_t server_handle,
    c_store_stamp_t start_time,
    c_store_stamp_t end_time,
    size_t limit,
    c_store_long_t *count);

static c_store_code_t CStore_read(
    c_store_t cStore,
    c_store_handle_t server_handle,
    c_store_stamp_t start_time,
    c_store_stamp_t end_time,
    size_t limit,
    size_t *count,
    c_store_value_t ***values);

static c_store_code_t CStore_releaseHandles(
    c_store_t cStore,
    size_t count,
    c_store_handle_t *server_handles,
    c_store_code_t *status_codes);

static c_store_code_t CStore_subscribe(
    c_store_t cStore,
    size_t count,
    c_store_handle_t *server_handles,
    c_store_code_t *status_codes);

static char *CStore_supportedValueTypeCodes(c_store_t cStore);

static bool CStore_supportsConnections(c_store_t cStore);

static bool CStore_supportsDeliver(c_store_t cStore);

static bool CStore_supportsCount(c_store_t cStore);

static bool CStore_supportsDelete(c_store_t cStore);

static bool CStore_supportsPull(c_store_t cStore);

static bool CStore_supportsSubscribe(c_store_t cStore);

static bool CStore_supportsThreads(c_store_t cStore);

static c_store_code_t CStore_unsubscribe(
    c_store_t cStore,
    size_t count,
    c_store_handle_t *server_handles,
    c_store_code_t *status_codes);

static c_store_code_t CStore_useCharset(c_store_t cStore, char *charsetName);

static c_store_code_t CStore_write(
    c_store_t cStore,
    size_t count,
    c_store_value_t **values,
    c_store_code_t *status_codes);

// Private forward declarations.

static c_store_t CStore_createContext(
    c_store_logger_t logger,
    void *implContext);

static void CStore_disposeContext(c_store_t cStore);

// Private variable definitions.

static struct c_store_vector cStoreVector =
    {
        CStore_useCharset,
        CStore_putEnv,
        CStore_supportsConnections,
        CStore_connect,
        CStore_supportsThreads,
        CStore_exchangeHandles,
        CStore_supportsSubscribe,
        CStore_subscribe,
        CStore_supportsDeliver,
        CStore_deliver,
        CStore_getQualityName,
        CStore_getQualityCode,
        CStore_getStateName,
        CStore_getStateCode,
        CStore_supportsCount,
        CStore_supportsDelete,
        CStore_supportsPull,
        CStore_supportedValueTypeCodes,
        CStore_count,
        CStore_read,
        CStore_freeValues,
        CStore_write,
        CStore_delete,
        CStore_interrupt,
        CStore_unsubscribe,
        CStore_releaseHandles,
        CStore_disconnect,
        CStore_dispose,
    };

// Private function definitions.

static c_store_t CStore_createContext(
    c_store_logger_t logger,
    void *implContext)
{
    LOG_DEBUG(logger, "Creating context");

    c_store_t store = implContext? malloc(sizeof(struct c_store_context)): NULL;

    if (store) {
        store->vector = &cStoreVector;
        store->logger = logger;
        store->context = implContext;
    } else {
        LOG_ERROR(logger, "Failed to allocate context");
    }

    return store;
}

static void CStore_disposeContext(c_store_t cStore)
{
    c_store_logger_t logger = cStore? cStore->logger: NULL;

    memset(cStore, 0, sizeof(struct c_store_context));
    free(cStore);

    LOG_DEBUG(logger, "Disposed of context");
}

#endif /* RVPF_C_STORE_VECTOR_H */

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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

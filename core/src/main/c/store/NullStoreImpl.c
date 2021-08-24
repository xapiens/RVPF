/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: NullStoreImpl.c 3961 2019-05-06 20:14:59Z SFB $
 */
#include "CStoreVector.h"

#include <string.h>

// Private structure definitions.

struct context
{
    char dummy;
};

// Public shareable object function definitions.

RVPF_EXPORT c_store_t RVPF_CStore_context(
    const c_store_logger_t logger,
    const char *vmPath,
    int argc,
    char *argv[],
    void *vm)
{
    struct context *context = CStore_allocate(sizeof(struct context));
    c_store_t store = CStore_createContext(logger, context);

    if (!store) {
        if (context) CStore_free(context);
        CStore_dispose(NULL);
    }

    return store;
}

// Private CStore implementation definitions.

static c_store_code_t CStore_connect(c_store_t cStore)
{
    return STATUS_CODE_SUCCESS;
}

static c_store_code_t CStore_delete(
    c_store_t cStore,
    size_t count,
    c_store_handle_t *server_handles,
    c_store_stamp_t *time_stamps,
    c_store_code_t *status_codes)
{
    return STATUS_CODE_SUCCESS;
}

static c_store_code_t CStore_deliver(
    c_store_t cStore,
    size_t limit,
    c_store_millis_t timeout,
    size_t *count,
    c_store_value_t ***values)
{
    return STATUS_CODE_UNSUPPORTED;
}

static c_store_code_t CStore_disconnect(c_store_t cStore)
{
    return STATUS_CODE_SUCCESS;
}

static void CStore_dispose(c_store_t cStore)
{
    if (cStore) {
        struct context *context = (struct context *) cStore->context;

        CStore_disposeContext(cStore);
        memset(context, 0, sizeof(struct context));
        CStore_free(context);
    }
}

static c_store_code_t CStore_exchangeHandles(
    c_store_t cStore,
    size_t count,
    char **tag_strings,
    c_store_handle_t *client_handles,
    c_store_handle_t *server_handles,
    c_store_code_t *status_codes)
{
    for (int i = 0; i < count; ++i) {
        server_handles[i] = client_handles[i];
    }

    return STATUS_CODE_SUCCESS;
}

static void CStore_freeValues(
    c_store_t cStore,
    size_t count,
    c_store_value_t **values)
{
    for (int i = 0; i < count; ++i) {
        CStore_free(values[i]);
        values[i] = NULL;
    }
    CStore_free(values);
}

static c_store_code_t CStore_getQualityCode(
    c_store_t cStore,
    char *qualityName,
    c_store_quality_t *qualityCode)
{
    return STATUS_CODE_UNSUPPORTED;
}

static char *CStore_getQualityName(
    c_store_t cStore,
    c_store_quality_t qualityCode)
{
    return NULL;
}

static c_store_code_t CStore_getStateCode(
    c_store_t cStore,
    c_store_handle_t server_handle,
    char *stateName,
    c_store_quality_t *stateCode)
{
    return STATUS_CODE_UNSUPPORTED;
}

static char *CStore_getStateName(
    c_store_t cStore,
    c_store_handle_t server_handle,
    c_store_quality_t stateCode)
{
    return NULL;
}

static c_store_code_t CStore_interrupt(c_store_t cStore)
{
    return STATUS_CODE_SUCCESS;
}

static c_store_code_t CStore_putEnv(c_store_t cStore, char *entryString)
{
    return STATUS_CODE_SUCCESS;
}

static c_store_code_t CStore_count(
    c_store_t cStore,
    c_store_handle_t server_handle,
    c_store_stamp_t start_time,
    c_store_stamp_t end_time,
    size_t limit,
    c_store_long_t *count)
{
    *count = 0;

    return STATUS_CODE_SUCCESS;
}

static c_store_code_t CStore_read(
    c_store_t cStore,
    c_store_handle_t server_handle,
    c_store_stamp_t start_time,
    c_store_stamp_t end_time,
    size_t limit,
    size_t *count,
    c_store_value_t ***values)
{
    return STATUS_CODE_SUCCESS;
}

static c_store_code_t CStore_releaseHandles(
    c_store_t cStore,
    size_t count,
    c_store_handle_t *server_handles,
    c_store_code_t *status_codes)
{
    return STATUS_CODE_SUCCESS;
}

static c_store_code_t CStore_subscribe(
    c_store_t cStore,
    size_t count,
    c_store_handle_t *server_handles,
    c_store_code_t *status_codes)
{
    return STATUS_CODE_UNSUPPORTED;
}

static char *CStore_supportedValueTypeCodes(c_store_t cStore)
{
    return "DIRzbacnxdfijm0orsqt";
}

static bool CStore_supportsConnections(c_store_t cStore)
{
    return true;
}

static bool CStore_supportsCount(c_store_t cStore)
{
    return true;
}

static bool CStore_supportsDelete(c_store_t cStore)
{
    return true;
}

static bool CStore_supportsDeliver(c_store_t cStore)
{
    return false;
}

static bool CStore_supportsPull(c_store_t cStore)
{
    return false;
}

static bool CStore_supportsSubscribe(c_store_t cStore)
{
    return false;
}

static bool CStore_supportsThreads(c_store_t cStore)
{
    return true;
}

static c_store_code_t CStore_unsubscribe(
    c_store_t cStore,
    size_t count,
    c_store_handle_t *server_handles,
    c_store_code_t *status_codes)
{
    return STATUS_CODE_UNSUPPORTED;
}

static c_store_code_t CStore_useCharset(c_store_t cStore, char *useCharset)
{
    return STATUS_CODE_SUCCESS;
}

static c_store_code_t CStore_write(
    c_store_t cStore,
    size_t count,
    c_store_value_t **values,
    c_store_code_t *status_codes)
{
    return STATUS_CODE_SUCCESS;
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

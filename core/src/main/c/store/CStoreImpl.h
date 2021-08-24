/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CStoreImpl.h 3961 2019-05-06 20:14:59Z SFB $
 */
#ifndef RVPF_C_STORE_IMPL_H
#define RVPF_C_STORE_IMPL_H

#include "Types.h"

#include <stdarg.h>

#ifdef _WIN32
#define RVPF_EXPORT __declspec(dllexport)
#else
#define RVPF_EXPORT extern
#endif

#define ASSERT(e) ((e)? (void) 0: CStore_assert(__FILE__, __LINE__, #e))

#define LOG_FATAL(logger, ...) CStore_log(logger, LOG_LEVEL_FATAL, __VA_ARGS__)
#define LOG_ERROR(logger, ...) CStore_log(logger, LOG_LEVEL_ERROR, __VA_ARGS__)
#define LOG_WARN(logger, ...) CStore_log(logger, LOG_LEVEL_WARN, __VA_ARGS__)
#define LOG_INFO(logger, ...) CStore_log(logger, LOG_LEVEL_INFO, __VA_ARGS__)
#define LOG_INFO_ENABLED(logger) (LOG_LEVEL_INFO <= logger->level)
#define LOG_DEBUG(logger, ...) CStore_log(logger, LOG_LEVEL_DEBUG, __VA_ARGS__)
#define LOG_DEBUG_ENABLED(logger) (LOG_LEVEL_DEBUG <= logger->level)
#define LOG_TRACE(logger, ...) CStore_log(logger, LOG_LEVEL_TRACE, __VA_ARGS__)
#define LOG_TRACE_ENABLED(logger) (LOG_LEVEL_TRACE <= logger->level)

#define TRACE(...) {fprintf(stderr, __VA_ARGS__); fflush(stderr);}

enum status_code { // Must match status codes in Status.java.
    STATUS_CODE_SUCCESS = 0,
    STATUS_CODE_UNKNOWN = -1001,
    STATUS_CODE_BAD_HANDLE = -1002,
    STATUS_CODE_FAILED = -1003,
    STATUS_CODE_IGNORED = -1004,
    STATUS_CODE_POINT_UNKNOWN = -1005,
    STATUS_CODE_ILLEGAL_STATE = -1006,
    STATUS_CODE_DISCONNECTED = -1007,
    STATUS_CODE_UNSUPPORTED = -1008,
    STATUS_CODE_UNRECOVERABLE = -1009,
};

enum log_level { // Must match Logger.LogLogger levels.
    LOG_LEVEL_NONE = 0,
    LOG_LEVEL_FATAL = 1,
    LOG_LEVEL_ERROR = 2,
    LOG_LEVEL_WARN = 3,
    LOG_LEVEL_INFO = 4,
    LOG_LEVEL_DEBUG = 5,
    LOG_LEVEL_TRACE = 6,
    LOG_LEVEL_ALL = 7,
};

enum value_type { // Must match type codes in Externalizer.java.
    VALUE_TYPE_NULL = '\0',
    VALUE_TYPE_DOUBLE = 'd',
    VALUE_TYPE_LONG = 'j',
    VALUE_TYPE_BOOLEAN = 'z',
    VALUE_TYPE_SHORT = 's',
    VALUE_TYPE_STRING = 't',
    VALUE_TYPE_BYTE_ARRAY = 'a',
    VALUE_TYPE_INTEGER = 'i',
    VALUE_TYPE_FLOAT = 'f',
    VALUE_TYPE_CHARACTER = 'c',
    VALUE_TYPE_BYTE = 'b',
    VALUE_TYPE_STATE = 'q',
    VALUE_TYPE_OBJECT = 'o',
};

typedef struct c_store_logger *c_store_logger_t;

struct c_store_logger
{
    int (*log)(
        c_store_logger_t logger,
        size_t size,
        int level,
        const char *format,
        va_list args);
    int level;
    void *context;
};

typedef struct
{
    c_store_handle_t handle;
    c_store_stamp_t stamp;
    c_store_bool_t deleted;
    c_store_quality_t quality;
    size_t size;
    c_store_byte_t value[];
} c_store_value_t;

typedef struct c_store_context
{
    struct c_store_vector *vector;
    c_store_logger_t logger;
    void *context;
} *c_store_t;

struct c_store_vector
{
    c_store_code_t (*useCharset)(c_store_t cStore, char *charsetName);
    c_store_code_t (*putEnv)(c_store_t cStore, char *entry);
    bool (*supportsConnections)(c_store_t cStore);
    c_store_code_t (*connect)(c_store_t cStore);
    bool (*supportsThreads)(c_store_t cStore);
    c_store_code_t (*exchangeHandles)(
        c_store_t cStore,
        size_t count,
        char **tag_strings,
        c_store_handle_t *client_handles,
        c_store_handle_t *server_handles,
        c_store_code_t *status_codes);
    bool (*supportsSubscribe)(c_store_t cStore);
    c_store_code_t (*subscribe)(
        c_store_t cStore,
        size_t count,
        c_store_handle_t *server_handles,
        c_store_code_t *status_codes);
    bool (*supportsDeliver)(c_store_t cStore);
    c_store_code_t (*deliver)(
        c_store_t cStore,
        size_t limit,
        c_store_millis_t timeout,
        size_t *count,
        c_store_value_t ***values);
    char *(*getQualityName)(c_store_t cStore, c_store_quality_t qualityCode);
    c_store_code_t (*getQualityCode)(
        c_store_t cStore,
        char *qualityName,
        c_store_quality_t *qualityCode);
    char *(*getStateName)(
        c_store_t cStore,
        c_store_handle_t server_handle,
        c_store_quality_t stateCode);
    c_store_code_t (*getStateCode)(
        c_store_t cStore,
        c_store_handle_t server_handle,
        char *stateName,
        c_store_quality_t *stateCode);
    bool (*supportsCount)(c_store_t cStore);
    bool (*supportsDelete)(c_store_t cStore);
    bool (*supportsPull)(c_store_t cStore);
    char *(*supportedValueTypeCodes)(c_store_t cStore);
    c_store_code_t (*count)(
        c_store_t cStore,
        c_store_handle_t server_handle,
        c_store_stamp_t start_time,
        c_store_stamp_t end_time,
        size_t limit,
        c_store_long_t *count);
    c_store_code_t (*read)(
        c_store_t cStore,
        c_store_handle_t server_handle,
        c_store_stamp_t start_time,
        c_store_stamp_t end_time,
        size_t limit,
        size_t *count,
        c_store_value_t ***values);
    void (*freeValues)(
        c_store_t cStore,
        size_t count,
        c_store_value_t **values);
    c_store_code_t (*write)(
        c_store_t cStore,
        size_t count,
        c_store_value_t **values,
        c_store_code_t *status_codes);
    c_store_code_t (*delete)(
        c_store_t cStore,
        size_t count,
        c_store_handle_t *server_handles,
        c_store_stamp_t *time_stamps,
        c_store_code_t *status_codes);
    c_store_code_t (*interrupt)(c_store_t cStore);
    c_store_code_t (*unsubscribe)(
        c_store_t cStore,
        size_t count,
        c_store_handle_t *server_handles,
        c_store_code_t *status_codes);
    c_store_code_t (*releaseHandles)(
        c_store_t cStore,
        size_t count,
        c_store_handle_t *server_handles,
        c_store_code_t *status_codes);
    c_store_code_t (*disconnect)(c_store_t cStore);
    void (*dispose)(c_store_t cStore);
};

typedef c_store_t c_store_context_function_t( // Must match RVPF_CStore_context.
    const c_store_logger_t logger,
    const char *vmPath,
    int argc,
    char *argv[],
    void *vm);

extern c_store_t RVPF_CStore_context( // Must match context_function_t.
    const c_store_logger_t logger,
    const char *vmPath,
    int argc,
    char *argv[],
    void *vm);

extern void *CStore_allocate(size_t size);

extern void CStore_assert(const char *file, int line, const char *message);

extern void CStore_closeLibrary(void *libraryHandle);

extern void CStore_free(void *memory);

extern enum value_type CStore_getValueType(c_store_value_t *storeValue);

extern void CStore_log(
    c_store_logger_t logger,
    int level,
    const char *format,
    ...);

extern c_store_value_t *CStore_newValue(
    c_store_t cStore,
    enum value_type valueType,
    ...);

extern void *CStore_openLibrary(const char *libraryPath);

extern bool CStore_parseBoolEnvValue(
    c_store_t cStore,
    char *value,
    bool defaultValue);

extern char *CStore_parseEnvEntry(c_store_t cStore, char *entryString);

extern void *CStore_resolveSymbol(
    void *libraryHandle,
    const char *symbol);

extern bool CStore_valueToByteArray(
    c_store_value_t *storeValue,
    c_store_byte_t **bytesValue,
    size_t *bytesLength);

extern bool CStore_valueToDouble(
    c_store_value_t *storeValue,
    c_store_double_t *doubleValue);

extern bool CStore_valueToLong(
    c_store_value_t *storeValue,
    c_store_long_t *longValue);

extern bool CStore_valueToStateCode(
    c_store_value_t *storeValue,
    c_store_int_t *stateCode);

extern bool CStore_valueToStateName(
    c_store_value_t *storeValue,
    char **stateName);

extern bool CStore_valueToString(
    c_store_value_t *storeValue,
    char **stringValue);

#endif /* RVPF_C_STORE_IMPL_H */

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

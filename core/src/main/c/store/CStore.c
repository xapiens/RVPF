/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CStore.c 3961 2019-05-06 20:14:59Z SFB $
 */
#include "CStore.h"
#include "ProxyStoreImpl.h"

#include <string.h>
#include <stdint.h>

// Private macro definitions.

#define CONTEXT_FUNCTION_NAME "RVPF_CStore_context"

// Private structure definitions.

struct logger_context
{
    jobject cStoreInstance;
};

// Private variable definitions.

static jclass _atomicLongClass = NULL;

static jmethodID _atomicLongSetMethod = NULL;

static jclass _byteArrayClass = NULL;

static jclass _integerClass = NULL;

static jmethodID _integerValueMethod = NULL;

static JavaVM *_javaVM = NULL;

static jmethodID _logMethod = NULL;

static jclass _valuesClass = NULL;

static struct
{
    jmethodID constructor;
    jmethodID add;
    jmethodID statusCode;
    jmethodID size;
    jmethodID next;
}
_valuesMethods = {};

static struct
{
    jfieldID time;
    jfieldID deleted;
    jfieldID quality;
    jfieldID value;
}
_valuesFields = {};

// Private forward declarations.

static void *_allocate(size_t size);

static jfieldID _getFieldID(
    JNIEnv *env,
    jclass cls,
    const char *name,
    const char *sig,
    c_store_logger_t logger);

static jobject _integerValue(JNIEnv *env,c_store_int_t value);

static void _logBack(
    JNIEnv *env,
    jobject obj,
    int level,
    const char *format,
    ...);

static int _loggerLog(
    c_store_logger_t logger,
    size_t size,
    int level,
    const char *format,
    va_list args);

static int _logJava(
    JNIEnv *env,
    jobject obj,
    size_t size,
    jint level,
    const char *format,
    va_list args);

static void _throwNew(const char *name, const char *msg);

// JNI function definitions.

/** Called when this library is loaded.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    _javaVM = vm;

    JNIEnv *env;

    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_ERR;
    }

    if (!CStore_loadClasses(env, NULL)) {
        _throwNew("java/lang/RuntimeException", "Failed to load classes");
        return JNI_ERR;
    }

    return JNI_VERSION_1_4;
}

/** Called when this library is unloaded.
 */
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    if (_javaVM) {
        CStore_unloadClasses(NULL);
        _javaVM = NULL;
    }
}

/** Returns a handle for the implementation context.
 *
 * @param libraryHandle The implementation library handle.
 * @param logLevel The log level for the implementation.
 *
 * @return The context handle.
 */
JNIEXPORT jlong JNICALL Java_org_rvpf_store_server_c_CStore_contextHandle(
    JNIEnv *env, jobject obj, jlong libraryHandle, jint logLevel)
{
    c_store_context_function_t *contextFunction =
        CStore_resolveSymbol((void *) (intptr_t) libraryHandle, CONTEXT_FUNCTION_NAME);

    if (!contextFunction) return 0;

    c_store_logger_t logger = _allocate(sizeof(struct c_store_logger));
    struct logger_context *loggerContext =
        _allocate(sizeof(struct logger_context));
    jobject cStoreInstance = (*env)->NewWeakGlobalRef(env, obj);

    if (!(logger && loggerContext && cStoreInstance)) {
        CStore_free(logger);
        CStore_free(loggerContext);
        (*env)->DeleteWeakGlobalRef(env, cStoreInstance);
        return 0;
    }

    loggerContext->cStoreInstance = cStoreInstance;
    logger->log = _loggerLog;
    logger->level = logLevel;
    logger->context = loggerContext;

    return (jlong) (size_t) contextFunction(logger, NULL, 0, NULL, _javaVM);
}

/** Closes a library.
 *
 * @param libraryHandle The library handle.
 */
JNIEXPORT void JNICALL Java_org_rvpf_store_server_c_CStore_closeLibrary(
    JNIEnv *env, jobject obj, jlong libraryHandle)
{
    CStore_closeLibrary((void *) (intptr_t) libraryHandle);
    _logBack(env, obj, LOG_LEVEL_DEBUG, "Closed library");
}

/** Connects.
 *
 * @param contextHandle The implementation context handle.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_connect(
    JNIEnv *env, jobject obj, jlong contextHandle)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;

    return store->vector->connect(store);
}

/** Counts values.
 *
 * @param contextHandle The implementation context handle.
 * @param serverHandle The server handle for the point.
 * @param startTime The inclusive start time.
 * @param endTime The exclusive end time.
 * @param limit A limit for the number of values.
 * @param count A container for the count.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_count(
    JNIEnv *env, jobject obj, jlong contextHandle, jint serverHandle,
    jlong startTime, jlong endTime, jint limit, jobject countContainer)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    c_store_long_t count = -1;
    c_store_code_t status_code;

    status_code = store->vector->count(store, serverHandle,
        startTime, endTime, limit, &count);

    (*env)->CallVoidMethod(env, countContainer, _atomicLongSetMethod, count);

    return status_code;
}

/** Deletes points values.
 *
 * @param contextHandle The implementation context handle.
 * @param serverHandles The server handles for the points.
 * @param times The times for the values.
 * @param statusCodes The individual status codes.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_delete(
    JNIEnv *env, jobject obj, jlong contextHandle,
    jintArray serverHandles, jlongArray times, jintArray statusCodes)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    size_t count = (*env)->GetArrayLength(env, serverHandles);
    c_store_handle_t *server_handles = (*env)->GetIntArrayElements(
                               env, serverHandles, NULL);
    c_store_stamp_t *time_stamps = (*env)->GetLongArrayElements(
                               env, times, NULL);
    c_store_code_t *status_codes = (*env)->GetIntArrayElements(
                               env, statusCodes, NULL);
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (server_handles && time_stamps && status_codes) {
        status_code =
            store->vector->delete(store, count,
                server_handles, time_stamps, status_codes);
    }

    if (server_handles) {
        (*env)->ReleaseIntArrayElements(
            env, serverHandles, server_handles, JNI_ABORT);
    }
    if (time_stamps) {
        (*env)->ReleaseLongArrayElements(
            env, times, time_stamps, JNI_ABORT);
    }
    if (status_codes) {
        (*env)->ReleaseIntArrayElements(
            env, statusCodes, status_codes, 0);
    }

    return status_code;
}

/** Delivers values.
 *
 * @param contextHandle The implementation context handle.
 * @param limit A limit for the number of values.
 * @param timeout A time limit in millis to wait for the first message
 *                (negative for infinite).
 * @param container A container for the values.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_deliver(
    JNIEnv *env, jobject obj, jlong contextHandle,
    jint limit, jlong timeout, jobject container)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    size_t count = 0;
    c_store_value_t **values = NULL;
    c_store_code_t status_code;

    status_code = store->vector->deliver(store,
        limit, timeout, &count, &values);

    if (values) {
        CStore_returnValues(env, count, values, container);
        store->vector->freeValues(store, count, values);
    }

    return status_code;
}

/** Disconnects.
 *
 * @param contextHandle The implementation context handle.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_disconnect(
    JNIEnv *env, jobject obj, jlong contextHandle)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;

    return store->vector->disconnect(store);
}

/** Exchanges handles.
 *
 * @param contextHandle The implementation context handle.
 * @param tags Tags for the points (input).
 * @param clientHandles Client handles for the points (input).
 * @param serverHandles Server handles for the points (output).
 * @param statusCodes The individual status codes.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_exchangeHandles(
    JNIEnv *env, jobject obj, jlong contextHandle,
    jobjectArray tags, jintArray clientHandles, jintArray serverHandles,
    jintArray statusCodes)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    size_t count = (*env)->GetArrayLength(env, tags);
    char **tag_strings = _allocate(count * sizeof(char *));
    c_store_handle_t *client_handles = (*env)->GetIntArrayElements(
                               env, clientHandles, NULL);
    c_store_handle_t *server_handles = (*env)->GetIntArrayElements(
                               env, serverHandles, NULL);
    c_store_code_t *status_codes = (*env)->GetIntArrayElements(
                               env, statusCodes, NULL);
    bool failed = !tag_strings || !client_handles || !server_handles;
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (tag_strings) {
        for (int i = 0; i < count; ++i) {
            jbyteArray tag = (*env)->GetObjectArrayElement(env, tags, i);

            if (tag) {
                tag_strings[i] = CStore_bytesToCString(env, tag);
                if (!tag_strings[i]) failed = true;
                (*env)->DeleteLocalRef(env, tag);
            } else {
                tag_strings[i] = NULL;
                failed = true;
            }
        }
    }

    if (!failed) {
        status_code = store->vector->exchangeHandles(
            store, count, tag_strings, client_handles, server_handles,
            status_codes);
    }

    if (server_handles) {
        (*env)->ReleaseIntArrayElements(
            env, serverHandles, server_handles, 0);
    }
    if (status_codes) {
        (*env)->ReleaseIntArrayElements(
            env, statusCodes, status_codes, 0);
    }
    if (client_handles) {
        (*env)->ReleaseIntArrayElements(
            env, clientHandles, client_handles, JNI_ABORT);
    }
    if (tag_strings) {
        for (int i = 0; i < count; ++i) {
            if (tag_strings[i]) {
                CStore_free(tag_strings[i]);
                tag_strings[i] = NULL;
            }
        }
        CStore_free(tag_strings);
    }

    return status_code;
}

/** Frees the implementation context.
 *
 * @param contextHandle The implementation context handle.
 */
JNIEXPORT void JNICALL Java_org_rvpf_store_server_c_CStore_freeContext(
    JNIEnv *env, jobject obj, jlong contextHandle)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    c_store_logger_t logger = store->logger;

    store->vector->dispose(store);

    struct logger_context *loggerContext = logger->context;

    (*env)->DeleteWeakGlobalRef(env, loggerContext->cStoreInstance);
    loggerContext->cStoreInstance = NULL;
    CStore_free(loggerContext);

    logger->context = NULL;
    logger->log = NULL;
    CStore_free(logger);
}

/** Gets a code for a quality name.
 *
 * @param contextHandle The implementation context handle.
 * @param qualityName The quality name.
 *
 * @return A quality code (may be <code>null</code>).
 */
JNIEXPORT jobject JNICALL Java_org_rvpf_store_server_c_CStore_getQualityCode(
    JNIEnv *env, jobject obj, jlong contextHandle, jbyteArray qualityName)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    char *name = CStore_bytesToCString(env, qualityName);
    c_store_int_t code;
    jobject qualityCode;

    if (!name) return NULL;

    if (store->vector->getQualityCode(store, name, &code)) {
        qualityCode = NULL;
    } else {
        qualityCode = _integerValue(env, code);
    }

    CStore_free(name);

    return qualityCode;
}

/** Gets a name for a quality code.
 *
 * @param contextHandle The implementation context handle.
 * @param qualityCode The quality code.
 *
 * @return A quality name (may be <code>null</code>).
 */
JNIEXPORT jbyteArray JNICALL Java_org_rvpf_store_server_c_CStore_getQualityName(
    JNIEnv *env, jobject obj, jlong contextHandle, jint qualityCode)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    char *nameString = store->vector->getQualityName(store, qualityCode);
    jbyteArray nameBytes = CStore_cStringToBytes(env, nameString);

    CStore_free(nameString);

    return nameBytes;
}

/** Gets a code for a state name.
 *
 * @param contextHandle The implementation context handle.
 * @param serverHandle The server handle for the point.
 * @param stateName The state name.
 *
 * @return A state code (may be <code>null</code>).
 */
JNIEXPORT jobject JNICALL Java_org_rvpf_store_server_c_CStore_getStateCode(
    JNIEnv *env, jobject obj, jlong contextHandle, jint serverHandle,
    jbyteArray stateName)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    char *name = CStore_bytesToCString(env, stateName);
    c_store_int_t code;
    jobject stateCode;

    if (!name) return NULL;

    if (store->vector->getStateCode(store, serverHandle, name, &code)) {
        stateCode = NULL;
    } else {
        stateCode = _integerValue(env, code);
    }

    CStore_free(name);

    return stateCode;
}

/** Gets a name for a state code.
 *
 * @param contextHandle The implementation context handle.
 * @param serverHandle The server handle for the point.
 * @param stateCode The state code.
 *
 * @return A state name (may be <code>null</code>).
 */
JNIEXPORT jbyteArray JNICALL Java_org_rvpf_store_server_c_CStore_getStateName(
    JNIEnv *env, jobject obj, jlong contextHandle, jint serverHandle,
    jint stateCode)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    char *nameString =
        store->vector->getStateName(store, serverHandle, stateCode);
    jbyteArray nameBytes = CStore_cStringToBytes(env, nameString);

    CStore_free(nameString);

    return nameBytes;
}

/** Opens a library.
 *
 * @param libraryFilePath The library file path.
 *
 * @return A library handle.
 */
JNIEXPORT jlong JNICALL Java_org_rvpf_store_server_c_CStore_openLibrary(
    JNIEnv *env, jobject obj, jbyteArray libraryFilePath)
{
    char *libraryPath = CStore_bytesToCString(env, libraryFilePath);

    if (!libraryPath) return 0;

    if (!_logMethod) {
        _logMethod = (*env)->GetMethodID(
            env, (*env)->GetObjectClass(env, obj), "log", "(I[B)V");
        if ((*env)->ExceptionCheck(env)) return 0;
    }

    _logBack(env, obj, LOG_LEVEL_INFO,
        "Loading library from '%s'", libraryPath);

    jlong libraryHandle = (jlong) (intptr_t) CStore_openLibrary(libraryPath);

    CStore_free(libraryPath);

    return libraryHandle;
}

/** Interrupts.
 *
 * @param contextHandle The implementation context handle.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_interrupt(
    JNIEnv *env, jobject obj, jlong contextHandle)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;

    return store->vector->interrupt(store);
}

/** Puts an environment entry.
 *
 * @param contextHandle The implementation context handle.
 * @param entry The entry (key=value).
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_putEnv(
    JNIEnv *env, jobject obj, jlong contextHandle, jbyteArray entry)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    char *entryString = CStore_bytesToCString(env, entry);
    c_store_code_t status_code;

    ASSERT(entryString);
    status_code = store->vector->putEnv(store, entryString);
    CStore_free(entryString);

    return status_code;
}

/** Reads values.
 *
 * @param contextHandle The implementation context handle.
 * @param serverHandle The server handle for the point.
 * @param startTime The inclusive start time.
 * @param endTime The exclusive end time.
 * @param limit A limit for the number of values.
 * @param container A container for the values.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_read(
    JNIEnv *env, jobject obj, jlong contextHandle, jint serverHandle,
    jlong startTime, jlong endTime, jint limit, jobject container)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    size_t count = 0;
    c_store_value_t **values = NULL;
    c_store_code_t status_code;

    status_code = store->vector->read(store, serverHandle,
        startTime, endTime, limit, &count, &values);

    if (values) {
        CStore_returnValues(env, count, values, container);
        store->vector->freeValues(store, count, values);
    }

    return status_code;
}

/** Releases handles.
 *
 * @param contextHandle The implementation context handle.
 * @param serverHandles Server handles for the points.
 * @param statusCodes The individual status codes.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_releaseHandles(
    JNIEnv *env, jobject obj, jlong contextHandle,
    jintArray serverHandles, jintArray statusCodes)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    size_t count = (*env)->GetArrayLength(env, serverHandles);
    c_store_handle_t *server_handles = (*env)->GetIntArrayElements(
                               env, serverHandles, NULL);
    c_store_code_t *status_codes = (*env)->GetIntArrayElements(
                               env, statusCodes, NULL);
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (server_handles && status_codes) {
        status_code =
            store->vector->releaseHandles(
                store, count, server_handles, status_codes);
    }

    if (server_handles) {
        (*env)->ReleaseIntArrayElements(
            env, serverHandles, server_handles, JNI_ABORT);
    }
    if (status_codes) {
        (*env)->ReleaseIntArrayElements(
            env, statusCodes, status_codes, 0);
    }

    return status_code;
}

/** Subscribes to point value events.
 *
 * @param contextHandle The implementation context handle.
 * @param serverHandles Server handles for the points.
 * @param statusCodes The individual status codes.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_subscribe(
    JNIEnv *env , jobject obj, jlong contextHandle,
    jintArray serverHandles, jintArray statusCodes)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    size_t count = (*env)->GetArrayLength(env, serverHandles);
    c_store_handle_t *server_handles = (*env)->GetIntArrayElements(
                               env, serverHandles, NULL);
    c_store_code_t *status_codes = (*env)->GetIntArrayElements(
                               env, statusCodes, NULL);
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (server_handles && status_codes) {
        status_code = store->vector->subscribe(
            store, count, server_handles, status_codes);
    }

    if (server_handles) {
        (*env)->ReleaseIntArrayElements(
            env, serverHandles, server_handles, JNI_ABORT);
    }
    if (status_codes) {
        (*env)->ReleaseIntArrayElements(
            env, statusCodes, status_codes, 0);
    }

    return status_code;
}

/*
 * Class:     org_rvpf_store_server_c_CStore
 * Method:    supportedValueTypeCodes
 * Signature: (J)[B
 */
/** Asks for the supported value type codes.
 *
 * @param contextHandle The implementation context handle.
 *
 * @return The supported value type codes.
 */
JNIEXPORT jbyteArray JNICALL Java_org_rvpf_store_server_c_CStore_supportedValueTypeCodes(
    JNIEnv *env, jobject obj, jlong contextHandle)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;

    char *codesString = store->vector->supportedValueTypeCodes(store);
    jbyteArray codesBytes = CStore_cStringToBytes(env, codesString);

    CStore_free(codesString);

    return codesBytes;
}

/** Asks if this implementation supports (multiple) connections.
 *
 * @param contextHandle The implementation context handle.
 *
 * @return A <code>true</code> value if connections are supported.
 */
JNIEXPORT jboolean JNICALL Java_org_rvpf_store_server_c_CStore_supportsConnections(
    JNIEnv *env, jobject obj, jlong contextHandle)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;

    return store->vector->supportsConnections(store);
}

/** Asks if this implementation supports count.
 *
 * @param contextHandle The implementation context handle.
 *
 * @return A <code>true</code> value if count is supported.
 */
JNIEXPORT jboolean JNICALL Java_org_rvpf_store_server_c_CStore_supportsCount(
    JNIEnv *env, jobject obj, jlong contextHandle)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;

    return store->vector->supportsCount(store);
}

/** Asks if this implementation supports delete.
 *
 * @param contextHandle The implementation context handle.
 *
 * @return A <code>true</code> value if delete is supported.
 */
JNIEXPORT jboolean JNICALL Java_org_rvpf_store_server_c_CStore_supportsDelete(
    JNIEnv *env, jobject obj, jlong contextHandle)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;

    return store->vector->supportsDelete(store);
}

/** Asks if this implementation supports deliver.
 *
 * @param contextHandle The implementation context handle.
 *
 * @return A <code>true</code> value if deliver is supported.
 */
JNIEXPORT jboolean JNICALL Java_org_rvpf_store_server_c_CStore_supportsDeliver(
    JNIEnv *env, jobject obj, jlong contextHandle)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;

    return store->vector->supportsDeliver(store);
}

/** Asks if this implementation supports pull queries.
 *
 * @param contextHandle The implementation context handle.
 *
 * @return A <code>true</code> value if pull queries are supported.
 */
JNIEXPORT jboolean JNICALL Java_org_rvpf_store_server_c_CStore_supportsPull(
    JNIEnv *env, jobject obj, jlong contextHandle)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;

    return store->vector->supportsPull(store);
}

/** Asks if this implementation supports subscriptions.
 *
 * @param contextHandle The implementation context handle.
 *
 * @return A <code>true</code> value if subscriptions are supported.
 */
JNIEXPORT jboolean JNICALL Java_org_rvpf_store_server_c_CStore_supportsSubscribe(
    JNIEnv *env, jobject obj, jlong contextHandle)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;

    return store->vector->supportsSubscribe(store);
}

/** Asks if this implementation supports threads.
 *
 * @param contextHandle The implementation context handle.
 *
 * @return A <code>true</code> value if threads are supported.
 */
JNIEXPORT jboolean JNICALL Java_org_rvpf_store_server_c_CStore_supportsThreads(
    JNIEnv *env, jobject obj, jlong contextHandle)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;

    return store->vector->supportsThreads(store);
}

/** Unsubscribes from point value events.
 *
 * @param contextHandle The implementation context handle.
 * @param serverHandles Server handles for the points.
 * @param statusCodes The individual status codes.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_unsubscribe(
    JNIEnv *env, jobject obj, jlong contextHandle,
    jintArray serverHandles, jintArray statusCodes)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    size_t count = (*env)->GetArrayLength(env, serverHandles);
    c_store_handle_t *server_handles = (*env)->GetIntArrayElements(
                               env, serverHandles, NULL);
    c_store_code_t *status_codes = (*env)->GetIntArrayElements(
                               env, statusCodes, NULL);
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (server_handles && status_codes) {
        status_code =
            store->vector->unsubscribe(store,
                count, server_handles, status_codes);
    }

    if (server_handles) {
        (*env)->ReleaseIntArrayElements(
            env, serverHandles, server_handles, JNI_ABORT);
    }
    if (status_codes) {
        (*env)->ReleaseIntArrayElements(
            env, statusCodes, status_codes, 0);
    }

    return status_code;
}

/** Specifies the use of a <var>Charset</var>.
 *
 * @param contextHandle The implementation context handle.
 * @param charsetName The <var>Charset</var> name in US-ASCII.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_useCharset(
    JNIEnv *env, jobject obj, jlong contextHandle, jbyteArray charsetName)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    char *nameString = CStore_bytesToCString(env, charsetName);
    c_store_code_t status_code;

    ASSERT(nameString);
    status_code = store->vector->useCharset(store, nameString);
    CStore_free(nameString);

    return status_code;
}

/** Writes points values.
 *
 * @param contextHandle The implementation context handle.
 * @param values The points values.
 * @param statusCodes The individual status codes.
 *
 * @return A status code.
 */
JNIEXPORT jint JNICALL Java_org_rvpf_store_server_c_CStore_write(
    JNIEnv *env, jobject obj, jlong contextHandle,
    jobject container, jintArray statusCodes)
{
    c_store_t store = (c_store_t) (size_t) contextHandle;
    size_t count = 0;
    c_store_value_t **values = NULL;
    c_store_handle_t *status_codes = (*env)->GetIntArrayElements(
                               env, statusCodes, NULL);
    c_store_code_t status_code =
        CStore_acceptValues(env, container, &count, &values);

    if (!status_codes) status_code = STATUS_CODE_FAILED;
    if (status_code == STATUS_CODE_SUCCESS) {
        status_code = store->vector->write(store,
            count, values, status_codes);
    }

    if (status_codes) {
        (*env)->ReleaseIntArrayElements(
            env, statusCodes, status_codes, 0);
    }
    if (values) store->vector->freeValues(store, count, values);

    return status_code;
}


// Helper function definitions.

int CStore_acceptValues(
    JNIEnv *env,
    jobject container,
    size_t *count,
    c_store_value_t ***values)
{
    if (!container) return STATUS_CODE_FAILED;

    int status_code = (*env)->CallIntMethod(env, container,
        _valuesMethods.statusCode);
    bool failed = false;

    if ((*env)->ExceptionCheck(env)) return STATUS_CODE_FAILED;
    *count = (*env)->CallIntMethod(env, container, _valuesMethods.size);
    if (*count) {
        *values = CStore_allocate(sizeof(c_store_value_t *) * *count);
        ASSERT(*values);
        for (int i = 0; i < *count; ++i) {
            int client_handle =
                (*env)->CallIntMethod(env, container, _valuesMethods.next);

            if ((*env)->ExceptionCheck(env)) return STATUS_CODE_FAILED;

            jbyteArray value =
                (*env)->GetObjectField(env, container,
                    _valuesFields.value);
            size_t size;

            if ((*env)->ExceptionCheck(env)) return STATUS_CODE_FAILED;

            if (value != NULL) {
                size = (*env)->GetArrayLength(env, value);
                if ((*env)->ExceptionCheck(env)) failed = true;
            } else size = 0;
            if (!failed) {
                (*values)[i] = CStore_allocate(sizeof (c_store_value_t) + size);
                if (!(*values)[i]) failed = true;
            }
            if (!failed) {
                (*values)[i]->handle = client_handle;
                (*values)[i]->stamp =
                    (*env)->GetLongField(env,
                        container, _valuesFields.time);
                if ((*env)->ExceptionCheck(env)) failed = true;
            }

            if (!failed) {
                (*values)[i]->deleted =
                    (*env)->GetBooleanField(env,
                        container, _valuesFields.deleted);
                if ((*env)->ExceptionCheck(env)) failed = true;
            }
            if (!(*values)[i]->deleted) {
                if (!failed) {
                    (*values)[i]->quality =
                        (*env)->GetIntField(env,
                            container, _valuesFields.quality);
                    if ((*env)->ExceptionCheck(env)) failed = true;
                }

                if (!failed && value != NULL) {
                    (*env)->GetByteArrayRegion(env,
                        value, 0, size, (*values)[i]->value);
                    if ((*env)->ExceptionCheck(env)) failed = true;
                    (*values)[i]->size = size;
                }
            }

            (*env)->DeleteLocalRef(env, value);
            if (failed) return STATUS_CODE_FAILED;
        }
    } else *values = NULL;

    return status_code;
}

char *CStore_bytesToCString(JNIEnv *env, jbyteArray bytes)
{
    if (!bytes) return NULL;;

    jsize length = (*env)->GetArrayLength(env, bytes);
    char *chars = _allocate(length + 1);

    if (!chars) return NULL;

    (*env)->GetByteArrayRegion(env, bytes, 0, length, (jbyte *) chars);
    if ((*env)->ExceptionCheck(env)) {
        CStore_free(chars);
        return NULL;
    }
    chars[length] = '\0';

    return chars;
}

jbyteArray CStore_cStringToBytes(JNIEnv *env, char *cString)
{
    if (!cString) return NULL;

    size_t size = strlen(cString);
    jbyteArray bytes = (*env)->NewByteArray(env, size);

    if (bytes) {
        (*env)->SetByteArrayRegion(env, bytes, 0, size, (jbyte *) cString);
    }

    return bytes;
}

jclass CStore_getClass(JNIEnv *env, const char *name, c_store_logger_t logger)
{
    jclass localClassRef = (*env)->FindClass(env, name);
    jclass globalClassRef;

    if (localClassRef) {
        globalClassRef = _javaVM? (*env)->NewWeakGlobalRef(env, localClassRef)
            : (*env)->NewGlobalRef(env, localClassRef);
        (*env)->DeleteLocalRef(env, localClassRef);
    } else globalClassRef = NULL;
    if (!globalClassRef) {
        (*env)->ExceptionClear(env);
        if (logger) LOG_ERROR(logger, "Failed to get class: %s", name);
    }

    return globalClassRef;
}

jmethodID CStore_getMethodID(
    JNIEnv *env,
    jclass cls,
    const char *name,
    const char *sig,
    c_store_logger_t logger)
{
    jmethodID methodID = (*env)->GetMethodID(env, cls, name, sig);

    if (!methodID) {
        (*env)->ExceptionClear(env);
        if (logger) {
            LOG_ERROR(logger, "Failed to get instance method: %s%s", name, sig);
        }
    }

    return methodID;
}

jmethodID CStore_getStaticMethodID(
    JNIEnv *env,
    jclass cls,
    const char *name,
    const char *sig,
    c_store_logger_t logger)
{
    jmethodID methodID = (*env)->GetStaticMethodID(env, cls, name, sig);

    if (!methodID) {
        (*env)->ExceptionClear(env);
        if (logger) {
            LOG_ERROR(logger, "Failed to get static method: %s%s", name, sig);
        }
    }

    return methodID;
}

bool CStore_loadClasses(JNIEnv *env, c_store_logger_t logger)
{
    if (_valuesClass) return true;

    bool failed = false;

    _valuesClass = CStore_getClass(env,
        "org/rvpf/store/server/c/Values", logger);
    if (!_valuesClass) return false;
    _byteArrayClass = CStore_getClass(env, "[B", logger);
    if (!_byteArrayClass) return false;
    _integerClass = CStore_getClass(env, "java/lang/Integer", logger);
    if (!_integerClass) return false;
    _atomicLongClass =
        CStore_getClass(env, "java/util/concurrent/atomic/AtomicLong", logger);
    if (!_atomicLongClass) return false;

    _valuesMethods.constructor =
        CStore_getMethodID(env, _valuesClass, "<init>", "()V", logger);
    failed |= _valuesMethods.constructor == NULL;
    _valuesMethods.add =
        CStore_getMethodID(env, _valuesClass, "add", "(IJZI[B)V", logger);
    failed |= _valuesMethods.add == NULL;
    _valuesMethods.statusCode =
        CStore_getMethodID(env, _valuesClass, "statusCode", "()I", logger);
    failed |= _valuesMethods.statusCode == NULL;
    _valuesMethods.size =
        CStore_getMethodID(env, _valuesClass, "size", "()I", logger);
    failed |= _valuesMethods.size == NULL;
    _valuesMethods.next =
        CStore_getMethodID(env, _valuesClass, "next", "()I", logger);
    failed |= _valuesMethods.next == NULL;

    _valuesFields.time =
        _getFieldID(env, _valuesClass, "_time", "J", logger);
    failed |= _valuesFields.time == NULL;
    _valuesFields.deleted =
        _getFieldID(env, _valuesClass, "_deleted", "Z", logger);
    failed |= _valuesFields.deleted == NULL;
    _valuesFields.quality =
        _getFieldID(env, _valuesClass, "_quality", "I", logger);
    failed |= _valuesFields.quality == NULL;
    _valuesFields.value =
        _getFieldID(env, _valuesClass, "_value", "[B", logger);
    failed |= _valuesFields.value == NULL;

    _integerValueMethod =
        CStore_getStaticMethodID(env, _integerClass, "valueOf",
            "(I)Ljava/lang/Integer;", logger);
    failed |= _integerValueMethod == NULL;
    _atomicLongSetMethod =
        CStore_getMethodID(env, _atomicLongClass, "set", "(J)V", logger);

    return !failed;
}

jobject CStore_newByteArray(JNIEnv *env, jsize size)
{
    return (*env)->NewObjectArray(env, size, _byteArrayClass, NULL);
}

jobject CStore_newValuesContainer(JNIEnv *env)
{
    return (*env)->NewObject(env, _valuesClass, _valuesMethods.constructor);
}

void CStore_returnValues(
    JNIEnv *env,
    int count,
    c_store_value_t **values,
    jobject container)
{
    for (int i = 0; i < count; ++i) {
        c_store_value_t *value = values[i];
        jbyteArray value_bytes = (*env)->NewByteArray(env, value->size);

        if (!value_bytes) return;

        (*env)->SetByteArrayRegion(env, value_bytes,
            0, value->size, value->value);
        ASSERT(!(*env)->ExceptionCheck(env));

        (*env)->CallVoidMethod(env, container, _valuesMethods.add,
            value->handle, value->stamp, value->deleted,
            value->quality, value_bytes);
        ASSERT(!(*env)->ExceptionCheck(env));

        (*env)->DeleteLocalRef(env, value_bytes);
    }
}

void CStore_unloadClasses(JNIEnv *env)
{
    if (env) {
        if (!_javaVM) {
            (*env)->DeleteGlobalRef(env, _valuesClass);
            (*env)->DeleteGlobalRef(env, _byteArrayClass);
            (*env)->DeleteGlobalRef(env, _integerClass);
        }
    } else if (_javaVM) {
        JNIEnv *env;

        if ((*_javaVM)->GetEnv(_javaVM, (void **)&env, JNI_VERSION_1_4) != JNI_OK) {
            env = NULL;
        }

        if (env) {
            (*env)->DeleteWeakGlobalRef(env, _valuesClass);
            (*env)->DeleteWeakGlobalRef(env, _byteArrayClass);
            (*env)->DeleteWeakGlobalRef(env, _integerClass);
        }
    }

    _valuesClass = NULL;
    _byteArrayClass = NULL;
    _integerClass = NULL;
}

// Private function definitions.

static void *_allocate(size_t size)
{
    char *memory = CStore_allocate(size);

    if (!memory) _throwNew("java/lang/OutOfMemoryError", NULL);

    return memory;
}

jfieldID _getFieldID(
    JNIEnv *env,
    jclass cls,
    const char *name,
    const char *sig,
    c_store_logger_t logger)
{
    jfieldID fieldID = (*env)->GetFieldID(env, cls, name, sig);

    if (!fieldID) {
        (*env)->ExceptionClear(env);
        if (logger) {
            LOG_ERROR(logger, "Failed to get field: %s%s", name, sig);
        }
    }

    return fieldID;
}

static jobject _integerValue(JNIEnv *env, c_store_int_t value)
{
    return (*env)->CallStaticObjectMethod(env,
        _integerClass, _integerValueMethod, value);
}

static void _logBack(
    JNIEnv *env,
    jobject obj,
    int level,
    const char *format,
    ...)
{
    int result = strlen(format);
    va_list args;

    do {
        va_start(args, format);
        result = _logJava(env, obj, (size_t) result, level, format, args);
        va_end(args);
    } while (result > 0);

    ASSERT(result == 0);
}

static int _loggerLog(
    c_store_logger_t logger,
    size_t size,
    int level,
    const char *format,
    va_list args)
{
    ASSERT(_javaVM);
    ASSERT(logger->context);

    JavaVMAttachArgs vmArgs;
    JNIEnv *env;

    vmArgs.version = JNI_VERSION_1_4;
    vmArgs.name = NULL;
    vmArgs.group = NULL;
    if ((*_javaVM)->AttachCurrentThreadAsDaemon(_javaVM, (void **) &env, &vmArgs)) {
        return -1;
    }

    struct logger_context *loggerContext = logger->context;

    return _logJava(env, loggerContext->cStoreInstance,
        size, level, format, args);
}

static int _logJava(
    JNIEnv *env,
    jobject obj,
    size_t size,
    jint level,
    const char *format,
    va_list args)
{
    if (size < 1024) size = 1024;

    char *buffer = CStore_allocate(size + 1);
    int result;

    ASSERT(buffer);

    result = vsnprintf(buffer, size + 1, format, args);
    if (result >= 0 && result <= size) {
        ASSERT((*env)->ExceptionCheck(env) == JNI_FALSE);

        jbyteArray bytes = CStore_cStringToBytes(env, buffer);

        if (bytes) {
            (*env)->CallVoidMethod(env, obj, _logMethod, level, bytes);
            result = (*env)->ExceptionCheck(env)? -4: 0;
            (*env)->DeleteLocalRef(env, bytes);
        } else result = -3;
    } else if (result == -1) result = size * 2;

    CStore_free(buffer);

    return result;
}

static void _throwNew(const char *name, const char *msg)
{
    if (_javaVM) {
        JNIEnv *env;

        if ((*_javaVM)->GetEnv(
                _javaVM, (void **)&env, JNI_VERSION_1_4) == JNI_OK) {
            jclass cls = (*env)->FindClass(env, name);

            ASSERT(cls);
            (*env)->ThrowNew(env, cls, msg);
            (*env)->DeleteLocalRef(env, cls);
        }
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

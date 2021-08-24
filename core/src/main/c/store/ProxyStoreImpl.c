/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProxyStoreImpl.c 3961 2019-05-06 20:14:59Z SFB $
 */
#include "ProxyStoreImpl.h"
#include "CStoreVector.h"

#include <string.h>

// Private type definitions.

struct context
{
    jobject proxy;
};

// Private variable definitions.

static int _contexts = 0;

static JavaVM *_javaVM = NULL;

static void *_jvmHandle = 0;

static jclass _storeProxyClass = NULL;

static struct
{
    jmethodID describeException;
    jmethodID constructor;
    jmethodID useCharset;
    jmethodID putEnv;
    jmethodID connect;
    jmethodID exchangeHandles;
    jmethodID supportsSubscribe;
    jmethodID subscribe;
    jmethodID supportsDeliver;
    jmethodID deliver;
    jmethodID supportsCount;
    jmethodID supportsDelete;
    jmethodID supportsPull;
    jmethodID supportedValueTypeCodes;
    jmethodID count;
    jmethodID read;
    jmethodID getStateCode;
    jmethodID getStateName;
    jmethodID write;
    jmethodID delete;
    jmethodID interrupt;
    jmethodID unsubscribe;
    jmethodID releaseHandles;
    jmethodID disconnect;
    jmethodID dispose;
}
_storeProxyMethods = {};

// Private forward declarations.

static bool _exceptionDescribed(JNIEnv *env, c_store_logger_t logger);

static JNIEnv *_getJNIEnv(void);

static bool _loadClasses(c_store_logger_t logger);

static void _unloadClasses(void);

// Public shareable object function definitions.

RVPF_EXPORT c_store_t RVPF_CStore_context(
    c_store_logger_t logger,
    const char *vmPath,
    int argc,
    char *argv[],
    void *vm)
{
    _javaVM = vm;

    if (!_javaVM) {
        const char *libraryPath = vmPath;

        if (!libraryPath) {
#ifdef _WIN32
            libraryPath = "jvm.dll";
#else
            libraryPath = "libjvm.so";
#endif

        }

        LOG_INFO(logger, "Creating Java VM from library \"%s\"", libraryPath);
        _jvmHandle = CStore_openLibrary(libraryPath);
        if (!_jvmHandle) {
            LOG_ERROR(logger, "Failed to load Java VM library");
            return NULL;
        }

        jint JNICALL (*createFunction)(JavaVM **, void **, void *) =
            CStore_resolveSymbol(_jvmHandle, "JNI_CreateJavaVM");

        if (createFunction) {
            JavaVMInitArgs vmArgs;
            JavaVMOption *vmOptions =
                argc > 0? CStore_allocate(argc * sizeof(JavaVMOption)): NULL;
            JNIEnv *env;

            if (vmOptions || !argc) {
                for (int i = 0; i < argc; ++i) {
                    vmOptions[i].optionString = argv[i];
                    vmOptions[i].extraInfo = NULL;
                }
                vmArgs.version = JNI_VERSION_1_4;
                vmArgs.options = vmOptions;
                vmArgs.nOptions = argc;
                vmArgs.ignoreUnrecognized = JNI_TRUE;
                if (createFunction(&_javaVM, (void **) &env, &vmArgs)) {
                    _javaVM = NULL;
                }
                CStore_free(vmOptions);
            }
        }

        if (!_javaVM) {
            CStore_closeLibrary(_jvmHandle);
            _jvmHandle = 0;
            LOG_ERROR(logger, "Failed to create Java VM");
            return NULL;
        }
    }

    if (!_loadClasses(logger)) {
        CStore_dispose(NULL);
        return NULL;
    }

    struct context *context = CStore_allocate(sizeof(struct context));
    c_store_t store = CStore_createContext(logger, context);

    if (store) {
        JNIEnv *env = _getJNIEnv();

        if (env) {
            jobject obj =
                (*env)->NewObject(env, _storeProxyClass,
                                  _storeProxyMethods.constructor);

            if ((*env)->ExceptionCheck(env)) {
                (*env)->ExceptionDescribe(env);
            } else {
                context->proxy = (*env)->NewGlobalRef(env, obj);
                (*env)->DeleteLocalRef(env, obj);
            }
        }

        ++_contexts;
        if (!context->proxy) {
            CStore_dispose((c_store_t) context);
            context = NULL;
        }
    } else {
        if (context) CStore_free(context);
        CStore_dispose(NULL);
    }

    return store;
}

// Private CStore implementation definitions.

static c_store_code_t CStore_connect(c_store_t cStore)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (env) {
        status_code = (*env)->CallIntMethod(env, context->proxy,
                                         _storeProxyMethods.connect);
        if (_exceptionDescribed(env, cStore->logger)) {
            status_code = STATUS_CODE_FAILED;
        }
    }

    return status_code;
}

static c_store_code_t CStore_count(
    c_store_t cStore,
    c_store_handle_t server_handle,
    c_store_stamp_t start_time,
    c_store_stamp_t end_time,
    size_t limit,
    c_store_long_t *count)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool failed = !env;
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (!failed) {
        if (!server_handle) return STATUS_CODE_BAD_HANDLE;

        *count = (*env)->CallLongMethod(env, context->proxy,
            _storeProxyMethods.count, server_handle, start_time, end_time, limit);

        if (!_exceptionDescribed(env, cStore->logger) && *count >= 0) {
            status_code = STATUS_CODE_SUCCESS;
        }
    }

    return status_code;
}

static c_store_code_t CStore_delete(
    c_store_t cStore,
    size_t count,
    c_store_handle_t *server_handles,
    c_store_stamp_t *time_stamps,
    c_store_code_t *status_codes)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool failed = !env;
    jintArray serverHandles = NULL;
    jlongArray times = NULL;
    jintArray statusCodes = NULL;
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (!failed) {
        serverHandles = (*env)->NewIntArray(env, count);
        times = (*env)->NewLongArray(env, count);
        statusCodes = (*env)->NewIntArray(env, count);

        if (!serverHandles || !statusCodes) failed = true;
    }

    if (!failed) {
        (*env)->SetIntArrayRegion(env, serverHandles, 0, count, server_handles);
        (*env)->SetLongArrayRegion(env, times, 0, count, time_stamps);
        (*env)->SetIntArrayRegion(env, statusCodes, 0, count, status_codes);
        status_code = (*env)->CallIntMethod(env, context->proxy,
            _storeProxyMethods.delete, serverHandles, times, statusCodes);
        if (_exceptionDescribed(env, cStore->logger)) {
            status_code = STATUS_CODE_FAILED;
        }
        (*env)->GetIntArrayRegion(env, statusCodes, 0, count, status_codes);
    }

    (*env)->DeleteLocalRef(env, serverHandles);
    (*env)->DeleteLocalRef(env, times);
    (*env)->DeleteLocalRef(env, statusCodes);

    return status_code;
}

static c_store_code_t CStore_deliver(
    c_store_t cStore,
    size_t limit,
    c_store_millis_t timeout,
    size_t *count,
    c_store_value_t ***values)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool failed = !env;
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (!failed) {
        jobject container = (*env)->CallObjectMethod(env, context->proxy,
            _storeProxyMethods.deliver, limit, timeout);

        if (!_exceptionDescribed(env, cStore->logger)) {
            status_code = CStore_acceptValues(env, container, count, values);
            if (_exceptionDescribed(env, cStore->logger)) {
                status_code = STATUS_CODE_FAILED;
            }
        }
        (*env)->DeleteLocalRef(env, container);
    }

    return status_code;
}

static c_store_code_t CStore_disconnect(c_store_t cStore)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (env) {
        status_code = (*env)->CallIntMethod(env, context->proxy,
                               _storeProxyMethods.disconnect);
        if (_exceptionDescribed(env, cStore->logger)) {
            status_code = STATUS_CODE_FAILED;
        }
    }

    return status_code;
}

static void CStore_dispose(c_store_t cStore)
{
    c_store_logger_t logger = cStore? cStore->logger: NULL;

    if (cStore) {
        ASSERT(_contexts > 0);

        struct context *context = (struct context *) cStore->context;
        JNIEnv *env = _getJNIEnv();

        if (env) {
            (*env)->CallVoidMethod(env, context->proxy,
                                   _storeProxyMethods.dispose);
            _exceptionDescribed(env, logger);
            (*env)->DeleteGlobalRef(env, context->proxy);
        }

        CStore_disposeContext(cStore);
        memset(context, 0, sizeof(struct context));
        CStore_free(context);
        --_contexts;
    }

    if (!_contexts) {
        _unloadClasses();

        if (_jvmHandle) {
            if (_javaVM) {
                (*_javaVM)->DestroyJavaVM(_javaVM);
                _javaVM = NULL;
                if (logger) LOG_DEBUG(logger, "Destroyed Java VM");
            }
            CStore_closeLibrary(_jvmHandle);
            _jvmHandle = 0;
        }
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
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool failed = !env;
    jobjectArray tags = NULL;
    jintArray clientHandles= NULL;
    jintArray serverHandles = NULL;
    jintArray statusCodes = NULL;
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (!failed) {
        tags = CStore_newByteArray(env, count);
        failed |= _exceptionDescribed(env, cStore->logger);
    }
    if (!failed) {
        clientHandles = (*env)->NewIntArray(env, count);
        failed |= _exceptionDescribed(env, cStore->logger);
    }
    if (!failed) {
        serverHandles = (*env)->NewIntArray(env, count);
        failed |= _exceptionDescribed(env, cStore->logger);
    }
    if (!failed) {
        statusCodes = (*env)->NewIntArray(env, count);
        failed |= _exceptionDescribed(env, cStore->logger);
    }

    if (!failed) {
        for (int i = 0; i < count; ++i) {
            jbyteArray tag = CStore_cStringToBytes(env, tag_strings[i]);

            if (tag) {
                (*env)->SetObjectArrayElement(env, tags, i, tag);
                (*env)->DeleteLocalRef(env, tag);
            } else {
                failed = true;
                break;
            }
        }
    }

    if (!failed) {
        (*env)->SetIntArrayRegion(env, clientHandles, 0, count, client_handles);
        (*env)->SetIntArrayRegion(env, statusCodes, 0, count, status_codes);
        status_code = (*env)->CallIntMethod(env, context->proxy,
                               _storeProxyMethods.exchangeHandles,
                               tags, clientHandles, serverHandles,
                               statusCodes);
        if (_exceptionDescribed(env, cStore->logger)) {
            status_code = STATUS_CODE_FAILED;
        } else {
            (*env)->GetIntArrayRegion(env, serverHandles, 0, count, server_handles);
            (*env)->GetIntArrayRegion(env, statusCodes, 0, count, status_codes);
        }
    }

    (*env)->DeleteLocalRef(env, statusCodes);
    (*env)->DeleteLocalRef(env, serverHandles);
    (*env)->DeleteLocalRef(env, clientHandles);
    (*env)->DeleteLocalRef(env, tags);

    return status_code;
}

static void CStore_freeValues(
    c_store_t cStore,
    size_t count,
    c_store_value_t **values)
{
    if (values) {
        for (int i = 0; i < count; ++i) {
            CStore_free(values[i]);
            values[i] = NULL;
        }
        CStore_free(values);
    }
}

static c_store_code_t CStore_getQualityCode(
    c_store_t cStore,
    char *qualityName,
    c_store_quality_t *qualityCode)
{
    return CStore_getStateCode(cStore, 0, qualityName, qualityCode);
}

static char *CStore_getQualityName(
    c_store_t cStore,
    c_store_quality_t qualityCode)
{
    return CStore_getStateName(cStore, 0, qualityCode);
}

static c_store_code_t CStore_getStateCode(
    c_store_t cStore,
    c_store_handle_t server_handle,
    char *stateName,
    c_store_quality_t *stateCode)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (env) {
        jbyteArray nameBytes = CStore_cStringToBytes(env, stateName);

        if (nameBytes) {
            int code = (*env)->CallIntMethod(env, context->proxy,
                    _storeProxyMethods.getStateCode, nameBytes, server_handle);
            if (!_exceptionDescribed(env, cStore->logger)) {
                *stateCode = code;
                status_code = STATUS_CODE_SUCCESS;
            }
        }
    }

    return status_code;
}

static char *CStore_getStateName(
    c_store_t cStore,
    c_store_handle_t server_handle,
    c_store_quality_t stateCode)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    char *stateName = NULL;

    if (env) {
        jarray nameBytes = (*env)->CallObjectMethod(env, context->proxy,
                _storeProxyMethods.getStateName, stateCode, server_handle);
        if (!_exceptionDescribed(env, cStore->logger) && nameBytes != NULL) {
            stateName = CStore_bytesToCString(env, nameBytes);
        }
    }

    return stateName;
}

static c_store_code_t CStore_interrupt(c_store_t cStore)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (env) {
        (*env)->CallVoidMethod(env, context->proxy,
                               _storeProxyMethods.disconnect);
        if (!_exceptionDescribed(env, cStore->logger)) {
            status_code = STATUS_CODE_SUCCESS;
        }
    }

    return status_code;
}

static c_store_code_t CStore_putEnv(c_store_t cStore, char *entryString)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (env) {
        jbyteArray entryBytes = CStore_cStringToBytes(env, entryString);

        if (entryBytes) {
            (*env)->CallVoidMethod(env, context->proxy,
                _storeProxyMethods.putEnv, entryBytes);
            if (!_exceptionDescribed(env, cStore->logger)) {
                status_code = STATUS_CODE_SUCCESS;
            }
        }
    }

    return status_code;
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
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool failed = !env;
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (!failed) {
        if (!server_handle) return STATUS_CODE_BAD_HANDLE;

        jobject container = (*env)->CallObjectMethod(env, context->proxy,
            _storeProxyMethods.read, server_handle, start_time, end_time, limit);

        status_code = CStore_acceptValues(env, container, count, values);
        if (_exceptionDescribed(env, cStore->logger)) {
            status_code = STATUS_CODE_FAILED;
        }
        (*env)->DeleteLocalRef(env, container);
    }

    return status_code;
}

static c_store_code_t CStore_releaseHandles(
    c_store_t cStore,
    size_t count,
    c_store_handle_t *server_handles,
    c_store_code_t *status_codes)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool failed = !env;
    jintArray serverHandles = NULL;
    jintArray statusCodes = NULL;
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (!failed) {
        serverHandles = (*env)->NewIntArray(env, count);
        statusCodes = (*env)->NewIntArray(env, count);

        if (!serverHandles || !statusCodes) failed = true;
    }

    if (!failed) {
        (*env)->SetIntArrayRegion(env, serverHandles, 0, count, server_handles);
        (*env)->SetIntArrayRegion(env, statusCodes, 0, count, status_codes);
        status_code = (*env)->CallIntMethod(env, context->proxy,
                               _storeProxyMethods.releaseHandles,
                               serverHandles, statusCodes);
        if (_exceptionDescribed(env, cStore->logger)) {
            status_code = STATUS_CODE_FAILED;
        }
        (*env)->GetIntArrayRegion(env, statusCodes, 0, count, status_codes);
    }

    (*env)->DeleteLocalRef(env, serverHandles);
    (*env)->DeleteLocalRef(env, statusCodes);

    return status_code;
}

static c_store_code_t CStore_subscribe(
    c_store_t cStore,
    size_t count,
    c_store_handle_t *server_handles,
    c_store_code_t *status_codes)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool failed = !env;
    jintArray serverHandles = NULL;
    jintArray statusCodes = NULL;
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (!failed) {
        serverHandles = (*env)->NewIntArray(env, count);
        statusCodes = (*env)->NewIntArray(env, count);

        if (!serverHandles || !statusCodes) failed = true;
    }

    if (!failed) {
        (*env)->SetIntArrayRegion(env, serverHandles, 0, count, server_handles);
        (*env)->SetIntArrayRegion(env, statusCodes, 0, count, status_codes);
        status_code = (*env)->CallIntMethod(env, context->proxy,
            _storeProxyMethods.subscribe, serverHandles, statusCodes);
        if (_exceptionDescribed(env, cStore->logger)) {
            status_code = STATUS_CODE_FAILED;
        }
        (*env)->GetIntArrayRegion(env, statusCodes, 0, count, status_codes);
    }

    (*env)->DeleteLocalRef(env, serverHandles);
    (*env)->DeleteLocalRef(env, statusCodes);

    return status_code;
}

static char *CStore_supportedValueTypeCodes(c_store_t cStore)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    char *typeCodes = "";

    if (env) {
        jarray codeBytes = (*env)->CallObjectMethod(env, context->proxy,
                _storeProxyMethods.supportedValueTypeCodes);
        if (!_exceptionDescribed(env, cStore->logger) && codeBytes != NULL) {
            typeCodes = CStore_bytesToCString(env, codeBytes);
        }
    }

    return typeCodes;
}

static bool CStore_supportsConnections(c_store_t cStore)
{
    return true;
}

static bool CStore_supportsCount(c_store_t cStore)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool answer = false;

    if (env) {
        answer = (*env)->CallBooleanMethod(env, context->proxy,
                                         _storeProxyMethods.supportsCount);
        if (_exceptionDescribed(env, cStore->logger)) answer = false;
    }

    return answer;
}

static bool CStore_supportsDelete(c_store_t cStore)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool answer = false;

    if (env) {
        answer = (*env)->CallBooleanMethod(env, context->proxy,
                                         _storeProxyMethods.supportsDelete);
        if (_exceptionDescribed(env, cStore->logger)) answer = false;
    }

    return answer;
}

static bool CStore_supportsDeliver(c_store_t cStore)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool answer = false;

    if (env) {
        answer = (*env)->CallBooleanMethod(env, context->proxy,
                                         _storeProxyMethods.supportsDeliver);
        if (_exceptionDescribed(env, cStore->logger)) answer = false;
    }

    return answer;
}

static bool CStore_supportsPull(c_store_t cStore)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool answer = false;

    if (env) {
        answer = (*env)->CallBooleanMethod(env, context->proxy,
                                         _storeProxyMethods.supportsPull);
        if (_exceptionDescribed(env, cStore->logger)) answer = false;
    }

    return answer;
}

static bool CStore_supportsSubscribe(c_store_t cStore)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool answer = false;

    if (env) {
        answer = (*env)->CallBooleanMethod(env, context->proxy,
                                         _storeProxyMethods.supportsSubscribe);
        if (_exceptionDescribed(env, cStore->logger)) answer = false;
    }

    return answer;
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
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool failed = !env;
    jintArray serverHandles = NULL;
    jintArray statusCodes = NULL;
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (!failed) {
        serverHandles = (*env)->NewIntArray(env, count);
        statusCodes = (*env)->NewIntArray(env, count);

        if (!serverHandles || !statusCodes) failed = true;
    }

    if (!failed) {
        (*env)->SetIntArrayRegion(env, serverHandles, 0, count, server_handles);
        (*env)->SetIntArrayRegion(env, statusCodes, 0, count, status_codes);
        status_code = (*env)->CallIntMethod(env, context->proxy,
            _storeProxyMethods.unsubscribe, serverHandles, statusCodes);
        if (_exceptionDescribed(env, cStore->logger)) {
            status_code = STATUS_CODE_FAILED;
        }
        (*env)->GetIntArrayRegion(env, statusCodes, 0, count, status_codes);
    }

    (*env)->DeleteLocalRef(env, serverHandles);
    (*env)->DeleteLocalRef(env, statusCodes);

    return status_code;
}

static c_store_code_t CStore_useCharset(c_store_t cStore, char *charsetName)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (env) {
        jbyteArray nameBytes = CStore_cStringToBytes(env, charsetName);

        if (nameBytes) {
            (*env)->CallVoidMethod(env, context->proxy,
                                   _storeProxyMethods.useCharset, nameBytes);
            if (!_exceptionDescribed(env, cStore->logger)) {
                status_code = STATUS_CODE_SUCCESS;
            }
        }
    }

    return status_code;
}

static c_store_code_t CStore_write(
    c_store_t cStore,
    size_t count,
    c_store_value_t **values,
    c_store_code_t *status_codes)
{
    struct context *context = (struct context *) cStore->context;
    JNIEnv *env = _getJNIEnv();
    bool failed = !env;
    jobject container = NULL;
    jintArray statusCodes = NULL;
    c_store_code_t status_code = STATUS_CODE_FAILED;

    if (!failed) {
        container = CStore_newValuesContainer(env);
        failed |= _exceptionDescribed(env, cStore->logger);
        statusCodes = (*env)->NewIntArray(env, count);
        failed |= _exceptionDescribed(env, cStore->logger);
    }

    if (!failed) {
        CStore_returnValues(env, count, values, container);
        (*env)->SetIntArrayRegion(env, statusCodes, 0, count, status_codes);
        ASSERT(!(*env)->ExceptionCheck(env));
        status_code = (*env)->CallIntMethod(env, context->proxy,
            _storeProxyMethods.write, container, statusCodes);
        if (_exceptionDescribed(env, cStore->logger)) {
            status_code = STATUS_CODE_FAILED;
        }
        (*env)->GetIntArrayRegion(env, statusCodes, 0, count, status_codes);
        ASSERT(!(*env)->ExceptionCheck(env));
    }

    (*env)->DeleteLocalRef(env, container);
    (*env)->DeleteLocalRef(env, statusCodes);

    return status_code;
}


// Private function definitions.

static bool _exceptionDescribed(JNIEnv *env, c_store_logger_t logger)
{
    ASSERT(_storeProxyClass);

    jthrowable exception = (*env)->ExceptionOccurred(env);

    if (exception) {
        (*env)->ExceptionClear(env);

        jbyteArray bytes = (*env)->CallStaticObjectMethod(env, _storeProxyClass,
            _storeProxyMethods.describeException, exception);
        char *description = CStore_bytesToCString(env, bytes);

        ASSERT(description);
        (*env)->DeleteLocalRef(env, bytes);
        LOG_WARN(logger, "%s", description);
        CStore_free(description);

        return true;
    }

    return false;
}

static JNIEnv *_getJNIEnv(void)
{
    JavaVMAttachArgs args;
    JNIEnv *env;

    args.version = JNI_VERSION_1_4;
    args.name = NULL;
    args.group = NULL;
    if ((*_javaVM)->AttachCurrentThread(_javaVM, (void **) &env, &args)) {
        env = NULL;
    }

    return env;
}

static bool _loadClasses(c_store_logger_t logger)
{
    if (_storeProxyClass) return true;

    JNIEnv *env = _getJNIEnv();
    bool failed = false;

    _storeProxyClass = CStore_getClass(env,
        "org/rvpf/store/server/c/StoreProxy", logger);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        return false;
    }

    _storeProxyMethods.describeException =
        CStore_getStaticMethodID(env, _storeProxyClass, "describeException",
        "(Ljava/lang/Throwable;)[B", logger);
    failed |= _storeProxyMethods.describeException == NULL;
    _storeProxyMethods.constructor =
        CStore_getMethodID(env, _storeProxyClass, "<init>", "()V", logger);
    failed |= _storeProxyMethods.constructor == NULL;
    _storeProxyMethods.useCharset =
        CStore_getMethodID(env, _storeProxyClass, "useCharset", "([B)V", logger);
    failed |= _storeProxyMethods.useCharset == NULL;
    _storeProxyMethods.putEnv =
        CStore_getMethodID(env, _storeProxyClass, "putEnv", "([B)V", logger);
    failed |= _storeProxyMethods.putEnv == NULL;
    _storeProxyMethods.connect =
        CStore_getMethodID(env, _storeProxyClass, "connect", "()I", logger);
    failed |= _storeProxyMethods.connect == NULL;
    _storeProxyMethods.exchangeHandles =
        CStore_getMethodID(env, _storeProxyClass, "exchangeHandles",
        "([[B[I[I[I)I", logger);
    failed |= _storeProxyMethods.exchangeHandles == NULL;
    _storeProxyMethods.supportsSubscribe =
        CStore_getMethodID(env, _storeProxyClass, "supportsSubscribe",
        "()Z", logger);
    failed |= _storeProxyMethods.supportsSubscribe == NULL;
    _storeProxyMethods.subscribe =
        CStore_getMethodID(env, _storeProxyClass, "subscribe",
        "([I[I)I", logger);
    failed |= _storeProxyMethods.subscribe == NULL;
    _storeProxyMethods.supportsDeliver =
        CStore_getMethodID(env, _storeProxyClass, "supportsDeliver",
        "()Z", logger);
    failed |= _storeProxyMethods.supportsDeliver == NULL;
    _storeProxyMethods.deliver =
        CStore_getMethodID(env, _storeProxyClass, "deliver",
        "(IJ)Lorg/rvpf/store/server/c/Values;", logger);
    failed |= _storeProxyMethods.deliver == NULL;
    _storeProxyMethods.supportsCount =
        CStore_getMethodID(env, _storeProxyClass, "supportsCount",
        "()Z", logger);
    failed |= _storeProxyMethods.supportsCount == NULL;
    _storeProxyMethods.supportsDelete =
        CStore_getMethodID(env, _storeProxyClass, "supportsDelete",
        "()Z", logger);
    failed |= _storeProxyMethods.supportsDelete == NULL;
    _storeProxyMethods.supportsPull =
        CStore_getMethodID(env, _storeProxyClass, "supportsPull",
        "()Z", logger);
    failed |= _storeProxyMethods.supportsPull == NULL;
    _storeProxyMethods.supportedValueTypeCodes =
        CStore_getMethodID(env, _storeProxyClass, "supportedValueTypeCodes",
        "()[B", logger);
    failed |= _storeProxyMethods.supportedValueTypeCodes == NULL;
    _storeProxyMethods.count =
        CStore_getMethodID(env, _storeProxyClass, "count",
        "(IJJI)J", logger);
    failed |= _storeProxyMethods.count == NULL;
    _storeProxyMethods.read =
        CStore_getMethodID(env, _storeProxyClass, "read",
        "(IJJI)Lorg/rvpf/store/server/c/Values;", logger);
    failed |= _storeProxyMethods.read == NULL;
    _storeProxyMethods.getStateCode =
        CStore_getMethodID(env, _storeProxyClass, "getStateCode",
        "([BI)I", logger);
    failed |= _storeProxyMethods.getStateCode == NULL;
    _storeProxyMethods.getStateName =
        CStore_getMethodID(env, _storeProxyClass, "getStateName",
        "(II)[B", logger);
    failed |= _storeProxyMethods.getStateName == NULL;
    _storeProxyMethods.write =
        CStore_getMethodID(env, _storeProxyClass, "write",
        "(Lorg/rvpf/store/server/c/Values;[I)I", logger);
    failed |= _storeProxyMethods.write == NULL;
    _storeProxyMethods.delete =
        CStore_getMethodID(env, _storeProxyClass, "delete",
        "([I[J[I)I", logger);
    failed |= _storeProxyMethods.delete == NULL;
    _storeProxyMethods.interrupt =
        CStore_getMethodID(env, _storeProxyClass, "interrupt", "()V", logger);
    failed |= _storeProxyMethods.interrupt == NULL;
    _storeProxyMethods.unsubscribe =
        CStore_getMethodID(env, _storeProxyClass, "unsubscribe",
        "([I[I)I", logger);
    failed |= _storeProxyMethods.unsubscribe == NULL;
    _storeProxyMethods.releaseHandles =
        CStore_getMethodID(env, _storeProxyClass, "releaseHandles",
        "([I[I)I", logger);
    failed |= _storeProxyMethods.releaseHandles == NULL;
    _storeProxyMethods.disconnect =
        CStore_getMethodID(env, _storeProxyClass, "disconnect", "()I", logger);
    failed |= _storeProxyMethods.disconnect == NULL;
    _storeProxyMethods.dispose =
        CStore_getMethodID(env, _storeProxyClass, "dispose", "()V", logger);
    failed |= _storeProxyMethods.dispose == NULL;

    failed |= !CStore_loadClasses(env, logger);

    return !failed;
}

static void _unloadClasses(void)
{
    JNIEnv *env = _getJNIEnv();

    if (env) {
        CStore_unloadClasses(env);

        if (_storeProxyClass) {
            (*env)->DeleteGlobalRef(env, _storeProxyClass);
            _storeProxyClass = NULL;
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

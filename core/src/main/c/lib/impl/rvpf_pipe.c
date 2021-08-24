/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: rvpf_pipe.c 3961 2019-05-06 20:14:59Z SFB $
 */

/** RVPF pipe API implementation.
 *
 * See header file (.h) for API description.
 */
#include "rvpf_version.h"
#ifdef __vms
#pragma module RVPF_PIPE RVPF_VERSION_REVISION
#endif

#include "rvpf_pipe.h"

#include <ctype.h>
#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

// Private macro definitions.

#define ENGINE_REQUEST_FORMAT_VERSION 1
#define INITIAL_CONTROL_BUFFER_CAPACITY 128
#define SINK_DELETE_REQUEST_TYPE "-"
#define SINK_REQUEST_FORMAT_VERSION 1
#define SINK_UPDATE_REQUEST_TYPE "+"

// Private structure definitions.

typedef struct rvpf_pipe_buffer {
    char *at;
    size_t mark;
    size_t position;
    size_t limit;
    size_t capacity;
} *RVPF_PIPE_Buffer;

typedef struct rvpf_pipe_request {
    char *requestID;
    int version;
    struct rvpf_pipe_buffer buffer;
} *RVPF_PIPE_Request;

struct rvpf_pipe_engineRequest {
    struct rvpf_pipe_request control;
    struct {
        char **at;
        int count;
    } transformParams;
    struct {
        char **at;
        int count;
    } pointParams;
    struct {
        struct rvpf_pipe_pointValue *at;
        int count;
    } inputs;
    struct rvpf_pipe_pointValue *result;
    struct {
        struct rvpf_pipe_pointValues {
            struct rvpf_pipe_pointValue result;
            struct rvpf_pipe_pointValues *next;
        } *first, *last;
        int count;
    } results;
};

struct rvpf_pipe_sinkRequest {
    struct rvpf_pipe_request control;
    enum rvpf_pipe_sinkRequestType requestType;
    struct rvpf_pipe_pointValue pointValue;
};

// Public variable definitions.

jmp_buf rvpf_pipe_jmp_buf;

char *rvpf_pipe_sink_request_types[] = {
    "Update", "Delete"
};

int rvpf_pipe_status = RVPF_PIPE_STATUS_OK;

// Private variable definitions.

static const char _deletedState[] = "DELETED";
static const char _nullRequestMessage[] = "Null request!";

// Private forward declarations.

static void *_alloc(size_t size);
static void _charToBuffer(char c, RVPF_PIPE_Buffer buffer);
static void _clearControl(RVPF_PIPE_Request control);
static void _clearPointValue(RVPF_PIPE_PointValue);
static char *_cloneString(const char *string);
static void _expandBuffer(RVPF_PIPE_Buffer buffer, size_t size);
static void _fillPointValue(
    RVPF_PIPE_PointValue pointValue,
    RVPF_PIPE_Buffer buffer,
    bool stampRequired);
static char *_firstLine(RVPF_PIPE_Buffer buffer);
static void _flushBuffer(RVPF_PIPE_Buffer buffer);
static void _freeEngineRequest(RVPF_PIPE_EngineRequest request);
static void _freeSinkRequest(RVPF_PIPE_SinkRequest request);
static void _intToBuffer(int value, RVPF_PIPE_Buffer buffer);
static char *_nextField(RVPF_PIPE_Buffer buffer, bool required, bool last);
static char *_nextLine(RVPF_PIPE_Buffer buffer, bool required);
static void _pointValueToBuffer(
    RVPF_PIPE_PointValue pointValue,
    RVPF_PIPE_Buffer buffer);
static char *_requestID(RVPF_PIPE_Buffer buffer);
static void _resetBuffer(RVPF_PIPE_Buffer buffer);
static void _stop(void);
static void _stringToBuffer(const char *text, RVPF_PIPE_Buffer buffer);
static int _stringToInt(const char *string);

// Public function definitions.

extern void rvpf_pipe_addEngineResult(
    RVPF_PIPE_EngineRequest request,
    const char *pointName,
    const char *stamp,
    const char *state,
    const char *value)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }
    if (!pointName || !*pointName) {
        rvpf_pipe_error("Missing point name");
    }
    if (!stamp || !*stamp) {
        rvpf_pipe_error("Missing time stamp");
    }

    struct rvpf_pipe_pointValues *pointValues =
        _alloc(sizeof(struct rvpf_pipe_pointValues));
    char *space;

    pointValues->result.pointName = _cloneString(pointName);
    pointValues->result.stamp = _cloneString(stamp);
    while ((space = strchr(pointValues->result.stamp, ' '))) {
        *space = 'T';
    }
    pointValues->result.state = _cloneString(state);
    pointValues->result.value = _cloneString(value);

    if (request->results.first) {
        request->results.last->next = pointValues;
        request->results.last = pointValues;
    } else {
        request->results.last = request->results.first = pointValues;
    }
    ++request->results.count;
}

extern void rvpf_pipe_clearEngineResults(RVPF_PIPE_EngineRequest request)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    if (request->result) {
        _clearPointValue(request->result);
        free(request->result);
        request->result = NULL;
    }

    request->results.last = NULL;
    while (request->results.first) {
        struct rvpf_pipe_pointValues *results = request->results.first;

        request->results.first = results->next;
        results->next = NULL;
        _clearPointValue(&results->result);
        free(results);
    }
}

extern void rvpf_pipe_debug(const char *format, ...)
{
    va_list ap;

    va_start(ap, format);
    rvpf_log(RVPF_LOG_LEVEL_DEBUG, format, ap);
    va_end(ap);
}

extern void rvpf_pipe_endEngineRequest(RVPF_PIPE_EngineRequest request)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    int summary;

    if (request->result) {
        if (request->result->value || request->results.count) {
            summary = 1 + request->results.count;
        } else {
            summary = 0;
        }
    } else if (request->results.count) {
        summary = request->results.count;
    } else {
        summary = -1;
    }

    _resetBuffer(&request->control.buffer);

    _stringToBuffer(request->control.requestID, &request->control.buffer);
    _stringToBuffer(" ", &request->control.buffer);
    _intToBuffer(summary, &request->control.buffer);
    _flushBuffer(&request->control.buffer);

    if (summary > 0) {
        for (struct rvpf_pipe_pointValues *next = request->results.first;
                next; next = next->next) {
            _pointValueToBuffer(&next->result, &request->control.buffer);
            _flushBuffer(&request->control.buffer);
        }
        if (request->result) {
            _pointValueToBuffer(request->result, &request->control.buffer);
            _flushBuffer(&request->control.buffer);
        }
    }

    _freeEngineRequest(request);
}

extern void rvpf_pipe_endSinkRequest(RVPF_PIPE_SinkRequest request, int summary)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    _resetBuffer(&request->control.buffer);

    _stringToBuffer(request->control.requestID, &request->control.buffer);
    _stringToBuffer(" ", &request->control.buffer);
    _intToBuffer(summary, &request->control.buffer);
    _flushBuffer(&request->control.buffer);

    _freeSinkRequest(request);
}

extern void rvpf_pipe_error(const char *format, ...)
{
    va_list ap;

    va_start(ap, format);
    rvpf_log(RVPF_LOG_LEVEL_ERROR, format, ap);
    va_end(ap);
    rvpf_pipe_status = RVPF_PIPE_STATUS_ERROR;
    longjmp(rvpf_pipe_jmp_buf, 1);
}

extern void rvpf_pipe_fatal(const char *format, ...)
{
    va_list ap;

    va_start(ap, format);
    rvpf_log(RVPF_LOG_LEVEL_FATAL, format, ap);
    va_end(ap);
    rvpf_pipe_status = RVPF_PIPE_STATUS_FATAL;
    longjmp(rvpf_pipe_jmp_buf, 1);
}

extern RVPF_PIPE_PointValue rvpf_pipe_getEngineInput(
    RVPF_PIPE_EngineRequest request,
    int position)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    return (position && position <= request->inputs.count)?
        request->inputs.at + position - 1: NULL;
}

extern int rvpf_pipe_getEngineInputsCount(RVPF_PIPE_EngineRequest request)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    return request->inputs.count;
}

extern char *rvpf_pipe_getEnginePointParam(
    RVPF_PIPE_EngineRequest request,
    int position)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    return (position && position <= request->pointParams.count)?
        request->pointParams.at[position - 1]: NULL;
}

extern int rvpf_pipe_getEnginePointParamsCount(RVPF_PIPE_EngineRequest request)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    return request->pointParams.count;
}

extern char *rvpf_pipe_getEngineRequestID(RVPF_PIPE_EngineRequest request)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    return request->control.requestID;
}

extern RVPF_PIPE_PointValue rvpf_pipe_getEngineResult(RVPF_PIPE_EngineRequest request)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    return request->result;
}

extern char *rvpf_pipe_getEngineTransformParam(
    RVPF_PIPE_EngineRequest request,
    int position)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    return (position && position <= request->transformParams.count)?
        request->transformParams.at[position - 1]: NULL;
}

extern int rvpf_pipe_getEngineTransformParamsCount(RVPF_PIPE_EngineRequest request)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    return request->transformParams.count;
}

extern char *rvpf_pipe_getSinkRequestID(RVPF_PIPE_SinkRequest request)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    return request->control.requestID;
}

extern RVPF_PIPE_SinkRequestType rvpf_pipe_getSinkRequestType(
    RVPF_PIPE_SinkRequest request)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    return request->requestType;
}

extern void rvpf_pipe_info(const char *format, ...)
{
    va_list ap;

    va_start(ap, format);
    rvpf_log(RVPF_LOG_LEVEL_INFO, format, ap);
    va_end(ap);
}

extern bool rvpf_pipe_isValueDeleted(const RVPF_PIPE_PointValue pointValue)
{
    return pointValue && pointValue->state == _deletedState;
}

extern RVPF_PIPE_EngineRequest rvpf_pipe_nextEngineRequest(void)
{
    RVPF_PIPE_EngineRequest request =
        _alloc(sizeof(struct rvpf_pipe_engineRequest));

    request->control.buffer.capacity = INITIAL_CONTROL_BUFFER_CAPACITY;
    request->control.buffer.at = _alloc(request->control.buffer.capacity);

    if (!_firstLine(&request->control.buffer)) {
        _freeEngineRequest(request);
        _stop();
    }

    request->control.requestID = _requestID(&request->control.buffer);

    request->control.version =
        _stringToInt(_nextField(&request->control.buffer, true, false));
    if (request->control.version > ENGINE_REQUEST_FORMAT_VERSION) {
        rvpf_pipe_error(
            "Unsupported request format version: %s",
            request->control.version);
    }
    request->transformParams.count =
        _stringToInt(_nextField(&request->control.buffer, true, false));
    request->pointParams.count =
        _stringToInt(_nextField(&request->control.buffer, true, false));
    request->inputs.count =
        _stringToInt(_nextField(&request->control.buffer, true, false));

    _nextLine(&request->control.buffer, true);
    request->result = _alloc(sizeof(struct rvpf_pipe_pointValue));
    _fillPointValue(request->result, &request->control.buffer, true);

    if (request->transformParams.count) {
        request->transformParams.at =
            _alloc(sizeof(char *) * request->transformParams.count);
        for (int i = 0; i < request->transformParams.count; ++i) {
            request->transformParams.at[i] =
                _cloneString(_nextLine(&request->control.buffer, true));
        }
    }

    if (request->pointParams.count) {
        request->pointParams.at =
            _alloc(sizeof(char *) * request->pointParams.count);
        for (int i = 0; i < request->pointParams.count; ++i) {
            request->pointParams.at[i] =
                _cloneString(_nextLine(&request->control.buffer, true));
        }
    }

    if (request->inputs.count) {
        request->inputs.at =
            _alloc(sizeof(struct rvpf_pipe_pointValue) * request->inputs.count);
        for (int i = 0; i < request->inputs.count; ++i) {
            _nextLine(&request->control.buffer, true);
            _fillPointValue(
                request->inputs.at + i,
                &request->control.buffer,
                false);
        }
    }

    return request;
}

extern RVPF_PIPE_SinkRequest rvpf_pipe_nextSinkRequest(void)
{
    RVPF_PIPE_SinkRequest request =
        _alloc(sizeof(struct rvpf_pipe_sinkRequest));
    char *field;

    request->control.buffer.capacity = INITIAL_CONTROL_BUFFER_CAPACITY;
    request->control.buffer.at = _alloc(request->control.buffer.capacity);

    if (!_firstLine(&request->control.buffer)) {
        _freeSinkRequest(request);
        _stop();
    }

    request->control.requestID = _requestID(&request->control.buffer);

    request->control.version =
        _stringToInt(_nextField(&request->control.buffer, true, false));
    if (request->control.version > SINK_REQUEST_FORMAT_VERSION) {
        rvpf_pipe_error(
            "Unsupported request format version: %s",
            request->control.version);
    }
    field = _nextField(&request->control.buffer, true, false);
    if (!strcmp(field, SINK_UPDATE_REQUEST_TYPE)) {
        request->requestType = RVPF_PIPE_SINK_UPDATE;
    } else if (!strcmp(field, SINK_DELETE_REQUEST_TYPE)) {
        request->requestType = RVPF_PIPE_SINK_DELETE;
    } else {
        rvpf_pipe_error("Unsupported request type '%s'", field);
    }

    _nextLine(&request->control.buffer, true);
    _fillPointValue(
        &request->pointValue, &request->control.buffer,
        request->requestType == RVPF_PIPE_SINK_UPDATE);

    return request;
}

extern RVPF_PIPE_PointValue rvpf_pipe_getSinkPointValue(
    RVPF_PIPE_SinkRequest request)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }

    return &request->pointValue;
}

extern void rvpf_pipe_setLogLevel(int level)
{
	rvpf_log_setLevel(level);
}

extern void rvpf_pipe_setEngineResultState(
    RVPF_PIPE_EngineRequest request,
    const char *state)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }
    if (!request->result) {
        rvpf_pipe_error("Can't set the state of a cleared result!");
    }

    if (request->result->state) {
        free(request->result->state);
    }
    request->result->state = _cloneString(state);
}

extern void rvpf_pipe_setEngineResultValue(
    RVPF_PIPE_EngineRequest request,
    const char *value)
{
    if (!request) {
        rvpf_pipe_fatal(_nullRequestMessage);
    }
    if (!request->result) {
        rvpf_pipe_error("Can't set the value of a cleared result!");
    }

    if (request->result->value) {
        free(request->result->value);
    }
    request->result->value = _cloneString(value);
}

extern void rvpf_pipe_trace(const char *format, ...)
{
    va_list ap;

    va_start(ap, format);
    rvpf_log(RVPF_LOG_LEVEL_TRACE, format, ap);
    va_end(ap);
}

extern char *rvpf_pipe_version(void)
{
	rvpf_log_setLevel(-RVPF_LOG_LEVEL_TRACE);

    return "RVPF_PIPE " RVPF_VERSION_REVISION;
}

extern void rvpf_pipe_warn(const char *format, ...)
{
    va_list ap;

    va_start(ap, format);
    rvpf_log(RVPF_LOG_LEVEL_WARN, format, ap);
    va_end(ap);
}

// Private function definitions.

static void *_alloc(size_t size)
{
    void *memory = calloc(1, size);

    if (memory == NULL) {
        rvpf_pipe_fatal("Failed to allocate %s bytes!", size);
    }

    return memory;
}

static void _charToBuffer(char c, RVPF_PIPE_Buffer buffer)
{
    _expandBuffer(buffer, 2);
    buffer->at[buffer->limit++] = c;
    buffer->at[buffer->limit] = '\0';
}

static void _clearControl(RVPF_PIPE_Request control)
{
    if (control->requestID) {
        free(control->requestID);
        control->requestID = NULL;
    }

    if (control->buffer.at) {
        free(control->buffer.at);
        control->buffer.at = NULL;
    }
}

static void _clearPointValue(RVPF_PIPE_PointValue pointValue)
{
    if (pointValue->pointName) {
        free(pointValue->pointName);
        pointValue->pointName = NULL;
    }

    if (pointValue->stamp) {
        free(pointValue->stamp);
        pointValue->stamp = NULL;
    }

    if (pointValue->state) {
        if (pointValue->state != _deletedState) {
            free(pointValue->state);
        }
        pointValue->state = NULL;
    }

    if (pointValue->value) {
        free(pointValue->value);
        pointValue->value = NULL;
    }
}

static char *_cloneString(const char *string)
{
    return string != NULL? strcpy(_alloc(strlen(string) + 1), string): NULL;
}

static void _expandBuffer(RVPF_PIPE_Buffer buffer, size_t size)
{
    if (size > buffer->capacity - buffer->limit) {
        if (size < buffer->capacity) {
            size = buffer->capacity;
        }
        size += size;
        buffer->at = realloc(buffer->at, size);
        memset(buffer->at + buffer->capacity, 0, size - buffer->capacity);
        buffer->capacity = size;
    }
}

static void _fillPointValue(
    RVPF_PIPE_PointValue pointValue,
    RVPF_PIPE_Buffer buffer,
    bool stampRequired)
{
    char *field;

    pointValue->pointName = _cloneString(_nextField(buffer, true, false));

    field = _nextField(buffer, stampRequired, false);
    if (field) {
        pointValue->stamp = _cloneString(field);
        field = _nextField(buffer, false, true);
    }

    if (field && *field == '[') {
        char *start = field + 1;
        char *next = start;
        char *stop = next;
        bool leftBracketSeen = false;
        bool rightBracketSeen = false;

        while (true) {
            if (!*next) {
                if (!rightBracketSeen) {
                    rvpf_pipe_warn(
                        "Invalid format for state field: %s", field);
                    return;
                }
                *stop = '\0';
                break;
            }

            if (rightBracketSeen) {
                if (*next == '[') {
                    *stop++ = '[';
                    rightBracketSeen = false;
                } else {
                    *stop = '\0';
                    ++next;
                    next += strspn(next, " ");
                    break;
                }
            } else if (leftBracketSeen) {
                if (*next == ']') {
                    *stop++ = ']';
                    leftBracketSeen = false;
                } else {
                    rvpf_pipe_warn(
                        "Invalid format for state field: %s", field);
                    return;
                }
            } else if (*next == '[') leftBracketSeen = true;
            else if (*next == ']') rightBracketSeen = true;
            else *stop++ = *next;

            ++next;
        }
        pointValue->state = _cloneString(start);
        field = next;
    }

    if (field) {
        if (*field == '"') {
            char *start = field + 1;
            char *next = start;
            char *stop = next;
            bool quoteSeen = false;

            while (true) {
                if (!*next) {
                    *stop = '\0';
                    if (!quoteSeen) {
                        rvpf_pipe_warn(
                            "Invalid format for value field: %s", field);
                        return;
                    }
                    break;
                }

                if (quoteSeen) {
                    if (*next == '"') {
                        *stop++ = '"';
                        quoteSeen = false;
                    } else {
                        *stop = '\0';
                        rvpf_pipe_warn(
                            "Invalid format for value field: %s", field);
                        return;
                    }
                } else if (*next == '"') quoteSeen = true;
                else *stop++ = *next;

                ++next;
            }
            pointValue->value = _cloneString(start);
        } else if (*field == '-') {
            if (pointValue->state) {
                free(pointValue->state);
            }
            pointValue->state = (char *) _deletedState;
        }
    }
}

static char *_firstLine(RVPF_PIPE_Buffer buffer)
{
    char *line;

    while (true) {
        line = _nextLine(buffer, false);

        if (line == NULL || strchr(line, ' ')) break;
        if (!strcmp(line, "0")) {
            line = NULL;
            break;
        }
        _flushBuffer(buffer);
    }

    return line;
}

static void _flushBuffer(RVPF_PIPE_Buffer buffer)
{
    fputs(buffer->at, stdout);
    fputs("\n", stdout);
    fflush(stdout);

    rvpf_log_trace("Sent: {%s}", buffer->at);
    _resetBuffer(buffer);
}

static void _freeEngineRequest(RVPF_PIPE_EngineRequest request)
{
    if (request->transformParams.at) {
        for (int i = 0; i < request->transformParams.count; ++i) {
            free(request->transformParams.at[i]);
            request->transformParams.at[i] = NULL;
        }
        free(request->transformParams.at);
        request->transformParams.at = NULL;
    }

    if (request->pointParams.at) {
        for (int i = 0; i < request->pointParams.count; ++i) {
            free(request->pointParams.at[i]);
            request->pointParams.at[i] = NULL;
        }
        free(request->pointParams.at);
        request->pointParams.at = NULL;
    }

    if (request->inputs.at) {
        for (int i = 0; i < request->inputs.count; ++i) {
            _clearPointValue(request->inputs.at + i);
        }
        free(request->inputs.at);
        request->inputs.at = NULL;
    }

    rvpf_pipe_clearEngineResults(request);

    _clearControl(&request->control);

    free(request);
}

static void _freeSinkRequest(RVPF_PIPE_SinkRequest request)
{
    _clearPointValue(&request->pointValue);

    _clearControl(&request->control);

    free(request);
}

static void _intToBuffer(int value, RVPF_PIPE_Buffer buffer)
{
    char digits[12];

    snprintf(digits, sizeof(digits), "%i", value);

    _stringToBuffer(digits, buffer);
}

static char *_nextField(RVPF_PIPE_Buffer buffer, bool required, bool last)
{
    if (buffer->position >= buffer->limit) {
        if (required) {
            rvpf_pipe_error("Unexpected request format");
        }
        buffer->mark = buffer->position;
        return NULL;
    }

    if (!buffer->at[buffer->position]) {
        ++buffer->position;
    }
    buffer->position += strspn(buffer->at + buffer->position, " ");
    buffer->mark = buffer->position;

    if (!last) {
        buffer->position += strcspn(buffer->at + buffer->position, " ");
        if (buffer->position < buffer->limit) {
            buffer->at[buffer->position] = '\0';
        }
    }
    if (rvpf_log_getLevel() >= RVPF_LOG_LEVEL_ALL) {
        rvpf_log_trace("Field: {%s}", buffer->at + buffer->mark);
    }

    return buffer->at + buffer->mark;
}

static char *_nextLine(RVPF_PIPE_Buffer buffer, bool required)
{
    _resetBuffer(buffer);

    while (true) {
        int next = fgetc(stdin);

        if (next == EOF) {
            if (buffer->limit) {
                rvpf_pipe_warn("Lost characters at end of input");
                buffer->limit = 0;
            }
            if (required) {
                rvpf_pipe_error("Unexpected end of input");
            }
            return NULL;
        }

        if (next == '\n') {
            while (buffer->limit && isspace((unsigned char) buffer->at[buffer->limit - 1])) {
                --buffer->limit; // Drops trailing spaces.
            }
            if (buffer->limit) {
                break;
            }
            continue;
        }

        if (next != '\r') {
            if (buffer->limit || !isspace(next)) {
                _expandBuffer(buffer, 2); // Includes the final nul.
                buffer->at[buffer->limit++] = next;
            }
        }
    }
    buffer->at[buffer->limit] = '\0';
    rvpf_log_trace("Received: {%s}", buffer->at);

    return buffer->at;
}

static void _pointValueToBuffer(
    RVPF_PIPE_PointValue pointValue,
    RVPF_PIPE_Buffer buffer)
{
    _stringToBuffer(pointValue->pointName, buffer);
    _charToBuffer(' ', buffer);
    _stringToBuffer(pointValue->stamp, buffer);

    if (pointValue->state) {
        size_t length = strlen(pointValue->state);

        _stringToBuffer(" [", buffer);
        for (int i = 0; i < length; ++i) {
            char c = pointValue->state[i];

            if (c == '[') _charToBuffer(']', buffer);
            else if (c == ']') _charToBuffer('[', buffer);
            _charToBuffer(c, buffer);
        }
        _charToBuffer(']', buffer);
    }

    if (pointValue->value) {
        size_t length = strlen(pointValue->value);

        _stringToBuffer(" \"", buffer);
        for (int i = 0; i < length; ++i) {
            char c = pointValue->value[i];

            if (c == '"') _charToBuffer('"', buffer);
            _charToBuffer(c, buffer);
        }
        _charToBuffer('"', buffer);
    }
}

static char *_requestID(RVPF_PIPE_Buffer buffer)
{
    char *requestID = _nextField(buffer, true, false);

    return _cloneString(requestID);
}

static void _resetBuffer(RVPF_PIPE_Buffer buffer)
{
    buffer->limit = buffer->position = buffer->mark = 0;
}

static void _stop(void)
{
    rvpf_pipe_status = RVPF_PIPE_STATUS_OK;
    longjmp(rvpf_pipe_jmp_buf, 1);
}

static void _stringToBuffer(const char *text, RVPF_PIPE_Buffer buffer)
{
    size_t length = strlen(text);

    _expandBuffer(buffer, length + 1);
    strcpy(buffer->at + buffer->limit, text);
    buffer->limit += length;
}

static int _stringToInt(const char *string)
{
    char *end;
    int value;

    value = strtol(string, &end, 10);
    if (end - string != strlen(string)) {
        rvpf_pipe_error("Bad decimal string '%s'", string);
    }

    return value;
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

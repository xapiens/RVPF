/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: rvpf_xpvpc.c 3961 2019-05-06 20:14:59Z SFB $
 */

/** RVPF XML point values port client API implementation.
 *
 * See header file (.h) for API description.
 */

#include "rvpf_version.h"
#ifdef __vms
#pragma module RVPF_XPVPC RVPF_VERSION_REVISION
#endif

#include "rvpf_xpvpc.h"
#include "rvpf_mem.h"

#include <assert.h>
#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>

// Private macro definitions.

#define MIN_BUFFER_SIZE 256

#define CLIENT_ATTRIBUTE "client"
#define DATA_ELEMENT "data"
#define DELETED_VALUE_ELEMENT "deleted-value"
#define FLUSH_ATTRIBUTE "flush"
#define ID_ATTRIBUTE "id"
#define LOGIN_ELEMENT "login"
#define MESSAGES_ELEMENT "messages"
#define PASSWORD_ATTRIBUTE "password"
#define POINT_ELEMENT "point"
#define POINT_VALUE_ELEMENT "point-value"
#define STAMP_ELEMENT "stamp"
#define STATE_ELEMENT "state"
#define USER_ATTRIBUTE "user"
#define VALUE_ELEMENT "value"

#define RESPONSE_START "<done ref='"
#define RESPONSE_END "'/>"

// Context definition.

struct rvpf_xpvpc_context
{
    char *client;
    long long id;
    int pending;
    RVPF_SSL_Context ssl;
    char *buffer;
    size_t size;
    size_t limit;
    size_t position;
    int status;
};

// Private forward declarations.

static void _addChar(RVPF_XPVPC_Context context, char c);

static void _addEncoded(RVPF_XPVPC_Context context, char *text, char quote);

static void _addLong(RVPF_XPVPC_Context context, long long n);

static void _addText(RVPF_XPVPC_Context context, char *text);

static void _match(RVPF_XPVPC_Context context, char *text);

static void _receiveLine(RVPF_XPVPC_Context context);

static void _resetText(RVPF_XPVPC_Context context);

static void _sendText(RVPF_XPVPC_Context context);

static void _verifyResponse(RVPF_XPVPC_Context context, long long expectedId);

// Private storage.

static int _autoFlush = 0;

static char *_deletedState = "DELETED";

static char *_messages[] =
{
    "ok",
    "illegal state",
    "illegal argument",
    "internal error",
    "unexpected response",
    "mismatched id",
    "allocation failed",
    "unknown error"
};

// Public function definitions.

extern void rvpf_xpvpc_clearError(RVPF_XPVPC_Context context)
{
    assert(context);

    _resetText(context);
    context->pending = 0;
    context->status = RVPF_XPVPC_OK;
    rvpf_ssl_clearError(context->ssl);
}

extern int rvpf_xpvpc_close(RVPF_XPVPC_Context context)
{
    assert(context);

    if (!rvpf_ssl_isOpen(context->ssl)) {
        rvpf_xpvpc_clearError(context);
        return context->status;
    }

    if (rvpf_xpvpc_succeeded(context)) rvpf_xpvpc_flush(context);
    else context->status = RVPF_XPVPC_OK;

    return rvpf_ssl_close(context->ssl);
}

extern RVPF_XPVPC_Context rvpf_xpvpc_create(void)
{
    RVPF_XPVPC_Context context =
        RVPF_MEM_ALLOCATE(sizeof(struct rvpf_xpvpc_context));

    context->ssl = rvpf_ssl_create();

    context->size = MIN_BUFFER_SIZE;
    context->buffer = RVPF_MEM_ALLOCATE(context->size);

    context->status = RVPF_XPVPC_OK;

    return context;
}

extern char *rvpf_xpvpc_deletedState(void)
{
    return _deletedState;
}

extern void rvpf_xpvpc_dispose(RVPF_XPVPC_Context context)
{
    if (context) {
        rvpf_xpvpc_close(context);
        rvpf_xpvpc_setClient(context, NULL);

        RVPF_MEM_FREE(context->buffer);
        context->buffer = NULL;
        rvpf_ssl_dispose(context->ssl);
        context->ssl = NULL;
        RVPF_MEM_FREE(context);
    }
}

extern char *rvpf_xpvpc_errorMessage(RVPF_XPVPC_Context context)
{
    assert(context);

    char *message = NULL;
    int status = context->status;

    if (context->status != RVPF_XPVPC_OK) {
        if (status < 0 || status >= RVPF_XPVPC_STATUS_CODES) {
            status = RVPF_XPVPC_UNKNOWN_ERROR;
        }
    } else {
        message = rvpf_ssl_errorMessage(context->ssl);
    }

    if (status != RVPF_XPVPC_OK) {
        message = _messages[status];
    }

    return message;
}

extern bool rvpf_xpvpc_failed(RVPF_XPVPC_Context context)
{
    assert(context);

    return context->status != RVPF_XPVPC_OK
        || rvpf_ssl_failed(context->ssl);
}

extern int rvpf_xpvpc_flush(RVPF_XPVPC_Context context)
{
    if (rvpf_xpvpc_failed(context)) {
        return rvpf_xpvpc_status(context);
    }
    if (!rvpf_xpvpc_isOpen(context)) {
        context->status = RVPF_XPVPC_ILLEGAL_STATE;
        return context->status;
    }

    if (context->pending) {
        _addText(context, "</");
        _addText(context, MESSAGES_ELEMENT);
        _addText(context, ">\n");

        _sendText(context);
        _verifyResponse(context, context->id);

        context->pending = 0;
    }

    return context->status;
}

extern bool rvpf_xpvpc_isOpen(RVPF_XPVPC_Context context)
{
    assert(context);

    return rvpf_ssl_isOpen(context->ssl);
}

extern int rvpf_xpvpc_login(
        RVPF_XPVPC_Context context,
        char *user, char *password)
{
    if (rvpf_xpvpc_failed(context)) {
        return rvpf_xpvpc_status(context);
    }
    if (!rvpf_xpvpc_isOpen(context)) {
        context->status = RVPF_XPVPC_ILLEGAL_STATE;
        return context->status;
    }

    rvpf_xpvpc_flush(context);

    _addChar(context, '<');
    _addText(context, LOGIN_ELEMENT);
    _addChar(context, ' ');
    if (context->client) {
        _addText(context, CLIENT_ATTRIBUTE);
        _addText(context, "='");
        _addEncoded(context, context->client, '\'');
        _addText(context, "' ");
    }
    _addText(context, ID_ATTRIBUTE);
    _addText(context, "='");
    _addLong(context, ++context->id);
    _addText(context, "' ");
    _addText(context, USER_ATTRIBUTE);
    _addText(context, "='");
    _addEncoded(context, user, '\'');
    _addText(context, "' ");
    _addText(context, PASSWORD_ATTRIBUTE);
    _addText(context, "='");
    _addEncoded(context, password, '\'');
    _addText(context, "'/>\n");

    _sendText(context);
    _verifyResponse(context, context->id);

    return context->status;
}

extern int rvpf_xpvpc_open(RVPF_XPVPC_Context context, char *address)
{
    assert(context);

    if (rvpf_ssl_isOpen(context->ssl)) {
        context->status = RVPF_XPVPC_ILLEGAL_STATE;
        return context->status;
    }

    context->status = RVPF_XPVPC_OK;

    return rvpf_ssl_open(context->ssl, address);
}

extern bool rvpf_xpvpc_printError(RVPF_XPVPC_Context context, char *prefix)
{
    char *message = rvpf_xpvpc_errorMessage(context);

    if (message == NULL) {
        return false;
    }

    if (prefix && *prefix) {
        fprintf(stderr, "%s ", prefix);
    }
    fprintf(stderr, "%s\n", message);

    return true;
}

extern int rvpf_xpvpc_sendValue(
    RVPF_XPVPC_Context context,
    char *point,
    char *stamp,
    char *state,
    char *value)
{
    if (rvpf_xpvpc_failed(context)) {
        return rvpf_xpvpc_status(context);
    }
    if (!rvpf_xpvpc_isOpen(context)) {
        context->status = RVPF_XPVPC_ILLEGAL_STATE;
        return context->status;
    }
    if (!point || !stamp) {
        context->status = RVPF_XPVPC_ILLEGAL_ARG;
        return context->status;
    }

    if (!context->pending) {
        _addChar(context, '<');
        _addText(context, MESSAGES_ELEMENT);
        _addChar(context, ' ');

        _addText(context, ID_ATTRIBUTE);
        _addText(context, "='");
        _addLong(context, ++context->id);
        _addText(context, "' ");

        _addText(context, FLUSH_ATTRIBUTE);
        _addText(context, "='yes'>\n");
    }

    _addText(context, " <");
    _addText(context,
        state == _deletedState?
            DELETED_VALUE_ELEMENT: POINT_VALUE_ELEMENT);
    _addText(context, ">\n");

    _addText(context, "  <");
    _addText(context, POINT_ELEMENT);
    _addChar(context, '>');
    _addEncoded(context, point, '\0');
    _addText(context, "</");
    _addText(context, POINT_ELEMENT);
    _addText(context, ">\n");

    _addText(context, "  <");
    _addText(context, STAMP_ELEMENT);
    _addChar(context, '>');
    _addEncoded(context, stamp, '\0');
    _addText(context, "</");
    _addText(context, STAMP_ELEMENT);
    _addText(context, ">\n");

    if (state != _deletedState) {
        if (state) {
            _addText(context, "  <");
            _addText(context, STATE_ELEMENT);
            _addChar(context, '>');
            _addEncoded(context, state, '\0');
            _addText(context, "</");
            _addText(context, STATE_ELEMENT);
            _addText(context, ">\n");
        }

        if (value) {
            while (isspace((unsigned char) *value)) {
                ++value;
            }
            _addText(context, "  <");
            _addText(context, VALUE_ELEMENT);
            _addChar(context, '>');
            _addEncoded(context, value, '\0');
            _addText(context, "</");
            _addText(context, VALUE_ELEMENT);
            _addText(context, ">\n");
        }
    }

    _addText(context, " </");
    _addText(context,
        state == _deletedState?
            DELETED_VALUE_ELEMENT: POINT_VALUE_ELEMENT);
    _addText(context, ">\n");

    ++context->pending;
    if (_autoFlush > 0 && context->pending >= _autoFlush) {
        rvpf_xpvpc_flush(context);
    }

    return context->status;
}

extern void rvpf_xpvpc_setAutoFlush(RVPF_XPVPC_Context context, int autoFlush)
{
    if (rvpf_xpvpc_isOpen(context)) rvpf_xpvpc_flush(context);

    _autoFlush = autoFlush;
}

extern void rvpf_xpvpc_setClient(RVPF_XPVPC_Context context, char *client)
{
    assert(context);

    if (context->client) {
        RVPF_MEM_FREE(context->client);
        context->client = NULL;
    }

    if (client) {
        context->client = RVPF_MEM_ALLOCATE(strlen(client) + 1);
        strcpy(context->client, client);
    }
}

extern RVPF_SSL_Context rvpf_xpvpc_ssl(RVPF_XPVPC_Context context)
{
    assert(context);

    return context->ssl;
}

extern int rvpf_xpvpc_status(RVPF_XPVPC_Context context)
{
    assert(context);

    return context->status != RVPF_XPVPC_OK? context->status:
            rvpf_ssl_status(context->ssl);
}

extern bool rvpf_xpvpc_succeeded(RVPF_XPVPC_Context context)
{
    assert(context);

    return context->status == RVPF_XPVPC_OK
        && rvpf_ssl_succeeded(context->ssl);
}

extern char *rvpf_xpvpc_version(void)
{
    return "RVPF_XPVPC " RVPF_VERSION_REVISION;
}

// Private function definitions.

static void _addChar(RVPF_XPVPC_Context context, char c)
{
    if (context->status != RVPF_XPVPC_OK) {
        return;
    }

    if (context->position >= context->size - 1) {
        size_t size = context->size * 2;
        char *buffer = RVPF_MEM_REALLOCATE(context->buffer, size);

        memset(buffer + size, '\0', size);
        context->buffer = buffer;
        context->size = size;
    }

    context->buffer[context->position++] = c;
    context->buffer[context->position] = '\0';
}

static void _addEncoded(RVPF_XPVPC_Context context, char *text, char quote)
{
    char *start;
    char *end;

    if (context->status != RVPF_XPVPC_OK) {
        return;
    }

    if (!text) {
        return;
    }

    // Trims.

    while (isspace((unsigned char) *text)) {
        ++text;
    }
    start = text;

    while (*text) {
        ++text;
    }

    while (text != start) {
        if (!isspace((unsigned char) text[-1])) {
            break;
        }
        --text;
    }
    end = text;

    // Encodes.

    text = start;
    while (text != end) {
        char next = *text++;

        switch (next) {
        case '<':
            _addText(context, "&lt;");
            break;
        case '>':
            _addText(context, "&gt;");
            break;
        case '&':
            _addText(context, "&amp;");
            break;
        case '"':
            if (quote == next) {
                _addText(context, "&quot;");
            } else {
                _addChar(context, next);
            }
            break;
        case '\'':
            if (quote == next) {
                _addText(context, "&apos;");
            } else {
                _addChar(context, next);
            }
            break;
        case '\t':
        case '\n':
        case '\r':
            _addChar(context, next);
            break;
        default:
            if (next < ' ') {
                _addText(context, "&#");
                _addLong(context, next);
                _addChar(context, ';');
            } else {
                _addChar(context, next);
            }
            break;
        }
    }
}

static void _addLong(RVPF_XPVPC_Context context, long long n)
{
    char buffer[20];
    size_t position = 0;

    if (context->status != RVPF_XPVPC_OK) {
        return;
    }

    do {
        if (position >= sizeof(buffer)) {
            context->status = RVPF_XPVPC_INTERNAL_ERROR;
            return;
        }
        buffer[position++] = n % 10 + '0';
    } while ((n /= 10) > 0);

    do {
        _addChar(context, buffer[--position]);
    } while (position > 0);
}

static void _addText(RVPF_XPVPC_Context context, char *text)
{
    if (context->status != RVPF_XPVPC_OK) {
        return;
    }

    while (*text) {
        _addChar(context, *text++);
    }
}

static void _match(RVPF_XPVPC_Context context, char *text)
{
    while (*text) {
        if (context->position >= context->limit
                || context->buffer[context->position] != *text) {
            context->status = RVPF_XPVPC_UNEXPECTED_RESPONSE;
            break;
        }
        ++text;
        ++context->position;
    }
}

static void _receiveLine(RVPF_XPVPC_Context context)
{
    if (context->status != RVPF_XPVPC_OK) {
        return;
    }

    context->limit = 0;
    context->position = 0;
    while (context->limit < context->size - 1) {
        int count = rvpf_ssl_receive(
            context->ssl,
            context->buffer + context->limit,
            context->size - 1 - context->limit);

        if (rvpf_ssl_failed(context->ssl)) {
            return;
        }

        if (context->limit + count >= context->size - 1) {
            context->status = RVPF_XPVPC_INTERNAL_ERROR;
            return;
        }
        context->buffer[context->limit + count] = '\0';

        do {
            if (context->buffer[context->limit] == '\n') {
                context->buffer[context->limit] = '\0';
                return;
            }
            ++context->limit;
        } while (--count > 0);
    }

    if (context->limit >= context->size) {
        context->status = RVPF_XPVPC_UNEXPECTED_RESPONSE;
    }
}

static void _resetText(RVPF_XPVPC_Context context)
{
    context->limit = context->position;
    context->position = 0;
}

static void _sendText(RVPF_XPVPC_Context context)
{
    if (rvpf_xpvpc_failed(context)) {
        return;
    }

    _resetText(context);

    while (context->position < context->limit) {
        int count = rvpf_ssl_send(
            context->ssl,
            context->buffer + context->position,
            context->limit - context->position);

        if (rvpf_ssl_failed(context->ssl)) {
            break;
        }

        context->position += count;
    }
}

static void _verifyResponse(RVPF_XPVPC_Context context, long long expectedId)
{
    long long receivedId = 0;

    _receiveLine(context);
    if (context->status != RVPF_XPVPC_OK) {
        return;
    }

    _match(context, RESPONSE_START);
    if (context->status != RVPF_XPVPC_OK) {
        return;
    }

    while (context->position < context->limit) {
        if (!isdigit((unsigned char) context->buffer[context->position])) {
            break;
        }
        receivedId *= 10;
        receivedId += context->buffer[context->position++] - '0';
    }

    _match(context, RESPONSE_END);
    if (context->status != RVPF_XPVPC_OK) {
        return;
    }

    if (receivedId != expectedId) {
        context->status = RVPF_XPVPC_MISMATCHED_ID;
    }

    _resetText(context);
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

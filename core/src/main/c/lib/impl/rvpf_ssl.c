/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: rvpf_ssl.c 3961 2019-05-06 20:14:59Z SFB $
 */

/** RVPF SSL support API.
 *
 * See header file (.h) for API description.
 */
#include "rvpf_version.h"

#ifdef __vms
#pragma module RVPF_SSL RVPF_VERSION_REVISION
#endif

#include "rvpf_ssl.h"
#include "rvpf_log.h"
#include "rvpf_mem.h"

#include <assert.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#ifdef SSL_ENABLED
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <openssl/bio.h>
#else
#include <errno.h>
#ifdef _WIN32
#include <winsock2.h>
#else
#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>
#include <netinet/in.h>
#include <netdb.h>
#endif
#endif

// Context definition.

struct rvpf_ssl_context
{
    char *host;
    int port;
#ifdef SSL_ENABLED
    BIO *bio;
    SSL_CTX *ctx;
    SSL *ssl;
    char *trustFile;
    char *trustDirectory;
    char *certificateFile;
#else
    int socket;
#endif
    int status;
    char *errorMessage;
};

// Private forward declarations.

static void _initCTX(RVPF_SSL_Context context);
static void _saveString(
    char *source,
    size_t length,
    char **destination,
    RVPF_SSL_Context context);

// Private storage.

static char *_messages[] =
{
    "ok",
#ifdef SSL_ENABLED
    "ask 'ERR'",
#else
    "see 'errno'",
#endif
    "illegal state",
    "illegal argument",
    "internal error",
    "server closed",
    "allocation failed",
    "bad address",
    "unknown host",
    "untrusted host",
    "unknown error"
};

// Public function definitions.

extern void rvpf_ssl_clearError(RVPF_SSL_Context context)
{
    assert(context);

    context->status = RVPF_SSL_OK;
#ifdef SSL_ENABLED
    ERR_clear_error();
#endif
}

extern int rvpf_ssl_close(RVPF_SSL_Context context)
{
    assert(context);

#ifdef SSL_ENABLED
    if (context->ssl) {
        SSL_shutdown(context->ssl);
        SSL_free(context->ssl);
        context->ssl = NULL;
        context->bio = NULL;
    } else if (context->bio) {
        BIO_free(context->bio);
        context->bio = NULL;
    }
#else
    if (context->socket >= 0) {
        int status;

#ifdef _WIN32
        status = closesocket(context->socket);
#else
        status = close(context->socket);
#endif
        if (status == -1) {
            context->status = RVPF_SSL_ASK_ERR;
        }
        context->socket = -1;

#ifdef _WIN32
        status = WSAGetLastError();

        WSACleanup();
        WSASetLastError(status);
#endif
    }
#endif

    return context->status;
}

extern RVPF_SSL_Context rvpf_ssl_create(void)
{
    RVPF_SSL_Context context =
        RVPF_MEM_ALLOCATE(sizeof(struct rvpf_ssl_context));

#ifdef SSL_ENABLED
    SSL_load_error_strings();
    SSL_library_init();
#else
    context->socket = -1;
#endif

    context->status = RVPF_SSL_OK;

    return context;
}

extern void rvpf_ssl_dispose(RVPF_SSL_Context context)
{
    if (context) {
        rvpf_ssl_close(context);

#ifdef SSL_ENABLED
        if (context->ctx != NULL) {
            SSL_CTX_free(context->ctx);
            context->ctx = NULL;
        }
        ERR_free_strings();
        RVPF_MEM_FREE(context->certificateFile);
        context->certificateFile = NULL;
        RVPF_MEM_FREE(context->trustDirectory);
        context->trustDirectory = NULL;
        RVPF_MEM_FREE(context->trustFile);
        context->trustFile = NULL;
#endif

        RVPF_MEM_FREE(context->host);
        context->host = NULL;
        RVPF_MEM_FREE(context->errorMessage);
        context->errorMessage = NULL;
        RVPF_MEM_FREE(context);
    }
}

extern bool rvpf_ssl_enabled(void)
{
#ifdef SSL_ENABLED
    return true;
#else
    return false;
#endif
}

extern char *rvpf_ssl_errorMessage(RVPF_SSL_Context context)
{
    assert(context);

    char *message = NULL;
    int status = context->status;

    if (status < 0 || status >= RVPF_SSL_STATUS_CODES) {
        status = RVPF_SSL_UNKNOWN_ERROR;
    }

    if (status != RVPF_SSL_OK) {
        if (status == RVPF_SSL_ASK_ERR) {
            RVPF_MEM_FREE(context->errorMessage);
            context->errorMessage = NULL;
#ifdef SSL_ENABLED
            BIO *mem = BIO_new(BIO_s_mem());

            if (!mem) {
                rvpf_log_fatal_s(
                    __FILE__,
                    __LINE__,
                    "Failed to allocate BIO memory");
                exit(-1);
            }

            char *buffer;
            long length;

            ERR_print_errors(mem);
            length = BIO_get_mem_data(mem, &buffer);
            _saveString(buffer, length, &context->errorMessage, context);
            message = context->errorMessage;
            BIO_free(mem);
#else
#ifdef _WIN32
            char buffer[32];

            snprintf(
                buffer, sizeof(buffer),
                "winsock2 error: %i", WSAGetLastError());
            _saveString(
                buffer,
                strlen(buffer),
                &context->errorMessage, context);
            message = context->errorMessage;
#else
            message = strerror(errno);
#endif
#endif
        } else {
            message = _messages[status];
        }
    }

    return message;
}

extern bool rvpf_ssl_failed(RVPF_SSL_Context context)
{
    assert(context);

    return context->status != RVPF_SSL_OK;
}

extern bool rvpf_ssl_isOpen(RVPF_SSL_Context context)
{
    assert(context);

#ifdef SSL_ENABLED
    return context->bio;
#else
    return context->socket >= 0;
#endif
}

extern int rvpf_ssl_open(RVPF_SSL_Context context, char *address)
{
    assert(context);

    if (rvpf_ssl_isOpen(context)) {
        context->status = RVPF_SSL_ILLEGAL_STATE;
        return context->status;
    }

    context->status = RVPF_SSL_OK;

    char *pointer = address? strrchr(address, ':'): NULL;
    char *host;
    size_t length;

    if (!pointer) {
        context->status = RVPF_SSL_BAD_ADDRESS;
    } else if (pointer > address) {
        length = pointer - address;
        host = address;
    } else {
        host = "127.0.0.1";
        length = strlen(host);
    }

    if (rvpf_ssl_succeeded(context)) {
        ++pointer;
        context->port = strtoul(pointer, &pointer, 10);
        if (*pointer != '\0' || context->port == 0) {
            context->status = RVPF_SSL_BAD_ADDRESS;
        }
    }

    if (rvpf_ssl_succeeded(context)) {
        _saveString(host, length, &context->host, context);
    }

    if (rvpf_ssl_failed(context)) {
        return context->status;
    }

#ifdef SSL_ENABLED
    bool secure = context->ctx != NULL;
    bool verified =
        context->trustFile || context->trustDirectory;
    bool certified = context->certificateFile;

    context->bio = BIO_new(BIO_s_connect());
    if (!context->bio) {
        context->status = RVPF_SSL_ASK_ERR;
    }
    if (rvpf_ssl_succeeded(context)) {
        BIO_set_conn_hostname(context->bio, context->host);
        BIO_set_conn_int_port(context->bio, &context->port);
        if (BIO_do_connect(context->bio) <= 0) {
            context->status = RVPF_SSL_ASK_ERR;
        }
    }

    if (verified && rvpf_ssl_succeeded(context)) {
        if (!SSL_CTX_load_verify_locations(context->ctx,
                context->trustFile, context->trustDirectory)) {
            context->status = RVPF_SSL_ASK_ERR;
        }
    }
    if (certified && rvpf_ssl_succeeded(context)) {
        if (!SSL_CTX_use_certificate_chain_file(context->ctx,
                context->certificateFile)) {
            context->status = RVPF_SSL_ASK_ERR;
        }
    }
    if (certified && rvpf_ssl_succeeded(context)) {
        if (!SSL_CTX_use_PrivateKey_file(context->ctx,
                context->certificateFile, SSL_FILETYPE_PEM)) {
            context->status = RVPF_SSL_ASK_ERR;
        }
    }
    if (secure && rvpf_ssl_succeeded(context)) {
        if (verified) {
            SSL_CTX_set_verify(context->ctx, SSL_VERIFY_PEER, NULL);
        }
        if (!SSL_CTX_set_cipher_list(context->ctx,
                "ALL:!ADH:!LOW:!EXP:!MD5:@STRENGTH")) {
            context->status = RVPF_SSL_ASK_ERR;
        }
    }

    if (secure && rvpf_ssl_succeeded(context)) {
        context->ssl = SSL_new(context->ctx);
        if (!context->ssl) {
            context->status = RVPF_SSL_ASK_ERR;
        } else {
            SSL_set_bio(context->ssl, context->bio, context->bio);
        }
    }
    if (secure && rvpf_ssl_succeeded(context)) {
        if (SSL_connect(context->ssl) <= 0) {
            context->status = RVPF_SSL_ASK_ERR;
        } else if (verified) {
            X509 *certificate = SSL_get_peer_certificate(context->ssl);

            if (certificate) {
                X509_free(certificate);
            } else {
                context->status = RVPF_SSL_UNTRUSTED_HOST;
            }
        }
    }
#else
#ifdef _WIN32
    WSADATA wsaData;

    if (WSAStartup(MAKEWORD(2, 2), &wsaData)) {
        context->status = RVPF_SSL_ASK_ERR;
        return context->status;
    }
#endif
    context->socket = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (context->socket == -1) {
        context->status = RVPF_SSL_ASK_ERR;
    }

    if (rvpf_ssl_succeeded(context)) {
        struct sockaddr_in sin = {0};
        struct hostent *h = gethostbyname(context->host);

        if (h == NULL) {
            context->status = RVPF_SSL_UNKNOWN_HOST;
        } else {
            sin.sin_family = AF_INET;
            sin.sin_addr.s_addr = *((unsigned long *) h->h_addr_list[0]);
            sin.sin_port = htons(context->port);

            if (connect(
                    context->socket,
                    (struct sockaddr *) &sin,
                    sizeof(sin)) == -1) {
                context->status = RVPF_SSL_ASK_ERR;
            }
        }
    }
#endif

    if (rvpf_ssl_failed(context)) {
        rvpf_ssl_close(context);
    }

    return context->status;
}

extern bool rvpf_ssl_printError(RVPF_SSL_Context context, char *prefix)
{
    char *message = rvpf_ssl_errorMessage(context);

    if (!message) {
        return false;
    }

    if (prefix && *prefix != '\0') {
        fprintf(stderr, "%s ", prefix);
    }
    fprintf(stderr, "%s\n", message);

    return true;
}

extern int rvpf_ssl_receive(RVPF_SSL_Context context, char *buffer, size_t size)
{
    if (rvpf_ssl_failed(context)) {
        return 0;
    }
    if (!buffer || size < 1) {
        context->status = RVPF_SSL_ILLEGAL_ARG;
        return 0;
    }

    ssize_t count;

#ifdef SSL_ENABLED
    if (context->ssl) {
        count = SSL_read(context->ssl, buffer, size);
    } else {
        count = BIO_read(context->bio, buffer, size);
    }
#else
    count = recv(context->socket, buffer, size, 0);
#endif

    if (count < 0) {
        context->status = count == -1
            ? RVPF_SSL_ASK_ERR
            : RVPF_SSL_INTERNAL_ERROR;
    } else if (count == 0) {
        context->status = RVPF_SSL_SERVER_CLOSED;
    }

    return count;
}

extern int rvpf_ssl_send(RVPF_SSL_Context context, char *buffer, size_t size)
{
    if (rvpf_ssl_failed(context)) {
        return 0;
    }
    if (!buffer || size < 1) {
        context->status = RVPF_SSL_ILLEGAL_ARG;
        return 0;
    }

    ssize_t count;

#ifdef SSL_ENABLED
    if (context->ssl) {
        count = SSL_write(context->ssl, buffer, size);
    } else {
        count = BIO_write(context->bio, buffer, size);
    }
#else
    count = send(context->socket, buffer, size, 0);
#endif

    if (count <= 0) {
        context->status = count == -1
            ? RVPF_SSL_ASK_ERR
            : RVPF_SSL_INTERNAL_ERROR;
    }

    return count;
}

extern void rvpf_ssl_setCertificate(RVPF_SSL_Context context, char *filePath)
{
    if (rvpf_ssl_succeeded(context)) {
        _initCTX(context);
#ifdef SSL_ENABLED
        _saveString(filePath, filePath != NULL? strlen(filePath): 0,
            &context->certificateFile, context);
#endif
    }
}

extern void rvpf_ssl_setTrust(
        RVPF_SSL_Context context,
        char *filePath,
        char *directoryPath)
{
    if (rvpf_ssl_succeeded(context)) {
        _initCTX(context);
#ifdef SSL_ENABLED
        _saveString(filePath, filePath != NULL? strlen(filePath): 0,
            &context->trustFile, context);
        _saveString(
            directoryPath,
            directoryPath != NULL? strlen(directoryPath): 0,
            &context->trustDirectory, context);
#endif
    }
}

extern int rvpf_ssl_status(RVPF_SSL_Context context)
{
    assert(context);

    return -context->status;
}

extern bool rvpf_ssl_succeeded(RVPF_SSL_Context context)
{
    assert(context);

    return context->status == RVPF_SSL_OK;
}

extern char *rvpf_ssl_version(void)
{
    static char *version = NULL;

    if (!version) {
        const char *rvpfVersion = "RVPF_SSL " RVPF_VERSION_REVISION;
#ifdef SSL_ENABLED
        const char *opensslVersion = SSLeay_version(SSLEAY_VERSION);

        version = RVPF_MEM_ALLOCATE(strlen(rvpfVersion) + 2 + strlen(opensslVersion) + 2);
        strcpy(version, rvpfVersion);
        strcat(version, " (");
        strcat(version, opensslVersion);
        strcat(version, ")");
#else
        version = (char *) rvpfVersion;
#endif
    }

    return version;
}

// Private function definitions.

static void _initCTX(RVPF_SSL_Context context)
{
#ifdef SSL_ENABLED
    if (!context->ctx) {
        context->ctx = SSL_CTX_new(SSLv23_method());
        if (context->ctx != NULL) {
            SSL_CTX_set_options(context->ctx, SSL_OP_ALL|SSL_OP_NO_SSLv2);
        } else {
            context->status = RVPF_SSL_ASK_ERR;
        }
    }
#endif
}

static void _saveString(
        char *source,
        size_t length,
        char **destination,
        RVPF_SSL_Context context)
{
    RVPF_MEM_FREE(*destination);
    *destination = NULL;

    if (source) {
        *destination = RVPF_MEM_ALLOCATE(length + 1);
        strncpy(*destination, source, length);
        (*destination)[length] = '\0';
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

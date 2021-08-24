/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: rvpf_ssl.h 3961 2019-05-06 20:14:59Z SFB $
 */

/** RVPF SSL support API. */

#ifndef RVPF_SSL_H_
#define RVPF_SSL_H_

#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifdef __vms
#pragma names save
#pragma names as_is
#pragma names shortened
#endif

/** Returned status code values.
 */
enum {
    RVPF_SSL_OK, /* Success. */
    RVPF_SSL_ASK_ERR, /* Error detected by C or SSL library. */
    RVPF_SSL_ILLEGAL_STATE, /* Illegal state. */
    RVPF_SSL_ILLEGAL_ARG, /* Illegal argument. */
    RVPF_SSL_INTERNAL_ERROR, /* Internal error. */
    RVPF_SSL_SERVER_CLOSED, /* Server closed. */
    RVPF_SSL_BAD_ADDRESS, /* Bad address. */
    RVPF_SSL_UNKNOWN_HOST, /* Unknown host. */
    RVPF_SSL_UNTRUSTED_HOST, /* Untrusted host. */
    RVPF_SSL_UNKNOWN_ERROR, /* Unknown error. */
    RVPF_SSL_STATUS_CODES /* Count of status codes. */
};

/** SSL context.
 */
typedef struct rvpf_ssl_context *RVPF_SSL_Context;

/** Clears an error.
 *
 * @param context The context.
 */
extern void rvpf_ssl_clearError(RVPF_SSL_Context context);

/** Closes the connection.
 *
 * @param context The context.
 *
 * @return A status code.
 */
extern int rvpf_ssl_close(RVPF_SSL_Context context);

/** Creates a context.
 *
 * @return The context.
 */
extern RVPF_SSL_Context rvpf_ssl_create(void);

/** Disposes of the context.
 *
 * @param context The context.
 */
extern void rvpf_ssl_dispose(RVPF_SSL_Context context);

/** Asks if SSL is enabled.
 *
 * @return A true value if SSL is enabled.
 */
extern bool rvpf_ssl_enabled(void);

/** Returns the error message.
 *
 * @param context The context.
 *
 * @return The error message or NULL.
 */
extern char *rvpf_ssl_errorMessage(RVPF_SSL_Context context);

/** Asks if the last operation has failed.
 *
 * @param context The context.
 *
 * @return A true value if the last operation has failed.
 */
extern bool rvpf_ssl_failed(RVPF_SSL_Context context);

/** Asks if the connection is open.
 *
 * @param context The context.
 *
 * @return A true value if the connection is open.
 */
extern bool rvpf_ssl_isOpen(RVPF_SSL_Context context);

/** Opens a connection to the XML Port.
 *
 * @param context The context.
 * @param address The server address ([host]:port).
 *
 * @return A status code.
 */
extern int rvpf_ssl_open(RVPF_SSL_Context context, char *address);

/** Prints an error message.
 *
 * @param context The context.
 * @param prefix An errror message prefix.
 *
 * @return A true value if an error message has been printed.
 */
extern bool rvpf_ssl_printError(RVPF_SSL_Context context, char *prefix);

/** Receives bytes.
 *
 * @param context The context.
 * @param buffer A buffer.
 * @param size The size of the buffer.
 *
 * @return The number of bytes received.
 */
extern int rvpf_ssl_receive(
        RVPF_SSL_Context context,
        char *buffer,
        size_t size);

/** Sends bytes.
 *
 * @param context The context.
 * @param buffer A buffer.
 * @param size The size of the buffer.
 *
 * @return The number of bytes sent.
 */
extern int rvpf_ssl_send(
        RVPF_SSL_Context context,
        char *buffer,
        size_t size);

/** Sets the certificate.
 *
 * @param context The context.
 * @param filePath A file path.
 */
extern void rvpf_ssl_setCertificate(RVPF_SSL_Context context, char *filePath);

/** Sets the trust configuration.
 *
 * @param context The context.
 * @param filePath A file path.
 * @param directoryPath A directory path.
 */
extern void rvpf_ssl_setTrust(
        RVPF_SSL_Context context,
        char *filePath,
        char *directoryPath);

/** Returns the current status.
 *
 * @param context The context.
 *
 * @return A negative status code when failed or 0 when succeeded.
 */
extern int rvpf_ssl_status(RVPF_SSL_Context context);

/** Asks if the last operation has succeeded.
 *
 * @param context The context.
 *
 * @return A true value if the last operation has succeeded.
 */
extern bool rvpf_ssl_succeeded(RVPF_SSL_Context context);

/** Returns version informations.
 *
 * @return The version informations.
 */
extern char *rvpf_ssl_version(void);

#ifdef __vms
#pragma names restore
#endif

#ifdef __cplusplus
}
#endif

#endif /*RVPF_SSL_H_*/

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

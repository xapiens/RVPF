/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: rvpf_xpvpc.h 3961 2019-05-06 20:14:59Z SFB $
 */

/** RVPF XML point values port client API. */

#ifndef RVPF_XPVPC_H_
#define RVPF_XPVPC_H_

#include "rvpf_ssl.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifdef __vms
#pragma names save
#pragma names as_is
#pragma names shortened
#endif

#define RVPF_XPVPC_DELETED_STATE rvpf_xpvpc_deletedState()

/** Returned status code values.
 */
enum {
    RVPF_XPVPC_OK, /* Success. */
    RVPF_XPVPC_ILLEGAL_STATE, /* Illegal state. */
    RVPF_XPVPC_ILLEGAL_ARG, /* Illegal argument. */
    RVPF_XPVPC_INTERNAL_ERROR, /* Internal error. */
    RVPF_XPVPC_UNEXPECTED_RESPONSE, /* Unexpected response from server. */
    RVPF_XPVPC_MISMATCHED_ID, /* Mismatched id. */
    RVPF_XPVPC_UNKNOWN_ERROR, /* Unknown error. */
    RVPF_XPVPC_STATUS_CODES /* Count of status codes. */
};

/** Opaque context.
 */
typedef struct rvpf_xpvpc_context *RVPF_XPVPC_Context;

/** Clears an error.
 *
 * @param context The context.
 */
extern void rvpf_xpvpc_clearError(RVPF_XPVPC_Context context);

/** Closes the connection.
 *
 * @param context The context.
 *
 * @return A status code.
 */
extern int rvpf_xpvpc_close(RVPF_XPVPC_Context context);

/** Creates a context.
 *
 * @return The context.
 */
extern RVPF_XPVPC_Context rvpf_xpvpc_create(void);

/** Returns a deleted value marker state.
 *
 * @return The deleted value marker state.
 */
extern char *rvpf_xpvpc_deletedState(void);

/** Disposes of the context.
 *
 * @param context The context.
 */
extern void rvpf_xpvpc_dispose(RVPF_XPVPC_Context context);

/** Returns the error message.
 *
 * @param context The context.
 *
 * @return The error message or NULL.
 */
extern char *rvpf_xpvpc_errorMessage(RVPF_XPVPC_Context context);

/** Asks if the last operation has failed.
 *
 * @param context The context.
 *
 * @return A true value if the last operation has failed.
 */
extern bool rvpf_xpvpc_failed(RVPF_XPVPC_Context context);

/** Flushes pending entries.
 *
 * @param context The context.
 *
 * @return A status code.
 */
extern int rvpf_xpvpc_flush(RVPF_XPVPC_Context context);

/** Asks if the connection is open.
 *
 * @param context The context.
 *
 * @return A true value if the connection is open.
 */
extern bool rvpf_xpvpc_isOpen(RVPF_XPVPC_Context context);

/** Logs in.
 *
 * @param context The context.
 * @param user The user.
 * @param password The password.
 *
 * @return A status code.
 */
extern int rvpf_xpvpc_login(RVPF_XPVPC_Context context, char *user, char *password);

/** Opens a connection to the XML Port.
 *
 * @param context The context.
 * @param address The server address ([host]:port).
 *
 * @return A status code.
 */
extern int rvpf_xpvpc_open(RVPF_XPVPC_Context context, char *address);

/** Prints an error message.
 *
 * @param context The context.
 * @param prefix An errror message prefix.
 *
 * @return A true value if an error message has been printed.
 */
extern bool rvpf_xpvpc_printError(RVPF_XPVPC_Context context, char *prefix);

/** Sends a point value.
 *
 * @param context The context.
 * @param point The point name.
 * @param stamp The time stamp.
 * @param state The value state.
 * @param value The value.
 *
 * @return A status code.
 */
extern int rvpf_xpvpc_sendValue(
    RVPF_XPVPC_Context context,
    char *point,
    char *stamp,
    char *state,
    char *value);

/** Sets the auto-flush trigger.
 *
 * @param context The context.
 * @param autoFlush The auto-flush trigger (inactive when less than 1).
 */
extern void rvpf_xpvpc_setAutoFlush(RVPF_XPVPC_Context context, int autoFlush);

/** Sets the client.
 *
 * @param context The context.
 * @param client The client.
 */
extern void rvpf_xpvpc_setClient(RVPF_XPVPC_Context context, char *client);

/** Returns the RVPF_SSL_Context context.
 *
 * @param context The context.
 *
 * @return The RVPF_SSL_Context.
 */
extern RVPF_SSL_Context rvpf_xpvpc_ssl(RVPF_XPVPC_Context context);

/** Returns the current status.
 *
 * @param context The context.
 *
 * @return A status code (negative for a RVPF_SSL failed code).
 */
extern int rvpf_xpvpc_status(RVPF_XPVPC_Context context);

/** Asks if the last operation has succeeded.
 *
 * @param context The context.
 *
 * @return A true value if the last operation has succeeded.
 */
extern bool rvpf_xpvpc_succeeded(RVPF_XPVPC_Context context);

/** Returns version informations.
 *
 * @return The version informations.
 */
extern char *rvpf_xpvpc_version(void);

#ifdef __vms
#pragma names restore
#endif

#ifdef __cplusplus
}
#endif

#endif /*RVPF_XPVPC_H_*/

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

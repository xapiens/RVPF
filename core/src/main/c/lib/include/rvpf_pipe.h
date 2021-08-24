/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: rvpf_pipe.h 3961 2019-05-06 20:14:59Z SFB $
 */

/** RVPF pipe API. */

#ifndef RVPF_PIPE_H_
#define RVPF_PIPE_H_

#include "rvpf_log.h"

#include <stdbool.h>
#include <setjmp.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifdef __vms
#pragma names save
#pragma names as_is
#pragma names shortened
#endif

#define RVPF_PIPE_STATUS_OK 0
#define RVPF_PIPE_STATUS_ERROR 1
#define RVPF_PIPE_STATUS_FATAL 2

typedef enum rvpf_pipe_sinkRequestType {
    RVPF_PIPE_SINK_UPDATE,
    RVPF_PIPE_SINK_DELETE
} RVPF_PIPE_SinkRequestType;

typedef struct rvpf_pipe_engineRequest *RVPF_PIPE_EngineRequest;

typedef struct rvpf_pipe_sinkRequest *RVPF_PIPE_SinkRequest;

typedef struct rvpf_pipe_pointValue {
    char *pointName;
    char *stamp;
    char *state;
    char *value;
} *RVPF_PIPE_PointValue;


/** The RVPF pipe request processing assumes that the main loop is preceeded
 * by code looking like this:
 * <code>
 *      if (setjmp(rvpf_pipe_jmp_buf)) {
 *          return rvpf_pipe_status; // Value returned by main.
 *      }
 * </code>
 */

/** This MUST be set by 'setjmp'! */
jmp_buf rvpf_pipe_jmp_buf; /* Omitted storage class (tentative definition). */

/** Sink request type names. */
extern char *rvpf_pipe_sink_request_types[];

/** This must be returned by 'main'. */
extern int rvpf_pipe_status;

/** Adds an engine result.
 *
 * @param request The engine request.
 * @param pointName The point name.
 * @param stamp The date time stamp.
 * @param value The value.
 */
extern void rvpf_pipe_addEngineResult(
    RVPF_PIPE_EngineRequest request,
    const char *pointName,
    const char *stamp,
    const char *state,
    const char *value);

/** Clear the engine results.
 *
 * @param request The engine request.
 */
extern void rvpf_pipe_clearEngineResults(RVPF_PIPE_EngineRequest request);

/** Logs a message at the DEBUG level.
 *
 * @param format The message text format.
 */
extern void rvpf_pipe_debug(const char *format, ...);

/** Ends an engine request.
 *
 * @param request The engine request.
 */
extern void rvpf_pipe_endEngineRequest(RVPF_PIPE_EngineRequest request);

/** Ends a sink request.
 *
 * @param request The sink request.
 * @param summary The response summary.
 */
extern void rvpf_pipe_endSinkRequest(
    RVPF_PIPE_SinkRequest request,
    int summary);

/** Logs a message at the ERROR level.
 *
 * @param format The message text format.
 */
extern void rvpf_pipe_error(const char *format, ...);

/** Logs a message at the FATAL level.
 *
 * @param format The message text format.
 */
extern void rvpf_pipe_fatal(const char *format, ...);

/** Gets an engine input.
 *
 * @param request The engine request.
 * @param position The input position (starts at 1).
 *
 * @return The input (<code>null</code> if out of range).
 */
extern RVPF_PIPE_PointValue rvpf_pipe_getEngineInput(
    RVPF_PIPE_EngineRequest request,
    int position);

/** Gets the engine inputs count.
 *
 * @param request The engine request.
 *
 * @return The engine inputs count.
 */
extern int rvpf_pipe_getEngineInputsCount(RVPF_PIPE_EngineRequest request);

/** Gets an engine point param.
 *
 * @param request The engine request.
 * @param position The input position (starts at 1).
 *
 * @return The point param (<code>null</code> if out of range).
 */
extern char *rvpf_pipe_getEnginePointParam(
    RVPF_PIPE_EngineRequest request,
    int position);

/** Gets the engine point params count.
 *
 * @param request The engine request.
 *
 * @return The point params count.
 */
extern int rvpf_pipe_getEnginePointParamsCount(
    RVPF_PIPE_EngineRequest request);

/** Gets the engine request ID.
 *
 * @param request The engine request.
 *
 * @return The request ID.
 */
extern char *rvpf_pipe_getEngineRequestID(RVPF_PIPE_EngineRequest request);

/** Gets the engine result.
 *
 * @param request The engine request.
 *
 * @return The result.
 */
extern RVPF_PIPE_PointValue rvpf_pipe_getEngineResult(
    RVPF_PIPE_EngineRequest request);

/** Gets an engine transform param.
 *
 * @param request The engine request.
 * @param position The input position (starts at 1).
 *
 * @return The transform param (<code>null</code> if out of range).
 */
extern char *rvpf_pipe_getEngineTransformParam(
    RVPF_PIPE_EngineRequest request,
    int position);

/** Gets the engine transform params count.
 *
 * @param request The engine request.
 *
 * @return The transform params count.
 */
extern int rvpf_pipe_getEngineTransformParamsCount(
    RVPF_PIPE_EngineRequest request);

/** Logs a message at the INFO level.
 *
 * @param format The message text format.
 */
extern void rvpf_pipe_info(const char *format, ...);

/** Asks if a point value is deleted.
 *
 * @param pointValue The point value.
 *
 * @return A true value if the point value is deleted.
 */
extern bool rvpf_pipe_isValueDeleted(const RVPF_PIPE_PointValue pointValue);

/** Gets the sink point value.
 *
 * @param request The engine request.
 *
 * @return The sink point value.
 */
extern RVPF_PIPE_PointValue rvpf_pipe_getSinkPointValue(
    RVPF_PIPE_SinkRequest request);

/** Gets the sink request ID.
 *
 * @param request The sink request.
 *
 * @return The request ID.
 */
extern char *rvpf_pipe_getSinkRequestID(RVPF_PIPE_SinkRequest request);

/** Gets the sink request type.
 *
 * @param request The sink request.
 *
 * @return The request type.
 */
extern RVPF_PIPE_SinkRequestType rvpf_pipe_getSinkRequestType(
    RVPF_PIPE_SinkRequest request);

/** Returns the next engine request.
 *
 * @return The next engine request.
 */
extern RVPF_PIPE_EngineRequest rvpf_pipe_nextEngineRequest(void);

/** Returns the next sink request.
 *
 * @return The next sink request.
 */
extern RVPF_PIPE_SinkRequest rvpf_pipe_nextSinkRequest(void);

/** Sets the engine result state.
 *
 * <p>Note: must not be called after 'clearEngineResults'.</p>
 *
 * @param request The engine request.
 * @param state The state.
 */
extern void rvpf_pipe_setEngineResultState(
    RVPF_PIPE_EngineRequest request,
    const char *state);

/** Sets the engine result value.
 *
 * <p>Note: must not be called after 'clearEngineResults'.</p>
 *
 * @param request The engine request.
 * @param value The value.
 */
extern void rvpf_pipe_setEngineResultValue(
    RVPF_PIPE_EngineRequest request,
    const char *value);

/** Sets the log level.
 *
 * @param level The new log level (negative is superseded by RVPF_LOG_LEVEL).
 */
extern void rvpf_pipe_setLogLevel(int level);

/** Logs a message at the TRACE level.
 *
 * @param format The message text format.
 */
extern void rvpf_pipe_trace(const char *format, ...);

/** Returns version informations.
 *
 * @return The version informations.
 */
extern char *rvpf_pipe_version(void);

/** Logs a message at the WARN level.
 *
 * @param format The message text format.
 */
extern void rvpf_pipe_warn(const char *format, ...);

#ifdef __vms
#pragma names restore
#endif

#ifdef __cplusplus
}
#endif

#endif /*RVPF_PIPE_H_*/

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

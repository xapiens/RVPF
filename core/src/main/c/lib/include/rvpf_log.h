/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: rvpf_log.h 3961 2019-05-06 20:14:59Z SFB $
 */

/** RVPF log API. */

#ifndef RVPF_LOG_H_
#define RVPF_LOG_H_

#include <stdarg.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#define RVPF_LOG(level, ...) rvpf_log_s(level, __FILE__, __LINE__, __VA_ARGS__)
#define RVPF_LOG_DEBUG(...) rvpf_log_debug_s(__FILE__, __LINE__, __VA_ARGS__)
#define RVPF_LOG_ERROR(...) rvpf_log_error_s(__FILE__, __LINE__, __VA_ARGS__)
#define RVPF_LOG_FATAL(...) rvpf_log_fatal_s(__FILE__, __LINE__, __VA_ARGS__)
#define RVPF_LOG_INFO(...) rvpf_log_info_s(__FILE__, __LINE__, __VA_ARGS__)
#define RVPF_LOG_TRACE(...) rvpf_log_trace_s(__FILE__, __LINE__, __VA_ARGS__)
#define RVPF_LOG_WARN(...) rvpf_log_warn_s(__FILE__, __LINE__, __VA_ARGS__)

/** Log levels. */
typedef enum rvpf_log_level {
    RVPF_LOG_LEVEL_NONE,
    RVPF_LOG_LEVEL_FATAL,
    RVPF_LOG_LEVEL_ERROR,
    RVPF_LOG_LEVEL_WARN,
    RVPF_LOG_LEVEL_INFO,
    RVPF_LOG_LEVEL_DEBUG,
    RVPF_LOG_LEVEL_TRACE,
    RVPF_LOG_LEVEL_ALL
} RVPF_LOG_Level;

/** Logs a message at the specified level.
 *
 * @param level The log level.
 * @param format The message text format.
 * @param ap The variable argument list pointer.
 */
extern void rvpf_log(RVPF_LOG_Level level, const char *format, va_list ap);

/** Logs a message at the specified level.
 *
 * @param file The name of the source file.
 * @param line The line in the source file.
 * @param level The log level.
 * @param format The message text format.
 * @param ap The variable argument list pointer.
 */
extern void rvpf_log_s(
    RVPF_LOG_Level level,
    const char *file,
    int line,
    const char *format,
    va_list ap);

/** Logs a message at the DEBUG level.
 *
 * @param format The message text format.
 */
extern void rvpf_log_debug(const char *format, ...);

/** Logs a message at the DEBUG level.
 *
 * @param file The name of the source file.
 * @param line The line in the source file.
 * @param format The message text format.
 */
extern void rvpf_log_debug_s(
    const char *file,
    int line,
    const char *format, ...);

/** Closes an open log file then defaults to 'stderr'.
 */
extern void rvpf_log_close(void);

/** Logs a message at the ERROR level.
 *
 * @param format The message text format.
 */
extern void rvpf_log_error(const char *format, ...);

/** Logs a message at the ERROR level.
 *
 * @param file The name of the source file.
 * @param line The line in the source file.
 * @param format The message text format.
 */
extern void rvpf_log_error_s(
    const char *file,
    int line,
    const char *format, ...);

/** Logs a message at the FATAL level.
 *
 * @param format The message text format.
 */
extern void rvpf_log_fatal(const char *format, ...);

/** Logs a message at the FATAL level.
 *
 * @param file The name of the source file.
 * @param line The line in the source file.
 * @param format The message text format.
 */
extern void rvpf_log_fatal_s(
    const char *file,
    int line,
    const char *format, ...);

/** Gets the log level.
 *
 * @return The current log level.
 */
extern RVPF_LOG_Level rvpf_log_getLevel(void);

/** Gets the current number of logged messages.
 *
 * @return The current number of logged messages.
 */
extern int rvpf_log_getLogged(void);

/** Logs a message at the INFO level.
 *
 * @param format The message text format.
 */
extern void rvpf_log_info(const char *format, ...);

/** Logs a message at the INFO level.
 *
 * @param file The name of the source file.
 * @param line The line in the source file.
 * @param format The message text format.
 */
extern void rvpf_log_info_s(
    const char *file,
    int line,
    const char *format, ...);

/** Asks if the debug level is enabled.
 *
 * @return True if the debug level is enabled.
 */
extern bool rvpf_log_isDebugEnabled(void);

/** Asks if log is enabled for a level.
 *
 * @param level The log level.
 *
 * @return True if log is enabled for the level.
 */
extern bool rvpf_log_isEnabledFor(RVPF_LOG_Level level);

/** Asks if the debug level is enabled.
 *
 * @return True if the debug level is enabled.
 */
extern bool rvpf_log_isInfoEnabled(void);

/** Asks if the debug level is enabled.
 *
 * @return True if the debug level is enabled.
 */
extern bool rvpf_log_isTraceEnabled(void);

/** Opens a log file.
 *
 * @param filePath The file path.
 *
 * @return True on success.
 */
extern bool rvpf_log_open(const char *filePath);

/** Sets the log level.
 *
 * @param level The new log level (negative is superseded by RVPF_LOG_LEVEL).
 */
extern void rvpf_log_setLevel(int level);

/** Logs a message at the TRACE level.
 *
 * @param format The message text format.
 */
extern void rvpf_log_trace(const char *format, ...);

/** Logs a message at the TRACE level.
 *
 * @param file The name of the source file.
 * @param line The line in the source file.
 * @param format The message text format.
 */
extern void rvpf_log_trace_s(
    const char *file,
    int line,
    const char *format, ...);

/** Returns version informations.
 *
 * @return The version informations.
 */
extern char *rvpf_log_version(void);

/** Logs a message at the WARN level.
 *
 * @param format The message text format.
 */
extern void rvpf_log_warn(const char *format, ...);

/** Logs a message at the WARN level.
 *
 * @param file The name of the source file.
 * @param line The line in the source file.
 * @param format The message text format.
 */
extern void rvpf_log_warn_s(
    const char *file,
    int line,
    const char *format, ...);

#ifdef __cplusplus
}
#endif

#endif /* RVPF_LOG_H_ */

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

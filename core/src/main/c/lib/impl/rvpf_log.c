/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: rvpf_log.c 3961 2019-05-06 20:14:59Z SFB $
 */

/** RVPF log API implementation.
 *
 * See header file (.h) for API description.
 */
#include "rvpf_version.h"

#include "rvpf_log.h"

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

// Private macro definitions.

#define _DEFAULT_LOG_LEVEL RVPF_LOG_LEVEL_WARN
#define _RVPF_LOG_LEVEL "RVPF_LOG_LEVEL"

// Private forward declarations.

static void _log(
    RVPF_LOG_Level level,
    const char *file,
    int line,
    const char *format,
    va_list ap);

// Private storage.

static RVPF_LOG_Level _level = _DEFAULT_LOG_LEVEL;
static bool _levelSet = false;
static const char *_levels[] = {
	"NONE",
	"FATAL",
	"ERROR",
	"WARN",
	"INFO",
	"DEBUG",
	"TRACE",
	"ALL"
};
static FILE *_logFile = NULL;
static int _logged = 0;

// Public function definitions.

extern void rvpf_log(RVPF_LOG_Level level, const char *format, va_list ap)
{
    if (_level >= level) {
        _log(level, NULL, 0, format, ap);
    }
}

extern void rvpf_log_s(
    RVPF_LOG_Level level,
    const char *file,
    int line,
    const char *format,
    va_list ap)
{
    if (_level >= level) {
        _log(level, file, line, format, ap);
    }
}

extern void rvpf_log_close(void)
{
    if (_logFile != NULL && _logFile != stderr) {
        FILE *logFile = _logFile;

        _logFile = stderr;
        if (fclose(logFile)) {
            rvpf_log_warn("Failed to close log file (errno = %i)!\n", errno);
        }
    }

    _logged = 0;
}

extern void rvpf_log_debug(const char *format, ...)
{
    if (_level >= RVPF_LOG_LEVEL_DEBUG) {
        va_list ap;

        va_start(ap, format);
        _log(RVPF_LOG_LEVEL_DEBUG, NULL, 0, format, ap);
        va_end(ap);
    }
}

extern void rvpf_log_debug_s(
    const char *file,
    int line,
    const char *format, ...)
{
    if (_level >= RVPF_LOG_LEVEL_DEBUG) {
        va_list ap;

        va_start(ap, format);
        _log(RVPF_LOG_LEVEL_DEBUG, file, line, format, ap);
        va_end(ap);
    }
}

extern void rvpf_log_error(const char *format, ...)
{
    if (_level >= RVPF_LOG_LEVEL_ERROR) {
        va_list ap;

        va_start(ap, format);
        _log(RVPF_LOG_LEVEL_ERROR, NULL, 0, format, ap);
        va_end(ap);
    }
}

extern void rvpf_log_error_s(
    const char *file,
    int line,
    const char *format, ...)
{
    if (_level >= RVPF_LOG_LEVEL_ERROR) {
        va_list ap;

        va_start(ap, format);
        _log(RVPF_LOG_LEVEL_ERROR, file, line, format, ap);
        va_end(ap);
    }
}

extern void rvpf_log_fatal(const char *format, ...)
{
    if (_level >= RVPF_LOG_LEVEL_FATAL) {
        va_list ap;

        va_start(ap, format);
        _log(RVPF_LOG_LEVEL_FATAL, NULL, 0, format, ap);
        va_end(ap);
    }
}

extern void rvpf_log_fatal_s(
    const char *file,
    int line,
    const char *format, ...)
{
    if (_level >= RVPF_LOG_LEVEL_FATAL) {
        va_list ap;

        va_start(ap, format);
        _log(RVPF_LOG_LEVEL_FATAL, file, line, format, ap);
        va_end(ap);
    }
}

extern RVPF_LOG_Level rvpf_log_getLevel(void)
{
	return _level;
}

extern int rvpf_log_getLogged(void)
{
    return _logged;
}

extern void rvpf_log_info(const char *format, ...)
{
    if (_level >= RVPF_LOG_LEVEL_INFO) {
        va_list ap;

        va_start(ap, format);
        _log(RVPF_LOG_LEVEL_INFO, NULL, 0, format, ap);
        va_end(ap);
    }
}

extern void rvpf_log_info_s(
    const char *file,
    int line,
    const char *format, ...)
{
    if (_level >= RVPF_LOG_LEVEL_INFO) {
        va_list ap;

        va_start(ap, format);
        _log(RVPF_LOG_LEVEL_INFO, file, line, format, ap);
        va_end(ap);
    }
}

extern bool rvpf_log_isDebugEnabled(void)
{
    return _level >= RVPF_LOG_LEVEL_DEBUG;
}

extern bool rvpf_log_isEnabledFor(RVPF_LOG_Level level)
{
    return _level >= level;
}

extern bool rvpf_log_isInfoEnabled(void)
{
    return _level >= RVPF_LOG_LEVEL_INFO;
}

extern bool rvpf_log_isTraceEnabled(void)
{
    return _level >= RVPF_LOG_LEVEL_TRACE;
}

extern bool rvpf_log_open(const char *filePath)
{
    FILE *logFile = fopen(filePath, "a");

    if (logFile == NULL) {
        rvpf_log_warn(
            "Failed to open log file '%s' (errno = %i)!\n",
            filePath,
            errno);
        return false;
    }
    rvpf_log_close();

    _logFile = logFile;

    return true;
}

extern void rvpf_log_setLevel(int level)
{
    if (level < (int) RVPF_LOG_LEVEL_NONE) {
        if (!_levelSet) {
            char *envString = getenv(_RVPF_LOG_LEVEL);

            level = envString != NULL? atoi(envString): -level;
            if (level >= (int) RVPF_LOG_LEVEL_NONE) rvpf_log_setLevel(level);
        }
    } else if (level <= RVPF_LOG_LEVEL_ALL) {
        _level = level;
        _levelSet = true;
    }
}

extern void rvpf_log_trace(const char *format, ...)
{
    if (_level >= RVPF_LOG_LEVEL_TRACE) {
        va_list ap;

        va_start(ap, format);
        _log(RVPF_LOG_LEVEL_TRACE, NULL, 0, format, ap);
        va_end(ap);
    }
}

extern void rvpf_log_trace_s(
    const char *file,
    int line,
    const char *format, ...)
{
    if (_level >= RVPF_LOG_LEVEL_TRACE) {
        va_list ap;

        va_start(ap, format);
        _log(RVPF_LOG_LEVEL_TRACE, file, line, format, ap);
        va_end(ap);
    }
}

extern char *rvpf_log_version(void)
{
    return "RVPF_LOG " RVPF_VERSION_REVISION;
}

extern void rvpf_log_warn(const char *format, ...)
{
    if (_level >= RVPF_LOG_LEVEL_WARN) {
        va_list ap;

        va_start(ap, format);
        _log(RVPF_LOG_LEVEL_WARN, NULL, 0, format, ap);
        va_end(ap);
    }
}

extern void rvpf_log_warn_s(
    const char *file,
    int line,
    const char *format, ...)
{
    if (_level >= RVPF_LOG_LEVEL_WARN) {
        va_list ap;

        va_start(ap, format);
        _log(RVPF_LOG_LEVEL_WARN, file, line, format, ap);
        va_end(ap);
    }
}

// Private function definitions.

static void _log(
    RVPF_LOG_Level level,
    const char *file,
    int line,
    const char *format,
    va_list ap)
{
    if (!_logFile) _logFile = stderr;

    if (_logFile != stderr) {
        time_t now = time(NULL);

        if (now > 0) {
            struct tm *now_tm_ptr;
            char now_string[20];

#if defined(__MINGW32__)
            now_tm_ptr = localtime(&now);
#else
            struct tm now_tm;

            now_tm_ptr = &now_tm;
            localtime_r(&now, now_tm_ptr);
#endif
            if (strftime(
                    now_string,
                    sizeof now_string,
                    "%Y-%m-%d %H:%M:%S",
                    now_tm_ptr)) {
                fputs(now_string, _logFile);
                fputs(" ", _logFile);
            }
        }
    }

    fputs(_levels[level], _logFile);

    if (file) fprintf(_logFile, " (FILE '%s', LINE %d)", file, line);

    if (format) {
        fputs(" ", _logFile);
        vfprintf(_logFile, format, ap);
    }

    fputs("\n", _logFile);
    fflush(_logFile);

    ++_logged;
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

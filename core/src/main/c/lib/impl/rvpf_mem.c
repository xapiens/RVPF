/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: rvpf_mem.c 3961 2019-05-06 20:14:59Z SFB $
 */

/** RVPF memory API implementation.
 *
 * See header file (.h) for API description.
 */
#include "rvpf_mem.h"
#include "rvpf_log.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

// Public function definitions.

extern void *rvpf_mem_allocate(size_t size, const char *file, int line)
{
    void *memory;

    if (size > 0) {
        memory = calloc(1, size);
        if (!memory) {
            rvpf_log_fatal_s(file, line, "Failed to allocate %d bytes", size);
            exit(-1);
        }
    } else {
        rvpf_log_fatal_s(file, line, "Invalid allocation size: %d", size);
        exit(-1);
    }

    return memory;
}

extern void rvpf_mem_free(void *memory)
{
    if (memory) free(memory);
}

extern void *rvpf_mem_reallocate(
    void *memory,
    size_t size,
    const char *file,
    int line)
{
    if (!memory) return rvpf_mem_allocate(size, file, line);

    if (size > 0) {
        memory = realloc(memory, size);
        if (!memory) {
            rvpf_log_fatal_s(file, line, "Failed to allocate %d bytes", size);
            exit(-1);
        }
    } else {
        rvpf_log_fatal_s(file, line, "Invalid allocation size: %d", size);
        exit(-1);
    }

    return memory;
}

extern char *rvpf_mem_string(const char *original, const char *file, int line)
{
    char *copy;

    if (original) {
        copy = rvpf_mem_allocate(strlen(original) + 1, file, line);
        strcpy(copy, original);
    } else copy = NULL;

    return copy;
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

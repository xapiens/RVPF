/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: rvpf_mem.h 3961 2019-05-06 20:14:59Z SFB $
 */

/** RVPF memory API. */

#ifndef RVPF_MEM_H_
#define RVPF_MEM_H_

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#define RVPF_MEM_ALLOCATE(size) rvpf_mem_allocate(size, __FILE__, __LINE__)
#define RVPF_MEM_FREE(memory) rvpf_mem_free(memory)
#define RVPF_MEM_REALLOCATE(memory, size) \
    rvpf_mem_reallocate(memory, size, __FILE__, __LINE__)
#define RVPF_MEM_STRING(original) rvpf_mem_string(original, __FILE__, __LINE__)

/** Allocates memory.
 *
 * <p>Note: the content of the allocated memory is cleared.</p>
 *
 * @param size The memory size in bytes.
 * @param file The name of the source file.
 * @param line The line in the source file.
 *
 * @return A pointer to the allocated memory.
 */
extern void *rvpf_mem_allocate(size_t size, const char *file, int line);

/** Frees allocated memory.
 *
 * @param memory A pointer to the allocated memory (may be NULL).
 */
extern void rvpf_mem_free(void *memory);

/** Reallocates memory.
 *
 * <p>Note: the content of the extended memory is undeterminate.</p>
 *
 * @param memory A pointer to the allocated memory (may be NULL).
 * @param size The memory size in bytes.
 * @param file The name of the source file.
 * @param line The line in the source file.
 *
 * @return A pointer to the allocated memory.
 */
extern void *rvpf_mem_reallocate(
    void *memory,
    size_t size,
    const char *file,
    int line);

/** Returns a copy of a string from new memory.
 *
 * @param original The original string.
 * @param file The name of the source file.
 * @param line The line in the source file.
 *
 * @return A pointer to the copy.
 */
extern char *rvpf_mem_string(const char *original, const char *file, int line);

#ifdef __cplusplus
}
#endif

#endif /* RVPF_MEM_H_ */

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

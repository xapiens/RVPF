/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: rvpf_tree.h 3961 2019-05-06 20:14:59Z SFB $
 */

/** RVPF tree API. */

#ifndef RVPF_TREE_H_
#define RVPF_TREE_H_

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/** Comparison function.
 */
typedef int (*RVPF_TREE_Comparator) (const void *, const void *);

/** Tree context.
 */
typedef struct rvpf_tree_context *RVPF_TREE_Context;

/** Clears the context.
 *
 * @param context The context.
 */
extern void rvpf_tree_clear(RVPF_TREE_Context context);

/** Creates a context.
 *
 * @return The context.
 */
extern RVPF_TREE_Context rvpf_tree_create(void);

/** Disposes of the context.
 *
 * @param context The context.
 */
extern void rvpf_tree_dispose(RVPF_TREE_Context context);

/** Gets the value associates with a key.
 *
 * @param context The context.
 * @param key The key.
 *
 * @return The value associated with the key (NULL if none).
 */
extern const void *rvpf_tree_get(RVPF_TREE_Context context, const void *key);

/** Associates a value with a key.
 *
 * @param context The context.
 * @param key The key (must come from RVPF_MEM_ALLOCATE).
 * @param value The value (must come from RVPF_MEM_ALLOCATE).
 *
 * @return The previous value (NULL if none).
 */
extern const void *rvpf_tree_put(
    RVPF_TREE_Context context,
    const void *key,
    const void *value);

/** Removes a key association.
 *
 * @param context The context.
 * @param key The key.
 *
 * @return The value associated with the key (NULL if none).
 */
extern const void *rvpf_tree_remove(RVPF_TREE_Context context, const void *key);

/** Sets the comparator function.
 *
 * @param context The context.
 * @param comparator The comparator function (NULL returns to default).
 */
extern void rvpf_tree_setComparator(
    RVPF_TREE_Context context,
    RVPF_TREE_Comparator comparator);

/** Returns the number of key/value associations..
 *
 * @param context The context.
 *
 * @return The number of associations.
 */
extern size_t rvpf_tree_size(RVPF_TREE_Context context);

#ifdef __cplusplus
}
#endif

#endif /* RVPF_TREE_H_ */

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

/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: HandlesMap.h 3961 2019-05-06 20:14:59Z SFB $
 */
#ifndef RVPF_HANDLES_MAP_H
#define RVPF_HANDLES_MAP_H

#include "Types.h"

typedef struct c_store_handles_map *c_store_handles_map_t;

extern void HandlesMap_clear(c_store_handles_map_t map);

extern c_store_handles_map_t HandlesMap_create(size_t initialLoadSize);

extern void HandlesMap_dispose(c_store_handles_map_t map);

extern c_store_handle_t HandlesMap_get(
    c_store_handles_map_t map,
    c_store_handle_t key);

extern c_store_handle_t *HandlesMap_keys(c_store_handles_map_t map);

extern c_store_handle_t HandlesMap_put(
    c_store_handles_map_t map,
    c_store_handle_t key,
    c_store_handle_t value);

extern c_store_handle_t HandlesMap_remove(
    c_store_handles_map_t map,
    c_store_handle_t key);

extern size_t HandlesMap_size(c_store_handles_map_t map);

extern c_store_handle_t *HandlesMap_values(c_store_handles_map_t map);

#endif /* RVPF_HANDLES_MAP_H */

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

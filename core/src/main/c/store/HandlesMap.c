/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: HandlesMap.c 3961 2019-05-06 20:14:59Z SFB $
 */

/* Notes.
 *
 * This implementation tries to reproduce some of the
 * behavior of the Java HashMap class for a handle to
 * handle Map.
 */
#include "HandlesMap.h"
#include "CStoreImpl.h"

#include <limits.h>
#include <string.h>

// Private macro defintions.

#define LOAD_FACTOR 0.75
#define MAXIMUM_CAPACITY (1 << 30)

#ifdef _WIN32
#define MAXIMUM_SIZE LONG_MAX
#else
#define MAXIMUM_SIZE INT_MAX
#endif

// Private type definitions.

typedef struct entry *entry_t;

#ifdef _WIN32
typedef unsigned long hash_t;
#else
typedef unsigned int hash_t;
#endif

// Private structure definitions.

struct c_store_handles_map {
    entry_t *table;
    size_t capacity;
    size_t size;
    size_t threshold;
};

struct entry {
    c_store_handle_t key;
    c_store_handle_t value;
    entry_t next;
};

// Private forward declarations.

static size_t _indexFor(c_store_handles_map_t map, c_store_handle_t key);

static void _rehash(c_store_handles_map_t map);

// Public function definitions.

void HandlesMap_clear(c_store_handles_map_t map)
{
    ASSERT(map);

    for (int i = 0; i < map->capacity; ++i) {
        entry_t entry = map->table[i];

        map->table[i] = NULL;
        while (entry) {
            entry_t next = entry->next;

            entry->next = NULL;
            entry->key = 0;
            entry->value = 0;
            CStore_free(entry);
            entry = next;
        }
    }
    map->size = 0;
}

c_store_handles_map_t HandlesMap_create(size_t initialLoadSize)
{
    ASSERT(initialLoadSize >= 0);

    c_store_handles_map_t map =
        CStore_allocate(sizeof(struct c_store_handles_map));
    size_t initialCapacity = 1 + (int) (initialLoadSize / LOAD_FACTOR);

    ASSERT(map);
    ASSERT(initialCapacity <= MAXIMUM_CAPACITY);
    for (
        map->capacity = 1;
        map->capacity < initialCapacity;
        map->capacity <<= 1);

    map->size = 0;
    map->threshold = map->capacity * LOAD_FACTOR;
    map->table = CStore_allocate(map->capacity * sizeof(entry_t));
    ASSERT(map->table);

    return map;
}

void HandlesMap_dispose(c_store_handles_map_t map)
{
    if (map) {
        HandlesMap_clear(map);
        CStore_free(map->table);
        map->table = NULL;
        CStore_free(map);
    }
}

c_store_handle_t HandlesMap_get(c_store_handles_map_t map, c_store_handle_t key)
{
    ASSERT(map);

    entry_t entry = map->table[_indexFor(map, key)];

    while (entry) {
        if (entry->key == key) {
            return entry->value;
        }

        entry = entry->next;
    };

    return 0;
}

c_store_handle_t *HandlesMap_keys(c_store_handles_map_t map)
{
    ASSERT(map);

    size_t mapSize = map->size;
    c_store_handle_t *keys =
        CStore_allocate((mapSize? mapSize: 1) * sizeof(c_store_handle_t));
    size_t index = 0;

    ASSERT(keys);
    for (int i = 0; i < map->capacity; ++i) {
        entry_t entry = map->table[i];

        while (entry) {
            ASSERT(index < mapSize);
            keys[index++] = entry->key;
            entry = entry->next;
        }
    }
    ASSERT(index == mapSize);

    return keys;
}

c_store_handle_t HandlesMap_put(
    c_store_handles_map_t map,
    c_store_handle_t key,
    c_store_handle_t value)
{
    ASSERT(map);

    size_t index = _indexFor(map, key);
    entry_t entry = map->table[index];

    while (entry) {
        if (entry->key == key) {
            c_store_handle_t previousValue = entry->value;

            entry->value = value;

            return previousValue;
        }

        entry = entry->next;
    };

    entry = CStore_allocate(sizeof(struct entry));
    ASSERT(entry);
    entry->key = key;
    entry->value = value;
    entry->next = map->table[index];
    map->table[index] = entry;

    if (++map->size > map->threshold) _rehash(map);

    return 0;
}

c_store_handle_t HandlesMap_remove(
    c_store_handles_map_t map,
    c_store_handle_t key)
{
    ASSERT(map);

    size_t index = _indexFor(map, key);
    entry_t entry = map->table[index];
    entry_t previousEntry = NULL;

    while (entry) {
        if (entry->key == key) {
            c_store_handle_t value = entry->value;

            if (previousEntry) previousEntry->next = entry->next;
            else map->table[index] = NULL;
            --map->size;

            entry->next = NULL;
            entry->key = 0;
            entry->value = 0;
            CStore_free(entry);

            return value;
        }

        previousEntry = entry;
        entry = entry->next;
    };

    return 0;
}

size_t HandlesMap_size(c_store_handles_map_t map)
{
    ASSERT(map);

    return map->size;
}

c_store_handle_t *HandlesMap_values(c_store_handles_map_t map)
{
    ASSERT(map);

    size_t mapSize = map->size;
    c_store_handle_t *values =
        CStore_allocate((mapSize? mapSize: 1) * sizeof(c_store_handle_t));
    size_t index = 0;

    ASSERT(values);
    for (int i = 0; i < map->capacity; ++i) {
        entry_t entry = map->table[i];

        while (entry) {
            ASSERT(index < mapSize);
            values[index++] = entry->value;
            entry = entry->next;
        }
    }
    ASSERT(index == mapSize);

    return values;
}

// Private function definitions.

static size_t _indexFor(c_store_handles_map_t map, c_store_handle_t key)
{
    hash_t hash = key;

    hash += ~(hash << 9);
    hash ^=  (hash >> 14);
    hash +=  (hash << 4);
    hash ^=  (hash >> 10);

    return hash & (map->capacity - 1);
}

static void _rehash(c_store_handles_map_t map)
{
    if (map->capacity == MAXIMUM_CAPACITY) {
        map->threshold = MAXIMUM_SIZE;
        return;
    }

    entry_t *oldTable = map->table;
    size_t oldCapacity = map->capacity;

    map->capacity <<= 1;
    map->threshold = map->capacity * LOAD_FACTOR;
    map->table = CStore_allocate(map->capacity * sizeof(entry_t));
    ASSERT(map->table);

    for (int i = 0; i < oldCapacity; ++i) {
        entry_t entry = oldTable[i];

        while (entry) {
            entry_t next = entry->next;
            size_t index = _indexFor(map, entry->key);

            entry->next = map->table[index];
            map->table[index] = entry;

            entry = next;
        }
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

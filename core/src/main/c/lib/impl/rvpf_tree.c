/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: rvpf_tree.c 3961 2019-05-06 20:14:59Z SFB $
 */

/** RVPF tree API implementation.
 *
 * See header file (.h) for API description.
 */
#include "rvpf_tree.h"
#include "rvpf_mem.h"
#include "rvpf_log.h"

#include <assert.h>
#include <search.h>
#include <string.h>

#if defined(__linux__) || defined(__CYGWIN__)
#define _TDESTROY_AVAILABLE
#endif

// Context definition.

struct rvpf_tree_node
{
    struct rvpf_tree_context *context;
    const void *key;
    const void *value;
};

struct rvpf_tree_context
{
    struct rvpf_tree_node *root;
    size_t size;
    RVPF_TREE_Comparator comparator;
};

// Private forward declarations.

static void _clearNode(void *node);
static int _defaultComparator(const void *first, const void *second);
static int _nodeComparator(const void *first, const void *second);

// Public function definitions.

extern void rvpf_tree_clear(struct rvpf_tree_context *context)
{
    if (!context) return;

#ifdef _TDESTROY_AVAILABLE
    tdestroy(context->root, _clearNode);
    context->root = NULL;
    context->size = 0;
#else
    while (context->root) {
        _clearNode(context->root);
        tdelete(context->root, (void **) &context->root, _nodeComparator);
        --context->size;
    }
    assert(context->size == 0);
#endif
}

extern struct rvpf_tree_context *rvpf_tree_create(void)
{
    struct rvpf_tree_context *context = (RVPF_TREE_Context) RVPF_MEM_ALLOCATE(
        sizeof(struct rvpf_tree_context));

    context->comparator = _defaultComparator;

    return context;
}

extern void rvpf_tree_dispose(struct rvpf_tree_context *context)
{
    rvpf_tree_clear(context);
    RVPF_MEM_FREE(context);
}

extern const void *rvpf_tree_get(struct rvpf_tree_context *context, const void *key)
{
    struct rvpf_tree_node keyNode = {context, key};
    struct rvpf_tree_node *foundNode =
        tfind(&keyNode, (void **) &context->root, _nodeComparator);

    return foundNode? foundNode->value: NULL;
}

extern const void *rvpf_tree_put(struct rvpf_tree_context *context, const void *key, const void *value)
{
    struct rvpf_tree_node *newNode =
            RVPF_MEM_ALLOCATE(sizeof(struct rvpf_tree_node));

    newNode->context = context;
    newNode->key = key;
    newNode->value = value;
    ++context->size;

    struct rvpf_tree_node *foundNode =
        tsearch(&newNode, (void **) &context->root, _nodeComparator);
    const void *oldValue;

    if (foundNode == newNode) oldValue = NULL;
    else {
        --context->size;
        RVPF_MEM_FREE(newNode);
        oldValue = foundNode->value;
        foundNode->value = value;
    }

    return oldValue;
}

extern const void *rvpf_tree_remove(struct rvpf_tree_context *context, const void *key)
{
    struct rvpf_tree_node keyNode = {context, key};
    struct rvpf_tree_node *foundNode =
        tfind(&keyNode, (void **) &context->root, _nodeComparator);
    const void *deletedValue;

    if (foundNode) {
        deletedValue = foundNode->value;
        tdelete(&keyNode, (void **) &context->root, _nodeComparator);
        --context->size;
        RVPF_MEM_FREE(foundNode);
    } else deletedValue = NULL;

    return deletedValue;
}

extern void rvpf_tree_setComparator(
    struct rvpf_tree_context *context,
    RVPF_TREE_Comparator comparator)
{
    context->comparator = comparator? comparator: _defaultComparator;
}

extern size_t rvpf_tree_size(struct rvpf_tree_context *context)
{
	return context? context->size: 0;
}

// Private function definitions.

static void _clearNode(void *node)
{
    struct rvpf_tree_node *treeNode = node;

    if (treeNode) {
        RVPF_MEM_FREE((void *) treeNode->value);
        treeNode->value = NULL;
        RVPF_MEM_FREE((void *) treeNode->key);
        treeNode->key = NULL;
        RVPF_MEM_FREE(treeNode);
    }
}

static int _defaultComparator(const void *first, const void *second)
{
	return strcmp(first, second);
}

static int _nodeComparator(const void *first, const void *second)
{
	const struct rvpf_tree_node *firstNode = first;
	const struct rvpf_tree_node *secondNode = second;
	struct rvpf_tree_context *context = firstNode->context;

	return context->comparator(firstNode->key, secondNode->key);
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

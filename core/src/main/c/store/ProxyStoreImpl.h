/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProxyStoreImpl.h 3961 2019-05-06 20:14:59Z SFB $
 */
#ifndef PROXY_STORE_IMPL_H
#define PROXY_STORE_IMPL_H

#include "CStoreImpl.h"

extern int CStore_acceptValues(
    JNIEnv *env,
    jobject container,
    size_t *count,
    c_store_value_t ***values);

extern char *CStore_bytesToCString(JNIEnv *env, jbyteArray bytes);

extern jbyteArray CStore_cStringToBytes(JNIEnv *env, char *cString);

extern jclass CStore_getClass(
    JNIEnv *env,
    const char *name,
    c_store_logger_t logger);

extern jmethodID CStore_getMethodID(
    JNIEnv *env,
    jclass cls,
    const char *name,
    const char *sig,
    c_store_logger_t logger);

extern jmethodID CStore_getStaticMethodID(
    JNIEnv *env,
    jclass cls,
    const char *name,
    const char *sig,
    c_store_logger_t logger);

extern bool CStore_loadClasses(JNIEnv *env, c_store_logger_t logger);

extern jobject CStore_newByteArray(JNIEnv *env, jsize size);

extern jobject CStore_newValuesContainer(JNIEnv *env);

extern void CStore_returnValues(
    JNIEnv *env,
    int count,
    c_store_value_t **values,
    jobject container);

extern void CStore_unloadClasses(JNIEnv *env);

#endif /* PROXY_STORE_IMPL_H */

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

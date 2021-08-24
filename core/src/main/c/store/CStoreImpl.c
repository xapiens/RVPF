/** Related Values Processing Framework.
 *
 * Copyright (C) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CStoreImpl.c 3961 2019-05-06 20:14:59Z SFB $
 */
#include "CStoreImpl.h"

#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <stdio.h>

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#else
#include <dlfcn.h>
#endif

// Private macro definitions.

#define MAX_BYTES_BLOCK 65534

// Private type definitions.

union number {
    c_store_double_t doubleValue;
    c_store_long_t longValue;
    c_store_float_t floatValue;
    c_store_int_t intValue;
    c_store_short_t shortValue;
    c_store_byte_t byteValue;
    c_store_bool_t boolValue;
};

// Private variable definitions.

static char *_assertInput = NULL;

static char _assertOutput;

// Private forward declarations.

static char *_joinedStringValue(c_store_value_t *storeValue);

static size_t _joinedValueLength(c_store_value_t *storeValue);

static void _joinValue(c_store_value_t *storeValue, void *buffer);

static size_t _splitLength(size_t joinedLength);

static void _splitValue(
        void *value,
        size_t valueLength,
        c_store_value_t *storeValue,
        size_t offset);

// Helper function definitions.

void *CStore_allocate(size_t size)
{
    void *memory = malloc(size);

    if (memory) memset(memory, 0, size);

    return memory;
}

void CStore_assert(const char *file, int line, const char *message)
{
    fprintf(stderr, "Assertion failed: %s, %s, line %i.\n",
        message, file, line);

    _assertOutput = *_assertInput; // Crash!
}

void CStore_closeLibrary(void *libraryHandle)
{
#ifdef _WIN32
    FreeLibrary(libraryHandle);
#else
    dlclose(libraryHandle);
#endif
}

void CStore_free(void *memory)
{
    free(memory);
}

enum value_type CStore_getValueType(c_store_value_t *storeValue)
{
    return storeValue->size? storeValue->value[0]: VALUE_TYPE_NULL;
}

void CStore_log(c_store_logger_t logger, int level, const char *format, ...)
{
    ASSERT(logger);

    if (level <= logger->level) {
        int result = strlen(format);
        va_list args;

        do {
            va_start(args, format);
            result = logger->log(
                logger, (size_t) result, level, format, args);
            va_end(args);
        } while (result > 0);

        ASSERT(result == 0);
    }
}

c_store_value_t *CStore_newValue(
    c_store_t cStore,
    enum value_type valueType,
    ...)
{
    va_list args;
    char textBuffer[12];
    size_t textLength;
    size_t bytesLength;
    union number number;
    void *bytes;
    size_t valueLength;
    c_store_quality_t *stateCode;

    va_start(args, valueType);
    switch (valueType) {
    case VALUE_TYPE_NULL:
        valueLength = 0;
        break;
    case VALUE_TYPE_DOUBLE:
        number.doubleValue = va_arg(args, c_store_double_t);
        valueLength = 1 + sizeof(number.doubleValue);
        break;
    case VALUE_TYPE_LONG:
        number.longValue = va_arg(args, c_store_long_t);
        valueLength = 1 + sizeof(number.longValue);
        break;
    case VALUE_TYPE_BOOLEAN:
        number.boolValue = va_arg(args, int);
        valueLength = 1 + sizeof(number.boolValue);
        break;
    case VALUE_TYPE_SHORT:
        number.shortValue = va_arg(args, int);
        valueLength = 1 + sizeof(number.shortValue);
        break;
    case VALUE_TYPE_STATE:
        stateCode = va_arg(args, c_store_quality_t *);
        if (stateCode) {
            sprintf(textBuffer, "%li", (long) *stateCode);
            textLength = strlen(textBuffer);
        } else textLength = 0;
        valueLength = 1 + _splitLength(textLength);
        bytes = va_arg(args, void *);
        if (bytes) {
            textBuffer[textLength++] = ':';
            ++valueLength;
            bytesLength = va_arg(args, size_t);
            valueLength += _splitLength(bytesLength) - 2;
        } else bytesLength = 0;
        break;
    case VALUE_TYPE_STRING:
    case VALUE_TYPE_BYTE_ARRAY:
        bytes = va_arg(args, void *);
        bytesLength = va_arg(args, size_t);
        valueLength = 1 + _splitLength(bytesLength);
        break;
    case VALUE_TYPE_INTEGER:
        number.intValue = va_arg(args, c_store_int_t);
        valueLength = 1 + sizeof(number.intValue);
        break;
    case VALUE_TYPE_FLOAT:
        number.floatValue = va_arg(args, double);
        valueLength = 1 + sizeof(number.floatValue);
        break;
    case VALUE_TYPE_CHARACTER:
    case VALUE_TYPE_BYTE:
        number.byteValue = va_arg(args, int);
        valueLength = 1 + sizeof(number.byteValue);
        break;
    default:
        ASSERT(false);
        valueLength = 0;
        break;
    }

    va_end(args);

    c_store_value_t *storeValue =
        CStore_allocate(sizeof(c_store_value_t) + valueLength);

    ASSERT(storeValue);
    if (valueLength) {
        storeValue->size = valueLength;
        storeValue->value[0] = valueType;
    }

    switch (valueType) {
    case VALUE_TYPE_DOUBLE:
    case VALUE_TYPE_LONG:
        ASSERT(valueLength == 9);
        storeValue->value[1] = number.longValue >> 56;
        storeValue->value[2] = number.longValue >> 48;
        storeValue->value[3] = number.longValue >> 40;
        storeValue->value[4] = number.longValue >> 32;
        storeValue->value[5] = number.longValue >> 24;
        storeValue->value[6] = number.longValue >> 16;
        storeValue->value[7] = number.longValue >> 8;
        storeValue->value[8] = number.longValue >> 0;
        break;
    case VALUE_TYPE_BOOLEAN:
    case VALUE_TYPE_CHARACTER:
    case VALUE_TYPE_BYTE:
        ASSERT(valueLength == 2);
        storeValue->value[1] = number.byteValue;
        break;
    case VALUE_TYPE_SHORT:
        ASSERT(valueLength == 3);
        storeValue->value[1] = number.shortValue >> 8;
        storeValue->value[2] = number.shortValue >> 0;
        break;
    case VALUE_TYPE_STATE:
        _splitValue(textBuffer, textLength, storeValue, 0);
        if (bytes) {
            _splitValue(bytes, bytesLength,
                    storeValue, _splitLength(textLength) - 2);
        }
        ASSERT(textLength + bytesLength == _joinedValueLength(storeValue));
        break;
    case VALUE_TYPE_STRING:
    case VALUE_TYPE_BYTE_ARRAY:
        ASSERT(valueLength);
        _splitValue(bytes, bytesLength, storeValue, 0);
        break;
    case VALUE_TYPE_INTEGER:
    case VALUE_TYPE_FLOAT:
        ASSERT(valueLength == 5);
        storeValue->value[1] = number.intValue >> 24;
        storeValue->value[2] = number.intValue >> 16;
        storeValue->value[3] = number.intValue >> 8;
        storeValue->value[4] = number.intValue >> 0;
        break;
    default:
        ASSERT(!valueLength);
        break;
    }

    return storeValue;
}

void *CStore_openLibrary(const char *libraryPath)
{
    void *libraryHandle;

#ifdef _WIN32
    libraryHandle = LoadLibrary(libraryPath);
#else
    libraryHandle = dlopen(libraryPath, RTLD_LAZY);
#endif

    return libraryHandle;
}

bool CStore_parseBoolEnvValue(c_store_t cStore, char *value, bool defaultValue)
{
    if (!value) return defaultValue;
    if (!*value) return true;

    for (int i = 0; value[i]; ++i) {
        value[i] = toupper((unsigned char) value[i]);
    }

    if (!strcmp(value, "1")) return true;
    if (!strcmp(value, "T")) return true;
    if (!strcmp(value, "Y")) return true;
    if (!strcmp(value, "ON")) return true;
    if (!strcmp(value, "YES")) return true;
    if (!strcmp(value, "TRUE")) return true;

    if (!strcmp(value, "0")) return false;
    if (!strcmp(value, "F")) return false;
    if (!strcmp(value, "N")) return false;
    if (!strcmp(value, "NO")) return false;
    if (!strcmp(value, "OFF")) return false;
    if (!strcmp(value, "FALSE")) return false;

    LOG_WARN(cStore->logger, "The boolean value '%s' is not recognized", value);

    return defaultValue;
}

char *CStore_parseEnvEntry(c_store_t cStore, char *entryString)
{
    ASSERT(entryString);

    char *eqPos = strchr(entryString, '=');

    ASSERT(eqPos);
    *eqPos = '\0';

    char *value = CStore_allocate(strlen(eqPos + 1) + 1);

    ASSERT(value);
    strcpy(value, eqPos + 1);

    return value;
}

void *CStore_resolveSymbol(void *libraryHandle, const char *symbol)
{
    void *address;

#ifdef _WIN32
    address = GetProcAddress(libraryHandle, symbol);
#else
    address = dlsym(libraryHandle, symbol);
#endif

    return address;
}

bool CStore_valueToByteArray(
    c_store_value_t *storeValue,
    c_store_byte_t **bytesValue,
    size_t *bytesLength)
{
    if (CStore_getValueType(storeValue) != VALUE_TYPE_BYTE_ARRAY) return false;

    *bytesLength = _joinedValueLength(storeValue);
    *bytesValue = CStore_allocate(*bytesLength);
    ASSERT(*bytesValue);
    _joinValue(storeValue, *bytesValue);

    return true;

}

bool CStore_valueToDouble(
    c_store_value_t *storeValue,
    c_store_double_t *doubleValue)
{
    union number number;

    switch (CStore_getValueType(storeValue)) {
    case VALUE_TYPE_DOUBLE:
        number.longValue =
            (c_store_long_t) (unsigned char) storeValue->value[1] << 56;
        number.longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[2] << 48;
        number.longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[3] << 40;
        number.longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[4] << 32;
        number.longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[5] << 24;
        number.longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[6] << 16;
        number.longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[7] << 8;
        number.longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[8] << 0;
        break;
    case VALUE_TYPE_FLOAT:
        number.longValue = (unsigned char) storeValue->value[1] << 24;
        number.longValue |= (unsigned char) storeValue->value[2] << 16;
        number.longValue |= (unsigned char) storeValue->value[3] << 8;
        number.longValue |= (unsigned char) storeValue->value[4] << 0;
        break;
    case VALUE_TYPE_STRING:
        {
            char *string;
            char *pointer;

            if (!CStore_valueToString(storeValue, &string)) {
                *doubleValue = 0;
                return false;
            }
            number.doubleValue = strtod(string, &pointer);
            if (*pointer) {
                CStore_free(string);
                return false;
            }
            CStore_free(string);
        }
        break;
    case VALUE_TYPE_LONG:
    case VALUE_TYPE_INTEGER:
    case VALUE_TYPE_SHORT:
    case VALUE_TYPE_BYTE:
        {
            c_store_long_t longValue;

            if (!CStore_valueToLong(storeValue, &longValue)) {
                *doubleValue = 0;
                return false;
            }
            *doubleValue = longValue;
            break;
        }
    default:
        *doubleValue = 0;
        return false;
    }

    *doubleValue = number.doubleValue;

    return true;
}

bool CStore_valueToLong(
    c_store_value_t *storeValue,
    c_store_long_t *longValue)
{
    switch (CStore_getValueType(storeValue)) {
    case VALUE_TYPE_LONG:
        *longValue =
            (c_store_long_t) (unsigned char) storeValue->value[1] << 56;
        *longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[2] << 48;
        *longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[3] << 40;
        *longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[4] << 32;
        *longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[5] << 24;
        *longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[6] << 16;
        *longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[7] << 8;
        *longValue |=
            (c_store_long_t) (unsigned char) storeValue->value[8] << 0;
        break;
    case VALUE_TYPE_INTEGER:
        *longValue = (unsigned char) storeValue->value[1] << 24;
        *longValue |= (unsigned char) storeValue->value[2] << 16;
        *longValue |= (unsigned char) storeValue->value[3] << 8;
        *longValue |= (unsigned char) storeValue->value[4] << 0;
        break;
    case VALUE_TYPE_SHORT:
        *longValue = (unsigned char) storeValue->value[1] << 8;
        *longValue |= (unsigned char) storeValue->value[2] << 0;
        break;
    case VALUE_TYPE_BYTE:
        *longValue = storeValue->value[1];
        break;
    case VALUE_TYPE_STRING:
        {
            char *string;
            char *pointer;

            if (!CStore_valueToString(storeValue, &string)) {
                *longValue = 0;
                return false;
            }
            *longValue = strtol(string, &pointer, 0);
            if (*pointer) {
                *longValue = 0;
                CStore_free(string);
                return false;
            }
            CStore_free(string);
        }
        break;
    case VALUE_TYPE_DOUBLE:
    case VALUE_TYPE_FLOAT:
        {
            c_store_double_t doubleValue;

            if (!CStore_valueToDouble(storeValue, &doubleValue)) {
                *longValue = 0;
                return false;
            }
            *longValue = doubleValue;
            break;
        }
    case VALUE_TYPE_BOOLEAN:
        *longValue = storeValue->value[1] != 0;
        break;
    default:
        *longValue = 0;
        return false;
    }

    return true;
}

bool CStore_valueToStateCode(
    c_store_value_t *storeValue,
    c_store_int_t *stateCode)
{
    if (CStore_getValueType(storeValue) == VALUE_TYPE_STATE) {
        char *stateString = _joinedStringValue(storeValue);
        char *colon = strchr(stateString, ':');
        size_t length = colon? colon - stateString: strlen(stateString);

        *stateCode = length? strtol(stateString, NULL, 10): 0;
        CStore_free(stateString);
    } else {
        c_store_long_t longValue;

        if (!CStore_valueToLong(storeValue, &longValue)) return false;
        *stateCode = longValue;
    }

    return true;
}

bool CStore_valueToStateName(
    c_store_value_t *storeValue,
    char **stateName)
{
    if (CStore_getValueType(storeValue) == VALUE_TYPE_STATE) {
        char *stateString = _joinedStringValue(storeValue);
        char *colon = strchr(stateString, ':');

        if (colon) {
            size_t length = &stateString[storeValue->size] - (colon + 1);

            *stateName = CStore_allocate(length + 1);
            ASSERT(*stateName);
            memcpy(*stateName, colon + 1, length);
            CStore_free(stateString);
        } else *stateName = NULL;
    } else {
        if (!CStore_valueToString(storeValue, stateName)) return false;
    }

    return true;
}

bool CStore_valueToString(
    c_store_value_t *storeValue,
    char **stringValue)
{
    if (CStore_getValueType(storeValue) != VALUE_TYPE_STRING) return false;

    *stringValue = _joinedStringValue(storeValue);

    return true;

}


// Private function definitions.

static char *_joinedStringValue(c_store_value_t *storeValue)
{
    char *stringValue = CStore_allocate(_joinedValueLength(storeValue) + 1);

    ASSERT(stringValue);
    _joinValue(storeValue, stringValue);

    return stringValue;
}

static size_t _joinedValueLength(c_store_value_t *storeValue)
{
    c_store_byte_t *valuePointer = &storeValue->value[1];
    size_t valueLength = 0;

    for (;;) {
        size_t length = (*valuePointer++  & 0xFF) << 8;

        length += *valuePointer++ & 0xFF;
        if (length == 0) break;
        valueLength += length;
        valuePointer += length;
    }

    return valueLength;
}

static void _joinValue(c_store_value_t *storeValue, void *buffer)
{
    c_store_byte_t *valuePointer = &storeValue->value[1];
    c_store_byte_t *bufferPointer = buffer;

    for (;;) {
        size_t length = (*valuePointer++  & 0xFF) << 8;

        length += *valuePointer++ & 0xFF;
        if (length == 0) break;
        memcpy(bufferPointer, valuePointer, length);
        valuePointer += length;
        bufferPointer += length;
    }
}

static size_t _splitLength(size_t joinedLength)
{
    size_t length = joinedLength + (joinedLength / MAX_BYTES_BLOCK + 1) * 2;

    if (length % MAX_BYTES_BLOCK) length += 2;

    return length;
}

static void _splitValue(
        void *value,
        size_t valueLength,
        c_store_value_t *storeValue,
        size_t offset)
{
    c_store_byte_t *joinedPointer = value;
    c_store_byte_t *splitPointer = &storeValue->value[1 + offset];

    for (;;) {
        size_t length = valueLength;

        if (length > MAX_BYTES_BLOCK) length = MAX_BYTES_BLOCK;
        valueLength -= length;

        *splitPointer++ = (length >> 8) & 0xFF;
        *splitPointer++ = length & 0xFF;
        if (length == 0) break;
        memcpy(splitPointer, joinedPointer, length);
        joinedPointer += length;
        splitPointer += length;
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DataBufferPool.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.valve;

import java.nio.ByteBuffer;

import java.util.Deque;
import java.util.LinkedList;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;

/**
 * Data buffer pool.
 */
public final class DataBufferPool
{
    /**
     * Constructs an instance.
     */
    private DataBufferPool() {}

    /**
     * Borrows a buffer.
     *
     * @return The buffer.
     */
    @Nonnull
    @CheckReturnValue
    synchronized ByteBuffer borrowBuffer()
    {
        ByteBuffer buffer = _buffers.pollLast();

        if (buffer == null) {
            buffer = ByteBuffer.allocate(_bufferSize);
        }

        return buffer;
    }

    /**
     * Returns a buffer.
     *
     * @param buffer The buffer.
     */
    synchronized void returnBuffer(@Nonnull final ByteBuffer buffer)
    {
        if (buffer.capacity() == _bufferSize) {
            buffer.clear();
            _buffers.addLast(buffer);
        }
    }

    /**
     * Uses a new buffer size if greater than current.
     *
     * @param bufferSize The new buffer size.
     */
    synchronized void useBufferSize(final int bufferSize)
    {
        Require.success((_bufferSize == 0) || (this == EXPANDING_BUFFERS));

        if (bufferSize > _bufferSize) {
            _bufferSize = bufferSize;
            _buffers.clear();
        }
    }

    /** Expanding buffers. */
    static final DataBufferPool EXPANDING_BUFFERS = new DataBufferPool();

    /** Fixed buffers. */
    static final DataBufferPool FIXED_BUFFERS = new DataBufferPool();

    private int _bufferSize;
    private final Deque<ByteBuffer> _buffers = new LinkedList<ByteBuffer>();
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA
 */

/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.types;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Control Net Path for class, instance, attribute
 * <p>
 * Example (with suitable static import):
 * <p>
 * <code>CNPath path = Identity.instance(1).attr(7)</code>
 *
 * @author Kay Kasemir, László Pataki
 */
@SuppressWarnings(
{
    "hiding", "boxing", "cast"
})
public class CNClassPath
    extends CNPath
{
    public CNClassPath() {}

    protected CNClassPath(final int class_code, final String class_name)
    {
        this.class_code = class_code;
        this.class_name = class_name;
    }

    public CNPath attr(final int attr)
    {
        this.attr = attr;

        return this;
    }

    @Override
    public void decode(
            final ByteBuffer buf,
            int available,
            final StringBuilder log)
        throws Exception
    {
        final byte[] raw = new byte[2];

        buf.get(raw);
        available = ByteBuffer
            .wrap(raw)
            .order(ByteOrder.LITTLE_ENDIAN)
            .getShort();

        if (raw[0] == 0x02) {
            buf.get(raw);

            if (raw[0] == 0x20) {
                this.class_code = new Integer(raw[1]);
                this.class_name = "Ethernet Link";
            }

            buf.get(raw);

            if (raw[0] == 0x24) {
                this.instance(new Integer(raw[1]));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void encode(final ByteBuffer buf, final StringBuilder log)
    {
        buf.put(this.getPathLength());
        buf.put((byte) classSegmentType());

        if (shortClassId()) {
            buf.put((byte) 0);    // Padding
            buf.putShort((short) this.class_code);
        } else {
            buf.put((byte) this.class_code);
        }

        buf.put((byte) instanceSegmentType());

        if (shortInstanceId()) {
            buf.put((byte) 0);    // Padding
            buf.putShort((short) this.instance);
        } else {
            buf.put((byte) this.instance);
        }

        if (hasAttribute()) {
            buf.put((byte) attributeSegmentType());

            if (shortAttributeId()) {
                buf.put((byte) 0);    // Padding
                buf.putShort((short) this.attr);
            } else {
                buf.put((byte) this.attr);
            }
        }
    }

    /** @return Path length in words */
    public byte getPathLength()
    {
        return (byte) (getRequestSize() / 2);
    }

    @Override
    public int getRequestSize()
    {
        int size = 4;    // a base path with 2 single byte elements

        if (shortClassId()) {
            size += 2;
        }

        if (shortInstanceId()) {
            size += 2;
        }

        if (hasAttribute()) {
            size += 2;

            if (shortAttributeId()) {
                size += 2;
            }
        }

        return size;
    }

    @Override
    public int getResponseSize(final ByteBuffer buf)
        throws Exception
    {
        return 2 + this.getPathLength() * 2;
    }

    public CNClassPath instance(final int instance)
    {
        this.instance = instance;

        return this;
    }

    @Override
    public String toString()
    {
        StringBuilder description = new StringBuilder();

        description.append("Path ");

        if (hasAttribute()) {
            description.append("(3 el)");
        } else {
            description.append("(2 el)");
        }

        description
            .append(" Class(0x")
            .append(Integer.toHexString(classSegmentType()))
            .append(" ");
        description
            .append("0x")
            .append(Integer.toHexString(this.class_code))
            .append(") ");
        description.append(this.class_name);

        description
            .append(", instance(0x")
            .append(Integer.toHexString(instanceSegmentType()))
            .append(") ")
            .append(this.instance);

        if (hasAttribute()) {
            description
                .append(", attribute(0x")
                .append(Integer.toHexString(attributeSegmentType()))
                .append(") ")
                .append(this.attr);
        }

        return description.toString();
    }

    private byte attributeSegmentType()
    {
        if (shortAttributeId()) {
            return 0x31;
        }

        return 0x30;
    }

    private byte classSegmentType()
    {
        if (shortClassId()) {
            return 0x21;
        }

        return 0x20;
    }

    private boolean hasAttribute()
    {
        return this.attr > 0;
    }

    private byte instanceSegmentType()
    {
        if (shortInstanceId()) {
            return 0x25;
        }

        return 0x24;
    }

    private boolean shortAttributeId()
    {
        return this.attr > 0xFF;
    }

    private boolean shortClassId()
    {
        return this.class_code > 0xFF;
    }

    private boolean shortInstanceId()
    {
        return this.instance > 0xFF;
    }

    private int class_code;
    private String class_name;
    private int instance = 1, attr = 0;
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

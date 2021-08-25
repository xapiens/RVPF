/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.types;

/**
 * CN Services
 * <p>
 * Spec 4, p.36
 *
 * @author Kay Kasemir, László Pataki
 */
@SuppressWarnings(
{
    "boxing", "hiding"
})
public enum CNService
{
    Get_Attribute_All(0x01),
    Get_Attributes_List(0x03),    // Logix5000 Data Access
    Get_Attribute_Single(0x0E),    // Spec 6 p. 43
    CIP_MultiRequest(0x0A),    // Logix5000 Data Access
    CIP_ReadData(0x4C),    // Logix5000 Data Access
    CIP_ReadDataFragmented(0x52),    // Logix5000 Data Access
    CIP_WriteData(0x4D),    // Logix5000 Data Access
    CIP_WriteDataFragmented(0x53),    // Logix5000 Data Access
    CM_Unconnected_Send(0x52),
    Get_Instance_Attribute_List(0x55),    // Logix5000 Data Access
    Get_Connection_Data(0x56),    // CIP VOL1_3.3: 3-5.5
    Get_Attribute_All_Reply(0x01 | 0x80),
    Get_Attributes_List_Reply(0x03 | 0x80),    // Logix5000 Data Access
    Get_Attribute_Single_Reply(0x0E | 0x80),
    CIP_MultiRequest_Reply(0x0A | 0x80),
    CIP_ReadData_Reply(0x4C | 0x80),
    CIP_ReadDataFragmented_Reply(0x52 | 0x80),
    CIP_WriteData_Reply(0x4D | 0x80),
    CIP_WriteDataFragmente_Reply(0x53 | 0x80),    // Logix5000 Data Access
    Get_Instance_Attribute_List_Reply(0x55 | 0x80),    // Logix5000 Data Access
    CM_Unconnected_Send_Reply(0x52 | 0x80);

    private CNService(final int code)
    {
        this.code = (byte) code;
    }

    /**
     * @param code
     *            Service code
     * @return Matching {@link CNService} or <code>null</code>
     */
    public static CNService forCode(final int code)
    {
        for (final CNService service: values()) {
            if ((byte) code == service.code) {
                return service;
            }
        }

        return null;
    }

    /** @return Code (ID) of service as used in protocol */
    public byte getCode()
    {
        return this.code;
    }

    /**
     * Obtain the 'reply' for a service
     *
     * @return {@link CNService} for the reply, or <code>null</code> if there is no known reply
     */
    public CNService getReply()
    {
        if (this == CM_Unconnected_Send) {
            return null;
        }

        // Is this already a 'reply'?
        if ((this.code & 0x80) == 0x80) {
            return null;
        }

        return forCode(this.code | 0x80);
    }

    /** @return <code>true</code> if this is a 'reply' to a request */
    public boolean isReply()
    {
        return (this.code & 0x80) == 0x80;
    }

    @Override
    public String toString()
    {
        return String.format("%s (0x%02X)", this.name(), this.code);
    }

    final private byte code;
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

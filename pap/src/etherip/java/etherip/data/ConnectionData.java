/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.data;

/**
 * Get_Connection_Data Service Response
 *
 * @see CIP_VOL2_3.3: Table 3-5.27 Get_Connection_Data Service Response
 * @author László Pataki
 */
@SuppressWarnings(
{
    "hiding", "javadoc"
})
public class ConnectionData
{
    public byte getConnectionAdditionalStatus()
    {
        return this.connectionAdditionalStatus;
    }

    public byte getConnectionGeneralStatus()
    {
        return this.connectionGeneralStatus;
    }

    public int getConnectionNumber()
    {
        return this.connectionNumber;
    }

    public int getConnectionSerialNumber()
    {
        return this.connectionSerialNumber;
    }

    public int getOriginatorPort()
    {
        return this.originatorPort;
    }

    public int getTargetPort()
    {
        return this.targetPort;
    }

    public void setConnectionAdditionalStatus(
            final byte connectionAdditionalState)
    {
        this.connectionAdditionalStatus = connectionAdditionalState;
    }

    public void setConnectionGeneralStatus(final byte connectionState)
    {
        this.connectionGeneralStatus = connectionState;
    }

    public void setConnectionNumber(final int connectionNumber)
    {
        this.connectionNumber = connectionNumber;
    }

    public void setConnectionSerialNumber(final int connectionSerialNumber)
    {
        this.connectionSerialNumber = connectionSerialNumber;
    }

    public void setOriginatorPort(final int originatorPort)
    {
        this.originatorPort = originatorPort;
    }

    public void setTargetPort(final int targetPort)
    {
        this.targetPort = targetPort;
    }

    @Override
    public String toString()
    {
        return "ConnectionData [connectionGeneralStatus="
               + this.connectionGeneralStatus + ", connectionAdditionalStatus="
               + this.connectionAdditionalStatus + ", connectionNumber="
               + this.connectionNumber + ", originatorPort="
               + this.originatorPort + ", targetPort=" + this.targetPort
               + ", connectionSerialNumber=" + this.connectionSerialNumber
               + "]";
    }

    int connectionNumber, originatorPort, targetPort, connectionSerialNumber;
    private byte connectionGeneralStatus, connectionAdditionalStatus;
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

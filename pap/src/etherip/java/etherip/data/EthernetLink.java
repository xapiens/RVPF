/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.data;

/**
 * Ethernet Link Object
 *
 * @see CIP_Vol1_3.3: 5-4 Ethernet Link Object
 * @author László Pataki
 */
@SuppressWarnings(
{
    "hiding", "javadoc"
})
public class EthernetLink
{
    public EthernetLink()
    {
        this.interfaceSpeed = null;
        this.interfaceFlags = null;
        this.physicalAddress = null;
        this.interfaceCounters = null;
        this.mediaCounters = null;
        this.interfaceControl = null;
    }

    public String getInterfaceControlRaw()
    {
        return this.interfaceControl;
    }

    public String getInterfaceCountersRaw()
    {
        return this.interfaceCounters;
    }

    public InterfaceFlags getInterfaceFlags()
    {
        return this.interfaceFlags;
    }

    public Integer getInterfaceSpeed()
    {
        return this.interfaceSpeed;
    }

    public String getMediaCountersRaw()
    {
        return this.mediaCounters;
    }

    public String getPhysicalAddress()
    {
        return this.physicalAddress;
    }

    public void setInterfaceControl(final String interfaceControlRaw)
    {
        this.interfaceControl = interfaceControlRaw;
    }

    public void setInterfaceCounters(final String interfaceCountersRaw)
    {
        this.interfaceCounters = interfaceCountersRaw;
    }

    public void setInterfaceFlags(final InterfaceFlags interfaceFlags)
    {
        this.interfaceFlags = interfaceFlags;
    }

    public void setInterfaceSpeed(final Integer interfaceSpeed)
    {
        this.interfaceSpeed = interfaceSpeed;
    }

    public void setMediaCountersRaw(final String mediaCounters)
    {
        this.mediaCounters = mediaCounters;
    }

    public void setPhysicalAddress(final String physicalAddress)
    {
        this.physicalAddress = physicalAddress;
    }

    @Override
    public String toString()
    {
        return "EthernetLink [interfaceSpeed=" + this.interfaceSpeed
               + ", interfaceFlags=" + this.interfaceFlags
               + ", physicalAddress=" + this.physicalAddress
               + ", interfaceCounters=" + this.interfaceCounters
               + ", mediaCounters=" + this.mediaCounters
               + ", interfaceControl=" + this.interfaceControl + "]";
    }

    InterfaceFlags interfaceFlags;
    Integer interfaceSpeed;
    String physicalAddress, interfaceCounters, mediaCounters, interfaceControl;
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

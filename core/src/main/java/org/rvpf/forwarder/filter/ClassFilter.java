/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClassFilter.java 3948 2019-05-02 20:37:43Z SFB $
 */

package org.rvpf.forwarder.filter;

import java.io.Serializable;

import java.util.HashSet;
import java.util.Set;

import org.rvpf.base.ClassDef;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.forwarder.ForwarderModule;

/**
 * Class filter.
 */
public final class ClassFilter
    extends ForwarderFilter.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable[] filter(Serializable message)
    {
        final Class<? extends Serializable> messageClass = message.getClass();

        if (_dropped.isEmpty()) {
            if (!_allowed.contains(messageClass)) {
                message = null;
            }
        } else if (_allowed.isEmpty() || _allowed.contains(messageClass)) {
            if (_dropped.contains(messageClass)) {
                message = null;
            }
        }

        if (message == null) {
            logDropped(
                ForwarderMessages.MESSAGE_CLASS_DROPPED,
                messageClass.getName());
        }

        return (message != null)? new Serializable[] {message, }: NO_MESSAGES;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final ForwarderModule forwarderModule,
            final KeyedGroups filterProperties)
    {
        boolean success = super.setUp(forwarderModule, filterProperties);

        for (final ClassDef classDef:
                filterProperties.getClassDefs(ALLOWED_CLASS_PROPERTY)) {
            final Class<?> messageClass = classDef.getInstanceClass();

            if (messageClass != null) {
                _allowed.add(messageClass);
            } else {
                success = false;
            }
        }

        for (final ClassDef classDef:
                filterProperties.getClassDefs(DROPPED_CLASS_PROPERTY)) {
            final Class<?> messageClass = classDef.getInstanceClass();

            if (messageClass != null) {
                _dropped.add(messageClass);
            } else {
                success = false;
            }
        }

        return success;
    }

    /** Allowed class property. */
    public static final String ALLOWED_CLASS_PROPERTY = "allowed.class";

    /** Dropped class property. */
    public static final String DROPPED_CLASS_PROPERTY = "dropped.class";

    private final Set<Class<?>> _allowed = new HashSet<>();
    private final Set<Class<?>> _dropped = new HashSet<>();
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ToolsMessages.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.tool;

import org.rvpf.base.logger.Messages;

/** Tools messages.
 */
public enum ToolsMessages
    implements Messages.Entry
{
    BAD_LINE,
    BAD_MAGIC,
    BAD_MESSAGE,
    CLASS,
    CLASS_FIELD,
    CLASS_NO_ENTRIES,
    CLASS_NOT_FOUND,
    CLASS_REFERENCE,
    DUPLICATE_MESSAGE_NAME,
    FORMAT_ERROR,
    INVALID_CONSTANT_TYPE,
    MESSAGE_ENTRY_ORDER,
    MESSAGE_ENTRY_UNREFERENCED,
    MESSAGE_NAME_MISSING,
    MESSAGE_NAME_NOT_DEFINED,
    MESSAGE_NAME_ORDER,
    MESSAGE_RESOURCE_NOT_FOUND,
    MESSAGES_VERIFICATION_FAILED,
    MESSAGES_VERIFICATION_SUCCESSFUL,
    MISSING_MESSAGE_NAMES,
    SCANNING_MESSAGE_REFERENCES,
    SCANNING_MESSAGE_REFERENCES_UNDER,
    SINGLE_CLASS,
    VERIFYING_MESSAGES_CLASS,
    VERIFYING_MESSAGES_CLASSES,
    VERIFYING_MESSAGES_RESOURCE,
    VERIFYING_MESSAGES_RESOURCES;

    /** {@inheritDoc}
     */
    @Override
    public String getBundleName()
    {
        return _BUNDLE_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized String toString()
    {
        if (_string == null) {
            _string = Messages.getString(this);
        }

        return _string;
    }

    private static final String _BUNDLE_NAME = "org.rvpf.messages.tool";

    private String _string;
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

/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JNLPMessages.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.jnlp.loader;

import org.rvpf.base.logger.Messages;

/** JNLP messages.
 */
public enum JNLPMessages
    implements Messages.Entry
{
    CACHE_CREATED,
    CACHE_DIRECTORY,
    CACHE_FAILED,
    CACHE_FILE_FOUND,
    CACHE_FILE_LOST,
    CACHE_INDEX_FOUND,
    CACHE_LOCKED,
    CACHE_UNLOCKED,
    CERTIFICATES_DIFFER,
    CONNECT_FAILED,
    CONNECT_SUCCEEDED,
    DOWNLOAD_DELETE_FAILED,
    DOWNLOAD_FAILED,
    DOWNLOADING_FROM,
    ENTRY_NOT_SIGNED,
    FILE_UP_TO_DATE,
    GET_FILE_FAILED,
    IMPLEMENTATION,
    JAR_FAILED,
    JNLP_FAILED,
    JNLP_FILE,
    LAST_MODIFIED_MISSING,
    LOOKING_FOR,
    NO_JAR,
    NO_JNLP_URL,
    NO_MAIN_CLASS,
    OBSOLETE_REMOVE_FAILED,
    OBSOLETE_REMOVED,
    ONE_MAIN_JAR,
    PURGE_FAILED,
    PURGED,
    RESPONSE_FAILED,
    RESPONSE_UNEXPECTED,
    SET_FILE_MODIFIED_FAILED,
    SIGNATURE_VALIDATION,
    SIGNED_ENTRY_MISSING,
    STARTING_APPLICATION,
    UNEXPECTED_EXCEPTION,
    UNREF_REMOVE_FAILED,
    UNREF_REMOVED,
    VERIFICATION_COMPLETED,
    WILL_RETRY;

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

    private static final String _BUNDLE_NAME = "org.rvpf.messages.jnlp";

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

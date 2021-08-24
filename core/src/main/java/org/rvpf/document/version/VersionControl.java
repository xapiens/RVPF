/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: VersionControl.java 4103 2019-07-01 13:31:25Z SFB $
 */

package org.rvpf.document.version;

import java.io.File;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;

/**
 * Version control.
 */
public interface VersionControl
{
    /**
     * Checkouts a module.
     *
     * <p>Used by a metadata script.</p>
     *
     * @param modulePath The module path.
     * @param directory The destination directory.
     * @param revision The optional revision to checkout.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean checkout(
            @Nonnull String modulePath,
            @Nonnull File directory,
            @Nonnull Optional<String> revision);

    /**
     * Disposes of the allocated resources.
     */
    void dispose();

    /**
     * Gets the workspace revision.
     *
     * @return The workspace revision.
     */
    @Nonnull
    @CheckReturnValue
    String getWorkspaceRevision();

    /**
     * Selects the workspace directory.
     *
     * @param directory The workspace directory.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean selectWorkspace(@Nonnull File directory);

    /**
     * Sets the password for authentication.
     *
     * @param password The optional password.
     */
    void setPassword(@Nonnull final Optional<char[]> password);

    /**
     * Sets the user for identification.
     *
     * @param user The user.
     */
    void setUser(@Nonnull final String user);

    /**
     * Updates the workspace.
     *
     * @param revision The optional revision to update to.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean update(@Nonnull Optional<String> revision);

    /** Bad document event. */
    String BAD_DOCUMENT_EVENT = "BadDocument";

    /** Default class of version control provider. */
    ClassDef DEFAULT_VERSION_CONTROL_CLASS = new ClassDefImpl(
        "org.rvpf.ext.SVNVersionControl");

    /** Document restored event. */
    String DOCUMENT_RESTORED_EVENT = "DocumentRestored";

    /** Document updated event. */
    String DOCUMENT_UPDATED_EVENT = "DocumentUpdated";

    /** Document version control properties. */
    String DOCUMENT_VERSION_PROPERTIES = "document.version";

    /** Good document event. */
    String GOOD_DOCUMENT_EVENT = "GoodDocument";

    /** The user password for connection to the repository. */
    String REPOSITORY_PASSWORD_PROPERTY = "repository.password";

    /** The user identification for connection to the repository. */
    String REPOSITORY_USER_PROPERTY = "repository.user";

    /** Update document signal. */
    String UPDATE_DOCUMENT_SIGNAL = "UpdateDocument";

    /**
     * The path of a file whose presence will trigger an automatic update on
     * startup.
     */
    String UPDATE_TRIGGER_PROPERTY = "update.trigger";

    /** The class of the version control provider. */
    String VERSION_CONTROL_CLASS_PROPERTY = "control.class";

    /** The document version control workspace directory. */
    String WORKSPACE_PROPERTY = "workspace";
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

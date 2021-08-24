/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SVNVersionControl.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.ext;

import java.io.File;
import java.io.IOException;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.subversion.javahl.ClientException;
import org.apache.subversion.javahl.SVNClient;
import org.apache.subversion.javahl.callback.InfoCallback;
import org.apache.subversion.javahl.callback.StatusCallback;
import org.apache.subversion.javahl.types.Depth;
import org.apache.subversion.javahl.types.Info;
import org.apache.subversion.javahl.types.Revision;
import org.apache.subversion.javahl.types.Status;

import org.rvpf.base.logger.Logger;
import org.rvpf.document.version.VersionControl;
import org.rvpf.service.ServiceMessages;

/**
 * Subversion version control.
 */
public final class SVNVersionControl
    implements VersionControl
{
    /**
     * Constructs a SVNVersionControl.
     */
    public SVNVersionControl()
    {
        _svn = new SVNClient();
        _LOGGER.info(ExtMessages.SVN_CLIENT, _svn.getVersion().toString());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean checkout(
            final String modulePath,
            final File directory,
            final Optional<String> revisionString)
    {
        final Revision revision = _getRevision(revisionString);

        if (revision == null) {
            return false;
        }

        final String canonicalPath;

        try {
            canonicalPath = directory.getCanonicalPath();
        } catch (final IOException exception) {
            _LOGGER.warn(ServiceMessages.INVALID_PATH, directory);

            return false;
        }

        return _checkout(modulePath, canonicalPath, revision);
    }

    /** {@inheritDoc}
     */
    @Override
    public void dispose()
    {
        _svn.dispose();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getWorkspaceRevision()
    {
        final Info info2;

        try {
            info2 = _getInfo2(_path, null, null);
        } catch (final ClientException exception) {
            throw new RuntimeException(exception);
        }

        return Long.toString(info2.getRev());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean selectWorkspace(final File directory)
    {
        Info info2;

        try {
            _path = directory.getCanonicalPath();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        try {
            info2 = _getInfo2(_path, null, null);
        } catch (final ClientException exception) {
            info2 = null;
        }

        if (info2 == null) {
            _LOGGER.error(ExtMessages.WORKSPACE_SELECT_FAILED, _path);

            return false;
        }

        _LOGGER.info(ExtMessages.REPOSITORY_LOCATION, info2.getUrl());

        final List<Status> statuses = _getStatuses();

        for (final Status status: statuses) {
            if (status.isManaged()) {
                _LOGGER.warn(ExtMessages.WORKSPACE_MODIFIED);

                break;
            }
        }

        try {
            info2 = _getInfo2(
                info2.getUrl(),
                Revision.HEAD,
                new Revision.Number(info2.getRev()));

            _LOGGER
                .info(
                    ExtMessages.REPOSITORY_REVISION,
                    String.valueOf(info2.getRev()));
        } catch (final ClientException exception) {
            _LOGGER
                .warn(
                    ExtMessages.REPOSITORY_CONNECT_FAILED,
                    exception.getMessage());
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void setPassword(final Optional<char[]> password)
    {
        _svn.password(password.isPresent()? new String(password.get()): "");
    }

    /** {@inheritDoc}
     */
    @Override
    public void setUser(final String user)
    {
        _svn.username(user);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean update(final Optional<String> revisionString)
    {
        final Revision revision = _getRevision(revisionString);

        return (revision != null)? _update(revision): false;
    }

    private static Revision _getRevision(final Optional<String> revisionString)
    {
        final long revisionNumber;

        if (revisionString.isPresent()) {
            final String revision = revisionString.get().trim();

            if (revision.isEmpty()) {
                revisionNumber = -1;
            } else {
                try {
                    revisionNumber = Long.parseLong(revision);
                } catch (final IllegalArgumentException exception) {
                    _LOGGER.warn(ExtMessages.BAD_REVISION, revisionString);

                    return null;
                }
            }
        } else {
            revisionNumber = -1;
        }

        return (revisionNumber <= 0)? Revision.HEAD: new Revision.Number(
            revisionNumber);
    }

    private boolean _checkout(
            final String modulePath,
            final String path,
            final Revision revision)
    {
        try {
            _svn
                .checkout(
                    modulePath,
                    path,
                    revision,
                    revision,
                    Depth.infinity,
                    false,
                    false);
        } catch (final ClientException exception) {
            _LOGGER.warn(ExtMessages.CHECKOUT_REJECTED, exception.getMessage());

            return false;
        }

        return true;
    }

    private Info _getInfo2(
            final String source,
            final Revision revision,
            final Revision pegRevision)
        throws ClientException
    {
        final AtomicReference<Info> info2 = new AtomicReference<Info>();

        _svn
            .info2(
                source,
                revision,
                pegRevision,
                Depth.files,
                null,
                new InfoCallback()
                {
                    @Override
                    public void singleInfo(final Info newInfo2)
                    {
                        info2.compareAndSet(null, newInfo2);
                    }
                });

        return info2.get();
    }

    private List<Status> _getStatuses()
    {
        final List<Status> statuses = new LinkedList<Status>();

        try {
            _svn
                .status(
                    _path,
                    Depth.infinity,
                    false,
                    false,
                    false,
                    false,
                    null,
                    new StatusCallback()
                    {
                        @Override
                        public void doStatus(
                                final String path,
                                        final Status status)
                        {
                            statuses.add(status);
                        }
                    });
        } catch (final ClientException exception) {
            throw new RuntimeException(exception);
        }

        return statuses;
    }

    private boolean _update(final Revision revision)
    {
        final Set<String> paths = new HashSet<>();

        paths.add(_path);

        try {
            _svn
                .update(
                    paths,
                    revision,
                    Depth.infinity,
                    false,
                    false,
                    false,
                    false);
        } catch (final ClientException exception) {
            _LOGGER.warn(ExtMessages.UPDATE_REJECTED, exception.getMessage());

            return false;
        }

        return true;
    }

    private static final Logger _LOGGER = Logger
        .getInstance(SVNVersionControl.class);

    private String _path;
    private final SVNClient _svn;
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

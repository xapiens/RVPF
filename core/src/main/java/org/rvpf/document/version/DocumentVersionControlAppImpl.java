/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DocumentVersionControlAppImpl.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.document.version;

import java.io.File;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.alert.Event;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.service.app.ServiceAppImpl;

/**
 * Document version control application implementation.
 */
public final class DocumentVersionControlAppImpl
    extends ServiceAppImpl
    implements ServiceThread.Target
{
    /** {@inheritDoc}
     */
    @Override
    public boolean onEvent(final Event event)
    {
        if (VersionControl.BAD_DOCUMENT_EVENT
            .equalsIgnoreCase(event.getName())) {
            synchronized (_mutex) {
                _restoreEvent = event;
                _mutex.notifyAll();
            }

            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onSignal(final Signal signal)
    {
        if (VersionControl.UPDATE_DOCUMENT_SIGNAL
            .equalsIgnoreCase(signal.getName())) {
            synchronized (_mutex) {
                _updateSignal = signal;
                _mutex.notifyAll();
            }

            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws InterruptedException
    {
        synchronized (_mutex) {
            for (;;) {
                while ((_updateSignal == null) && (_restoreEvent == null)) {
                    _mutex.wait();
                }

                if (_updateSignal != null) {
                    _update();
                    _updateSignal = null;
                    _restoreEvent = null;
                } else if (_restoreEvent != null) {
                    _restore();
                    _restoreEvent = null;
                }
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Service service)
    {
        if (!super.setUp(service)) {
            return false;
        }

        final Config config = service.getConfig();
        final KeyedGroups properties = config
            .getPropertiesGroup(VersionControl.DOCUMENT_VERSION_PROPERTIES);

        if (properties.isMissing()) {
            getThisLogger()
                .error(
                    ServiceMessages.MISSING_PROPERTIES,
                    VersionControl.DOCUMENT_VERSION_PROPERTIES);

            return false;
        }

        final Optional<String> workspace = properties
            .getString(VersionControl.WORKSPACE_PROPERTY);
        final File directory;

        if (!workspace.isPresent()) {
            getThisLogger()
                .error(
                    BaseMessages.MISSING_PROPERTY_IN,
                    VersionControl.WORKSPACE_PROPERTY,
                    VersionControl.DOCUMENT_VERSION_PROPERTIES);

            return false;
        }

        directory = config.getFile(workspace.get());

        if (!directory.isDirectory()) {
            getThisLogger()
                .error(ServiceMessages.WORKSPACE_LOCATION_NOT_DIR, directory);

            return false;
        }

        final ClassDef classDef = properties
            .getClassDef(
                VersionControl.VERSION_CONTROL_CLASS_PROPERTY,
                VersionControl.DEFAULT_VERSION_CONTROL_CLASS);

        _versionControl = classDef.createInstance(VersionControl.class);

        if (_versionControl == null) {
            return false;
        }

        final Optional<String> user = properties
            .getString(VersionControl.REPOSITORY_USER_PROPERTY);
        final Optional<char[]> password = properties
            .getPassword(VersionControl.REPOSITORY_PASSWORD_PROPERTY);

        if (user.isPresent()) {
            _versionControl.setUser(user.get());
            _versionControl.setPassword(password);
        }

        if (!_versionControl.selectWorkspace(directory)) {
            return false;
        }

        getThisLogger()
            .info(
                ServiceMessages.WORKSPACE_LOCATION,
                directory.getAbsolutePath());
        getThisLogger()
            .info(
                ServiceMessages.WORKSPACE_REVISION,
                _versionControl.getWorkspaceRevision());

        final Optional<String> updateTrigger = properties
            .getString(VersionControl.UPDATE_TRIGGER_PROPERTY);

        if (updateTrigger.isPresent()) {
            final File triggerFile = config.getFile(updateTrigger.get());

            getThisLogger()
                .info(
                    ServiceMessages.TRIGGER_FILE,
                    triggerFile.getAbsolutePath());

            if (triggerFile.exists()) {
                getThisLogger().info(ServiceMessages.WORKSPACE_UPDATE);
                _update();

                if (!triggerFile.delete()) {
                    getThisLogger().info(ServiceMessages.TRIGGER_DELETE_FAILED);
                }
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        final ServiceThread thread = new ServiceThread(
            this,
            "Document version control");

        if (_thread.compareAndSet(null, thread)) {
            getThisLogger()
                .debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            Require
                .ignored(
                    thread.interruptAndJoin(getThisLogger(), getJoinTimeout()));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_versionControl != null) {
            _versionControl.dispose();
            _versionControl = null;
        }

        super.tearDown();
    }

    private void _restore()
    {
        final String rejectedRevision = _restoreEvent.getInfo().orElse(null);
        final String currentRevision = _versionControl.getWorkspaceRevision();
        final String restoredRevision;

        if ((_previousRevision != null)
                && !_previousRevision.equals(currentRevision)
                && currentRevision.equals(rejectedRevision)) {
            if (_versionControl.update(Optional.of(_previousRevision))) {
                restoredRevision = _versionControl.getWorkspaceRevision();
                getThisLogger()
                    .info(ServiceMessages.WORKSPACE_RESTORED, restoredRevision);
                getService()
                    .sendEvent(
                        VersionControl.DOCUMENT_RESTORED_EVENT,
                        Optional.of(restoredRevision));
            }

            _previousRevision = null;
        }
    }

    private void _update()
    {
        final Optional<String> requestedRevision = _updateSignal.getInfo();

        _previousRevision = _versionControl.getWorkspaceRevision();

        if (_versionControl.update(requestedRevision)) {
            final String updatedRevision = _versionControl
                .getWorkspaceRevision();
            final String info;

            if (updatedRevision.equals(_previousRevision)) {
                getThisLogger()
                    .info(ServiceMessages.WORKSPACE_UPDATED, updatedRevision);
                info = updatedRevision;
                _previousRevision = null;
            } else {
                getThisLogger()
                    .info(
                        ServiceMessages.WORKSPACE_UPDATED_TO,
                        _previousRevision,
                        updatedRevision);
                info = updatedRevision + " " + _previousRevision;
            }

            if (_updateSignal != null) {
                getService()
                    .sendEvent(
                        VersionControl.DOCUMENT_UPDATED_EVENT,
                        Optional.of(info));
            }
        }
    }

    private final Object _mutex = new Object();
    private String _previousRevision;
    private Event _restoreEvent;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
    private Signal _updateSignal;
    private VersionControl _versionControl;
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

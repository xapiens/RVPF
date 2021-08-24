/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PipeSink.java 4057 2019-06-04 18:44:38Z SFB $
 */

package org.rvpf.store.server.sink;

import java.io.File;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.Optional;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.pipe.PipeRequest;
import org.rvpf.base.pipe.PipeSinkRequest;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.service.ProcessMonitor;
import org.rvpf.service.ServiceMessages;
import org.rvpf.store.server.StoreMessages;

/**
 * Pipe sink.
 *
 * <p>This sink feeds values to an external program. It communicates with this
 * program thru 'pipes' on the program's standard input ('stdin'), output
 * ('stdout') and error ('stderr') streams.</p>
 *
 * <h1>Lines sent to the process</h1>
 *
 * <ol>
 *   <li>The request header:
 *
 *     <ol type="a">
 *       <li>A request ID (long).</li>
 *       <li>The request format version (currently 1).</li>
 *       <li>A request type:
 *
 *         <ul>
 *           <li>+: Update.</li>
 *           <li>-: Delete.</li>
 *         </ul>
 *       </li>
 *     </ol>
 *   </li>
 *   <li>The value description:
 *
 *     <ol type="a">
 *       <li>The name of the point.</li>
 *       <li>The timestamp.</li>
 *       <li>The state inside '[]' (update).</li>
 *       <li>The value between '"' (update).</li>
 *     </ol>
 *   </li>
 * </ol>
 *
 * <h1>Line received from the process</h1>
 *
 * <ol>
 *   <li>The request ID (long).</li>
 *   <li>A response summary:
 *
 *     <ul>
 *       <li>&lt; 0: The request failed.</li>
 *       <li>= 0: The request did nothing.</li>
 *       <li>= 1: Notify.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h1>Loop</h1>
 *
 * <p>The program is expected to loop on the requests until it receives a line
 * containing a single field holding only the digit '0'.</p>
 *
 * <h1>Notes</h1>
 *
 * <ul>
 *   <li>On each line, the items are separated by spaces.</li>
 *   <li>A point may be identified by its UUID.</li>
 *   <li>The timestamp has the ISO 8601 format.</li>
 *   <li>A null state or value is indicated by a missing field.</li>
 *   <li>The request ID is used for synchronization verification and must be
 *     returned verbatim.</li>
 *   <li>Leading spaces are stripped from the lines received from the process;
 *     resulting empty lines are ignored.</li>
 *   <li>The state is encoded by replacing ']' by '[]' and '[' by ']['.</li>
 *   <li>The value is encoded by replacing '"' by '""'.</li>
 * </ul>
 */
public final class PipeSink
    extends SinkModule.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        if (_processMonitor != null) {
            _processMonitor.close();
        }

        super.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean delete(final VersionedValue versionedValue)
    {
        final Point point = versionedValue.getPoint().get();

        return _processRequest(
            PipeSinkRequest.DELETE_REQUEST_TYPE,
            new PointValue(
                point,
                Optional.of(versionedValue.getStamp()),
                null,
                null)) > 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final SinkServiceAppImpl sinkAppImpl)
    {
        if (!super.setUp(sinkAppImpl)) {
            return false;
        }

        final KeyedGroups pipeProperties = sinkAppImpl
            .getServerProperties()
            .getGroup(PIPE_PROPERTIES);

        if (pipeProperties.isMissing()) {
            getThisLogger()
                .error(ServiceMessages.MISSING_PROPERTIES, PIPE_PROPERTIES);

            return false;
        }

        final Optional<String> path = pipeProperties
            .getString(DIRECTORY_PROPERTY);
        final ProcessMonitor.Builder processMonitorBuilder = ProcessMonitor
            .newBuilder();

        processMonitorBuilder
            .setOwnerName(
                pipeProperties
                    .getString(NAME_PROPERTY, Optional.of(_DEFAULT_NAME))
                    .get())
            .setKillDelay(
                pipeProperties
                    .getElapsed(
                            KILL_DELAY_PROPERTY,
                                    Optional.empty(),
                                    Optional.empty()))
            .setDirectory(
                Optional
                    .ofNullable(path.isPresent()? new File(path.get()): null))
            .setCommand(pipeProperties.getString(COMMAND_PROPERTY))
            .setProgram(pipeProperties.getString(PROGRAM_PROPERTY))
            .setArgs(pipeProperties.getStrings(ARG_PROPERTY))
            .setSets(pipeProperties.getStrings(SET_PROPERTY));

        final Optional<ElapsedTime> keepProcess = pipeProperties
            .getElapsed(HOLD_PROPERTY, Optional.empty(), Optional.empty());
        final String charsetName = pipeProperties
            .getString(
                CHARSET_PROPERTY,
                Optional.of(StandardCharsets.UTF_8.name()))
            .get();
        final Charset charset;

        if ((charsetName != null) && (charsetName.trim().length() > 0)) {
            try {
                charset = Charset.forName(charsetName);
            } catch (final IllegalArgumentException exception) {
                getThisLogger()
                    .error(ServiceMessages.CHARSET_UNKNOWN, charsetName);

                return false;
            }

            getThisLogger().info(ServiceMessages.CHARSET, charset.name());
        } else {
            charset = null;
        }

        if (keepProcess.isPresent()) {
            getThisLogger()
                .debug(
                    StoreMessages.KEEP_SINK_PROCESS,
                    Long.valueOf(keepProcess.get().toRaw()),
                    keepProcess.get());

            if ((!keepProcess.get().isEmpty())
                    && (!SnoozeAlarm.validate(
                        keepProcess.get(),
                        this,
                        StoreMessages.KEEP_SINK_PROCESS_TEXT))) {
                return false;
            }
        }

        _processMonitor = processMonitorBuilder
            .setCharset(Optional.ofNullable(charset))
            .setKeepProcess(keepProcess)
            .build();

        return _processMonitor != null;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        close();

        _processMonitor = null;

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean update(final VersionedValue versionedValue)
    {
        return _processRequest(
            PipeSinkRequest.UPDATE_REQUEST_TYPE,
            versionedValue) > 0;
    }

    private int _processRequest(
            final String requestType,
            final PointValue pointValue)
    {
        int summary = 0;

        if (_processMonitor.activateProcess()) {
            try {
                final long requestID = ProcessMonitor.newRequestID();
                final String line;
                String[] fields;

                {
                    final StringBuilder stringBuilder = new StringBuilder();

                    stringBuilder.append(requestID);
                    stringBuilder.append(' ');
                    stringBuilder
                        .append(PipeSinkRequest.REQUEST_FORMAT_VERSION);
                    stringBuilder.append(' ');
                    stringBuilder.append(requestType);

                    if (!_processMonitor.writeLine(stringBuilder.toString())) {
                        return -1;
                    }
                }

                if (!_processMonitor
                    .writeLine(PipeRequest.pointValueToString(pointValue))) {
                    return -1;
                }

                line = _processMonitor.readLine();

                if (line == null) {
                    return -1;
                }

                fields = PipeSinkRequest.SPACE_PATTERN.split(line);

                if (fields.length == 2) {
                    try {
                        if (Long.parseLong(fields[0]) == requestID) {
                            summary = Integer.parseInt(fields[1]);
                        } else {
                            fields = null;
                        }
                    } catch (final NumberFormatException exception) {
                        fields = null;
                    }
                } else {
                    fields = null;
                }

                if (fields == null) {
                    getThisLogger()
                        .warn(ServiceMessages.BAD_RESPONSE_SUMMARY, line);

                    return -1;
                }
            } finally {
                _processMonitor.freeProcess();
            }
        } else {
            abort();
        }

        return summary;
    }

    /** An argument for the process. */
    public static final String ARG_PROPERTY = "arg";

    /**
     * Specifies the java Charset to use instead of the local default for
     * communication with the external program.
     */
    public static final String CHARSET_PROPERTY = "charset";

    /** The operating system command used to activate the process. */
    public static final String COMMAND_PROPERTY = "command";

    /**
     * Specifies the working directory into which the external program should
     * execute.
     */
    public static final String DIRECTORY_PROPERTY = "dir";

    /**
     * The time in millis during which an idle external program will be held. A
     * negative value means no limit, zero means immediate termination after
     * use. Defaults to no limit.
     */
    public static final String HOLD_PROPERTY = "hold";

    /** The delay before killing a process after closing its input. */
    public static final String KILL_DELAY_PROPERTY = "kill.delay";

    /** An identifying name for the pipe program. */
    public static final String NAME_PROPERTY = "name";

    /** The pipe properties group. */
    public static final String PIPE_PROPERTIES = "pipe";

    /** The path to the program file. */
    public static final String PROGRAM_PROPERTY = "program";

    /** Sets an environment variable. */
    public static final String SET_PROPERTY = "set";
    private static final String _DEFAULT_NAME = "PipeSink";

    private ProcessMonitor _processMonitor;
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

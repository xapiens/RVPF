/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PipeExecutor.java 4056 2019-06-04 15:21:02Z SFB $
 */

package org.rvpf.processor.engine.executor;

import java.io.File;
import java.io.Serializable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Params;
import org.rvpf.base.exception.ServiceNotReadyException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.pipe.PipeEngineRequest;
import org.rvpf.base.pipe.PipeRequest;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.config.Config;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.service.ProcessMonitor;
import org.rvpf.service.ServiceMessages;

/**
 * Pipe executor.
 *
 * <p>This executor uses an external program to produce results from inputs. It
 * communicates with this program thru 'pipes' on the program's standard input
 * ('stdin'), output ('stdout') and error ('stderr') streams.</p>
 *
 * <h1>Lines sent to the program's 'stdin'</h1>
 *
 * <ol>
 *   <li>Five integers:
 *
 *     <ol type="a">
 *       <li>A request ID (long).</li>
 *       <li>The request format version (currently 1).</li>
 *       <li>The number of transform params.</li>
 *       <li>The number of point params.</li>
 *       <li>The number of input values.</li>
 *     </ol>
 *   </li>
 *   <li>The requested value description:
 *
 *     <ol type="a">
 *       <li>The name of the point.</li>
 *       <li>The timestamp.</li>
 *       <li>The state inside '[]'.</li>
 *       <li>The stored value between '"'.</li>
 *     </ol>
 *   </li>
 *   <li>A line for each transform param.</li>
 *   <li>A line for each point param.</li>
 *   <li>A line for each input value containing:
 *
 *     <ol type="a">
 *       <li>The name of the point.</li>
 *       <li>The timestamp.</li>
 *       <li>The state inside '[]'.</li>
 *       <li>The stored value between '"'.</li>
 *     </ol>
 *   </li>
 * </ol>
 *
 * <h1>Lines received from the program's 'stdout'</h1>
 *
 * <ol>
 *   <li>Two integers:
 *
 *     <ol type="a">
 *       <li>The request ID (long).</li>
 *       <li>A response summary:
 *
 *         <ul>
 *           <li>&lt; 0: No result.</li>
 *           <li>= 0: A null result.</li>
 *           <li>&gt; 0: The number of result lines.</li>
 *         </ul>
 *       </li>
 *     </ol>
 *   </li>
 *   <li>A line for each result value containing:
 *
 *     <ol type="a">
 *       <li>The name of the point.</li>
 *       <li>The timestamp.</li>
 *       <li>The state inside '[]'.</li>
 *       <li>The value between '"'.</li>
 *     </ol>
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
 *   <li>A missing input is indicated by omitting timestamp, state and
 *     value.</li>
 *   <li>A null state or value is indicated by a missing field.</li>
 *   <li>The request ID is used for synchronization verification and must be
 *     returned verbatim.</li>
 *   <li>Leading spaces are stripped from the lines received from the process;
 *     resulting empty lines are ignored.</li>
 *   <li>The state is encoded by replacing ']' by '[]' and '[' by ']['.</li>
 *   <li>The value is encoded by replacing '"' by '""'.</li>
 * </ul>
 */
public final class PipeExecutor
    implements EngineExecutor
{
    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        if (_processMonitor != null) {
            _processMonitor.close();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void disposeContext(final Serializable context) {}

    /** {@inheritDoc}
     */
    @Override
    public List<PointValue> execute(
            final ResultValue resultValue,
            final String[] params,
            final Serializable context)
        throws ServiceNotReadyException
    {
        final _Context pipeContext = (_Context) context;
        final List<PointValue> response;

        if (_processMonitor.activateProcess()) {
            try {
                final long requestID = _sendRequest(
                    resultValue,
                    params,
                    pipeContext);

                if (requestID <= 0) {
                    response = null;
                } else {
                    response = _getResponse(
                        requestID,
                        resultValue,
                        pipeContext);
                }
            } finally {
                _processMonitor.freeProcess();
            }
        } else {
            throw new ServiceNotReadyException();
        }

        return response;
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable newContext(final Params params, final Logger logger)
    {
        final _Context context = new _Context();

        context.failReturnsNull = params
            .getBoolean(Transform.FAIL_RETURNS_NULL_PARAM);

        context.timeLimit = params
            .getElapsed(
                TIME_LIMIT_PARAM,
                Optional.ofNullable(_timeLimit),
                Optional.ofNullable(_timeLimit))
            .orElse(null);

        if (context.timeLimit != null) {
            logger.info(ProcessorMessages.TIME_LIMIT, context.timeLimit);
        }

        context.params = params.getStrings(Transform.PARAM_PARAM);

        context.logger = logger;

        return context;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final String name,
            final Params params,
            final Config config,
            final Logger logger)
    {
        _timeLimit = params
            .getElapsed(TIME_LIMIT_PARAM, Optional.empty(), Optional.empty())
            .orElse(null);

        final String path = params.getString(DIRECTORY_PARAM).orElse(null);
        final ProcessMonitor.Builder processMonitorBuilder = ProcessMonitor
            .newBuilder();

        processMonitorBuilder
            .setOwnerName(name)
            .setKillDelay(
                params
                    .getElapsed(
                            KILL_DELAY_PARAM,
                                    Optional.empty(),
                                    Optional.empty()))
            .setService(
                config.hasService()? Optional
                    .of(config.getService()): Optional.empty())
            .setDirectory(
                Optional.ofNullable((path == null)? null: new File(path)))
            .setCommand(params.getString(COMMAND_PARAM))
            .setProgram(params.getString(PROGRAM_PARAM))
            .setArgs(params.getStrings(ARG_PARAM))
            .setSets(params.getStrings(SET_PARAM));

        final ElapsedTime keepProcess = params
            .getElapsed(
                KEEP_PROCESS_PARAM,
                Optional.of(_DEFAULT_KEEP_PROCESS),
                Optional.empty())
            .get();
        final String charsetName = params
            .getString(CHARSET_PARAM, Optional.of(DEFAULT_CHARSET))
            .get();
        final Charset charset;

        if ((charsetName != null) && (charsetName.trim().length() > 0)) {
            try {
                charset = Charset.forName(charsetName);
            } catch (final IllegalArgumentException exception) {
                logger.error(ServiceMessages.CHARSET_UNKNOWN, charsetName);

                return false;
            }

            logger.info(ServiceMessages.CHARSET, charset.name());
        } else {
            charset = null;
        }

        if (keepProcess.toMillis() != _DEFAULT_KEEP_PROCESS.toMillis()) {
            logger
                .debug(
                    ProcessorMessages.KEEP_PROCESS,
                    name,
                    Long.valueOf(keepProcess.toMillis()),
                    keepProcess);
        }

        _processMonitor = processMonitorBuilder
            .setCharset(Optional.ofNullable(charset))
            .setKeepProcess(Optional.of(keepProcess))
            .build();

        return _processMonitor != null;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        close();
    }

    private List<PointValue> _getResponse(
            final long requestID,
            final ResultValue resultValue,
            final _Context context)
    {
        final List<PointValue> response = new LinkedList<PointValue>();
        String line = _processMonitor.readLine();
        int summary = 0;
        String[] fields;
        PointValue pointValue;

        // Gets the response summary.

        if (line == null) {
            return null;
        }

        fields = PipeEngineRequest.SPACE_PATTERN.split(line);

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
            context.logger.warn(ServiceMessages.BAD_RESPONSE_SUMMARY, line);

            return null;
        }

        // Gets the results.

        if (summary == 0) {
            pointValue = new PointValue(resultValue);
            pointValue.setValue(null);
            response.add(pointValue);
        } else {
            for (int i = 0; i < summary; ++i) {
                line = _processMonitor.readLine();
                pointValue = PipeRequest.stringToPointValue(line);

                if ((pointValue == null) || !pointValue.hasStamp()) {
                    context.logger
                        .warn(ProcessorMessages.BAD_RESPONSE_VALUE, line);

                    return null;
                }

                response.add(pointValue);
            }
        }

        if (response.isEmpty()) {
            if (context.failReturnsNull) {
                pointValue = new PointValue(resultValue);
                pointValue.setValue(null);
            } else {
                pointValue = null;
            }

            response.add(pointValue);
        }

        return response;
    }

    /**
     * Sends a request to the program.
     *
     * @param resultValue The result value requested.
     *
     * @return The request ID (0 on failure).
     */
    private long _sendRequest(
            @Nonnull final ResultValue resultValue,
            @Nonnull final String[] pointParams,
            @Nonnull final _Context context)
    {
        final List<PointValue> inputValues = resultValue.getInputValues();
        final long requestID = ProcessMonitor.newRequestID();

        if (!_processMonitor
            .writeLine(
                requestID + " " + PipeEngineRequest.REQUEST_FORMAT_VERSION
                + " " + context.params.length + " " + pointParams.length + " "
                + inputValues.size())) {
            return 0;
        }

        if (!_writePointValue(resultValue)) {
            return 0;
        }

        for (int i = 0; i < context.params.length; ++i) {
            if (!_processMonitor
                .writeLine(PipeRequest.cleanString(context.params[i]))) {
                return 0;
            }
        }

        for (final String pointParam: pointParams) {
            if (!_processMonitor
                .writeLine(PipeRequest.cleanString(pointParam))) {
                return 0;
            }
        }

        for (final PointValue inputValue: inputValues) {
            if (!_writePointValue(inputValue)) {
                return 0;
            }
        }

        _processMonitor
            .limit((context.timeLimit != null)
                   ? context.timeLimit.toMillis(): 0);

        return requestID;
    }

    private boolean _writePointValue(@Nonnull final PointValue pointValue)
    {
        return _processMonitor
            .writeLine(PipeRequest.pointValueToString(pointValue));
    }

    /** An argument for the process. */
    public static final String ARG_PARAM = "Arg";

    /** The character set for the program execution. */
    public static final String CHARSET_PARAM = "Charset";

    /** The operating system command used to activate the process. */
    public static final String COMMAND_PARAM = "Command";

    /** Default character set. */
    public static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();

    /**
     * Specifies the working directory for the program execution. Defaults to
     * the current working directory.
     */
    public static final String DIRECTORY_PARAM = "Directory";

    /**
     * The number of milliseconds to keep the process active between
     * invocations. A negative value means no limit. The default is zero: the
     * process is not kept active.
     */
    public static final String KEEP_PROCESS_PARAM = "KeepProcess";

    /** The delay before killing a process after closing its input. */
    public static final String KILL_DELAY_PARAM = "KillDelay";

    /** The path to the program file. */
    public static final String PROGRAM_PARAM = "Program";

    /** Sets an environment variable. */
    public static final String SET_PARAM = "Set";

    /** The time limit to produce a result. */
    public static final String TIME_LIMIT_PARAM = "TimeLimit";
    private static final ElapsedTime _DEFAULT_KEEP_PROCESS = ElapsedTime
        .fromMillis(0);
    private static final long serialVersionUID = 1L;

    private ProcessMonitor _processMonitor;
    private ElapsedTime _timeLimit;

    /**
     * Context.
     */
    private static final class _Context
        implements Serializable
    {
        /**
         * Constructs an instance.
         */
        _Context() {}

        private static final long serialVersionUID = 1L;

        boolean failReturnsNull;
        Logger logger;
        String[] params;
        ElapsedTime timeLimit;
    }
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

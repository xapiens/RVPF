/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PipeProgramExample.java 4095 2019-06-24 17:44:43Z SFB $
 */

/**
 * Related Values Processing Framework.
 *
 * $Id: PipeProgramExample.java 4095 2019-06-24 17:44:43Z SFB $
 */
package org.rvpf.tests.example;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.pipe.PipeEngineRequest;
import org.rvpf.base.pipe.PipeRequest;
import org.rvpf.base.pipe.PipeSinkRequest;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;

/**
 * Pipe program example.
 */
public final class PipeProgramExample
{
    private PipeProgramExample() {}

    /**
     * Main program entry.
     *
     * @param args The application arguments.
     */
    public static void main(@Nonnull final String[] args)
    {
        final String mode = (args.length == 1)? args[0]: null;

        if (TRANSFORM_MODE.equalsIgnoreCase(mode)) {
            PipeRequest
                .debug("Started " + PROGRAM_NAME + " in " + TRANSFORM_MODE
                       + " mode");
            _transform();
        } else if (SINK_MODE.equalsIgnoreCase(mode)) {
            PipeRequest
                .debug("Started " + PROGRAM_NAME + " in " + SINK_MODE
                       + " mode");
            _sink();
        } else {
            throw PipeRequest.error("Usage: PipeProgramExample TRANSFORM|SINK");
        }

        PipeRequest.debug("Stopped " + PROGRAM_NAME);
    }

    private static void _sink()
    {
        for (;;) {
            final Optional<PipeSinkRequest> optionalRequest = PipeSinkRequest
                .nextRequest();

            if (!optionalRequest.isPresent()) {
                break;
            }

            final PipeSinkRequest request = optionalRequest.get();
            final PointValue pointValue = request.getPointValue();

            PipeRequest
                .debug("Got request " + request.getRequestID() + " ("
                       + request.getRequestType() + ") for point '"
                       + pointValue.getPointName().get() + "'");

            if (pointValue.getState() != null) {
                PipeRequest.debug("State: {" + pointValue.getState() + "}");
            }

            if (pointValue.getValue() != null) {
                PipeRequest.debug("Value: {" + pointValue.getValue() + "}");
            }

            request.respond(1);
        }
    }

    private static void _transform()
    {
        for (;;) {
            final Optional<PipeEngineRequest> optionalRequest =
                PipeEngineRequest
                    .nextRequest();

            if (!optionalRequest.isPresent()) {
                break;
            }

            final PipeEngineRequest request = optionalRequest.get();

            PipeRequest
                .debug("Got request " + request.getRequestID()
                       + " (Transform) for point '"
                       + request.getResult().getPointName().get() + "'");

            Require.success(request.getTransformParams().length == 1);
            Require.success(request.getPointParams().length == 1);
            Require.success(request.getInputs().length > 0);

            final double modulo = Double
                .parseDouble(request.getTransformParams()[0]);
            final double factor = Double
                .parseDouble(request.getPointParams()[0]);

            if (modulo > 0) {
                double total = 0.0;
                boolean containsNulls = false;

                for (final PointValue inputValue: request.getInputs()) {
                    if (inputValue.getValue() != null) {
                        total += Double
                            .parseDouble((String) inputValue.getValue());
                    } else {
                        containsNulls = true;

                        break;
                    }
                }

                request.setResultState(request.getInputs()[0].getState());
                request
                    .setResultValue(
                        containsNulls? null: String
                            .valueOf((total * factor) % modulo));
            } else {
                request.clearResults();
            }

            request.respond();
        }
    }

    public static final String PROGRAM_NAME = "PipeProgramExample";
    public static final String SINK_MODE = "SINK";
    public static final String TRANSFORM_MODE = "TRANSFORM";
}

// End.








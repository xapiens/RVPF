/** Related Values Processing Framework.
 *
 * $Id: test-rvpf_pipe.c 2377 2014-09-16 15:04:26Z SFB $
 */
#include "rvpf_pipe.h"

#include <ctype.h>
#include <math.h>
#include <stdbool.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

// Private macro definitions.

#define PROGRAM_NAME "test-rvpf_pipe"
#define SINK_MODE "SINK"
#define TRANSFORM_MODE "TRANSFORM"

// Private forward declarations.

static void _doSink(void);

static void _doTransform(void);

// Main.

extern int main(int argc, char **argv)
{
    char *mode = argc == 2? argv[1]: NULL;

    if (setjmp(rvpf_pipe_jmp_buf)) {
        if (!rvpf_pipe_status) {
            rvpf_pipe_debug("Stopped %s", PROGRAM_NAME);
        }

        return rvpf_pipe_status;
    }

    rvpf_pipe_debug("%s", rvpf_pipe_version());

    if (mode) {
        for (char *pos = mode; *pos; ++pos) {
            *pos = toupper(*pos);
        }

        if (!strcmp(mode, TRANSFORM_MODE)) {
            rvpf_pipe_debug("Started %s in %s mode", PROGRAM_NAME, mode);
            _doTransform();
        } else if (!strcmp(mode, SINK_MODE)) {
            rvpf_pipe_debug("Started %s in %s mode", PROGRAM_NAME, mode);
            _doSink();
        }
    }

    rvpf_pipe_error("Usage: %s TRANSFORM|SINK", PROGRAM_NAME);
}

// Private function definitions.

static void _doSink(void)
{
    while (true) {
        RVPF_PIPE_SinkRequest request = rvpf_pipe_nextSinkRequest();
        RVPF_PIPE_PointValue pointValue = rvpf_pipe_getSinkPointValue(request);

        rvpf_pipe_debug(
            "Got request %s (%s) for point '%s'",
            rvpf_pipe_getSinkRequestID(request),
            rvpf_pipe_sink_request_types[rvpf_pipe_getSinkRequestType(request)],
            pointValue->pointName);

        if (pointValue->state) rvpf_pipe_debug("State: {%s}", pointValue->state);
        if (pointValue->value) rvpf_pipe_debug("Value: {%s}", pointValue->value);

        rvpf_pipe_endSinkRequest(request, 1);
    }
}

static void _doTransform(void)
{
    while (true) {
        RVPF_PIPE_EngineRequest request = rvpf_pipe_nextEngineRequest();

        rvpf_pipe_debug(
            "Got request %s (Transform) for point '%s'",
            rvpf_pipe_getEngineRequestID(request),
            rvpf_pipe_getEngineResult(request)->pointName);

        if (rvpf_pipe_getEngineTransformParamsCount(request) != 1) {
            rvpf_pipe_error("The transform should have 1 parameter");
        }
        if (rvpf_pipe_getEnginePointParamsCount(request) != 1) {
            rvpf_pipe_error("The point should have 1 parameter");
        }
        if (rvpf_pipe_getEngineInputsCount(request) < 1) {
            rvpf_pipe_error("The point should have at least 1 input");
        }

        double modulo =
            strtod(rvpf_pipe_getEngineTransformParam(request, 1), NULL);
        double factor =
            strtod(rvpf_pipe_getEnginePointParam(request, 1), NULL);

        if (modulo > 0) {
            int inputsCount = rvpf_pipe_getEngineInputsCount(request);
            double total = 0.0;
            bool containsNulls = false;

            for (int i = 1; i <= inputsCount; ++i) {
                char *value = rvpf_pipe_getEngineInput(request, i)->value;

                if (value) {
                    total += strtod(value, NULL);
                } else {
                    containsNulls = true;
                    break;
                }
            }

            if (containsNulls) {
                rvpf_pipe_setEngineResultValue(request, NULL);
            } else {
                char buffer[80];

                snprintf(
                    buffer,
                    sizeof(buffer), "%.1f",
                    fmod(total * factor, modulo));
                rvpf_pipe_setEngineResultValue(request, buffer);
                rvpf_pipe_setEngineResultState(request,
                    rvpf_pipe_getEngineInput(request, 1)->state);
            }
        } else {
            rvpf_pipe_clearEngineResults(request);
        }

        rvpf_pipe_endEngineRequest(request);
    }
}

// End.

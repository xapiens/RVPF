// $Id: script-engine-tests.js 3840 2018-11-20 21:20:34Z SFB $

'use strict';

var ScriptExecutor = Java.type("org.rvpf.processor.engine.executor.ScriptExecutor");
var LogManager = Java.type("org.apache.logging.log4j.LogManager");
var Double = Java.type("java.lang.Double");

var metadata = this[ScriptExecutor.METADATA_ATTRIBUTE];

var _NAME = "script-engine-tests";
var _LOGGER = LogManager.getLogger("org.rvpf." + _NAME);

_LOGGER.debug("JavaScript engine version: " + this[ScriptExecutor.ENGINE_VERSION_PROPERTY]);

var global = this;

function assert(assertion)
{
    if (!assertion) throw new AssertionError();
}

function apply()
{
    _LOGGER.debug("Apply text execution (" + metadata.getServiceUUID().get().toString() + ")");

    var result = global[ScriptExecutor.RESULT_ATTRIBUTE];
    var results = global[ScriptExecutor.RESULTS_ATTRIBUTE];
    var transformParams = global[ScriptExecutor.TRANSFORM_PARAMS_ATTRIBUTE];
    var pointParams = global[ScriptExecutor.POINT_PARAMS_ATTRIBUTE];
    var inputs = global[ScriptExecutor.INPUTS_ATTRIBUTE];

    assert(result != null);
    assert(results != null);
    assert(transformParams.size() == 1);
    assert(pointParams.size() == 1);
    assert(inputs.size() > 0);

    var modulo = new Double(transformParams.get(0));
    var factor = new Double(pointParams.get(0));

    if (modulo > 0) {
        var total = 0.0;
        var containsNull = false;
        var iterator = inputs.iterator();

        while (iterator.hasNext()) {
            var input = iterator.next();

            if (input.getValue() != null) {
                total = total + new Double(input.getValue());
            } else {
                containsNull = true;
                break;
            }
        }
        result.setState(inputs.get(0).getState());
        if (containsNull) {
            result.setValue(null);
        } else {
            result.setValue(new Double((total * factor) % modulo));
        }
    } else results.clear();
}

_LOGGER.debug("Script loaded (" + metadata.getServiceUUID().get().toString() + ")")

// End.

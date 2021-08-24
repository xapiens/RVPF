# $Id: script-engine-tests.py 3840 2018-11-20 21:20:34Z SFB $

from java.lang import Double
from org.rvpf.base.logger import StringLogger
from org.rvpf.processor.engine.executor import ScriptExecutor
import sys, os

try: True
except:
    True = 1
    False = not True

metadata = globals()[ScriptExecutor.METADATA_ATTRIBUTE]

_NAME = 'script-engine-tests'
_LOGGER = StringLogger.getInstance('org.rvpf.' + _NAME)

_LOGGER.debug("Jython engine version: {0}", globals()[ScriptExecutor.ENGINE_VERSION_PROPERTY])

def apply():

    _LOGGER.debug("Apply text execution {0}", metadata.getServiceUUID().get())
    result = globals()[ScriptExecutor.RESULT_ATTRIBUTE]
    assert result is not None
    results = globals()[ScriptExecutor.RESULTS_ATTRIBUTE]
    assert results is not None

    transformParams = globals()[ScriptExecutor.TRANSFORM_PARAMS_ATTRIBUTE]
    assert transformParams.size() == 1
    modulo = float(str(transformParams.get(0)))

    pointParams = globals()[ScriptExecutor.POINT_PARAMS_ATTRIBUTE]
    assert pointParams.size() == 1
    factor = float(str(pointParams.get(0)))

    inputs = globals()[ScriptExecutor.INPUTS_ATTRIBUTE]
    assert inputs.size() > 0

    if modulo > 0:
        total = 0.0
        containsNull = False
        iterator = inputs.iterator()
        while iterator.hasNext():
            input = iterator.next()
            if input.getValue() is not None:
                total = total + float(str(input.getValue()))
            else:
                containsNull = True
                break
        result.setState(inputs.get(0).getState())
        if containsNull: result.setValue(None)
        else: result.setValue(Double((total * factor) % modulo))
    else:
        results.clear()

_LOGGER.debug("Script loaded ({0})", metadata.getServiceUUID().get())

# End.

@ECHO OFF

REM $Id: wrap.cmd 3078 2016-06-26 20:30:42Z SFB $

CD bin
CALL setup\rvpf-env
CD ..

"%RVPF_HOME%\core\wrap" %*

REM End.

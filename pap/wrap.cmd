@ECHO OFF

REM $Id: wrap.cmd 3191 2016-09-27 14:42:51Z SFB $

SETLOCAL

CD bin
CALL setup\rvpf-env
CD ..

"%RVPF_HOME%\core\wrap" %*

REM End.

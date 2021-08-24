@ECHO OFF

REM $$

SETLOCAL

CD /D %~dp0

SET CP=../../../lib/opt/derbytools.jar;../../../lib/opt/derby.jar

SET OPTS=-cp %CP%
SET OPTS=%OPTS% -Dderby.system.home=../../../data/derby
SET OPTS=%OPTS% -Dij.database=jdbc:derby:RVPF;create=true

"java" %OPTS% org.apache.derby.tools.ij %1

REM End.

@ECHO OFF

REM $Id: h2.cmd 807 2008-04-03 12:21:47Z SFB $

SETLOCAL

CD /D %~dp0

SET CP=..\..\..\lib\opt\h2.jar

"java" -cp %CP% org.h2.tools.RunScript -url jdbc:h2:file:../../../data/h2/rvpf -user sa -script %1

REM End.

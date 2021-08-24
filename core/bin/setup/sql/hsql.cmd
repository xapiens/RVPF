@ECHO OFF

REM $Id: hsql.cmd 807 2008-04-03 12:21:47Z SFB $

SETLOCAL

CD /D %~dp0

SET CP=../../../lib/hsqldb.jar

"java" -cp %CP% org.hsqldb.util.ScriptTool -url jdbc:hsqldb:file:../../../data/hsqldb/ -database rvpf -script %1

REM End.

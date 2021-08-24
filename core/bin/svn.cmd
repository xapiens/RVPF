@ECHO OFF

REM $Id: svn.cmd 3280 2016-12-14 20:20:41Z SFB $

SETLOCAL

SET CLASSPATH=%RVPF_LIB%\svnkit-all.jar

"%JAVA%" -Dsun.io.useCanonCaches=false org.tmatesoft.svn.cli.SVN %*

REM End.

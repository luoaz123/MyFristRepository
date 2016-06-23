@echo off

rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

rem -----------------------------------------------------------------------------
rem Start Script for the CATALINA Server
rem
rem $Id: run.bat 562770 20011-12-04 22:13:58Z markt $
rem -----------------------------------------------------------------------------

if "%JAVA_HOME%" == "" goto javaerror
if not exist "%JAVA_HOME%\bin\java.exe" goto javaerror
set JXVS_HOME=%CD%\..
goto run

:javaerror
echo.
echo Error: JAVA_HOME environment variable not set, JXVS not started.
echo.
goto end

:run
if "%1" == "" goto bootstrap
if "%1" == "stop" goto bootstrap
echo Usage: JXVS.sh ( commands ... )
echo commands:
echo   start             Start Catalina in a separate window.
echo   stop              Stop Catalina, waiting up to 5 seconds for the process to end.
goto end

:bootstrap
start "JXVS" "%JAVA_HOME%\bin\java" -Xdebug -Xint -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -DjxvsHome="%JXVS_HOME%" -classpath %JXVS_HOME%\lib\startup.jar org.sainfy.jxvs.launcher.Launcher
goto end

:end
@REM
@REM Copyright 2015 the original author or authors.
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM      https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Startup Batch Script
@REM

@echo off
@setlocal

set ERROR_CODE=0

@REM Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%"=="" goto OkJHome

for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
goto checkJCmd

:OkJHome
set "JAVACMD=%JAVA_HOME%\bin\java.exe"

:checkJCmd
if exist "%JAVACMD%" goto init

echo Error: JAVA_HOME is not defined and no 'java' command could be found in your PATH. >&2
echo Please set the JAVA_HOME variable in your environment to match the location of your Java installation. >&2
goto error

:init
@REM Find the project base dir, i.e. the directory that contains the folder ".mvn".
@REM Fallback to current working directory if not found.

set "EXEC_DIR=%CD%"
set "WDIR=%EXEC_DIR%"
:findDir
if exist "%WDIR%"\.mvn goto baseDirFound
cd "%WDIR%\.."
if "%WDIR%"=="%CD%" goto baseDirNotFound
set "WDIR=%CD%"
goto findDir

:baseDirFound
set "MAVEN_PROJECTBASEDIR=%WDIR%"
cd "%EXEC_DIR%"
goto endDisplayBaseDir

:baseDirNotFound
set "MAVEN_PROJECTBASEDIR=%EXEC_DIR%"
cd "%EXEC_DIR%"

:endDisplayBaseDir

@REM ==== START VALIDATION ====

if not "%MAVEN_PROJECTBASEDIR%"=="" goto endMavDir
echo Error: MAVEN_PROJECTBASEDIR is not set. >&2
goto error

:endMavDir

@REM ==== START MAVEN WRAPPER DOWNLOAD ====

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

if exist "%WRAPPER_JAR%" goto runWrapper
echo Downloading Maven Wrapper... >&2
powershell -Command "&{"^
	"$webclient = new-object System.Net.WebClient;"^
	"$webclient.DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"^
	"}"
if "%ERRORLEVEL%"=="0" goto runWrapper
echo Error: Failed to download Maven Wrapper. >&2
goto error

:runWrapper
"%JAVACMD%" ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*

if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%

cmd /C exit /B %ERROR_CODE%

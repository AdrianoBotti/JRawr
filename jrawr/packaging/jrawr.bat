@echo off
setlocal EnableExtensions

set "APP_DIR=%~dp0"
if "%APP_DIR:~-1%"=="\" set "APP_DIR=%APP_DIR:~0,-1%"
set "DATA_DIR=%APP_DIR%\data"
if not defined JRAWR_ADDRESS set "JRAWR_ADDRESS=127.0.0.1"
if not defined JRAWR_PORT set "JRAWR_PORT=8080"

if not defined LOCALAPPDATA (
    echo LOCALAPPDATA is not set; cannot determine the user data directory. 1>&2
    exit /b 1
)
set "USER_DATA_DIR=%LOCALAPPDATA%\JRawr"

if not exist "%APP_DIR%\jrawr.jar" (
    echo jrawr.jar was not found next to this launcher. 1>&2
    exit /b 1
)
if not exist "%DATA_DIR%\ItemCache.xml" (
    echo Game data was not found in "%DATA_DIR%". 1>&2
    exit /b 1
)

set "JAVA_BIN="
if defined JRAWR_JAVA set "JAVA_BIN=%JRAWR_JAVA%"
if not defined JAVA_BIN if exist "%APP_DIR%\runtime\bin\java.exe" set "JAVA_BIN=%APP_DIR%\runtime\bin\java.exe"
if not defined JAVA_BIN if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_BIN set "JAVA_BIN=java"

if not "%JAVA_BIN%"=="java" if not exist "%JAVA_BIN%" (
    echo Java executable not found: "%JAVA_BIN%" 1>&2
    echo Set JRAWR_JAVA to a Java 25 executable, or install Java 25. 1>&2
    exit /b 1
)

if not exist "%USER_DATA_DIR%" mkdir "%USER_DATA_DIR%"

"%JAVA_BIN%" %JAVA_OPTS% -jar "%APP_DIR%\jrawr.jar" --server.address="%JRAWR_ADDRESS%" --server.port="%JRAWR_PORT%" --jrawr.data-dir="%DATA_DIR%" --jrawr.characters-dir="%USER_DATA_DIR%" --spring.thymeleaf.cache=true --spring.web.resources.cache.period=365d
exit /b %ERRORLEVEL%

@echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: JAVA_HOME is not set and no java command could be found in your PATH.
    exit /b 1
)

"%JAVA_EXE%" -Dfile.encoding=UTF-8 "-Xmx64m" "-Xms64m" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

endlocal

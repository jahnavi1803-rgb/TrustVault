@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------

@echo off
@setlocal

set ERROR_CODE=0

if exist "C:\Users\tejac\jdk-21\jdk-21.0.5+11\bin\java.exe" (
  set "JAVA_HOME=C:\Users\tejac\jdk-21\jdk-21.0.5+11"
) else (
  if not exist "%JAVA_HOME%\bin\java.exe" (
    echo JAVA_HOME is not set correctly: %JAVA_HOME%
    exit /b 1
  )
)

@set "MAVEN_PROJECTBASEDIR=%~dp0"
@if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"

@set "WRAPPER_DIR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper"
@set "WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar"
@set "WRAPPER_PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties"

if not exist "%WRAPPER_JAR%" (
  powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar', '%WRAPPER_JAR%')}"
)

"%JAVA_HOME%\bin\java.exe" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & exit /b %ERROR_CODE%

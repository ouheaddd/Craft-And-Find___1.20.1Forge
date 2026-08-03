@echo off
setlocal
set "APP_HOME=%~dp0"
set "WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar"

if exist "%WRAPPER_JAR%" (
    java -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
    exit /b %ERRORLEVEL%
)

set "GRADLE_VERSION=8.1.1"
if defined GRADLE_USER_HOME (
    set "CACHE_ROOT=%GRADLE_USER_HOME%\craftandfind-bootstrap"
) else (
    set "CACHE_ROOT=%USERPROFILE%\.gradle\craftandfind-bootstrap"
)
set "DIST_DIR=%CACHE_ROOT%\gradle-%GRADLE_VERSION%"
set "DIST_ZIP=%CACHE_ROOT%\gradle-%GRADLE_VERSION%-bin.zip"
set "DIST_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%DIST_DIR%\bin\gradle.bat" (
    echo Gradle wrapper JAR is absent; downloading Gradle %GRADLE_VERSION%...
    if not exist "%CACHE_ROOT%" mkdir "%CACHE_ROOT%"

    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$ErrorActionPreference='Stop'; Invoke-WebRequest -UseBasicParsing -Uri '%DIST_URL%' -OutFile '%DIST_ZIP%'; if (Test-Path '%DIST_DIR%') { Remove-Item -Recurse -Force '%DIST_DIR%' }; Expand-Archive -Path '%DIST_ZIP%' -DestinationPath '%CACHE_ROOT%' -Force; Remove-Item -Force '%DIST_ZIP%'"
    if errorlevel 1 (
        echo Failed to download or unpack Gradle.
        exit /b 1
    )
)

call "%DIST_DIR%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%

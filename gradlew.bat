@echo off
setlocal
set GRADLE_VERSION=8.11.1
if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set BASE_DIR=%GRADLE_USER_HOME%\channelonly-bootstrap
set GRADLE_HOME=%BASE_DIR%\gradle-%GRADLE_VERSION%
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%BASE_DIR%" mkdir "%BASE_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $base='%BASE_DIR%'; $zip=Join-Path $base 'gradle-%GRADLE_VERSION%-bin.zip'; if (!(Test-Path $zip)) { Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile $zip }; Expand-Archive -LiteralPath $zip -DestinationPath $base -Force"
  if errorlevel 1 exit /b %errorlevel%
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
endlocal

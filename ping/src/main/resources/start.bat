@echo off
setlocal enabledelayedexpansion
set PONG_JAR_PATH=pong-0.0.1.jar
set PING_JAR_PATH=ping-0.0.1.jar
set LOG_PATH=D:\test\logs\ping

start "Pong Service" cmd /k "java -jar %PONG_JAR_PATH% --server.port=8081"

timeout /t 3 /nobreak >nul

set /a port=8091
for /L %%i in (1,1,3) do (
    start "Ping Service !port!" cmd /k "java -Xms256m -Xmx256m -jar %PING_JAR_PATH% --server.port=!port!"
    set /a port+=1
)

rem timeout /t 3 /nobreak >nul
rem set /a port=8091
rem for /L %%i in (1,1,3) do (
rem     start "Monitoring Ping Service !port! info Log" powershell -NoProfile -Command "Get-Content -Path '%LOG_PATH%-!port!\info.log' -Wait"
rem     set /a port+=1
rem )
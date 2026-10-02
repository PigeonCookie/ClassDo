@echo off
chcp 65001 >nul
setlocal

rem 用 adb 把 Debug 版 APK 装到已连接的手机 / 模拟器上
rem 需要 ANDROID_HOME（或 ANDROID_SDK_ROOT）指向 Android SDK

if not defined ANDROID_HOME set "ANDROID_HOME=D:\AndroidDev\sdk"
if not defined ANDROID_SDK_ROOT set "ANDROID_SDK_ROOT=%ANDROID_HOME%"

set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
set "APK=%~dp0app\build\outputs\apk\debug\app-debug.apk"

if not exist "%ADB%" (
    echo 找不到 adb：%ADB%
    echo 请设好 ANDROID_HOME，或改这个脚本里的路径。
    pause
    exit /b 1
)
if not exist "%APK%" (
    echo 还没编译出 APK，请先运行 build-apk.bat
    pause
    exit /b 1
)

echo 正在检查设备...
"%ADB%" devices

echo.
echo 正在安装 %APK% ...
"%ADB%" install -r "%APK%"

echo.
pause

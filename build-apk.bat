@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem  一键编译 Debug + Release
rem
rem  依赖：JDK 17、Android SDK（platform 34 + build-tools 34）
rem  下面几行是本机的默认路径，换机器改成你自己的，
rem  或者提前设好 JAVA_HOME / ANDROID_HOME 环境变量。
rem  用 Android Studio 打开项目的话不需要这个脚本。
rem ============================================================

if not defined JAVA_HOME set "JAVA_HOME=D:\java17"
if not defined ANDROID_HOME set "ANDROID_HOME=D:\AndroidDev\sdk"
if not defined ANDROID_SDK_ROOT set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=D:\AndroidDev\gradle-home"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"

echo.
echo ==========================================================
echo   [1/2] 编译 Debug 版（可以直接装到手机上）
echo ==========================================================
call gradlew.bat assembleDebug --console=plain
if errorlevel 1 goto failed

echo.
echo ==========================================================
echo   [2/2] 编译 Release 版
echo   （没配 keystore.properties 的话会打成未签名包）
echo ==========================================================
call gradlew.bat assembleRelease --console=plain
if errorlevel 1 goto failed

echo.
echo ==========================================================
echo   编译完成！APK 在这里：
echo     Debug   : app\build\outputs\apk\debug\app-debug.apk
echo     Release : app\build\outputs\apk\release\app-release.apk
echo ==========================================================
pause
exit /b 0

:failed
echo.
echo **********************************************************
echo   编译失败，把上面红色的错误信息发出来看看。
echo **********************************************************
pause
exit /b 1

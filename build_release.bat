@echo off
echo Building Pixelism Camera Release APK...
echo.

cd /d "%~dp0"

echo Cleaning project...
call gradlew clean
if %errorlevel% neq 0 (
    echo ERROR: Clean failed
    pause
    exit /b 1
)

echo.
echo Building release APK...
call gradlew assembleRelease
if %errorlevel% neq 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)

echo.
echo Build completed successfully!
echo.
echo APK location: app\build\outputs\apk\release\app-release.apk
echo.

if exist "app\build\outputs\apk\release\app-release.apk" (
    echo Opening APK folder...
    explorer "app\build\outputs\apk\release"
) else (
    echo APK not found in expected location
)

pause

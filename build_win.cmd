if not "%1"=="" set "AARCH=%1"
if "%AARCH%"=="" set "AARCH=x86-64"
if "%AARCH%"=="x86_64" set "AARCH=x86-64"
if "%AARCH%"=="amd64" set "AARCH=x86-64"
if "%AARCH%"=="arm64" set "AARCH=aarch64"
set "TARGET_DIR=src\main\resources\windows-%AARCH%"

if not exist "src\main\native\parakeet\CMakeLists.txt" (
    git submodule update --init --recursive
)

cmake -Bbuild -DCMAKE_INSTALL_PREFIX="%TARGET_DIR%"
cmake --build build --config Release -j %NUMBER_OF_PROCESSORS%
cmake --install build

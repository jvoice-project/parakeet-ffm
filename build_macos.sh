#!/bin/bash
set -xe

AARCH=${1:-$(uname -m)}
TARGET_VERSION=15

case "$AARCH" in
  x86_64|amd64|x86-64)
    AARCH=x86_64
    FFM_ARCH=x86-64
    ;;
  arm64|aarch64)
    AARCH=arm64
    FFM_ARCH=aarch64
    ;;
  *)
    echo "Unsupported arch $AARCH"
    exit 1
    ;;
esac

TARGET_DIR="src/main/resources/native/macos-$FFM_ARCH"

if [ ! -f "src/main/native/parakeet/CMakeLists.txt" ]; then
    echo "Initializing submodules..."
    git submodule update --init --recursive
fi

cmake -Bbuild -DCMAKE_INSTALL_PREFIX="$TARGET_DIR" -DCMAKE_OSX_DEPLOYMENT_TARGET=$TARGET_VERSION -DCMAKE_OSX_ARCHITECTURES=$AARCH
cmake --build build --config Release
cmake --install build

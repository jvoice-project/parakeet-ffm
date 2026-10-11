#!/bin/bash
set -xe

# Use TARGETARCH if provided (e.g. from Docker)
# Otherwise, try to detect it using uname -m
AARCH=${1:-${TARGETARCH:-$(uname -m)}}

case $AARCH in
    x86_64|amd64|x86-64)
        FFM_ARCH="x86-64" ;;
    aarch64|arm64)
        FFM_ARCH="aarch64" ;;
    *)
        echo "Unsupported architecture: ${AARCH}"
        exit 1 ;;
esac

TARGET_DIR="src/main/resources/linux-$FFM_ARCH"

if [ ! -f "src/main/native/parakeet/CMakeLists.txt" ]; then
    echo "Initializing submodules..."
    git submodule update --init --recursive
fi

cmake -Bbuild -DCMAKE_INSTALL_PREFIX="$TARGET_DIR"
cmake --build build --config Release -j$(nproc)
cmake --install build

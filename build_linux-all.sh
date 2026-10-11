#!/bin/bash
set -e

# Ensure we are in the project root
cd "$(dirname "$0")"

echo "Building native libraries for all supported platforms using Docker..."

# Initialize submodules if not already done
if [ ! -f "src/main/native/parakeet/CMakeLists.txt" ]; then
    echo "Initializing submodules..."
    git submodule update --init --recursive
fi

# Build for Linux amd64 and arm64
# The --output flag extracts the built libraries from the 'export' stage
# and places them into src/main/resources/tmp
docker buildx build \
    --platform linux/amd64,linux/arm64 \
    --target export \
    --output "type=local,dest=src/main/resources/tmp" \
    .

# Ensure resource directories exist
mkdir -p src/main/resources/native/linux-x86-64 src/main/resources/native/linux-aarch64

# Binaries
if [ -f "src/main/resources/tmp/linux_amd64/libparakeet.so" ]; then
    mv src/main/resources/tmp/linux_amd64/libparakeet.so src/main/resources/native/linux-x86-64/
fi
if [ -f "src/main/resources/tmp/linux_arm64/libparakeet.so" ]; then
    mv src/main/resources/tmp/linux_arm64/libparakeet.so src/main/resources/native/linux-aarch64/
fi

rm -rf src/main/resources/tmp

echo "Build complete. Binaries are located in src/main/resources/native/"
ls -R src/main/resources/native/linux-*

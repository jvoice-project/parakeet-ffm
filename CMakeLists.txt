cmake_minimum_required(VERSION 3.18)
project(parakeet4-ffm C CXX)
include(ExternalProject)

set(PARAKEET_SUBMODULE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/src/main/native/parakeet")

if(NOT EXISTS "${PARAKEET_SUBMODULE_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "parakeet.cpp submodule not initialized. Run: git submodule update --init --recursive")
endif()

# Apply patches to parakeet.cpp submodule
set(PARAKEET_SRC_DIR "${CMAKE_CURRENT_BINARY_DIR}/parakeet_patched/parakeet")

# Copy parakeet.cpp source to build directory to apply patches without modifying source tree
file(REMOVE_RECURSE "${PARAKEET_SRC_DIR}")
file(COPY "${CMAKE_CURRENT_SOURCE_DIR}/src/main/native/parakeet" DESTINATION "${CMAKE_CURRENT_BINARY_DIR}/parakeet_patched" PATTERN ".git" EXCLUDE)

# Apply patch
execute_process(
    COMMAND patch -p1 -i "${CMAKE_CURRENT_SOURCE_DIR}/src/main/native/parakeet.patch"
    WORKING_DIRECTORY "${PARAKEET_SRC_DIR}"
    RESULT_VARIABLE patch_result
)
if(NOT patch_result EQUAL 0)
    message(FATAL_ERROR "Failed to apply patch to parakeet source")
endif()

option(PARAKEET_GGML_CUDA   "Forward GGML_CUDA" OFF)
option(PARAKEET_GGML_VULKAN "Forward GGML_VULKAN" OFF)
option(PARAKEET_GGML_HIP    "Forward GGML_HIP (ROCm)" OFF)
option(GGML_NATIVE          "Optimize build for host processor (-march=native GCC/Clang flag)" OFF)

set(PARAKEET_CMAKE_ARGS
    -DCMAKE_BUILD_TYPE=Release
    -DPARAKEET_VERSION=0.6.1
    -DPARAKEET_SHARED=ON
    -DBUILD_SHARED_LIBS=OFF
    -DCMAKE_POSITION_INDEPENDENT_CODE=ON
    -DPARAKEET_BUILD_CLI=OFF
    -DPARAKEET_BUILD_SERVER=OFF
    -DPARAKEET_BUILD_TESTS=OFF
    -DGGML_NATIVE=${GGML_NATIVE}
    -DGGML_OPENMP=OFF
    -DCMAKE_OSX_DEPLOYMENT_TARGET=${CMAKE_OSX_DEPLOYMENT_TARGET}
    -DCMAKE_OSX_ARCHITECTURES=${CMAKE_OSX_ARCHITECTURES})

if(APPLE AND CMAKE_OSX_ARCHITECTURES STREQUAL "arm64")
    list(APPEND PARAKEET_CMAKE_ARGS -DPARAKEET_GGML_METAL=ON -DGGML_METAL_EMBED_LIBRARY=ON)
endif()

if(PARAKEET_GGML_CUDA)
    list(APPEND PARAKEET_CMAKE_ARGS -DPARAKEET_GGML_CUDA=ON)
endif()

if(PARAKEET_GGML_VULKAN)
    list(APPEND PARAKEET_CMAKE_ARGS -DPARAKEET_GGML_VULKAN=ON)
endif()

if(PARAKEET_GGML_HIP)
    list(APPEND PARAKEET_CMAKE_ARGS -DPARAKEET_GGML_HIP=ON)
endif()

if(MSVC)
    list(APPEND PARAKEET_CMAKE_ARGS -DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded)
elseif(NOT APPLE)
    list(APPEND PARAKEET_CMAKE_ARGS "-DCMAKE_SHARED_LINKER_FLAGS=-static-libgcc -static-libstdc++")
endif()

ExternalProject_Add(parakeet_cpp
    SOURCE_DIR     "${PARAKEET_SRC_DIR}"
    CMAKE_ARGS     ${PARAKEET_CMAKE_ARGS}
    BUILD_ALWAYS   1
    INSTALL_COMMAND ""
)

ExternalProject_Get_Property(parakeet_cpp BINARY_DIR)

if(WIN32)
    install(FILES
        "${BINARY_DIR}/parakeet.dll"
        "${BINARY_DIR}/$<CONFIG>/parakeet.dll"
        DESTINATION ${CMAKE_INSTALL_PREFIX}
        OPTIONAL)
else()
    install(FILES
        "${BINARY_DIR}/${CMAKE_SHARED_LIBRARY_PREFIX}parakeet${CMAKE_SHARED_LIBRARY_SUFFIX}"
        DESTINATION ${CMAKE_INSTALL_PREFIX})
endif()

# Building `parakeet-ffm` Native Libraries

`parakeet-ffm` connects Java 21+ via the Foreign Function & Memory (FFM) API to [parakeet.cpp](https://github.com/mudler/parakeet.cpp), an inference engine for NVIDIA Parakeet and Nemotron ASR/VAD models based on [ggml](https://github.com/ggml-org/ggml).

This guide explains:

1. Prerequisites for building each backend (CPU, CUDA, Vulkan, Metal).
1. How to compile the native library for your platform.
1. How to configure `parakeet-ffm` to load and use your custom-built library.
1. Runtime device selection and troubleshooting.

---

## Supported Backends by Platform

| Platform    | Architecture            | Supported Backends                         |
|:------------|:------------------------|:-------------------------------------------|
| **Linux**   | `x86_64`                | **CPU**, **CUDA**, **Vulkan**              |
| **Linux**   | `arm64` (`aarch64`)     | **CPU**, **Vulkan**                        |
| **Windows** | `x86_64`                | **CPU**, **CUDA**, **Vulkan**              |
| **macOS**   | `arm64` (Apple Silicon) | **Metal** (Hardware acceleration), **CPU** |
| **macOS**   | `x86_64` (Intel)        | **CPU**                                    |

---

## Prerequisites

### General Requirements

* **Git** (with submodules checked out)
* **CMake >= 3.18**
* **C++17 Compiler**: GCC 10+, Clang 12+, or MSVC 2019+
* **Java >= 21** (JDK 21 or newer with `--enable-preview`)

Make sure git submodules are initialized before building:

```bash
git submodule update --init --recursive
```

### Backend-Specific Dependencies

#### CPU (Universal Fallback)

* No additional dependencies beyond standard C++ build tools (`build-essential` on Debian/Ubuntu, Xcode Command Line Tools on macOS, MSVC on Windows).
* To build with host-specific optimizations (such as AVX2 / AVX-512 / ARM Neon with `-march=native`), pass `-DGGML_NATIVE=ON`.

#### CUDA (NVIDIA GPUs on Linux & Windows)

* **NVIDIA CUDA Toolkit** (11.8+ or 12.x+, including `nvcc` and cuBLAS).
* Compatible **NVIDIA GPU Display Driver**.
* Ensure `nvcc` is available on your `PATH`.

#### Vulkan (AMD, Intel & NVIDIA GPUs on Linux & Windows)

* **Linux**: Install Vulkan headers, loader, and shader compiler:

  ```bash
  # Debian / Ubuntu:
  apt-get install -y libvulkan-dev glslc vulkan-tools spirv-headers spirv-tools
  
  # Fedora / RHEL:
  dnf install -y vulkan-headers vulkan-loader-devel glslc spirv-headers-devel
  
  # Arch Linux:
  pacman -S vulkan-headers vulkan-icd-loader shaderc spirv-headers
  ```

* **Windows**: Install the [LunarG Vulkan SDK](https://vulkan.lunarg.com/sdk/home).
  Ensure the `VULKAN_SDK` environment variable is set.

#### Metal (Apple Silicon on macOS)

* macOS 15+ (Sequoia) with **Xcode** or **Xcode Command Line Tools** (`xcode-select --install`).
* Metal frameworks and shader compilers are built into macOS.

---

## Building the Native Library

You can build the native library using the provided build scripts or directly via CMake.

### Linux

The helper script `./build_linux.sh` automatically detects the architecture, initializes submodules, and installs the output into `src/main/resources/native/linux-<arch>/`:

```bash
# CPU build (portable):
./build_linux.sh

# CPU build optimized for current host CPU:
./build_linux.sh x86_64 -DGGML_NATIVE=ON

# Vulkan build:
./build_linux.sh x86_64 -DPARAKEET_GGML_VULKAN=ON

# CUDA build:
./build_linux.sh x86_64 -DPARAKEET_GGML_CUDA=ON
```

---

### Windows

The helper script `build_win.cmd` automatically detects initializes submodules and installs the output into `src/main/resources/native/windows-<arch>/`.
Open a **Developer Command Prompt for Visual Studio** (or Developer PowerShell):

```cmd
:: CPU build (portable)
build_win.cmd x86-64

:: Vulkan build
build_win.cmd x86-64 -DPARAKEET_GGML_VULKAN=ON

:: CUDA build
build_win.cmd x86-64 -DPARAKEET_GGML_CUDA=ON
```

---

### macOS

> [!NOTE]
> parakeet-ffm already bundles Metal support by default, so you do not need to build it separately.

No backend flags are required for macOS.
The build system automatically enables Metal acceleration when building for Apple Silicon (`arm64`), and targets the CPU backend for Intel (`x86_64`):

```bash
# macOS Apple Silicon (arm64): Metal acceleration is enabled automatically
./build_macos.sh arm64

# macOS Intel (x86_64): Builds with CPU backend automatically
./build_macos.sh x86_64
```

The resulting library is installed into:

* Apple Silicon: `src/main/resources/native/macos-aarch64/libparakeet.dylib`
* Intel: `src/main/resources/native/macos-x86-64/libparakeet.dylib`

---

## Making `parakeet-ffm` Use Your Custom Build

Use the `-Dparakeet.library.path` System Property to point `parakeet-ffm` to your custom build.
Pass `-Dparakeet.library.path` to the JVM when launching your application:

### Pointing Directly to the Library File

```bash
# Linux
java --enable-preview --enable-native-access=ALL-UNNAMED \
     -Dparakeet.library.path="/path/to/build/libparakeet.so" \
     -jar your-app.jar

# Windows
java --enable-preview --enable-native-access=ALL-UNNAMED ^
     -Dparakeet.library.path="C:\path\to\build\parakeet.dll" ^
     -jar your-app.jar

# macOS
java --enable-preview --enable-native-access=ALL-UNNAMED \
     -Dparakeet.library.path="/path/to/build/libparakeet.dylib" \
     -jar your-app.jar
```

### Pointing to a Directory

If you point to a directory, `NativeLibraryLoader` looks for `libparakeet.so` (Linux), `parakeet.dll` (Windows), or `libparakeet.dylib` (macOS) in that directory:

```bash
java --enable-preview --enable-native-access=ALL-UNNAMED \
     -Dparakeet.library.path="/path/to/build" \
     -jar your-app.jar
```

### Using with Maven

Add the argument to your Surefire / Failsafe configuration or pass it via command-line:

```bash
./mvnw test -Dparakeet.library.path="/path/to/build"
```

### Using with Gradle

```groovy
tasks.withType(JavaExec).configureEach {
    jvmArgs([
        '--enable-preview',
        '--enable-native-access=ALL-UNNAMED',
        '-Dparakeet.library.path=/path/to/build'
    ])
}
```

---

## Runtime Device Selection & Fallback

When `parakeet.cpp` initializes, it probes the device registry and automatically picks the fastest available accelerator:

1. If a GPU (CUDA, Vulkan, or Metal) is available, it initializes and runs inference on that device.
1. If initialization fails or no GPU is present, it falls back to the CPU backend automatically.

### Overriding the Device

You can force a specific device at runtime using the `PARAKEET_DEVICE` environment variable:

* **Force CPU (even if a GPU backend is compiled in):**

  ```bash
  export PARAKEET_DEVICE=cpu
  ```

* **Select a specific CUDA device:**

  ```bash
  export PARAKEET_DEVICE=CUDA0
  ```

* **Select a specific Vulkan device:**

  ```bash
  export PARAKEET_DEVICE=Vulkan0
  ```

* **Select Metal:**

  ```bash
  export PARAKEET_DEVICE=Metal
  ```

---

## Troubleshooting

### `parakeet.cpp submodule not initialized`

Run the following from the repository root:
```bash
git submodule update --init --recursive
```

### `UnsatisfiedLinkError: libcuda.so.1: cannot open shared object file`

This error occurs when running a CUDA-compiled binary on a machine where NVIDIA drivers are missing or not in the dynamic linker path:

* Ensure NVIDIA GPU drivers are installed.
* On Linux, ensure CUDA library paths are exported:
  ```bash
  export LD_LIBRARY_PATH="/usr/local/cuda/lib64:$LD_LIBRARY_PATH"
  ```
* If you do not have an NVIDIA GPU, use the CPU or Vulkan build instead.

### `UnsatisfiedLinkError: libvulkan.so.1: cannot open shared object file` (Linux)

On minimal Linux installations or Docker containers, the Vulkan loader package is not installed by default. Install it with:

```bash
sudo apt-get install -y libvulkan1
```

### Java Preview Feature Warning / Error

Java 21 requires both `--enable-preview` and `--enable-native-access`:

```bash
java --enable-preview --enable-native-access=ALL-UNNAMED -jar app.jar
```

If using named modules, replace `ALL-UNNAMED` with your module name or `io.github.jvoiceproject.parakeet`.

# parakeet-ffm

A modern Java wrapper for [parakeet.cpp](https://github.com/mudler/parakeet.cpp), powered by the Java Foreign Function & Memory (FFM) API (`java.lang.foreign`).
parakeet.cpp is a fast, portable C++ implementation of NVIDIA Parakeet and Nemotron speech-to-text (ASR) and Voice Activity Detection (VAD) models powered by [ggml](https://github.com/ggml-org/ggml).

## Features

- **High-Performance ASR**: Offline and streaming speech recognition using TDT, RNN-T, and CTC decoder architectures.
- **Cache-Aware Streaming**: Incremental streaming with End-of-Utterance (`<EOU>`) and End-of-Backchannel (`<EOB>`) event detection.
- **Word & Token Timestamps**: Detailed word boundaries, token IDs, and confidence scores.
- **Voice Activity Detection**: Standalone VAD support using Silero VAD GGUF models or ASR models with integrated VAD heads.
- **Pure Java FFM API**: Direct native downcalls via Java's Foreign Function & Memory API and off-heap memory management with zero JNI glue code and zero third-party bridge dependencies.

## Platform Support

Java >= 21 is required.
Because FFM in Java 21 is a preview feature, `--enable-preview` and native access flag `--enable-native-access=ALL-UNNAMED` (or your module name) are required at compile and run time on Java 21.

The library aims to support the following platforms:

* Windows x86_64
* Linux x86_64 / arm64 (GLIBC >= 2.31)
* macOS x86_64 / arm64 (macOS 15 Sequioa and newer, with Metal acceleration)

The bundled JAR includes the self-contained `libparakeet` shared library.
`parakeet-ffm` automatically extracts and links the library for the current platform.

## Usage

The package is distributed through [Maven Central](https://central.sonatype.com/artifact/io.github.jvoice-project/parakeet-ffm):

### Maven

```xml
<dependency>
    <groupId>io.github.jvoice-project</groupId>
    <artifactId>parakeet-ffm</artifactId>
    <!-- replace $version with a specific version -->
    <version>$version</version>
</dependency>
```

When compiling and running your project on Java 21, enable preview features:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <release>21</release>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

Run your application with:

```shell
java --enable-preview --enable-native-access=ALL-UNNAMED -jar your-app.jar
```

### Gradle

```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.jvoice-project:parakeet-ffm:+' // gets latest version
}

tasks.withType(JavaCompile).configureEach {
    options.compilerArgs.add('--enable-preview')
}

tasks.withType(JavaExec).configureEach {
    jvmArgs(['--enable-preview', '--enable-native-access=ALL-UNNAMED'])
}
```

All releases are signed with the PGP key `047A5F7D27B9F2408F31EB6D577886B2F4A44CB7`,
which you can find on [keys.openpgp.org](https://keys.openpgp.org/search?q=047A5F7D27B9F2408F31EB6D577886B2F4A44CB7).

To import the public key:

```shell
gpg --keyserver keys.openpgp.org --recv-keys 047A5F7D27B9F2408F31EB6D577886B2F4A44CB7
```

## Examples

GGUF models can be downloaded from [mudler/parakeet-cpp-gguf](https://huggingface.co/mudler/parakeet-cpp-gguf).

### Simple Offline Transcription

```java
import io.github.jvoiceproject.parakeet.Parakeet;
import io.github.jvoiceproject.parakeet.ParakeetModel;
import java.nio.file.Path;

try (ParakeetModel model = Parakeet.loadModel(Path.of("tdt_ctc-110m-q8_0.gguf"))) {
    String transcript = model.transcribe(Path.of("audio.wav"));
    System.out.println("Transcript: " + transcript);
}
```

### Transcription with Word and Token Timestamps

```java
import io.github.jvoiceproject.parakeet.Parakeet;
import io.github.jvoiceproject.parakeet.ParakeetModel;
import io.github.jvoiceproject.parakeet.transcribe.TranscribeOptions;
import io.github.jvoiceproject.parakeet.transcribe.Transcription;
import io.github.jvoiceproject.parakeet.transcribe.Word;
import java.nio.file.Path;

try (ParakeetModel model = Parakeet.loadModel(Path.of("tdt_ctc-110m-q8_0.gguf"))) {
    Transcription result = model.transcribeWithTimestamps(Path.of("audio.wav"), TranscribeOptions.DEFAULT);
    System.out.println("Full text: " + result.text());

    for (Word word : result.words()) {
        System.out.printf("[%0.2fs - %0.2fs] %s (confidence: %0.2f)%n",
            word.start(), word.end(), word.text(), word.confidence());
    }
}
```

### Real-Time Streaming Transcription

For cache-aware streaming models (such as `realtime_eou_120m-v1-q8_0.gguf` or `nemotron-3.5-asr-streaming-0.6b`):

```java
import io.github.jvoiceproject.parakeet.Parakeet;
import io.github.jvoiceproject.parakeet.ParakeetModel;
import io.github.jvoiceproject.parakeet.transcribe.TranscribeStreamResult;
import io.github.jvoiceproject.parakeet.transcribe.TranscribeStream;

import java.nio.file.Path;

try (ParakeetModel model = Parakeet.loadModel(Path.of("realtime_eou_120m-v1-q8_0.gguf"));
     TranscribeStream stream = model.startTranscribeStream()) {
    // Feed 16 kHz mono float PCM chunks as they arrive (e.g. from a microphone)
    float[] pcmChunk = ...;
    TranscribeStreamResult result = stream.feed(pcmChunk);
    if (!result.text().isEmpty()) {
        System.out.print(result.text());
    }
    if (result.eou()) {
        System.out.println(" [End of Utterance]");
    }

    // Finalize at end of audio
    TranscribeStreamResult finalResult = stream.finish();
    System.out.println(finalResult.text());
}
```

### Voice Activity Detection (VAD)

Using a Silero VAD model (such as `silero-vad-f16.gguf`):

```java
import io.github.jvoiceproject.parakeet.Parakeet;
import io.github.jvoiceproject.parakeet.ParakeetModel;
import io.github.jvoiceproject.parakeet.vad.VadOptions;
import io.github.jvoiceproject.parakeet.vad.VadResult;
import io.github.jvoiceproject.parakeet.vad.VadSegment;
import java.nio.file.Path;

try (ParakeetModel vad = Parakeet.loadModel(Path.of("silero-vad-f16.gguf"))) {
    VadOptions options = VadOptions.builder()
            .threshold(0.5)
            .minPause(0.2)
            .build();

    VadResult result = vad.detectSpeech(Path.of("audio.wav"), options);
    for (VadSegment segment : result.segments()) {
        System.out.printf("Speech detected: %0.2fs - %0.2fs%n", segment.start(), segment.end());
    }
}
```

## Development

You need Java >= 21, CMake >= 3.18, and a C++17 compiler.

### Initializing Submodules

Ensure all git submodules are checked out recursively:

```shell
git submodule update --init --recursive
```

### Building the Native Library Locally

Compile `libparakeet` and install it into `src/main/resources/native/`:

- **Linux:** Run `./build_linux.sh`
- **macOS:** Run `./build_macos.sh`
- **Windows:** Run `.\build_win.cmd`

### Building with Docker (Multi-Architecture)

To build portable Linux libraries (`amd64`, `arm64`) with GLIBC 2.31 compatibility using Docker Buildx:

```shell
./build_linux-all.sh
```

### Using a Custom Native Library

By default, `parakeet-ffm` checks `parakeet.library.path` (or `jna.library.path`) before falling back to the bundled resources.
To test or benchmark against a custom build (e.g., compiled with `-march=native` or CUDA):

```shell
java --enable-preview --enable-native-access=io.github.jvoiceproject.parakeet -Dparakeet.library.path="/path/to/custom/parakeet/build" -jar ...
```

### Java Build & Tests

Run the test suite and package the JAR:

```shell
./mvnw clean verify
```

Maven will automatically download the test models and test audio during `generate-test-resources` and execute the unit and integration tests.

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).
Third-party notices and licenses are listed in the [NOTICE](NOTICE) file.

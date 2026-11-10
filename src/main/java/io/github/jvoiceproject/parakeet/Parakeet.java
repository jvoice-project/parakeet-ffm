/*
 * #%L
 * parakeet-ffm
 * %%
 * Copyright (C) 2026 Contributors to parakeet-ffm
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.github.jvoiceproject.parakeet;

import io.github.jvoiceproject.parakeet.internal.Json;
import io.github.jvoiceproject.parakeet.internal.NativeLibraryLoader;
import io.github.jvoiceproject.parakeet.internal.ParakeetBinding;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.google.gson.reflect.TypeToken;

/**
 * Main entry point for the parakeet.cpp Speech-to-Text and Voice Activity Detection library.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class Parakeet {

    private static volatile ParakeetBinding instance;
    private static volatile boolean libraryLoaded;

    private Parakeet() {}

    /**
     * Loads the native parakeet library.
     *
     * @throws IOException if the native library cannot be located or extracted.
     * @throws UnsupportedOperationException if the operating system or architecture is not
     *     supported.
     * @throws LinkageError if the native library cannot be dynamically linked or ABI version
     *     mismatch occurs.
     */
    public static void loadLibrary() throws IOException {
        if (!libraryLoaded) {
            synchronized (Parakeet.class) {
                if (!libraryLoaded) {
                    instance = NativeLibraryLoader.load();
                    libraryLoaded = true;
                }
            }
        }
    }

    /**
     * Returns whether the native library is currently loaded.
     *
     * @return {@code true} if the native library is loaded, {@code false} otherwise.
     */
    public static boolean isLibraryLoaded() {
        return libraryLoaded;
    }

    /**
     * Returns the native parakeet.cpp library version string.
     *
     * @return Version string (e.g. "0.6.1").
     * @throws IllegalStateException if the native library is not loaded.
     */
    public static String version() {
        return getBinding().parakeet_version();
    }

    /** Expected C-API ABI version of the native parakeet library. */
    public static final int EXPECTED_ABI_VERSION = ParakeetBinding.EXPECTED_ABI_VERSION;

    /**
     * Returns the C-API ABI version number.
     *
     * @return ABI version integer.
     * @throws IllegalStateException if the native library is not loaded.
     */
    public static int abiVersion() {
        return getBinding().parakeet_capi_abi_version();
    }

    /**
     * Loads a GGUF model file into a new {@link ParakeetModel} context.
     *
     * @param gguf Path to .gguf model file.
     * @return Loaded {@link ParakeetModel}.
     * @throws ParakeetException if loading fails or file does not exist.
     * @throws IllegalStateException if the native library is not loaded.
     */
    public static ParakeetModel loadModel(Path gguf) {
        ParakeetBinding lib = getBinding();
        validateModelPath(gguf);
        MemorySegment ctx = lib.parakeet_capi_load(gguf.toAbsolutePath().toString());
        if (ctx == null || ctx.equals(MemorySegment.NULL) || ctx.address() == 0) {
            throw new ParakeetException(
                    "Failed to load model from " + gguf + ": " + lib.parakeet_capi_load_error());
        }
        return new ParakeetModel(lib, ctx);
    }

    /**
     * Loads a specific component from a bundle GGUF file.
     *
     * @param bundle Path to bundle .gguf file.
     * @param component Component name (e.g., <code>asr</code>, <code>vad</code>).
     * @return Loaded {@link ParakeetModel}.
     * @throws ParakeetException if loading fails or file does not exist.
     * @throws IllegalStateException if the native library is not loaded.
     */
    public static ParakeetModel loadModel(Path bundle, String component) {
        ParakeetBinding lib = getBinding();
        validateModelPath(bundle);
        MemorySegment ctx =
                lib.parakeet_capi_load_component(bundle.toAbsolutePath().toString(), component);
        if (ctx == null || ctx.equals(MemorySegment.NULL) || ctx.address() == 0) {
            throw new ParakeetException(
                    "Failed to load component '"
                            + component
                            + "' from bundle "
                            + bundle
                            + ": "
                            + lib.parakeet_capi_load_error());
        }
        return new ParakeetModel(lib, ctx);
    }

    /**
     * Inspects the components of a bundle GGUF file.
     *
     * @param bundle Path to bundle .gguf file.
     * @return List of {@link BundleComponent} metadata descriptors.
     * @throws ParakeetException if inspection fails.
     * @throws IllegalStateException if the native library is not loaded.
     */
    public static List<BundleComponent> bundleComponents(Path bundle) {
        ParakeetBinding lib = getBinding();
        validateModelPath(bundle);
        String json = lib.parakeet_capi_bundle_components_json(bundle.toAbsolutePath().toString());
        if (json == null) {
            throw new ParakeetException(
                    "Failed to inspect bundle components for "
                            + bundle
                            + ": "
                            + lib.parakeet_capi_load_error());
        }
        List<BundleComponent> list =
                Json.GSON.fromJson(json, new TypeToken<List<BundleComponent>>() {}.getType());
        return list != null ? list : List.of();
    }

    /**
     * Sets or resets the {@link ParakeetBinding} instance.
     *
     * @param binding the binding instance to set, or {@code null} to reset
     * @throws LinkageError if the native library cannot be dynamically linked or ABI version mismatch occurs
     */
    static void setInstance(ParakeetBinding binding) {
        if (binding != null) {
            NativeLibraryLoader.validateAbiVersion(binding, "custom binding");
            synchronized (Parakeet.class) {
                instance = binding;
                libraryLoaded = true;
            }
        } else {
            synchronized (Parakeet.class) {
                instance = null;
                libraryLoaded = false;
            }
        }
    }

    private static void assertLibraryLoaded() throws IllegalStateException {
        if (!libraryLoaded) {
            throw new IllegalStateException(
                    "Native library is not loaded. Call Parakeet.loadLibrary() first.");
        }
    }

    private static ParakeetBinding getBinding() {
        assertLibraryLoaded();
        return instance;
    }

    private static void validateModelPath(Path path) {
        Objects.requireNonNull(path, "model path cannot be null");
        if (!Files.exists(path) || Files.isDirectory(path)) {
            throw new ParakeetException(
                    "Model file does not exist: " + path,
                    new FileNotFoundException("Model file does not exist: " + path));
        }
    }
}

/*
 * #%L
 * parakeet4j
 * %%
 * Copyright (C) 2026 Contributors to parakeet4j
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
package io.github.jvoiceproject.parakeet.internal;

import io.github.jvoiceproject.parakeet.ParakeetException;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Java Foreign Function &amp; Memory (FFM) implementation of {@link ParakeetBinding}.
 *
 * @see <a
 *     href="https://github.com/mudler/parakeet.cpp/blob/v0.6.1/include/parakeet_capi.h">parakeet_capi.h</a>
 * @author Florian Hotze - Initial contribution
 */
public final class FfmParakeetBinding implements ParakeetBinding {

    private final MethodHandle mh_abi_version;
    private final MethodHandle mh_version;
    private final MethodHandle mh_load;
    private final MethodHandle mh_load_component;
    private final MethodHandle mh_bundle_components_json;
    private final MethodHandle mh_load_error;
    private final MethodHandle mh_free;
    private final MethodHandle mh_model_kind;
    private final MethodHandle mh_last_error;
    private final MethodHandle mh_free_string;
    private final MethodHandle mh_set_concurrency;

    private final MethodHandle mh_transcribe_path_lang;
    private final MethodHandle mh_transcribe_pcm_lang;
    private final MethodHandle mh_transcribe_path_json;
    private final MethodHandle mh_transcribe_pcm_batch_lang;
    private final MethodHandle mh_transcribe_pcm_batch_json_lang;

    private final MethodHandle mh_stream_begin_lang;
    private final MethodHandle mh_stream_feed_json;
    private final MethodHandle mh_stream_finalize_json;
    private final MethodHandle mh_stream_free;

    private final MethodHandle mh_vad_pcm_json;
    private final MethodHandle mh_vad_path_json;
    private final MethodHandle mh_transcribe_path_json_vad_with;
    private final MethodHandle mh_vad_stream_begin;
    private final MethodHandle mh_vad_stream_feed_json;
    private final MethodHandle mh_vad_stream_reset;
    private final MethodHandle mh_vad_stream_free;

    public FfmParakeetBinding(SymbolLookup lookup) {
        Linker linker = Linker.nativeLinker();

        mh_abi_version =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_abi_version",
                        FunctionDescriptor.of(ValueLayout.JAVA_INT));
        mh_version =
                find(
                        lookup,
                        linker,
                        "parakeet_version",
                        FunctionDescriptor.of(ValueLayout.ADDRESS));
        mh_load =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_load",
                        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mh_load_component =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_load_component",
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mh_bundle_components_json =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_bundle_components_json",
                        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mh_load_error =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_load_error",
                        FunctionDescriptor.of(ValueLayout.ADDRESS));
        mh_free =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_free",
                        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        mh_model_kind =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_model_kind",
                        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        mh_last_error =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_last_error",
                        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mh_free_string =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_free_string",
                        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        mh_set_concurrency =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_set_concurrency",
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT));

        mh_transcribe_path_lang =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_transcribe_path_lang",
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS));
        mh_transcribe_pcm_lang =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_transcribe_pcm_lang",
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS));
        mh_transcribe_path_json =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_transcribe_path_json",
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT));
        mh_transcribe_pcm_batch_lang =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_transcribe_pcm_batch_lang",
                        FunctionDescriptor.of(
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS));
        mh_transcribe_pcm_batch_json_lang =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_transcribe_pcm_batch_json_lang",
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS));

        mh_stream_begin_lang =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_stream_begin_lang",
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mh_stream_feed_json =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_stream_feed_json",
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT));
        mh_stream_finalize_json =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_stream_finalize_json",
                        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        mh_stream_free =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_stream_free",
                        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

        mh_vad_pcm_json =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_vad_pcm_json",
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS));
        mh_vad_path_json =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_vad_path_json",
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS));
        mh_transcribe_path_json_vad_with =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_transcribe_path_json_vad_with",
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS));
        mh_vad_stream_begin =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_vad_stream_begin",
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.ADDRESS));
        mh_vad_stream_feed_json =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_vad_stream_feed_json",
                        FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT));
        mh_vad_stream_reset =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_vad_stream_reset",
                        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        mh_vad_stream_free =
                find(
                        lookup,
                        linker,
                        "parakeet_capi_vad_stream_free",
                        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    }

    /**
     * Resolves a native function symbol and downcalls it into a MethodHandle.
     *
     * <p>This method centralizes FFM boilerplate and provides fail-fast error handling. If a
     * requested symbol is missing from the underlying shared library (e.g., due to a version
     * mismatch between the Java wrapper and the C binary), this throws a descriptive {@link
     * ParakeetException} immediately during class instantiation, preventing cryptic native crashes
     * or NullPointerExceptions later in execution.
     *
     * @param lookup The symbol lookup context of the native library.
     * @param linker The native linker for ABI mapping.
     * @param name The exact C function name as-exported in the header.
     * @param descriptor The FFM function descriptor matching the C signature.
     * @return A {@link MethodHandle} configured to invoke the native function.
     */
    private static MethodHandle find(
            SymbolLookup lookup, Linker linker, String name, FunctionDescriptor descriptor) {
        MemorySegment symbol =
                lookup.find(name)
                        .orElseThrow(
                                () ->
                                        new ParakeetException(
                                                "Required native symbol '"
                                                        + name
                                                        + "' not found in library"));
        return linker.downcallHandle(symbol, descriptor);
    }

    /**
     * Safely decodes a natively-allocated C string into a Java String, and guarantees the native
     * memory is subsequently freed.
     *
     * <p>The Parakeet C API explicitly transfers ownership of dynamically allocated ({@code
     * malloc}'d) JSON strings and transcripts to the caller. The caller <i>must</i> release them
     * via {@code parakeet_capi_free_string}. This method ensures safe unbounded UTF-8 parsing (via
     * {@code reinterpret(Long.MAX_VALUE)}) and leverages a {@code finally} block to guarantee the
     * native pointer is freed even if the string decoding throws an exception, strictly preventing
     * native memory leaks.
     *
     * @param ptr The MemorySegment pointing to a dynamically allocated NUL-terminated C string.
     * @return The decoded Java String, or {@code null} if the native pointer is null/empty.
     */
    private String readAndFreeString(MemorySegment ptr) {
        if (ptr == null || ptr.equals(MemorySegment.NULL) || ptr.address() == 0) {
            return null;
        }
        try {
            return ptr.reinterpret(Long.MAX_VALUE).getUtf8String(0);
        } finally {
            try {
                mh_free_string.invokeExact(ptr);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Safely decodes a static or context-owned C string into a Java String without freeing it.
     *
     * <p>Functions like {@code parakeet_capi_last_error} and {@code parakeet_capi_load_error}
     * return pointers to read-only memory or memory owned internally by the native {@code
     * parakeet_ctx} struct. Attempting to free these pointers will result in a C heap corruption or
     * a JVM crash (segfault). This method provides a safe way to map these unowned, NUL-terminated
     * strings into Java space.
     *
     * @param ptr The MemorySegment pointing to an unowned, static, or context-managed
     *     NUL-terminated C string.
     * @return The decoded Java String, or {@code null} if the native pointer is null/empty.
     */
    private static String readStaticString(MemorySegment ptr) {
        if (ptr == null || ptr.equals(MemorySegment.NULL) || ptr.address() == 0) {
            return null;
        }
        return ptr.reinterpret(Long.MAX_VALUE).getUtf8String(0);
    }

    @Override
    public int parakeet_capi_abi_version() {
        try {
            return (int) mh_abi_version.invokeExact();
        } catch (Throwable t) {
            throw new ParakeetException("Failed to call parakeet_capi_abi_version", t);
        }
    }

    @Override
    public String parakeet_version() {
        try {
            MemorySegment ptr = (MemorySegment) mh_version.invokeExact();
            return readStaticString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to call parakeet_version", t);
        }
    }

    @Override
    public MemorySegment parakeet_capi_load(String ggufPath) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pathSeg =
                    ggufPath != null ? arena.allocateUtf8String(ggufPath) : MemorySegment.NULL;
            return (MemorySegment) mh_load.invokeExact(pathSeg);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to load model from " + ggufPath, t);
        }
    }

    @Override
    public MemorySegment parakeet_capi_load_component(String ggufPath, String component) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pathSeg =
                    ggufPath != null ? arena.allocateUtf8String(ggufPath) : MemorySegment.NULL;
            MemorySegment compSeg =
                    component != null ? arena.allocateUtf8String(component) : MemorySegment.NULL;
            return (MemorySegment) mh_load_component.invokeExact(pathSeg, compSeg);
        } catch (Throwable t) {
            throw new ParakeetException(
                    "Failed to load component " + component + " from " + ggufPath, t);
        }
    }

    @Override
    public String parakeet_capi_bundle_components_json(String ggufPath) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pathSeg =
                    ggufPath != null ? arena.allocateUtf8String(ggufPath) : MemorySegment.NULL;
            MemorySegment ptr = (MemorySegment) mh_bundle_components_json.invokeExact(pathSeg);
            return readAndFreeString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to inspect bundle components for " + ggufPath, t);
        }
    }

    @Override
    public void parakeet_capi_free(MemorySegment ctx) {
        if (ctx == null || ctx.equals(MemorySegment.NULL) || ctx.address() == 0) {
            return;
        }
        try {
            mh_free.invokeExact(ctx);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to free model context", t);
        }
    }

    @Override
    public int parakeet_capi_model_kind(MemorySegment ctx) {
        try {
            return (int) mh_model_kind.invokeExact(ctx);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to query model kind", t);
        }
    }

    @Override
    public String parakeet_capi_last_error(MemorySegment ctx) {
        try {
            MemorySegment ptr = (MemorySegment) mh_last_error.invokeExact(ctx);
            return readStaticString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to get context last error", t);
        }
    }

    @Override
    public String parakeet_capi_load_error() {
        try {
            MemorySegment ptr = (MemorySegment) mh_load_error.invokeExact();
            return readStaticString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to get load error", t);
        }
    }

    @Override
    public int parakeet_capi_set_concurrency(MemorySegment ctx, int backends, int threadsEach) {
        try {
            return (int) mh_set_concurrency.invokeExact(ctx, backends, threadsEach);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to set concurrency", t);
        }
    }

    @Override
    public String parakeet_capi_transcribe_path_lang(
            MemorySegment ctx, String wavPath, int decoder, String targetLang) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pathSeg =
                    wavPath != null ? arena.allocateUtf8String(wavPath) : MemorySegment.NULL;
            MemorySegment langSeg =
                    targetLang != null ? arena.allocateUtf8String(targetLang) : MemorySegment.NULL;
            MemorySegment ptr =
                    (MemorySegment)
                            mh_transcribe_path_lang.invokeExact(ctx, pathSeg, decoder, langSeg);
            return readAndFreeString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to transcribe audio from " + wavPath, t);
        }
    }

    @Override
    public String parakeet_capi_transcribe_pcm_lang(
            MemorySegment ctx,
            float[] pcm,
            int nSamples,
            int sampleRate,
            int decoder,
            String targetLang) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pcmSeg =
                    pcm != null
                            ? arena.allocateArray(ValueLayout.JAVA_FLOAT, pcm)
                            : MemorySegment.NULL;
            MemorySegment langSeg =
                    targetLang != null ? arena.allocateUtf8String(targetLang) : MemorySegment.NULL;
            MemorySegment ptr =
                    (MemorySegment)
                            mh_transcribe_pcm_lang.invokeExact(
                                    ctx, pcmSeg, nSamples, sampleRate, decoder, langSeg);
            return readAndFreeString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to transcribe PCM audio", t);
        }
    }

    @Override
    public String parakeet_capi_transcribe_path_json(
            MemorySegment ctx, String wavPath, int decoder) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pathSeg =
                    wavPath != null ? arena.allocateUtf8String(wavPath) : MemorySegment.NULL;
            MemorySegment ptr =
                    (MemorySegment) mh_transcribe_path_json.invokeExact(ctx, pathSeg, decoder);
            return readAndFreeString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException(
                    "Failed to transcribe audio with timestamps from " + wavPath, t);
        }
    }

    @Override
    public int parakeet_capi_transcribe_pcm_batch_lang(
            MemorySegment ctx,
            float[] pcmConcat,
            int[] sampleCounts,
            int batchSize,
            int sampleRate,
            int decoder,
            String targetLang,
            String[] outTexts) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pcmSeg =
                    pcmConcat != null
                            ? arena.allocateArray(ValueLayout.JAVA_FLOAT, pcmConcat)
                            : MemorySegment.NULL;
            MemorySegment countsSeg =
                    sampleCounts != null
                            ? arena.allocateArray(ValueLayout.JAVA_INT, sampleCounts)
                            : MemorySegment.NULL;
            MemorySegment langSeg =
                    targetLang != null ? arena.allocateUtf8String(targetLang) : MemorySegment.NULL;

            MemorySegment clipPointers = arena.allocateArray(ValueLayout.ADDRESS, batchSize);
            if (pcmConcat != null && sampleCounts != null) {
                long offset = 0;
                for (int i = 0; i < batchSize; i++) {
                    clipPointers.setAtIndex(
                            ValueLayout.ADDRESS,
                            i,
                            pcmSeg.asSlice(offset, (long) sampleCounts[i] * Float.BYTES));
                    offset += (long) sampleCounts[i] * Float.BYTES;
                }
            }

            MemorySegment outPointers = arena.allocateArray(ValueLayout.ADDRESS, batchSize);

            int rc =
                    (int)
                            mh_transcribe_pcm_batch_lang.invokeExact(
                                    ctx,
                                    clipPointers,
                                    countsSeg,
                                    batchSize,
                                    sampleRate,
                                    decoder,
                                    langSeg,
                                    outPointers);
            if (rc == 0 && outTexts != null) {
                for (int i = 0; i < batchSize; i++) {
                    MemorySegment ptr = outPointers.getAtIndex(ValueLayout.ADDRESS, i);
                    outTexts[i] = readAndFreeString(ptr);
                }
            }
            return rc;
        } catch (Throwable t) {
            throw new ParakeetException("Failed to transcribe PCM batch", t);
        }
    }

    @Override
    public String parakeet_capi_transcribe_pcm_batch_json_lang(
            MemorySegment ctx,
            float[] pcmConcat,
            int[] sampleCounts,
            int batchSize,
            int sampleRate,
            int decoder,
            String targetLang) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pcmSeg =
                    pcmConcat != null
                            ? arena.allocateArray(ValueLayout.JAVA_FLOAT, pcmConcat)
                            : MemorySegment.NULL;
            MemorySegment countsSeg =
                    sampleCounts != null
                            ? arena.allocateArray(ValueLayout.JAVA_INT, sampleCounts)
                            : MemorySegment.NULL;
            MemorySegment langSeg =
                    targetLang != null ? arena.allocateUtf8String(targetLang) : MemorySegment.NULL;
            MemorySegment ptr =
                    (MemorySegment)
                            mh_transcribe_pcm_batch_json_lang.invokeExact(
                                    ctx,
                                    pcmSeg,
                                    countsSeg,
                                    batchSize,
                                    sampleRate,
                                    decoder,
                                    langSeg);
            return readAndFreeString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to transcribe PCM batch json", t);
        }
    }

    @Override
    public MemorySegment parakeet_capi_stream_begin_lang(MemorySegment ctx, String targetLang) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment langSeg =
                    targetLang != null ? arena.allocateUtf8String(targetLang) : MemorySegment.NULL;
            return (MemorySegment) mh_stream_begin_lang.invokeExact(ctx, langSeg);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to begin streaming session", t);
        }
    }

    @Override
    public String parakeet_capi_stream_feed_json(MemorySegment stream, float[] pcm, int nSamples) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pcmSeg =
                    pcm != null
                            ? arena.allocateArray(ValueLayout.JAVA_FLOAT, pcm)
                            : MemorySegment.NULL;
            MemorySegment ptr =
                    (MemorySegment) mh_stream_feed_json.invokeExact(stream, pcmSeg, nSamples);
            return readAndFreeString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to feed PCM to streaming session", t);
        }
    }

    @Override
    public String parakeet_capi_stream_finalize_json(MemorySegment stream) {
        try {
            MemorySegment ptr = (MemorySegment) mh_stream_finalize_json.invokeExact(stream);
            return readAndFreeString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to finalize streaming session", t);
        }
    }

    @Override
    public void parakeet_capi_stream_free(MemorySegment stream) {
        if (stream == null || stream.equals(MemorySegment.NULL) || stream.address() == 0) {
            return;
        }
        try {
            mh_stream_free.invokeExact(stream);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to free stream handle", t);
        }
    }

    @Override
    public String parakeet_capi_vad_pcm_json(
            MemorySegment ctx, float[] pcm, int nSamples, int sampleRate, String optionsJson) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pcmSeg =
                    pcm != null
                            ? arena.allocateArray(ValueLayout.JAVA_FLOAT, pcm)
                            : MemorySegment.NULL;
            MemorySegment optsSeg =
                    optionsJson != null
                            ? arena.allocateUtf8String(optionsJson)
                            : MemorySegment.NULL;
            MemorySegment ptr =
                    (MemorySegment)
                            mh_vad_pcm_json.invokeExact(ctx, pcmSeg, nSamples, sampleRate, optsSeg);
            return readAndFreeString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to run VAD on PCM audio", t);
        }
    }

    @Override
    public String parakeet_capi_vad_path_json(
            MemorySegment ctx, String wavPath, String optionsJson) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pathSeg =
                    wavPath != null ? arena.allocateUtf8String(wavPath) : MemorySegment.NULL;
            MemorySegment optsSeg =
                    optionsJson != null
                            ? arena.allocateUtf8String(optionsJson)
                            : MemorySegment.NULL;
            MemorySegment ptr = (MemorySegment) mh_vad_path_json.invokeExact(ctx, pathSeg, optsSeg);
            return readAndFreeString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to run VAD on audio file " + wavPath, t);
        }
    }

    @Override
    public String parakeet_capi_transcribe_path_json_vad_with(
            MemorySegment asrCtx,
            MemorySegment vadCtx,
            String wavPath,
            int decoder,
            String vadOptsJson) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pathSeg =
                    wavPath != null ? arena.allocateUtf8String(wavPath) : MemorySegment.NULL;
            MemorySegment optsSeg =
                    vadOptsJson != null
                            ? arena.allocateUtf8String(vadOptsJson)
                            : MemorySegment.NULL;
            MemorySegment ptr =
                    (MemorySegment)
                            mh_transcribe_path_json_vad_with.invokeExact(
                                    asrCtx, vadCtx, pathSeg, decoder, optsSeg);
            return readAndFreeString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to transcribe audio with VAD segmentation", t);
        }
    }

    @Override
    public MemorySegment parakeet_capi_vad_stream_begin(
            MemorySegment vadCtx, int sampleRate, String optionsJson) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment optsSeg =
                    optionsJson != null
                            ? arena.allocateUtf8String(optionsJson)
                            : MemorySegment.NULL;
            return (MemorySegment) mh_vad_stream_begin.invokeExact(vadCtx, sampleRate, optsSeg);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to begin VAD stream", t);
        }
    }

    @Override
    public String parakeet_capi_vad_stream_feed_json(
            MemorySegment vadStream, float[] pcm, int nSamples, int isLast) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pcmSeg =
                    pcm != null
                            ? arena.allocateArray(ValueLayout.JAVA_FLOAT, pcm)
                            : MemorySegment.NULL;
            MemorySegment ptr =
                    (MemorySegment)
                            mh_vad_stream_feed_json.invokeExact(
                                    vadStream, pcmSeg, nSamples, isLast);
            return readAndFreeString(ptr);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to feed PCM to VAD stream", t);
        }
    }

    @Override
    public int parakeet_capi_vad_stream_reset(MemorySegment vadStream) {
        try {
            return (int) mh_vad_stream_reset.invokeExact(vadStream);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to reset VAD stream", t);
        }
    }

    @Override
    public void parakeet_capi_vad_stream_free(MemorySegment vadStream) {
        if (vadStream == null || vadStream.equals(MemorySegment.NULL) || vadStream.address() == 0) {
            return;
        }
        try {
            mh_vad_stream_free.invokeExact(vadStream);
        } catch (Throwable t) {
            throw new ParakeetException("Failed to free VAD stream", t);
        }
    }
}

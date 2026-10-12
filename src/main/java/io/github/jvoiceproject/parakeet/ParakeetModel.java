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

import io.github.jvoiceproject.parakeet.internal.AudioConverter;
import io.github.jvoiceproject.parakeet.internal.Json;
import io.github.jvoiceproject.parakeet.internal.ParakeetBinding;
import io.github.jvoiceproject.parakeet.transcribe.TranscribeOptions;
import io.github.jvoiceproject.parakeet.transcribe.TranscribeStream;
import io.github.jvoiceproject.parakeet.transcribe.Transcription;
import io.github.jvoiceproject.parakeet.vad.VadOptions;
import io.github.jvoiceproject.parakeet.vad.VadResult;
import io.github.jvoiceproject.parakeet.vad.VadStream;

import java.io.FileNotFoundException;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.reflect.TypeToken;

/**
 * An opened Parakeet model context. Manages native resources and active streams.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class ParakeetModel implements AutoCloseable {

    private final ParakeetBinding lib;
    private final MemorySegment ctx;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private final Set<TranscribeStream> openTranscribeStreams =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<VadStream> openVadStreams =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    ParakeetModel(ParakeetBinding lib, MemorySegment ctx) {
        this.lib = Objects.requireNonNull(lib, "lib cannot be null");
        this.ctx = Objects.requireNonNull(ctx, "ctx cannot be null");
    }

    MemorySegment ctx() {
        return ctx;
    }

    /**
     * Returns the kind of model loaded in this context.
     *
     * @return {@link ModelKind} of this model.
     * @throws IllegalStateException if this model is closed.
     */
    public ModelKind kind() {
        rwLock.readLock().lock();
        try {
            checkOpen();
            return ModelKind.fromValue(lib.parakeet_capi_model_kind(ctx));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Sets concurrency pool size for parallel CPU transcription.
     *
     * <p>By default, a context runs one request at a time: calls from several threads are safe but
     * queue behing one computed backend. This method can be used to turn on a pool of CPU backends
     * for the context's ASR model, each with <code>threadsEach</code> many compute threads. Choose
     * <code>backends * threadsEach</code> at most the number of physical cores: the compute thread
     * teams spin, and oversubscribing slows everything.
     *
     * @param backends Number of parallel compute backends.
     * @param threadsEach Compute threads per backend (&lt;= 0) for default budget).
     * @return Effective number of backends configured.
     * @throws IllegalStateException if this model is closed.
     */
    public int setConcurrency(int backends, int threadsEach) {
        rwLock.readLock().lock();
        try {
            checkOpen();
            return lib.parakeet_capi_set_concurrency(ctx, backends, threadsEach);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Transcribes a WAV audio file with default options.
     *
     * @param wav Path to WAV audio file.
     * @return Transcript text.
     * @throws ParakeetException if transcription fails.
     */
    public String transcribe(Path wav) {
        return transcribe(wav, TranscribeOptions.DEFAULT);
    }

    /**
     * Transcribes a WAV audio file with specified options.
     *
     * @param wav Path to WAV audio file.
     * @param options {@link TranscribeOptions}.
     * @return Transcript text.
     * @throws ParakeetException if transcription fails.
     */
    public String transcribe(Path wav, TranscribeOptions options) {
        validateWavFile(wav);
        TranscribeOptions opts = options != null ? options : TranscribeOptions.DEFAULT;

        rwLock.readLock().lock();
        try {
            checkOpen();
            String text =
                    lib.parakeet_capi_transcribe_path_lang(
                            ctx,
                            wav.toAbsolutePath().toString(),
                            opts.getDecoder().getValue(),
                            opts.getLanguage());
            if (text == null) {
                throw new ParakeetException(
                        "Failed to transcribe " + wav + ": " + lib.parakeet_capi_last_error(ctx));
            }
            return text;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Transcribes in-memory mono float PCM audio with default options.
     *
     * @param pcm Float audio samples in range [-1.0, 1.0].
     * @param sampleRate Sample rate in Hz (e.g., 16000 = 16 kHz).
     * @return Transcript text.
     */
    public String transcribe(float[] pcm, int sampleRate) {
        return transcribe(pcm, sampleRate, TranscribeOptions.DEFAULT);
    }

    /**
     * Transcribes in-memory mono float PCM audio with specified options.
     *
     * @param pcm Float audio samples in range [-1.0, 1.0].
     * @param sampleRate Sample rate in Hz (e.g., 16000 = 16 kHz).
     * @param options {@link TranscribeOptions}.
     * @return Transcript text.
     */
    public String transcribe(float[] pcm, int sampleRate, TranscribeOptions options) {
        Objects.requireNonNull(pcm, "pcm samples cannot be null");
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be positive: " + sampleRate);
        }
        TranscribeOptions opts = options != null ? options : TranscribeOptions.DEFAULT;

        rwLock.readLock().lock();
        try {
            checkOpen();
            String text =
                    lib.parakeet_capi_transcribe_pcm_lang(
                            ctx,
                            pcm,
                            pcm.length,
                            sampleRate,
                            opts.getDecoder().getValue(),
                            opts.getLanguage());
            if (text == null) {
                throw new ParakeetException(
                        "Failed to transcribe PCM audio: " + lib.parakeet_capi_last_error(ctx));
            }
            return text;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Transcribes in-memory mono signed 16-bit PCM audio with default options.
     *
     * @param pcm Signed 16-bit audio samples in range [-32768, 32767].
     * @param sampleRate Sample rate in Hz (e.g., 16000 = 16 kHz).
     * @return Transcript text.
     */
    public String transcribe(short[] pcm, int sampleRate) {
        return transcribe(pcm, sampleRate, TranscribeOptions.DEFAULT);
    }

    /**
     * Transcribes in-memory mono signed 16-bit PCM audio with specified options.
     *
     * @param pcm Signed 16-bit audio samples in range [-32768, 32767].
     * @param sampleRate Sample rate in Hz (e.g., 16000 = 16 kHz).
     * @param options {@link TranscribeOptions}.
     * @return Transcript text.
     */
    public String transcribe(short[] pcm, int sampleRate, TranscribeOptions options) {
        Objects.requireNonNull(pcm, "pcm samples cannot be null");
        return transcribe(AudioConverter.pcm16ToFloat(pcm), sampleRate, options);
    }

    /**
     * Transcribes a WAV audio file returning word/token timestamps and confidence scores.
     *
     * @param wav Path to WAV audio file.
     * @param options {@link TranscribeOptions}.
     * @return {@link Transcription} object with word and token details.
     */
    public Transcription transcribeWithTimestamps(Path wav, TranscribeOptions options) {
        validateWavFile(wav);
        TranscribeOptions opts = options != null ? options : TranscribeOptions.DEFAULT;

        rwLock.readLock().lock();
        try {
            checkOpen();
            String json =
                    lib.parakeet_capi_transcribe_path_json(
                            ctx, wav.toAbsolutePath().toString(), opts.getDecoder().getValue());
            if (json == null) {
                throw new ParakeetException(
                        "Failed to transcribe " + wav + ": " + lib.parakeet_capi_last_error(ctx));
            }
            return Json.GSON.fromJson(json, Transcription.class);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Transcribes a batch of in-memory PCM audio clips.
     *
     * @param pcmClips List of float sample arrays.
     * @param sampleRate Audio sample rate in Hz.
     * @param options {@link TranscribeOptions}.
     * @return List of transcript strings in order.
     */
    public List<String> batchTranscribe(
            List<float[]> pcmClips, int sampleRate, TranscribeOptions options) {
        Objects.requireNonNull(pcmClips, "pcmClips cannot be null");
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be positive: " + sampleRate);
        }
        TranscribeOptions opts = options != null ? options : TranscribeOptions.DEFAULT;

        List<float[]> clips = List.copyOf(pcmClips);
        if (clips.isEmpty()) {
            return List.of();
        }

        int totalSamples = 0;
        int[] sampleCounts = new int[clips.size()];
        for (int i = 0; i < clips.size(); i++) {
            float[] clip = clips.get(i);
            Objects.requireNonNull(clip, "pcm clip " + i + " cannot be null");
            sampleCounts[i] = clip.length;
            totalSamples += clip.length;
        }

        float[] pcmConcat = new float[totalSamples];
        int offset = 0;
        for (float[] clip : clips) {
            System.arraycopy(clip, 0, pcmConcat, offset, clip.length);
            offset += clip.length;
        }

        String[] outTexts = new String[clips.size()];
        List<String> result = new ArrayList<>(clips.size());

        rwLock.readLock().lock();
        try {
            checkOpen();
            int rc =
                    lib.parakeet_capi_transcribe_pcm_batch_lang(
                            ctx,
                            pcmConcat,
                            sampleCounts,
                            clips.size(),
                            sampleRate,
                            opts.getDecoder().getValue(),
                            opts.getLanguage(),
                            outTexts);
            if (rc != 0) {
                throw new ParakeetException(
                        "Batch transcription failed: " + lib.parakeet_capi_last_error(ctx));
            }

            for (String t : outTexts) {
                result.add(t != null ? t : "");
            }
            return result;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Transcribes a batch of in-memory PCM audio clips returning timestamps.
     *
     * @param pcmClips List of float sample arrays.
     * @param sampleRate Audio sample rate in Hz.
     * @param options {@link TranscribeOptions}.
     * @return List of {@link Transcription} objects in order.
     */
    public List<Transcription> batchTranscribeWithTimestamps(
            List<float[]> pcmClips, int sampleRate, TranscribeOptions options) {
        Objects.requireNonNull(pcmClips, "pcmClips cannot be null");
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be positive: " + sampleRate);
        }
        TranscribeOptions opts = options != null ? options : TranscribeOptions.DEFAULT;

        List<float[]> clips = List.copyOf(pcmClips);
        if (clips.isEmpty()) {
            return List.of();
        }

        int totalSamples = 0;
        int[] sampleCounts = new int[clips.size()];
        for (int i = 0; i < clips.size(); i++) {
            float[] clip = clips.get(i);
            Objects.requireNonNull(clip, "pcm clip " + i + " cannot be null");
            sampleCounts[i] = clip.length;
            totalSamples += clip.length;
        }

        float[] pcmConcat = new float[totalSamples];
        int offset = 0;
        for (float[] clip : clips) {
            System.arraycopy(clip, 0, pcmConcat, offset, clip.length);
            offset += clip.length;
        }

        rwLock.readLock().lock();
        try {
            checkOpen();
            String json =
                    lib.parakeet_capi_transcribe_pcm_batch_json_lang(
                            ctx,
                            pcmConcat,
                            sampleCounts,
                            clips.size(),
                            sampleRate,
                            opts.getDecoder().getValue(),
                            opts.getLanguage());
            if (json == null) {
                throw new ParakeetException(
                        "Batch transcription failed: " + lib.parakeet_capi_last_error(ctx));
            }
            List<Transcription> list =
                    Json.GSON.fromJson(json, new TypeToken<List<Transcription>>() {}.getType());
            return list != null ? list : List.of();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Starts an incremental, cache-aware streaming transcription session.
     *
     * @return Active {@link TranscribeStream}.
     */
    public TranscribeStream startTranscribeStream() {
        return startTranscribeStream(null);
    }

    /**
     * Starts an incremental streaming session targeting a specific language prompt.
     *
     * @param targetLang Language locale (e.g. "en", "de", "auto") or null for default.
     * @return Active {@link TranscribeStream}.
     */
    public TranscribeStream startTranscribeStream(String targetLang) {
        rwLock.readLock().lock();
        try {
            checkOpen();
            MemorySegment streamHandle = lib.parakeet_capi_stream_begin_lang(ctx, targetLang);
            if (streamHandle == null
                    || streamHandle.equals(MemorySegment.NULL)
                    || streamHandle.address() == 0) {
                throw new ParakeetException(
                        "Failed to start streaming session: " + lib.parakeet_capi_last_error(ctx));
            }

            TranscribeStream stream =
                    new TranscribeStream(lib, streamHandle, openTranscribeStreams::remove);
            openTranscribeStreams.add(stream);
            return stream;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Performs Voice Activity Detection on a WAV audio file returning speech regions.
     *
     * @param wav Path to WAV audio file.
     * @param options {@link VadOptions} configuration.
     * @return {@link VadResult} containing speech regions.
     */
    public VadResult detectSpeech(Path wav, VadOptions options) {
        validateWavFile(wav);
        VadOptions opts =
                (options != null ? options : VadOptions.DEFAULT)
                        .toBuilder().mode(VadOptions.Mode.SPEECH).build();

        rwLock.readLock().lock();
        try {
            checkOpen();
            String json =
                    lib.parakeet_capi_vad_path_json(
                            ctx, wav.toAbsolutePath().toString(), opts.toJson());
            if (json == null) {
                throw new ParakeetException(
                        "VAD speech detection failed for "
                                + wav
                                + ": "
                                + lib.parakeet_capi_last_error(ctx));
            }
            return Json.GSON.fromJson(json, VadResult.class);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Performs Voice Activity Detection on in-memory PCM audio returning speech regions.
     *
     * @param pcm Float audio samples in range [-1.0, 1.0].
     * @param sampleRate Audio sample rate in Hz.
     * @param options {@link VadOptions} configuration.
     * @return {@link VadResult} containing speech regions.
     */
    public VadResult detectSpeech(float[] pcm, int sampleRate, VadOptions options) {
        Objects.requireNonNull(pcm, "pcm samples cannot be null");
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be positive: " + sampleRate);
        }
        VadOptions opts =
                (options != null ? options : VadOptions.DEFAULT)
                        .toBuilder().mode(VadOptions.Mode.SPEECH).build();

        rwLock.readLock().lock();
        try {
            checkOpen();
            String json =
                    lib.parakeet_capi_vad_pcm_json(ctx, pcm, pcm.length, sampleRate, opts.toJson());
            if (json == null) {
                throw new ParakeetException(
                        "VAD speech detection failed: " + lib.parakeet_capi_last_error(ctx));
            }
            return Json.GSON.fromJson(json, VadResult.class);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Performs Voice Activity Detection on in-memory signed 16-bit PCM audio returning speech
     * regions.
     *
     * @param pcm Signed 16-bit audio samples in range [-32768, 32767].
     * @param sampleRate Audio sample rate in Hz.
     * @param options {@link VadOptions} configuration.
     * @return {@link VadResult} containing speech regions.
     */
    public VadResult detectSpeech(short[] pcm, int sampleRate, VadOptions options) {
        Objects.requireNonNull(pcm, "pcm samples cannot be null");
        return detectSpeech(AudioConverter.pcm16ToFloat(pcm), sampleRate, options);
    }

    /**
     * Segments audio into balanced speech chunks (capped by maxSegment) for ASR processing.
     *
     * <p>These are the cuts the transcriber internally uses for ASR.
     *
     * @param wav Path to WAV audio file.
     * @param options {@link VadOptions} configuration.
     * @return {@link VadResult} containing segmented regions.
     */
    public VadResult segmentSpeech(Path wav, VadOptions options) {
        validateWavFile(wav);
        VadOptions opts =
                (options != null ? options : VadOptions.DEFAULT)
                        .toBuilder().mode(VadOptions.Mode.SEGMENTS).build();

        rwLock.readLock().lock();
        try {
            checkOpen();

            String json =
                    lib.parakeet_capi_vad_path_json(
                            ctx, wav.toAbsolutePath().toString(), opts.toJson());
            if (json == null) {
                throw new ParakeetException(
                        "VAD speech segmentation failed for "
                                + wav
                                + ": "
                                + lib.parakeet_capi_last_error(ctx));
            }
            return Json.GSON.fromJson(json, VadResult.class);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Segments in-memory PCM audio into balanced speech chunks (capped by maxSegment).
     *
     * @param pcm Float audio samples in range [-1.0, 1.0].
     * @param sampleRate Audio sample rate in Hz.
     * @param options {@link VadOptions} configuration.
     * @return {@link VadResult} containing segmented regions.
     */
    public VadResult segmentSpeech(float[] pcm, int sampleRate, VadOptions options) {
        Objects.requireNonNull(pcm, "pcm samples cannot be null");
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be positive: " + sampleRate);
        }
        VadOptions opts =
                (options != null ? options : VadOptions.DEFAULT)
                        .toBuilder().mode(VadOptions.Mode.SEGMENTS).build();

        rwLock.readLock().lock();
        try {
            checkOpen();
            String json =
                    lib.parakeet_capi_vad_pcm_json(ctx, pcm, pcm.length, sampleRate, opts.toJson());
            if (json == null) {
                throw new ParakeetException(
                        "VAD speech segmentation failed: " + lib.parakeet_capi_last_error(ctx));
            }
            return Json.GSON.fromJson(json, VadResult.class);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Segments in-memory signed 16-bit PCM audio into balanced speech chunks (capped by
     * maxSegment).
     *
     * @param pcm Signed 16-bit audio samples in range [-32768, 32767].
     * @param sampleRate Audio sample rate in Hz.
     * @param options {@link VadOptions} configuration.
     * @return {@link VadResult} containing segmented regions.
     */
    public VadResult segmentSpeech(short[] pcm, int sampleRate, VadOptions options) {
        Objects.requireNonNull(pcm, "pcm samples cannot be null");
        return segmentSpeech(AudioConverter.pcm16ToFloat(pcm), sampleRate, options);
    }

    /**
     * Starts an incremental streaming Voice Activity Detection session.
     *
     * @param sampleRate Sample rate in Hz (Silero supports 16000 or 8000 Hz).
     * @param options {@link VadOptions} configuration.
     * @return Active {@link VadStream}.
     */
    public VadStream startVadStream(int sampleRate, VadOptions options) {
        VadOptions opts = options != null ? options : VadOptions.DEFAULT;

        rwLock.readLock().lock();
        try {
            checkOpen();
            MemorySegment vadStreamHandle =
                    lib.parakeet_capi_vad_stream_begin(ctx, sampleRate, opts.toJson());
            if (vadStreamHandle == null
                    || vadStreamHandle.equals(MemorySegment.NULL)
                    || vadStreamHandle.address() == 0) {
                throw new ParakeetException(
                        "Failed to start VAD stream: " + lib.parakeet_capi_last_error(ctx));
            }

            VadStream stream = new VadStream(lib, vadStreamHandle, openVadStreams::remove);
            openVadStreams.add(stream);
            return stream;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Transcribes audio using this model's integrated VAD head for segmentation with default ASR
     * options.
     *
     * @param wav Path to WAV audio file.
     * @param vadOptions VAD segmentation options.
     * @return {@link Transcription} with segment-guided timestamps.
     */
    public Transcription transcribeWithVad(Path wav, VadOptions vadOptions) {
        return transcribeWithVad(null, wav, TranscribeOptions.DEFAULT, vadOptions);
    }

    /**
     * Transcribes audio using this model's integrated VAD head for segmentation.
     *
     * @param wav Path to WAV audio file.
     * @param transcribeOptions ASR options.
     * @param vadOptions VAD segmentation options.
     * @return {@link Transcription} with segment-guided timestamps.
     */
    public Transcription transcribeWithVad(
            Path wav, TranscribeOptions transcribeOptions, VadOptions vadOptions) {
        return transcribeWithVad(null, wav, transcribeOptions, vadOptions);
    }

    /**
     * Transcribes audio using a separate VAD model (e.g., Silero VAD) for segmentation with default
     * ASR options.
     *
     * @param vadModel A model loaded with a VAD head or Silero VAD architecture.
     * @param wav Path to WAV audio file.
     * @param vadOptions VAD segmentation options.
     * @return {@link Transcription} with segment-guided timestamps.
     */
    public Transcription transcribeWithVad(
            ParakeetModel vadModel, Path wav, VadOptions vadOptions) {
        return transcribeWithVad(vadModel, wav, TranscribeOptions.DEFAULT, vadOptions);
    }

    /**
     * Transcribes audio using a separate VAD model (e.g., Silero VAD) for segmentation.
     *
     * <p>This allows the ASR model's segmenter to take its probabilities from the VAD model, so any
     * ASW model (also those without a VAD head) can cut long audio.
     *
     * @param vadModel A model loaded with a VAD head or Silero VAD architecture.
     * @param wav Path to WAV audio file.
     * @param transcribeOptions ASR options.
     * @param vadOptions VAD segmentation options.
     * @return {@link Transcription} with segment-guided timestamps.
     */
    public Transcription transcribeWithVad(
            ParakeetModel vadModel,
            Path wav,
            TranscribeOptions transcribeOptions,
            VadOptions vadOptions) {
        validateWavFile(wav);

        if (vadModel == null || vadModel == this) {
            rwLock.readLock().lock();
            try {
                checkOpen();
                return doTranscribeWithVad(MemorySegment.NULL, wav, transcribeOptions, vadOptions);
            } finally {
                rwLock.readLock().unlock();
            }
        } else {
            ParakeetModel first =
                    System.identityHashCode(this) <= System.identityHashCode(vadModel)
                            ? this
                            : vadModel;
            ParakeetModel second = (first == this) ? vadModel : this;

            first.rwLock.readLock().lock();
            try {
                second.rwLock.readLock().lock();
                try {
                    checkOpen();
                    vadModel.checkOpen();
                    return doTranscribeWithVad(vadModel.ctx, wav, transcribeOptions, vadOptions);
                } finally {
                    second.rwLock.readLock().unlock();
                }
            } finally {
                first.rwLock.readLock().unlock();
            }
        }
    }

    private Transcription doTranscribeWithVad(
            MemorySegment vadCtx,
            Path wav,
            TranscribeOptions transcribeOptions,
            VadOptions vadOptions) {
        TranscribeOptions tOpts =
                transcribeOptions != null ? transcribeOptions : TranscribeOptions.DEFAULT;
        VadOptions vOpts = vadOptions != null ? vadOptions : VadOptions.DEFAULT;

        String json =
                lib.parakeet_capi_transcribe_path_json_vad_with(
                        ctx,
                        vadCtx,
                        wav.toAbsolutePath().toString(),
                        tOpts.getDecoder().getValue(),
                        vOpts.toJson());
        if (json == null) {
            throw new ParakeetException(
                    "VAD-guided transcription failed: " + lib.parakeet_capi_last_error(ctx));
        }
        return Json.GSON.fromJson(json, Transcription.class);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            rwLock.writeLock().lock();
            try {
                for (TranscribeStream s : openTranscribeStreams) {
                    try {
                        s.close();
                    } catch (Exception ignored) {
                    }
                }
                openTranscribeStreams.clear();

                for (VadStream vs : openVadStreams) {
                    try {
                        vs.close();
                    } catch (Exception ignored) {
                    }
                }
                openVadStreams.clear();

                lib.parakeet_capi_free(ctx);
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    private void checkOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Model has already been closed");
        }
    }

    private static void validateWavFile(Path path) {
        Objects.requireNonNull(path, "audio path cannot be null");
        if (!Files.exists(path) || Files.isDirectory(path)) {
            throw new ParakeetException(
                    "Audio file does not exist: " + path,
                    new FileNotFoundException("Audio file does not exist: " + path));
        }
    }
}

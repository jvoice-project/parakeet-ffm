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
package io.github.jvoiceproject.parakeet.internal;

import java.lang.foreign.MemorySegment;

/**
 * Low-level binding interface for the parakeet.cpp C-API.
 *
 * @see <a
 *     href="https://github.com/mudler/parakeet.cpp/blob/v0.6.1/include/parakeet_capi.h">parakeet_capi.h</a>
 * @author Florian Hotze - Initial contribution
 */
public interface ParakeetBinding {

    /** Expected C-API ABI version supported by this binding. */
    int EXPECTED_ABI_VERSION = 10;

    /**
     * ABI version of this header/implementation. Bumped on any breaking change to the function
     * signatures or semantics.
     *
     * @return The ABI version integer.
     */
    int parakeet_capi_abi_version();

    /**
     * Returns the version string of the loaded parakeet library.
     *
     * @return The parakeet version string.
     */
    String parakeet_version();

    /**
     * Load a GGUF model. A bundle GGUF (several models in one file) loads its ASR component by
     * default.
     *
     * @param ggufPath Path to the GGUF model file.
     * @return An owning context pointer, or NULL on failure. Must be released with {@link
     *     #parakeet_capi_free}.
     */
    MemorySegment parakeet_capi_load(String ggufPath);

    /**
     * Open one named component of a bundle GGUF. The context is of the kind the component declares:
     * "asr" gives an ASR context, "diar" a diarization context, "ced" a sound tagger, "voice" a
     * speaker encoder, "vad" a Silero VAD context.
     *
     * @param ggufPath Path to the bundle GGUF file.
     * @param component Name of the component to load. A NULL or empty string behaves like {@link
     *     #parakeet_capi_load}.
     * @return An owning context pointer, or NULL on failure (check {@link
     *     #parakeet_capi_load_error}).
     */
    MemorySegment parakeet_capi_load_component(String ggufPath, String component);

    /**
     * Inspects the components of a bundle GGUF, read from the header only.
     *
     * @param ggufPath Path to the bundle GGUF file.
     * @return A JSON array string representing the components, or NULL if the file is not a bundle
     *     or is malformed (check {@link #parakeet_capi_load_error}).
     */
    String parakeet_capi_bundle_components_json(String ggufPath);

    /**
     * Free a context obtained from {@link #parakeet_capi_load} or {@link
     * #parakeet_capi_load_component}. Safe to call on a NULL pointer.
     *
     * @param ctx The native context pointer to free.
     */
    void parakeet_capi_free(MemorySegment ctx);

    /**
     * Which kind of model a context holds, allowing callers to dispatch without probing individual
     * entry points.
     *
     * @param ctx The loaded context pointer.
     * @return 0 = None, 1 = ASR, 2 = Diarization, 3 = Sound, 4 = Speaker, 5 = VAD.
     */
    int parakeet_capi_model_kind(MemorySegment ctx);

    /**
     * Human-readable description of the last error on the context, or an empty string if none.
     *
     * @param ctx The native context pointer.
     * @return The error message.
     */
    String parakeet_capi_last_error(MemorySegment ctx);

    /**
     * The reason the last load, load_component, or bundle_components_json call on this thread
     * failed. Valid until the next of those calls on the same thread.
     *
     * @return The load error message.
     */
    String parakeet_capi_load_error();

    /**
     * Turns on a pool of CPU backends for the context's ASR model, enabling concurrent
     * transcriptions. Calls run in parallel up to {@code backends} at a time.
     *
     * @param ctx The native context pointer.
     * @param backends Number of backend instances to pool.
     * @param threadsEach Compute threads per backend (&lt;= 0 computes dynamically based on CLI
     *     budget).
     * @return The effective number of backends (1 if &lt;= 1, or 1 if using a GPU). 0 on failure.
     */
    int parakeet_capi_set_concurrency(MemorySegment ctx, int backends, int threadsEach);

    /**
     * Transcribe a WAV file, selecting the language prompt for multilingual (nemotron) models.
     *
     * @param ctx The native context pointer.
     * @param wavPath Path to the WAV audio file.
     * @param decoder Selects the head: 0 = default, 1 = ctc, 2 = tdt/rnnt.
     * @param targetLang Locale string (e.g., "en", "de", "auto"). NULL or "" uses the model's
     *     default.
     * @return Transcribed text string, or NULL on error (sets context's last error).
     */
    String parakeet_capi_transcribe_path_lang(
            MemorySegment ctx, String wavPath, int decoder, String targetLang);

    /**
     * Transcribe in-memory mono float PCM, selecting the language prompt for multilingual models.
     * If sampleRate is not 16000, the audio is linearly resampled to 16 kHz first.
     *
     * @param ctx The native context pointer.
     * @param pcm In-memory mono float PCM samples.
     * @param nSamples Number of samples.
     * @param sampleRate Sample rate in Hz.
     * @param decoder Selects the head: 0 = default, 1 = ctc, 2 = tdt/rnnt.
     * @param targetLang Locale string (e.g., "en", "de", "auto"). NULL or "" uses the model's
     *     default.
     * @return Transcribed text string, or NULL on error.
     */
    String parakeet_capi_transcribe_pcm_lang(
            MemorySegment ctx,
            float[] pcm,
            int nSamples,
            int sampleRate,
            int decoder,
            String targetLang);

    /**
     * Transcribe a WAV file returning a JSON document with per-word and per-token timestamps +
     * confidence (matching NeMo 'max_prob' method).
     *
     * @param ctx The native context pointer.
     * @param wavPath Path to the WAV audio file.
     * @param decoder Selects the head: 0 = default, 1 = ctc, 2 = tdt/rnnt.
     * @return JSON document string containing words, tokens, and timestamps, or NULL on error.
     */
    String parakeet_capi_transcribe_path_json(MemorySegment ctx, String wavPath, int decoder);

    /**
     * Transcribe a batch of in-memory mono float PCM clips, selecting the language prompt for
     * multilingual models. ONE {@code targetLang} applies to the whole batch.
     *
     * @param ctx The native context pointer.
     * @param pcmConcat Concatenated 16 kHz mono float samples for all clips.
     * @param sampleCounts Array containing the sample count for each clip in the batch.
     * @param batchSize The number of clips (length of sampleCounts array).
     * @param sampleRate Sample rate in Hz.
     * @param decoder Selects the head: 0 = default, 1 = ctc, 2 = tdt/rnnt.
     * @param targetLang Locale string (e.g., "en", "de", "auto"). NULL or "" uses the model's
     *     default.
     * @param outTexts Caller-allocated String array to hold transcripts on success.
     * @return 0 on success (populates outTexts), or nonzero on error (leaves outTexts unchanged,
     *     sets last_error).
     */
    int parakeet_capi_transcribe_pcm_batch_lang(
            MemorySegment ctx,
            float[] pcmConcat,
            int[] sampleCounts,
            int batchSize,
            int sampleRate,
            int decoder,
            String targetLang,
            String[] outTexts);

    /**
     * Batched transcription with timestamps, returning ONE JSON string that is a JSON ARRAY of
     * documents identically shaped to {@link #parakeet_capi_transcribe_path_json}.
     *
     * @param ctx The native context pointer.
     * @param pcmConcat Concatenated 16 kHz mono float samples for all clips.
     * @param sampleCounts Array containing the sample count for each clip in the batch.
     * @param batchSize The number of clips (length of sampleCounts array).
     * @param sampleRate Sample rate in Hz.
     * @param decoder Selects the head: 0 = default, 1 = ctc, 2 = tdt/rnnt.
     * @param targetLang Locale string (e.g., "en", "de", "auto"). NULL or "" uses the model's
     *     default.
     * @return JSON string array of batched results, or NULL on error.
     */
    String parakeet_capi_transcribe_pcm_batch_json_lang(
            MemorySegment ctx,
            float[] pcmConcat,
            int[] sampleCounts,
            int batchSize,
            int sampleRate,
            int decoder,
            String targetLang);

    /**
     * Begin a streaming session selecting the language prompt for multilingual models. The context
     * (and its model) must outlive the stream.
     *
     * @param ctx The native context pointer.
     * @param targetLang Locale string (e.g., "en", "de", "auto"). NULL or "" uses the model's
     *     default.
     * @return Streaming session pointer, or NULL on failure (e.g., model is not a streaming model).
     */
    MemorySegment parakeet_capi_stream_begin_lang(MemorySegment ctx, String targetLang);

    /**
     * Feed a block of 16 kHz MONO float PCM to the streaming session. The session buffers the audio
     * and decodes as full encoder chunks become available. Returns a JSON document of newly
     * finalized text, plus end-of-utterance (EOU) / backchannel (EOB) events.
     *
     * @param stream The native stream pointer.
     * @param pcm Mono float PCM samples.
     * @param nSamples Number of samples.
     * @return JSON document string, or NULL on error.
     */
    String parakeet_capi_stream_feed_json(MemorySegment stream, float[] pcm, int nSamples);

    /**
     * Flush the end-of-stream tail: processes any remaining buffered audio. Returns the same JSON
     * document shape as {@link #parakeet_capi_stream_feed_json}.
     *
     * @param stream The native stream pointer.
     * @return The final newly-finalized text and events in a JSON document, or NULL on error.
     */
    String parakeet_capi_stream_finalize_json(MemorySegment stream);

    /**
     * Free a streaming session. Safe to call on a NULL pointer.
     *
     * @param stream The native stream pointer to free.
     */
    void parakeet_capi_stream_free(MemorySegment stream);

    /**
     * Standalone voice-activity detection. Returns speech segments as JSON without transcribing.
     * The context can hold either an ASR model with a VAD head or a Silero VAD model.
     *
     * @param ctx The native context pointer.
     * @param pcm Mono float PCM samples.
     * @param nSamples Number of samples.
     * @param sampleRate Sample rate in Hz. Resampled to 16 kHz internally if necessary.
     * @param optionsJson Flat JSON object for settings (threshold, min_pause, mode, etc). NULL uses
     *     defaults.
     * @return JSON string of speech regions, or NULL on error.
     */
    String parakeet_capi_vad_pcm_json(
            MemorySegment ctx, float[] pcm, int nSamples, int sampleRate, String optionsJson);

    /**
     * Standalone voice-activity detection reading from a WAV file. Returns speech segments as JSON
     * without transcribing.
     *
     * @param ctx The native context pointer.
     * @param wavPath Path to the WAV audio file.
     * @param optionsJson Flat JSON object for settings (threshold, min_pause, mode, etc). NULL uses
     *     defaults.
     * @return JSON string of speech regions, or NULL on error.
     */
    String parakeet_capi_vad_path_json(MemorySegment ctx, String wavPath, String optionsJson);

    /**
     * Transcribe long audio cut at pauses into segments of at most 30s. The segmenter can take its
     * probabilities from an explicit Silero VAD context, or the ASR model's own VAD head. Word and
     * token times are relative to the whole file.
     *
     * @param asrCtx The native ASR context pointer.
     * @param vadCtx A Silero VAD context pointer (or NULL to use the ASR model's own head).
     * @param wavPath Path to the WAV audio file.
     * @param decoder Selects the head: 0 = default, 1 = ctc, 2 = tdt/rnnt.
     * @param vadOptsJson VAD options specifying threshold, min_pause, max_segment, etc. NULL uses
     *     defaults.
     * @return JSON document containing transcribed text with timestamps, or NULL on error.
     */
    String parakeet_capi_transcribe_path_json_vad_with(
            MemorySegment asrCtx,
            MemorySegment vadCtx,
            String wavPath,
            int decoder,
            String vadOptsJson);

    /**
     * Begin a streaming Silero VAD session. One stream per audio stream; the Silero context must
     * outlive it and stay loaded.
     *
     * @param vadCtx A loaded Silero VAD context.
     * @param sampleRate Sample rate (16000 or 8000). Streams do not resample.
     * @param optionsJson VAD options object. "mode" must be "speech" and "run_gate" must be 0. NULL
     *     uses defaults.
     * @return Native stream pointer, or NULL on error.
     */
    MemorySegment parakeet_capi_vad_stream_begin(
            MemorySegment vadCtx, int sampleRate, String optionsJson);

    /**
     * Feeds mono float PCM in [-1, 1] to the VAD stream. Returns a JSON document of speech start
     * and end events that became known in this call.
     *
     * @param vadStream The native VAD stream pointer.
     * @param pcm Mono float PCM samples.
     * @param nSamples Number of samples.
     * @param isLast If non-zero, zero-pads the buffered partial chunk and closes any open region.
     * @return JSON document listing known speech events, or NULL on error.
     */
    String parakeet_capi_vad_stream_feed_json(
            MemorySegment vadStream, float[] pcm, int nSamples, int isLast);

    /**
     * Resets the VAD stream back to the initial state (empty buffers, time 0).
     *
     * @param vadStream The native VAD stream pointer.
     * @return 0 on success.
     */
    int parakeet_capi_vad_stream_reset(MemorySegment vadStream);

    /**
     * Free a streaming Silero VAD session. Safe to call on a NULL pointer.
     *
     * @param vadStream The native VAD stream pointer to free.
     */
    void parakeet_capi_vad_stream_free(MemorySegment vadStream);
}

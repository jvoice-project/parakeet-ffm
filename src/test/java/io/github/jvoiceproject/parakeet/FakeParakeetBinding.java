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

import io.github.jvoiceproject.parakeet.internal.ParakeetBinding;

import java.lang.foreign.MemorySegment;

/**
 * In-memory fake implementation of {@link ParakeetBinding} for unit tests. Built for parakeet.cpp
 * v0.6.1.
 *
 * @author Florian Hotze - Initial contribution
 */
public class FakeParakeetBinding implements ParakeetBinding {

    public int abiVersion = 10;
    public String version = "0.6.1";
    public int modelKind = 1;
    public String lastError = "Fake error";
    public String loadError = "Fake load error";

    public String transcriptToReturn = "Test transcript";
    public String jsonToReturn = "{}";
    public boolean shouldFail = false;

    public int freeCtxCount = 0;
    public int freeStreamCount = 0;
    public int freeVadStreamCount = 0;

    public String lastTargetLang;
    public int lastDecoder;
    public String lastOptionsJson;
    public int lastSampleRate;
    public MemorySegment lastVadCtx;

    @Override
    public int parakeet_capi_abi_version() {
        return abiVersion;
    }

    @Override
    public String parakeet_version() {
        return version;
    }

    @Override
    public MemorySegment parakeet_capi_load(String ggufPath) {
        if (shouldFail) {
            return MemorySegment.NULL;
        }
        return MemorySegment.ofAddress(0x1000);
    }

    @Override
    public MemorySegment parakeet_capi_load_component(String ggufPath, String component) {
        if (shouldFail) {
            return MemorySegment.NULL;
        }
        return MemorySegment.ofAddress(0x2000);
    }

    @Override
    public String parakeet_capi_bundle_components_json(String ggufPath) {
        if (shouldFail) {
            return null;
        }
        return jsonToReturn;
    }

    @Override
    public void parakeet_capi_free(MemorySegment ctx) {
        freeCtxCount++;
    }

    @Override
    public int parakeet_capi_model_kind(MemorySegment ctx) {
        return modelKind;
    }

    @Override
    public String parakeet_capi_last_error(MemorySegment ctx) {
        return lastError;
    }

    @Override
    public String parakeet_capi_load_error() {
        return loadError;
    }

    @Override
    public int parakeet_capi_set_concurrency(MemorySegment ctx, int backends, int threadsEach) {
        return backends;
    }

    @Override
    public String parakeet_capi_transcribe_path_lang(
            MemorySegment ctx, String wavPath, int decoder, String targetLang) {
        lastDecoder = decoder;
        lastTargetLang = targetLang;
        if (shouldFail) {
            return null;
        }
        return transcriptToReturn;
    }

    @Override
    public String parakeet_capi_transcribe_pcm_lang(
            MemorySegment ctx,
            float[] pcm,
            int nSamples,
            int sampleRate,
            int decoder,
            String targetLang) {
        lastDecoder = decoder;
        lastTargetLang = targetLang;
        lastSampleRate = sampleRate;
        if (shouldFail) {
            return null;
        }
        return transcriptToReturn;
    }

    @Override
    public String parakeet_capi_transcribe_path_json(
            MemorySegment ctx, String wavPath, int decoder) {
        lastDecoder = decoder;
        if (shouldFail) {
            return null;
        }
        return jsonToReturn;
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
        lastDecoder = decoder;
        lastTargetLang = targetLang;
        lastSampleRate = sampleRate;
        if (shouldFail) {
            return -1;
        }
        if (outTexts != null) {
            for (int i = 0; i < batchSize; i++) {
                outTexts[i] = transcriptToReturn;
            }
        }
        return 0;
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
        lastDecoder = decoder;
        lastTargetLang = targetLang;
        lastSampleRate = sampleRate;
        if (shouldFail) {
            return null;
        }
        return jsonToReturn;
    }

    @Override
    public MemorySegment parakeet_capi_stream_begin_lang(MemorySegment ctx, String targetLang) {
        lastTargetLang = targetLang;
        if (shouldFail) {
            return MemorySegment.NULL;
        }
        return MemorySegment.ofAddress(0x3000);
    }

    @Override
    public String parakeet_capi_stream_feed_json(MemorySegment stream, float[] pcm, int nSamples) {
        if (shouldFail) {
            return null;
        }
        return jsonToReturn;
    }

    @Override
    public String parakeet_capi_stream_finalize_json(MemorySegment stream) {
        if (shouldFail) {
            return null;
        }
        return jsonToReturn;
    }

    @Override
    public void parakeet_capi_stream_free(MemorySegment stream) {
        freeStreamCount++;
    }

    @Override
    public String parakeet_capi_vad_pcm_json(
            MemorySegment ctx, float[] pcm, int nSamples, int sampleRate, String optionsJson) {
        lastOptionsJson = optionsJson;
        lastSampleRate = sampleRate;
        if (shouldFail) {
            return null;
        }
        return jsonToReturn;
    }

    @Override
    public String parakeet_capi_vad_path_json(
            MemorySegment ctx, String wavPath, String optionsJson) {
        lastOptionsJson = optionsJson;
        if (shouldFail) {
            return null;
        }
        return jsonToReturn;
    }

    @Override
    public String parakeet_capi_transcribe_path_json_vad_with(
            MemorySegment asrCtx,
            MemorySegment vadCtx,
            String wavPath,
            int decoder,
            String vadOptsJson) {
        lastVadCtx = vadCtx;
        lastDecoder = decoder;
        lastOptionsJson = vadOptsJson;
        if (shouldFail) {
            return null;
        }
        return jsonToReturn;
    }

    @Override
    public MemorySegment parakeet_capi_vad_stream_begin(
            MemorySegment vadCtx, int sampleRate, String optionsJson) {
        lastOptionsJson = optionsJson;
        lastSampleRate = sampleRate;
        if (shouldFail) {
            return MemorySegment.NULL;
        }
        return MemorySegment.ofAddress(0x4000);
    }

    @Override
    public String parakeet_capi_vad_stream_feed_json(
            MemorySegment vadStream, float[] pcm, int nSamples, int isLast) {
        if (shouldFail) {
            return null;
        }
        return jsonToReturn;
    }

    @Override
    public int parakeet_capi_vad_stream_reset(MemorySegment vadStream) {
        return shouldFail ? -1 : 0;
    }

    @Override
    public void parakeet_capi_vad_stream_free(MemorySegment vadStream) {
        freeVadStreamCount++;
    }
}

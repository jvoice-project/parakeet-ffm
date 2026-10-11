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
package io.github.jvoiceproject.parakeet.transcribe;

import io.github.jvoiceproject.parakeet.ParakeetException;
import io.github.jvoiceproject.parakeet.internal.Json;
import io.github.jvoiceproject.parakeet.internal.ParakeetBinding;

import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * An incremental, cache-aware streaming speech recognition session.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class TranscribeStream implements AutoCloseable {

    private final ParakeetBinding lib;
    private final MemorySegment stream;
    private final Consumer<TranscribeStream> onClose;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public TranscribeStream(
            ParakeetBinding lib, MemorySegment stream, Consumer<TranscribeStream> onClose) {
        this.lib = Objects.requireNonNull(lib, "lib cannot be null");
        this.stream = Objects.requireNonNull(stream, "stream cannot be null");
        this.onClose = Objects.requireNonNull(onClose, "onClose cannot be null");
    }

    /**
     * Feeds 16 kHz mono float PCM samples to the streaming decoder.
     *
     * @param pcm Float audio samples in range [-1.0, 1.0].
     * @return {@link TranscribeStreamResult} containing partial text, timestamps, and turn-taking
     *     events.
     * @throws IllegalStateException if this stream is closed.
     * @throws ParakeetException if decoding fails.
     */
    public TranscribeStreamResult feed(float[] pcm) {
        checkOpen();
        Objects.requireNonNull(pcm, "pcm samples cannot be null");
        String json = lib.parakeet_capi_stream_feed_json(stream, pcm, pcm.length);
        if (json == null) {
            throw new ParakeetException("Streaming feed failed");
        }
        return Json.GSON.fromJson(json, TranscribeStreamResult.class);
    }

    /**
     * Flushes tail decoder frames and finalizes the streaming session.
     *
     * @return Final {@link TranscribeStreamResult} containing trailing text and events.
     * @throws IllegalStateException if this stream is closed.
     * @throws ParakeetException if finalizing fails.
     */
    public TranscribeStreamResult finish() {
        checkOpen();
        String json = lib.parakeet_capi_stream_finalize_json(stream);
        if (json == null) {
            throw new ParakeetException("Streaming finalize failed");
        }
        return Json.GSON.fromJson(json, TranscribeStreamResult.class);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                lib.parakeet_capi_stream_free(stream);
            } finally {
                onClose.accept(this);
            }
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    private void checkOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Streaming session has already been closed");
        }
    }
}

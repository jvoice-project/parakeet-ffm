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
package io.github.jvoiceproject.parakeet.vad;

import io.github.jvoiceproject.parakeet.ParakeetException;
import io.github.jvoiceproject.parakeet.internal.Json;
import io.github.jvoiceproject.parakeet.internal.ParakeetBinding;

import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * An incremental streaming session for Voice Activity Detection (powered by Silero VAD).
 *
 * <p>A stream maintains stateful incremental buffers and is intended to be fed sequentially. All
 * operations are thread-safe and guarded against concurrent access and closing races.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class VadStream implements AutoCloseable {

    private final ParakeetBinding lib;
    private final MemorySegment vadStream;
    private final Consumer<VadStream> onClose;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();

    public VadStream(ParakeetBinding lib, MemorySegment vadStream, Consumer<VadStream> onClose) {
        this.lib = Objects.requireNonNull(lib, "lib cannot be null");
        this.vadStream = Objects.requireNonNull(vadStream, "vadStream cannot be null");
        this.onClose = Objects.requireNonNull(onClose, "onClose cannot be null");
    }

    /**
     * Feeds mono float PCM audio chunks to the VAD stream.
     *
     * @param pcm Float audio samples in range [-1.0, 1.0].
     * @param isLast Whether this is the final chunk of audio.
     * @return {@link VadStreamResult} containing newly detected speech events.
     * @throws IllegalStateException if this stream is closed.
     * @throws ParakeetException if feeding fails.
     */
    public VadStreamResult feed(float[] pcm, boolean isLast) {
        String json;
        lock.lock();
        try {
            checkOpen();
            Objects.requireNonNull(pcm, "pcm samples cannot be null");
            json =
                    lib.parakeet_capi_vad_stream_feed_json(
                            vadStream, pcm, pcm.length, isLast ? 1 : 0);
        } finally {
            lock.unlock();
        }
        if (json == null) {
            throw new ParakeetException("VAD stream feed failed");
        }
        return Json.GSON.fromJson(json, VadStreamResult.class);
    }

    /**
     * Resets internal buffers and timestamps to 0 without recreating the stream.
     *
     * @throws IllegalStateException if this stream is closed.
     * @throws ParakeetException if reset fails.
     */
    public void reset() {
        lock.lock();
        try {
            checkOpen();
            int rc = lib.parakeet_capi_vad_stream_reset(vadStream);
            if (rc != 0) {
                throw new ParakeetException("Failed to reset VAD stream (code " + rc + ")");
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            if (closed.compareAndSet(false, true)) {
                try {
                    lib.parakeet_capi_vad_stream_free(vadStream);
                } finally {
                    onClose.accept(this);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    private void checkOpen() {
        if (closed.get()) {
            throw new IllegalStateException("VAD streaming session has already been closed");
        }
    }
}

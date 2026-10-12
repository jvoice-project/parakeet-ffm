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

import java.util.Objects;

/**
 * Audio conversion utilities for converting between PCM sample formats.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class AudioConverter {
    private AudioConverter() {}

    /**
     * Converts signed 16-bit PCM audio samples to float PCM in range [-1.0, 1.0].
     *
     * @param pcm Signed 16-bit PCM audio samples.
     * @return Float PCM audio samples.
     */
    public static float[] pcm16ToFloat(short[] pcm) {
        Objects.requireNonNull(pcm, "pcm samples cannot be null");
        float[] floats = new float[pcm.length];
        pcm16ToFloat(pcm, floats);
        return floats;
    }

    /**
     * Converts signed 16-bit PCM audio samples to float PCM in range [-1.0, 1.0] and writes them
     * into the provided target array.
     *
     * @param pcm Signed 16-bit PCM audio samples.
     * @param target Target array to receive the converted float PCM samples (must have length
     *     {@code >= pcm.length}).
     * @throws IllegalArgumentException if {@code target.length < pcm.length}.
     */
    public static void pcm16ToFloat(short[] pcm, float[] target) {
        Objects.requireNonNull(pcm, "pcm samples cannot be null");
        Objects.requireNonNull(target, "target array cannot be null");
        if (target.length < pcm.length) {
            throw new IllegalArgumentException(
                    "target array length ("
                            + target.length
                            + ") must be >= pcm length ("
                            + pcm.length
                            + ")");
        }
        for (int i = 0; i < pcm.length; i++) {
            target[i] = pcm[i] / 32768.0f;
        }
    }
}

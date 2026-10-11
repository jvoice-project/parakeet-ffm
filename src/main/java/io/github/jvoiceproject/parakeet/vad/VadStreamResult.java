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
package io.github.jvoiceproject.parakeet.vad;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Result of feeding audio to a streaming Voice Activity Detection session.
 *
 * @param frameSec VAD frame duration in seconds.
 * @param firstFrame Index of first frame processed in this call.
 * @param events Speech activity events emitted in this call.
 * @param probabilities Frame speech probabilities (if requested).
 * @author Florian Hotze - Initial contribution
 */
public record VadStreamResult(
        @SerializedName("frame_sec") double frameSec,
        @SerializedName("first_frame") int firstFrame,
        List<VadEvent> events,
        List<Float> probabilities) {

    // TODO: defensive copying ensures true immutability, but this is always mapped from JSON so
    // shouldn't be needed
    public VadStreamResult {
        events = events == null ? List.of() : List.copyOf(events);
        probabilities = probabilities == null ? List.of() : List.copyOf(probabilities);
    }
}

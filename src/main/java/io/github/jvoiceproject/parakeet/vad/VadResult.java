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

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Result of offline Voice Activity Detection.
 *
 * @param mode VAD mode ("speech" or "segments").
 * @param duration Total audio duration in seconds.
 * @param frameSec VAD frame duration in seconds.
 * @param backend Compute backend used.
 * @param segments Detected speech segments.
 * @param probabilities Frame-by-frame speech probabilities (if requested).
 * @author Florian Hotze - Initial contribution
 */
public record VadResult(
        String mode,
        double duration,
        @SerializedName("frame_sec") double frameSec,
        String backend,
        List<VadSegment> segments,
        List<Float> probabilities) {

    public VadResult {
        segments = segments == null ? List.of() : List.copyOf(segments);
        probabilities = probabilities == null ? List.of() : List.copyOf(probabilities);
    }
}

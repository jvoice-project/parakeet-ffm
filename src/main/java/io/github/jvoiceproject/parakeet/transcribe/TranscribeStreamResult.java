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

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Result of a streaming audio feed or finalize call.
 *
 * @param text Newly finalized transcript text since the last call.
 * @param eou True if an End of Utterance (&lt;EOU&gt;) event fired.
 * @param eob True if an End of Backchannel (&lt;EOB&gt;) event fired.
 * @param frameSec Encoder frame duration in seconds.
 * @param events Events drained during this step.
 * @param words Words finalized during this step.
 * @author Florian Hotze - Initial contribution
 */
public record TranscribeStreamResult(
        String text,
        boolean eou,
        boolean eob,
        @SerializedName("frame_sec") double frameSec,
        List<TranscribeStreamEvent> events,
        List<Word> words) {

    // TODO: defensive copying ensures true immutability, but this is always mapped from JSON so
    // shouldn't be needed
    public TranscribeStreamResult {
        events = events == null ? List.of() : List.copyOf(events);
        words = words == null ? List.of() : List.copyOf(words);
    }
}

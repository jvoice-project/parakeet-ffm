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
package io.github.jvoiceproject.parakeet.transcribe;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Result of an ASR transcription containing full text and token/word timestamps.
 *
 * @param text Full transcript text.
 * @param frameSec Encoder frame duration in seconds.
 * @param words List of timestamped words.
 * @param tokens List of timestamped tokens.
 * @author Florian Hotze - Initial contribution
 */
public record Transcription(
        String text,
        @SerializedName("frame_sec") double frameSec,
        List<Word> words,
        List<Token> tokens) {

    public Transcription {
        words = words == null ? List.of() : List.copyOf(words);
        tokens = tokens == null ? List.of() : List.copyOf(tokens);
    }
}

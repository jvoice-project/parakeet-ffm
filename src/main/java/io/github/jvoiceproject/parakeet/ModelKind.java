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

/**
 * Kind of model loaded in a Parakeet context.
 *
 * @author Florian Hotze - Initial contribution
 */
public enum ModelKind {
    NONE(0),
    /** Speech-to-Text recognition model */
    ASR(1),
    /** Speaker diarization */
    DIARIZATION(2),
    /** Sound event tagging (CED) */
    SOUND(3),
    /** Speaker identification (voice-detect) */
    SPEAKER(4),
    /** Voice Activity Detection model (Silero VAD or slice) */
    VAD(5);

    private final int value;

    ModelKind(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ModelKind fromValue(int value) {
        for (ModelKind kind : values()) {
            if (kind.value == value) {
                return kind;
            }
        }
        return NONE;
    }
}

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

import java.util.Objects;

/**
 * Configuration options for ASR transcription.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class TranscribeOptions {

    /** Decoder head selection for ASR transcription. */
    public enum Decoder {
        /** Default decoder chosen based on model architecture. */
        DEFAULT(0),
        /** Force CTC head. */
        CTC(1),
        /** Force transducer (TDT/RNNT) head. */
        TRANSDUCER(2);

        private final int value;

        Decoder(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static final TranscribeOptions DEFAULT = new Builder().build();

    private final Decoder decoder;
    private final String language;

    private TranscribeOptions(Builder builder) {
        this.decoder = builder.decoder;
        this.language = builder.language;
    }

    public Decoder getDecoder() {
        return decoder;
    }

    public String getLanguage() {
        return language;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Decoder decoder = Decoder.DEFAULT;
        private String language;

        public Builder decoder(Decoder decoder) {
            this.decoder = Objects.requireNonNull(decoder, "decoder cannot be null");
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public TranscribeOptions build() {
            return new TranscribeOptions(this);
        }
    }
}

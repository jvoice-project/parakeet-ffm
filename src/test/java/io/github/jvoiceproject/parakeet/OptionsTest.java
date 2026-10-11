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

import io.github.jvoiceproject.parakeet.transcribe.TranscribeOptions;
import io.github.jvoiceproject.parakeet.vad.VadOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TranscribeOptions} and {@link VadOptions}.
 *
 * @author Florian Hotze - Initial contribution
 */
public class OptionsTest {

    @Test
    void transcribeOptionsDefaultsAndBuilder() {
        TranscribeOptions def = TranscribeOptions.DEFAULT;

        assertEquals(TranscribeOptions.Decoder.DEFAULT, def.getDecoder());
        assertNull(def.getLanguage());

        TranscribeOptions custom =
                TranscribeOptions.builder()
                        .decoder(TranscribeOptions.Decoder.TRANSDUCER)
                        .language("de")
                        .build();
        assertEquals(TranscribeOptions.Decoder.TRANSDUCER, custom.getDecoder());
        assertEquals("de", custom.getLanguage());
    }

    @Test
    void vadOptionsDefaultsAndSerialization() {
        VadOptions def = VadOptions.DEFAULT;
        assertNull(def.toJson(), "Empty options should serialize to null to keep native defaults");

        VadOptions custom =
                VadOptions.builder()
                        .threshold(0.7)
                        .minPause(0.25)
                        .mode(VadOptions.Mode.SPEECH)
                        .probabilities(true)
                        .build();

        String json = custom.toJson();
        assertNotNull(json, "Custom options should serialize to JSON");
        assertTrue(json.contains("\"threshold\":0.7"));
        assertTrue(json.contains("\"min_pause\":0.25"));
        assertTrue(json.contains("\"mode\":\"speech\""));
        assertTrue(json.contains("\"probabilities\":1"));
        assertFalse(json.contains("min_speech"), "Unset keys should not be included");
    }

    @Test
    void vadOptionsValidation() {
        assertThrows(IllegalArgumentException.class, () -> VadOptions.builder().threshold(0.0));
        assertThrows(IllegalArgumentException.class, () -> VadOptions.builder().threshold(1.5));
        assertThrows(IllegalArgumentException.class, () -> VadOptions.builder().minPause(-0.1));
        assertThrows(IllegalArgumentException.class, () -> VadOptions.builder().speechPad(-0.5));
        assertThrows(IllegalArgumentException.class, () -> VadOptions.builder().runGate(1.0));
    }
}

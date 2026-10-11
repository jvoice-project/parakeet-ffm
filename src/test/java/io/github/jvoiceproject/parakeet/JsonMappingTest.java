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

import io.github.jvoiceproject.parakeet.internal.Json;
import io.github.jvoiceproject.parakeet.transcribe.TranscribeStreamResult;
import io.github.jvoiceproject.parakeet.transcribe.Transcription;
import io.github.jvoiceproject.parakeet.vad.VadResult;
import io.github.jvoiceproject.parakeet.vad.VadStreamResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

/**
 * Validates deserialization of documented parakeet.cpp JSON responses into Java records.
 *
 * @author Florian Hotze - Initial contribution
 */
public class JsonMappingTest {

    @Test
    void parseTranscription() {
        String json =
                """
                {
                  "text": "Hello world",
                  "frame_sec": 0.080000,
                  "words": [
                    {"w": "Hello", "start": 0.480, "end": 0.640, "conf": 0.9100},
                    {"w": "world", "start": 0.640, "end": 0.920, "conf": 0.8800}
                  ],
                  "tokens": [
                    {"id": 123, "t": 0.480, "conf": 0.9100},
                    {"id": 456, "t": 0.640, "conf": 0.8800}
                  ]
                }
                """;

        Transcription t = Json.GSON.fromJson(json, Transcription.class);

        assertEquals("Hello world", t.text());
        assertEquals(0.08, t.frameSec(), 1e-6);
        assertEquals(2, t.words().size());

        assertEquals("Hello", t.words().getFirst().text());
        assertEquals(0.480, t.words().getFirst().start());
        assertEquals(0.640, t.words().getFirst().end());
        assertEquals(0.9100, t.words().getFirst().confidence());

        assertEquals(2, t.tokens().size());

        assertEquals(123, t.tokens().getFirst().id());
        assertEquals(0.480, t.tokens().getFirst().time());
    }

    @Test
    void parseStreamResult() {
        String json =
                """
                {
                  "text": "some words",
                  "eou": 1,
                  "eob": 0,
                  "frame_sec": 0.080000,
                  "events": [
                    {"type": "eou", "frame": 31, "t": 2.480}
                  ],
                  "words": [
                    {"w": "some", "start": 0.480, "end": 0.640, "conf": 0.9100}
                  ]
                }
                """;

        TranscribeStreamResult res = Json.GSON.fromJson(json, TranscribeStreamResult.class);

        assertEquals("some words", res.text());
        assertTrue(res.eou());
        assertFalse(res.eob());
        assertEquals(0.08, res.frameSec(), 1e-6);
        assertEquals(1, res.events().size());

        assertEquals("eou", res.events().getFirst().type());
        assertEquals(31, res.events().getFirst().frame());
        assertEquals(2.480, res.events().getFirst().time());

        assertEquals(1, res.words().size());
    }

    @Test
    void parseVadResult() {
        String json =
                """
                {
                  "mode": "speech",
                  "duration": 12.340,
                  "frame_sec": 0.080,
                  "backend": "cpu",
                  "segments": [
                    {"start": 0.480, "end": 3.200}
                  ],
                  "probabilities": [0.0123, 0.9850]
                }
                """;

        VadResult res = Json.GSON.fromJson(json, VadResult.class);
        assertEquals("speech", res.mode());
        assertEquals(12.340, res.duration());
        assertEquals(0.080, res.frameSec());
        assertEquals("cpu", res.backend());
        assertEquals(1, res.segments().size());

        assertEquals(0.480, res.segments().getFirst().start());
        assertEquals(3.200, res.segments().getFirst().end());

        assertEquals(2, res.probabilities().size());
    }

    @Test
    void parseVadStreamResult() {
        String json =
                """
                {
                  "frame_sec": 0.032,
                  "first_frame": 120,
                  "events": [
                    {"type": "start", "time": 3.456},
                    {"type": "end", "time": 5.120}
                  ],
                  "probabilities": [0.01]
                }
                """;

        VadStreamResult res = Json.GSON.fromJson(json, VadStreamResult.class);

        assertEquals(0.032, res.frameSec());
        assertEquals(120, res.firstFrame());

        assertEquals(2, res.events().size());
        assertEquals("start", res.events().getFirst().type());
        assertEquals(3.456, res.events().getFirst().time());
    }

    @Test
    void parseBundleComponents() {
        String json =
                """
                [
                  {
                    "name": "asr_en",
                    "kind": "asr",
                    "license": "CC-BY-4.0",
                    "license_url": "https://example.com/license",
                    "source": "nvidia/parakeet",
                    "attribution": "NVIDIA",
                    "changes": "converted to gguf",
                    "source_sha256": "abc",
                    "content_sha256": "def",
                    "tensors": 150,
                    "bytes": 500000000
                  }
                ]
                """;

        List<BundleComponent> list =
                Json.GSON.fromJson(json, new TypeToken<List<BundleComponent>>() {}.getType());

        assertNotNull(list);
        assertEquals(1, list.size());
        BundleComponent comp = list.getFirst();
        assertEquals("asr_en", comp.name());
        assertEquals("asr", comp.kind());
        assertEquals("CC-BY-4.0", comp.license());
        assertEquals(150, comp.tensors());
        assertEquals(500000000L, comp.bytes());
    }
}

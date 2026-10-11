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

import io.github.jvoiceproject.parakeet.vad.VadOptions;
import io.github.jvoiceproject.parakeet.vad.VadResult;
import io.github.jvoiceproject.parakeet.vad.VadSegment;
import io.github.jvoiceproject.parakeet.vad.VadStream;
import io.github.jvoiceproject.parakeet.vad.VadStreamResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Voice Activity Detection (VAD) using Silero VAD model.
 *
 * @author Florian Hotze - Initial contribution
 */
@Tag("vad")
public class ParakeetVadIntegrationTest extends ParakeetIntegrationTest {

    @Test
    void offlineVad() {
        try (ParakeetModel vad = Parakeet.loadModel(vadModelPath)) {
            assertEquals(ModelKind.VAD, vad.kind());

            VadResult res = vad.detectSpeech(audioPath, VadOptions.DEFAULT);
            assertNotNull(res);
            assertEquals("speech", res.mode());
            assertTrue(res.duration() > 0.0);
            assertFalse(res.segments().isEmpty(), "Should detect speech in audio file");

            for (VadSegment seg : res.segments()) {
                assertTrue(seg.start() >= 0.0);
                assertTrue(seg.end() <= res.duration() + 0.1);
                assertTrue(seg.start() < seg.end());
            }
        }
    }

    @Test
    void streamingVad() throws Exception {
        try (ParakeetModel vad = Parakeet.loadModel(vadModelPath)) {
            float[] pcm = readWavMono16k(audioPath);

            try (VadStream vs = vad.startVadStream(16000, VadOptions.DEFAULT)) {
                int chunkSize = 1600; // 100 ms
                int eventCount = 0;
                for (int offset = 0; offset < pcm.length; offset += chunkSize) {
                    int len = Math.min(chunkSize, pcm.length - offset);
                    boolean isLast = (offset + len >= pcm.length);
                    float[] chunk = new float[len];
                    System.arraycopy(pcm, offset, chunk, 0, len);
                    VadStreamResult res = vs.feed(chunk, isLast);
                    eventCount += res.events().size();
                }
                assertTrue(eventCount > 0, "VAD stream should emit speech start/end events");
            }
        }
    }
}

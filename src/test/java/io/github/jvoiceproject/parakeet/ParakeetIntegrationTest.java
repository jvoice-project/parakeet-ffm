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
import io.github.jvoiceproject.parakeet.transcribe.TranscribeStream;
import io.github.jvoiceproject.parakeet.transcribe.TranscribeStreamResult;
import io.github.jvoiceproject.parakeet.transcribe.Transcription;
import io.github.jvoiceproject.parakeet.transcribe.Word;
import io.github.jvoiceproject.parakeet.vad.VadOptions;
import io.github.jvoiceproject.parakeet.vad.VadResult;
import io.github.jvoiceproject.parakeet.vad.VadSegment;
import io.github.jvoiceproject.parakeet.vad.VadStream;
import io.github.jvoiceproject.parakeet.vad.VadStreamResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration tests running against real native library and downloaded GGUF models.
 *
 * @author Florian Hotze - Initial contribution
 */
@Tag("integration")
public class ParakeetIntegrationTest {

    private static Path modelPath;
    private static Path streamModelPath;
    private static Path vadModelPath;
    private static Path audioPath;
    private static String expectedText;

    @BeforeAll
    static void setUp() {
        String modelEnv = System.getenv("PARAKEET_MODEL");
        String streamEnv = System.getenv("PARAKEET_STREAM_MODEL");
        String vadEnv = System.getenv("PARAKEET_VAD_MODEL");
        String audioEnv = System.getenv("TEST_AUDIO");
        expectedText = System.getenv("EXPECTED_TEXT");

        assertNotNull(modelEnv, "PARAKEET_MODEL environment variable must be set");
        assertNotNull(streamEnv, "PARAKEET_STREAM_MODEL environment variable must be set");
        assertNotNull(vadEnv, "PARAKEET_VAD_MODEL environment variable must be set");
        assertNotNull(audioEnv, "TEST_AUDIO environment variable must be set");
        assertNotNull(expectedText, "EXPECTED_TEXT environment variable must be set");

        modelPath = Path.of(modelEnv);
        streamModelPath = Path.of(streamEnv);
        vadModelPath = Path.of(vadEnv);
        audioPath = Path.of(audioEnv);

        assertTrue(Files.exists(modelPath), "Model file must exist at " + modelPath);
        assertTrue(
                Files.exists(streamModelPath),
                "Stream model file must exist at " + streamModelPath);
        assertTrue(Files.exists(vadModelPath), "VAD model file must exist at " + vadModelPath);
        assertTrue(Files.exists(audioPath), "Audio file must exist at " + audioPath);
    }

    @Test
    void versionAndAbi() {
        String ver = Parakeet.version();
        assertNotNull(ver);
        assertFalse(ver.isBlank());
        assertEquals(10, Parakeet.abiVersion(), "Expected ABI version == 10");
        System.out.println("Parakeet version: " + ver + ", ABI: " + Parakeet.abiVersion());
    }

    @Test
    void loadMissingModelThrows() {
        assertThrows(
                ParakeetException.class,
                () -> Parakeet.loadModel(Path.of("/nonexistent/model.gguf")));
    }

    @Test
    void offlineTranscription() throws Exception {
        try (ParakeetModel model = Parakeet.loadModel(modelPath)) {
            assertEquals(ModelKind.ASR, model.kind());

            String text = model.transcribe(audioPath);
            assertEquals(expectedText, text);

            float[] pcm = readWavMono16k(audioPath);
            String pcmText = model.transcribe(pcm, 16000, TranscribeOptions.DEFAULT);
            assertEquals(expectedText, pcmText);
        }
    }

    @Test
    void offlineTranscriptionWithTimestamps() {
        try (ParakeetModel model = Parakeet.loadModel(modelPath)) {
            assertEquals(ModelKind.ASR, model.kind());

            Transcription t = model.transcribeWithTimestamps(audioPath, TranscribeOptions.DEFAULT);
            assertNotNull(t);
            assertEquals(expectedText, t.text());
            assertTrue(t.frameSec() > 0.0);
            assertFalse(t.words().isEmpty());

            double lastStart = -1.0;
            for (Word w : t.words()) {
                assertNotNull(w.text());
                assertTrue(w.start() <= w.end());
                assertTrue(w.start() >= lastStart);
                assertTrue(w.confidence() > 0.0 && w.confidence() <= 1.0);
                lastStart = w.start();
            }
        }
    }

    @Test
    void batchTranscription() throws Exception {
        try (ParakeetModel model = Parakeet.loadModel(modelPath)) {
            assertEquals(ModelKind.ASR, model.kind());

            float[] pcm = readWavMono16k(audioPath);
            List<float[]> clips = List.of(pcm, pcm);

            List<String> results = model.batchTranscribe(clips, 16000, TranscribeOptions.DEFAULT);
            assertEquals(2, results.size());
            assertEquals(expectedText, results.get(0));
            assertEquals(expectedText, results.get(1));

            List<Transcription> tsResults =
                    model.batchTranscribeWithTimestamps(clips, 16000, TranscribeOptions.DEFAULT);
            assertEquals(2, tsResults.size());
            assertEquals(expectedText, tsResults.get(0).text());
            assertEquals(expectedText, tsResults.get(1).text());
        }
    }

    @Test
    void concurrency() throws Exception {
        try (ParakeetModel model = Parakeet.loadModel(modelPath)) {
            int backends = model.setConcurrency(2, 1);
            assertTrue(backends >= 1);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Callable<String> task = () -> model.transcribe(audioPath);
                List<Future<String>> futures = executor.invokeAll(List.of(task, task));
                for (Future<String> f : futures) {
                    assertEquals(expectedText, f.get());
                }
            } finally {
                executor.shutdown();
            }
        }
    }

    @Test
    void offlineModelRejectsStreaming() {
        try (ParakeetModel model = Parakeet.loadModel(modelPath)) {
            assertThrows(ParakeetException.class, model::startTranscribeStream);
        }
    }

    @Test
    void streamingModel() throws Exception {
        try (ParakeetModel model = Parakeet.loadModel(streamModelPath)) {
            assertEquals(ModelKind.ASR, model.kind());

            float[] pcm = readWavMono16k(audioPath);
            StringBuilder sb = new StringBuilder();

            try (TranscribeStream stream = model.startTranscribeStream()) {
                int chunkSize = 2560; // 160 ms at 16 kHz
                for (int offset = 0; offset < pcm.length; offset += chunkSize) {
                    int len = Math.min(chunkSize, pcm.length - offset);
                    float[] chunk = new float[len];
                    System.arraycopy(pcm, offset, chunk, 0, len);
                    TranscribeStreamResult res = stream.feed(chunk);
                    if (!res.text().isEmpty()) {
                        sb.append(res.text());
                    }
                }
                TranscribeStreamResult fin = stream.finish();
                if (!fin.text().isEmpty()) {
                    sb.append(fin.text());
                }
            }

            String fullText = sb.toString();
            assertFalse(fullText.isBlank(), "Streaming transcription should produce text");
            System.out.println("Streaming result: " + fullText);
        }
    }

    @Test
    void vadSpeechDetection() {
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
    void vadStreaming() throws Exception {
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

    @Test
    void transcribeWithVad() {
        try (ParakeetModel asr = Parakeet.loadModel(modelPath);
                ParakeetModel vad = Parakeet.loadModel(vadModelPath)) {
            Transcription t =
                    asr.transcribeWithVad(
                            vad, audioPath, TranscribeOptions.DEFAULT, VadOptions.DEFAULT);
            assertNotNull(t);
            assertEquals(expectedText, t.text());
        }
    }

    private static float[] readWavMono16k(Path wavPath) throws Exception {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(wavPath.toFile())) {
            AudioFormat targetFormat =
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
            try (AudioInputStream pcmAis = AudioSystem.getAudioInputStream(targetFormat, ais)) {
                byte[] bytes = pcmAis.readAllBytes();
                ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                float[] samples = new float[bytes.length / 2];
                for (int i = 0; i < samples.length; i++) {
                    samples[i] = bb.getShort() / 32768.0f;
                }
                return samples;
            }
        }
    }
}

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
import io.github.jvoiceproject.parakeet.vad.VadOptions;
import io.github.jvoiceproject.parakeet.vad.VadResult;
import io.github.jvoiceproject.parakeet.vad.VadStream;
import io.github.jvoiceproject.parakeet.vad.VadStreamResult;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link ParakeetModel} and streams using {@link FakeParakeetBinding}.
 *
 * @author Florian Hotze - Initial contribution
 */
public class ParakeetModelTest {

    private FakeParakeetBinding fake;
    private ParakeetModel model;

    @BeforeEach
    void setUp() {
        fake = new FakeParakeetBinding();
        model = new ParakeetModel(fake, MemorySegment.ofAddress(0x1000));
    }

    @Test
    void modelKindMapping() {
        fake.modelKind = 1;
        assertEquals(ModelKind.ASR, model.kind());

        fake.modelKind = 5;
        assertEquals(ModelKind.VAD, model.kind());

        fake.modelKind = 99;
        assertEquals(ModelKind.NONE, model.kind());
    }

    @Test
    void setConcurrency() {
        assertEquals(4, model.setConcurrency(4, 2));
    }

    @Test
    void transcribeFloatPcm() {
        fake.transcriptToReturn = "Hello world";
        TranscribeOptions options =
                TranscribeOptions.builder()
                        .decoder(TranscribeOptions.Decoder.CTC)
                        .language("en")
                        .build();

        float[] pcm = new float[] {0.1f, -0.1f};
        String result = model.transcribe(pcm, 16000, options);

        assertEquals("Hello world", result);
        assertEquals(TranscribeOptions.Decoder.CTC.getValue(), fake.lastDecoder);
        assertEquals("en", fake.lastTargetLang);
        assertEquals(16000, fake.lastSampleRate);
    }

    @Test
    void transcribeSignedPcm() {
        fake.transcriptToReturn = "Hello world";
        short[] pcm = new short[] {-32768, 0, 16384};

        TranscribeOptions options =
                TranscribeOptions.builder()
                        .decoder(TranscribeOptions.Decoder.CTC)
                        .language("en")
                        .build();

        String result = model.transcribe(pcm, 16000, options);

        assertNotNull(fake.lastPcm);
        assertEquals(3, fake.lastPcm.length);
        assertEquals(-1.0f, fake.lastPcm[0], 1e-6f);
        assertEquals(0.0f, fake.lastPcm[1], 1e-6f);
        assertEquals(0.5f, fake.lastPcm[2], 1e-6f);

        assertEquals("Hello world", result);
        assertEquals(TranscribeOptions.Decoder.CTC.getValue(), fake.lastDecoder);
        assertEquals("en", fake.lastTargetLang);
        assertEquals(16000, fake.lastSampleRate);
    }

    @Test
    void transcribeWavSuccess(@TempDir Path tempDir) throws IOException {
        Path wav = tempDir.resolve("test.wav");
        Files.write(wav, new byte[] {1, 2, 3});

        fake.transcriptToReturn = "Wav transcript";
        String result = model.transcribe(wav);

        assertEquals("Wav transcript", result);
    }

    @Test
    void transcribeWavMissingFileThrows() {
        Path wav = Path.of("/nonexistent/file.wav");
        assertThrows(ParakeetException.class, () -> model.transcribe(wav));
    }

    @Test
    void transcribeWithTimestampsWav(@TempDir Path tempDir) throws IOException {
        Path wav = tempDir.resolve("test.wav");
        Files.write(wav, new byte[] {1, 2, 3});

        fake.jsonToReturn =
                """
                {
                  "text": "Hello",
                  "frame_sec": 0.08,
                  "words": [{"w": "Hello", "start": 0.1, "end": 0.5, "conf": 0.95}],
                  "tokens": [{"id": 10, "t": 0.1, "conf": 0.95}]
                }
                """;

        Transcription t = model.transcribeWithTimestamps(wav, TranscribeOptions.DEFAULT);

        assertNotNull(t);
        assertEquals("Hello", t.text());
        assertEquals(0.08, t.frameSec());
        assertEquals(1, t.words().size());
        assertEquals("Hello", t.words().getFirst().text());
        assertEquals(1, t.tokens().size());
        assertEquals(10, t.tokens().getFirst().id());
    }

    @Test
    void transcribeStreamSessionLifecycle() {
        fake.jsonToReturn =
                """
                {
                  "text": "streamed",
                  "eou": 1,
                  "eob": 0,
                  "frame_sec": 0.08,
                  "events": [{"type": "eou", "frame": 5, "t": 0.4}],
                  "words": [{"w": "streamed", "start": 0.1, "end": 0.4, "conf": 0.99}]
                }
                """;

        TranscribeStream stream = model.startTranscribeStream("en");
        assertNotNull(stream);
        assertEquals("en", fake.lastTargetLang);
        assertFalse(stream.isClosed());

        TranscribeStreamResult feedRes = stream.feed(new float[] {0.1f, 0.2f});
        assertEquals("streamed", feedRes.text());
        assertTrue(feedRes.eou());
        assertFalse(feedRes.eob());
        assertEquals(1, feedRes.events().size());
        assertEquals("eou", feedRes.events().get(0).type());
        assertEquals(1, feedRes.words().size());

        TranscribeStreamResult finRes = stream.finish();
        assertEquals("streamed", finRes.text());

        stream.close();
        assertTrue(stream.isClosed());
        assertEquals(1, fake.freeStreamCount);

        // Subsequent call throws
        assertThrows(IllegalStateException.class, () -> stream.feed(new float[] {0.1f}));
    }

    @Test
    void transcribeStreamFeedSignedPcm() {
        fake.jsonToReturn =
                """
                {
                  "text": "Hello world",
                  "eou": 0,
                  "eob": 0,
                  "frame_sec": 0.08,
                  "events": [],
                  "words": []
                }
                """;

        try (TranscribeStream stream = model.startTranscribeStream("en")) {
            short[] pcm1 = new short[] {-32768, 16384};
            TranscribeStreamResult result = stream.feed(pcm1);
            assertEquals("Hello world", result.text());
            float[] firstBuffer = fake.lastPcm;
            assertNotNull(firstBuffer);
            assertEquals(2, fake.lastNSamples);
            assertEquals(-1.0f, firstBuffer[0], 1e-6f);
            assertEquals(0.5f, firstBuffer[1], 1e-6f);

            // Equal/smaller chunk reuses the same floatBuffer
            short[] pcm2 = new short[] {0};
            stream.feed(pcm2);
            assertSame(firstBuffer, fake.lastPcm);
            assertEquals(1, fake.lastNSamples);
            assertEquals(0.0f, fake.lastPcm[0], 1e-6f);

            // Larger chunk grows floatBuffer
            short[] pcm3 = new short[] {16384, -16384, 0};
            stream.feed(pcm3);
            assertNotSame(firstBuffer, fake.lastPcm);
            assertEquals(3, fake.lastNSamples);
            assertEquals(0.5f, fake.lastPcm[0], 1e-6f);
            assertEquals(-0.5f, fake.lastPcm[1], 1e-6f);
            assertEquals(0.0f, fake.lastPcm[2], 1e-6f);
        }
    }

    @Test
    void detectSpeechFloatPcm(@TempDir Path tempDir) throws IOException {
        Path wav = tempDir.resolve("speech.wav");
        Files.write(wav, new byte[] {1, 2, 3});

        fake.jsonToReturn =
                """
                {
                  "mode": "speech",
                  "duration": 5.0,
                  "frame_sec": 0.08,
                  "backend": "cpu",
                  "segments": [{"start": 0.5, "end": 2.5}],
                  "probabilities": [0.1, 0.9]
                }
                """;

        VadOptions options = VadOptions.builder().threshold(0.6).minPause(0.3).build();
        VadResult vadRes = model.detectSpeech(wav, options);

        assertEquals("speech", vadRes.mode());
        assertEquals(5.0, vadRes.duration());
        assertEquals(1, vadRes.segments().size());
        assertEquals(0.5, vadRes.segments().getFirst().start());
        assertEquals(2.5, vadRes.segments().getFirst().end());
        assertEquals(2, vadRes.probabilities().size());
    }

    @Test
    void detectSpeechSignedPcm() {
        fake.jsonToReturn =
                """
                {
                  "mode": "speech",
                  "duration": 5.0,
                  "frame_sec": 0.08,
                  "backend": "cpu",
                  "segments": [{"start": 0.5, "end": 2.5}],
                  "probabilities": [0.1, 0.9]
                }
                """;

        VadOptions options = VadOptions.builder().threshold(0.6).minPause(0.3).build();
        short[] pcm = new short[] {16384, -16384};

        VadResult detectRes = model.detectSpeech(pcm, 16000, options);
        assertNotNull(detectRes);
        assertEquals("speech", detectRes.mode());
        assertNotNull(fake.lastPcm);
        assertEquals(0.5f, fake.lastPcm[0], 1e-6f);

        fake.jsonToReturn =
                """
                {
                  "mode": "segments",
                  "duration": 5.0,
                  "frame_sec": 0.08,
                  "backend": "cpu",
                  "segments": [{"start": 0.0, "end": 5.0}],
                  "probabilities": []
                }
                """;
        VadResult segmentRes = model.segmentSpeech(pcm, 16000, options);
        assertNotNull(segmentRes);
        assertEquals("segments", segmentRes.mode());
    }

    @Test
    void vadStreamFeedFloatPcmLifecycle(@TempDir Path tempDir) throws IOException {
        Path wav = tempDir.resolve("speech.wav");
        Files.write(wav, new byte[] {1, 2, 3});

        // Streaming VAD
        fake.jsonToReturn =
                """
                {
                  "frame_sec": 0.032,
                  "first_frame": 10,
                  "events": [{"type": "start", "time": 0.32}],
                  "probabilities": [0.8]
                }
                """;

        VadOptions options = VadOptions.builder().threshold(0.6).minPause(0.3).build();
        VadStream vadStream = model.startVadStream(16000, options);
        assertNotNull(vadStream);
        assertFalse(vadStream.isClosed());

        VadStreamResult feedRes = vadStream.feed(new float[] {0.1f}, false);
        assertEquals(0.032, feedRes.frameSec());
        assertEquals(10, feedRes.firstFrame());
        assertEquals(1, feedRes.events().size());
        assertEquals("start", feedRes.events().getFirst().type());

        vadStream.reset();
        vadStream.close();
        assertTrue(vadStream.isClosed());
        assertEquals(1, fake.freeVadStreamCount);
    }

    @Test
    void vadStreamFeedSignedPcm() {
        fake.jsonToReturn =
                """
                {
                  "frame_sec": 0.032,
                  "first_frame": 10,
                  "events": [{"type": "start", "time": 0.32}],
                  "probabilities": [0.8]
                }
                """;

        try (VadStream vadStream = model.startVadStream(16000, VadOptions.DEFAULT)) {
            short[] pcm1 = new short[] {16384, -16384};
            VadStreamResult result = vadStream.feed(pcm1, false);
            assertNotNull(result);
            assertEquals(1, result.events().size());
            float[] firstBuffer = fake.lastPcm;
            assertNotNull(firstBuffer);
            assertEquals(2, fake.lastNSamples);
            assertEquals(0.5f, firstBuffer[0], 1e-6f);
            assertEquals(-0.5f, firstBuffer[1], 1e-6f);

            // Equal/smaller chunk reuses the same floatBuffer
            short[] pcm2 = new short[] {0};
            vadStream.feed(pcm2, true);
            assertSame(firstBuffer, fake.lastPcm);
            assertEquals(1, fake.lastNSamples);
            assertEquals(0.0f, fake.lastPcm[0], 1e-6f);

            // Larger chunk grows floatBuffer
            short[] pcm3 = new short[] {-32768, 0, 16384};
            vadStream.feed(pcm3, false);
            assertNotSame(firstBuffer, fake.lastPcm);
            assertEquals(3, fake.lastNSamples);
            assertEquals(-1.0f, fake.lastPcm[0], 1e-6f);
            assertEquals(0.0f, fake.lastPcm[1], 1e-6f);
            assertEquals(0.5f, fake.lastPcm[2], 1e-6f);
        }
    }

    @Test
    void signedPcmOverloadsNullChecks() {
        assertThrows(NullPointerException.class, () -> model.transcribe((short[]) null, 16000));
        assertThrows(
                NullPointerException.class,
                () -> model.transcribe((short[]) null, 16000, TranscribeOptions.DEFAULT));
        assertThrows(
                NullPointerException.class,
                () -> model.detectSpeech((short[]) null, 16000, VadOptions.DEFAULT));
        assertThrows(
                NullPointerException.class,
                () -> model.segmentSpeech((short[]) null, 16000, VadOptions.DEFAULT));

        try (TranscribeStream stream = model.startTranscribeStream()) {
            assertThrows(NullPointerException.class, () -> stream.feed((short[]) null));
        }
        try (VadStream stream = model.startVadStream(16000, VadOptions.DEFAULT)) {
            assertThrows(NullPointerException.class, () -> stream.feed((short[]) null, false));
        }
    }

    @Test
    void closeModelClosesActiveStreams() {
        TranscribeStream s1 = model.startTranscribeStream();
        VadStream v1 = model.startVadStream(16000, VadOptions.DEFAULT);

        assertFalse(s1.isClosed());
        assertFalse(v1.isClosed());
        assertFalse(model.isClosed());

        model.close();

        assertTrue(model.isClosed());
        assertTrue(s1.isClosed());
        assertTrue(v1.isClosed());
        assertEquals(1, fake.freeStreamCount);
        assertEquals(1, fake.freeVadStreamCount);
        assertEquals(1, fake.freeCtxCount);

        // Repeated close is idempotent
        model.close();
        assertEquals(1, fake.freeCtxCount);

        // Calls on closed model throw
        assertThrows(
                IllegalStateException.class,
                () -> model.transcribe(new float[] {0.1f}, 16000, TranscribeOptions.DEFAULT));
    }

    @Test
    void nativeFailureThrowsParakeetException() {
        fake.shouldFail = true;
        fake.lastError = "Model error occurred";

        ParakeetException ex =
                assertThrows(
                        ParakeetException.class,
                        () ->
                                model.transcribe(
                                        new float[] {0.1f}, 16000, TranscribeOptions.DEFAULT));
        assertTrue(ex.getMessage().contains("Model error occurred"));
    }

    @Test
    void concurrentTranscribeAndClose() throws Exception {
        fake.transcriptToReturn = "Concurrent";
        int threadCount = 8;
        int iterationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger closedRejectionCount = new AtomicInteger();

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(
                        executor.submit(
                                () -> {
                                    try {
                                        startLatch.await();
                                        for (int j = 0; j < iterationsPerThread; j++) {
                                            try {
                                                model.transcribe(
                                                        new float[] {0.1f, 0.2f},
                                                        16000,
                                                        TranscribeOptions.DEFAULT);
                                                successCount.incrementAndGet();
                                            } catch (IllegalStateException e) {
                                                closedRejectionCount.incrementAndGet();
                                            }
                                        }
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                }));
            }

            futures.add(
                    executor.submit(
                            () -> {
                                try {
                                    startLatch.await();
                                    Thread.sleep(5);
                                    model.close();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }));

            startLatch.countDown();
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }

            assertTrue(model.isClosed());
            assertEquals(1, fake.freeCtxCount);
            assertEquals(
                    threadCount * iterationsPerThread,
                    successCount.get() + closedRejectionCount.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentTranscribeStreamFeedAndClose() throws Exception {
        fake.jsonToReturn =
                """
                {
                  "text": "streamed",
                  "eou": 0,
                  "eob": 0,
                  "frame_sec": 0.08,
                  "events": [],
                  "words": []
                }
                """;

        TranscribeStream stream = model.startTranscribeStream();
        int feederThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(feederThreads + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger feedsCompleted = new AtomicInteger();
        AtomicInteger streamClosedRejections = new AtomicInteger();

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < feederThreads; i++) {
                futures.add(
                        executor.submit(
                                () -> {
                                    try {
                                        startLatch.await();
                                        for (int j = 0; j < 50; j++) {
                                            try {
                                                stream.feed(new float[] {0.1f});
                                                feedsCompleted.incrementAndGet();
                                            } catch (IllegalStateException e) {
                                                streamClosedRejections.incrementAndGet();
                                            }
                                        }
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                }));
            }

            futures.add(
                    executor.submit(
                            () -> {
                                try {
                                    startLatch.await();
                                    Thread.sleep(5);
                                    stream.close();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }));

            startLatch.countDown();
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }

            assertTrue(stream.isClosed());
            assertEquals(1, fake.freeStreamCount);
            assertEquals(feederThreads * 50, feedsCompleted.get() + streamClosedRejections.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentVadStreamFeedAndClose() throws Exception {
        fake.jsonToReturn =
                """
                {
                  "frame_sec": 0.032,
                  "first_frame": 0,
                  "events": [],
                  "probabilities": [0.1]
                }
                """;

        VadStream stream = model.startVadStream(16000, VadOptions.DEFAULT);
        int feederThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(feederThreads + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger feedsCompleted = new AtomicInteger();
        AtomicInteger streamClosedRejections = new AtomicInteger();

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < feederThreads; i++) {
                futures.add(
                        executor.submit(
                                () -> {
                                    try {
                                        startLatch.await();
                                        for (int j = 0; j < 50; j++) {
                                            try {
                                                stream.feed(new float[] {0.1f}, false);
                                                feedsCompleted.incrementAndGet();
                                            } catch (IllegalStateException e) {
                                                streamClosedRejections.incrementAndGet();
                                            }
                                        }
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                }));
            }

            futures.add(
                    executor.submit(
                            () -> {
                                try {
                                    startLatch.await();
                                    Thread.sleep(5);
                                    stream.close();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }));

            startLatch.countDown();
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }

            assertTrue(stream.isClosed());
            assertEquals(1, fake.freeVadStreamCount);
            assertEquals(feederThreads * 50, feedsCompleted.get() + streamClosedRejections.get());
        } finally {
            executor.shutdownNow();
        }
    }
}

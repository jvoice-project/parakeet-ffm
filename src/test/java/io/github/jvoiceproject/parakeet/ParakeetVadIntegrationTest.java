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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodType;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assumptions;
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

    @Test
    void vadWithGermanLocale() throws Exception {
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = linker.defaultLookup();
        Optional<MemorySegment> setlocaleOpt = lookup.find("setlocale");
        if (setlocaleOpt.isEmpty()) {
            throw new AssertionError("setlocale symbol should be found");
        }
        MemorySegment symbol = setlocaleOpt.get();

        MethodHandle setlocale =
                linker.downcallHandle(
                                symbol,
                                FunctionDescriptor.of(
                                        ValueLayout.ADDRESS,
                                        ValueLayout.JAVA_INT,
                                        ValueLayout.ADDRESS))
                        .asType(
                                MethodType.methodType(
                                        MemorySegment.class, Integer.class, MemorySegment.class));
        @SuppressWarnings("unchecked")
        BiFunction<Integer, MemorySegment, MemorySegment> setLocaleFn =
                MethodHandleProxies.asInterfaceInstance(BiFunction.class, setlocale);

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        int lcNumeric = os.contains("mac") || os.contains("darwin") || os.contains("win") ? 4 : 1;
        int lcAll = os.contains("mac") || os.contains("darwin") || os.contains("win") ? 0 : 6;

        MemorySegment origLcNumeric = setLocaleFn.apply(lcNumeric, MemorySegment.NULL);
        String origNumeric =
                (origLcNumeric != null
                                && !origLcNumeric.equals(MemorySegment.NULL)
                                && origLcNumeric.address() != 0)
                        ? origLcNumeric.reinterpret(Long.MAX_VALUE).getUtf8String(0)
                        : null;

        MemorySegment origLcAll = setLocaleFn.apply(lcAll, MemorySegment.NULL);
        String origAll =
                (origLcAll != null
                                && !origLcAll.equals(MemorySegment.NULL)
                                && origLcAll.address() != 0)
                        ? origLcAll.reinterpret(Long.MAX_VALUE).getUtf8String(0)
                        : null;

        try (Arena arena = Arena.ofConfined()) {
            try {
                MemorySegment deLocale = arena.allocateUtf8String("de_DE.UTF-8");
                MemorySegment resNumeric = setLocaleFn.apply(lcNumeric, deLocale);
                MemorySegment resAll = setLocaleFn.apply(lcAll, deLocale);

                boolean deLocaleSet =
                        (resNumeric != null
                                        && !resNumeric.equals(MemorySegment.NULL)
                                        && resNumeric.address() != 0)
                                || (resAll != null
                                        && !resAll.equals(MemorySegment.NULL)
                                        && resAll.address() != 0);

                Assumptions.assumeTrue(
                        deLocaleSet, "de_DE.UTF-8 locale is not available on this system");

                try (ParakeetModel vad = Parakeet.loadModel(vadModelPath)) {
                    assertEquals(ModelKind.VAD, vad.kind());

                    VadOptions options =
                            VadOptions.builder()
                                    .threshold(0.5)
                                    .minPause(0.3)
                                    .minSpeech(0.2)
                                    .speechPad(0.1)
                                    .build();

                    VadResult res = vad.detectSpeech(audioPath, options);
                    assertNotNull(res);
                    assertEquals("speech", res.mode());
                    assertTrue(res.duration() > 0.0);
                    assertFalse(res.segments().isEmpty(), "Should detect speech in audio file");

                    for (VadSegment seg : res.segments()) {
                        assertTrue(seg.start() >= 0.0);
                        assertTrue(seg.end() <= res.duration() + 0.1);
                        assertTrue(seg.start() < seg.end());
                    }

                    float[] pcm = readWavMono16k(audioPath);
                    try (VadStream vs = vad.startVadStream(16000, options)) {
                        int chunkSize = 1600; // 100 ms
                        int eventCount = 0;
                        for (int offset = 0; offset < pcm.length; offset += chunkSize) {
                            int len = Math.min(chunkSize, pcm.length - offset);
                            boolean isLast = (offset + len >= pcm.length);
                            float[] chunk = new float[len];
                            System.arraycopy(pcm, offset, chunk, 0, len);
                            VadStreamResult sRes = vs.feed(chunk, isLast);
                            assertNotNull(sRes);
                            assertTrue(sRes.frameSec() > 0.0);
                            eventCount += sRes.events().size();
                        }
                        assertTrue(
                                eventCount > 0,
                                "VAD stream should emit speech start/end" + " events");
                    }
                }
            } finally {
                if (origAll != null) {
                    setLocaleFn.apply(lcAll, arena.allocateUtf8String(origAll));
                }
                if (origNumeric != null) {
                    setLocaleFn.apply(lcNumeric, arena.allocateUtf8String(origNumeric));
                }
            }
        }
    }
}

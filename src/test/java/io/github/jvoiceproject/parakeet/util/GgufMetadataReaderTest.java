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
package io.github.jvoiceproject.parakeet.util;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link GgufMetadataReader}.
 *
 * @author Florian Hotze - Initial contribution
 */
class GgufMetadataReaderTest {

    private @TempDir Path tempDir;
    private GgufMetadataReader reader;

    @BeforeEach
    void setUp() {
        reader = new GgufMetadataReader();
    }

    private static byte[] createGgufHeader(int version, List<KvPair> pairs) {
        ByteBuffer buf = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
        // Magic
        buf.putInt(0x46554747);
        // Version
        buf.putInt(version);
        // Tensor count
        if (version == 1) {
            buf.putInt(0);
            buf.putInt(pairs.size());
        } else {
            buf.putLong(0L);
            buf.putLong(pairs.size());
        }

        for (KvPair pair : pairs) {
            byte[] keyBytes = pair.key.getBytes(StandardCharsets.UTF_8);
            if (version == 1) {
                buf.putInt(keyBytes.length);
            } else {
                buf.putLong(keyBytes.length);
            }
            buf.put(keyBytes);
            pair.write(buf);
        }

        byte[] result = new byte[buf.position()];
        buf.flip();
        buf.get(result);
        return result;
    }

    private record KvPair(String key, int type, Object value) {
        void write(ByteBuffer buf) {
            buf.putInt(type);
            switch (type) {
                case 4 -> buf.putInt((Integer) value); // UINT32
                case 7 -> buf.put((byte) (((Boolean) value) ? 1 : 0)); // BOOL
                case 8 -> { // STRING
                    byte[] strBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                    buf.putLong(strBytes.length);
                    buf.put(strBytes);
                }
                case 9 -> { // ARRAY
                    @SuppressWarnings("unchecked")
                    List<String> list = (List<String>) value;
                    buf.putInt(8); // TYPE_STRING
                    buf.putLong(list.size());
                    for (String str : list) {
                        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
                        buf.putLong(strBytes.length);
                        buf.put(strBytes);
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported test type: " + type);
            }
        }
    }

    @Test
    void parsesValidGgufV3Metadata() throws IOException {
        byte[] ggufBytes =
                createGgufHeader(
                        3,
                        List.of( //
                                new KvPair("general.architecture", 8, "parakeet"), //
                                new KvPair("general.name", 8, "nvidia/parakeet-tdt-0.6b"), //
                                new KvPair("general.languages", 9, List.of("en", "de", "fr")), //
                                new KvPair("parakeet.arch", 8, "tdt"), //
                                new KvPair("custom.other_int", 4, 42), //
                                new KvPair("custom.other_bool", 7, true) //
                                ));

        GgufMetadata metadata = reader.parse(new ByteArrayInputStream(ggufBytes));
        assertNotNull(metadata);
        assertEquals("nvidia/parakeet-tdt-0.6b", metadata.name());
        assertEquals("parakeet", metadata.architecture());
        assertEquals(List.of("en", "de", "fr"), metadata.languages());
        assertEquals("tdt", metadata.parakeetArch());
        assertTrue(metadata.isAsr());
        assertFalse(metadata.isVadOnly());
        assertFalse(metadata.isBundle());
    }

    @Test
    void parsesValidSileroVadModelMetadata() throws IOException {
        byte[] ggufBytes =
                createGgufHeader(
                        3,
                        List.of( //
                                new KvPair("general.architecture", 8, "silero_vad"), //
                                new KvPair("general.name", 8, "Silero VAD v4") //
                                ));

        GgufMetadata metadata = reader.parse(new ByteArrayInputStream(ggufBytes));
        assertNotNull(metadata);
        assertEquals("Silero VAD v4", metadata.name());
        assertEquals("silero_vad", metadata.architecture());
        assertTrue(metadata.isVadOnly());
        assertFalse(metadata.isAsr());
        assertFalse(metadata.isBundle());
    }

    @Test
    void parsesVadOnlyParakeetModelSliceMetadata() throws IOException {
        byte[] ggufBytes =
                createGgufHeader(
                        3,
                        List.of( //
                                new KvPair("general.architecture", 8, "parakeet"), //
                                new KvPair("general.name", 8, "Parakeet VAD-only Slice"), //
                                new KvPair("parakeet.arch", 8, "vad") //
                                ));

        GgufMetadata metadata = reader.parse(new ByteArrayInputStream(ggufBytes));
        assertNotNull(metadata);
        assertEquals("Parakeet VAD-only Slice", metadata.name());
        assertEquals("parakeet", metadata.architecture());
        assertEquals("vad", metadata.parakeetArch());
        assertTrue(metadata.isVadOnly());
        assertFalse(metadata.isAsr());
        assertFalse(metadata.isBundle());
    }

    @Test
    void throwsIOExceptionOnInvalidMagicOrTruncated() {
        assertThrows(
                IOException.class,
                () -> reader.parse(new ByteArrayInputStream(new byte[] {0, 1, 2, 3})));
        assertThrows(
                IOException.class, () -> reader.parse(new ByteArrayInputStream(new byte[] {0})));

        // Truncated header
        byte[] truncated = new byte[] {0x47, 0x47, 0x55, 0x46, 3, 0, 0, 0};
        assertThrows(IOException.class, () -> reader.parse(new ByteArrayInputStream(truncated)));
    }

    @Test
    void readsFromFileAndCaches() throws IOException {
        Path file = tempDir.resolve("test-model.gguf");
        byte[] ggufBytes =
                createGgufHeader(
                        3,
                        List.of( //
                                new KvPair("general.name", 8, "Test Model"), //
                                new KvPair("general.architecture", 8, "parakeet") //
                                ));
        Files.write(file, ggufBytes);

        GgufMetadata meta1 = reader.read(file);
        assertNotNull(meta1);
        assertEquals("Test Model", meta1.name());

        // Cache hit
        GgufMetadata meta2 = reader.read(file);
        assertNotNull(meta2);
        assertSame(meta1, meta2);

        // Modify file
        byte[] updatedBytes =
                createGgufHeader(
                        3,
                        List.of( //
                                new KvPair("general.name", 8, "Updated Model"), //
                                new KvPair("general.architecture", 8, "parakeet") //
                                ));
        Files.write(file, updatedBytes);
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() + 1000));

        GgufMetadata meta3 = reader.read(file);
        assertNotNull(meta3);
        assertEquals("Updated Model", meta3.name());
    }

    @Test
    void throwsFileNotFoundExceptionOnNonExistentFile() {
        assertThrows(
                FileNotFoundException.class, () -> reader.read(tempDir.resolve("missing.gguf")));
    }
}

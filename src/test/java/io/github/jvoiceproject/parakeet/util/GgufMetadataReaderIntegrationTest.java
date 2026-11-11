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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link GgufMetadataReader} testing against real GGUF model files.
 *
 * @author Florian Hotze - Initial contribution
 */
@Tag("integration")
class GgufMetadataReaderIntegrationTest {

    private static Path modelPath;
    private static Path streamModelPath;
    private static Path vadModelPath;

    private GgufMetadataReader reader;

    @BeforeAll
    static void setUpModels() {
        String modelEnv = System.getenv("PARAKEET_MODEL");
        String streamEnv = System.getenv("PARAKEET_STREAM_MODEL");
        String vadEnv = System.getenv("PARAKEET_VAD_MODEL");

        assertNotNull(modelEnv, "PARAKEET_MODEL environment variable must be set");
        assertNotNull(streamEnv, "PARAKEET_STREAM_MODEL environment variable must be set");
        assertNotNull(vadEnv, "PARAKEET_VAD_MODEL environment variable must be set");

        modelPath = Path.of(modelEnv);
        streamModelPath = Path.of(streamEnv);
        vadModelPath = Path.of(vadEnv);

        assertTrue(Files.exists(modelPath), "Model file must exist at " + modelPath);
        assertTrue(
                Files.exists(streamModelPath),
                "Stream model file must exist at " + streamModelPath);
        assertTrue(Files.exists(vadModelPath), "VAD model file must exist at " + vadModelPath);
    }

    @BeforeEach
    void setUp() {
        reader = new GgufMetadataReader();
    }

    @Test
    void readOfflineParakeetModel() throws Exception {
        GgufMetadata metadata = reader.read(modelPath);

        assertNotNull(metadata);
        assertEquals("nvidia/parakeet-tdt_ctc-110m", metadata.name());
        assertEquals("parakeet", metadata.architecture());
        assertEquals("hybrid_tdt_ctc", metadata.parakeetArch());
        assertTrue(metadata.isAsr());
        assertFalse(metadata.isVadOnly());
        assertFalse(metadata.isBundle());
        assertNotNull(metadata.languages());
        assertTrue(metadata.languages().isEmpty());
    }

    @Test
    void readStreamingParakeetModel() throws Exception {
        GgufMetadata metadata = reader.read(streamModelPath);

        assertNotNull(metadata);
        assertEquals("nvidia/parakeet_realtime_eou_120m-v1", metadata.name());
        assertEquals("parakeet", metadata.architecture());
        assertEquals("rnnt", metadata.parakeetArch());
        assertTrue(metadata.isAsr());
        assertFalse(metadata.isVadOnly());
        assertFalse(metadata.isBundle());
        assertNotNull(metadata.languages());
        assertTrue(metadata.languages().isEmpty());
    }

    @Test
    void readSileroVadModel() throws Exception {
        GgufMetadata metadata = reader.read(vadModelPath);

        assertNotNull(metadata);
        assertEquals("silero-vad", metadata.name());
        assertEquals("silero_vad", metadata.architecture());
        assertNull(metadata.parakeetArch());
        assertFalse(metadata.isAsr());
        assertTrue(metadata.isVadOnly());
        assertFalse(metadata.isBundle());
        assertNotNull(metadata.languages());
        assertTrue(metadata.languages().isEmpty());
    }

    @Test
    void cachingWithRealModel() throws Exception {
        GgufMetadata meta1 = reader.read(modelPath);
        GgufMetadata meta2 = reader.read(modelPath);

        assertNotNull(meta1);
        assertSame(meta1, meta2);
    }

    @Test
    void parseFromInputStreamWithRealModel() throws Exception {
        try (InputStream in = Files.newInputStream(modelPath)) {
            GgufMetadata metadata = reader.parse(in);

            assertNotNull(metadata);
            assertEquals("nvidia/parakeet-tdt_ctc-110m", metadata.name());
            assertEquals("parakeet", metadata.architecture());
            assertEquals("hybrid_tdt_ctc", metadata.parakeetArch());
            assertTrue(metadata.isAsr());
            assertFalse(metadata.isBundle());
        }
    }
}

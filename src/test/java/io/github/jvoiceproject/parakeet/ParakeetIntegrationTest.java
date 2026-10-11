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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Abstract base class for Parakeet integration tests, providing common setup and helpers.
 *
 * @author Florian Hotze - Initial contribution
 */
@Tag("integration")
public abstract class ParakeetIntegrationTest {

    protected static Path vadModelPath;
    protected static Path audioPath;

    @BeforeAll
    static void setUpBase() {
        String vadEnv = System.getenv("PARAKEET_VAD_MODEL");
        String audioEnv = System.getenv("TEST_AUDIO");

        assertNotNull(vadEnv, "PARAKEET_VAD_MODEL environment variable must be set");
        assertNotNull(audioEnv, "TEST_AUDIO environment variable must be set");

        vadModelPath = Path.of(vadEnv);
        audioPath = Path.of(audioEnv);

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

    protected static float[] readWavMono16k(Path wavPath) throws Exception {
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

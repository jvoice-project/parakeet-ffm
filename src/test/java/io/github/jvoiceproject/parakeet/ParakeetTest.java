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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link Parakeet} library loading, lifecycle management, and entry-point methods.
 *
 * @author Florian Hotze - Initial contribution
 */
public class ParakeetTest {

    private FakeParakeetBinding fake;

    @BeforeEach
    void setUp() {
        fake = new FakeParakeetBinding();
    }

    @AfterEach
    void tearDown() {
        Parakeet.setInstance(null);
    }

    @Test
    void expectedAbiVersionConstantIsTen() {
        assertEquals(10, Parakeet.EXPECTED_ABI_VERSION);
    }

    @Test
    void isLibraryLoadedReturnsFalseInitially() {
        Parakeet.setInstance(null);
        assertFalse(Parakeet.isLibraryLoaded());
    }

    @Test
    void setInstanceValidatesAbiVersion() {
        fake.abiVersion = 10;
        Parakeet.setInstance(fake);
        assertTrue(Parakeet.isLibraryLoaded());
        assertEquals(10, Parakeet.abiVersion());

        fake.abiVersion = 8;
        assertThrows(LinkageError.class, () -> Parakeet.setInstance(fake));
    }

    @Test
    void setInstanceNullClearsInstance() throws IOException {
        fake.abiVersion = 10;
        Parakeet.setInstance(fake);
        Parakeet.setInstance(null);
        assertFalse(Parakeet.isLibraryLoaded());

        // After clearing, loadLibrary() will reload the bundled library
        Parakeet.loadLibrary();
        assertTrue(Parakeet.isLibraryLoaded());
        assertEquals(10, Parakeet.abiVersion());
    }

    @Test
    void parakeetMethodsThrowIllegalStateExceptionWhenLibraryNotLoaded() {
        Parakeet.setInstance(null);
        assertThrows(IllegalStateException.class, Parakeet::version);
        assertThrows(IllegalStateException.class, Parakeet::abiVersion);
        assertThrows(IllegalStateException.class, () -> Parakeet.loadModel(Path.of("model.gguf")));
        assertThrows(
                IllegalStateException.class,
                () -> Parakeet.loadModel(Path.of("model.gguf"), "asr"));
        assertThrows(
                IllegalStateException.class,
                () -> Parakeet.bundleComponents(Path.of("model.gguf")));
    }

    @Test
    void parakeetLoadLibrarySetsLibraryLoaded() throws IOException {
        Parakeet.setInstance(null);
        Parakeet.loadLibrary();
        assertTrue(Parakeet.isLibraryLoaded());
        assertNotNull(Parakeet.version());
    }

    @Test
    void loadModelWithInvalidGgufFileThrows(@TempDir Path tempDir) throws IOException {
        fake.abiVersion = 10;
        Parakeet.setInstance(fake);

        Path invalidFile = tempDir.resolve("invalid.gguf");
        Files.write(invalidFile, new byte[] {1, 2, 3, 4, 5, 6, 7, 8});

        ParakeetException ex =
                assertThrows(ParakeetException.class, () -> Parakeet.loadModel(invalidFile));
        assertTrue(ex.getMessage().contains("Failed to read GGUF metadata"));
    }

    @Test
    void loadModelWithUnsupportedArchitectureThrows(@TempDir Path tempDir) throws IOException {
        fake.abiVersion = 10;
        Parakeet.setInstance(fake);

        Path llamaFile = tempDir.resolve("llama.gguf");
        Files.write(llamaFile, createSimpleGguf("llama"));

        ParakeetException ex =
                assertThrows(ParakeetException.class, () -> Parakeet.loadModel(llamaFile));
        assertTrue(ex.getMessage().contains("Unsupported GGUF model architecture 'llama'"));
    }

    @Test
    void loadModelBundleComponentRejectsNonBundleFile(@TempDir Path tempDir) throws IOException {
        fake.abiVersion = 10;
        Parakeet.setInstance(fake);

        Path asrFile = tempDir.resolve("parakeet.gguf");
        Files.write(asrFile, createSimpleGguf("parakeet"));

        ParakeetException ex =
                assertThrows(ParakeetException.class, () -> Parakeet.loadModel(asrFile, "asr"));
        assertTrue(ex.getMessage().contains("Model file is not a GGUF bundle"));
    }

    @Test
    void bundleComponentsRejectsNonBundleFile(@TempDir Path tempDir) throws IOException {
        fake.abiVersion = 10;
        Parakeet.setInstance(fake);

        Path asrFile = tempDir.resolve("parakeet.gguf");
        Files.write(asrFile, createSimpleGguf("parakeet"));

        ParakeetException ex =
                assertThrows(ParakeetException.class, () -> Parakeet.bundleComponents(asrFile));
        assertTrue(ex.getMessage().contains("Model file is not a GGUF bundle"));
    }

    @Test
    void loadModelWithValidBundleFile(@TempDir Path tempDir) throws IOException {
        fake.abiVersion = 10;
        fake.jsonToReturn =
                """
                [{"name": "asr", "kind": "asr"}]
                """;
        Parakeet.setInstance(fake);

        Path bundleFile = tempDir.resolve("test-bundle.gguf");
        Files.write(bundleFile, createSimpleGguf("parakeet-bundle"));

        // loadModel(Path) accepts bundles (default component)
        try (ParakeetModel model = Parakeet.loadModel(bundleFile)) {
            assertNotNull(model);
        }

        // loadModel(Path, String) accepts bundles
        try (ParakeetModel model = Parakeet.loadModel(bundleFile, "asr")) {
            assertNotNull(model);
        }

        // bundleComponents(Path) accepts bundles
        List<BundleComponent> components = Parakeet.bundleComponents(bundleFile);
        assertEquals(1, components.size());
        assertEquals("asr", components.getFirst().name());
    }

    private static byte[] createSimpleGguf(String architecture) {
        ByteBuffer buf = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(0x46554747); // GGUF magic
        buf.putInt(3); // version 3
        buf.putLong(0L); // tensor count
        buf.putLong(1L); // kv count = 1
        byte[] keyBytes = "general.architecture".getBytes(StandardCharsets.UTF_8);
        buf.putLong(keyBytes.length);
        buf.put(keyBytes);
        buf.putInt(8); // type STRING
        byte[] valBytes = architecture.getBytes(StandardCharsets.UTF_8);
        buf.putLong(valBytes.length);
        buf.put(valBytes);
        byte[] res = new byte[buf.position()];
        buf.flip();
        buf.get(res);
        return res;
    }
}

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
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}

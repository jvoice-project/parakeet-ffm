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

import io.github.jvoiceproject.parakeet.internal.NativeLibraryLoader;
import io.github.jvoiceproject.parakeet.internal.ParakeetBinding;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NativeLibraryLoader} ABI version validation and configuration.
 *
 * @author Florian Hotze - Initial contribution
 */
public class NativeLibraryLoaderTest {

    private FakeParakeetBinding fake;

    @BeforeEach
    void setUp() {
        fake = new FakeParakeetBinding();
    }

    @AfterEach
    void tearDown() {
        NativeLibraryLoader.setInstance(null);
    }

    @Test
    void expectedAbiVersionConstantIsTen() {
        assertEquals(10, Parakeet.EXPECTED_ABI_VERSION);
        assertEquals(10, ParakeetBinding.EXPECTED_ABI_VERSION);
    }

    @Test
    void validateAbiVersionSucceedsWithExpectedVersion() {
        fake.abiVersion = 10;
        assertDoesNotThrow(() -> NativeLibraryLoader.validateAbiVersion(fake, "test-lib"));
    }

    @Test
    void validateAbiVersionFailsWithOutdatedVersion() {
        fake.abiVersion = 9;
        ParakeetException ex =
                assertThrows(
                        ParakeetException.class,
                        () ->
                                NativeLibraryLoader.validateAbiVersion(
                                        fake, "/path/to/libparakeet.so"));
        assertTrue(
                ex.getMessage().contains("Incompatible parakeet native library ABI version"),
                "Message should state incompatibility: " + ex.getMessage());
        assertTrue(
                ex.getMessage().contains("expected 10, got 9"),
                "Message should specify expected and actual version: " + ex.getMessage());
        assertTrue(
                ex.getMessage().contains("/path/to/libparakeet.so"),
                "Message should specify the library source: " + ex.getMessage());
    }

    @Test
    void validateAbiVersionFailsWithNewerVersion() {
        fake.abiVersion = 11;
        ParakeetException ex =
                assertThrows(
                        ParakeetException.class,
                        () -> NativeLibraryLoader.validateAbiVersion(fake, "custom.so"));
        assertTrue(ex.getMessage().contains("expected 10, got 11"));
    }

    @Test
    void validateAbiVersionFailsWhenQueryThrows() {
        ParakeetBinding failingBinding =
                new FakeParakeetBinding() {
                    @Override
                    public int parakeet_capi_abi_version() {
                        throw new RuntimeException("Crash reading symbol");
                    }
                };

        ParakeetException ex =
                assertThrows(
                        ParakeetException.class,
                        () -> NativeLibraryLoader.validateAbiVersion(failingBinding, "corrupt.so"));
        assertTrue(ex.getMessage().contains("Failed to query parakeet ABI version"));
    }

    @Test
    void setInstanceValidatesAbiVersion() {
        fake.abiVersion = 10;
        NativeLibraryLoader.setInstance(fake);
        assertEquals(10, Parakeet.abiVersion());

        fake.abiVersion = 8;
        assertThrows(ParakeetException.class, () -> NativeLibraryLoader.setInstance(fake));
    }

    @Test
    void setInstanceNullClearsInstance() {
        fake.abiVersion = 10;
        NativeLibraryLoader.setInstance(fake);
        NativeLibraryLoader.setInstance(null);
        // After clearing, getInstance() will reload the bundled library
        ParakeetBinding reloaded = NativeLibraryLoader.getInstance();
        assertNotNull(reloaded);
        assertEquals(10, reloaded.parakeet_capi_abi_version());
    }
}

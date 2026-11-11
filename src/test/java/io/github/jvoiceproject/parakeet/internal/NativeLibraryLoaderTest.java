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
package io.github.jvoiceproject.parakeet.internal;

import io.github.jvoiceproject.parakeet.FakeParakeetBinding;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NativeLibraryLoader} ABI version validation, loading, and configuration.
 *
 * @author Florian Hotze - Initial contribution
 */
public class NativeLibraryLoaderTest {

    private FakeParakeetBinding fake;

    @BeforeEach
    void setUp() {
        fake = new FakeParakeetBinding();
    }

    @Test
    void validateAbiVersionSucceedsWithExpectedVersion() {
        fake.abiVersion = 10;
        assertDoesNotThrow(() -> NativeLibraryLoader.validateAbiVersion(fake, "test-lib"));
    }

    @Test
    void validateAbiVersionFailsWithOutdatedVersion() {
        fake.abiVersion = 9;
        LinkageError ex =
                assertThrows(
                        LinkageError.class,
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
        LinkageError ex =
                assertThrows(
                        LinkageError.class,
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

        LinkageError ex =
                assertThrows(
                        LinkageError.class,
                        () -> NativeLibraryLoader.validateAbiVersion(failingBinding, "corrupt.so"));
        assertTrue(ex.getMessage().contains("Failed to query parakeet ABI version"));
    }

    @Test
    void loadSucceedsWithBundledLibrary() throws IOException {
        ParakeetBinding binding = NativeLibraryLoader.load();
        assertNotNull(binding);
        assertEquals(ParakeetBinding.EXPECTED_ABI_VERSION, binding.parakeet_capi_abi_version());
    }

    @Test
    void loadFailsWithUnsupportedOperationExceptionOnUnsupportedOs() {
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Solaris");
            UnsupportedOperationException ex =
                    assertThrows(UnsupportedOperationException.class, NativeLibraryLoader::load);
            assertTrue(ex.getMessage().contains("Unsupported operating system: solaris"));
        } finally {
            System.setProperty("os.name", originalOs);
        }
    }

    @Test
    void loadFailsWithUnsupportedOperationExceptionOnUnsupportedArch() {
        String originalArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.arch", "mips");
            UnsupportedOperationException ex =
                    assertThrows(UnsupportedOperationException.class, NativeLibraryLoader::load);
            assertTrue(ex.getMessage().contains("Unsupported architecture: mips"));
        } finally {
            System.setProperty("os.arch", originalArch);
        }
    }

    @Test
    void extractFromResourcesFailsWithFileNotFoundExceptionWhenMissingOnClasspath() {
        FileNotFoundException ex =
                assertThrows(
                        FileNotFoundException.class,
                        () ->
                                NativeLibraryLoader.extractFromResources(
                                        "/native/missing/libparakeet.so", ".so"));
        assertTrue(ex.getMessage().contains("Bundled native library not found on classpath"));
        assertTrue(ex.getMessage().contains("/native/missing/libparakeet.so"));
    }

    @Test
    void loadFailsWithUnsatisfiedLinkErrorOnCorruptLibrary() throws IOException {
        Path tempFile = Files.createTempFile("corrupt-lib", ".so");
        Files.writeString(tempFile, "not a valid shared library");
        String originalProp = System.getProperty("parakeet.library.path");
        try {
            System.setProperty("parakeet.library.path", tempFile.toAbsolutePath().toString());
            LinkageError ex = assertThrows(LinkageError.class, NativeLibraryLoader::load);
            assertTrue(ex.getMessage().contains("Failed to link native parakeet library"));
        } finally {
            if (originalProp != null) {
                System.setProperty("parakeet.library.path", originalProp);
            } else {
                System.clearProperty("parakeet.library.path");
            }
            Files.deleteIfExists(tempFile);
        }
    }
}

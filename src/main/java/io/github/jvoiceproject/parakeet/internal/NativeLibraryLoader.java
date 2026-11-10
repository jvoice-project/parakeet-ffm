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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Loads and provides the native {@link ParakeetBinding} instance using Java FFM.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class NativeLibraryLoader {
    private static final String PLATFORM_RESOURCE_PATH_PATTERN = "/native/%s-%s/%sparakeet%s";

    private NativeLibraryLoader() {}

    /**
     * Load the native library.
     *
     * @return the loaded native library binding
     * @throws IOException if the library cannot be located/found
     * @throws UnsupportedOperationException if the operating system or architecture is not
     *     supported
     * @throws LinkageError if the library cannot be loaded (linked)
     */
    public static ParakeetBinding load() throws IOException, UnsupportedOperationException {
        Path libraryPath = locateLibrary();
        try {
            SymbolLookup lookup = SymbolLookup.libraryLookup(libraryPath, Arena.global());
            ParakeetBinding binding = new FfmParakeetBinding(lookup);
            validateAbiVersion(binding, libraryPath.toString());
            return binding;
        } catch (LinkageError le) {
            throw le;
        } catch (Throwable t) {
            UnsatisfiedLinkError error =
                    new UnsatisfiedLinkError(
                            "Failed to load native parakeet library from '"
                                    + libraryPath
                                    + "': "
                                    + t.getMessage());
            error.initCause(t);
            throw error;
        }
    }

    /**
     * Validates the ABI version of the loaded native library against the expected ABI version.
     *
     * @param binding the native library binding
     * @param source the source of the native library (e.g., path, class name)
     * @throws LinkageError if the ABI version cannot be read or ABI version mismatch occurs
     */
    public static void validateAbiVersion(ParakeetBinding binding, String source) {
        int actualAbi;
        try {
            actualAbi = binding.parakeet_capi_abi_version();
        } catch (Throwable t) {
            throw new LinkageError(
                    "Failed to query parakeet ABI version from '" + source + "': " + t.getMessage(),
                    t);
        }
        if (actualAbi != ParakeetBinding.EXPECTED_ABI_VERSION) {
            throw new LinkageError(
                    String.format(
                            "Incompatible parakeet native library ABI version from '%s': expected"
                                    + " %d, got %d",
                            source, ParakeetBinding.EXPECTED_ABI_VERSION, actualAbi));
        }
    }

    /**
     * Locates the native library.
     *
     * @return the path to the native library
     * @throws IOException if the library cannot be located/found
     * @throws UnsupportedOperationException if the operating system or architecture is not
     *     supported
     */
    private static Path locateLibrary() throws IOException, UnsupportedOperationException {
        String customPath = System.getProperty("parakeet.library.path");
        if (customPath != null) {
            Path p = Path.of(customPath);
            if (Files.isRegularFile(p)) {
                return p.toAbsolutePath();
            }
            if (Files.isDirectory(p)) {
                String libFileName = getLibraryFileName();
                Path direct = p.resolve(libFileName);
                if (Files.isRegularFile(direct)) {
                    return direct.toAbsolutePath();
                }
            }
        }

        return extractFromResources();
    }

    /**
     * Extracts the native library from the resources.
     *
     * @return the path to the extracted native library
     * @throws IOException if the library cannot be extracted
     * @throws UnsupportedOperationException if the operating system or architecture is not
     *     supported
     */
    private static Path extractFromResources() throws IOException, UnsupportedOperationException {
        String resourcePath = getPlatformResourcePath();
        try (InputStream is = NativeLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException(
                        "Bundled native library not found on classpath: "
                                + resourcePath
                                + "\n"
                                + "Build the native library first or specify"
                                + " -Dparakeet.library.path=...");
            }

            Path target = Files.createTempFile("parakeet-", getLibraryExtension());
            try (OutputStream os = Files.newOutputStream(target)) {
                is.transferTo(os);
            }

            target.toFile().deleteOnExit();
            return target.toAbsolutePath();
        }
    }

    /**
     * Gets the platform-specific resource path for the native library.
     *
     * @return the platform-specific resource path
     * @throws UnsupportedOperationException if the operating system or architecture is not
     *     supported
     */
    private static String getPlatformResourcePath() throws UnsupportedOperationException {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        String osPrefix;
        String libPrefix;

        if (os.contains("nux") || os.contains("nix") || os.contains("aix")) {
            osPrefix = "linux";
            libPrefix = "lib";
        } else if (os.contains("mac") || os.contains("darwin")) {
            osPrefix = "macos";
            libPrefix = "lib";
        } else if (os.contains("win")) {
            osPrefix = "windows";
            libPrefix = "";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + os);
        }

        String archSuffix;
        if (arch.equals("x86_64") || arch.equals("amd64") || arch.equals("x86-64")) {
            archSuffix = "x86-64";
        } else if (arch.equals("aarch64") || arch.equals("arm64")) {
            archSuffix = "aarch64";
        } else {
            throw new UnsupportedOperationException("Unsupported architecture: " + arch);
        }

        return PLATFORM_RESOURCE_PATH_PATTERN.formatted(
                osPrefix, archSuffix, libPrefix, getLibraryExtension());
    }

    private static String getLibraryFileName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String prefix = os.contains("win") ? "" : "lib";
        return prefix + "parakeet" + getLibraryExtension();
    }

    private static String getLibraryExtension() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return ".dll";
        } else if (os.contains("mac") || os.contains("darwin")) {
            return ".dylib";
        } else {
            return ".so";
        }
    }
}

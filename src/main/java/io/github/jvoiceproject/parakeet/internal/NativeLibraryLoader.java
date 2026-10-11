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

import io.github.jvoiceproject.parakeet.ParakeetException;

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
    private static final String PLATFORM_RESOURCE_PATH_PATTERN = "/%s-%s/%sparakeet%s";

    private static volatile ParakeetBinding instance;

    private NativeLibraryLoader() {}

    public static ParakeetBinding getInstance() {
        ParakeetBinding binding = instance;
        if (binding == null) {
            synchronized (NativeLibraryLoader.class) {
                binding = instance;
                if (binding == null) {
                    binding = load();
                    instance = binding;
                }
            }
        }
        return binding;
    }

    public static void setInstance(ParakeetBinding binding) {
        if (binding != null) {
            validateAbiVersion(binding, "custom binding");
        }
        synchronized (NativeLibraryLoader.class) {
            instance = binding;
        }
    }

    private static ParakeetBinding load() {
        Path libraryPath = locateLibrary();
        try {
            SymbolLookup lookup = SymbolLookup.libraryLookup(libraryPath, Arena.global());
            ParakeetBinding binding = new FfmParakeetBinding(lookup);
            validateAbiVersion(binding, libraryPath);
            return binding;
        } catch (ParakeetException pe) {
            throw pe;
        } catch (Throwable t) {
            throw new ParakeetException(
                    "Failed to load native parakeet library from '"
                            + libraryPath
                            + "': "
                            + t.getMessage(),
                    t);
        }
    }

    public static void validateAbiVersion(ParakeetBinding binding, Object source) {
        int actualAbi;
        try {
            actualAbi = binding.parakeet_capi_abi_version();
        } catch (Throwable t) {
            throw new ParakeetException(
                    "Failed to query parakeet ABI version from '" + source + "': " + t.getMessage(),
                    t);
        }
        if (actualAbi != ParakeetBinding.EXPECTED_ABI_VERSION) {
            throw new ParakeetException(
                    String.format(
                            "Incompatible parakeet native library ABI version from '%s': expected"
                                    + " %d, got %d",
                            source, ParakeetBinding.EXPECTED_ABI_VERSION, actualAbi));
        }
    }

    private static Path locateLibrary() {
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

    private static Path extractFromResources() {
        String resourcePath = getPlatformResourcePath();
        try (InputStream is = NativeLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new ParakeetException(
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
        } catch (IOException e) {
            throw new ParakeetException("Failed to extract native library from " + resourcePath, e);
        }
    }

    private static String getPlatformResourcePath() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        String osPrefix;
        String libPrefix;

        if (os.contains("linux")) {
            osPrefix = "linux";
            libPrefix = "lib";
        } else if (os.contains("mac") || os.contains("darwin")) {
            osPrefix = "macos";
            libPrefix = "lib";
        } else if (os.contains("win")) {
            osPrefix = "windows";
            libPrefix = "";
        } else {
            throw new ParakeetException("Unsupported operating system: " + os);
        }

        String archSuffix;
        if (arch.equals("x86_64") || arch.equals("amd64") || arch.equals("x86-64")) {
            archSuffix = "x86-64";
        } else if (arch.equals("aarch64") || arch.equals("arm64")) {
            archSuffix = "aarch64";
        } else {
            throw new ParakeetException("Unsupported architecture: " + arch);
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

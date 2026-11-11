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

import java.util.List;

/**
 * The {@link GgufMetadata} record represents extracted key-value header metadata from a GGUF model
 * file.
 *
 * @see <a href="https://github.com/mudler/parakeet.cpp/blob/master/docs/bundle.md">parakeet.cpp:
 *     Bundle Docs</a>
 * @author Florian Hotze - Initial contribution
 */
public record GgufMetadata(
        String name, String architecture, List<String> languages, String parakeetArch) {

    public GgufMetadata {
        languages = List.copyOf(languages);
    }

    /**
     * Determines whether this model is exclusively a Voice Activity Detection (VAD) model.
     *
     * @return true if the architecture indicates a standalone VAD model
     */
    public boolean isVadOnly() {
        return "silero_vad".equalsIgnoreCase(architecture)
                || ("parakeet".equalsIgnoreCase(architecture)
                        && "vad".equalsIgnoreCase(parakeetArch));
    }

    /**
     * Determines whether this model is an ASR model, which might include a VAD head.
     *
     * @return true if the architecture indicates an ASR model
     */
    public boolean isAsr() {
        return ("parakeet".equalsIgnoreCase(architecture) && !"vad".equalsIgnoreCase(parakeetArch))
                || "canary".equalsIgnoreCase(architecture);
    }
}

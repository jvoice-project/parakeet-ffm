/*
 * #%L
 * parakeet4j
 * %%
 * Copyright (C) 2026 Contributors to parakeet4j
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

import com.google.gson.annotations.SerializedName;

/**
 * Metadata for a component inside a bundle GGUF file.
 *
 * @see <a href="https://github.com/mudler/parakeet.cpp/blob/v0.6.1/docs/bundle.md">parakeet.cpp:
 *     Bundle GGUF</a>
 * @param name Component name.
 * @param kind Component kind (e.g., <code>asr</code, <code>vad</code>, <code>diar</code>).
 * @param license Component license.
 * @param licenseUrl Component license URL.
 * @param source Upstream source repository or checkpoint.
 * @param attribution Attribution text.
 * @param changes Description of changes.
 * @param sourceSha256 SHA256 of source model.
 * @param contentSha256 SHA256 of component content.
 * @param tensors Number of tensors in component.
 * @param bytes Total bytes of component tensors.
 * @author Florian Hotze - Initial contribution
 */
public record BundleComponent(
        String name,
        String kind,
        String license,
        @SerializedName("license_url") String licenseUrl,
        String source,
        String attribution,
        String changes,
        @SerializedName("source_sha256") String sourceSha256,
        @SerializedName("content_sha256") String contentSha256,
        long tensors,
        long bytes) {}

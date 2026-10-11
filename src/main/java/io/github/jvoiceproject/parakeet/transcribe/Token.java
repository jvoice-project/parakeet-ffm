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
package io.github.jvoiceproject.parakeet.transcribe;

import com.google.gson.annotations.SerializedName;

/**
 * A transcribed token with timestamp and confidence.
 *
 * @param id Token vocabulary ID.
 * @param time Timestamp in seconds.
 * @param confidence Confidence score in (0, 1].
 * @author Florian Hotze - Initial contribution
 */
public record Token(
        int id, @SerializedName("t") double time, @SerializedName("conf") double confidence) {}

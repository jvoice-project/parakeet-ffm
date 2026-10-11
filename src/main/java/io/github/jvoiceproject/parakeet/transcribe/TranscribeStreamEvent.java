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
package io.github.jvoiceproject.parakeet.transcribe;

import com.google.gson.annotations.SerializedName;

/**
 * An event emitted during streaming (e.g., End of Utterance (EOU) or End of Backchannel (EOB)).
 *
 * @param type Event type (<code>eou</code> or <code>eob</code>).
 * @param frame Encoder frame index.
 * @param time Emission timestamp in seconds from stream start.
 * @author Florian Hotze - Initial contribution
 */
public record TranscribeStreamEvent(String type, int frame, @SerializedName("t") double time) {}

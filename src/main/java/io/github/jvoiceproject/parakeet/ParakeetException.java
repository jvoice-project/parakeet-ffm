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

/**
 * Exception thrown when a native Parakeet call or initialization fails.
 *
 * @author Florian Hotze - Initial contribution
 */
public class ParakeetException extends RuntimeException {
    public ParakeetException(String message) {
        super(message);
    }

    public ParakeetException(Throwable cause) {
        super(cause);
    }

    public ParakeetException(String message, Throwable cause) {
        super(message, cause);
    }
}

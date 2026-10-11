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
module io.github.jvoiceproject.parakeet {
    requires com.google.gson;

    exports io.github.jvoiceproject.parakeet;
    exports io.github.jvoiceproject.parakeet.transcribe;
    exports io.github.jvoiceproject.parakeet.vad;

    opens io.github.jvoiceproject.parakeet to
            com.google.gson;
    opens io.github.jvoiceproject.parakeet.transcribe to
            com.google.gson;
    opens io.github.jvoiceproject.parakeet.vad to
            com.google.gson;
}

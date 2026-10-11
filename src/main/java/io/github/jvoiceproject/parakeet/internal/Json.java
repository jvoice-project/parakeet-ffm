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

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Shared Gson instance configured for Parakeet JSON schemas.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class Json {

    public static final Gson GSON =
            new GsonBuilder()
                    .registerTypeAdapter(Boolean.class, new BooleanTypeAdapter())
                    .registerTypeAdapter(boolean.class, new BooleanTypeAdapter())
                    .create();

    private Json() {}

    /**
     * Type adapter for boolean values, adding support for integer booleans as emitted by C/C++.
     * Null or missing tokens safely default to false.
     */
    private static class BooleanTypeAdapter extends TypeAdapter<Boolean> {
        @Override
        public void write(JsonWriter out, Boolean value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else if (value) {
                out.value(1);
            } else {
                out.value(0);
            }
        }

        @Override
        public Boolean read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token == JsonToken.BOOLEAN) {
                return in.nextBoolean();
            } else if (token == JsonToken.NUMBER) {
                return in.nextInt() != 0;
            } else if (token == JsonToken.NULL) {
                in.nextNull();
                return Boolean.FALSE;
            }
            return Boolean.FALSE;
        }
    }
}

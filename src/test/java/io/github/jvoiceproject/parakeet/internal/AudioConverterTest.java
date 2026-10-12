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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AudioConverter}.
 *
 * @author Florian Hotze - Initial contribution
 */
public class AudioConverterTest {

    @Test
    void pcm16ToFloatConversionAccuracy() {
        short[] input = new short[] {Short.MIN_VALUE, -16384, 0, 16384, Short.MAX_VALUE};
        float[] result = AudioConverter.pcm16ToFloat(input);

        assertNotNull(result);
        assertEquals(5, result.length);
        assertEquals(-1.0f, result[0], 1e-6f);
        assertEquals(-0.5f, result[1], 1e-6f);
        assertEquals(0.0f, result[2], 1e-6f);
        assertEquals(0.5f, result[3], 1e-6f);
        assertEquals(32767.0f / 32768.0f, result[4], 1e-6f);
    }

    @Test
    void pcm16ToFloatEmptyArray() {
        float[] result = AudioConverter.pcm16ToFloat(new short[0]);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void pcm16ToFloatNullThrows() {
        assertThrows(NullPointerException.class, () -> AudioConverter.pcm16ToFloat(null));
        assertThrows(
                NullPointerException.class, () -> AudioConverter.pcm16ToFloat(null, new float[1]));
        assertThrows(
                NullPointerException.class, () -> AudioConverter.pcm16ToFloat(new short[1], null));
    }

    @Test
    void pcm16ToFloatIntoTargetArray() {
        short[] input = new short[] {Short.MIN_VALUE, 0, 16384};
        float[] target = new float[] {99.0f, 99.0f, 99.0f, 42.0f};

        AudioConverter.pcm16ToFloat(input, target);

        assertEquals(-1.0f, target[0], 1e-6f);
        assertEquals(0.0f, target[1], 1e-6f);
        assertEquals(0.5f, target[2], 1e-6f);
        assertEquals(42.0f, target[3], 1e-6f);
    }

    @Test
    void pcm16ToFloatTargetTooSmallThrows() {
        short[] input = new short[] {1, 2, 3};
        float[] target = new float[2];

        assertThrows(
                IllegalArgumentException.class, () -> AudioConverter.pcm16ToFloat(input, target));
    }
}

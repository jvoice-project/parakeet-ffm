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
package io.github.jvoiceproject.parakeet.vad;

import io.github.jvoiceproject.parakeet.internal.Json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration options for Voice Activity Detection (VAD).
 *
 * @author Florian Hotze - Initial contribution
 */
public final class VadOptions {

    public enum Mode {
        /**
         * Returns the speech regions after smoothing. They are ordered, disjoint, and silence is
         * never included.
         */
        SPEECH("speech"),
        /**
         * Returns the cuts the transcriber uses internally: Pieces of at most max_segment seconds
         * cut at pauses, pieces without speec dropped, and audio of at most max_segments seconds
         * returned whole as one segment even when it holds no speech.
         */
        SEGMENTS("segments");

        private final String value;

        Mode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static final VadOptions DEFAULT = new Builder().build();

    private final Double threshold;
    private final Double minPause;
    private final Double minSpeech;
    private final Double speechPad;
    private final Double maxSegment;
    private final Double trim;
    private final Double runGate;
    private final Mode mode;
    private final Boolean probabilities;

    private VadOptions(Builder builder) {
        this.threshold = builder.threshold;
        this.minPause = builder.minPause;
        this.minSpeech = builder.minSpeech;
        this.speechPad = builder.speechPad;
        this.maxSegment = builder.maxSegment;
        this.trim = builder.trim;
        this.runGate = builder.runGate;
        this.mode = builder.mode;
        this.probabilities = builder.probabilities;
    }

    public Double getThreshold() {
        return threshold;
    }

    public Double getMinPause() {
        return minPause;
    }

    public Double getMinSpeech() {
        return minSpeech;
    }

    public Double getSpeechPad() {
        return speechPad;
    }

    public Double getMaxSegment() {
        return maxSegment;
    }

    public Double getTrim() {
        return trim;
    }

    public Double getRunGate() {
        return runGate;
    }

    public Mode getMode() {
        return mode;
    }

    public Boolean getProbabilities() {
        return probabilities;
    }

    public String toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (threshold != null) {
            map.put("threshold", threshold);
        }
        if (minPause != null) {
            map.put("min_pause", minPause);
        }
        if (minSpeech != null) {
            map.put("min_speech", minSpeech);
        }
        if (speechPad != null) {
            map.put("speech_pad", speechPad);
        }
        if (maxSegment != null) {
            map.put("max_segment", maxSegment);
        }
        if (trim != null) {
            map.put("trim", trim);
        }
        if (runGate != null) {
            map.put("run_gate", runGate);
        }
        if (mode != null) {
            map.put("mode", mode.getValue());
        }
        if (probabilities != null) {
            map.put("probabilities", probabilities);
        }
        if (map.isEmpty()) {
            return null;
        }
        return Json.GSON.toJson(map);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.threshold = this.threshold;
        b.minPause = this.minPause;
        b.minSpeech = this.minSpeech;
        b.speechPad = this.speechPad;
        b.maxSegment = this.maxSegment;
        b.trim = this.trim;
        b.runGate = this.runGate;
        b.mode = this.mode;
        b.probabilities = this.probabilities;
        return b;
    }

    public static final class Builder {
        private Double threshold;
        private Double minPause;
        private Double minSpeech;
        private Double speechPad;
        private Double maxSegment;
        private Double trim;
        private Double runGate;
        private Mode mode;
        private Boolean probabilities;

        /**
         * Sets the threshold for the VAD:
         *
         * <p>A frame is considered speech when p >= treshold.
         *
         * @param threshold in (0, 1]; default 0.5
         * @return
         */
        public Builder threshold(double threshold) {
            if (threshold <= 0.0 || threshold > 1.0) {
                throw new IllegalArgumentException("threshold must be in (0, 1]");
            }
            this.threshold = threshold;
            return this;
        }

        /**
         * Minimum pause between speech segments. A silence at least this longs separates regions of
         * speech.
         *
         * @param minPause seconds
         * @return
         */
        public Builder minPause(double minPause) {
            if (minPause <= 0.0) {
                throw new IllegalArgumentException("minPause must be > 0");
            }
            this.minPause = minPause;
            return this;
        }

        /**
         * Minimum speech duration. Shorter speech runs are dropped.
         *
         * @param minSpeech seconds
         * @return
         */
        public Builder minSpeech(double minSpeech) {
            if (minSpeech <= 0.0) {
                throw new IllegalArgumentException("minSpeech must be > 0");
            }
            this.minSpeech = minSpeech;
            return this;
        }

        /**
         * In {@link Mode#SPEECH}: Each region is widened by this on both sides.
         *
         * @param speechPad seconds; default 0
         * @return
         */
        public Builder speechPad(double speechPad) {
            if (speechPad < 0.0) {
                throw new IllegalArgumentException("speechPad must be >= 0");
            }
            this.speechPad = speechPad;
            return this;
        }

        /**
         * In {@link Mode#SEGMENTS}: Maximum segment length. Longer segments are split.
         *
         * @param maxSegment seconds; default 30
         * @return
         */
        public Builder maxSegment(double maxSegment) {
            if (maxSegment <= 0.0) {
                throw new IllegalArgumentException("maxSegment must be > 0");
            }
            this.maxSegment = maxSegment;
            return this;
        }

        /**
         * In {@link Mode#SEGMENTS}: Each segment shrinks to its first and last speech frame plus
         * this much.
         *
         * @param trim seconds; default 0.1; 0 keeps the whole cuts
         * @return
         */
        public Builder trim(double trim) {
            if (trim < 0.0) {
                throw new IllegalArgumentException("trim must be >= 0");
            }
            this.trim = trim;
            return this;
        }

        /**
         * A speech run (frames with p >= threshold) whose median frame probability is below this is
         * dropped; a median equal to it keeps the run.
         *
         * @param runGate probability in [0, 1); default 0 (= off)
         * @return
         */
        public Builder runGate(double runGate) {
            if (runGate < 0.0 || runGate >= 1.0) {
                throw new IllegalArgumentException("runGate must be in [0, 1)");
            }
            this.runGate = runGate;
            return this;
        }

        /**
         * VAD mode
         *
         * @param mode default {@link Mode#SPEECH}
         * @return
         */
        public Builder mode(Mode mode) {
            this.mode = Objects.requireNonNull(mode, "mode cannot be null");
            return this;
        }

        /**
         * Whether to add per-frame probabilities to the output.
         *
         * @param probabilities default false
         * @return
         */
        public Builder probabilities(boolean probabilities) {
            this.probabilities = probabilities;
            return this;
        }

        public VadOptions build() {
            return new VadOptions(this);
        }
    }
}

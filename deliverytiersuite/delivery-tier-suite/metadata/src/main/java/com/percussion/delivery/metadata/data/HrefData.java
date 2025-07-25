/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// REFACTORED: CP-JAVA11

package com.percussion.delivery.metadata.data;

import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a hyperlink with a key and URL.
 */
@XmlRootElement
public class HrefData {

    private final String key;
    private final String url;

    public HrefData(String key, String url) {
        this.key = key;
        this.url = url;
    }

    public Optional<String> getKey() {
        return Optional.ofNullable(key);
    }

    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String key;
        private String url;

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public HrefData build() {
            return new HrefData(key, url);
        }
    }
}

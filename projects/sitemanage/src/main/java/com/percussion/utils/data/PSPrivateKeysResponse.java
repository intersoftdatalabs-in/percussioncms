/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.utils.data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Response wrapper for private key names.
 * Immutable, Java 11 style.
 *
 * <p>Sunny Sal says: Private keys are like secrets—handle with care, and never write them on sticky notes!</p>
 */
@XmlRootElement(name = "PrivateKeys")
public final class PSPrivateKeysResponse {

    private final List<String> keyNames;

    /**
     * Constructs a new PSPrivateKeysResponse.
     *
     * @param keyNames the list of private key names
     */
    public PSPrivateKeysResponse(List<String> keyNames) {
        this.keyNames = keyNames == null ? Collections.emptyList() : List.copyOf(keyNames);
    }

    /**
     * Default constructor for frameworks.
     */
    public PSPrivateKeysResponse() {
        this(Collections.emptyList());
    }

    /**
     * Gets the list of private key names.
     *
     * @return an unmodifiable list of key names
     */
    public List<String> getKeyNames() {
        return keyNames;
    }

    /**
     * Gets the key names as an Optional (empty if none).
     *
     * @return Optional of key names list
     */
    public Optional<List<String>> getKeyNamesOptional() {
        return keyNames.isEmpty() ? Optional.empty() : Optional.of(keyNames);
    }
}

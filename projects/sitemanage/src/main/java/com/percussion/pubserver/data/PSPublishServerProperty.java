// REFACTORED: CP-JAVA11
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
package com.percussion.pubserver.data;

import java.util.Objects;

/**
 * Represents a key-value property for a publishing server.
 * Immutable except for value setter.
 * @author ignacioerro
 */
public class PSPublishServerProperty {
    private static final long serialVersionUID = 1L;

    private String key;
    private String value;

    /** Returns the property key. */
    public String getKey() {
        return key;
    }

    /** Sets the property key. */
    public void setKey(String key) {
        this.key = key;
    }

    /** Returns the property value. */
    public String getValue() {
        return value;
    }

    /** Sets the property value (trimmed). */
    public void setValue(String value) {
        this.value = value == null ? null : value.trim();
    }

    /** Returns the serial version UID. */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PSPublishServerProperty)) return false;
        var other = (PSPublishServerProperty) obj;
        return Objects.equals(key, other.key) && Objects.equals(value, other.value);
    }
}

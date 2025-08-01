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
package com.percussion.services.aaclient;

import org.json.simple.ItemList;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

/**
 * A JSON object implementation that extends LinkedHashMap to maintain insertion order.
 * This class provides methods to convert the object to JSON string format with proper escaping.
 *
 * @author FangYidong<fangyidong@yahoo.com.cn>
 */
public class JSONObject extends LinkedHashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    /**
     * Converts this JSON object to its string representation.
     *
     * @return JSON string representation of this object
     */
    @Override
    public String toString() {
        var list = new ItemList();

        entrySet().stream()
            .map(entry -> toString(entry.getKey(), entry.getValue()))
            .forEach(list::add);

        return "{" + list + "}";
    }
    
    /**
     * Converts a key-value pair to JSON string format.
     *
     * @param key the key to convert, must not be null
     * @param value the value to convert, may be null
     * @return JSON string representation of the key-value pair
     * @throws IllegalArgumentException if key is null
     */
    public static String toString(String key, Object value) {
        Objects.requireNonNull(key, "Key cannot be null");

        var sb = new StringBuilder();
        sb.append("\"");
        sb.append(escape(key));
        sb.append("\":");

        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append("\"");
            sb.append(escape((String) value));
            sb.append("\"");
        } else {
            sb.append(value);
        }

        return sb.toString();
    }
    
    /**
     * Escapes special characters in a string for JSON format.
     * Handles quotes, backslashes, control characters, and Unicode escape sequences.
     *
     * @param s the string to escape, may be null
     * @return escaped string suitable for JSON, or null if input is null
     */
    public static String escape(String s) {
        return Optional.ofNullable(s)
            .map(JSONObject::escapeInternal)
            .orElse(null);
    }

    /**
     * Internal method to perform the actual string escaping.
     *
     * @param s the string to escape, guaranteed not null
     * @return escaped string
     */
    private static String escapeInternal(String s) {
        var sb = new StringBuilder();

        for (var i = 0; i < s.length(); i++) {
            var ch = s.charAt(i);

            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                default:
                    if (ch <= '\u001F') {
                        var hexString = Integer.toHexString(ch);
                        sb.append("\\u");
                        // Pad with zeros to ensure 4-character hex representation
                        sb.append("0".repeat(Math.max(0, 4 - hexString.length())));
                        sb.append(hexString.toUpperCase());
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }

        return sb.toString();
    }
}

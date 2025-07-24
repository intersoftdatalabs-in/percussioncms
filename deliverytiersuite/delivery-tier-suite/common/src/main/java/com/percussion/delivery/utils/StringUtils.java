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

package com.percussion.delivery.utils;

/**
 * Utility class for String operations.
 * // REFACTORED: CP-JAVA11
 */
public class StringUtils {

    /**
     * Joins two URL parts, ensuring a single slash between them.
     *
     * @param firstPart  the first part of the URL, may be null or empty
     * @param secondPart the second part of the URL, may be null or empty
     * @return the joined URL
     */
    public static String joinURL(String firstPart, String secondPart) {
        var first = firstPart == null ? "" : firstPart;
        var second = secondPart == null ? "" : secondPart;
        if (first.endsWith("/")) {
            first = first.substring(0, first.length() - 1);
        }
        if (second.startsWith("/")) {
            second = second.substring(1);
        }
        return first + "/" + second;
    }
}

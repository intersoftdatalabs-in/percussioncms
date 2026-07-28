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
 * Utility helpers for working with {@link String} values used throughout the delivery tier.
 */
public class StringUtils {

  /** Default constructor; this is a utility class and is not meant to be instantiated. */
  private StringUtils() {}

  /**
   * Joins two URL path fragments with exactly one forward slash, trimming any existing leading or
   * trailing slash from the parts.
   *
   * @param firstPart the leading URL fragment; may be <code>null</code> or empty.
   * @param secondPart the trailing URL fragment; may be <code>null</code> or empty.
   * @return the joined URL path, never <code>null</code>.
   */
  public static String joinURL(String firstPart, String secondPart) {

    String ret = null;

    if (null != firstPart && firstPart != "") {
      if (firstPart.endsWith("/")) {
        firstPart = firstPart.substring(0, firstPart.length() - 1);
      }
    } else {
      firstPart = "";
    }

    if (null != secondPart && "" != secondPart) {

      if (secondPart.startsWith("/")) {
        secondPart = secondPart.substring(1);
      }
    } else {
      secondPart = "";
    }

    return firstPart + "/" + secondPart;
  }
}

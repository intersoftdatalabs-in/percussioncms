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

package com.percussion.util;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/** Utility class for functional programming. */
public class FunctionalUtils {

  /** Private constructor. */
  private FunctionalUtils() {}

  /** Pattern for splitting comma-separated strings. */
  public static Pattern COMMA_SPLIT = Pattern.compile(",");

  /** Predicate for positive numbers or non-null objects. */
  public static Predicate<? super Object> IS_POSITIVE_NUMBER_OR_NOT_NULL =
      p -> p != null && (p instanceof Number) && (((Number) p).doubleValue() > 0);

  /**
   * Convert a comma-separated string to a stream.
   *
   * @param string the input string, may be <code>null</code>.
   * @return the stream of trimmed non-blank tokens.
   */
  public static Stream<String> commaStringToStream(String string) {
    return COMMA_SPLIT.splitAsStream(string).filter(StringUtils::isNotBlank).map(StringUtils::trim);
  }
}

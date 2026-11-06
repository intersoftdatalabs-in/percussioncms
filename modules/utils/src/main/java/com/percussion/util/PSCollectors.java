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

import java.util.stream.Collector;
import java.util.stream.Collectors;

  /** Member. */
public class PSCollectors {

  /** Prevent instantiation, this class only provides static collectors. */
  private PSCollectors() {}

  /**
   * Returns a collector joining character sequences with commas.
   *
   * @return a comma-joining collector, never <code>null</code>
   */
  public static Collector<CharSequence, ?, String> commaSeparatedString() {
    return Collectors.joining(",");
  }
}

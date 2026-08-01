/*
 * Copyright 2026 Intersoft Data Labs (https://intsof.com)
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intsof.common.utilities;

import java.nio.file.Path;

/** Ensures resolved paths remain under a configured root (path-traversal guard). */
final class PathsUnder {

  private PathsUnder() {}

  /**
   * Resolve {@code segment} under {@code root} and verify the result stays within {@code root}.
   *
   * @param root absolute normalized parent directory
   * @param segment single validated path segment
   * @return absolute normalized child path
   * @throws IllegalArgumentException if the resolved path escapes {@code root}
   */
  static Path resolveUnder(Path root, String segment) {
    Path normalizedRoot = root.toAbsolutePath().normalize();
    Path resolved = normalizedRoot.resolve(segment).normalize();
    if (!resolved.startsWith(normalizedRoot)) {
      throw new IllegalArgumentException("Resolved path escapes configuration root: " + segment);
    }
    // Disallow resolving to the root itself (empty segment already rejected elsewhere)
    if (resolved.equals(normalizedRoot)) {
      throw new IllegalArgumentException("Resolved path must be a child of configuration root");
    }
    return resolved;
  }
}

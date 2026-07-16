/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.assetmanagement.service.impl;

/**
 * Package-visible helpers for rejecting empty / whitespace-only text asset uploads (GH-728 /
 * #775+#776). Extracted for pure unit tests without Spring-wiring {@code PSAssetService}.
 */
final class PSEmptyUploadContent {

  private PSEmptyUploadContent() {}

  /** True when content is null, empty, or whitespace-only ({@link String#isBlank()}). */
  static boolean isEmptyOrWhitespaceOnly(String content) {
    return content == null || content.isBlank();
  }

  /**
   * User-facing rejection message. Uses "empty or whitespace-only" (not "0 bytes") because the
   * guard rejects both zero-length and blank content.
   */
  static String rejectionMessage(String fileName) {
    return "The uploaded file '"
        + (fileName != null ? fileName : "")
        + "' is empty or whitespace-only. Cannot create a text-based asset from an empty file.";
  }
}

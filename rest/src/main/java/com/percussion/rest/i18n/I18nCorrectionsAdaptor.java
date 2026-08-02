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

package com.percussion.rest.i18n;

/**
 * Server-side submit of browser i18n corrections (e.g. to GCM).
 *
 * <p>Implementation lives in sitemanage apibridge.
 */
public interface I18nCorrectionsAdaptor {

  /**
   * Validate session/role gates and forward the correction.
   *
   * @param submission plugin body; never null
   * @return result with optional transport message id
   * @throws IllegalArgumentException validation failure (maps to 400)
   * @throws SecurityException not allowed / feature off for user (maps to 403)
   * @throws IllegalStateException not configured / native lib missing (maps to 503)
   * @throws RuntimeException transport/backend failure (maps to 502; resource must not echo the
   *     exception message to clients — may contain SDK/token details)
   */
  I18nCorrectionResult submit(I18nCorrectionSubmission submission);
}

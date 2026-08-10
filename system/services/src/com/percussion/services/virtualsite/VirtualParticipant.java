/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.services.virtualsite;

import java.util.Objects;

/** Lightweight identity projection for a Virtual Site page (not a CMS content item). */
public final class VirtualParticipant {

  private final String siteKey;
  private final String stableId;
  private final String versionId;
  private final String publishedPath;
  private final String sourcePath;

  public VirtualParticipant(
      String siteKey,
      String stableId,
      String versionId,
      String publishedPath,
      String sourcePath) {
    this.siteKey = Objects.requireNonNull(siteKey, "siteKey");
    this.stableId = Objects.requireNonNull(stableId, "stableId");
    this.versionId = versionId != null ? versionId : "";
    this.publishedPath = Objects.requireNonNull(publishedPath, "publishedPath");
    this.sourcePath = sourcePath != null ? sourcePath : "";
  }

  public String siteKey() {
    return siteKey;
  }

  public String stableId() {
    return stableId;
  }

  public String versionId() {
    return versionId;
  }

  public String publishedPath() {
    return publishedPath;
  }

  public String sourcePath() {
    return sourcePath;
  }
}

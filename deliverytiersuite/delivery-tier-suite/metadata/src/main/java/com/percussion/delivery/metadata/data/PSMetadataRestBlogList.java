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
package com.percussion.delivery.metadata.data;

import java.util.ArrayList;
import java.util.List;

/**
 * REST response shape for the {@code /blogs/get} endpoint. Carries the {@link PSMetadataBlogYear}
 * tree built from the indexed blog entries.
 */
public class PSMetadataRestBlogList {
  /** Per-year bucket list; never {@code null}. */
  private List<PSMetadataBlogYear> years = new ArrayList<>();

  /** No-arg constructor required by the JSON binding layer. */
  public PSMetadataRestBlogList() {}

  /**
   * Returns the list of year buckets.
   *
   * @return the years, may be empty but never <code>null</code>.
   */
  public List<PSMetadataBlogYear> getYears() {
    return years;
  }

  /**
   * Replaces the year buckets.
   *
   * @param years the years to set; may be <code>null</code>.
   */
  public void setYears(List<PSMetadataBlogYear> years) {
    this.years = years;
  }
}

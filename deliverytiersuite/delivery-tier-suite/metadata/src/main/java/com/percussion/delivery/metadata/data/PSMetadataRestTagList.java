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
 * Represents a list of {@link PSMetadataRestTag} instances. Returned by the {@code /tags/get} REST
 * endpoint.
 */
public class PSMetadataRestTagList {
  /** Backend list of tag entries; never {@code null}. */
  private List<PSMetadataRestTag> properties = new ArrayList<>();

  /** No-arg constructor required by the JSON binding layer. */
  public PSMetadataRestTagList() {}

  /**
   * Returns the underlying list of tag entries.
   *
   * @return the properties list, may be empty but never <code>null</code>.
   */
  public List<PSMetadataRestTag> getProperties() {
    return properties;
  }

  /**
   * Replaces the tag entries.
   *
   * <p>To preserve the {@link #getProperties() never-null} contract, a {@code null} argument is
   * normalized to an empty list rather than assigned as-is.
   *
   * @param properties the properties to set; may be {@code null}, in which case an empty list is
   *     stored.
   */
  public void setProperties(List<PSMetadataRestTag> properties) {
    this.properties = properties == null ? new ArrayList<>() : properties;
  }
}

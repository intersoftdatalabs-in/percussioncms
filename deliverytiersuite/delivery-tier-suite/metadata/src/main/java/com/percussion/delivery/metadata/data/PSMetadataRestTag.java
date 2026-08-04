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

/**
 * Represents an object with a tag name and the count of pages that are tagged with it. Returned by
 * the {@code /tags/get} REST endpoint.
 */
public class PSMetadataRestTag {
  private String tagName;

  private Integer tagCount;

  /** No-arg constructor required by the JSON binding layer. */
  public PSMetadataRestTag() {}

  /**
   * Returns the tag name.
   *
   * @return the tagName, may be <code>null</code>.
   */
  public String getTagName() {
    return tagName;
  }

  /**
   * Sets the tag name.
   *
   * @param tagName the tagName to set; may be <code>null</code>.
   */
  public void setTagName(String tagName) {
    this.tagName = tagName;
  }

  /**
   * Returns the page count associated with the tag.
   *
   * @return the tagCount, may be <code>null</code>.
   */
  public Integer getTagCount() {
    return tagCount;
  }

  /**
   * Sets the page count associated with the tag.
   *
   * @param tagCount the tagCount to set; may be <code>null</code>.
   */
  public void setTagCount(Integer tagCount) {
    this.tagCount = tagCount;
  }
}

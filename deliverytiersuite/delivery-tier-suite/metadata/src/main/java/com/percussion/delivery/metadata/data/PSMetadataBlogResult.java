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
 * Carries the previous / current / next blog entry pointers returned by the blog pagination REST
 * endpoint. The three slots are populated only when a corresponding neighbour exists in the blog
 * archive; otherwise they remain {@code null}.
 */
public class PSMetadataBlogResult {
  private PSMetadataRestEntry previous;

  private PSMetadataRestEntry current;

  private PSMetadataRestEntry next;

  /** No-arg constructor required by the JSON binding layer. */
  public PSMetadataBlogResult() {}

  /**
   * Returns the previous blog entry, or {@code null} if the current entry is the oldest one
   * indexed.
   *
   * @return the previous entry, may be <code>null</code>.
   */
  public PSMetadataRestEntry getPrevious() {
    return previous;
  }

  /**
   * Sets the previous blog entry.
   *
   * @param previous the previous entry to set; may be <code>null</code>.
   */
  public void setPrevious(PSMetadataRestEntry previous) {
    this.previous = previous;
  }

  /**
   * Returns the current blog entry being viewed.
   *
   * @return the current entry, may be <code>null</code>.
   */
  public PSMetadataRestEntry getCurrent() {
    return current;
  }

  /**
   * Sets the current blog entry being viewed.
   *
   * @param current the current entry to set; may be <code>null</code>.
   */
  public void setCurrent(PSMetadataRestEntry current) {
    this.current = current;
  }

  /**
   * Returns the next blog entry, or {@code null} if the current entry is the newest one indexed.
   *
   * @return the next entry, may be <code>null</code>.
   */
  public PSMetadataRestEntry getNext() {
    return next;
  }

  /**
   * Sets the next blog entry.
   *
   * @param next the next entry to set; may be <code>null</code>.
   */
  public void setNext(PSMetadataRestEntry next) {
    this.next = next;
  }
}

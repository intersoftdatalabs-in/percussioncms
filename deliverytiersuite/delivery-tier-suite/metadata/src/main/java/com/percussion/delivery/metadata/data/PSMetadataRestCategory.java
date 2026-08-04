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

import com.percussion.delivery.metadata.impl.utils.PSPair;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an object with a category name, the count of pages that are categorized with it and
 * their children. Used by the metadata REST service to carry the category tree returned by the
 * {@code /categories/get} endpoint.
 */
public class PSMetadataRestCategory {
  private String category;

  private PSPair<Integer, Integer> count;

  private List<PSMetadataRestCategory> children;

  /** No-arg constructor required by the JSON binding layer. */
  public PSMetadataRestCategory() {
    super();
  }

  /**
   * Constructs a category node with an empty count pair and child list.
   *
   * @param category the category name; may not be {@code null}.
   */
  public PSMetadataRestCategory(String category) {
    super();
    this.category = category;
    this.count = new PSPair<>(0, 0);
    this.children = new ArrayList<>();
  }

  /**
   * Constructs a fully-populated category node.
   *
   * @param category the category name; may not be {@code null}.
   * @param count the count pair (own / total) for this category; may not be {@code null}.
   * @param children the child categories; may not be {@code null}.
   */
  public PSMetadataRestCategory(
      String category, PSPair<Integer, Integer> count, List<PSMetadataRestCategory> children) {
    super();
    this.category = category;
    this.count = count;
    this.children = children;
  }

  /**
   * Returns the category name.
   *
   * @return the category, may be <code>null</code>.
   */
  public String getCategory() {
    return category;
  }

  /**
   * Sets the category name.
   *
   * @param category the category to set; may be <code>null</code>.
   */
  public void setCategory(String category) {
    this.category = category;
  }

  /**
   * Returns the count pair (own / total) for this category.
   *
   * @return the count, may be <code>null</code>.
   */
  public PSPair<Integer, Integer> getCount() {
    return count;
  }

  /**
   * Sets the count pair for this category.
   *
   * @param count the count to set; may be <code>null</code>.
   */
  public void setCount(PSPair<Integer, Integer> count) {
    this.count = count;
  }

  /**
   * Returns the immediate child categories.
   *
   * @return the children list, may be <code>null</code>.
   */
  public List<PSMetadataRestCategory> getChildren() {
    return children;
  }

  /**
   * Replaces the immediate child categories.
   *
   * @param children the children to set; may be <code>null</code>.
   */
  public void setChildrens(List<PSMetadataRestCategory> children) {
    this.children = children;
  }
}

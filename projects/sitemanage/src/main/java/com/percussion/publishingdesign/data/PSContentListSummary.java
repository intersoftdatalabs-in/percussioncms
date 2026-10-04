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
package com.percussion.publishingdesign.data;

import jakarta.xml.bind.annotation.XmlRootElement;

/** JSON/XML view of a content list for the design façade. */
@XmlRootElement(name = "contentList")
public class PSContentListSummary {
  private String contentListId;
  private String name;
  private String description;

  /** modern | legacy | unknown */
  private String listType;

  private String generator;
  private String url;

  /**
   * Item filter uuid, or a filter name. {@code null} leaves the stored filter unchanged. Blank
   * clears it. A value that does not match an existing filter is rejected.
   */
  private String itemFilterId;

  /** Display name of the stored item filter. Ignored on write. */
  private String itemFilterName;

  public String getContentListId() {
    return contentListId;
  }

  public void setContentListId(String contentListId) {
    this.contentListId = contentListId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getListType() {
    return listType;
  }

  public void setListType(String listType) {
    this.listType = listType;
  }

  public String getGenerator() {
    return generator;
  }

  public void setGenerator(String generator) {
    this.generator = generator;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getItemFilterId() {
    return itemFilterId;
  }

  public void setItemFilterId(String itemFilterId) {
    this.itemFilterId = itemFilterId;
  }

  public String getItemFilterName() {
    return itemFilterName;
  }

  public void setItemFilterName(String itemFilterName) {
    this.itemFilterName = itemFilterName;
  }
}

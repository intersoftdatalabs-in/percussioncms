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
package com.percussion.itemmanagement.data;

import jakarta.xml.bind.annotation.XmlRootElement;

/** Filename / presence metadata for one itemmanagement binary field. */
@XmlRootElement(name = "ItemEditorBinaryMeta")
public class PSItemEditorBinaryMeta {

  private String contentId;
  private String field;
  private String filename;
  private String contentType;
  private boolean present;

  public String getContentId() {
    return contentId;
  }

  public void setContentId(String contentId) {
    this.contentId = contentId;
  }

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public boolean isPresent() {
    return present;
  }

  public void setPresent(boolean present) {
    this.present = present;
  }
}

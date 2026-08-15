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
import java.util.ArrayList;
import java.util.List;

/** Item field payload for the React content editor (995). */
@XmlRootElement(name = "ItemEditorFields")
public class PSItemEditorFields {

  private String contentId;
  private String contentType;
  private String name;
  private String checkoutUser;
  private List<PSItemEditorField> fields = new ArrayList<>();

  public String getContentId() {
    return contentId;
  }

  public void setContentId(String contentId) {
    this.contentId = contentId;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCheckoutUser() {
    return checkoutUser;
  }

  public void setCheckoutUser(String checkoutUser) {
    this.checkoutUser = checkoutUser;
  }

  public List<PSItemEditorField> getFields() {
    return fields;
  }

  public void setFields(List<PSItemEditorField> fields) {
    this.fields = fields == null ? new ArrayList<>() : fields;
  }
}

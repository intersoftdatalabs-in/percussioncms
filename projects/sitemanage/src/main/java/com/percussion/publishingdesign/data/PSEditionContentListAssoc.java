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

@XmlRootElement(name = "editionContentList")
public class PSEditionContentListAssoc {
  private String contentListId;
  private String deliveryContextId;
  private String assemblyContextId;
  private Integer sequence;

  public String getContentListId() {
    return contentListId;
  }

  public void setContentListId(String contentListId) {
    this.contentListId = contentListId;
  }

  public String getDeliveryContextId() {
    return deliveryContextId;
  }

  public void setDeliveryContextId(String deliveryContextId) {
    this.deliveryContextId = deliveryContextId;
  }

  public String getAssemblyContextId() {
    return assemblyContextId;
  }

  public void setAssemblyContextId(String assemblyContextId) {
    this.assemblyContextId = assemblyContextId;
  }

  public Integer getSequence() {
    return sequence;
  }

  public void setSequence(Integer sequence) {
    this.sequence = sequence;
  }
}

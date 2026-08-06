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

package com.percussion.rest.slots;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Content-type ↔ template association on a slot (pair of guids). */
@XmlRootElement(name = "SlotAssociation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlotAssociationSummary {

  private Guid contentTypeGuid;
  private Guid templateGuid;

  public SlotAssociationSummary() {}

  public Guid getContentTypeGuid() {
    return contentTypeGuid;
  }

  public void setContentTypeGuid(Guid contentTypeGuid) {
    this.contentTypeGuid = contentTypeGuid;
  }

  public Guid getTemplateGuid() {
    return templateGuid;
  }

  public void setTemplateGuid(Guid templateGuid) {
    this.templateGuid = templateGuid;
  }
}

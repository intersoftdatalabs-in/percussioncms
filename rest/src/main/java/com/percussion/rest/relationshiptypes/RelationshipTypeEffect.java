/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.rest.relationshiptypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Read-only projection of a relationship conditional effect. */
@XmlRootElement(name = "RelationshipTypeEffect")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Relationship type effect (extension call)")
public class RelationshipTypeEffect {

  @Schema(description = "Effect extension name")
  private String name;

  @Schema(description = "Fully qualified extension reference string")
  private String extensionRef;

  @Schema(description = "Activation end point (owner/dependent/either)")
  private String activationEndPoint;

  public RelationshipTypeEffect() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getExtensionRef() {
    return extensionRef;
  }

  public void setExtensionRef(String extensionRef) {
    this.extensionRef = extensionRef;
  }

  public String getActivationEndPoint() {
    return activationEndPoint;
  }

  public void setActivationEndPoint(String activationEndPoint) {
    this.activationEndPoint = activationEndPoint;
  }
}

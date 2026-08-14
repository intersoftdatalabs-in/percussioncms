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

// REFACTORED: CP-JAVA11
package com.percussion.rest.acls;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement(name = "CreateAclRequest")
@JsonRootName("CreateAclRequest")
@Schema(description = "A request to create an acl")
public class CreateAclRequest {

  @Schema(required = true, description = "A valid object guid.")
  private Guid objectGuid;

  @Schema(required = true, description = "A valid Typed Principal")
  private TypedPrincipal owner;

  public CreateAclRequest() {}

  public CreateAclRequest(Guid objectGuid, TypedPrincipal owner) {
    this.objectGuid = objectGuid;
    this.owner = owner;
  }

  public TypedPrincipal getOwner() {
    return owner;
  }

  public void setOwner(TypedPrincipal owner) {
    this.owner = owner;
  }

  public Guid getObjectGuid() {
    return objectGuid;
  }

  public void setObjectGuid(Guid objectGuid) {
    this.objectGuid = objectGuid;
  }

  @Override
  public String toString() {
    return "CreateAclRequest{" + "objectGuid=" + objectGuid + ", owner=" + owner + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CreateAclRequest)) return false;
    var that = (CreateAclRequest) o;
    return Objects.equals(objectGuid, that.objectGuid) && Objects.equals(owner, that.owner);
  }

  @Override
  public int hashCode() {
    return Objects.hash(objectGuid, owner);
  }
}

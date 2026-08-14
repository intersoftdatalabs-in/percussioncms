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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.percussion.security.IPSTypedPrincipal;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Typed Principal")
public class TypedPrincipal implements IPSTypedPrincipal {

  @Schema(description = "name", required = true)
  private String name;

  @Schema(description = "type", required = true)
  private PrincipalTypes type;

  public TypedPrincipal() {}

  public TypedPrincipal(String name, PrincipalTypes type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PrincipalTypes getType() {
    return type;
  }

  public void setType(PrincipalTypes type) {
    this.type = type;
  }

  @JsonIgnore
  @Override
  public boolean isType(PrincipalTypes principalType) {
    return type == principalType;
  }

  @JsonIgnore
  @Override
  public boolean isCommunity() {
    return type == PrincipalTypes.COMMUNITY;
  }

  @JsonIgnore
  @Override
  public boolean isRole() {
    return type == PrincipalTypes.ROLE;
  }

  @JsonIgnore
  @Override
  public boolean isUser() {
    return type == PrincipalTypes.USER || type == PrincipalTypes.SYSTEM_ENTRY;
  }

  @JsonIgnore
  @Override
  public boolean isGroup() {
    return type == PrincipalTypes.GROUP;
  }

  @JsonIgnore
  @Override
  public boolean isSubject() {
    return type == PrincipalTypes.SUBJECT;
  }

  @JsonIgnore
  @Override
  public boolean isSystemEntry() {
    return type == PrincipalTypes.SYSTEM_ENTRY;
  }

  @JsonIgnore
  @Override
  public boolean isSystemCommunity() {
    return type == PrincipalTypes.SYSTEM_COMMUNITY;
  }

  @JsonIgnore
  @Override
  public PrincipalTypes getPrincipalType() {
    return type;
  }

  @Override
  public String toString() {
    return "TypedPrincipal{" + "name='" + name + '\'' + ", type=" + type + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TypedPrincipal)) return false;
    var that = (TypedPrincipal) o;
    return Objects.equals(name, that.name) && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type);
  }
}

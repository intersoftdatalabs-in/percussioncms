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

package com.percussion.rest.deliverytypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Represents a Delivery Type in Percussion CMS. */
@XmlRootElement(name = "DeliveryType")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a Delivery Type.")
public class DeliveryType {

  @Schema(
      required = false,
      description = "id must match id supplied on url. Typically not sent to the server.")
  private Guid id;

  @Schema(required = true, description = "The name of the DeliveryType. Must be unique.")
  private String name;

  @Schema(required = false, description = "A friendly description of this DeliveryType")
  private String description;

  @Schema(
      required = false,
      description =
          "The Spring bean that implements this DeliveryType. Typically configured in"
              + " Rhythmyx/WEB-INF/config/user/spring/publisher-beans.xml")
  private String beanName;

  @Schema(
      required = false,
      description =
          "When set to true, Assembly will be invoked during Unpublishing operations for this"
              + " DeliveryType")
  private boolean unpublishingRequiresAssembly;

  public DeliveryType() {}

  public Optional<Guid> getId() {
    return Optional.ofNullable(id);
  }

  public void setId(Guid id) {
    this.id = id;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Optional<String> getBeanName() {
    return Optional.ofNullable(beanName);
  }

  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  public boolean isUnpublishingRequiresAssembly() {
    return unpublishingRequiresAssembly;
  }

  public void setUnpublishingRequiresAssembly(boolean unpublishingRequiresAssembly) {
    this.unpublishingRequiresAssembly = unpublishingRequiresAssembly;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DeliveryType)) return false;
    var that = (DeliveryType) o;
    return unpublishingRequiresAssembly == that.unpublishingRequiresAssembly
        && Objects.equals(id, that.id)
        && Objects.equals(name, that.name)
        && Objects.equals(description, that.description)
        && Objects.equals(beanName, that.beanName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, beanName, unpublishingRequiresAssembly);
  }

  @Override
  public String toString() {
    return "DeliveryType{"
        + "id="
        + id
        + ", name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + ", beanName='"
        + beanName
        + '\''
        + ", unpublishingRequiresAssembly="
        + unpublishingRequiresAssembly
        + '}';
  }
}

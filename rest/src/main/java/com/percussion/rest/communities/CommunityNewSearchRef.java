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

package com.percussion.rest.communities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * Search identity used in community new-search default GET/PUT (UI-09). PUT accepts {@code name},
 * numeric {@code id}, or {@code guid} (same keys as {@code /services/searches/{idOrName}}).
 */
@XmlRootElement(name = "CommunityNewSearchRef")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Search assigned as a Content Explorer new-search default for a community")
public class CommunityNewSearchRef {

  @Schema(description = "Search design GUID")
  private Guid guid;

  @Schema(description = "Numeric search id (UUID portion)")
  private int id;

  @Schema(description = "Internal search name (catalog key)")
  private String name;

  @Schema(description = "Display label")
  private String label;

  public CommunityNewSearchRef() {}

  public Guid getGuid() {
    return guid;
  }

  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CommunityNewSearchRef that)) {
      return false;
    }
    return id == that.id
        && Objects.equals(guid, that.guid)
        && Objects.equals(name, that.name)
        && Objects.equals(label, that.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, id, name, label);
  }
}

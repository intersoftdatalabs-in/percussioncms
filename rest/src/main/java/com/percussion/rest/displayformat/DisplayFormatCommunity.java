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

package com.percussion.rest.displayformat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/** One allowed community on a display format (GUID string + name). */
@XmlRootElement(name = "DisplayFormatCommunity")
@Schema(description = "Allowed community on a display format.")
public class DisplayFormatCommunity {

  @Schema(description = "Community GUID string (host-type-uuid) or numeric id.")
  private String guid;

  @Schema(description = "Community name.")
  private String name;

  public DisplayFormatCommunity() {}

  public DisplayFormatCommunity(String guid, String name) {
    this.guid = guid;
    this.name = name;
  }

  public String getGuid() {
    return guid;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DisplayFormatCommunity that)) {
      return false;
    }
    return Objects.equals(guid, that.guid) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, name);
  }
}

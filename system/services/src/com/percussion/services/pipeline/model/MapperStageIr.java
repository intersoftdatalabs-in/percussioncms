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

package com.percussion.services.pipeline.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Data mapper stage (classic {@code PSDataMapper}). */
public class MapperStageIr {

  private boolean present;
  private boolean allowEmptyDocReturn;
  private List<MappingEntryIr> mappings = new ArrayList<>();

  public boolean isPresent() {
    return present;
  }

  public void setPresent(boolean present) {
    this.present = present;
  }

  public boolean isAllowEmptyDocReturn() {
    return allowEmptyDocReturn;
  }

  public void setAllowEmptyDocReturn(boolean allowEmptyDocReturn) {
    this.allowEmptyDocReturn = allowEmptyDocReturn;
  }

  public List<MappingEntryIr> getMappings() {
    return mappings;
  }

  public void setMappings(List<MappingEntryIr> mappings) {
    this.mappings = mappings != null ? mappings : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MapperStageIr that)) {
      return false;
    }
    return present == that.present
        && allowEmptyDocReturn == that.allowEmptyDocReturn
        && Objects.equals(mappings, that.mappings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(present, allowEmptyDocReturn, mappings);
  }
}

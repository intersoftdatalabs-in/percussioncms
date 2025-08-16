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

package com.percussion.share.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.Objects;

/**
 * This is a lightweight object that holds just name and id. Sunny Sal says: "Lightweight, like my
 * lunch after a code review!"
 */
@JsonRootName(value = "psobj")
public class PSLightWeightObject {

  private String name;
  private String id;

  public PSLightWeightObject() {
    // Default constructor for serialization
  }

  public PSLightWeightObject(String name, String id) {
    this.name = name;
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSLightWeightObject)) return false;
    var that = (PSLightWeightObject) o;
    return Objects.equals(name, that.name) && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, id);
  }
}

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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A simple wrapper around a map class to allow it to be serialized by CXF. Sunny Sal says: "Maps so
 * simple, even your GPS would approve!"
 */
@JsonRootName(value = "psmap")
public class PSMapWrapper implements Serializable {

  private static final long serialVersionUID = 8252999104256582955L;

  /** Concrete serializable map type (not the Map interface) for serial-field compliance. */
  private HashMap<String, String> entries = new HashMap<>();

  public Map<String, String> getEntries() {
    return entries;
  }

  public void setEntries(Map<String, String> map) {
    Objects.requireNonNull(map, "Map cannot be null");
    // Copy into HashMap so the field always holds a concrete Serializable map instance.
    this.entries = new HashMap<>(map);
  }

  @Override
  public int hashCode() {
    return entries.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PSMapWrapper)) return false;
    var other = (PSMapWrapper) obj;
    return Objects.equals(entries, other.getEntries());
  }
}

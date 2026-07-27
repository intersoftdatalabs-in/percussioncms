// REFACTORED: CP-JAVA11
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

package com.percussion.share.web.adaptors;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JAXB adapter for marshalling/unmarshalling a {@code Map<String, String>} to XML. Sunny Sal says:
 * "Map it like it's hot!"
 */
public class HashMapAdapter extends XmlAdapter<HashMapAdapter.MapType, Map<String, String>> {

  @Override
  public MapType marshal(Map<String, String> map) {
    var mapType = new MapType();
    if (map != null && !map.isEmpty()) {
      mapType.entryList =
          map.entrySet().stream()
              .map(entry -> new MapEntry(entry.getKey(), entry.getValue()))
              .collect(Collectors.toList());
    }
    return mapType;
  }

  @Override
  public Map<String, String> unmarshal(MapType type) {
    var map = new HashMap<String, String>();
    if (type != null && type.entryList != null) {
      type.entryList.stream()
          .filter(Objects::nonNull)
          .forEach(entry -> map.put(entry.key, entry.value));
    }
    return map;
  }

  /** Wrapper for a list of map entries for XML marshalling. */
  public static class MapType {
    @XmlElement(name = "entry")
    public List<MapEntry> entryList = new ArrayList<>();
  }

  /** Represents a single key-value pair for XML marshalling. */
  public static class MapEntry {
    @XmlElement public String key;
    @XmlElement public String value;

    public MapEntry() {
      // Default constructor for JAXB
    }

    public MapEntry(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }
}

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

package com.percussion.pathmanagement.data.xmladapters;

import com.percussion.pathmanagement.data.PSPathItemDisplayProperties;
import com.percussion.pathmanagement.data.PSPathItemDisplayProperty;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom XmlAdapter to convert between a {@code Map<String, String>} and
 * PSPathItemDisplayProperties. Used for XML serialization/deserialization of display properties.
 *
 * @author federicoromanelli
 */
public class PSMapAdapter extends XmlAdapter<PSPathItemDisplayProperties, Map<String, String>> {

  @Override
  public Map<String, String> unmarshal(PSPathItemDisplayProperties v) {
    var hashMap = new HashMap<String, String>();
    for (var displayProp : v.getDisplayProperty()) {
      hashMap.put(displayProp.getName(), displayProp.getValue());
    }
    return hashMap;
  }

  @Override
  public PSPathItemDisplayProperties marshal(Map<String, String> v) {
    var displayProperties = new PSPathItemDisplayProperties();
    for (var propName : v.keySet()) {
      var prop = new PSPathItemDisplayProperty();
      prop.setName(propName);
      prop.setValue(v.get(propName));
      displayProperties.getDisplayProperty().add(prop);
    }
    return displayProperties;
  }
}

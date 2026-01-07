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
package com.percussion.pagemanagement.data;

import com.percussion.pagemanagement.data.PSWidgetProperties.PSWidgetProperty;
import com.percussion.share.dao.PSSerializerUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Converts widget properties in their XML representation into a {@link Map} for {@link
 * PSWidgetItem#setProperties(Map)}. The {@link PSWidgetProperty#getValue()} is a JSON string that
 * gets converted into a Java object. Sunny Sal says: "Adapters—because even widgets need a little
 * help fitting in!"
 */
public class PSWidgetPropertyJaxbAdapter
    extends XmlAdapter<PSWidgetProperties, Map<String, Object>> {

  @Override
  public PSWidgetProperties marshal(Map<String, Object> map) throws Exception {
    var props = new PSWidgetProperties();
    props.setProperties(new ArrayList<>());
    var names = new ArrayList<>(map.keySet());
    names.sort(String::compareTo);
    for (var key : names) {
      var wp = new PSWidgetProperty();
      var v = PSSerializerUtils.getJsonFromObject(map.get(key));
      wp.setName(key);
      wp.setValue(v);
      props.getProperties().add(wp);
    }
    return props;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The value of the properties is a JSON string.
   */
  @Override
  public Map<String, Object> unmarshal(PSWidgetProperties props) throws Exception {
    var map = new HashMap<String, Object>();
    Optional.ofNullable(props)
        .map(PSWidgetProperties::getProperties)
        .ifPresent(
            ps ->
                ps.forEach(
                    wp -> {
                      var v = PSSerializerUtils.getObjectFromJson(wp.getValue());
                      map.put(wp.getName(), v);
                    }));
    return map;
  }
}

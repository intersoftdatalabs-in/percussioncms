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
package com.percussion.share.test;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.share.dao.PSSerializerUtils;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

/**
 * Functions to test data objects.
 *
 * @author adamgent
 */
public class PSDataObjectTestUtils {

  public static class DataObjectXmlTestResults<T> {
    public String expectedXml;
    public String actualXml;
    public T original;
    public T actualSerialized;
  }

  @SuppressWarnings("unchecked")
  public static <T> DataObjectXmlTestResults<T> doXmlSerialization(T object) {
    var s = PSSerializerUtils.marshal(object);
    var klass = (Class<T>) object.getClass();
    var copy = PSSerializerUtils.unmarshal(s, klass);
    var sCopy = PSSerializerUtils.marshal(copy);

    var r = new DataObjectXmlTestResults<T>();
    r.original = object;
    r.actualSerialized = copy;
    r.expectedXml = s;
    r.actualXml = sCopy;

    return r;
  }

  public static <T> void assertXmlSerialization(T object) {
    var r = doXmlSerialization(object);
    assertEquals("Expected Xml serialization to be the same", r.expectedXml, r.actualXml);
  }

  public static <T> void assertEqualsMethod(T object) {
    var r = doXmlSerialization(object);
    assertEquals("Expected serialized object to be equal", r.original, r.actualSerialized);
  }

  @SuppressWarnings("unchecked")
  public static <T> void fillObject(T bean) {
    var props = getPropertiesOfType(bean, String.class);
    props.replaceAll((k, v) -> v == null ? "test" : v);
    try {
      var map = BeanUtils.describe(bean);
      map.putAll(props);
      BeanUtils.populate(bean, props);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T, P> Map<String, P> getPropertiesOfType(T bean, Class<P> pt) {
    try {
      var props = PropertyUtils.getPropertyDescriptors(bean);
      var map = BeanUtils.describe(bean);
      var defaults = new HashMap<String, P>();
      for (var pd : props) {
        if (pt.equals(pd.getPropertyType())) {
          defaults.put(pd.getName(), (P) map.get(pd.getName()));
        }
      }
      return defaults;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

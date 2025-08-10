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
package com.percussion.share.dao;

import static com.percussion.share.test.PSMatchers.emptyString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.util.PSPurgableTempFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Scenario description: Test PSJcrNodeMap behavior. Sunny Sal: "JCR node map, Java 11, and map ka
 * hero!"
 */
public class PSJcrNodeMapTest {

  private PSJcrNodeMap sut;
  private Node collaborator;
  private PropertyIterator pi;

  @BeforeEach
  void setUp() throws Exception {
    collaborator = Mockito.mock(Node.class);
    sut = new PSJcrNodeMap(collaborator);
    pi = Mockito.mock(PropertyIterator.class);
  }

  @Test
  void shouldGetFromNode() throws Exception {
    var property = expectProperty("testKey", "testValue", PropertyType.STRING);
    Mockito.when(collaborator.hasProperty("testKey")).thenReturn(true);
    Mockito.when(collaborator.getProperty("testKey")).thenReturn(property);
    Mockito.when(property.getString()).thenReturn("testValue");
    Mockito.when(property.getType()).thenReturn(PropertyType.STRING);

    var actual = (String) sut.get("testKey");
    assertEquals("testValue", actual);
  }

  @Test
  void shouldGetFromOverride() throws Exception {
    sut.put("testKey", "fromOverride");
    var actual = (String) sut.get("testKey");
    assertEquals("fromOverride", actual);

    PSPurgableTempFile ptf = null;
    try {
      ptf = new PSPurgableTempFile("tmp", null, null);
      sut.put("testBinary", ptf);
      assertEquals(ptf, sut.get("testBinary"));
    } finally {
      if (ptf != null) {
        ptf.delete();
      }
    }
  }

  @Test
  void shouldGetEmptyStringForBinary() throws Exception {
    var property = expectProperty("testKey", "testValue", PropertyType.BINARY);
    Mockito.when(collaborator.hasProperty("testKey")).thenReturn(true);
    Mockito.when(collaborator.getProperty("testKey")).thenReturn(property);
    Mockito.when(property.getType()).thenReturn(PropertyType.BINARY);

    var actual = (String) sut.get("testKey");
    assertThat(actual, is(emptyString()));
  }

  @Test
  void shouldGetEntrySet() throws Exception {
    var propertyA = expectProperty("a", "A", PropertyType.STRING);
    var propertyB = expectProperty("b", "B", PropertyType.STRING);

    Mockito.when(collaborator.getProperties()).thenReturn(pi);
    Mockito.when(pi.hasNext()).thenReturn(true, true, false);
    Mockito.when(pi.nextProperty()).thenReturn(propertyA, propertyB);

    Mockito.when(propertyA.getName()).thenReturn("a");
    Mockito.when(propertyA.getType()).thenReturn(PropertyType.STRING);
    Mockito.when(propertyA.getString()).thenReturn("A");

    Mockito.when(propertyB.getName()).thenReturn("b");
    Mockito.when(propertyB.getType()).thenReturn(PropertyType.STRING);
    Mockito.when(propertyB.getString()).thenReturn("B");

    Set<Entry<String, Object>> set = sut.entrySet();
    Map<String, Object> expected = new HashMap<>();
    expected.put("a", "A");
    expected.put("b", "B");
    Map<String, Object> actual = new HashMap<>();
    for (Entry<String, Object> e : set) {
      actual.put(e.getKey(), e.getValue());
    }
    assertEquals(expected, actual);
  }

  private Property expectProperty(String name, String value, int type) throws Exception {
    var property = Mockito.mock(Property.class, name);
    Mockito.when(property.getName()).thenReturn(name);
    Mockito.when(property.getType()).thenReturn(type);
    if (type != PropertyType.BINARY) {
      Mockito.when(property.getString()).thenReturn(value);
    }
    return property;
  }
}

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
package com.percussion.fastforward.managednav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.junit.jupiter.api.Test;

/**
 * GET section reads {@code no_externalurl} through {@link PSManagedNavService#readNavonProperty}.
 * Assembly stores {@code rx:no_externalUrl}. A blank first token must not hide the URL (#4985).
 */
class PSManagedNavServiceReadNavonPropertyTest {

  @Test
  void readsCamelCaseExternalUrlWhenRequestedLowercase() throws Exception {
    Node node = nodeWithOnly("rx:no_externalUrl", "https://example.com/edited");

    assertEquals(
        "https://example.com/edited",
        PSManagedNavService.readNavonProperty(node, "no_externalurl"));
  }

  @Test
  void skipsBlankDelimitedStarter() throws Exception {
    Node node = nodeWithOnly("rx:no_externalurl", ";https://example.com/edited");

    assertEquals(
        "https://example.com/edited",
        PSManagedNavService.readNavonProperty(node, "no_externalurl"));
  }

  @Test
  void readsFirstNonBlankMultiValue() throws Exception {
    Node node = mock(Node.class);
    Property property = mock(Property.class);
    Value blank = mock(Value.class);
    Value url = mock(Value.class);
    when(blank.getString()).thenReturn("");
    when(url.getString()).thenReturn("https://example.com/edited");
    when(node.hasProperty("no_externalurl")).thenReturn(true);
    when(node.getProperty("no_externalurl")).thenReturn(property);
    when(property.isMultiple()).thenReturn(true);
    when(property.getValues()).thenReturn(new Value[] {blank, url});

    assertEquals(
        "https://example.com/edited",
        PSManagedNavService.readNavonProperty(node, "no_externalurl"));
  }

  @Test
  void missingPropertyStaysAbsent() throws Exception {
    Node node = mock(Node.class);
    PropertyIterator empty = mock(PropertyIterator.class);
    when(node.hasProperty("no_externalurl")).thenReturn(false);
    when(node.hasProperty("rx:no_externalurl")).thenReturn(false);
    when(node.getProperties()).thenReturn(empty);
    when(empty.hasNext()).thenReturn(false);

    assertNull(PSManagedNavService.readNavonProperty(node, "no_externalurl"));
  }

  private static Node nodeWithOnly(String propertyName, String text) throws RepositoryException {
    Node node = mock(Node.class);
    Property property = mock(Property.class);
    PropertyIterator iterator = mock(PropertyIterator.class);
    when(node.hasProperty(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
    when(node.getProperties()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.nextProperty()).thenReturn(property);
    when(property.getName()).thenReturn(propertyName);
    when(property.isMultiple()).thenReturn(false);
    when(property.getString()).thenReturn(text);
    return node;
  }
}

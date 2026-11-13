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
package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.util.PSCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for #6132: {@link PSCollectionComponent} and its subclasses are parameterized
 * ({@code PSCollectionComponent<E>}), and the legacy raw APIs ({@code
 * PSWorkflowInfo.getWorkflowIds()}, {@code PSContentEditorSharedDef.getFieldGroups()}) are typed.
 * Runtime member-class checks stay intact.
 */
public class PSCollectionComponentGenericsTest {

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void propertySetAddIsCheckedAndMemberClassEnforced() {
    PSPropertySet set = new PSPropertySet();
    PSProperty prop = new PSProperty("sample");
    assertTrue(set.add(prop));
    assertSame(prop, set.get(0));
    // runtime member-class check stays: raw-handle wrong-type add still throws
    PSCollection raw = set;
    assertThrows(ClassCastException.class, () -> raw.add("not-a-property"));
    assertEquals(1, set.size());
  }

  @Test
  public void copyFromCopiesElementsBetweenSameTypedCollections() {
    PSPropertySet src = new PSPropertySet();
    src.add(new PSProperty("a"));
    src.add(new PSProperty("b"));
    PSPropertySet copy = new PSPropertySet();
    copy.copyFrom(src);
    assertEquals(2, copy.size());
    assertEquals("a", copy.get(0).getName());
  }

  @Test
  public void relationshipSetIteratorIsTyped() {
    PSRelationshipSet set = new PSRelationshipSet();
    Iterator<PSRelationship> it = set.iterator();
    assertNotNull(it);
    assertFalse(it.hasNext());
  }

  @Test
  public void workflowIdsAreLiveTypedIntegers() {
    PSWorkflowInfo info = new PSWorkflowInfo(PSWorkflowInfo.TYPE_INCLUSIONARY, new ArrayList<>());
    List<Integer> ids = info.getWorkflowIds();
    assertTrue(ids.isEmpty());
    ids.add(4);
    assertTrue(info.getWorkflowIds().contains(4));
  }

  @Test
  public void fieldGroupsIterateAsSharedFieldGroups() {
    PSContentEditorSharedDef def = new PSContentEditorSharedDef();
    def.addFieldGroup(new PSSharedFieldGroup("g", "g.xml"));
    Iterator<PSSharedFieldGroup> groups = def.getFieldGroups();
    assertTrue(groups.hasNext());
    assertEquals("g.xml", groups.next().getFilename());
  }
}

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
package com.percussion.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.IPSGroupProviderInstance;
import com.percussion.design.objectstore.PSAttribute;
import com.percussion.design.objectstore.PSAttributeList;
import com.percussion.design.objectstore.PSDirectory;
import com.percussion.design.objectstore.PSDirectorySet;
import com.percussion.design.objectstore.PSGlobalSubject;
import com.percussion.design.objectstore.PSJndiGroupProviderInstance;
import com.percussion.design.objectstore.PSJndiObjectClass;
import com.percussion.design.objectstore.PSLiteral;
import com.percussion.design.objectstore.PSLiteralSet;
import com.percussion.design.objectstore.PSReference;
import com.percussion.design.objectstore.PSSubject;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.util.PSCollection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for {@link PSCatalogerTypes} adapters used by security catalogers (issue #3289,
 * parent #2299 / #2022).
 */
@Tag("UnitTest")
class PSCatalogerTypesTest {

  @Test
  void typedIteratorPreservesNullsAndThrowsOnWrongType() {
    List<Object> withNull = new ArrayList<>();
    withNull.add("alice");
    withNull.add(null);
    withNull.add("bob");

    assertEquals(
        java.util.Arrays.asList("alice", null, "bob"),
        PSCatalogerTypes.collect(PSCatalogerTypes.typed(withNull.iterator(), String.class)));

    List<Object> mixed = new ArrayList<>();
    mixed.add("alice");
    mixed.add(Integer.valueOf(42));
    Iterator<String> it = PSCatalogerTypes.typed(mixed.iterator(), String.class);
    assertEquals("alice", it.next());
    assertThrows(ClassCastException.class, it::next);
  }

  @Test
  void typedIteratorThrowsWhenExhausted() {
    Iterator<String> empty = PSCatalogerTypes.typed(List.of().iterator(), String.class);
    assertThrows(NoSuchElementException.class, empty::next);
  }

  @Test
  void attributesIteratorReturnsNamedAttributes() {
    PSAttributeList attrs = new PSAttributeList();
    attrs.setAttribute("email", List.of("alice@example.com"));
    attrs.setAttribute("dept", List.of("Engineering"));

    List<PSAttribute> found = PSCatalogerTypes.attributeList(attrs);
    assertEquals(2, found.size());
    assertEquals("email", found.get(0).getName());
    assertEquals(List.of("alice@example.com"), found.get(0).getValues());
    assertEquals("dept", found.get(1).getName());
  }

  @Test
  void directoryRefsIteratorYieldsReferences() {
    PSDirectorySet set = new PSDirectorySet("corp", "uid");
    set.add(new PSReference("dir1", PSDirectory.class.getName()));
    set.add(new PSReference("dir2", PSDirectory.class.getName()));

    List<PSReference> refs = PSCatalogerTypes.collect(PSCatalogerTypes.directoryRefs(set));
    assertEquals(2, refs.size());
    assertEquals("dir1", refs.get(0).getName());
    assertEquals("dir2", refs.get(1).getName());
  }

  @Test
  void groupProviderInstancesIteratorYieldsInstances() {
    PSCollection raw = new PSCollection(IPSGroupProviderInstance.class);
    raw.add(new PSJndiGroupProviderInstance("groups", PSSecurityProvider.SP_TYPE_DIRCONN));

    List<IPSGroupProviderInstance> found =
        PSCatalogerTypes.collect(PSCatalogerTypes.groupProviderInstances(raw));
    assertEquals(1, found.size());
    assertEquals("groups", found.get(0).getName());
  }

  @Test
  void literalsIteratorYieldsValueText() {
    PSLiteralSet literals = new PSLiteralSet(PSTextLiteral.class);
    literals.add(new PSTextLiteral(PSSecurityProvider.INTERNAL_USER_NAME));
    literals.add(new PSTextLiteral("alice"));

    List<String> names = new ArrayList<>();
    Iterator<PSLiteral> it = PSCatalogerTypes.literals(literals);
    while (it.hasNext()) {
      names.add(it.next().getValueText());
    }

    assertEquals(List.of(PSSecurityProvider.INTERNAL_USER_NAME, "alice"), names);
  }

  @Test
  void stringsIteratorWrapsRawObjectstoreNames() {
    PSJndiGroupProviderInstance gp =
        new PSJndiGroupProviderInstance("groups", PSSecurityProvider.SP_TYPE_DIRCONN);
    gp.addGroupNode("ou=groups,dc=example,dc=com");
    gp.addObjectClass("groupOfNames", "member", PSJndiObjectClass.MEMBER_ATTR_STATIC);

    List<String> nodes = PSCatalogerTypes.collect(PSCatalogerTypes.strings(gp.getGroupNodes()));
    assertEquals(List.of("ou=groups,dc=example,dc=com"), nodes);

    List<String> ocs = PSCatalogerTypes.collect(PSCatalogerTypes.strings(gp.getObjectClassesNames()));
    assertEquals(List.of("groupOfNames"), ocs);
  }

  @Test
  void subjectIdentifierComparatorOrdersByNameThenType() {
    Comparator<PSSubject> cmp = PSCatalogerTypes.subjectIdentifierComparator();

    PSSubject aliceUser = subject("alice", PSSubject.SUBJECT_TYPE_USER);
    PSSubject aliceGroup = subject("alice", PSSubject.SUBJECT_TYPE_GROUP);
    PSSubject bobUser = subject("bob", PSSubject.SUBJECT_TYPE_USER);

    assertTrue(cmp.compare(aliceUser, bobUser) < 0);
    assertTrue(cmp.compare(bobUser, aliceUser) > 0);
    assertEquals(0, cmp.compare(aliceUser, subject("alice", PSSubject.SUBJECT_TYPE_USER)));
    assertTrue(cmp.compare(aliceUser, aliceGroup) != 0);

    Set<PSSubject> ordered = new TreeSet<>(cmp);
    ordered.add(bobUser);
    ordered.add(aliceGroup);
    ordered.add(aliceUser);
    List<String> keys = new ArrayList<>();
    for (PSSubject s : ordered) {
      keys.add(s.getName() + ":" + s.getType());
    }
    assertEquals(3, keys.size());
    assertEquals("alice", keys.get(0).substring(0, 5));
    assertEquals("alice", keys.get(1).substring(0, 5));
    assertEquals("bob:" + PSSubject.SUBJECT_TYPE_USER, keys.get(2));
  }

  @Test
  void subjectIdentifierComparatorTreatsCaseVariantsAsDistinct() {
    Comparator<PSSubject> cmp = PSCatalogerTypes.subjectIdentifierComparator();
    PSSubject lower = subject("alice", PSSubject.SUBJECT_TYPE_USER);
    PSSubject mixed = subject("Alice", PSSubject.SUBJECT_TYPE_USER);

    assertTrue(cmp.compare(lower, mixed) != 0);

    Set<PSSubject> set = new TreeSet<>(cmp);
    set.add(lower);
    set.add(mixed);
    assertEquals(2, set.size());
  }

  @Test
  void adaptersRejectNullSources() {
    assertThrows(NullPointerException.class, () -> PSCatalogerTypes.attributes(null));
    assertThrows(NullPointerException.class, () -> PSCatalogerTypes.directoryRefs(null));
    assertThrows(NullPointerException.class, () -> PSCatalogerTypes.groupProviderInstances(null));
    assertThrows(NullPointerException.class, () -> PSCatalogerTypes.literals(null));
    assertThrows(NullPointerException.class, () -> PSCatalogerTypes.strings(null));
    assertThrows(NullPointerException.class, () -> PSCatalogerTypes.typed(null, String.class));
    assertThrows(
        NullPointerException.class, () -> PSCatalogerTypes.typed(List.of().iterator(), null));
  }

  private static PSSubject subject(String name, int type) {
    return new PSGlobalSubject(name, type, new PSAttributeList());
  }
}

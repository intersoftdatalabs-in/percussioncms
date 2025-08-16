package com.percussion.security.shim.acl.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.security.Principal;
import java.util.Enumeration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupImplTest {
  private GroupImpl group;
  private Principal alice;
  private Principal bob;

  @BeforeEach
  void setUp() {
    group = new GroupImpl("testers");
    alice = () -> "Alice";
    bob = () -> "Bob";
  }

  @Test
  void testAddMember() {
    assertTrue(group.addMember(alice));
    assertTrue(group.isMember(alice));
    assertFalse(group.isMember(bob));
  }

  @Test
  void testRemoveMember() {
    group.addMember(alice);
    assertTrue(group.removeMember(alice));
    assertFalse(group.isMember(alice));
  }

  @Test
  void testDuplicateAdd() {
    assertTrue(group.addMember(alice));
    assertFalse(group.addMember(alice));
  }

  @Test
  void testMembersEnumeration() {
    group.addMember(alice);
    group.addMember(bob);
    Enumeration<? extends Principal> membersEnum = group.members();
    int count = 0;
    while (membersEnum.hasMoreElements()) {
      Principal p = membersEnum.nextElement();
      assertTrue(p.equals(alice) || p.equals(bob));
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  void testGetName() {
    assertEquals("testers", group.getName());
  }
}

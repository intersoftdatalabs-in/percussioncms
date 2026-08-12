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
package com.percussion.services.security;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.security.PSGroupEntry;
import com.percussion.security.PSRoleEntry;
import com.percussion.security.PSUserAttributes;
import com.percussion.security.PSUserEntry;
import com.percussion.security.shim.acl.Group;
import com.percussion.services.security.loginmods.data.PSGroup;
import com.percussion.services.security.loginmods.data.PSPrincipal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import javax.security.auth.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for utils
 *
 * @author dougrand
 */
public class PSJaasUtilsTest {

  @BeforeEach
  protected void setUp() throws Exception {
    ms_testData = new ArrayList<Principal>();
    ms_testRoleGroup = new PSGroup("Roles");
  }

  /** Test data intiialized during {@link #setUp()}, never <code>null</code> after that. */
  static Collection<Principal> ms_testData;

  /** Test group initialized during {@link #setUp()}, never <code>null</code> after that. */
  static Group ms_testRoleGroup;

  /**
   * Test locating the group containing role names.
   *
   * @throws Exception if the test failes.
   */
  @Test
  public void testFindRoleGroup() throws Exception {
    ms_testData.add(ms_testRoleGroup);

    Group r = PSJaasUtils.findOrCreateGroup(ms_testData, PSJaasUtils.ROLE_GROUP_NAME);
    assertTrue(r == ms_testRoleGroup);

    ms_testData.remove(ms_testRoleGroup);

    r = PSJaasUtils.findOrCreateGroup(ms_testData, PSJaasUtils.ROLE_GROUP_NAME);
    assertTrue(!(r == ms_testRoleGroup));
  }

  /**
   * Test various collection operations.
   *
   * @throws Exception if the test fails.
   */
  @Test
  public void testCollectionResults() throws Exception {
    ms_testData.add(new PSPrincipal("user"));
    ms_testData.add(new PSPrincipal("grape"));
    ms_testData.add(new PSPrincipal("orange"));

    Principal u = PSJaasUtils.findFirstPSPrincipal(ms_testData);
    assertTrue(u == ms_testData.iterator().next());
    assertEquals("user", u.getName());
  }

  /**
   * Test converting a subject to a user entry
   *
   * @throws Exception if the test fails.
   */
  @Test
  public void testSubjectToEntry() throws Exception {
    PSGroupEntry group1 = new PSGroupEntry("group1", 0);
    PSGroupEntry group2 = new PSGroupEntry("group2", 0);
    PSGroupEntry[] groups = new PSGroupEntry[] {group1, group2};

    PSRoleEntry role1 = new PSRoleEntry("role1", 0);
    PSRoleEntry role2 = new PSRoleEntry("role2", 0);
    PSRoleEntry[] roles = new PSRoleEntry[] {role1, role2};

    PSUserAttributes attrs = new PSUserAttributes();
    attrs.put("attr1", "attval1");
    attrs.put("attr2", "attval2");

    String username = "admin1";
    String pwd = "demo";
    PSUserEntry entry =
        new PSUserEntry(
            username, 0, groups, roles, attrs, PSUserEntry.createSignature(username, pwd));
    Subject sub = PSJaasUtils.userEntryToSubject(entry, pwd);
    PSUserEntry entry2 = PSJaasUtils.subjectToUserEntry(sub, username, pwd);
    assertEquals(entry, entry2);
    assertEquals(sub, PSJaasUtils.userEntryToSubject(entry2, pwd));

    entry =
        new PSUserEntry(
            username,
            0,
            groups,
            roles,
            new PSUserAttributes(),
            PSUserEntry.createSignature(username, pwd));
    assertFalse(sub.equals(PSJaasUtils.userEntryToSubject(entry, pwd)));

    sub.getPrincipals().clear();
    assertFalse(entry2.equals(PSJaasUtils.subjectToUserEntry(sub, username, pwd)));

    entry =
        new PSUserEntry(
            username,
            0,
            groups,
            new PSRoleEntry[0],
            attrs,
            PSUserEntry.createSignature(username, pwd));
    assertFalse(sub.equals(PSJaasUtils.userEntryToSubject(entry, pwd)));

    sub.getPrincipals().clear();
    assertFalse(entry2.equals(PSJaasUtils.subjectToUserEntry(sub, username, pwd)));
  }
}

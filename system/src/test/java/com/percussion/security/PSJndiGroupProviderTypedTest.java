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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSAuthentication;
import com.percussion.design.objectstore.PSDirectory;
import com.percussion.design.objectstore.PSJndiGroupProviderInstance;
import com.percussion.design.objectstore.PSJndiObjectClass;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.NamingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@link PSJndiGroupProvider} internals (issue #2461 residual of #2386
 * / parent epic #2022). Covers pure logic that does not require a live LDAP directory.
 */
@Tag("UnitTest")
public class PSJndiGroupProviderTypedTest {

  private static final String PROVIDER_URL = "ldap://localhost:389/dc=example,dc=com";

  private PSJndiGroupProvider provider;

  @BeforeEach
  void setUp() throws NamingException {
    PSJndiGroupProviderInstance gp =
        new PSJndiGroupProviderInstance("test-groups", PSSecurityProvider.SP_TYPE_DIRCONN);
    gp.addObjectClass("groupOfNames", "member", PSJndiObjectClass.MEMBER_ATTR_STATIC);
    gp.addGroupNode("ou=groups,dc=example,dc=com");

    PSAuthentication auth =
        new PSAuthentication(
            "auth", PSAuthentication.SCHEME_SIMPLE, "cn=admin", "secret", null, null);
    PSDirectory dir =
        new PSDirectory(
            "dir",
            PSDirectory.CATALOG_SHALLOW,
            "com.sun.jndi.ldap.LdapCtxFactory",
            "auth",
            PROVIDER_URL,
            null);
    PSDirectoryDefinition dirDef = new PSDirectoryDefinition(auth, dir);

    provider = new PSJndiGroupProvider(gp, dirDef, "uid");
  }

  @Test
  void parseSearchUrlSplitsBaseAndFilter() {
    StringBuilder base = new StringBuilder();
    String filter =
        PSJndiGroupProvider.parseSearchUrl("ldap:///ou=percussion,c=us??sub?(cn=m*)", base);
    assertEquals("ou=percussion,c=us", base.toString());
    assertEquals("(cn=m*)", filter);
  }

  @Test
  void parseSearchUrlHandlesMissingFilterAndPrefix() {
    StringBuilder base = new StringBuilder();
    String filter = PSJndiGroupProvider.parseSearchUrl("ou=people,dc=example,dc=com", base);
    assertEquals("ou=people,dc=example,dc=com", base.toString());
    assertEquals("", filter);
  }

  @Test
  void isGroupSupportedMatchesConfiguredLocationSuffix() {
    assertTrue(provider.isGroupSupported("cn=Admins,ou=groups,dc=example,dc=com"));
    assertFalse(provider.isGroupSupported("cn=Admins,ou=other,dc=example,dc=com"));
  }

  @Test
  void isGroupSupportedRejectsNullOrEmpty() {
    assertThrows(IllegalArgumentException.class, () -> provider.isGroupSupported(null));
    assertThrows(IllegalArgumentException.class, () -> provider.isGroupSupported(""));
    assertThrows(IllegalArgumentException.class, () -> provider.isGroupSupported("   "));
  }

  @Test
  void shouldTreatAsGroupFalseWhenNotSupportedAndUserAttrDiffersFromCn() throws NamingException {
    // user object attr is uid (not cn), DN not under group locations → user
    assertFalse(provider.shouldTreatAsGroup("uid=alice,ou=people,dc=example,dc=com", false));
  }

  @Test
  void shouldTreatAsGroupTrueForSupportedGroupDnWithDistinctUserAttr() throws NamingException {
    assertTrue(provider.shouldTreatAsGroup("cn=Admins,ou=groups,dc=example,dc=com", false));
  }

  @Test
  void getGroupsReturnsEmptyWhenNoObjectClasses() throws NamingException {
    PSJndiGroupProviderInstance emptyGp =
        new PSJndiGroupProviderInstance("empty", PSSecurityProvider.SP_TYPE_DIRCONN);
    emptyGp.addGroupNode("ou=groups,dc=example,dc=com");

    PSAuthentication auth =
        new PSAuthentication(
            "auth", PSAuthentication.SCHEME_SIMPLE, "cn=admin", "secret", null, null);
    PSDirectory dir =
        new PSDirectory(
            "dir",
            PSDirectory.CATALOG_SHALLOW,
            "com.sun.jndi.ldap.LdapCtxFactory",
            "auth",
            PROVIDER_URL,
            null);
    PSJndiGroupProvider emptyProvider =
        new PSJndiGroupProvider(emptyGp, new PSDirectoryDefinition(auth, dir), "uid");

    Collection<String> groups = emptyProvider.getGroups(null);
    assertTrue(groups.isEmpty());
  }

  @Test
  void getGroupMembersRejectsNullGroups() {
    assertThrows(IllegalArgumentException.class, () -> provider.getGroupMembers(null));
  }

  @Test
  void getGroupMembersSkipsUnsupportedGroupsWithoutLdap() {
    List<java.security.Principal> groups = new ArrayList<>();
    groups.add(() -> "cn=Nope,ou=other,dc=example,dc=com");

    Collection<IPSTypedPrincipal> members = provider.getGroupMembers(groups);
    assertTrue(members.isEmpty());
    // unsupported groups are left in the input collection
    assertEquals(1, groups.size());
  }

  @Test
  void typedAttrMapObjectClassesAreLowercased() throws Exception {
    // Exercise package-private style behavior via filterMemberList path indirectly:
    // verify getObjectClasses semantics by building attr maps the same shape as production.
    Map<String, List<String>> attrMap = new HashMap<>();
    attrMap.put(
        PSJndiProvider.OBJECT_CLASS_ATTR.toLowerCase(),
        List.of("groupOfNames", "top"));

    // Reflect private getObjectClasses to lock typed List<String> contract without LDAP.
    var method =
        PSJndiGroupProvider.class.getDeclaredMethod("getObjectClasses", Map.class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    List<String> ocs = (List<String>) method.invoke(provider, attrMap);
    assertEquals(List.of("groupofnames", "top"), ocs);
  }
}

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
package com.percussion.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.PSAuthenticateUserUtils;
import com.percussion.design.objectstore.PSAttributeList;
import com.percussion.design.objectstore.PSGlobalSubject;
import com.percussion.design.objectstore.PSSubject;
import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.security.IPSPasswordFilter;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.IPSRoleMgr;
import com.percussion.services.security.PSJaasUtils;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.dao.IPSUserLoginDao;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.data.PSUser;
import com.percussion.user.data.PSUserProviderType;
import com.percussion.utils.service.IPSUtilityService;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Self-service default community persist/GET (issue #3508). Production types: {@link
 * PSCurrentUser}, {@link IPSBackEndRoleMgr}, {@link PSAuthenticateUserUtils#SYS_DEFAULTCOMMUNITY}.
 */
class PSUserDefaultCommunityTest {

  private IPSBackEndRoleMgr backEndRoleMgr;
  private PSUserService userService;

  @BeforeEach
  void setUp() {
    backEndRoleMgr = mock(IPSBackEndRoleMgr.class);
    userService =
        spy(
            new PSUserService(
                mock(IPSUserLoginDao.class),
                mock(IPSPasswordFilter.class),
                backEndRoleMgr,
                mock(IPSRoleMgr.class),
                /* notificationService */ null,
                mock(IPSWorkflowService.class),
                mock(IPSSecurityWs.class),
                mock(IPSContentWs.class),
                mock(IPSIdMapper.class),
                mock(IPSUtilityService.class),
                mock(IPSMetadataService.class)));
    IPSSystemProperties systemProps = mock(IPSSystemProperties.class);
    when(systemProps.getProperty("accessibilityRoles")).thenReturn("");
    userService.setSystemProps(systemProps);
  }

  @Test
  void blankClears() {
    assertEquals("", PSUserService.canonicalizeAllowedCommunity("  ", List.of("Default")));
    assertEquals("", PSUserService.canonicalizeAllowedCommunity(null, List.of("Default")));
  }

  @Test
  void returnsCanonicalMembershipSpelling() {
    assertEquals(
        "Enterprise Investments",
        PSUserService.canonicalizeAllowedCommunity(
            "enterprise investments", List.of("Default", "Enterprise Investments")));
  }

  @Test
  void rejectsUnknown() {
    assertNull(PSUserService.canonicalizeAllowedCommunity("Unknown", List.of("Default")));
  }

  @Test
  void persistsAllowedCommunityOnUserSubject() throws Exception {
    PSCurrentUser current = memberUser("Admin", List.of("Default", "Corporate"));
    doReturn(current).when(userService).getCurrentUser();

    PSCurrentUser result = userService.updateMyDefaultCommunity("corporate");

    verify(backEndRoleMgr)
        .setSubjectAttribute(
            eq("Admin"),
            eq(PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY),
            eq("Corporate"));
    assertEquals("Corporate", result.getDefaultCommunity());
  }

  @Test
  void blankClearsStoredDefault() throws Exception {
    PSCurrentUser current = memberUser("Admin", List.of("Default"));
    current.setDefaultCommunity("Default");
    doReturn(current).when(userService).getCurrentUser();

    PSCurrentUser result = userService.updateMyDefaultCommunity("  ");

    verify(backEndRoleMgr)
        .setSubjectAttribute(
            eq("Admin"), eq(PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY), eq(""));
    assertEquals("", result.getDefaultCommunity());
  }

  @Test
  void rejectsCommunityNotInMembership() throws Exception {
    PSCurrentUser current = memberUser("Editor1", List.of("Default"));
    doReturn(current).when(userService).getCurrentUser();

    assertThrows(
        PSValidationException.class, () -> userService.updateMyDefaultCommunity("Unknown"));
    verify(backEndRoleMgr, never())
        .setSubjectAttribute(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void propagatesBackendFailureWithoutApplying() throws Exception {
    PSCurrentUser current = memberUser("Admin", List.of("Default"));
    doReturn(current).when(userService).getCurrentUser();
    doThrow(new RuntimeException("backend write failed"))
        .when(backEndRoleMgr)
        .setSubjectAttribute(
            eq("Admin"), eq(PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY), eq("Default"));

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> userService.updateMyDefaultCommunity("Default"));
    assertEquals("backend write failed", thrown.getMessage());
    assertEquals("", current.getDefaultCommunity());
  }

  @Test
  void applyAllowedStoredDefaultKeepsMembershipSpelling() {
    PSCurrentUser current = memberUser("Admin", List.of("Default", "Corporate"));
    PSUserService.applyAllowedStoredDefault(current, "corporate");
    assertEquals("Corporate", current.getDefaultCommunity());
  }

  @Test
  void applyAllowedStoredDefaultClearsDisallowed() {
    PSCurrentUser current = memberUser("Admin", List.of("Default"));
    PSUserService.applyAllowedStoredDefault(current, "Corporate");
    assertEquals("", current.getDefaultCommunity());
  }

  @Test
  void getCurrentUserOmitsStoredDefaultWhenNotInSessionCommunities() throws Exception {
    PSUser internal = new PSUser();
    internal.setName("Admin");
    internal.setEmail("admin@example.com");
    internal.setProviderType(PSUserProviderType.INTERNAL);
    internal.setRoles(List.of("Admin"));
    doReturn("Admin").when(userService).getCurrentUserName();
    doReturn(internal).when(userService).find("Admin");
    when(backEndRoleMgr.getGlobalSubjectAttributes(
            eq("Admin"), eq(PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY), eq(false)))
        .thenReturn(Set.of(subjectWithDefaultCommunity("Admin", "Corporate")));

    PSCurrentUser result = userService.getCurrentUser();

    // No session membership list in this unit fixture — do not return a stale default.
    assertEquals("", result.getDefaultCommunity());
  }

  private static PSCurrentUser memberUser(String name, List<String> communities) {
    PSCurrentUser user = new PSCurrentUser();
    user.setName(name);
    user.setProviderType(PSUserProviderType.INTERNAL);
    user.setRoles(List.of("Admin"));
    user.setCommunities(communities);
    return user;
  }

  private static Subject subjectWithDefaultCommunity(String name, String community) {
    PSAttributeList attrs = new PSAttributeList();
    attrs.setAttribute(
        PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY, List.of(community));
    PSSubject psSubject = new PSGlobalSubject(name, PSSubject.SUBJECT_TYPE_USER, attrs);
    return PSJaasUtils.convertSubject(psSubject);
  }
}

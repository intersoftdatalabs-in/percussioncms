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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSAttributeList;
import com.percussion.design.objectstore.PSGlobalSubject;
import com.percussion.design.objectstore.PSSubject;
import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.security.IPSPasswordFilter;
import com.percussion.security.PSSecurityCatalogException;
import com.percussion.services.auth.account.IPSAccountSecurityService;
import com.percussion.services.auth.account.PSEmailChangeResult;
import com.percussion.services.auth.account.PSEmailVerificationStatus;
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
import com.percussion.user.data.PSUserAccountUpdate;
import com.percussion.user.data.PSUserProviderType;
import com.percussion.utils.service.IPSUtilityService;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;
import java.util.Collections;
import java.util.List;
import javax.security.auth.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for self-service account profile update (issue #2395 / parent #2374 slice 2).
 *
 * <p>Covers: email validation helper, INTERNAL self email persist, DIRECTORY rejection, null
 * update rejection, and directory email loading on {@code getCurrentUser()}. Self-only is
 * structural (no target user name accepted).
 */
@DisplayName("Self-service account profile (#2395)")
class PSUserAccountProfileTest {

  private IPSBackEndRoleMgr backEndRoleMgr;
  private IPSRoleMgr roleMgr;
  private PSUserService userService;

  @BeforeEach
  void setUp() {
    backEndRoleMgr = mock(IPSBackEndRoleMgr.class);
    roleMgr = mock(IPSRoleMgr.class);
    userService =
        spy(
            new PSUserService(
                mock(IPSUserLoginDao.class),
                mock(IPSPasswordFilter.class),
                backEndRoleMgr,
                roleMgr,
                /* notificationService */ null,
                mock(IPSWorkflowService.class),
                mock(IPSSecurityWs.class),
                mock(IPSContentWs.class),
                mock(IPSIdMapper.class),
                mock(IPSUtilityService.class),
                mock(IPSMetadataService.class)));
    // getCurrentUser() reads accessibility roles via system props
    IPSSystemProperties systemProps = mock(IPSSystemProperties.class);
    when(systemProps.getProperty("accessibilityRoles")).thenReturn("");
    userService.setSystemProps(systemProps);
  }

  @Nested
  @DisplayName("isValidEmailAddress")
  class EmailValidation {

    @Test
    void acceptsCommonAddresses() {
      assertTrue(PSUserService.isValidEmailAddress("user@example.com"));
      assertTrue(PSUserService.isValidEmailAddress("first.last+tag@sub.example.co.uk"));
      assertTrue(PSUserService.isValidEmailAddress("a@b.co"));
    }

    @Test
    void rejectsInvalidShapes() {
      assertFalse(PSUserService.isValidEmailAddress(null));
      assertFalse(PSUserService.isValidEmailAddress(""));
      assertFalse(PSUserService.isValidEmailAddress("   "));
      assertFalse(PSUserService.isValidEmailAddress("not-an-email"));
      assertFalse(PSUserService.isValidEmailAddress("missing-domain@"));
      assertFalse(PSUserService.isValidEmailAddress("@nodomain.com"));
      assertFalse(PSUserService.isValidEmailAddress("spaces emma@example.com"));
      // Domain label hygiene (PR #2593 review): consecutive dots / hyphen edges
      assertFalse(PSUserService.isValidEmailAddress("user@domain..com"));
      assertFalse(PSUserService.isValidEmailAddress("user@-domain.com"));
      assertFalse(PSUserService.isValidEmailAddress("user@domain-.com"));
    }
  }

  @Nested
  @DisplayName("updateMyAccount")
  class UpdateMyAccount {

    @Test
    void persistsEmailForInternalCurrentUser() throws Exception {
      // Single getCurrentUser() — response is the loaded user with email applied (no re-fetch).
      PSCurrentUser current = internalUser("Editor1", "old@example.com");
      doReturn(current).when(userService).getCurrentUser();

      PSUserAccountUpdate update = new PSUserAccountUpdate();
      update.setEmail("new@example.com");

      PSCurrentUser result = userService.updateMyAccount(update);

      ArgumentCaptor<String> emailCap = ArgumentCaptor.forClass(String.class);
      verify(backEndRoleMgr).setSubjectEmail(eq("Editor1"), emailCap.capture());
      assertEquals("new@example.com", emailCap.getValue());
      assertEquals("new@example.com", result.getEmail());
      assertEquals("Editor1", result.getName());
    }

    @Test
    void allowsClearingEmailWhenBlank() throws Exception {
      PSCurrentUser current = internalUser("Admin", "old@example.com");
      doReturn(current).when(userService).getCurrentUser();

      PSUserAccountUpdate update = new PSUserAccountUpdate();
      update.setEmail("  ");

      PSCurrentUser result = userService.updateMyAccount(update);

      verify(backEndRoleMgr).setSubjectEmail("Admin", "");
      assertEquals("", result.getEmail());
    }

    @Test
    void propagatesBackendFailureWithoutSilentSuccess() throws Exception {
      PSCurrentUser current = internalUser("Editor1", "old@example.com");
      doReturn(current).when(userService).getCurrentUser();
      org.mockito.Mockito.doThrow(new RuntimeException("backend write failed"))
          .when(backEndRoleMgr)
          .setSubjectEmail(eq("Editor1"), eq("new@example.com"));

      PSUserAccountUpdate update = new PSUserAccountUpdate();
      update.setEmail("new@example.com");

      RuntimeException thrown =
          assertThrows(RuntimeException.class, () -> userService.updateMyAccount(update));
      assertEquals("backend write failed", thrown.getMessage());
      // Email on the session object must remain pre-update (no apply-after-failure)
      assertEquals("old@example.com", current.getEmail());
    }

    @Test
    void rejectsDirectoryManagedUsers() throws Exception {
      PSCurrentUser current = new PSCurrentUser();
      current.setName("ldap.user");
      current.setProviderType(PSUserProviderType.DIRECTORY);
      current.setEmail("dir@example.com");
      current.setRoles(List.of("Editor"));
      doReturn(current).when(userService).getCurrentUser();

      PSUserAccountUpdate update = new PSUserAccountUpdate();
      update.setEmail("new@example.com");

      assertThrows(PSValidationException.class, () -> userService.updateMyAccount(update));
      verify(backEndRoleMgr, never()).setSubjectEmail(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsInvalidEmailShape() throws Exception {
      PSCurrentUser current = internalUser("Admin", "old@example.com");
      doReturn(current).when(userService).getCurrentUser();

      PSUserAccountUpdate update = new PSUserAccountUpdate();
      update.setEmail("not-valid");

      assertThrows(PSValidationException.class, () -> userService.updateMyAccount(update));
      verify(backEndRoleMgr, never()).setSubjectEmail(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsNullUpdate() {
      assertThrows(PSValidationException.class, () -> userService.updateMyAccount(null));
    }
  }

  @Nested
  @DisplayName("getCurrentUser directory email (#2395)")
  class GetCurrentUserDirectoryEmail {

    @Test
    void populatesEmailWhenDirectoryCatalogReturnsSubject() throws Exception {
      PSUser dirUser = directoryUser("ldap.user");
      doReturn("ldap.user").when(userService).getCurrentUserName();
      doReturn(dirUser).when(userService).find("ldap.user");
      // getSubjectEmail uses 3-arg findUsers(names, "Default", "backend") — mock that overload
      // (interface defaults are not auto-delegated by Mockito to the 5-arg abstract method).
      when(roleMgr.findUsers(anyList(), eq("Default"), eq("backend")))
          .thenReturn(List.of(subjectWithEmail("ldap.user", "dir@example.com")));

      PSCurrentUser result = userService.getCurrentUser();

      assertEquals("ldap.user", result.getName());
      assertEquals(PSUserProviderType.DIRECTORY, result.getProviderType());
      assertEquals("dir@example.com", result.getEmail());
    }

    @Test
    void leavesEmailEmptyWhenDirectoryCatalogThrows() throws Exception {
      PSUser dirUser = directoryUser("ldap.user");
      doReturn("ldap.user").when(userService).getCurrentUserName();
      doReturn(dirUser).when(userService).find("ldap.user");
      when(roleMgr.findUsers(anyList(), eq("Default"), eq("backend")))
          .thenThrow(new PSSecurityCatalogException("catalog unavailable"));

      PSCurrentUser result = userService.getCurrentUser();

      assertEquals("ldap.user", result.getName());
      assertEquals(PSUserProviderType.DIRECTORY, result.getProviderType());
      // find() leaves email as empty default; exception path must not fail getCurrentUser
      assertEquals("", result.getEmail());
    }

    @Test
    void doesNotQueryCatalogEmailForInternalUsers() throws Exception {
      PSUser internal = new PSUser();
      internal.setName("Admin");
      internal.setEmail("admin@example.com");
      internal.setProviderType(PSUserProviderType.INTERNAL);
      internal.setRoles(List.of("Admin"));
      doReturn("Admin").when(userService).getCurrentUserName();
      doReturn(internal).when(userService).find("Admin");

      PSCurrentUser result = userService.getCurrentUser();

      assertEquals("admin@example.com", result.getEmail());
      // Directory email path is only for non-INTERNAL (find already loaded INTERNAL email)
      verify(roleMgr, never()).findUsers(anyList(), eq("Default"), eq("backend"));
    }
  }

  @Nested
  @DisplayName("Verified email lifecycle enforcement (M2)")
  class EmailPolicyAndStaging {

    @Test
    void rejectsBlankSelfUpdateWhenEmailRequiredAndStagesVerifiedEmailChanges() throws Exception {
      IPSAccountSecurityService accountSecurityService = mock(IPSAccountSecurityService.class);
      when(accountSecurityService.isEmailRequired()).thenReturn(true);
      userService.setAccountSecurityService(accountSecurityService);

      PSCurrentUser current = internalUser("Editor1", "verified@example.com");
      doReturn(current).when(userService).getCurrentUser();

      PSUserAccountUpdate blankUpdate = new PSUserAccountUpdate();
      blankUpdate.setEmail("   ");
      assertThrows(PSValidationException.class, () -> userService.updateMyAccount(blankUpdate));

      when(accountSecurityService.getEmailStatus("Editor1", "verified@example.com", false))
          .thenReturn(
              new PSEmailVerificationStatus(
                  "Editor1",
                  "verified@example.com",
                  true,
                  null,
                  "",
                  "INTERACTIVE_MFA_REQUIRED",
                  true));
      when(accountSecurityService.stageEmailChange(
              eq("Editor1"),
              eq("verified@example.com"),
              eq("pending@example.com"),
              any(),
              eq(true)))
          .thenReturn(
              new PSEmailChangeResult(
                  true, false, "verified@example.com", "pending@example.com", true, "Staged"));

      PSUserAccountUpdate stagedUpdate = new PSUserAccountUpdate();
      stagedUpdate.setEmail("pending@example.com");
      PSCurrentUser res = userService.updateMyAccount(stagedUpdate);

      assertEquals("verified@example.com", res.getEmail());
      verify(backEndRoleMgr, never()).setSubjectEmail("Editor1", "pending@example.com");
      verify(accountSecurityService)
          .stageEmailChange(
              eq("Editor1"),
              eq("verified@example.com"),
              eq("pending@example.com"),
              any(),
              eq(true));
    }

    @Test
    void enforcesRequiredEmailOnCreateForHumanUsersWhileExemptingServiceAccounts() {
      IPSAccountSecurityService accountSecurityService = mock(IPSAccountSecurityService.class);
      when(accountSecurityService.isEmailRequired()).thenReturn(true);
      when(accountSecurityService.isSystemServiceUser("PercussionAdmin")).thenReturn(true);
      when(accountSecurityService.isSystemServiceUser("Editor2")).thenReturn(false);
      when(backEndRoleMgr.getRhythmyxRoles())
          .thenReturn(new java.util.ArrayList<>(List.of("Editor", "Admin")));
      userService.setAccountSecurityService(accountSecurityService);

      PSUser missingEmailHuman = new PSUser();
      missingEmailHuman.setName("Editor2");
      missingEmailHuman.setPassword("ValidPassword123!");
      missingEmailHuman.setEmail("");
      missingEmailHuman.setRoles(List.of("Editor"));

      assertThrows(
          PSValidationException.class, () -> userService.doValidation(missingEmailHuman, true));

      PSUser serviceUser = new PSUser();
      serviceUser.setName("PercussionAdmin");
      serviceUser.setPassword("ValidPassword123!");
      serviceUser.setEmail("");
      serviceUser.setRoles(List.of("Admin"));
      // Non-interactive service user bypasses human email-required check (SYSTEM_USERS check on name
      // still applies inside validator if name is restricted; verify email field has no error)
      PSValidationException ex =
          assertThrows(
              PSValidationException.class, () -> userService.doValidation(serviceUser, true));
      assertFalse(ex.getValidationErrors().getFieldErrors().stream().anyMatch(err -> "email".equals(err.getField())));
    }

    @Test
    void enforcesSharedPasswordPolicyOnCreateAndInternalUpdate() throws Exception {
      when(backEndRoleMgr.getRhythmyxRoles())
          .thenReturn(new java.util.ArrayList<>(List.of("Editor", "Admin")));

      PSUser shortPwCreate = new PSUser();
      shortPwCreate.setName("EditorShort");
      shortPwCreate.setPassword("ShortPw12!");
      shortPwCreate.setEmail("short@example.com");
      shortPwCreate.setRoles(List.of("Editor"));

      PSValidationException shortEx =
          assertThrows(
              PSValidationException.class, () -> userService.doValidation(shortPwCreate, true));
      assertTrue(
          shortEx.getValidationErrors().getFieldErrors().stream()
              .anyMatch(err -> "password".equals(err.getField())));

      PSUser validCreate = new PSUser();
      validCreate.setName("EditorValid");
      validCreate.setPassword("ValidPassword12!");
      validCreate.setEmail("valid@example.com");
      validCreate.setRoles(List.of("Editor"));
      userService.doValidation(validCreate, true);

      doReturn("Admin").when(userService).getCurrentUserName();
      PSUser blankPwInternalUpdate = new PSUser();
      blankPwInternalUpdate.setName("EditorValid");
      blankPwInternalUpdate.setProviderType(PSUserProviderType.INTERNAL);
      blankPwInternalUpdate.setPassword("");
      blankPwInternalUpdate.setEmail("valid@example.com");
      blankPwInternalUpdate.setRoles(List.of("Editor"));
      userService.doValidation(blankPwInternalUpdate, false);

      PSUser tooLongUpdate = new PSUser();
      tooLongUpdate.setName("EditorValid");
      tooLongUpdate.setProviderType(PSUserProviderType.INTERNAL);
      tooLongUpdate.setPassword("a".repeat(151));
      tooLongUpdate.setEmail("valid@example.com");
      tooLongUpdate.setRoles(List.of("Editor"));
      assertThrows(
          PSValidationException.class, () -> userService.doValidation(tooLongUpdate, false));
    }
  }

  private static PSCurrentUser internalUser(String name, String email) {
    PSCurrentUser user = new PSCurrentUser();
    user.setName(name);
    user.setEmail(email);
    user.setProviderType(PSUserProviderType.INTERNAL);
    user.setRoles(List.of("Admin"));
    return user;
  }

  private static PSUser directoryUser(String name) {
    PSUser user = new PSUser();
    user.setName(name);
    user.setProviderType(PSUserProviderType.DIRECTORY);
    user.setRoles(List.of("Editor"));
    // intentionally leave email at default empty — getCurrentUser must load it
    return user;
  }

  /** JAAS Subject with sys_email attribute, round-trippable via {@link PSJaasUtils}. */
  private static Subject subjectWithEmail(String name, String email) {
    PSAttributeList attrs = new PSAttributeList();
    attrs.setAttribute("sys_email", Collections.singletonList(email));
    PSSubject psSubject = new PSGlobalSubject(name, PSSubject.SUBJECT_TYPE_USER, attrs);
    return PSJaasUtils.convertSubject(psSubject);
  }
}

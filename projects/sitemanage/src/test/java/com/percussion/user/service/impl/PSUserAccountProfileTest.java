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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.security.IPSPasswordFilter;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.IPSRoleMgr;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.dao.IPSUserLoginDao;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.data.PSUserAccountUpdate;
import com.percussion.user.data.PSUserProviderType;
import com.percussion.utils.service.IPSUtilityService;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for self-service account profile update (issue #2395 / parent #2374 slice 2).
 *
 * <p>Covers: email validation helper, INTERNAL self email persist, DIRECTORY rejection, null
 * update rejection. Self-only is structural (no target user name accepted).
 */
@DisplayName("Self-service account profile (#2395)")
class PSUserAccountProfileTest {

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
  }

  @Nested
  @DisplayName("isValidEmailAddress")
  class EmailValidation {

    @Test
    void acceptsCommonAddresses() {
      assertTrue(PSUserService.isValidEmailAddress("user@example.com"));
      assertTrue(PSUserService.isValidEmailAddress("first.last+tag@sub.example.co.uk"));
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
    }
  }

  @Nested
  @DisplayName("updateMyAccount")
  class UpdateMyAccount {

    @Test
    void persistsEmailForInternalCurrentUser() throws Exception {
      PSCurrentUser current = internalUser("Editor1", "old@example.com");
      PSCurrentUser refreshed = internalUser("Editor1", "new@example.com");
      doReturn(current, refreshed).when(userService).getCurrentUser();

      PSUserAccountUpdate update = new PSUserAccountUpdate();
      update.setEmail("new@example.com");

      PSCurrentUser result = userService.updateMyAccount(update);

      ArgumentCaptor<String> emailCap = ArgumentCaptor.forClass(String.class);
      verify(backEndRoleMgr).setSubjectEmail(eq("Editor1"), emailCap.capture());
      assertEquals("new@example.com", emailCap.getValue());
      assertEquals("new@example.com", result.getEmail());
    }

    @Test
    void allowsClearingEmailWhenBlank() throws Exception {
      PSCurrentUser current = internalUser("Admin", "old@example.com");
      PSCurrentUser refreshed = internalUser("Admin", "");
      doReturn(current, refreshed).when(userService).getCurrentUser();

      PSUserAccountUpdate update = new PSUserAccountUpdate();
      update.setEmail("  ");

      userService.updateMyAccount(update);

      verify(backEndRoleMgr).setSubjectEmail("Admin", "");
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

  private static PSCurrentUser internalUser(String name, String email) {
    PSCurrentUser user = new PSCurrentUser();
    user.setName(name);
    user.setEmail(email);
    user.setProviderType(PSUserProviderType.INTERNAL);
    user.setRoles(List.of("Admin"));
    return user;
  }
}

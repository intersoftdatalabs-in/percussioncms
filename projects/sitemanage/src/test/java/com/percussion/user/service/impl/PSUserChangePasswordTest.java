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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.security.IPSPasswordFilter;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.IPSRoleMgr;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.dao.IPSUserLoginDao;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.data.PSUser;
import com.percussion.user.data.PSUserLogin;
import com.percussion.user.data.PSUserProviderType;
import com.percussion.utils.service.IPSUtilityService;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

/**
 * Unit tests for self-service {@link PSUserService#changePassword(PSUser)} (issue #3338).
 *
 * <p>Successful persist must return a non-null {@link PSUser} (HTTP 2xx via JAX-RS) and must not
 * reload the current user after {@code USERLOGIN} is updated. Validation failures stay
 * {@link PSValidationException} (mapped 400), not 500.
 */
@DisplayName("Self-service changePassword (#3338)")
class PSUserChangePasswordTest {

  private IPSUserLoginDao userLoginDao;
  private IPSPasswordFilter passwordFilter;
  private PSUserService userService;

  @BeforeEach
  void setUp() {
    userLoginDao = mock(IPSUserLoginDao.class);
    passwordFilter = mock(IPSPasswordFilter.class);
    userService =
        spy(
            new PSUserService(
                userLoginDao,
                passwordFilter,
                mock(IPSBackEndRoleMgr.class),
                mock(IPSRoleMgr.class),
                /* notificationService */ null,
                mock(IPSWorkflowService.class),
                mock(IPSSecurityWs.class),
                mock(IPSContentWs.class),
                mock(IPSIdMapper.class),
                mock(IPSUtilityService.class),
                mock(IPSMetadataService.class)));
  }

  @Test
  @DisplayName("INTERNAL self change persists hash and returns non-null user without password")
  void internalSelfChangeReturnsUserAndDoesNotRefetch() throws Exception {
    PSCurrentUser current = internalCurrent("Admin", "admin@example.com");
    doReturn(current).when(userService).getCurrentUser();
    when(userLoginDao.find("Admin")).thenReturn(login("Admin"));
    when(passwordFilter.encrypt("new-secret")).thenReturn("hashed-secret");
    when(userLoginDao.save(any(PSUserLogin.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PSUser request = requestUser("Admin", "new-secret");
    PSUser result = userService.changePassword(request);

    assertNotNull(result, "null entity becomes HTTP 500 from CXF JSON/XML writers");
    assertEquals("Admin", result.getName());
    assertNull(result.getPassword());
    assertEquals(PSUserProviderType.INTERNAL, result.getProviderType());
    assertEquals(List.of("Admin"), result.getRoles());
    assertEquals("admin@example.com", result.getEmail());

    ArgumentCaptor<PSUserLogin> saved = ArgumentCaptor.forClass(PSUserLogin.class);
    verify(userLoginDao).save(saved.capture());
    assertEquals("Admin", saved.getValue().getUserid());
    assertEquals("hashed-secret", saved.getValue().getPassword());

    InOrder order = Mockito.inOrder(userService, userLoginDao);
    order.verify(userService).getCurrentUser();
    order.verify(userLoginDao).save(any(PSUserLogin.class));
    verify(userService, times(1)).getCurrentUser();
    verify(userService, never()).find(anyString());

    String json = PSSerializerUtils.getJsonFromObject(result);
    assertTrue(json.contains("Admin"), json);
    assertFalse(json.contains("new-secret"), "response must not echo the new password");
    assertFalse(json.contains("hashed-secret"), "response must not include the stored hash");
  }

  @Test
  @DisplayName("blank password is validation, not persist")
  void blankPasswordRejected() throws Exception {
    assertThrows(
        PSValidationException.class,
        () -> userService.changePassword(requestUser("Admin", "  ")));
    verify(userLoginDao, never()).save(any(PSUserLogin.class));
  }

  @Test
  @DisplayName("null user is validation, not NPE")
  void nullUserRejected() throws Exception {
    assertThrows(PSValidationException.class, () -> userService.changePassword(null));
    verify(userLoginDao, never()).save(any(PSUserLogin.class));
  }

  @Test
  @DisplayName("cannot change another user's password")
  void rejectsOtherUser() throws Exception {
    PSCurrentUser current = internalCurrent("Admin", "admin@example.com");
    doReturn(current).when(userService).getCurrentUser();
    when(userLoginDao.find("editor1")).thenReturn(login("editor1"));

    assertThrows(
        PSValidationException.class,
        () -> userService.changePassword(requestUser("editor1", "new-secret")));
    verify(userLoginDao, never()).save(any(PSUserLogin.class));
  }

  @Test
  @DisplayName("DIRECTORY provider is validation, not null 500")
  void rejectsDirectoryUser() throws Exception {
    when(userLoginDao.find("ldap.user")).thenReturn(null);

    assertThrows(
        PSValidationException.class,
        () -> userService.changePassword(requestUser("ldap.user", "new-secret")));
    verify(userLoginDao, never()).save(any(PSUserLogin.class));
    verify(userService, never()).getCurrentUser();
  }

  @Test
  @DisplayName("persist exception still surfaces (no silent success)")
  void persistFailurePropagates() throws Exception {
    PSCurrentUser current = internalCurrent("Admin", "admin@example.com");
    doReturn(current).when(userService).getCurrentUser();
    when(userLoginDao.find("Admin")).thenReturn(login("Admin"));
    when(passwordFilter.encrypt(anyString())).thenReturn("hashed");
    when(userLoginDao.save(any(PSUserLogin.class)))
        .thenThrow(new IPSGenericDao.SaveException("db write failed"));

    assertThrows(
        IPSGenericDao.SaveException.class,
        () -> userService.changePassword(requestUser("Admin", "new-secret")));
  }

  private static PSUser requestUser(String name, String password) {
    PSUser user = new PSUser();
    user.setName(name);
    user.setPassword(password);
    user.setEmail("admin@example.com");
    user.setRoles(List.of("Admin"));
    return user;
  }

  private static PSCurrentUser internalCurrent(String name, String email) {
    PSCurrentUser user = new PSCurrentUser();
    user.setName(name);
    user.setEmail(email);
    user.setProviderType(PSUserProviderType.INTERNAL);
    user.setRoles(List.of("Admin"));
    return user;
  }

  private static PSUserLogin login(String userId) {
    PSUserLogin login = new PSUserLogin();
    login.setUserid(userId);
    login.setPassword("old-hash");
    return login;
  }
}

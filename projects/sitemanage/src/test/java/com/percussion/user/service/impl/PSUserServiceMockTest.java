/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.security.IPSPasswordFilter;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.IPSRoleMgr;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.sitemanage.dao.IPSUserLoginDao;
import com.percussion.user.data.PSExternalUser;
import com.percussion.user.service.IPSUserService.PSDirectoryServiceConfigException;
import com.percussion.user.service.IPSUserService.PSDirectoryServiceConnectionException;
import com.percussion.user.service.IPSUserService.PSDirectoryServiceDisabledException;
import com.percussion.user.service.IPSUserService.PSDirectoryServiceException;
import com.percussion.user.service.IPSUserService.PSDirectoryServiceStatus;
import com.percussion.user.service.IPSUserService.PSDirectoryServiceStatus.ServiceStatus;
import com.percussion.utils.service.IPSUtilityService;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link PSUserService}.
 *
 * <p>Covers issue #937: {@code checkDirectoryService()} historically invoked {@code
 * findUsersFromDirectoryService("a")} with a meaningless literal query. The fix replaces it with
 * the existing no-arg overload, which is the documented probe entry point. These tests pin the
 * behavior so that (a) the contract of {@code checkDirectoryService()} is preserved and (b) any
 * regression that re-introduces a literal query argument is caught immediately.
 */
@DisplayName("PSUserService#checkDirectoryService")
class PSUserServiceMockTest {

  private PSUserService userService;
  private PSUserService userServiceSpy;

  @BeforeEach
  void setUp() {
    userService =
        new PSUserService(
            Mockito.mock(IPSUserLoginDao.class),
            Mockito.mock(IPSPasswordFilter.class),
            Mockito.mock(IPSBackEndRoleMgr.class),
            Mockito.mock(IPSRoleMgr.class),
            /* notificationService */ null,
            Mockito.mock(IPSWorkflowService.class),
            Mockito.mock(IPSSecurityWs.class),
            Mockito.mock(IPSContentWs.class),
            Mockito.mock(IPSIdMapper.class),
            Mockito.mock(IPSUtilityService.class),
            Mockito.mock(IPSMetadataService.class));
    userServiceSpy = spy(userService);
  }

  @Test
  @DisplayName("returns ENABLED when the no-arg probe succeeds")
  void returnsEnabledWhenProbeSucceeds() {
    List<PSExternalUser> empty = Collections.emptyList();
    doReturn(empty).when(userServiceSpy).findUsersFromDirectoryService();

    PSDirectoryServiceStatus status = userServiceSpy.checkDirectoryService();

    assertEquals(ServiceStatus.ENABLED, status.getStatus());
    Mockito.verify(userServiceSpy).findUsersFromDirectoryService();
  }

  @Test
  @DisplayName("returns CONFIG_ERROR when the probe surfaces a configuration error")
  void returnsConfigErrorOnConfigException() {
    PSDirectoryServiceException cause = new PSDirectoryServiceConfigException("bad config");
    doThrow(cause).when(userServiceSpy).findUsersFromDirectoryService();

    PSDirectoryServiceStatus status = userServiceSpy.checkDirectoryService();

    assertEquals(ServiceStatus.CONFIG_ERROR, status.getStatus());
    assertSame(cause, status.getError());
  }

  @Test
  @DisplayName("returns CONNECTION_ERROR when the probe surfaces a connection error")
  void returnsConnectionErrorOnConnectionException() {
    PSDirectoryServiceException cause =
        new PSDirectoryServiceConnectionException("ldap unreachable");
    doThrow(cause).when(userServiceSpy).findUsersFromDirectoryService();

    PSDirectoryServiceStatus status = userServiceSpy.checkDirectoryService();

    assertEquals(ServiceStatus.CONNECTION_ERROR, status.getStatus());
    assertSame(cause, status.getError());
  }

  @Test
  @DisplayName("returns DISABLED when the probe surfaces a disabled directory service")
  void returnsDisabledOnDisabledException() {
    PSDirectoryServiceException cause =
        new PSDirectoryServiceDisabledException("no directory service enabled");
    doThrow(cause).when(userServiceSpy).findUsersFromDirectoryService();

    PSDirectoryServiceStatus status = userServiceSpy.checkDirectoryService();

    assertEquals(ServiceStatus.DISABLED, status.getStatus());
    assertSame(cause, status.getError());
  }

  @Test
  @DisplayName(
      "returns UNKNOWN_ERROR when the probe surfaces a generic directory service exception")
  void returnsUnknownErrorOnGenericException() {
    PSDirectoryServiceException cause = new PSDirectoryServiceException("boom");
    doThrow(cause).when(userServiceSpy).findUsersFromDirectoryService();

    PSDirectoryServiceStatus status = userServiceSpy.checkDirectoryService();

    assertEquals(ServiceStatus.UNKNOWN_ERROR, status.getStatus());
    assertSame(cause, status.getError());
  }

  @Test
  @DisplayName("does not propagate underlying directory exception to the caller")
  void doesNotPropagateDirectoryException() {
    doThrow(new PSDirectoryServiceConfigException("regression"))
        .when(userServiceSpy)
        .findUsersFromDirectoryService();

    // checkDirectoryService() must translate the exception into a status payload,
    // never let it bubble up.
    PSDirectoryServiceStatus status = assertDoesNotThrow(userServiceSpy::checkDirectoryService);
    assertEquals(ServiceStatus.CONFIG_ERROR, status.getStatus());
  }
}

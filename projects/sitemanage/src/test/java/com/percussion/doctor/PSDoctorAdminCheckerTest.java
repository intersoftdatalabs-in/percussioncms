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
package com.percussion.doctor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.user.service.IPSUserService.PSNoCurrentUserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PSDoctorAdminCheckerTest {

  @Mock private IPSUserService userService;

  private PSDoctorAdminChecker checker;

  @BeforeEach
  void setUp() {
    checker = new PSDoctorAdminChecker(userService);
  }

  @Test
  void adminUserAllowed() throws Exception {
    PSCurrentUser user = new PSCurrentUser();
    user.setName("admin1");
    when(userService.getCurrentUser()).thenReturn(user);
    when(userService.isAdminUser("admin1")).thenReturn(true);

    assertTrue(checker.isCurrentUserAdmin());
  }

  @Test
  void nonAdminRejected() throws Exception {
    PSCurrentUser user = new PSCurrentUser();
    user.setName("editor");
    when(userService.getCurrentUser()).thenReturn(user);
    when(userService.isAdminUser("editor")).thenReturn(false);

    assertFalse(checker.isCurrentUserAdmin());
  }

  @Test
  void anonymousNoCurrentUserRejected() throws Exception {
    when(userService.getCurrentUser()).thenThrow(new PSNoCurrentUserException("none"));

    assertFalse(checker.isCurrentUserAdmin());
  }

  @Test
  void blankUserNameRejected() throws Exception {
    PSCurrentUser user = new PSCurrentUser();
    user.setName("  ");
    when(userService.getCurrentUser()).thenReturn(user);

    assertFalse(checker.isCurrentUserAdmin());
  }

  @Test
  void dataServiceFailureRejected() throws Exception {
    when(userService.getCurrentUser()).thenThrow(new PSDataServiceException("boom"));

    assertFalse(checker.isCurrentUserAdmin());
  }
}

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

package com.percussion.itemmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.itemmanagement.data.PSItemUserInfo;
import com.percussion.itemmanagement.service.IPSItemWorkflowService.PSItemWorkflowServiceException;
import org.junit.jupiter.api.Test;

class CheckoutOwnerInfoTest {

  @Test
  void otherUserLockIsVisible() throws Exception {
    PSItemUserInfo info = CheckoutOwnerInfo.fromSummary("42", "Home", "editor", "Admin");
    assertEquals("editor", info.getCheckOutUser());
    assertEquals("Admin", info.getCurrentUser());
    assertEquals("Home", info.getItemName());
  }

  @Test
  void notCheckedOutIsEmptyOwner() throws Exception {
    PSItemUserInfo info = CheckoutOwnerInfo.fromSummary("42", "Home", null, "Admin");
    assertEquals("", info.getCheckOutUser());
  }

  @Test
  void blankNameFallsBackToId() throws Exception {
    PSItemUserInfo info = CheckoutOwnerInfo.fromSummary("42", "  ", "", "Admin");
    assertEquals("42", info.getItemName());
  }

  @Test
  void missingSessionUserFails() {
    assertThrows(
        PSItemWorkflowServiceException.class,
        () -> CheckoutOwnerInfo.fromSummary("42", "Home", "editor", " "));
  }
}

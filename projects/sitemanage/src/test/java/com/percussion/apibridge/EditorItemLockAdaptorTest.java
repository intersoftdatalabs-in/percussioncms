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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.percussion.itemmanagement.data.PSItemUserInfo;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService.PSItemWorkflowServiceException;
import com.percussion.rest.editor.EditorItemLockInfo;
import com.percussion.rest.errors.NotAuthorizedException;
import com.percussion.share.data.PSNoContent;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EditorItemLockAdaptorTest {

  @Mock private IPSItemWorkflowService workflow;

  private EditorItemLockAdaptor adaptor;
  private final URI base = URI.create("http://localhost/rest");

  @BeforeEach
  void init() {
    adaptor = new EditorItemLockAdaptor(workflow);
  }

  @Test
  void checkoutSuccessWhenHeldBySelf() throws Exception {
    doReturn(true).when(workflow).isModifiableByUser("42");
    when(workflow.checkOut("42"))
        .thenReturn(new PSItemUserInfo("Home", "admin", "admin", "Assignee"));
    EditorItemLockInfo info = adaptor.checkout(base, "42");
    assertEquals("admin", info.getCheckOutUser());
  }

  @Test
  void checkoutForbiddenWhenNotModifiable() throws Exception {
    doReturn(false).when(workflow).isModifiableByUser("42");
    assertThrows(NotAuthorizedException.class, () -> adaptor.checkout(base, "42"));
  }

  @Test
  void checkoutConflictWhenHeldByOther() throws Exception {
    doReturn(true).when(workflow).isModifiableByUser("42");
    when(workflow.checkOut("42"))
        .thenReturn(new PSItemUserInfo("Home", "editor", "admin", "Assignee"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.checkout(base, "42"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void checkoutBlankIdIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.checkout(base, " "));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void checkinSuccess() throws Exception {
    doReturn(true).when(workflow).isModifiableByUser("42");
    when(workflow.checkIn("42")).thenReturn(new PSNoContent("checkIn"));
    assertEquals("", adaptor.checkin(base, "42").getCheckOutUser());
  }

  @Test
  void checkinForbiddenWhenNotModifiable() throws Exception {
    doReturn(false).when(workflow).isModifiableByUser("42");
    assertThrows(NotAuthorizedException.class, () -> adaptor.checkin(base, "42"));
  }

  @Test
  void checkinConflictWhenServiceThrows() throws Exception {
    doReturn(true).when(workflow).isModifiableByUser("42");
    when(workflow.checkIn("42")).thenThrow(new PSItemWorkflowServiceException("held"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.checkin(base, "42"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void heldByOtherIgnoresEmptySession() {
    EditorItemLockInfo info = new EditorItemLockInfo("Home", "editor", "", "Assignee");
    org.junit.jupiter.api.Assertions.assertFalse(EditorItemLockAdaptor.heldByOther(info));
  }
}

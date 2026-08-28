/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * CD-01 DELETE requires a held design-session lock and calls {@code
 * IPSContentDesignWs.deleteContentTypes} without ignoring dependents.
 */
@Tag("UnitTest")
class ContentTypeAdaptorDeleteTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private PSItemDefManager itemDefManager;
  private ContentTypeAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    itemDefManager = mock(PSItemDefManager.class);
    adaptor = new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> true);
    guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void delete_whenLockHeld_callsDesignWsWithoutIgnoringDependents() throws Exception {
    stubHeldLock();
    stubDefinition();

    assertEquals(Boolean.TRUE, adaptor.deleteContentType(null, "311"));

    verify(designWs)
        .deleteContentTypes(eq(List.of(guid)), eq(false), eq("test-session"), eq("Admin"));
    verify(systemDesign).isLocked(anyList(), eq("Admin"));
  }

  @Test
  void delete_conflictWhenLockNotHeld() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(Collections.singletonList(null));
    stubDefinition();

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class, () -> adaptor.deleteContentType(null, "311"));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).deleteContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_conflictWhenLockedByOtherUser() throws Exception {
    PSObjectSummary other = new PSObjectSummary(guid, "percPage");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));
    stubDefinition();

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class, () -> adaptor.deleteContentType(null, "311"));
    assertTrue(ex.getMessage().contains("editor2"), ex.getMessage());
    verify(designWs, never()).deleteContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_lockErrorFromDesignWs_is409() throws Exception {
    stubHeldLock();
    stubDefinition();
    PSErrorsException errors = new PSErrorsException();
    errors.addError(
        guid, new PSLockErrorException(1, "is not locked", "stack", "Admin", 12));
    doThrow(errors)
        .when(designWs)
        .deleteContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class, () -> adaptor.deleteContentType(null, "311"));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
  }

  @Test
  void delete_dependents_throws400() throws Exception {
    stubHeldLock();
    stubDefinition();
    PSErrorsException errors = new PSErrorsException();
    errors.addError(
        guid,
        new PSErrorException(
            1, "Content Type 311 has dependents: percPage items", "stack"));
    doThrow(errors)
        .when(designWs)
        .deleteContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.deleteContentType(null, "311"));
    assertTrue(ex.getMessage().contains("dependents"), ex.getMessage());
  }

  @Test
  void delete_unexpectedDesignError_is500() throws Exception {
    stubHeldLock();
    stubDefinition();
    PSErrorsException errors = new PSErrorsException();
    errors.addError(
        guid, new PSErrorException(1, "Failed to delete PSItemDefinition 0-2-311: disk full", "stack"));
    doThrow(errors)
        .when(designWs)
        .deleteContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.deleteContentType(null, "311"));
    assertTrue(ex.getMessage().contains("Failed to delete content type"), ex.getMessage());
    assertTrue(ex.getMessage().contains("disk full"), ex.getMessage());
  }

  @Test
  void delete_unknownName_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("missing"));
    assertNull(adaptor.deleteContentType(null, "missing"));
    verify(systemDesign, never()).isLocked(anyList(), any());
    verify(designWs, never()).deleteContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.deleteContentType(null, "311"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void delete_wildcardName_isBadRequest() throws Exception {
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.deleteContentType(null, "perc*"));
    verify(designWs, never()).deleteContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_blankIdOrName_returnsNullBeforeSessionCheck() throws Exception {
    PSRequestInfoBase.resetRequestInfo();
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    assertNull(adaptor.deleteContentType(null, "  "));
    assertNull(adaptor.deleteContentType(null, null));
    verify(designWs, never()).deleteContentTypes(anyList(), anyBoolean(), any(), any());
    verify(systemDesign, never()).isLocked(anyList(), any());
  }

  @Test
  void isDeleteDependencyFailure_detectsDependentsMessage() {
    PSErrorsException errors = new PSErrorsException();
    errors.addError(guid, new PSErrorException(1, "object has dependents", "stack"));
    assertTrue(ContentTypeAdaptor.isDeleteDependencyFailure(errors));
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary held = new PSObjectSummary(guid, "percPage");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }

  private void stubDefinition() throws Exception {
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn("percPage");
    when(def.getTypeId()).thenReturn(311);
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);
  }
}

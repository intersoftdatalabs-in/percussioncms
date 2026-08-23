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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.rest.ObjectLockSummary;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Content type design-session lock/unlock is a thin adaptor over {@link IPSContentDesignWs} (self
 * only; Admin). Unlock releases via {@link IPSSystemDesignWs#releaseLocks} without saving.
 */
@Tag("UnitTest")
class ContentTypeAdaptorLockTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private ContentTypeAdaptor adaptor;

  @BeforeEach
  void setUp() throws Exception {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    adaptor = new ContentTypeAdaptor(designWs, null, systemDesign, () -> true);
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(mock(PSItemDefinition.class)));
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void lockContentType_loadsWithLockTrueOverrideFalse() throws Exception {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));

    ObjectLockSummary summary = adaptor.lockContentType(null, "311");

    assertNotNull(summary);
    assertEquals("Admin", summary.getLocker());
    assertEquals("test-session", summary.getSession());
    assertEquals(ContentTypeAdaptor.DESIGN_LOCK_MINUTES, summary.getRemainingTime());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> ids = ArgumentCaptor.forClass(List.class);
    verify(designWs)
        .loadContentTypes(ids.capture(), eq(true), eq(false), eq("test-session"), eq("Admin"));
    assertEquals(1, ids.getValue().size());
    assertEquals(guid, ids.getValue().get(0));
  }

  @Test
  void lockContentType_resolvesNameViaFind() throws Exception {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn("percPage");
    when(designWs.findContentTypes("percPage")).thenReturn(List.of(sum));
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(mock(PSItemDefinition.class)));

    ObjectLockSummary summary = adaptor.lockContentType(null, "percPage");
    assertNotNull(summary);
    verify(designWs).findContentTypes("percPage");
  }

  @Test
  void lockContentType_unknownName_returnsNull() throws Exception {
    when(designWs.findContentTypes("missing")).thenReturn(List.of());
    assertNull(adaptor.lockContentType(null, "missing"));
    verify(designWs, never()).loadContentTypes(anyList(), anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void lockContentType_conflictWhenLockedByOtherUser() throws Exception {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
    PSErrorResultsException errors = new PSErrorResultsException();
    errors.addError(
        guid, new PSLockErrorException(1, "CREATE_LOCK_FAILED", "stack", "editor2", 12));
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), any(), any())).thenThrow(errors);

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class, () -> adaptor.lockContentType(null, "311"));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    assertTrue(ex.getMessage().contains("editor2"), ex.getMessage());
  }

  @Test
  void lockContentType_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied = new ContentTypeAdaptor(designWs, null, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.lockContentType(null, "311"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void lockContentType_requiresSessionUser() {
    PSRequestInfoBase.resetRequestInfo();
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.lockContentType(null, "311"));
    assertTrue(ex.getMessage().toLowerCase().contains("session"), ex.getMessage());
  }

  @Test
  void unlockContentType_releasesWithoutSave() throws Exception {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
    PSObjectSummary held = new PSObjectSummary(guid, "percPage");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));

    assertEquals(Boolean.TRUE, adaptor.unlockContentType(null, "311"));
    verify(designWs, never()).loadContentTypes(anyList(), eq(true), anyBoolean(), any(), any());
    verify(systemDesign).isLocked(anyList(), eq("Admin"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> ids = ArgumentCaptor.forClass(List.class);
    verify(systemDesign).releaseLocks(ids.capture(), eq("test-session"), eq("Admin"));
    assertEquals(guid, ids.getValue().get(0));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void unlockContentType_conflictWhenLockedByOtherUser() throws Exception {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
    PSObjectSummary other = new PSObjectSummary(guid, "percPage");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class, () -> adaptor.unlockContentType(null, "311"));
    assertTrue(ex.getMessage().contains("editor2"), ex.getMessage());
    verify(systemDesign, never()).releaseLocks(anyList(), any(), any());
  }

  @Test
  void unlockContentType_failsWhenDesignServiceMissing() throws Exception {
    ContentTypeAdaptor noSys = new ContentTypeAdaptor(designWs, null, null, () -> true);
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> noSys.unlockContentType(null, "311"));
    assertFalse(ex instanceof ContentTypeDesignLockException, ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("unavailable"), ex.getMessage());
  }

  @Test
  void lockContentType_reportsActualRemainingTime() throws Exception {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));
    PSObjectSummary held = new PSObjectSummary(guid, "percPage");
    held.setLockedInfo("test-session", "Admin", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));

    ObjectLockSummary summary = adaptor.lockContentType(null, "311");
    assertEquals(12, summary.getRemainingTime());
  }

  @Test
  void resolveContentTypeGuid_rejectsWildcardAndOversizedId() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.resolveContentTypeGuid("perc*"));
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.resolveContentTypeGuid("99999999999999999999"));
  }

  @Test
  void hasLockError_doesNotMatchBlockquoteText() {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 1L);
    PSErrorResultsException errors = new PSErrorResultsException();
    errors.addError(guid, "Failed to open content type design session: percBlockquote");
    assertFalse(ContentTypeAdaptor.hasLockError(errors));
  }

  @Test
  void hasLockError_detectsLockException() {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 1L);
    PSErrorResultsException errors = new PSErrorResultsException();
    errors.addError(guid, new PSLockErrorException(1, "locked", "stack", "u", 1));
    assertTrue(ContentTypeAdaptor.hasLockError(errors));
    assertEquals("u", ContentTypeAdaptor.firstLockLocker(errors));
    assertFalse(ContentTypeAdaptor.isNotFoundError(errors));
  }

  @Test
  void isNotFoundError_whenNoLockMetadata() {
    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 1L);
    PSErrorResultsException errors = new PSErrorResultsException();
    errors.addError(guid, "object not found");
    assertFalse(ContentTypeAdaptor.hasLockError(errors));
    assertTrue(ContentTypeAdaptor.isNotFoundError(errors));
    assertNull(ContentTypeAdaptor.firstLockLocker(errors));
  }

  @Test
  void toLockSummary_setsSessionUserMinutes() {
    ObjectLockSummary summary = ContentTypeAdaptor.toLockSummary("s", "Admin", 30);
    assertEquals("s", summary.getSession());
    assertEquals("Admin", summary.getLocker());
    assertEquals(30, summary.getRemainingTime());
  }
}

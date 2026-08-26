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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.locking.data.PSObjectLockSummary;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * CD-12: dedicated allowedTemplates GET/PUT requires a held design lock; validates template ids.
 */
@Tag("UnitTest")
class ContentTypeAdaptorAllowedTemplatesTest {

  private IPSContentDesignWs designSvc;
  private PSItemDefManager itemDefManager;
  private IPSSystemDesignWs systemDesign;
  private IPSAssemblyService assembly;
  private ContentTypeAdaptor adaptor;
  private PSItemDefinition percPage;

  @BeforeEach
  void setUp() throws Exception {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "test-user");

    designSvc = mock(IPSContentDesignWs.class);
    itemDefManager = mock(PSItemDefManager.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    assembly = mock(IPSAssemblyService.class);
    adaptor = new ContentTypeAdaptor(designSvc, itemDefManager, systemDesign, assembly);

    percPage = mock(PSItemDefinition.class);
    when(percPage.getTypeId()).thenReturn(311);
    when(itemDefManager.getItemDef("percPage", PSItemDefManager.COMMUNITY_ANY)).thenReturn(percPage);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void getAllowedTemplates_returnsAssociatedTemplates() throws Exception {
    PSAssemblyTemplate t = template("perc.page", 42L, "Page");
    when(assembly.findTemplatesByContentType(any())).thenReturn(List.of(t));

    List<NamedObjectRef> out = adaptor.getAllowedTemplates(null, "percPage");
    assertEquals(1, out.size());
    assertEquals("perc.page", out.get(0).getName());
    assertEquals("Page", out.get(0).getLabel());
    verify(designSvc, never())
        .saveAssociatedTemplates(any(), anyList(), anyBoolean(), any(), any());
  }

  @Test
  void getAllowedTemplates_missingType_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new com.percussion.cms.objectstore.PSInvalidContentTypeException("missing"));
    assertNull(adaptor.getAllowedTemplates(null, "missing"));
  }

  @Test
  void getAllowedTemplates_cacheMiss_doesNotUseObjectStoreFallback() throws Exception {
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY)))
        .thenThrow(new com.percussion.cms.objectstore.PSInvalidContentTypeException("311"));
    ContentTypeAdaptor spy = spy(adaptor);
    doReturn(percPage).when(spy).loadItemDefFromObjectStore("311");
    assertNull(spy.getAllowedTemplates(null, "311"));
    verify(spy, never()).loadItemDefFromObjectStore("311");
  }

  @Test
  void replaceAllowedTemplates_cacheMiss_doesNotUseObjectStoreFallback() throws Exception {
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY)))
        .thenThrow(new com.percussion.cms.objectstore.PSInvalidContentTypeException("311"));
    ContentTypeAdaptor spy = spy(adaptor);
    doReturn(percPage).when(spy).loadItemDefFromObjectStore("311");
    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("perc.page");
    assertNull(spy.replaceAllowedTemplates(null, "311", List.of(ref)));
    verify(spy, never()).loadItemDefFromObjectStore("311");
  }

  @Test
  void replaceAllowedTemplates_withHeldLock_persistsAndReturnsNewSet() throws Exception {
    stubHeldLock();
    PSAssemblyTemplate t = template("perc.page", 42L, "Page");
    when(assembly.findTemplateByName("perc.page")).thenReturn(t);
    when(assembly.findTemplatesByContentType(any())).thenReturn(List.of(t));

    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("perc.page");
    List<NamedObjectRef> out = adaptor.replaceAllowedTemplates(null, "percPage", List.of(ref));

    assertEquals(1, out.size());
    assertEquals("perc.page", out.get(0).getName());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> ids = ArgumentCaptor.forClass(List.class);
    verify(designSvc)
        .saveAssociatedTemplates(
            any(), ids.capture(), eq(false), eq("test-session"), eq("test-user"));
    assertEquals(1, ids.getValue().size());
    assertEquals(42, ids.getValue().get(0).getUUID());
  }

  @Test
  void replaceAllowedTemplates_emptyList_clearsAssociations() throws Exception {
    stubHeldLock();
    when(assembly.findTemplatesByContentType(any())).thenReturn(List.of());

    List<NamedObjectRef> out =
        adaptor.replaceAllowedTemplates(null, "percPage", Collections.emptyList());
    assertNotNull(out);
    assertTrue(out.isEmpty());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> emptyIds = ArgumentCaptor.forClass(List.class);
    verify(designSvc)
        .saveAssociatedTemplates(
            any(), emptyIds.capture(), eq(false), eq("test-session"), eq("test-user"));
    assertTrue(emptyIds.getValue().isEmpty());
  }

  @Test
  void replaceAllowedTemplates_withoutLock_throws409() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("test-user")))
        .thenReturn(Collections.singletonList(null));

    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("perc.page");
    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.replaceAllowedTemplates(null, "percPage", List.of(ref)));
    assertTrue(ex.getMessage().contains("design lock required"));
    verify(designSvc, never())
        .saveAssociatedTemplates(any(), anyList(), anyBoolean(), any(), any());
  }

  @Test
  void replaceAllowedTemplates_lockedByOtherUser_throws409() throws Exception {
    PSObjectSummary summary = new PSObjectSummary();
    summary.setLocked(new PSObjectLockSummary("other-session", "editor", 30));
    when(systemDesign.isLocked(anyList(), eq("test-user")))
        .thenReturn(Collections.singletonList(summary));

    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("perc.page");
    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.replaceAllowedTemplates(null, "percPage", List.of(ref)));
    assertTrue(ex.getMessage().contains("locked by editor"));
    verify(designSvc, never())
        .saveAssociatedTemplates(any(), anyList(), anyBoolean(), any(), any());
  }

  @Test
  void replaceAllowedTemplates_invalidTemplateName_throws400() throws Exception {
    stubHeldLock();
    when(assembly.findTemplateByName("nope")).thenReturn(null);

    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("nope");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.replaceAllowedTemplates(null, "percPage", List.of(ref)));
    assertTrue(ex.getMessage().contains("template not found"));
    verify(designSvc, never())
        .saveAssociatedTemplates(any(), anyList(), anyBoolean(), any(), any());
  }

  @Test
  void replaceAllowedTemplates_nullBody_throws400() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.replaceAllowedTemplates(null, "percPage", null));
  }

  @Test
  void replaceAllowedTemplates_missingSession_throwsConflict() {
    PSRequestInfoBase.resetRequestInfo();
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> adaptor.replaceAllowedTemplates(null, "percPage", List.of()));
    assertTrue(ex.getMessage().toLowerCase().contains("session"));
  }

  @Test
  void replaceAllowedTemplates_saveReportsUnlocked_mapsTo409() throws Exception {
    stubHeldLock();
    PSAssemblyTemplate t = template("perc.page", 42L, "Page");
    when(assembly.findTemplateByName("perc.page")).thenReturn(t);

    PSErrorsException errors = new PSErrorsException();
    errors.addError(
        new PSGuid(PSTypeEnum.NODEDEF, 311L),
        new PSErrorException("Template Links object 0-2-311 is not locked"));
    doThrow(errors)
        .when(designSvc)
        .saveAssociatedTemplates(any(), anyList(), eq(false), any(), any());

    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("perc.page");
    assertThrows(
        ContentTypeDesignLockException.class,
        () -> adaptor.replaceAllowedTemplates(null, "percPage", List.of(ref)));
  }

  @Test
  void isNotLockedError_detectsMessage() {
    PSErrorsException errors = new PSErrorsException();
    errors.addError(
        new PSGuid(PSTypeEnum.NODEDEF, 1L), new PSErrorException("object is not locked"));
    assertTrue(ContentTypeAdaptor.isNotLockedError(errors));
    PSErrorsException unrelated = new PSErrorsException();
    unrelated.addError(new PSGuid(PSTypeEnum.NODEDEF, 1L), "unrelated not locked phrase");
    assertFalse(ContentTypeAdaptor.isNotLockedError(unrelated));
  }

  @Test
  void isNotLockedError_detectsTypedLockErrorException() {
    PSErrorsException errors = new PSErrorsException();
    errors.addError(
        new PSGuid(PSTypeEnum.NODEDEF, 1L),
        new PSLockErrorException(0, "lock failed", "stack"));
    assertTrue(ContentTypeAdaptor.isNotLockedError(errors));
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary summary = new PSObjectSummary();
    summary.setLocked(new PSObjectLockSummary("test-session", "test-user", 30));
    when(systemDesign.isLocked(anyList(), eq("test-user")))
        .thenReturn(Collections.singletonList(summary));
  }

  private static PSAssemblyTemplate template(String name, long uuid, String label) {
    PSAssemblyTemplate t = mock(PSAssemblyTemplate.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.TEMPLATE, uuid);
    when(t.getGUID()).thenReturn(guid);
    when(t.getName()).thenReturn(name);
    when(t.getLabel()).thenReturn(label);
    return t;
  }
}

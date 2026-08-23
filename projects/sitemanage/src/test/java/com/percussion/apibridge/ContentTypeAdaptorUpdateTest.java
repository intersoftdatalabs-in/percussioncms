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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.rest.contenttypes.ContentTypeField;
import com.percussion.rest.contenttypes.NamedObjectRef;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorResultsException;
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
import org.mockito.ArgumentCaptor;

/**
 * PUT content-type save requires a held design-session lock (does not auto lock-save-unlock).
 * Field rule expressions on the wire DTO stay read-only.
 */
@Tag("UnitTest")
class ContentTypeAdaptorUpdateTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private ContentTypeAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() throws Exception {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    adaptor = new ContentTypeAdaptor(designWs, null, systemDesign, () -> true);
    guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(mock(PSItemDefinition.class)));
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void update_roundTripsDescriptionWhenLockHeld() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubLockedDefinition("percPage", "Page", "old description");
    ContentTypeDetail body = new ContentTypeDetail();
    body.setDescription("updated description");

    ContentTypeDetail out = adaptor.updateContentType(null, "311", body);

    assertEquals("Page", out.getLabel());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSItemDefinition>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveContentTypes(saved.capture(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    assertSame(def, saved.getValue().get(0));
    verify(def).setDescription("updated description");
    verify(systemDesign).isLocked(anyList(), eq("Admin"));
    verify(designWs, never()).saveAssociatedTemplates(any(), anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_roundTripsLabelWhenLockHeld() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubLockedDefinition("percPage", "Page", "desc");
    ContentTypeDetail body = new ContentTypeDetail();
    body.setLabel("Pages");

    adaptor.updateContentType(null, "311", body);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSItemDefinition>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveContentTypes(saved.capture(), eq(false), eq("test-session"), eq("Admin"));
    assertSame(def, saved.getValue().get(0));
    verify(def).setLabel("Pages");
  }

  @Test
  void update_proceedsWhenIsLockedSummaryMissing() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("Admin")))
        .thenReturn(Collections.singletonList(null));
    PSItemDefinition def = stubLockedDefinition("percPage", "Page", "old");
    ContentTypeDetail body = new ContentTypeDetail();
    body.setDescription("updated");

    ContentTypeDetail out = adaptor.updateContentType(null, "311", body);

    assertEquals("Page", out.getLabel());
    verify(designWs).loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin"));
    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    verify(def).setDescription("updated");
  }

  @Test
  void update_conflictWhenLockedByOtherUser() throws Exception {
    PSObjectSummary other = new PSObjectSummary(guid, "percPage");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));
    ContentTypeDetail body = new ContentTypeDetail();
    body.setDescription("nope");

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.updateContentType(null, "311", body));
    assertTrue(ex.getMessage().contains("editor2"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_conflictWhenLockedInAnotherSession() throws Exception {
    stubHeldLock();
    PSErrorResultsException errors = new PSErrorResultsException();
    errors.addError(
        guid, new PSLockErrorException(1, "LOCK_EXTENSION_INVALID_SESSION", "stack", "Admin", 12));
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenThrow(errors);
    ContentTypeDetail body = new ContentTypeDetail();
    body.setDescription("nope");

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.updateContentType(null, "311", body));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_unknownName_returnsNull() throws Exception {
    when(designWs.findContentTypes("missing")).thenReturn(List.of());
    assertNull(adaptor.updateContentType(null, "missing", new ContentTypeDetail()));
    verify(systemDesign, never()).isLocked(anyList(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied = new ContentTypeAdaptor(designWs, null, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.updateContentType(null, "311", new ContentTypeDetail()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void update_ignoresReadOnlyFieldExpressions() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubLockedDefinition("percPage", "Page", "desc");
    PSField field = mock(PSField.class);
    PSFieldSet fieldSet = mock(PSFieldSet.class);
    when(fieldSet.findFieldByName("sys_title", false)).thenReturn(field);
    when(def.getFieldSet()).thenReturn(fieldSet);
    when(def.getComplexChildren()).thenReturn(List.of());

    ContentTypeField patch = new ContentTypeField();
    patch.setName("sys_title");
    patch.setValidationExpression("sys_title <> ''");
    patch.setVisibilityExpression("1 = 1");
    patch.setInputTranslationExpression("sys_ToUpper");
    patch.setOutputTranslationExpression("sys_ToLower");
    patch.setControlPropertyNames(List.of("height"));
    patch.setLabel("Hacked label");
    ContentTypeDetail body = new ContentTypeDetail();
    body.setFields(List.of(patch));

    adaptor.updateContentType(null, "311", body);

    verify(field, never()).setUserSearchable(anyBoolean());
    verify(field, never()).setOccurrenceDimension(anyInt(), any());
    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void update_savesTemplateAssociationsWithoutReleasingLock() throws Exception {
    adaptor = spy(adaptor);
    stubHeldLock();
    stubLockedDefinition("percPage", "Page", "desc");
    IPSGuid tpl = new PSGuid(PSTypeEnum.TEMPLATE, 501L);
    doReturn(List.of(tpl)).when(adaptor).resolveTemplateGuids(any());
    ContentTypeDetail body = new ContentTypeDetail();
    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("rffSnTitle");
    body.setAllowedTemplates(List.of(ref));

    adaptor.updateContentType(null, "311", body);

    verify(designWs)
        .saveAssociatedTemplates(
            eq(guid), eq(List.of(tpl)), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void update_templateAssociationFailureAfterSaveIsPartialSuccess() throws Exception {
    adaptor = spy(adaptor);
    stubHeldLock();
    stubLockedDefinition("percPage", "Page", "desc");
    IPSGuid tpl = new PSGuid(PSTypeEnum.TEMPLATE, 501L);
    doReturn(List.of(tpl)).when(adaptor).resolveTemplateGuids(any());
    doThrow(new PSErrorsException())
        .when(designWs)
        .saveAssociatedTemplates(any(), anyList(), eq(false), any(), any());
    ContentTypeDetail body = new ContentTypeDetail();
    NamedObjectRef ref = new NamedObjectRef();
    ref.setName("rffSnTitle");
    body.setAllowedTemplates(List.of(ref));

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class, () -> adaptor.updateContentType(null, "311", body));
    assertTrue(ex.getMessage().toLowerCase().contains("template"), ex.getMessage());
    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void update_unknownNumericId_returnsNull() throws Exception {
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of());
    assertNull(adaptor.updateContentType(null, "999", new ContentTypeDetail()));
    verify(systemDesign, never()).isLocked(anyList(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_oversizedNumericId_isBadRequest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.updateContentType(null, "99999999999999999999", new ContentTypeDetail()));
  }

  @Test
  void update_wildcardName_isBadRequest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.updateContentType(null, "perc*", new ContentTypeDetail()));
    verify(designWs, never()).findContentTypes(any());
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary held = new PSObjectSummary(guid, "percPage");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }

  private PSItemDefinition stubLockedDefinition(String name, String label, String description)
      throws Exception {
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn(name);
    when(def.getLabel()).thenReturn(label);
    when(def.getDescription()).thenReturn(description);
    when(def.isEnabled()).thenReturn(true);
    when(def.isHidden()).thenReturn(false);
    when(def.getAppName()).thenReturn("rx_cePage");
    when(def.getEditorUrl()).thenReturn("/Rhythmyx/rx_cePage/percPage.html");
    when(def.getTypeId()).thenReturn(311);
    when(def.getFieldSet()).thenReturn(null);
    when(def.getContentEditor()).thenReturn(null);
    doAnswer(
            inv -> {
              when(def.getDescription()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(def)
        .setDescription(any());
    doAnswer(
            inv -> {
              when(def.getLabel()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(def)
        .setLabel(any());
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));
    return def;
  }
}

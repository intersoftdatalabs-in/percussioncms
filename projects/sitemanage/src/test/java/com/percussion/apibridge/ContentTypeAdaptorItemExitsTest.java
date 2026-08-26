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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSConditional;
import com.percussion.design.objectstore.PSConditionalExit;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSExtensionCallSet;
import com.percussion.design.objectstore.PSExtensionParamValue;
import com.percussion.design.objectstore.PSInputTranslations;
import com.percussion.design.objectstore.PSOutputTranslations;
import com.percussion.design.objectstore.PSPipe;
import com.percussion.design.objectstore.PSRule;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.design.objectstore.PSValidationRules;
import com.percussion.extension.PSExtensionRef;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.rest.contenttypes.ContentTypeItemExit;
import com.percussion.rest.contenttypes.ContentTypeItemExitParam;
import com.percussion.rest.contenttypes.ContentTypeItemExits;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.locking.data.PSObjectLockSummary;
import com.percussion.util.PSCollection;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
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
 * CD-09: item-level exits GET maps design-WS objects; PUT replace requires a held lock and
 * persists via {@code saveContentTypes}.
 */
@Tag("UnitTest")
class ContentTypeAdaptorItemExitsTest {

  private IPSContentDesignWs designSvc;
  private PSItemDefManager itemDefManager;
  private IPSSystemDesignWs systemDesign;
  private ContentTypeAdaptor adaptor;
  private PSItemDefinition percPage;
  private PSContentEditor editor;

  @BeforeEach
  void setUp() throws Exception {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "test-user");

    designSvc = mock(IPSContentDesignWs.class);
    itemDefManager = mock(PSItemDefManager.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    adaptor = new ContentTypeAdaptor(designSvc, itemDefManager, systemDesign, () -> true);

    percPage = mock(PSItemDefinition.class);
    editor = mock(PSContentEditor.class);
    when(percPage.getTypeId()).thenReturn(311);
    when(percPage.getContentEditor()).thenReturn(editor);
    when(itemDefManager.getItemDef("percPage", PSItemDefManager.COMMUNITY_ANY)).thenReturn(percPage);
    when(itemDefManager.getItemDef(311L, PSItemDefManager.COMMUNITY_ANY)).thenReturn(percPage);

    IPSGuid guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
    IPSCatalogSummary summary = mock(IPSCatalogSummary.class);
    when(summary.getGUID()).thenReturn(guid);
    when(summary.getName()).thenReturn("percPage");
    when(summary.getLabel()).thenReturn("Page");
    when(designSvc.findContentTypes("percPage")).thenReturn(List.of(summary));
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void getItemExits_mapsTranslationsValidationsAndPipeExits() {
    when(editor.getInputTranslations()).thenReturn(singleExit("sys_ToUpperCase").iterator());
    when(editor.getOutputTranslations()).thenReturn(Collections.emptyIterator());
    when(editor.getValidationRules()).thenReturn(singleExit("sys_ValidateRequired").iterator());
    when(editor.getMaxErrorsToStopValidation()).thenReturn(5);
    PSPipe pipe = mock(PSPipe.class);
    PSExtensionCallSet pre = new PSExtensionCallSet();
    pre.add(extensionCall("sys_PreProcess"));
    when(pipe.getInputDataExtensions()).thenReturn(pre);
    when(pipe.getResultDataExtensions()).thenReturn(new PSExtensionCallSet());
    when(editor.getPipe()).thenReturn(pipe);

    ContentTypeItemExits out = adaptor.getItemExits(null, "percPage");
    assertNotNull(out);
    assertEquals(1, out.getInputTranslations().size());
    assertTrue(out.getInputTranslations().get(0).getExtension().contains("sys_ToUpperCase"));
    assertTrue(out.getOutputTranslations().isEmpty());
    assertEquals(1, out.getValidations().size());
    assertTrue(out.getValidations().get(0).getExtension().contains("sys_ValidateRequired"));
    assertEquals(Integer.valueOf(5), out.getMaxErrorsToStopValidation());
    assertEquals(1, out.getPreExits().size());
    assertTrue(out.getPreExits().get(0).getName().contains("sys_PreProcess"));
    assertTrue(out.getPostExits().isEmpty());
    assertEquals("CT_ITEM_EXIT_CONDITIONS", out.getDesignGaps().get(0).getCode());
  }

  @Test
  void getItemExits_missingType_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("missing"));
    assertNull(adaptor.getItemExits(null, "missing"));
  }

  @Test
  void getItemExits_cacheMiss_doesNotUseObjectStoreFallback() throws Exception {
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY)))
        .thenThrow(new PSInvalidContentTypeException("311"));
    ContentTypeAdaptor spy = spy(adaptor);
    doReturn(percPage).when(spy).loadItemDefFromObjectStore("311");
    assertNull(spy.getItemExits(null, "311"));
    verify(spy, never()).loadItemDefFromObjectStore("311");
  }

  @Test
  void replaceItemExits_withHeldLock_persistsAndReturnsEnvelope() throws Exception {
    stubHeldLock();
    when(editor.getInputTranslations()).thenReturn(Collections.emptyIterator());
    when(editor.getOutputTranslations()).thenReturn(Collections.emptyIterator());
    when(editor.getValidationRules()).thenReturn(Collections.emptyIterator());
    when(editor.getMaxErrorsToStopValidation()).thenReturn(10);
    when(editor.getPipe()).thenReturn(null);
    when(designSvc.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("test-user")))
        .thenReturn(List.of(percPage));

    ContentTypeItemExits body = emptyBody();
    ContentTypeItemExit call = new ContentTypeItemExit();
    call.setExtension("Java/global/percussion/generic/sys_ToUpperCase");
    call.setParameters(List.of(new ContentTypeItemExitParam(null, "sys_title")));
    body.setInputTranslations(List.of(call));

    ContentTypeItemExits out = adaptor.replaceItemExits(null, "percPage", body);
    assertNotNull(out);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSItemDefinition>> saved = ArgumentCaptor.forClass(List.class);
    verify(designSvc)
        .saveContentTypes(saved.capture(), eq(false), eq("test-session"), eq("test-user"));
    assertEquals(1, saved.getValue().size());
    verify(editor).setInputTranslation(any(PSInputTranslations.class));
    verify(editor).setOutputTranslation(any(PSOutputTranslations.class));
    verify(editor).setValidationRules(any(PSValidationRules.class));
  }

  @Test
  void replaceItemExits_withoutLock_throws409() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("test-user")))
        .thenReturn(Collections.singletonList(null));

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.replaceItemExits(null, "percPage", emptyBody()));
    assertTrue(ex.getMessage().contains("design lock required"));
    verify(designSvc, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void replaceItemExits_lockedByOtherUser_throws409() throws Exception {
    PSObjectSummary summary = new PSObjectSummary();
    summary.setLocked(new PSObjectLockSummary("other-session", "editor", 30));
    when(systemDesign.isLocked(anyList(), eq("test-user")))
        .thenReturn(Collections.singletonList(summary));

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.replaceItemExits(null, "percPage", emptyBody()));
    assertTrue(ex.getMessage().contains("locked by editor"));
    verify(designSvc, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void replaceItemExits_invalidExtension_throws400() throws Exception {
    stubHeldLock();
    when(designSvc.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("test-user")))
        .thenReturn(List.of(percPage));

    ContentTypeItemExits body = emptyBody();
    ContentTypeItemExit call = new ContentTypeItemExit();
    call.setExtension("not a valid fqn");
    body.setInputTranslations(List.of(call));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.replaceItemExits(null, "percPage", body));
    assertTrue(ex.getMessage().contains("invalid extension FQN"), ex.getMessage());
    verify(designSvc, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void replaceItemExits_nullRequiredLists_throws400() {
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.replaceItemExits(null, "percPage", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.replaceItemExits(null, "percPage", new ContentTypeItemExits()));
  }

  @Test
  void toExtensionCall_roundTripsFqnAndLiteralParams() {
    ContentTypeItemExit dto = new ContentTypeItemExit();
    dto.setExtension("Java/global/percussion/generic/sys_ToUpperCase");
    dto.setParameters(List.of(new ContentTypeItemExitParam("ignored", "sys_title")));
    PSExtensionCall call = ContentTypeAdaptor.toExtensionCall(dto, "inputTranslations[0]");
    assertEquals("sys_ToUpperCase", call.getExtensionRef().getExtensionName());
    assertEquals("Java/global/percussion/generic/sys_ToUpperCase", call.getExtensionRef().getFQN());
    assertEquals(1, call.getParamValues().length);
    assertEquals("sys_title", call.getParamValues()[0].getValue().getValueDisplayText());
  }

  @Test
  void summarizeApplyWhen_emptyAndPresent() {
    assertNull(ContentTypeAdaptor.summarizeApplyWhen(null));
    com.percussion.design.objectstore.PSApplyWhen empty =
        new com.percussion.design.objectstore.PSApplyWhen();
    assertNull(ContentTypeAdaptor.summarizeApplyWhen(empty));

    PSCollection conditionals = new PSCollection(PSConditional.class);
    conditionals.add(
        new PSConditional(
            new PSTextLiteral("sys_communityid"),
            PSConditional.OPTYPE_EQUALS,
            new PSTextLiteral("1001")));
    com.percussion.design.objectstore.PSApplyWhen when =
        new com.percussion.design.objectstore.PSApplyWhen();
    when.add(new PSRule(conditionals));
    String summary = ContentTypeAdaptor.summarizeApplyWhen(when);
    assertTrue(summary.contains("sys_communityid"), summary);
  }

  @Test
  void mapConditionalExits_copiesConditionAndMaxErrors() {
    PSConditionalExit exit = new PSConditionalExit(singleCallSet("sys_ValidateRequired"));
    exit.setMaxErrorsToStop(3);
    com.percussion.design.objectstore.PSApplyWhen when =
        new com.percussion.design.objectstore.PSApplyWhen();
    PSCollection conditionals = new PSCollection(PSConditional.class);
    conditionals.add(
        new PSConditional(
            new PSTextLiteral("sys_title"),
            PSConditional.OPTYPE_NOTEQUALS,
            new PSTextLiteral("")));
    when.add(new PSRule(conditionals));
    exit.setCondition(when);

    PSInputTranslations col = new PSInputTranslations();
    col.add(exit);
    List<ContentTypeItemExit> mapped = ContentTypeAdaptor.mapConditionalExits(col.iterator());
    assertEquals(1, mapped.size());
    assertEquals(Integer.valueOf(3), mapped.get(0).getMaxErrorsToStop());
    assertTrue(mapped.get(0).getCondition().contains("sys_title"), mapped.get(0).getCondition());
  }

  @Test
  void itemExitDesignGaps_areStructured() {
    assertEquals("CT_ITEM_EXIT_CONDITIONS", ContentTypeAdaptor.itemExitDesignGaps().get(0).getCode());
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary summary = new PSObjectSummary();
    summary.setLocked(new PSObjectLockSummary("test-session", "test-user", 30));
    when(systemDesign.isLocked(anyList(), eq("test-user")))
        .thenReturn(Collections.singletonList(summary));
  }

  private static ContentTypeItemExits emptyBody() {
    ContentTypeItemExits body = new ContentTypeItemExits();
    body.setInputTranslations(List.of());
    body.setOutputTranslations(List.of());
    body.setValidations(List.of());
    return body;
  }

  private static PSInputTranslations singleExit(String extensionName) {
    PSInputTranslations col = new PSInputTranslations();
    col.add(new PSConditionalExit(singleCallSet(extensionName)));
    return col;
  }

  private static PSExtensionCallSet singleCallSet(String extensionName) {
    PSExtensionCallSet set = new PSExtensionCallSet();
    set.add(extensionCall(extensionName));
    return set;
  }

  private static PSExtensionCall extensionCall(String extensionName) {
    return new PSExtensionCall(
        new PSExtensionRef("Java", "global/percussion/generic/", extensionName),
        new PSExtensionParamValue[0]);
  }
}

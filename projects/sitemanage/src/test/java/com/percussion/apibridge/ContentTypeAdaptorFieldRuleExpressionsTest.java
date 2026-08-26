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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSConditional;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSFieldTranslation;
import com.percussion.design.objectstore.PSFieldValidationRules;
import com.percussion.design.objectstore.PSRule;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.design.objectstore.PSVisibilityRules;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.rest.contenttypes.ContentTypeFieldConditional;
import com.percussion.rest.contenttypes.ContentTypeFieldRule;
import com.percussion.rest.contenttypes.ContentTypeFieldRuleExpressions;
import com.percussion.rest.contenttypes.ContentTypeItemExit;
import com.percussion.rest.contenttypes.ContentTypeItemExitParam;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.util.PSCollection;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
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
 * CD-05–07 field rule expressions: GET maps design objects; PUT requires a held lock and persists
 * via {@code saveContentTypes}.
 */
@Tag("UnitTest")
class ContentTypeAdaptorFieldRuleExpressionsTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private PSItemDefManager itemDefManager;
  private ContentTypeAdaptor adaptor;
  private IPSGuid guid;
  private PSField field;

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
  void toPsConditional_mapsAliasAndRoundTrips() {
    ContentTypeFieldConditional body = new ContentTypeFieldConditional("sys_title", "!=", "");
    PSConditional cond = ContentTypeAdaptor.toPsConditional(body, "validation[0].conditionals[0]");
    assertEquals(PSConditional.OPTYPE_NOTEQUALS, cond.getOperator());
    assertEquals("sys_title", cond.getVariable().getValueText());

    List<ContentTypeFieldRule> mapped =
        ContentTypeAdaptor.toFieldRules(new PSRule(singleConditional(cond)));
    assertEquals(1, mapped.size());
    assertEquals(ContentTypeFieldRule.TYPE_CONDITIONAL, mapped.get(0).getType());
    assertEquals("sys_title", mapped.get(0).getConditionals().get(0).getVariable());
    assertEquals("<>", mapped.get(0).getConditionals().get(0).getOperator());
  }

  @Test
  void toPsRule_extensionRoundTrip() {
    ContentTypeFieldRule dto = new ContentTypeFieldRule();
    dto.setType(ContentTypeFieldRule.TYPE_EXTENSION);
    dto.setExtension("Java/global/percussion/generic/sys_ToUpperCase");
    dto.setParameters(List.of(new ContentTypeItemExitParam(null, "sys_title")));
    PSRule rule = ContentTypeAdaptor.toPsRule(dto, "input[0]");
    assertTrue(rule.isExtensionSetRule());
    List<ContentTypeFieldRule> mapped = ContentTypeAdaptor.toFieldRules(rule);
    assertEquals(1, mapped.size());
    assertEquals(ContentTypeFieldRule.TYPE_EXTENSION, mapped.get(0).getType());
    assertTrue(mapped.get(0).getExtension().contains("sys_ToUpperCase"));
  }

  @Test
  void toFieldValidationRules_emptyClears() {
    ContentTypeFieldRuleExpressions body = emptyBody();
    assertNull(ContentTypeAdaptor.toFieldValidationRules(body));
    assertNull(ContentTypeAdaptor.toVisibilityRules(body.getVisibility(), "visibility"));
    assertNull(ContentTypeAdaptor.toFieldTranslation(body.getInputTranslation(), "inputTranslation"));
  }

  @Test
  void toVisibilityRules_rejectsReference() {
    ContentTypeFieldRule ref = new ContentTypeFieldRule();
    ref.setType(ContentTypeFieldRule.TYPE_REFERENCE);
    ref.setReference("sharedRequired");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> ContentTypeAdaptor.toVisibilityRules(List.of(ref), "visibility"));
    assertTrue(ex.getMessage().contains("reference"), ex.getMessage());
  }

  @Test
  void toPsConditional_requiresVariableAndOperator() {
    ContentTypeFieldConditional missingVar = new ContentTypeFieldConditional();
    missingVar.setOperator("=");
    IllegalArgumentException varEx =
        assertThrows(
            IllegalArgumentException.class,
            () -> ContentTypeAdaptor.toPsConditional(missingVar, "c"));
    assertTrue(varEx.getMessage().contains("variable"), varEx.getMessage());

    ContentTypeFieldConditional missingOp = new ContentTypeFieldConditional();
    missingOp.setVariable("sys_title");
    IllegalArgumentException opEx =
        assertThrows(
            IllegalArgumentException.class,
            () -> ContentTypeAdaptor.toPsConditional(missingOp, "c"));
    assertTrue(opEx.getMessage().contains("operator"), opEx.getMessage());
  }

  @Test
  void get_returnsMappedRulesAndSummary() throws Exception {
    stubDefinition();
    stubExistingValidation();

    ContentTypeFieldRuleExpressions out =
        adaptor.getFieldRuleExpressions(null, "311", "sys_title");
    assertEquals("sys_title", out.getFieldName());
    assertEquals(1, out.getValidation().size());
    assertEquals("sys_title", out.getValidation().get(0).getConditionals().get(0).getVariable());
    assertTrue(out.getValidationExpression().contains("sys_title"), out.getValidationExpression());
    assertTrue(out.getVisibility().isEmpty());
    assertTrue(out.getInputTranslation().isEmpty());
    assertEquals("CT_FIELD_RULE_APPLY_WHEN", out.getDesignGaps().get(0).getCode());
  }

  @Test
  void get_unknownField_is404() throws Exception {
    stubDefinition();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.getFieldRuleExpressions(null, "311", "nope"));
    assertEquals(404, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Unknown field"), ex.getMessage());
  }

  @Test
  void get_unknownType_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("missing"));
    assertNull(adaptor.getFieldRuleExpressions(null, "missing", "sys_title"));
  }

  @Test
  void put_persistsWhenLockHeld() throws Exception {
    stubHeldLock();
    stubDefinition();

    ContentTypeFieldRuleExpressions body = emptyBody();
    ContentTypeFieldRule rule = new ContentTypeFieldRule();
    rule.setType(ContentTypeFieldRule.TYPE_CONDITIONAL);
    rule.setConditionals(List.of(new ContentTypeFieldConditional("sys_title", "<>", "")));
    body.setValidation(List.of(rule));
    ContentTypeItemExit call = new ContentTypeItemExit();
    call.setExtension("Java/global/percussion/generic/sys_ToUpperCase");
    call.setParameters(List.of(new ContentTypeItemExitParam(null, "sys_title")));
    body.setInputTranslation(List.of(call));

    ContentTypeFieldRuleExpressions out =
        adaptor.replaceFieldRuleExpressions(null, "311", "sys_title", body);

    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    ArgumentCaptor<PSFieldValidationRules> rules =
        ArgumentCaptor.forClass(PSFieldValidationRules.class);
    verify(field).setValidationRules(rules.capture());
    assertTrue(rules.getValue().getRules().hasNext());
    ArgumentCaptor<PSFieldTranslation> in = ArgumentCaptor.forClass(PSFieldTranslation.class);
    verify(field).setInputTranslation(in.capture());
    assertTrue(in.getValue().getTranslations().size() > 0);
    assertEquals("sys_title", out.getFieldName());
    assertEquals(1, out.getValidation().size());
    assertEquals(1, out.getInputTranslation().size());
  }

  @Test
  void put_emptyListsClear() throws Exception {
    stubHeldLock();
    stubDefinition();
    ContentTypeFieldRuleExpressions out =
        adaptor.replaceFieldRuleExpressions(null, "311", "sys_title", emptyBody());
    verify(field).setValidationRules(null);
    verify(field).setVisibilityRules(null);
    verify(field).setInputTranslation(null);
    verify(field).setOutputTranslation(null);
    assertTrue(out.getValidation().isEmpty());
  }

  @Test
  void put_cacheMissAfterSave_fallsBackToLockedField() throws Exception {
    stubHeldLock();
    stubDefinition();
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY)))
        .thenThrow(new PSInvalidContentTypeException("not cached"));

    ContentTypeFieldRuleExpressions body = emptyBody();
    ContentTypeFieldRule rule = new ContentTypeFieldRule();
    rule.setType(ContentTypeFieldRule.TYPE_CONDITIONAL);
    rule.setConditionals(List.of(new ContentTypeFieldConditional("sys_title", "=", "x")));
    body.setValidation(List.of(rule));

    ContentTypeFieldRuleExpressions out =
        adaptor.replaceFieldRuleExpressions(null, "311", "sys_title", body);
    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals("sys_title", out.getFieldName());
    assertEquals(1, out.getValidation().size());
  }

  @Test
  void put_conflictWhenLockNotHeld() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of());
    stubDefinition();
    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.replaceFieldRuleExpressions(null, "311", "sys_title", emptyBody()));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_conflictWhenLockedByOtherUser() throws Exception {
    PSObjectSummary other = new PSObjectSummary(guid, "percPage");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));
    stubDefinition();
    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.replaceFieldRuleExpressions(null, "311", "sys_title", emptyBody()));
    assertTrue(ex.getMessage().contains("editor2"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_unknownField_isBadRequest() throws Exception {
    stubHeldLock();
    stubDefinition();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.replaceFieldRuleExpressions(null, "311", "nope", emptyBody()));
    assertTrue(ex.getMessage().contains("Unknown field"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_unknownType_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("missing"));
    assertNull(adaptor.replaceFieldRuleExpressions(null, "missing", "sys_title", emptyBody()));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_missingLists_isBadRequest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.replaceFieldRuleExpressions(null, "311", "sys_title", null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            adaptor.replaceFieldRuleExpressions(
                null, "311", "sys_title", new ContentTypeFieldRuleExpressions()));
  }

  @Test
  void put_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.replaceFieldRuleExpressions(null, "311", "sys_title", emptyBody()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void fieldRuleDesignGaps_areStructured() {
    assertEquals(
        "CT_FIELD_RULE_APPLY_WHEN", ContentTypeAdaptor.fieldRuleDesignGaps().get(0).getCode());
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary held = new PSObjectSummary(guid, "percPage");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }

  private PSItemDefinition stubDefinition() throws Exception {
    field = mock(PSField.class);
    when(field.getSubmitName()).thenReturn("sys_title");
    final PSFieldValidationRules[] storedVal = new PSFieldValidationRules[1];
    final PSVisibilityRules[] storedVis = new PSVisibilityRules[1];
    final PSFieldTranslation[] storedIn = new PSFieldTranslation[1];
    final PSFieldTranslation[] storedOut = new PSFieldTranslation[1];
    when(field.getValidationRules()).thenAnswer(inv -> storedVal[0]);
    when(field.getVisibilityRules()).thenAnswer(inv -> storedVis[0]);
    when(field.getInputTranslation()).thenAnswer(inv -> storedIn[0]);
    when(field.getOutputTranslation()).thenAnswer(inv -> storedOut[0]);
    doAnswer(
            inv -> {
              storedVal[0] = inv.getArgument(0);
              return null;
            })
        .when(field)
        .setValidationRules(any());
    doAnswer(
            inv -> {
              storedVis[0] = inv.getArgument(0);
              return null;
            })
        .when(field)
        .setVisibilityRules(any());
    doAnswer(
            inv -> {
              storedIn[0] = inv.getArgument(0);
              return null;
            })
        .when(field)
        .setInputTranslation(any());
    doAnswer(
            inv -> {
              storedOut[0] = inv.getArgument(0);
              return null;
            })
        .when(field)
        .setOutputTranslation(any());

    PSFieldSet fieldSet = mock(PSFieldSet.class);
    when(fieldSet.findFieldByName("sys_title", false)).thenReturn(field);
    when(fieldSet.findFieldByName("nope", false)).thenReturn(null);

    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn("percPage");
    when(def.getTypeId()).thenReturn(311);
    when(def.getFieldSet()).thenReturn(fieldSet);
    when(def.getComplexChildren()).thenReturn(List.of());
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);
    when(designWs.loadContentTypes(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(def));
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));
    return def;
  }

  private void stubExistingValidation() {
    PSCollection conditionals = new PSCollection(PSConditional.class);
    conditionals.add(
        new PSConditional(
            new PSTextLiteral("sys_title"),
            PSConditional.OPTYPE_NOTEQUALS,
            new PSTextLiteral("")));
    PSFieldValidationRules rules = new PSFieldValidationRules();
    PSCollection ruleCol = new PSCollection(PSRule.class);
    ruleCol.add(new PSRule(conditionals));
    rules.setRules(ruleCol);
    when(field.getValidationRules()).thenReturn(rules);
  }

  private static ContentTypeFieldRuleExpressions emptyBody() {
    ContentTypeFieldRuleExpressions body = new ContentTypeFieldRuleExpressions();
    body.setValidation(List.of());
    body.setVisibility(List.of());
    body.setInputTranslation(List.of());
    body.setOutputTranslation(List.of());
    return body;
  }

  @SuppressWarnings("unchecked")
  private static PSCollection singleConditional(PSConditional cond) {
    PSCollection conditionals = new PSCollection(PSConditional.class);
    conditionals.add(cond);
    return conditionals;
  }
}

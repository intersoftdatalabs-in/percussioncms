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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSConditional;
import com.percussion.design.objectstore.PSControlRef;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSExtensionCallSet;
import com.percussion.design.objectstore.PSExtensionParamValue;
import com.percussion.design.objectstore.PSFieldTranslation;
import com.percussion.design.objectstore.PSFieldValidationRules;
import com.percussion.design.objectstore.PSParam;
import com.percussion.design.objectstore.PSRule;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.design.objectstore.PSVisibilityRules;
import com.percussion.extension.PSExtensionRef;
import com.percussion.util.PSCollection;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral unit tests for ContentTypeAdaptor field-rule mapping helpers (P0.2c flags + CD-05–07
 * read-only expressions).
 */
@Tag("UnitTest")
public class ContentTypeAdaptorFieldRulesTest {

  @Test
  public void mapOccurrenceCoversAllKnownDimensions() {
    assertEquals(
        "optional",
        ContentTypeAdaptor.mapOccurrence(
            com.percussion.design.objectstore.PSField.OCCURRENCE_DIMENSION_OPTIONAL));
    assertEquals(
        "required",
        ContentTypeAdaptor.mapOccurrence(
            com.percussion.design.objectstore.PSField.OCCURRENCE_DIMENSION_REQUIRED));
    assertEquals(
        "oneOrMore",
        ContentTypeAdaptor.mapOccurrence(
            com.percussion.design.objectstore.PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE));
    assertEquals(
        "zeroOrMore",
        ContentTypeAdaptor.mapOccurrence(
            com.percussion.design.objectstore.PSField.OCCURRENCE_DIMENSION_ZERO_OR_MORE));
    assertEquals(
        "count",
        ContentTypeAdaptor.mapOccurrence(
            com.percussion.design.objectstore.PSField.OCCURRENCE_DIMENSION_COUNT));
    assertEquals("unknown", ContentTypeAdaptor.mapOccurrence(-1));
    assertEquals("unknown", ContentTypeAdaptor.mapOccurrence(99));
  }

  @Test
  public void hasTranslationNullSafe() {
    assertFalse(ContentTypeAdaptor.hasTranslation(null));

    PSFieldTranslation empty = mock(PSFieldTranslation.class);
    when(empty.getTranslations()).thenReturn(null);
    assertFalse(ContentTypeAdaptor.hasTranslation(empty));

    PSFieldTranslation emptySet = mock(PSFieldTranslation.class);
    when(emptySet.getTranslations()).thenReturn(new PSExtensionCallSet());
    assertFalse(ContentTypeAdaptor.hasTranslation(emptySet));

    PSExtensionCallSet calls = mock(PSExtensionCallSet.class);
    when(calls.isEmpty()).thenReturn(false);
    PSFieldTranslation present = mock(PSFieldTranslation.class);
    when(present.getTranslations()).thenReturn(calls);
    assertTrue(ContentTypeAdaptor.hasTranslation(present));
  }

  @Test
  public void summarizeValidationRulesEmptyAndNull() {
    assertNull(ContentTypeAdaptor.summarizeValidationRules(null));
    assertNull(ContentTypeAdaptor.summarizeValidationRules(new PSFieldValidationRules()));
  }

  @Test
  public void summarizeValidationRulesPresentConditional() {
    PSCollection conditionals = new PSCollection(PSConditional.class);
    conditionals.add(
        new PSConditional(
            new PSTextLiteral("sys_title"),
            PSConditional.OPTYPE_NOTEQUALS,
            new PSTextLiteral("")));
    PSRule rule = new PSRule(conditionals);

    PSFieldValidationRules rules = new PSFieldValidationRules();
    PSCollection ruleCol = new PSCollection(PSRule.class);
    ruleCol.add(rule);
    rules.setRules(ruleCol);

    String summary = ContentTypeAdaptor.summarizeValidationRules(rules);
    assertTrue(summary != null && !summary.isBlank(), "expected non-empty summary");
    assertTrue(summary.contains("sys_title"), summary);
    assertTrue(summary.contains("<>") || summary.contains("!="), summary);
  }

  @Test
  public void summarizeValidationRulesWithReference() {
    PSFieldValidationRules rules = new PSFieldValidationRules();
    PSCollection refs = new PSCollection(String.class);
    refs.add("sharedRequiredCheck");
    rules.setRuleReferences(refs);

    String summary = ContentTypeAdaptor.summarizeValidationRules(rules);
    assertEquals("ref:sharedRequiredCheck", summary);
  }

  @Test
  public void summarizeVisibilityRulesEmptyAndPresent() {
    assertNull(ContentTypeAdaptor.summarizeVisibilityRules(null));
    assertNull(ContentTypeAdaptor.summarizeVisibilityRules(new PSVisibilityRules()));

    PSCollection conditionals = new PSCollection(PSConditional.class);
    conditionals.add(
        new PSConditional(
            new PSTextLiteral("sys_communityid"),
            PSConditional.OPTYPE_EQUALS,
            new PSTextLiteral("1001")));
    PSVisibilityRules visibility = new PSVisibilityRules();
    visibility.add(new PSRule(conditionals));

    String summary = ContentTypeAdaptor.summarizeVisibilityRules(visibility);
    assertTrue(summary.contains("sys_communityid"), summary);
    assertTrue(summary.contains("1001"), summary);
  }

  @Test
  public void summarizeTranslationEmptyAndPresent() {
    assertNull(ContentTypeAdaptor.summarizeTranslation(null));

    PSFieldTranslation empty = new PSFieldTranslation(new PSExtensionCallSet());
    assertNull(ContentTypeAdaptor.summarizeTranslation(empty));

    PSExtensionCallSet set = new PSExtensionCallSet();
    set.add(
        new PSExtensionCall(
            new PSExtensionRef("Java", "global/percussion/generic/", "sys_ToUpperCase"),
            new PSExtensionParamValue[] {new PSExtensionParamValue(new PSTextLiteral("x"))}));
    PSFieldTranslation present = new PSFieldTranslation(set);

    String summary = ContentTypeAdaptor.summarizeTranslation(present);
    assertTrue(summary.contains("sys_ToUpperCase"), summary);
  }

  @Test
  public void summarizeExtensionRule() {
    PSExtensionCallSet set = new PSExtensionCallSet();
    set.add(
        new PSExtensionCall(
            new PSExtensionRef("Java", "global/percussion/generic/", "sys_ValidateDate"),
            new PSExtensionParamValue[0]));
    PSRule rule = new PSRule(set);
    String summary = ContentTypeAdaptor.summarizeRule(rule);
    assertTrue(summary.contains("sys_ValidateDate"), summary);
  }

  @Test
  public void controlPropertyNamesEmptyAndPresent() {
    assertTrue(ContentTypeAdaptor.controlPropertyNames(null).isEmpty());

    PSControlRef bare = new PSControlRef("sys_EditBox");
    assertTrue(ContentTypeAdaptor.controlPropertyNames(bare).isEmpty());

    PSCollection params = new PSCollection(PSParam.class);
    params.add(new PSParam("height", new PSTextLiteral("200")));
    params.add(new PSParam("width", new PSTextLiteral("400")));
    PSControlRef withParams = new PSControlRef("sys_TextArea");
    withParams.setParameters(params);
    List<String> names = ContentTypeAdaptor.controlPropertyNames(withParams);
    assertEquals(List.of("height", "width"), names);
  }
}

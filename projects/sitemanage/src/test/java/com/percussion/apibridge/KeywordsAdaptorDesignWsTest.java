/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.keywords.KeywordChoiceSummary;
import com.percussion.rest.keywords.KeywordNotFoundException;
import com.percussion.rest.keywords.KeywordSummary;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSKeyword;
import com.percussion.services.content.data.PSKeywordChoice;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.content.IPSContentDesignWs;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Keywords REST adaptor must call content <em>design</em> web service (Workbench path), not only
 * {@code IPSContentService}.
 */
@Tag("UnitTest")
class KeywordsAdaptorDesignWsTest {

  @BeforeEach
  void setRequestInfo() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "test-user");
  }

  @AfterEach
  void clearRequestInfo() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void listKeywords_usesFindAndLoadOnDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 42L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);

    PSKeyword kw = new PSKeyword();
    kw.setLabel("Colors");
    kw.setValue("colors");

    when(designWs.findKeywords(isNull())).thenReturn(List.of(sum));
    when(designWs.loadKeywords(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(kw));

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    List<KeywordSummary> list = adaptor.listKeywords(null, false);

    assertEquals(1, list.size());
    assertEquals("Colors", list.get(0).getLabel());
    // includeChoices=false leaves KeywordSummary choices as the default empty list
    assertNotNull(list.get(0).getChoices());
    assertTrue(list.get(0).getChoices().isEmpty());
    verify(designWs).findKeywords(null);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> ids = ArgumentCaptor.forClass(List.class);
    verify(designWs).loadKeywords(ids.capture(), eq(false), eq(false), any(), any());
    assertEquals(1, ids.getValue().size());
    assertEquals(guid, ids.getValue().get(0));
  }

  @Test
  void listKeywords_includeChoices_embedsSortedChoices() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 42L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);

    PSKeywordChoice late = new PSKeywordChoice();
    late.setLabel("Blue");
    late.setValue("blue");
    late.setSequence(2);
    PSKeywordChoice early = new PSKeywordChoice();
    early.setLabel("Red");
    early.setValue("red");
    early.setSequence(1);

    PSKeyword kw = new PSKeyword();
    kw.setLabel("Colors");
    kw.setValue("colors");
    kw.setChoices(List.of(late, early));

    when(designWs.findKeywords(isNull())).thenReturn(List.of(sum));
    when(designWs.loadKeywords(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(kw));

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    List<KeywordSummary> list = adaptor.listKeywords(null, true);

    assertEquals(1, list.size());
    assertNotNull(list.get(0).getChoices());
    assertEquals(2, list.get(0).getChoices().size());
    assertEquals("Red", list.get(0).getChoices().get(0).getLabel());
    assertEquals("Blue", list.get(0).getChoices().get(1).getLabel());
  }

  @Test
  void listKeywords_emptyFind_returnsEmptyWithoutLoad() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    when(designWs.findKeywords(isNull())).thenReturn(Collections.emptyList());

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    List<KeywordSummary> list = adaptor.listKeywords(null, true);

    assertNotNull(list);
    assertTrue(list.isEmpty());
    verify(designWs).findKeywords(null);
    verify(designWs, never()).loadKeywords(anyList(), anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void listKeywords_nullFind_returnsEmptyWithoutLoad() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    when(designWs.findKeywords(isNull())).thenReturn(null);

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    // Early return on null find is independent of includeChoices
    assertTrue(adaptor.listKeywords(null, false).isEmpty());
    assertTrue(adaptor.listKeywords(null, true).isEmpty());
    verify(designWs, never()).loadKeywords(anyList(), anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void listKeywords_skipsNullSummariesNullGuidsAndNonKeywordDefs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid goodGuid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 10L);
    IPSGuid choiceGuid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 11L);
    IPSCatalogSummary goodSum = mock(IPSCatalogSummary.class);
    when(goodSum.getGUID()).thenReturn(goodGuid);
    IPSCatalogSummary nullGuidSum = mock(IPSCatalogSummary.class);
    when(nullGuidSum.getGUID()).thenReturn(null);
    IPSCatalogSummary choiceSum = mock(IPSCatalogSummary.class);
    when(choiceSum.getGUID()).thenReturn(choiceGuid);

    PSKeyword def = new PSKeyword();
    def.setLabel("Status");
    def.setValue("status");
    def.setKeywordType(PSKeyword.KEYWORD_TYPE);

    PSKeyword choiceRow = new PSKeyword();
    choiceRow.setLabel("Open");
    choiceRow.setValue("open");
    // Choice rows use the parent keyword value as keywordType, not KEYWORD_TYPE
    choiceRow.setKeywordType("status");

    when(designWs.findKeywords(isNull()))
        .thenReturn(Arrays.asList(null, nullGuidSum, goodSum, choiceSum));
    when(designWs.loadKeywords(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(Arrays.asList(def, choiceRow, null));

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    List<KeywordSummary> list = adaptor.listKeywords(null, false);

    // Only KEYWORD_TYPE def rows appear; choice rows (keywordType="status") excluded
    assertEquals(1, list.size());
    assertEquals("Status", list.get(0).getLabel());
    assertEquals("status", list.get(0).getValue());
    assertTrue(
        list.stream().noneMatch(s -> "Open".equals(s.getLabel())),
        "non-keyword-def choice rows must not appear in list output");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> ids = ArgumentCaptor.forClass(List.class);
    verify(designWs).loadKeywords(ids.capture(), eq(false), eq(false), any(), any());
    assertEquals(2, ids.getValue().size());
    assertEquals(goodGuid, ids.getValue().get(0));
    assertEquals(choiceGuid, ids.getValue().get(1));
  }

  @Test
  void listKeywords_designWsError_throwsIllegalState() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 1L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(designWs.findKeywords(isNull())).thenReturn(List.of(sum));
    when(designWs.loadKeywords(anyList(), eq(false), eq(false), any(), any()))
        .thenThrow(new PSErrorResultsException());

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.listKeywords(null, false));
    assertTrue(ex.getMessage().contains("Failed to list keywords"));
  }

  @Test
  void createKeyword_usesCreateAndSaveOnDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 7L);
    PSKeyword created = new PSKeyword();
    created.setGUID(guid);
    created.setLabel("NewKw");
    created.setValue("newkw");

    when(designWs.findKeywords("NewKw")).thenReturn(Collections.emptyList());
    when(designWs.createKeywords(eq(List.of("NewKw")), eq("test-session"), eq("test-user")))
        .thenReturn(List.of(created));
    when(designWs.loadKeywords(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(created));

    KeywordSummary body = new KeywordSummary();
    body.setLabel("NewKw");
    body.setDescription("desc");
    body.setSequence(3);
    KeywordChoiceSummary choice = new KeywordChoiceSummary();
    choice.setLabel("Red");
    choice.setValue("red");
    body.setChoices(List.of(choice));

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    KeywordSummary out = adaptor.createKeyword(null, body);

    assertNotNull(out);
    assertEquals("NewKw", out.getLabel());
    verify(designWs).createKeywords(eq(List.of("NewKw")), eq("test-session"), eq("test-user"));
    verify(designWs).saveKeywords(anyList(), eq(true), eq("test-session"), eq("test-user"));
  }

  @Test
  void createKeyword_duplicateLabel_throwsBeforeCreate() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSCatalogSummary existing = mock(IPSCatalogSummary.class);
    when(existing.getName()).thenReturn("Colors");
    when(existing.getLabel()).thenReturn("Colors");
    when(existing.getGUID()).thenReturn(new PSGuid(PSTypeEnum.KEYWORD_DEF, 1L));
    when(designWs.findKeywords("Colors")).thenReturn(List.of(existing));

    KeywordSummary body = new KeywordSummary();
    body.setLabel("Colors");
    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createKeyword(null, body));
    assertTrue(ex.getMessage().contains("already in use"));
    verify(designWs, never()).createKeywords(anyList(), any(), any());
  }

  @Test
  void updateKeyword_loadsWithLockAndSaves() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 9L);
    PSKeyword kw = new PSKeyword();
    kw.setGUID(guid);
    kw.setLabel("Old");
    kw.setValue("old");

    when(designWs.loadKeywords(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(kw));
    when(designWs.findKeywords("Updated")).thenReturn(Collections.emptyList());
    when(designWs.loadKeywords(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(kw));

    KeywordSummary body = new KeywordSummary();
    body.setLabel("Updated");
    body.setDescription("d");

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    KeywordSummary out = adaptor.updateKeyword(null, String.valueOf(guid.getUUID()), body);

    assertNotNull(out);
    verify(designWs).loadKeywords(eq(List.of(guid)), eq(true), eq(false), any(), any());
    verify(designWs).saveKeywords(anyList(), eq(true), eq("test-session"), eq("test-user"));
  }

  @Test
  void updateKeyword_notFoundViaIsNotFound_returnsNull() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 11L);
    PSErrorResultsException err = new PSErrorResultsException();
    err.addError(guid, "missing");
    when(designWs.loadKeywords(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenThrow(err);

    KeywordSummary body = new KeywordSummary();
    body.setLabel("X");
    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);

    assertNull(adaptor.updateKeyword(null, String.valueOf(guid.getUUID()), body));
    verify(designWs, never()).saveKeywords(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void deleteKeyword_loadsThenDeletesOnDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 5L);
    PSKeyword kw = new PSKeyword();
    kw.setGUID(guid);
    when(designWs.loadKeywords(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(kw));

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    adaptor.deleteKeyword(null, String.valueOf(guid.getUUID()));

    verify(designWs).loadKeywords(eq(List.of(guid)), eq(false), eq(false), any(), any());
    verify(designWs)
        .deleteKeywords(eq(List.of(guid)), eq(false), eq("test-session"), eq("test-user"));
  }

  @Test
  void deleteKeyword_missingOnLoad_throwsKeywordNotFound() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 6L);
    when(designWs.loadKeywords(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenThrow(new PSErrorResultsException());

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    assertThrows(
        KeywordNotFoundException.class,
        () -> adaptor.deleteKeyword(null, String.valueOf(guid.getUUID())));
    verify(designWs, never()).deleteKeywords(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void getKeyword_byNumericId_usesLoadKeywords() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 42L);
    PSKeyword kw = new PSKeyword();
    kw.setGUID(guid);
    kw.setLabel("Colors");
    kw.setValue("colors");
    when(designWs.loadKeywords(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(kw));

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    KeywordSummary out = adaptor.getKeyword(null, "42");

    assertNotNull(out);
    assertEquals("Colors", out.getLabel());
    // get always embeds choices
    assertNotNull(out.getChoices());
    verify(designWs).loadKeywords(eq(List.of(guid)), eq(false), eq(false), any(), any());
  }

  @Test
  void getKeyword_byGuidShape_usesLoadKeywords() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 42L);
    PSKeyword kw = new PSKeyword();
    kw.setGUID(guid);
    kw.setLabel("ByGuid");
    kw.setValue("byguid");
    when(designWs.loadKeywords(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(kw));

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    KeywordSummary out = adaptor.getKeyword(null, guid.toString());

    assertNotNull(out);
    assertEquals("ByGuid", out.getLabel());
    // get always embeds choices (toSummary(..., true))
    assertNotNull(out.getChoices());
  }

  @Test
  void getKeyword_byLabel_loadsAllAndMatches() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 3L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSKeyword kw = new PSKeyword();
    kw.setGUID(guid);
    kw.setLabel("Seasons");
    kw.setValue("seasons");

    when(designWs.findKeywords(isNull())).thenReturn(List.of(sum));
    when(designWs.loadKeywords(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(kw));

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    KeywordSummary out = adaptor.getKeyword(null, "Seasons");

    assertNotNull(out);
    assertEquals("seasons", out.getValue());
  }

  @Test
  void getKeyword_byValue_loadsAllAndMatches() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.KEYWORD_DEF, 4L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSKeyword kw = new PSKeyword();
    kw.setGUID(guid);
    kw.setLabel("Priority");
    kw.setValue("priority");

    when(designWs.findKeywords(isNull())).thenReturn(List.of(sum));
    when(designWs.loadKeywords(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(kw));

    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    KeywordSummary out = adaptor.getKeyword(null, "priority");

    assertNotNull(out);
    assertEquals("Priority", out.getLabel());
  }

  @Test
  void getKeyword_blank_returnsNullWithoutDesignCall() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);

    assertNull(adaptor.getKeyword(null, null));
    assertNull(adaptor.getKeyword(null, "   "));
    verify(designWs, never()).findKeywords(any());
    verify(designWs, never()).loadKeywords(anyList(), anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void createKeyword_nullOrBlankLabel_throwsBeforeDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);

    assertThrows(IllegalArgumentException.class, () -> adaptor.createKeyword(null, null));
    KeywordSummary blank = new KeywordSummary();
    blank.setLabel("  ");
    assertThrows(IllegalArgumentException.class, () -> adaptor.createKeyword(null, blank));
    verify(designWs, never()).createKeywords(anyList(), any(), any());
  }

  @Test
  void updateKeyword_validationErrors_throwBeforeDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    KeywordSummary body = new KeywordSummary();
    body.setLabel("X");

    assertThrows(IllegalArgumentException.class, () -> adaptor.updateKeyword(null, "  ", body));
    assertThrows(IllegalArgumentException.class, () -> adaptor.updateKeyword(null, "9", null));
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.updateKeyword(null, "not-a-guid", body));
    verify(designWs, never()).loadKeywords(anyList(), anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void deleteKeyword_validationErrors_throwBeforeDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);

    assertThrows(IllegalArgumentException.class, () -> adaptor.deleteKeyword(null, " "));
    assertThrows(IllegalArgumentException.class, () -> adaptor.deleteKeyword(null, "xyz"));
    verify(designWs, never()).loadKeywords(anyList(), anyBoolean(), anyBoolean(), any(), any());
    verify(designWs, never()).deleteKeywords(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void writeOperations_requireSessionAndUser() throws Exception {
    PSRequestInfoBase.resetRequestInfo();
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    // no KEY_JSESSIONID / KEY_USER

    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    KeywordsAdaptor adaptor = new KeywordsAdaptor(designWs);
    KeywordSummary body = new KeywordSummary();
    body.setLabel("NeedsSession");

    IllegalStateException createEx =
        assertThrows(IllegalStateException.class, () -> adaptor.createKeyword(null, body));
    assertTrue(createEx.getMessage().contains("session and user"));

    IllegalStateException updateEx =
        assertThrows(IllegalStateException.class, () -> adaptor.updateKeyword(null, "9", body));
    assertTrue(updateEx.getMessage().contains("session and user"));

    IllegalStateException deleteEx =
        assertThrows(IllegalStateException.class, () -> adaptor.deleteKeyword(null, "9"));
    assertTrue(deleteEx.getMessage().contains("session and user"));
  }

  @Test
  void isNotFound_requiresErrorForRequestedGuidOnly() {
    IPSGuid requested = new PSGuid(PSTypeEnum.KEYWORD_DEF, 1L);
    IPSGuid other = new PSGuid(PSTypeEnum.KEYWORD_DEF, 2L);

    PSErrorResultsException partial = new PSErrorResultsException();
    partial.addError(other, "fail");
    partial.addResult(requested, new Object());
    assertFalse(KeywordsAdaptor.isNotFound(partial, requested));

    PSErrorResultsException missing = new PSErrorResultsException();
    missing.addError(requested, "missing");
    assertTrue(KeywordsAdaptor.isNotFound(missing, requested));

    assertFalse(KeywordsAdaptor.isNotFound(null, requested));
    assertFalse(KeywordsAdaptor.isNotFound(missing, null));
  }
}

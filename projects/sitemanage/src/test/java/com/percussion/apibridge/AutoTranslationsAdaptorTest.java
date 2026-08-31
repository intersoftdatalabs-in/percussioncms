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

import com.percussion.rest.locales.AutoTranslationDesignLockException;
import com.percussion.rest.locales.AutoTranslationRow;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSAutoTranslation;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag("UnitTest")
class AutoTranslationsAdaptorTest {

  private IPSContentDesignWs designWs;
  private IPSCatalogSummary localeFr;
  private IPSCatalogSummary typePage;
  private IPSCatalogSummary workflowDefault;
  private IPSCatalogSummary communityDefault;
  private IPSGuid typeGuid;
  private IPSGuid workflowGuid;
  private IPSGuid communityGuid;

  @BeforeEach
  void setRequestInfo() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");

    designWs = mock(IPSContentDesignWs.class);
    localeFr = summary("fr-fr", new PSGuid(PSTypeEnum.LOCALE, 7L));
    typeGuid = new PSGuid(PSTypeEnum.NODEDEF, 200L);
    typePage = summary("percPage", typeGuid);
    workflowGuid = new PSGuid(PSTypeEnum.WORKFLOW, 4L);
    workflowDefault = summary("Default Workflow", workflowGuid);
    communityGuid = new PSGuid(PSTypeEnum.COMMUNITY_DEF, 10L);
    communityDefault = summary("Default", communityGuid);
  }

  @AfterEach
  void clearRequestInfo() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void getAutoTranslations_mapsRowsAndFillsNames() throws Exception {
    PSAutoTranslation at = new PSAutoTranslation();
    at.setLocale("fr-fr");
    at.setContentTypeId(typeGuid.longValue());
    at.setWorkflowId(workflowGuid.longValue());
    at.setCommunityId(communityGuid.longValue());
    when(designWs.loadTranslationSettings(eq(false), eq(false), any(), any()))
        .thenReturn(List.of(at));

    List<AutoTranslationRow> out = allowAdmin().getAutoTranslations(null);
    assertEquals(1, out.size());
    assertEquals("fr-fr", out.get(0).getLocale());
    assertEquals("percPage", out.get(0).getContentTypeName());
    assertEquals("Default Workflow", out.get(0).getWorkflowName());
    assertEquals("Default", out.get(0).getCommunityName());
    verify(designWs).loadTranslationSettings(false, false, "test-session", "Admin");
  }

  @Test
  void getAutoTranslations_forbiddenWhenNotAdmin() throws Exception {
    AutoTranslationsAdaptor denied = adaptor(() -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.getAutoTranslations(null));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).loadTranslationSettings(anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void saveAutoTranslations_replacesSetWithLockReleasedOnSave() throws Exception {
    when(designWs.loadTranslationSettings(eq(true), eq(true), any(), any()))
        .thenReturn(List.of());
    PSAutoTranslation saved = sampleEntity();
    when(designWs.loadTranslationSettings(eq(false), eq(false), any(), any()))
        .thenReturn(List.of(saved));

    AutoTranslationRow body = sampleRow();
    List<AutoTranslationRow> out = allowAdmin().saveAutoTranslations(null, List.of(body));

    assertEquals(1, out.size());
    assertEquals("fr-fr", out.get(0).getLocale());
    verify(designWs).loadTranslationSettings(true, true, "test-session", "Admin");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSAutoTranslation>> captor = ArgumentCaptor.forClass(List.class);
    verify(designWs)
        .saveTranslationSettings(captor.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals(1, captor.getValue().size());
    assertEquals("fr-fr", captor.getValue().get(0).getLocale());
    assertEquals(typeGuid.getUUID(), captor.getValue().get(0).getContentTypeId());
  }

  @Test
  void saveAutoTranslations_emptyListClears() throws Exception {
    when(designWs.loadTranslationSettings(eq(true), eq(true), any(), any()))
        .thenReturn(List.of(sampleEntity()));
    when(designWs.loadTranslationSettings(eq(false), eq(false), any(), any()))
        .thenReturn(List.of());

    List<AutoTranslationRow> out = allowAdmin().saveAutoTranslations(null, List.of());
    assertTrue(out.isEmpty());
    verify(designWs)
        .saveTranslationSettings(eq(List.of()), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void saveAutoTranslations_unknownLocaleIs400() throws Exception {
    AutoTranslationRow body = sampleRow();
    body.setLocale("xx-xx");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> allowAdmin().saveAutoTranslations(null, List.of(body)));
    assertTrue(ex.getMessage().contains("unknown locale"));
    verify(designWs, never()).loadTranslationSettings(eq(true), anyBoolean(), any(), any());
    verify(designWs, never()).saveTranslationSettings(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void saveAutoTranslations_unknownContentTypeIs400() throws Exception {
    AutoTranslationRow body = sampleRow();
    body.setContentTypeName("missingType");
    body.setContentTypeId(null);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> allowAdmin().saveAutoTranslations(null, List.of(body)));
    assertTrue(ex.getMessage().contains("unknown content type"));
    verify(designWs, never()).saveTranslationSettings(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void saveAutoTranslations_duplicateRowIs400() throws Exception {
    AutoTranslationRow a = sampleRow();
    AutoTranslationRow b = sampleRow();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> allowAdmin().saveAutoTranslations(null, List.of(a, b)));
    assertTrue(ex.getMessage().contains("duplicate"));
    verify(designWs, never()).saveTranslationSettings(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void saveAutoTranslations_lockConflictOnLoadIs409() throws Exception {
    when(designWs.loadTranslationSettings(eq(true), eq(true), any(), any()))
        .thenThrow(new PSLockErrorException(1, "locked", "stack", "other", 1000L));
    AutoTranslationDesignLockException ex =
        assertThrows(
            AutoTranslationDesignLockException.class,
            () -> allowAdmin().saveAutoTranslations(null, List.of(sampleRow())));
    assertTrue(ex.getMessage().contains("locked by other"));
    verify(designWs, never()).saveTranslationSettings(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void saveAutoTranslations_lockConflictOnSaveIs409() throws Exception {
    when(designWs.loadTranslationSettings(eq(true), eq(true), any(), any()))
        .thenReturn(List.of());
    doThrow(new PSLockErrorException(1, "not locked", "stack"))
        .when(designWs)
        .saveTranslationSettings(anyList(), eq(true), any(), any());
    AutoTranslationDesignLockException ex =
        assertThrows(
            AutoTranslationDesignLockException.class,
            () -> allowAdmin().saveAutoTranslations(null, List.of(sampleRow())));
    assertNotNull(ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("lock"));
  }

  @Test
  void saveAutoTranslations_forbiddenWhenNotAdmin() throws Exception {
    AutoTranslationsAdaptor denied = adaptor(() -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.saveAutoTranslations(null, List.of(sampleRow())));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).saveTranslationSettings(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void saveAutoTranslations_requiresSessionAndUser() {
    PSRequestInfoBase.resetRequestInfo();
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> allowAdmin().saveAutoTranslations(null, List.of(sampleRow())));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void saveAutoTranslations_nullBodyIs400() {
    assertThrows(
        IllegalArgumentException.class, () -> allowAdmin().saveAutoTranslations(null, null));
  }

  @Test
  void copyVersions_copiesMatchingKeys() {
    PSAutoTranslation current = sampleEntity();
    current.setVersion(3);
    PSAutoTranslation incoming = sampleEntity();
    AutoTranslationsAdaptor.copyVersions(List.of(incoming), List.of(current));
    assertEquals(Integer.valueOf(3), incoming.getVersion());
  }

  @Test
  void copyVersions_matchesUuidVersusTypedLong() {
    PSAutoTranslation current = sampleEntity();
    current.setContentTypeId(typeGuid.getUUID());
    current.setVersion(7);
    PSAutoTranslation incoming = sampleEntity();
    incoming.setContentTypeId(typeGuid.longValue());
    AutoTranslationsAdaptor.copyVersions(List.of(incoming), List.of(current));
    assertEquals(Integer.valueOf(7), incoming.getVersion());
  }

  @Test
  void resolveCatalog_matchesUuidOrLongValueOrName() {
    IPSCatalogSummary found =
        AutoTranslationsAdaptor.resolveCatalog(
            List.of(typePage), (long) typeGuid.getUUID(), null, "content type");
    assertEquals("percPage", found.getName());
    found =
        AutoTranslationsAdaptor.resolveCatalog(
            List.of(typePage), typeGuid.longValue(), null, "content type");
    assertEquals("percPage", found.getName());
    found =
        AutoTranslationsAdaptor.resolveCatalog(List.of(typePage), null, "PERCPAGE", "content type");
    assertEquals("percPage", found.getName());
  }

  @Test
  void mapLockConflict_includesLocker() {
    PSLockErrorException err = new PSLockErrorException(1, "locked", "stack", "other", 1000L);
    AutoTranslationDesignLockException mapped = AutoTranslationsAdaptor.mapLockConflict(err);
    assertTrue(mapped.getMessage().contains("locked by other"));
  }

  private AutoTranslationsAdaptor allowAdmin() {
    return adaptor(() -> true);
  }

  private AutoTranslationsAdaptor adaptor(java.util.function.BooleanSupplier admin) {
    return new AutoTranslationsAdaptor(
        designWs,
        () -> List.of(localeFr),
        () -> List.of(typePage),
        () -> List.of(workflowDefault),
        () -> List.of(communityDefault),
        admin);
  }

  private AutoTranslationRow sampleRow() {
    AutoTranslationRow row = new AutoTranslationRow();
    row.setLocale("fr-fr");
    row.setContentTypeName("percPage");
    row.setWorkflowName("Default Workflow");
    row.setCommunityName("Default");
    return row;
  }

  private PSAutoTranslation sampleEntity() {
    PSAutoTranslation at = new PSAutoTranslation();
    at.setLocale("fr-fr");
    at.setContentTypeId(typeGuid.longValue());
    at.setContentTypeName("percPage");
    at.setWorkflowId(workflowGuid.longValue());
    at.setWorkflowName("Default Workflow");
    at.setCommunityId(communityGuid.longValue());
    at.setCommunityName("Default");
    return at;
  }

  private static IPSCatalogSummary summary(String name, IPSGuid guid) {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getName()).thenReturn(name);
    when(sum.getGUID()).thenReturn(guid);
    return sum;
  }
}

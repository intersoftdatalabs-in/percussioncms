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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.i18n.PSLocale;
import com.percussion.i18n.PSLocaleFormat;
import com.percussion.rest.locales.LocaleDesignLockException;
import com.percussion.rest.locales.LocaleDetail;
import com.percussion.rest.locales.LocaleFormatSummary;
import com.percussion.rest.locales.LocaleNotFoundException;
import com.percussion.rest.locales.LocaleSummary;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag("UnitTest")
class LocalesAdaptorDesignWsTest {

  @BeforeEach
  void setRequestInfo() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
  }

  @AfterEach
  void clearRequestInfo() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void listLocales_usesFindAndLoadOnDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 1L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);

    PSLocale loc = new PSLocale();
    loc.setLanguageString("en-us");
    loc.setDisplayName("English");
    loc.setLocaleId(1);

    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    LocalesAdaptor adaptor = new LocalesAdaptor(designWs, lang -> Optional.empty(), Set::of);
    List<LocaleSummary> list = adaptor.listLocales(null);

    assertEquals(1, list.size());
    assertEquals("en-us", list.get(0).getLanguageString());
    verify(designWs).findLocales(null, null);
    verify(designWs).loadLocales(anyList(), eq(false), eq(false), any(), any());
  }

  @Test
  void listLocales_emptyFind_skipsLoad() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of());

    LocalesAdaptor adaptor = new LocalesAdaptor(designWs, lang -> Optional.empty(), Set::of);
    assertTrue(adaptor.listLocales(null).isEmpty());
    verify(designWs).findLocales(null, null);
  }

  @Test
  void createLocale_usesCreateAndSaveOnDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    PSLocale created = newLocale(7, "fr-ca", "French (Canada)");
    when(designWs.findLocales(eq("fr-ca"), isNull())).thenReturn(List.of());
    when(designWs.createLocales(
            eq(List.of("fr-ca")), eq(List.of("French (Canada)")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(created));
    when(designWs.loadLocales(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(created));

    LocaleDetail body = new LocaleDetail();
    body.setLanguageString("fr-ca");
    body.setLabel("French (Canada)");
    body.setDescription("Canadian French");
    body.setStatus("active");
    body.setBaseLocale(false);

    LocalesAdaptor adaptor = allowAdmin(designWs);
    LocaleDetail out = adaptor.createLocale(null, body);

    assertNotNull(out);
    assertEquals("fr-ca", out.getLanguageString());
    verify(designWs)
        .createLocales(
            eq(List.of("fr-ca")), eq(List.of("French (Canada)")), eq("test-session"), eq("Admin"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSLocale>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveLocales(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    assertEquals("Canadian French", saved.getValue().get(0).getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, saved.getValue().get(0).getStatus());
  }

  @Test
  void createLocale_duplicateLanguage_is409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSCatalogSummary existing = mock(IPSCatalogSummary.class);
    when(existing.getName()).thenReturn("en-us");
    when(designWs.findLocales(eq("en-us"), isNull())).thenReturn(List.of(existing));

    LocaleDetail body = new LocaleDetail();
    body.setLanguageString("en-us");
    body.setLabel("English");
    LocalesAdaptor adaptor = allowAdmin(designWs);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createLocale(null, body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).createLocales(anyList(), anyList(), any(), any());
  }

  @Test
  void createLocale_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    LocalesAdaptor denied =
        new LocalesAdaptor(designWs, lang -> Optional.empty(), Set::of, () -> false);
    LocaleDetail body = new LocaleDetail();
    body.setLanguageString("fr-ca");
    body.setLabel("French");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.createLocale(null, body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).createLocales(anyList(), anyList(), any(), any());
  }

  @Test
  void createLocale_blankLabel_throwsBeforeDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    LocalesAdaptor adaptor = allowAdmin(designWs);
    LocaleDetail body = new LocaleDetail();
    body.setLanguageString("fr-ca");
    body.setLabel("  ");
    assertThrows(IllegalArgumentException.class, () -> adaptor.createLocale(null, body));
    verify(designWs, never()).createLocales(anyList(), anyList(), any(), any());
  }

  @Test
  void createLocale_invalidLanguage_throwsBeforeDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    LocalesAdaptor adaptor = allowAdmin(designWs);
    LocaleDetail body = new LocaleDetail();
    body.setLanguageString("not a locale!");
    body.setLabel("X");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createLocale(null, body));
    assertTrue(ex.getMessage().contains("invalid languageString"));
    verify(designWs, never()).createLocales(anyList(), anyList(), any(), any());
  }

  @Test
  void updateLocale_loadsWithLockAndSaves() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");

    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    LocaleDetail body = new LocaleDetail();
    body.setLabel("Updated");
    body.setDescription("d");
    body.setStatus("inactive");

    LocalesAdaptor adaptor = allowAdmin(designWs);
    LocaleDetail out = adaptor.updateLocale(null, "en-us", body);

    assertNotNull(out);
    verify(designWs).loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any());
    verify(designWs).saveLocales(anyList(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals("Updated", loc.getDisplayName());
    assertEquals(PSLocale.STATUS_INACTIVE, loc.getStatus());
  }

  @Test
  void updateLocale_languageStringChange_throwsBeforeLock() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    LocaleDetail body = new LocaleDetail();
    body.setLanguageString("fr-ca");
    body.setLabel("Nope");
    LocalesAdaptor adaptor = allowAdmin(designWs);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.updateLocale(null, "en-us", body));
    assertTrue(ex.getMessage().contains("cannot be changed"));
    verify(designWs, never()).loadLocales(anyList(), eq(true), eq(false), any(), any());
    verify(designWs, never()).saveLocales(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void updateLocale_notFound_returnsNull() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of());
    LocalesAdaptor adaptor = allowAdmin(designWs);
    LocaleDetail body = new LocaleDetail();
    body.setLabel("X");
    assertNull(adaptor.updateLocale(null, "missing", body));
    verify(designWs, never()).saveLocales(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void updateLocale_lockConflict_is409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 11L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(11, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSErrorResultsException err = new PSErrorResultsException();
    err.addError(guid, new PSLockErrorException(1, "locked", "stack", "other", 1000L));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any())).thenThrow(err);

    LocalesAdaptor adaptor = allowAdmin(designWs);
    LocaleDetail body = new LocaleDetail();
    body.setLabel("X");
    LocaleDesignLockException ex =
        assertThrows(
            LocaleDesignLockException.class, () -> adaptor.updateLocale(null, "en-us", body));
    assertTrue(ex.getMessage().contains("locked by other"));
    verify(designWs, never()).saveLocales(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void updateLocale_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    LocalesAdaptor denied =
        new LocalesAdaptor(designWs, lang -> Optional.empty(), Set::of, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.updateLocale(null, "en-us", new LocaleDetail()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void deleteLocale_deletesOnDesignWs() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 5L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(5, "fr-ca", "French");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    LocalesAdaptor adaptor = allowAdmin(designWs);
    adaptor.deleteLocale(null, "fr-ca");

    verify(designWs).deleteLocales(eq(List.of(guid)), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void deleteLocale_missing_throwsNotFound() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of());
    LocalesAdaptor adaptor = allowAdmin(designWs);
    assertThrows(LocaleNotFoundException.class, () -> adaptor.deleteLocale(null, "xx"));
    verify(designWs, never()).deleteLocales(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void deleteLocale_dependencyConflict_is409() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 5L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(5, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSErrorsException deps = new PSErrorsException();
    deps.addError(guid, new PSErrorException("Object has dependencies"));
    doThrow(deps)
        .when(designWs)
        .deleteLocales(eq(List.of(guid)), eq(false), any(), any());

    LocalesAdaptor adaptor = allowAdmin(designWs);
    LocaleDesignLockException ex =
        assertThrows(LocaleDesignLockException.class, () -> adaptor.deleteLocale(null, "en-us"));
    assertTrue(ex.getMessage().toLowerCase().contains("depend"));
  }

  @Test
  void deleteLocale_forbiddenWhenNotAdmin() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    LocalesAdaptor denied =
        new LocalesAdaptor(designWs, lang -> Optional.empty(), Set::of, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.deleteLocale(null, "en-us"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).deleteLocales(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void writeOperations_requireSessionAndUser() {
    PSRequestInfoBase.resetRequestInfo();
    PSRequestInfoBase.initRequestInfo(new HashMap<>());

    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    LocalesAdaptor adaptor = allowAdmin(designWs);
    LocaleDetail body = new LocaleDetail();
    body.setLanguageString("fr-ca");
    body.setLabel("French");

    WebApplicationException createEx =
        assertThrows(WebApplicationException.class, () -> adaptor.createLocale(null, body));
    assertEquals(403, createEx.getResponse().getStatus());

    WebApplicationException updateEx =
        assertThrows(
            WebApplicationException.class, () -> adaptor.updateLocale(null, "en-us", body));
    assertEquals(403, updateEx.getResponse().getStatus());

    WebApplicationException deleteEx =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteLocale(null, "en-us"));
    assertEquals(403, deleteEx.getResponse().getStatus());
  }

  @Test
  void updateLocale_savesFormatProfileWhenRequested() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor = allowAdmin(designWs, saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    body.setLabel("English");
    body.setHasFormatProfile(true);
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDatePattern("yyyy-MM-dd");
    fmt.setTextDir("ltr");
    fmt.setCurrencyCode("USD");
    body.setFormat(fmt);

    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertEquals("en-us", saved.get(0).getLanguageString());
    assertEquals("yyyy-MM-dd", saved.get(0).getDatePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_clearsFormatProfileWhenHasFormatProfileFalse() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor = allowAdmin(designWs, saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    body.setLabel("English");
    body.setHasFormatProfile(false);

    adaptor.updateLocale(null, "en-us", body);

    assertTrue(saved.isEmpty());
    assertEquals(List.of("en-us"), deleted);
  }

  @Test
  void updateLocale_omittedFormatDoesNotTouchProfile() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor = allowAdmin(designWs, saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    body.setLabel("Updated");
    adaptor.updateLocale(null, "en-us", body);

    assertTrue(saved.isEmpty());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_labelOnlyLeavesDescriptionStatusAndFormat() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor = allowAdmin(designWs, saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    body.setLabel("US English");
    LocaleDetail out = adaptor.updateLocale(null, "en-us", body);

    assertNotNull(out);
    assertEquals("US English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertTrue(saved.isEmpty());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_statusOnlyLeavesLabelDescriptionAndFormat() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    body.setStatus("inactive");
    LocaleDetail out = adaptor.updateLocale(null, "en-us", body);

    assertNotNull(out);
    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_INACTIVE, loc.getStatus());
    assertTrue(saved.isEmpty());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_datePatternOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDatePattern("yyyy-MM-dd");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals("yyyy-MM-dd", saved.get(0).getDatePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankDatePatternClearsDatePatternOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDatePattern("");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getDatePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void isDatePatternOnlyWrite_requiresOmittedHasFormatProfileAndOtherFields() {
    LocaleDetail omitted = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDatePattern("yyyy-MM-dd");
    omitted.setFormat(fmt);
    assertTrue(LocalesAdaptor.isDatePatternOnlyWrite(omitted));

    LocaleDetail withFlag = new LocaleDetail();
    withFlag.setHasFormatProfile(true);
    withFlag.setFormat(fmt);
    assertFalse(LocalesAdaptor.isDatePatternOnlyWrite(withFlag));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setDatePattern("yyyy-MM-dd");
    both.setTimePattern("HH:mm");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isDatePatternOnlyWrite(extra));
  }

  @Test
  void updateLocale_timePatternOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setTimePattern("hh:mm a");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals("hh:mm a", saved.get(0).getTimePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankTimePatternClearsTimePatternOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTimePattern("HH:mm:ss");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setTimePattern("");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getTimePattern());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void isTimePatternOnlyWrite_requiresOmittedHasFormatProfileAndOtherFields() {
    LocaleDetail omitted = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setTimePattern("hh:mm a");
    omitted.setFormat(fmt);
    assertTrue(LocalesAdaptor.isTimePatternOnlyWrite(omitted));

    LocaleDetail withFlag = new LocaleDetail();
    withFlag.setHasFormatProfile(true);
    withFlag.setFormat(fmt);
    assertFalse(LocalesAdaptor.isTimePatternOnlyWrite(withFlag));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setTimePattern("hh:mm a");
    both.setDatePattern("yyyy-MM-dd");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isTimePatternOnlyWrite(extra));
  }

  @Test
  void updateLocale_dateTimePatternOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDateTimePattern("yyyy-MM-dd HH:mm:ss");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals("yyyy-MM-dd HH:mm:ss", saved.get(0).getDateTimePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankDateTimePatternClearsDateTimePatternOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDateTimePattern("");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getDateTimePattern());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void isDateTimePatternOnlyWrite_requiresOmittedHasFormatProfileAndOtherFields() {
    LocaleDetail omitted = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDateTimePattern("yyyy-MM-dd HH:mm:ss");
    omitted.setFormat(fmt);
    assertTrue(LocalesAdaptor.isDateTimePatternOnlyWrite(omitted));

    LocaleDetail withFlag = new LocaleDetail();
    withFlag.setHasFormatProfile(true);
    withFlag.setFormat(fmt);
    assertFalse(LocalesAdaptor.isDateTimePatternOnlyWrite(withFlag));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setDateTimePattern("yyyy-MM-dd HH:mm:ss");
    both.setDatePattern("yyyy-MM-dd");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isDateTimePatternOnlyWrite(extra));
  }

  @Test
  void updateLocale_decimalSepOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDecimalSep(",");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals(",", saved.get(0).getDecimalSep());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("MM/dd/yyyy HH:mm:ss", saved.get(0).getDateTimePattern());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankDecimalSepClearsDecimalSepOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDecimalSep("");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void isDecimalSepOnlyWrite_requiresOmittedHasFormatProfileAndOtherFields() {
    LocaleDetail omitted = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDecimalSep(",");
    omitted.setFormat(fmt);
    assertTrue(LocalesAdaptor.isDecimalSepOnlyWrite(omitted));

    LocaleDetail withFlag = new LocaleDetail();
    withFlag.setHasFormatProfile(true);
    withFlag.setFormat(fmt);
    assertFalse(LocalesAdaptor.isDecimalSepOnlyWrite(withFlag));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setDecimalSep(",");
    both.setGroupingSep(" ");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isDecimalSepOnlyWrite(extra));
  }

  @Test
  void updateLocale_groupingSepOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setGroupingSep(".");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals(".", saved.get(0).getGroupingSep());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("MM/dd/yyyy HH:mm:ss", saved.get(0).getDateTimePattern());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankGroupingSepClearsGroupingSepOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setGroupingSep("");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getGroupingSep());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void isGroupingSepOnlyWrite_requiresOmittedHasFormatProfileAndOtherFields() {
    LocaleDetail omitted = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setGroupingSep(".");
    omitted.setFormat(fmt);
    assertTrue(LocalesAdaptor.isGroupingSepOnlyWrite(omitted));

    LocaleDetail withFlag = new LocaleDetail();
    withFlag.setHasFormatProfile(true);
    withFlag.setFormat(fmt);
    assertFalse(LocalesAdaptor.isGroupingSepOnlyWrite(withFlag));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setGroupingSep(".");
    both.setDecimalSep(",");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isGroupingSepOnlyWrite(extra));
  }

  @Test
  void updateLocale_currencyCodeOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setCurrencyCode("EUR");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals("EUR", saved.get(0).getCurrencyCode());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("MM/dd/yyyy HH:mm:ss", saved.get(0).getDateTimePattern());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankCurrencyCodeClearsCurrencyCodeOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setCurrencyCode("");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getCurrencyCode());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void isCurrencyCodeOnlyWrite_requiresOmittedHasFormatProfileAndOtherFields() {
    LocaleDetail omitted = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setCurrencyCode("EUR");
    omitted.setFormat(fmt);
    assertTrue(LocalesAdaptor.isCurrencyCodeOnlyWrite(omitted));

    LocaleDetail withFlag = new LocaleDetail();
    withFlag.setHasFormatProfile(true);
    withFlag.setFormat(fmt);
    assertFalse(LocalesAdaptor.isCurrencyCodeOnlyWrite(withFlag));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setCurrencyCode("EUR");
    both.setGroupingSep(",");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isCurrencyCodeOnlyWrite(extra));
  }

  @Test
  void updateLocale_currencyPatternOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setCurrencyCode("USD");
    existing.setCurrencyPattern("¤#,##0.00");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setCurrencyPattern("#,##0.00¤");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals("#,##0.00¤", saved.get(0).getCurrencyPattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("MM/dd/yyyy HH:mm:ss", saved.get(0).getDateTimePattern());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankCurrencyPatternClearsCurrencyPatternOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    existing.setCurrencyPattern("¤#,##0.00");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setCurrencyPattern("");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getCurrencyPattern());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void isCurrencyPatternOnlyWrite_requiresOmittedHasFormatProfileAndOtherFields() {
    LocaleDetail omitted = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setCurrencyPattern("#,##0.00¤");
    omitted.setFormat(fmt);
    assertTrue(LocalesAdaptor.isCurrencyPatternOnlyWrite(omitted));

    LocaleDetail withFlag = new LocaleDetail();
    withFlag.setHasFormatProfile(true);
    withFlag.setFormat(fmt);
    assertFalse(LocalesAdaptor.isCurrencyPatternOnlyWrite(withFlag));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setCurrencyPattern("#,##0.00¤");
    both.setCurrencyCode("EUR");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isCurrencyPatternOnlyWrite(extra));
  }

  @Test
  void updateLocale_textDirOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setTextDir("rtl");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals("rtl", saved.get(0).getTextDir());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("MM/dd/yyyy HH:mm:ss", saved.get(0).getDateTimePattern());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankTextDirClearsTextDirOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setTextDir("");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getTextDir());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void isTextDirOnlyWrite_requiresOmittedHasFormatProfileAndOtherFields() {
    LocaleDetail omitted = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setTextDir("rtl");
    omitted.setFormat(fmt);
    assertTrue(LocalesAdaptor.isTextDirOnlyWrite(omitted));

    LocaleDetail withFlag = new LocaleDetail();
    withFlag.setHasFormatProfile(true);
    withFlag.setFormat(fmt);
    assertFalse(LocalesAdaptor.isTextDirOnlyWrite(withFlag));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setTextDir("rtl");
    both.setDecimalSep(".");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isTextDirOnlyWrite(extra));
  }

  @Test
  void updateLocale_firstDayOfWeekOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setCurrencyCode("USD");
    existing.setCurrencyPattern("¤#,##0.00");
    existing.setFirstDayOfWeek(1);
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setFirstDayOfWeek(2);
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals(Integer.valueOf(2), saved.get(0).getFirstDayOfWeek());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("MM/dd/yyyy HH:mm:ss", saved.get(0).getDateTimePattern());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertEquals("¤#,##0.00", saved.get(0).getCurrencyPattern());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankFirstDayOfWeekClearsFirstDayOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    existing.setCurrencyPattern("¤#,##0.00");
    existing.setFirstDayOfWeek(1);
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setFirstDayOfWeek(null);
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getFirstDayOfWeek());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertEquals("¤#,##0.00", saved.get(0).getCurrencyPattern());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_zeroFirstDayOfWeekClearsFirstDayOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setFirstDayOfWeek(1);
    List<PSLocaleFormat> saved = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, lang -> {});

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setFirstDayOfWeek(0);
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getFirstDayOfWeek());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
  }

  @Test
  void updateLocale_outOfRangeFirstDayOfWeekDoesNotWrite() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setFirstDayOfWeek(1);
    existing.setDatePattern("MM/dd/yyyy");
    List<PSLocaleFormat> saved = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, lang -> {});

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setFirstDayOfWeek(8);
    body.setFormat(fmt);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.updateLocale(null, "en-us", body));
    assertTrue(ex.getMessage().contains("firstDayOfWeek"));
    assertTrue(saved.isEmpty());
    assertEquals(Integer.valueOf(1), existing.getFirstDayOfWeek());
    assertEquals("MM/dd/yyyy", existing.getDatePattern());
  }

  @Test
  void updateLocale_defaultTzOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setCurrencyCode("USD");
    existing.setCurrencyPattern("¤#,##0.00");
    existing.setFirstDayOfWeek(1);
    existing.setMeasurementSystem("us");
    existing.setDefaultTz("America/New_York");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDefaultTz("Europe/London");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals("Europe/London", saved.get(0).getDefaultTz());
    assertEquals(Integer.valueOf(1), saved.get(0).getFirstDayOfWeek());
    assertEquals("us", saved.get(0).getMeasurementSystem());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("MM/dd/yyyy HH:mm:ss", saved.get(0).getDateTimePattern());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertEquals("¤#,##0.00", saved.get(0).getCurrencyPattern());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankDefaultTzClearsDefaultTzOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    existing.setCurrencyPattern("¤#,##0.00");
    existing.setFirstDayOfWeek(1);
    existing.setMeasurementSystem("us");
    existing.setDefaultTz("America/New_York");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDefaultTz("");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getDefaultTz());
    assertEquals(Integer.valueOf(1), saved.get(0).getFirstDayOfWeek());
    assertEquals("us", saved.get(0).getMeasurementSystem());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertEquals("¤#,##0.00", saved.get(0).getCurrencyPattern());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void isDefaultTzOnlyWrite_requiresPresentAndOmittedOtherFields() {
    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary only = new LocaleFormatSummary();
    only.setDefaultTz("Europe/London");
    body.setFormat(only);
    assertTrue(LocalesAdaptor.isDefaultTzOnlyWrite(body));

    LocaleDetail blank = new LocaleDetail();
    LocaleFormatSummary blankFmt = new LocaleFormatSummary();
    blankFmt.setDefaultTz("");
    blank.setFormat(blankFmt);
    assertTrue(LocalesAdaptor.isDefaultTzOnlyWrite(blank));

    LocaleDetail absent = new LocaleDetail();
    absent.setFormat(new LocaleFormatSummary());
    assertFalse(LocalesAdaptor.isDefaultTzOnlyWrite(absent));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setDefaultTz("Europe/London");
    both.setMeasurementSystem("metric");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isDefaultTzOnlyWrite(extra));

    LocaleDetail withProfile = new LocaleDetail();
    withProfile.setFormat(only);
    withProfile.setHasFormatProfile(Boolean.TRUE);
    assertFalse(LocalesAdaptor.isDefaultTzOnlyWrite(withProfile));
  }

  @Test
  void updateLocale_numberingSystemOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setCurrencyCode("USD");
    existing.setCurrencyPattern("¤#,##0.00");
    existing.setFirstDayOfWeek(1);
    existing.setMeasurementSystem("us");
    existing.setDefaultTz("America/New_York");
    existing.setNumberingSystem("latn");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setNumberingSystem("arab");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals("arab", saved.get(0).getNumberingSystem());
    assertEquals("America/New_York", saved.get(0).getDefaultTz());
    assertEquals(Integer.valueOf(1), saved.get(0).getFirstDayOfWeek());
    assertEquals("us", saved.get(0).getMeasurementSystem());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("MM/dd/yyyy HH:mm:ss", saved.get(0).getDateTimePattern());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertEquals("¤#,##0.00", saved.get(0).getCurrencyPattern());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankNumberingSystemClearsNumberingSystemOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    existing.setCurrencyPattern("¤#,##0.00");
    existing.setFirstDayOfWeek(1);
    existing.setMeasurementSystem("us");
    existing.setDefaultTz("America/New_York");
    existing.setNumberingSystem("latn");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setNumberingSystem("");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getNumberingSystem());
    assertEquals("America/New_York", saved.get(0).getDefaultTz());
    assertEquals(Integer.valueOf(1), saved.get(0).getFirstDayOfWeek());
    assertEquals("us", saved.get(0).getMeasurementSystem());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertEquals("¤#,##0.00", saved.get(0).getCurrencyPattern());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void isNumberingSystemOnlyWrite_requiresPresentAndOmittedOtherFields() {
    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary only = new LocaleFormatSummary();
    only.setNumberingSystem("arab");
    body.setFormat(only);
    assertTrue(LocalesAdaptor.isNumberingSystemOnlyWrite(body));

    LocaleDetail blank = new LocaleDetail();
    LocaleFormatSummary blankFmt = new LocaleFormatSummary();
    blankFmt.setNumberingSystem("");
    blank.setFormat(blankFmt);
    assertTrue(LocalesAdaptor.isNumberingSystemOnlyWrite(blank));

    LocaleDetail absent = new LocaleDetail();
    absent.setFormat(new LocaleFormatSummary());
    assertFalse(LocalesAdaptor.isNumberingSystemOnlyWrite(absent));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setNumberingSystem("arab");
    both.setMeasurementSystem("metric");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isNumberingSystemOnlyWrite(extra));

    LocaleDetail withTz = new LocaleDetail();
    LocaleFormatSummary withTzFmt = new LocaleFormatSummary();
    withTzFmt.setNumberingSystem("arab");
    withTzFmt.setDefaultTz("Europe/London");
    withTz.setFormat(withTzFmt);
    assertFalse(LocalesAdaptor.isNumberingSystemOnlyWrite(withTz));

    LocaleDetail withProfile = new LocaleDetail();
    withProfile.setFormat(only);
    withProfile.setHasFormatProfile(Boolean.TRUE);
    assertFalse(LocalesAdaptor.isNumberingSystemOnlyWrite(withProfile));
  }

  @Test
  void updateLocale_calendarOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setCurrencyCode("USD");
    existing.setCurrencyPattern("¤#,##0.00");
    existing.setFirstDayOfWeek(1);
    existing.setMeasurementSystem("us");
    existing.setDefaultTz("America/New_York");
    existing.setNumberingSystem("latn");
    existing.setCalendar("gregory");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setCalendar("islamic");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals("islamic", saved.get(0).getCalendar());
    assertEquals("latn", saved.get(0).getNumberingSystem());
    assertEquals("America/New_York", saved.get(0).getDefaultTz());
    assertEquals(Integer.valueOf(1), saved.get(0).getFirstDayOfWeek());
    assertEquals("us", saved.get(0).getMeasurementSystem());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("MM/dd/yyyy HH:mm:ss", saved.get(0).getDateTimePattern());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertEquals("¤#,##0.00", saved.get(0).getCurrencyPattern());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankCalendarClearsCalendarOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    existing.setCurrencyPattern("¤#,##0.00");
    existing.setFirstDayOfWeek(1);
    existing.setMeasurementSystem("us");
    existing.setDefaultTz("America/New_York");
    existing.setNumberingSystem("latn");
    existing.setCalendar("gregory");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setCalendar("");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getCalendar());
    assertEquals("latn", saved.get(0).getNumberingSystem());
    assertEquals("America/New_York", saved.get(0).getDefaultTz());
    assertEquals(Integer.valueOf(1), saved.get(0).getFirstDayOfWeek());
    assertEquals("us", saved.get(0).getMeasurementSystem());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertEquals("¤#,##0.00", saved.get(0).getCurrencyPattern());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void isCalendarOnlyWrite_requiresPresentAndOmittedOtherFields() {
    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary only = new LocaleFormatSummary();
    only.setCalendar("gregory");
    body.setFormat(only);
    assertTrue(LocalesAdaptor.isCalendarOnlyWrite(body));

    LocaleDetail blank = new LocaleDetail();
    LocaleFormatSummary blankFmt = new LocaleFormatSummary();
    blankFmt.setCalendar("");
    blank.setFormat(blankFmt);
    assertTrue(LocalesAdaptor.isCalendarOnlyWrite(blank));

    LocaleDetail absent = new LocaleDetail();
    absent.setFormat(new LocaleFormatSummary());
    assertFalse(LocalesAdaptor.isCalendarOnlyWrite(absent));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setCalendar("gregory");
    both.setNumberingSystem("latn");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isCalendarOnlyWrite(extra));

    LocaleDetail withTz = new LocaleDetail();
    LocaleFormatSummary withTzFmt = new LocaleFormatSummary();
    withTzFmt.setCalendar("gregory");
    withTzFmt.setDefaultTz("Europe/London");
    withTz.setFormat(withTzFmt);
    assertFalse(LocalesAdaptor.isCalendarOnlyWrite(withTz));

    LocaleDetail withProfile = new LocaleDetail();
    withProfile.setFormat(only);
    withProfile.setHasFormatProfile(Boolean.TRUE);
    assertFalse(LocalesAdaptor.isCalendarOnlyWrite(withProfile));
  }

  @Test
  void isFirstDayOfWeekOnlyWrite_requiresPresentAndOmittedOtherFields() {
    LocaleDetail omitted = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setFirstDayOfWeek(2);
    omitted.setFormat(fmt);
    assertTrue(LocalesAdaptor.isFirstDayOfWeekOnlyWrite(omitted));

    LocaleDetail cleared = new LocaleDetail();
    LocaleFormatSummary blank = new LocaleFormatSummary();
    blank.setFirstDayOfWeek(null);
    cleared.setFormat(blank);
    assertTrue(LocalesAdaptor.isFirstDayOfWeekOnlyWrite(cleared));

    LocaleDetail absent = new LocaleDetail();
    absent.setFormat(new LocaleFormatSummary());
    assertFalse(LocalesAdaptor.isFirstDayOfWeekOnlyWrite(absent));

    LocaleDetail withFlag = new LocaleDetail();
    withFlag.setHasFormatProfile(true);
    withFlag.setFormat(fmt);
    assertFalse(LocalesAdaptor.isFirstDayOfWeekOnlyWrite(withFlag));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setFirstDayOfWeek(2);
    both.setCurrencyCode("EUR");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isFirstDayOfWeekOnlyWrite(extra));
  }

  @Test
  void updateLocale_measurementSystemOnlyLeavesOtherFormatFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTextDir("ltr");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setCurrencyCode("USD");
    existing.setCurrencyPattern("¤#,##0.00");
    existing.setFirstDayOfWeek(1);
    existing.setMeasurementSystem("us");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setMeasurementSystem("METRIC");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());
    assertEquals(1, saved.size());
    assertEquals("metric", saved.get(0).getMeasurementSystem());
    assertEquals(Integer.valueOf(1), saved.get(0).getFirstDayOfWeek());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("MM/dd/yyyy HH:mm:ss", saved.get(0).getDateTimePattern());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertEquals("¤#,##0.00", saved.get(0).getCurrencyPattern());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_blankMeasurementSystemClearsMeasurementOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));
    when(designWs.loadLocales(eq(List.of(guid)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setTimePattern("HH:mm:ss");
    existing.setDateTimePattern("MM/dd/yyyy HH:mm:ss");
    existing.setDecimalSep(".");
    existing.setGroupingSep(",");
    existing.setTextDir("ltr");
    existing.setCurrencyCode("USD");
    existing.setCurrencyPattern("¤#,##0.00");
    existing.setFirstDayOfWeek(1);
    existing.setMeasurementSystem("us");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setMeasurementSystem("");
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getMeasurementSystem());
    assertEquals(Integer.valueOf(1), saved.get(0).getFirstDayOfWeek());
    assertEquals(".", saved.get(0).getDecimalSep());
    assertEquals(",", saved.get(0).getGroupingSep());
    assertEquals("USD", saved.get(0).getCurrencyCode());
    assertEquals("¤#,##0.00", saved.get(0).getCurrencyPattern());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertEquals("HH:mm:ss", saved.get(0).getTimePattern());
    assertEquals("ltr", saved.get(0).getTextDir());
    assertTrue(deleted.isEmpty());
  }

  @Test
  void updateLocale_invalidMeasurementSystemDoesNotWrite() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setMeasurementSystem("us");
    existing.setDatePattern("MM/dd/yyyy");
    List<PSLocaleFormat> saved = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, lang -> {});

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setMeasurementSystem("imperial");
    body.setFormat(fmt);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.updateLocale(null, "en-us", body));
    assertTrue(ex.getMessage().contains("measurementSystem"));
    assertTrue(saved.isEmpty());
    assertEquals("us", existing.getMeasurementSystem());
    assertEquals("MM/dd/yyyy", existing.getDatePattern());
  }

  @Test
  void isMeasurementSystemOnlyWrite_requiresOmittedHasFormatProfileAndOtherFields() {
    LocaleDetail omitted = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setMeasurementSystem("metric");
    omitted.setFormat(fmt);
    assertTrue(LocalesAdaptor.isMeasurementSystemOnlyWrite(omitted));

    LocaleDetail cleared = new LocaleDetail();
    LocaleFormatSummary blank = new LocaleFormatSummary();
    blank.setMeasurementSystem("");
    cleared.setFormat(blank);
    assertTrue(LocalesAdaptor.isMeasurementSystemOnlyWrite(cleared));

    LocaleDetail absent = new LocaleDetail();
    absent.setFormat(new LocaleFormatSummary());
    assertFalse(LocalesAdaptor.isMeasurementSystemOnlyWrite(absent));

    LocaleDetail withFlag = new LocaleDetail();
    withFlag.setHasFormatProfile(true);
    withFlag.setFormat(fmt);
    assertFalse(LocalesAdaptor.isMeasurementSystemOnlyWrite(withFlag));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setMeasurementSystem("metric");
    both.setCurrencyCode("EUR");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isMeasurementSystemOnlyWrite(extra));
  }

  @Test
  void applyWritableFields_omittedAndBlankLabelLeaveExisting() {
    PSLocale loc = newLocale(9, "en-us", "English");
    loc.setDescription("US English");
    loc.setStatus(PSLocale.STATUS_ACTIVE);
    LocaleDetail omitted = new LocaleDetail();
    LocalesAdaptor.applyWritableFields(loc, omitted, true);
    assertEquals("English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, loc.getStatus());

    LocaleDetail blank = new LocaleDetail();
    blank.setLabel("   ");
    LocalesAdaptor.applyWritableFields(loc, blank, true);
    assertEquals("English", loc.getDisplayName());

    LocaleDetail next = new LocaleDetail();
    next.setLabel("US English");
    LocalesAdaptor.applyWritableFields(loc, next, true);
    assertEquals("US English", loc.getDisplayName());
    assertEquals("US English", loc.getDescription());
  }

  @Test
  void updateLocale_invalidFormatPattern_throwsBeforeSave() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    PSLocale loc = newLocale(9, "en-us", "English");
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadLocales(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(loc));

    LocalesAdaptor adaptor = allowAdmin(designWs);
    LocaleDetail body = new LocaleDetail();
    body.setLabel("English");
    body.setHasFormatProfile(true);
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setDatePattern("'unclosed");
    body.setFormat(fmt);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.updateLocale(null, "en-us", body));
    assertTrue(ex.getMessage().contains("datePattern"));
    verify(designWs, never()).saveLocales(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void parseStatus_knownAndUnknown() {
    assertEquals(PSLocale.STATUS_ACTIVE, LocalesAdaptor.parseStatus("active"));
    assertEquals(PSLocale.STATUS_INACTIVE, LocalesAdaptor.parseStatus("INACTIVE"));
    assertThrows(IllegalArgumentException.class, () -> LocalesAdaptor.parseStatus("nope"));
  }

  @Test
  void isValidLanguageString_acceptsBcp47() {
    assertTrue(LocalesAdaptor.isValidLanguageString("en-us"));
    assertTrue(LocalesAdaptor.isValidLanguageString("fr"));
    assertTrue(LocalesAdaptor.isValidLanguageString("zh-hans"));
  }

  @Test
  void mapLockConflict_includesLocker() {
    PSLockErrorException err = new PSLockErrorException(1, "locked", "stack", "other", 1000L);
    LocaleDesignLockException mapped = LocalesAdaptor.mapLockConflict(err);
    assertTrue(mapped.getMessage().contains("locked by other"));
  }

  private static LocalesAdaptor allowAdmin(IPSContentDesignWs designWs) {
    return new LocalesAdaptor(designWs, lang -> Optional.empty(), Set::of, () -> true);
  }

  private static LocalesAdaptor allowAdmin(
      IPSContentDesignWs designWs,
      Consumer<PSLocaleFormat> saver,
      Consumer<String> deleter) {
    return allowAdmin(designWs, lang -> Optional.empty(), saver, deleter);
  }

  private static LocalesAdaptor allowAdmin(
      IPSContentDesignWs designWs,
      Function<String, Optional<PSLocaleFormat>> formatByLang,
      Consumer<PSLocaleFormat> saver,
      Consumer<String> deleter) {
    return new LocalesAdaptor(designWs, formatByLang, Set::of, () -> true, saver, deleter);
  }

  private static PSLocale newLocale(int id, String lang, String label) {
    PSLocale loc = new PSLocale();
    loc.setLocaleId(id);
    loc.setLanguageString(lang);
    loc.setDisplayName(label);
    loc.setStatus(PSLocale.STATUS_INACTIVE);
    return loc;
  }

  private static IPSCatalogSummary catalogSummary(IPSGuid guid) {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    return sum;
  }

  @Test
  void updateLocale_fallbackChainOnlyLeavesOtherFields() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guidEn = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSGuid guidFr = new PSGuid(PSTypeEnum.LOCALE, 10L);
    PSLocale locEn = newLocale(9, "en-us", "English");
    locEn.setDescription("US English");
    locEn.setStatus(PSLocale.STATUS_ACTIVE);
    PSLocale locFr = newLocale(10, "fr-fr", "French");
    IPSCatalogSummary sum0 = catalogSummary(guidEn);
    IPSCatalogSummary sum1 = catalogSummary(guidFr);
    when(designWs.findLocales(isNull(), isNull()))
        .thenReturn(List.of(sum0, sum1));
    when(designWs.loadLocales(eq(List.of(guidEn, guidFr)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(locEn, locFr));
    when(designWs.loadLocales(eq(List.of(guidEn)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(locEn));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setDatePattern("MM/dd/yyyy");
    existing.setCalendar("gregory");
    existing.setFallbackChain("es");
    List<PSLocaleFormat> saved = new ArrayList<>();
    List<String> deleted = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, deleted::add);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setFallbackChain(List.of("fr-fr", "fr-fr"));
    body.setFormat(fmt);
    LocaleDetail out = adaptor.updateLocale(null, "en-us", body);

    assertEquals("English", locEn.getDisplayName());
    assertEquals("US English", locEn.getDescription());
    assertEquals(PSLocale.STATUS_ACTIVE, locEn.getStatus());
    assertEquals(1, saved.size());
    assertEquals("fr-fr", saved.get(0).getFallbackChain());
    assertEquals("gregory", saved.get(0).getCalendar());
    assertEquals("MM/dd/yyyy", saved.get(0).getDatePattern());
    assertTrue(deleted.isEmpty());
    assertNotNull(out);
    assertEquals(List.of("fr-fr"), out.getFormat().getFallbackChain());
  }

  @Test
  void updateLocale_emptyFallbackChainClearsChainOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guidEn = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSGuid guidFr = new PSGuid(PSTypeEnum.LOCALE, 10L);
    PSLocale locEn = newLocale(9, "en-us", "English");
    PSLocale locFr = newLocale(10, "fr-fr", "French");
    IPSCatalogSummary sum0 = catalogSummary(guidEn);
    IPSCatalogSummary sum1 = catalogSummary(guidFr);
    when(designWs.findLocales(isNull(), isNull()))
        .thenReturn(List.of(sum0, sum1));
    when(designWs.loadLocales(eq(List.of(guidEn, guidFr)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(locEn, locFr));
    when(designWs.loadLocales(eq(List.of(guidEn)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(locEn));

    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setCalendar("gregory");
    existing.setFallbackChain("fr-fr,es");
    List<PSLocaleFormat> saved = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, lang -> {});

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setFallbackChain(List.of());
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getFallbackChain());
    assertEquals("gregory", saved.get(0).getCalendar());
  }

  @Test
  void updateLocale_blankOnlyFallbackChainClearsChainOnly() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guidEn = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSGuid guidFr = new PSGuid(PSTypeEnum.LOCALE, 10L);
    PSLocale locEn = newLocale(9, "en-us", "English");
    PSLocale locFr = newLocale(10, "fr-fr", "French");
    IPSCatalogSummary sum0 = catalogSummary(guidEn);
    IPSCatalogSummary sum1 = catalogSummary(guidFr);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum0, sum1));
    when(designWs.loadLocales(eq(List.of(guidEn, guidFr)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(locEn, locFr));
    when(designWs.loadLocales(eq(List.of(guidEn)), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(locEn));

    // Blank-only lists are the wire encoding of a clear: nested empty arrays do
    // not survive the CXF JSON provider, so the SPA sends [""] and the server
    // normalizes blanks before storing.
    PSLocaleFormat existing = new PSLocaleFormat("en-us");
    existing.setFallbackChain("fr-fr");
    List<PSLocaleFormat> saved = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.of(existing), saved::add, lang -> {});

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setFallbackChain(List.of(""));
    body.setFormat(fmt);
    adaptor.updateLocale(null, "en-us", body);

    assertEquals(1, saved.size());
    assertNull(saved.get(0).getFallbackChain());
  }

  @Test
  void updateLocale_unknownFallbackLocaleIs400WithoutWrite() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guidEn = new PSGuid(PSTypeEnum.LOCALE, 9L);
    PSLocale locEn = newLocale(9, "en-us", "English");
    IPSCatalogSummary sum0 = catalogSummary(guidEn);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum0));
    when(designWs.loadLocales(eq(List.of(guidEn)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(locEn));

    List<PSLocaleFormat> saved = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.empty(), saved::add, lang -> {});

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setFallbackChain(List.of("xx-zz"));
    body.setFormat(fmt);
    assertThrows(IllegalArgumentException.class, () -> adaptor.updateLocale(null, "en-us", body));
    assertTrue(saved.isEmpty());
    verify(designWs, never()).saveLocales(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void updateLocale_selfFallbackIs400WithoutWrite() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guidEn = new PSGuid(PSTypeEnum.LOCALE, 9L);
    IPSGuid guidFr = new PSGuid(PSTypeEnum.LOCALE, 10L);
    PSLocale locEn = newLocale(9, "en-us", "English");
    PSLocale locFr = newLocale(10, "fr-fr", "French");
    IPSCatalogSummary sum0 = catalogSummary(guidEn);
    IPSCatalogSummary sum1 = catalogSummary(guidFr);
    when(designWs.findLocales(isNull(), isNull()))
        .thenReturn(List.of(sum0, sum1));
    when(designWs.loadLocales(eq(List.of(guidEn, guidFr)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(locEn, locFr));

    List<PSLocaleFormat> saved = new ArrayList<>();
    LocalesAdaptor adaptor =
        allowAdmin(designWs, lang -> Optional.empty(), saved::add, lang -> {});

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setFallbackChain(List.of("EN_us", "fr-fr"));
    body.setFormat(fmt);
    assertThrows(IllegalArgumentException.class, () -> adaptor.updateLocale(null, "en-us", body));
    assertTrue(saved.isEmpty());
  }

  @Test
  void updateLocale_invalidFallbackPatternIs400() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guidEn = new PSGuid(PSTypeEnum.LOCALE, 9L);
    PSLocale locEn = newLocale(9, "en-us", "English");
    IPSCatalogSummary sum0 = catalogSummary(guidEn);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum0));
    when(designWs.loadLocales(eq(List.of(guidEn)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(locEn));
    LocalesAdaptor adaptor = allowAdmin(designWs);

    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setFallbackChain(List.of("en-us!!"));
    body.setFormat(fmt);
    assertThrows(IllegalArgumentException.class, () -> adaptor.updateLocale(null, "en-us", body));
    verify(designWs, never()).saveLocales(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void updateLocale_unknownIdWithChainBodyReturnsNull() throws Exception {
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    IPSGuid guidEn = new PSGuid(PSTypeEnum.LOCALE, 9L);
    PSLocale locEn = newLocale(9, "en-us", "English");
    IPSCatalogSummary sum0 = catalogSummary(guidEn);
    when(designWs.findLocales(isNull(), isNull())).thenReturn(List.of(sum0));
    when(designWs.loadLocales(eq(List.of(guidEn)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(locEn));

    LocalesAdaptor adaptor = allowAdmin(designWs);
    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setFallbackChain(List.of("en-us"));
    body.setFormat(fmt);
    assertNull(adaptor.updateLocale(null, "xx-zz", body));
  }

  @Test
  void isFallbackChainOnlyWrite_requiresPresentAndOmittedOtherFields() {
    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary only = new LocaleFormatSummary();
    only.setFallbackChain(List.of("en-us"));
    body.setFormat(only);
    assertTrue(LocalesAdaptor.isFallbackChainOnlyWrite(body));

    LocaleDetail blank = new LocaleDetail();
    LocaleFormatSummary blankFmt = new LocaleFormatSummary();
    blankFmt.setFallbackChain(List.of());
    blank.setFormat(blankFmt);
    assertTrue(LocalesAdaptor.isFallbackChainOnlyWrite(blank));

    LocaleDetail absent = new LocaleDetail();
    absent.setFormat(new LocaleFormatSummary());
    assertFalse(LocalesAdaptor.isFallbackChainOnlyWrite(absent));

    LocaleDetail extra = new LocaleDetail();
    LocaleFormatSummary both = new LocaleFormatSummary();
    both.setFallbackChain(List.of("en-us"));
    both.setCalendar("gregory");
    extra.setFormat(both);
    assertFalse(LocalesAdaptor.isFallbackChainOnlyWrite(extra));
    assertFalse(LocalesAdaptor.isCalendarOnlyWrite(extra));

    LocaleDetail withProfile = new LocaleDetail();
    withProfile.setFormat(only);
    withProfile.setHasFormatProfile(Boolean.TRUE);
    assertFalse(LocalesAdaptor.isFallbackChainOnlyWrite(withProfile));
  }

  @Test
  void calendarOnlyWrite_rejectsChainBody() {
    LocaleDetail body = new LocaleDetail();
    LocaleFormatSummary fmt = new LocaleFormatSummary();
    fmt.setCalendar("gregory");
    fmt.setFallbackChain(List.of("en-us"));
    body.setFormat(fmt);
    assertFalse(LocalesAdaptor.isCalendarOnlyWrite(body));
  }

  @Test
  void fallbackChain_normalizeDecodeRoundTrip() {
    List<String> withNull = new ArrayList<>(List.of("EN_us", "es", "en-us", " "));
    withNull.add(null);
    assertEquals(List.of("en-us", "es"), LocalesAdaptor.normalizeFallbackChain(withNull));
    assertEquals(List.of("en-us", "es"), LocalesAdaptor.decodeFallbackChain("en-us,es"));
    assertNull(LocalesAdaptor.decodeFallbackChain(null));
    assertNull(LocalesAdaptor.decodeFallbackChain("  "));
  }
}

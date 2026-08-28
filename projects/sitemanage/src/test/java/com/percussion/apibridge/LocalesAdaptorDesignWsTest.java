/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.i18n.PSLocale;
import com.percussion.rest.locales.LocaleDesignLockException;
import com.percussion.rest.locales.LocaleDetail;
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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

  private static PSLocale newLocale(int id, String lang, String label) {
    PSLocale loc = new PSLocale();
    loc.setLocaleId(id);
    loc.setLanguageString(lang);
    loc.setDisplayName(label);
    loc.setStatus(PSLocale.STATUS_INACTIVE);
    return loc;
  }
}

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
package com.percussion.services.virtualsite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.sitemgr.data.PSSite;
import com.percussion.services.sitemgr.data.PSSiteProperty;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

class PSVirtualSiteHelperTest {

  @Test
  void repositoryWhenNoSourceKind() {
    PSSite site = mock(PSSite.class);
    when(site.getProperties()).thenReturn(Set.of());
    assertEquals(SourceKind.REPOSITORY, PSVirtualSiteHelper.sourceKind(site));
    assertFalse(PSVirtualSiteHelper.isVirtual(site));
  }

  @Test
  void virtualWhenGitFilesystem() {
    PSSite site = mock(PSSite.class);
    PSSiteProperty kind = new PSSiteProperty();
    kind.setName(PSVirtualSiteHelper.PROP_SOURCE_KIND);
    kind.setValue("git-filesystem");
    PSSiteProperty root = new PSSiteProperty();
    root.setName(PSVirtualSiteHelper.PROP_ROOT_PATH);
    root.setValue("C:/docs/product-docs");
    Set<PSSiteProperty> props = new HashSet<>();
    props.add(kind);
    props.add(root);
    when(site.getProperties()).thenReturn(props);
    when(site.getName()).thenReturn("Help");

    assertTrue(PSVirtualSiteHelper.isVirtual(site));
    assertEquals(
        VirtualSiteSourceType.GIT_FILESYSTEM,
        PSVirtualSiteHelper.virtualSourceType(site).orElseThrow());
    assertEquals("Help", PSVirtualSiteHelper.siteKey(site));
    assertTrue(PSVirtualSiteHelper.rootPath(site).isPresent());
  }

  @Test
  void repositoryWhenSourceKindExplicitlyRepository() {
    PSSite site = mock(PSSite.class);
    PSSiteProperty kind = new PSSiteProperty();
    kind.setName(PSVirtualSiteHelper.PROP_SOURCE_KIND);
    kind.setValue("repository");
    when(site.getProperties()).thenReturn(Set.of(kind));
    assertEquals(SourceKind.REPOSITORY, PSVirtualSiteHelper.sourceKind(site));
    assertFalse(PSVirtualSiteHelper.isVirtual(site));
  }

  @Test
  void configFileDefaultsAndOverride() {
    PSSite bare = mock(PSSite.class);
    when(bare.getProperties()).thenReturn(Set.of());
    assertEquals(VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, PSVirtualSiteHelper.configFile(bare));

    PSSite site = mock(PSSite.class);
    PSSiteProperty cfg = new PSSiteProperty();
    cfg.setName(PSVirtualSiteHelper.PROP_CONFIG_FILE);
    cfg.setValue("custom-site.yaml");
    when(site.getProperties()).thenReturn(Set.of(cfg));
    assertEquals("custom-site.yaml", PSVirtualSiteHelper.configFile(site));
  }

  @Test
  void siteKeyPrefersPropertyThenNameThenDefault() {
    PSSite withKey = mock(PSSite.class);
    PSSiteProperty key = new PSSiteProperty();
    key.setName(PSVirtualSiteHelper.PROP_SITE_KEY);
    key.setValue("product-docs");
    when(withKey.getProperties()).thenReturn(Set.of(key));
    when(withKey.getName()).thenReturn("Help");
    assertEquals("product-docs", PSVirtualSiteHelper.siteKey(withKey));

    PSSite named = mock(PSSite.class);
    when(named.getProperties()).thenReturn(Set.of());
    when(named.getName()).thenReturn("HelpSite");
    assertEquals("HelpSite", PSVirtualSiteHelper.siteKey(named));

    assertEquals("default", PSVirtualSiteHelper.siteKey(null));
  }

  @Test
  void blankRootPathIsAbsent() {
    PSSite site = mock(PSSite.class);
    PSSiteProperty root = new PSSiteProperty();
    root.setName(PSVirtualSiteHelper.PROP_ROOT_PATH);
    root.setValue("   ");
    when(site.getProperties()).thenReturn(Set.of(root));
    assertTrue(PSVirtualSiteHelper.rootPath(site).isEmpty());
  }

  @Test
  void rootPathNormalizesViaNio() {
    PSSite site = siteWith(
        prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "docs/product-docs/./pages/../"));
    Path root = PSVirtualSiteHelper.rootPath(site).orElseThrow();
    assertEquals(Path.of("docs", "product-docs").normalize(), root);
  }

  @Test
  void allowedSourceKindsIncludeGitCsvSqlHttpJsonObjectStorageRssAtomIcalendarSitemapXmlRobotsTxtLlmsTxtAndOpenApiYaml() {
    List<String> allowed = PSVirtualSiteHelper.allowedSourceKindWireNames();
    assertEquals(
        List.of(
            "git-filesystem",
            "csv-filesystem",
            "sql-database",
            "http-json",
            "object-storage",
            "rss-atom",
            "icalendar",
            "sitemap-xml",
            "robots-txt",
            "llms-txt",
            "openapi-yaml"),
        allowed);
  }

  @Test
  void validatePassesForNullAndRepositorySites() {
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(null));

    PSSite bare = mock(PSSite.class);
    when(bare.getProperties()).thenReturn(Set.of());
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(bare));

    PSSite repo = siteWith(prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "repository"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(repo));
  }

  @Test
  void validatePassesForAllowListedVirtualWithSafeRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "product-docs"),
            prop(PSVirtualSiteHelper.PROP_CONFIG_FILE, "_config.yaml"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
  }

  @Test
  void validatePassesForCsvFilesystemWithSafeRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "csv-filesystem"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "csv-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
    assertEquals(
        VirtualSiteSourceType.CSV_FILESYSTEM,
        PSVirtualSiteHelper.virtualSourceType(site).orElseThrow());
  }

  @Test
  void validatePassesForSqlDatabaseWithSafeRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "sql-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
    assertEquals(
        VirtualSiteSourceType.SQL_DATABASE,
        PSVirtualSiteHelper.virtualSourceType(site).orElseThrow());
  }

  @Test
  void validatePassesForHttpJsonWithSafeRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "http-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
    assertEquals(
        VirtualSiteSourceType.HTTP_JSON,
        PSVirtualSiteHelper.virtualSourceType(site).orElseThrow());
  }

  @Test
  void validatePassesForObjectStorageWithSafeRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "object-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
    assertEquals(
        VirtualSiteSourceType.OBJECT_STORAGE,
        PSVirtualSiteHelper.virtualSourceType(site).orElseThrow());
  }

  @Test
  void validatePassesForObjectStorageDriveLetterStyleRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "C:/object-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
  }

  @Test
  void validateRejectsRemoteUrlForObjectStorage() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "object-docs"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_REMOTE_URL));
    assertTrue(ex.getMessage().contains("object-storage"));
  }

  @Test
  void validateRejectsPathTraversalForObjectStorage() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "../outside"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH));
    assertTrue(ex.getMessage().contains(".."));
  }

  @Test
  void validateRejectsCloudUrlRootForObjectStorage() {
    for (String cloud :
        List.of(
            "s3://bucket/docs",
            "gs://bucket/docs",
            "azure://account/container",
            "https://s3.amazonaws.com/bucket/docs",
            "http://minio.example/bucket")) {
      PSSite site =
          siteWith(
              prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage"),
              prop(PSVirtualSiteHelper.PROP_ROOT_PATH, cloud));
      VirtualSiteException ex =
          assertThrows(
              VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site), cloud);
      assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH), ex.getMessage());
      assertTrue(ex.getMessage().toLowerCase().contains("cloud"), ex.getMessage());
      assertFalse(ex.getMessage().contains("AKIA"), ex.getMessage());
    }
  }

  @Test
  void validateRejectsCredentialPropertyForObjectStorage() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "object-docs"),
            prop("aws_secret_access_key", "not-a-real-secret"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().toLowerCase().contains("credential"), ex.getMessage());
    assertTrue(ex.getMessage().contains("object-storage"), ex.getMessage());
    assertFalse(ex.getMessage().contains("not-a-real-secret"), ex.getMessage());
  }

  @Test
  void validatePassesForRssAtomWithSafeRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "rss-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
    assertEquals(
        VirtualSiteSourceType.RSS_ATOM,
        PSVirtualSiteHelper.virtualSourceType(site).orElseThrow());
  }

  @Test
  void validatePassesForRssAtomDriveLetterStyleRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "C:/rss-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
  }

  @Test
  void validateRejectsRemoteUrlForRssAtom() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "rss-docs"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_REMOTE_URL));
    assertTrue(ex.getMessage().contains("rss-atom"));
  }

  @Test
  void validateRejectsPathTraversalForRssAtom() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "../outside"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH));
    assertTrue(ex.getMessage().contains(".."));
  }

  @Test
  void validateRejectsCloudUrlRootForRssAtom() {
    for (String cloud :
        List.of(
            "s3://bucket/feeds",
            "https://feeds.example.com/rss.xml",
            "http://minio.example/feeds")) {
      PSSite site =
          siteWith(
              prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom"),
              prop(PSVirtualSiteHelper.PROP_ROOT_PATH, cloud));
      VirtualSiteException ex =
          assertThrows(
              VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site), cloud);
      assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH), ex.getMessage());
      assertTrue(ex.getMessage().toLowerCase().contains("cloud"), ex.getMessage());
      assertTrue(ex.getMessage().contains("rss-atom"), ex.getMessage());
    }
  }

  @Test
  void validateRejectsCredentialPropertyForRssAtom() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "rss-docs"),
            prop("aws_secret_access_key", "not-a-real-secret"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().toLowerCase().contains("credential"), ex.getMessage());
    assertTrue(ex.getMessage().contains("rss-atom"), ex.getMessage());
    assertFalse(ex.getMessage().contains("not-a-real-secret"), ex.getMessage());
  }

  @Test
  void rejectCloudOrRemoteRootPathNamesRssAtomNotObjectStorage() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSVirtualSiteHelper.rejectCloudOrRemoteRootPath(
                    "https://feeds.example.com/rss.xml", VirtualSiteSourceType.RSS_ATOM));
    assertTrue(ex.getMessage().contains("rss-atom"), ex.getMessage());
    assertFalse(ex.getMessage().contains("object-storage"), ex.getMessage());
  }

  @Test
  void rejectCredentialPropertiesNamesRssAtomNotObjectStorage() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "rss-docs"),
            prop("aws_secret_access_key", "not-a-real-secret"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSVirtualSiteHelper.rejectCredentialProperties(
                    site, VirtualSiteSourceType.RSS_ATOM));
    assertTrue(ex.getMessage().contains("rss-atom"), ex.getMessage());
    assertFalse(ex.getMessage().contains("object-storage"), ex.getMessage());
    assertFalse(ex.getMessage().contains("not-a-real-secret"), ex.getMessage());
  }

  @Test
  void validatePassesForIcalendarWithSafeRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "icalendar"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "cal-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
    assertEquals(
        VirtualSiteSourceType.ICALENDAR,
        PSVirtualSiteHelper.virtualSourceType(site).orElseThrow());
  }

  @Test
  void validatePassesForIcalendarDriveLetterStyleRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "icalendar"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "C:/cal-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
  }

  @Test
  void validateRejectsRemoteUrlForIcalendar() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "icalendar"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "cal-docs"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_REMOTE_URL));
    assertTrue(ex.getMessage().contains("icalendar"));
  }

  @Test
  void validateRejectsPathTraversalForIcalendar() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "icalendar"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "../outside"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH));
    assertTrue(ex.getMessage().contains(".."));
  }

  @Test
  void validateRejectsCloudUrlRootForIcalendar() {
    for (String cloud :
        List.of(
            "s3://bucket/calendars",
            "https://calendar.example.com/cal.ics",
            "caldav://calendar.example.com/cal")) {
      PSSite site =
          siteWith(
              prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "icalendar"),
              prop(PSVirtualSiteHelper.PROP_ROOT_PATH, cloud));
      VirtualSiteException ex =
          assertThrows(
              VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site), cloud);
      assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH), ex.getMessage());
      assertTrue(ex.getMessage().toLowerCase().contains("cloud"), ex.getMessage());
      assertTrue(ex.getMessage().contains("icalendar"), ex.getMessage());
    }
  }

  @Test
  void validateRejectsCredentialPropertyForIcalendar() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "icalendar"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "cal-docs"),
            prop("aws_secret_access_key", "not-a-real-secret"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().toLowerCase().contains("credential"), ex.getMessage());
    assertTrue(ex.getMessage().contains("icalendar"), ex.getMessage());
    assertFalse(ex.getMessage().contains("not-a-real-secret"), ex.getMessage());
  }

  @Test
  void rejectCloudOrRemoteRootPathNamesIcalendarNotRssAtom() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSVirtualSiteHelper.rejectCloudOrRemoteRootPath(
                    "https://calendar.example.com/cal.ics", VirtualSiteSourceType.ICALENDAR));
    assertTrue(ex.getMessage().contains("icalendar"), ex.getMessage());
    assertFalse(ex.getMessage().contains("rss-atom"), ex.getMessage());
  }

  @Test
  void rejectCredentialPropertiesNamesIcalendarNotRssAtom() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "icalendar"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "cal-docs"),
            prop("aws_secret_access_key", "not-a-real-secret"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSVirtualSiteHelper.rejectCredentialProperties(
                    site, VirtualSiteSourceType.ICALENDAR));
    assertTrue(ex.getMessage().contains("icalendar"), ex.getMessage());
    assertFalse(ex.getMessage().contains("rss-atom"), ex.getMessage());
    assertFalse(ex.getMessage().contains("not-a-real-secret"), ex.getMessage());
  }

  @Test
  void rejectCloudOrRemoteRootPathRequiresType() {
    assertThrows(
        NullPointerException.class,
        () -> PSVirtualSiteHelper.rejectCloudOrRemoteRootPath("s3://bucket/docs", null));
  }

  @Test
  void rejectCredentialPropertiesRequiresType() {
    PSSite site = siteWith(prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "rss-docs"));
    assertThrows(
        NullPointerException.class, () -> PSVirtualSiteHelper.rejectCredentialProperties(site, null));
  }

  @Test
  void validateRejectsRemoteUrlForHttpJson() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "http-docs"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_REMOTE_URL));
    assertTrue(ex.getMessage().contains("http-json"));
  }

  @Test
  void validateRejectsPathTraversalForHttpJson() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "../outside"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH));
    assertTrue(ex.getMessage().contains(".."));
  }

  @Test
  void validateRejectsRemoteUrlForSqlDatabase() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "sql-docs"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_REMOTE_URL));
    assertTrue(ex.getMessage().contains("sql-database"));
  }

  @Test
  void validateRejectsRemoteUrlForCsvFilesystem() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "csv-filesystem"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "csv-docs"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_REMOTE_URL));
    assertTrue(ex.getMessage().contains("csv-filesystem"));
  }

  @Test
  void validateRejectsPathTraversalForCsvFilesystem() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "csv-filesystem"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "../outside"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH));
    assertTrue(ex.getMessage().contains(".."));
  }

  @Test
  void validateRejectsUnknownSourceKind() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-api"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "product-docs"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_SOURCE_KIND));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("git-filesystem"));
    assertTrue(ex.getMessage().contains("csv-filesystem"));
    assertTrue(ex.getMessage().contains("sql-database"));
    assertTrue(ex.getMessage().contains("http-json"));
    assertTrue(ex.getMessage().contains("object-storage"));
    assertTrue(ex.getMessage().contains("rss-atom"));
    assertTrue(ex.getMessage().contains("icalendar"));
    assertTrue(ex.getMessage().contains("sitemap-xml"));
    assertTrue(ex.getMessage().contains("robots-txt"));
    assertTrue(ex.getMessage().contains("llms-txt"));
    assertTrue(ex.getMessage().contains("openapi-yaml"));
  }

  @Test
  void validatePassesForSitemapXmlWithSafeRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "sitemap-xml"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "sm-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
    assertEquals(
        VirtualSiteSourceType.SITEMAP_XML,
        PSVirtualSiteHelper.virtualSourceType(site).orElseThrow());
  }

  @Test
  void validateRejectsRemoteUrlForSitemapXml() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "sitemap-xml"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "sm-docs"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_REMOTE_URL));
    assertTrue(ex.getMessage().contains("sitemap-xml"));
  }

  @Test
  void validateRejectsCredentialPropertyForSitemapXml() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "sitemap-xml"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "sm-docs"),
            prop("aws_secret_access_key", "not-a-real-secret"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().toLowerCase().contains("credential"), ex.getMessage());
    assertTrue(ex.getMessage().contains("sitemap-xml"), ex.getMessage());
    assertFalse(ex.getMessage().contains("not-a-real-secret"), ex.getMessage());
  }

  @Test
  void validateRejectsCloudUrlRootForSitemapXml() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "sitemap-xml"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "https://example.com/sitemap.xml"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH), ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("cloud"), ex.getMessage());
    assertTrue(ex.getMessage().contains("sitemap-xml"), ex.getMessage());
  }

  @Test
  void validatePassesForRobotsTxtWithSafeRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "robots-txt"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "rb-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
    assertEquals(
        VirtualSiteSourceType.ROBOTS_TXT,
        PSVirtualSiteHelper.virtualSourceType(site).orElseThrow());
  }

  @Test
  void validateRejectsRemoteUrlForRobotsTxt() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "robots-txt"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "rb-docs"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_REMOTE_URL));
    assertTrue(ex.getMessage().contains("robots-txt"));
  }

  @Test
  void validateRejectsCredentialPropertyForRobotsTxt() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "robots-txt"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "rb-docs"),
            prop("aws_secret_access_key", "not-a-real-secret"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().toLowerCase().contains("credential"), ex.getMessage());
    assertTrue(ex.getMessage().contains("robots-txt"), ex.getMessage());
    assertFalse(ex.getMessage().contains("not-a-real-secret"), ex.getMessage());
  }

  @Test
  void validateRejectsCloudUrlRootForRobotsTxt() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "robots-txt"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "https://example.com/robots.txt"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH), ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("cloud"), ex.getMessage());
    assertTrue(ex.getMessage().contains("robots-txt"), ex.getMessage());
  }

  @Test
  void validatePassesForLlmsTxtWithSafeRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "llms-txt"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "llms-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
    assertEquals(
        VirtualSiteSourceType.LLMS_TXT,
        PSVirtualSiteHelper.virtualSourceType(site).orElseThrow());
  }

  @Test
  void validateRejectsRemoteUrlForLlmsTxt() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "llms-txt"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "llms-docs"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_REMOTE_URL));
    assertTrue(ex.getMessage().contains("llms-txt"));
  }

  @Test
  void validateRejectsCredentialPropertyForLlmsTxt() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "llms-txt"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "llms-docs"),
            prop("aws_secret_access_key", "not-a-real-secret"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().toLowerCase().contains("credential"), ex.getMessage());
    assertTrue(ex.getMessage().contains("llms-txt"), ex.getMessage());
    assertFalse(ex.getMessage().contains("not-a-real-secret"), ex.getMessage());
  }

  @Test
  void validateRejectsCloudUrlRootForLlmsTxt() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "llms-txt"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "https://example.com/llms.txt"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH), ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("cloud"), ex.getMessage());
    assertTrue(ex.getMessage().contains("llms-txt"), ex.getMessage());
  }

  @Test
  void validatePassesForOpenApiYamlWithSafeRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "openapi-yaml"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "oa-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
    assertEquals(
        VirtualSiteSourceType.OPENAPI_YAML,
        PSVirtualSiteHelper.virtualSourceType(site).orElseThrow());
  }

  @Test
  void validateRejectsRemoteUrlForOpenApiYaml() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "openapi-yaml"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "oa-docs"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_REMOTE_URL));
    assertTrue(ex.getMessage().contains("openapi-yaml"));
  }

  @Test
  void validateRejectsCredentialPropertyForOpenApiYaml() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "openapi-yaml"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "oa-docs"),
            prop("aws_secret_access_key", "not-a-real-secret"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().toLowerCase().contains("credential"), ex.getMessage());
    assertTrue(ex.getMessage().contains("openapi-yaml"), ex.getMessage());
    assertFalse(ex.getMessage().contains("not-a-real-secret"), ex.getMessage());
  }

  @Test
  void validateRejectsCloudUrlRootForOpenApiYaml() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "openapi-yaml"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "https://example.com/openapi.yaml"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH), ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("cloud"), ex.getMessage());
    assertTrue(ex.getMessage().contains("openapi-yaml"), ex.getMessage());
  }

  @Test
  void validateRejectsMissingRootPathWhenVirtual() {
    PSSite site = siteWith(prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH));
    assertTrue(ex.getMessage().contains("required"));
  }

  @Test
  void validatePassesForRemoteWithoutLocalRoot() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/docs/product-docs.git"),
            prop(PSVirtualSiteHelper.PROP_BRANCH, "main"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
    assertTrue(PSVirtualSiteHelper.hasRemote(site));
    assertEquals("main", PSVirtualSiteHelper.branch(site));
  }

  @Test
  void validatePassesForRemoteWithRelativeSubPath() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/repo.git"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "product-docs"));
    assertDoesNotThrow(() -> PSVirtualSiteHelper.validate(site));
  }

  @Test
  void validateRejectsUnsafeRemoteUrl() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "http://evil.example/../x"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_REMOTE_URL));
    assertFalse(ex.getMessage().contains("http://evil.example"));
  }

  @Test
  void validateRejectsAbsoluteRootWhenRemoteSet() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/repo.git"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "C:/docs/product-docs"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"));
  }

  @Test
  void validateRejectsParentSegmentInRemoteSubPath() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/repo.git"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "../outside"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH));
  }

  @Test
  void branchDefaultsToMain() {
    PSSite site = siteWith(prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"));
    assertEquals(PSVirtualSiteHelper.DEFAULT_BRANCH, PSVirtualSiteHelper.branch(site));
    assertEquals(PSVirtualSiteHelper.DEFAULT_BRANCH, PSVirtualSiteHelper.branch(null));
  }

  @Test
  void resolveDiscoverRootUsesCheckoutAndRelativeSubPath() throws Exception {
    Path checkout = Path.of("work", "docs-checkout").normalize();
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"),
            prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/repo.git"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "product-docs"));
    Path resolved = PSVirtualSiteHelper.resolveDiscoverRoot(site, checkout);
    assertEquals(checkout.resolve("product-docs").normalize(), resolved);
  }

  @Test
  void validateRejectsBlankRootPathWhenVirtual() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "   "));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH));
  }

  @Test
  void validateRejectsPathTraversalInRootPath() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "../outside"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH));
    assertTrue(ex.getMessage().contains(".."));
  }

  @Test
  void validateRejectsRelativeParentSegmentsAfterNormalize() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "docs/../../etc"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_ROOT_PATH));
  }

  @Test
  void validateRejectsConfigFileWithPathSeparators() {
    PSSite site =
        siteWith(
            prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"),
            prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "product-docs"),
            prop(PSVirtualSiteHelper.PROP_CONFIG_FILE, "../evil.yaml"));
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> PSVirtualSiteHelper.validate(site));
    assertTrue(ex.getMessage().contains(PSVirtualSiteHelper.PROP_CONFIG_FILE));
  }

  @Test
  void putPropertySetsUpdatesAndClears() {
    com.percussion.services.guidmgr.data.PSGuid ctx =
        new com.percussion.services.guidmgr.data.PSGuid(
            com.percussion.services.catalog.PSTypeEnum.CONTEXT, 1L);
    PSSite site = new PSSite();
    PSVirtualSiteHelper.putProperty(site, ctx, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    assertEquals(
        "git-filesystem",
        PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_SOURCE_KIND).orElse(null));

    PSVirtualSiteHelper.putProperty(site, ctx, PSVirtualSiteHelper.PROP_SOURCE_KIND, "repository");
    assertEquals(
        "repository",
        PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_SOURCE_KIND).orElse(null));

    PSVirtualSiteHelper.putProperty(site, ctx, PSVirtualSiteHelper.PROP_SOURCE_KIND, null);
    assertTrue(
        PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_SOURCE_KIND).isEmpty());
  }

  @Test
  void isSafeRootPathRejectsEmptyAndDotAndParent() {
    assertFalse(PSVirtualSiteHelper.isSafeRootPath(null));
    assertFalse(PSVirtualSiteHelper.isSafeRootPath(Path.of("").normalize()));
    assertFalse(PSVirtualSiteHelper.isSafeRootPath(Path.of(".").normalize()));
    assertFalse(PSVirtualSiteHelper.isSafeRootPath(Path.of("..").normalize()));
    assertFalse(PSVirtualSiteHelper.isSafeRootPath(Path.of("a", "..", "..", "b").normalize()));
    assertTrue(PSVirtualSiteHelper.isSafeRootPath(Path.of("product-docs").normalize()));
    assertTrue(PSVirtualSiteHelper.isSafeRootPath(Path.of("docs", "product-docs").normalize()));
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void isSafeRootPathAllowsWindowsAbsolute() {
    assertTrue(PSVirtualSiteHelper.isSafeRootPath(Path.of("C:\\docs\\product-docs").normalize()));
    // Absolute path whose normalize removes ".." is still absolute and has no remaining ".."
    assertTrue(
        PSVirtualSiteHelper.isSafeRootPath(Path.of("C:\\docs\\product-docs\\..\\help").normalize()));
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void isSafeRootPathAllowsUnixAbsolute() {
    assertTrue(PSVirtualSiteHelper.isSafeRootPath(Path.of("/opt/product-docs").normalize()));
    assertTrue(
        PSVirtualSiteHelper.isSafeRootPath(Path.of("/opt/product-docs/../help").normalize()));
  }

  private static PSSiteProperty prop(String name, String value) {
    PSSiteProperty p = new PSSiteProperty();
    p.setName(name);
    p.setValue(value);
    return p;
  }

  private static PSSite siteWith(PSSiteProperty... properties) {
    PSSite site = mock(PSSite.class);
    Set<PSSiteProperty> props = new HashSet<>();
    for (PSSiteProperty p : properties) {
      props.add(p);
    }
    when(site.getProperties()).thenReturn(props);
    return site;
  }
}

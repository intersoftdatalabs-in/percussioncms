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
  void allowedSourceKindsIncludeGitFilesystemOnlyInPhase1() {
    List<String> allowed = PSVirtualSiteHelper.allowedSourceKindWireNames();
    assertEquals(List.of("git-filesystem"), allowed);
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

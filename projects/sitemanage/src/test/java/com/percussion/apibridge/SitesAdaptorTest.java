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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.sites.Site;
import com.percussion.rest.sites.VirtualSiteBuildRequest;
import com.percussion.rest.sites.VirtualSiteBuildResult;
import com.percussion.rest.sites.VirtualSitePreviewFile;
import com.percussion.rest.sites.VirtualSitePreviewStatus;
import com.percussion.rest.sites.VirtualSiteProperties;
import com.percussion.rest.sites.VirtualSitePublishResult;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.data.PSSite;
import com.percussion.services.sitemgr.data.PSSiteProperty;
import com.percussion.services.virtualsite.PSGitRemoteCheckout;
import com.percussion.services.virtualsite.PSVirtualSiteBuildResult;
import com.percussion.services.virtualsite.PSManagedNavSiteHelper;
import com.percussion.services.virtualsite.PSVirtualSiteHelper;
import java.time.Duration;
import com.percussion.utils.guid.IPSGuid;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// uses PSVirtualSiteHelper.putProperty (no GuidManager) for property bag setup

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class SitesAdaptorTest {

  @Mock private IPSSiteManager siteManager;

  @TempDir Path tempDir;

  private SitesAdaptor adaptor;
  private IPSGuid previewCtx;
  private IPSGuid siteGuid;

  @BeforeEach
  void setUp() {
    adaptor = new SitesAdaptor(siteManager);
    previewCtx = new PSGuid(PSTypeEnum.CONTEXT, 1);
    siteGuid = new PSGuid(PSTypeEnum.SITE, 100);
  }

  @Test
  void readVirtual_mapsHelperProperties() {
    PSSite site = new PSSite();
    site.setName("Help");
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, "C:/docs/product-docs");
    put(site, PSVirtualSiteHelper.PROP_CONFIG_FILE, "custom.yaml");
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "product-docs");
    put(site, PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git");
    put(site, PSVirtualSiteHelper.PROP_BRANCH, "release/8.2");

    VirtualSiteProperties v = SitesAdaptor.readVirtual(site);
    assertEquals("git-filesystem", v.getSourceKind());
    assertEquals("C:/docs/product-docs", v.getRootPath());
    assertEquals("custom.yaml", v.getConfigFile());
    assertEquals("product-docs", v.getSiteKey());
    assertEquals("https://git.example.com/org/docs.git", v.getRemoteUrl());
    assertEquals("release/8.2", v.getBranch());
    assertTrue(Boolean.TRUE.equals(v.getVirtual()));
  }

  @Test
  void readVirtual_repositoryIsNotVirtual() {
    PSSite site = new PSSite();
    site.setName("Corp");
    VirtualSiteProperties v = SitesAdaptor.readVirtual(site);
    assertFalse(Boolean.TRUE.equals(v.getVirtual()));
    assertTrue(v.getSourceKind() == null || v.getSourceKind().isBlank());
  }

  @Test
  void findByName_returnsDetailWithVirtual() {
    PSSite site = new PSSite();
    site.setName("Help");
    site.setDescription("docs");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, "C:/docs");
    when(siteManager.findSite("Help")).thenReturn(site);

    Site out = adaptor.findByName("Help");
    assertNotNull(out);
    assertEquals("Help", out.getName());
    assertTrue(out.getVirtual() != null);
    assertTrue(Boolean.TRUE.equals(out.getVirtual().getVirtual()));
    assertNull(out.getManagedNavigation());
  }

  @Test
  void findByName_traditionalIncludesManagedNavigationFlag() {
    PSSite site = new PSSite();
    site.setName("Corp");
    site.setGUID(siteGuid);
    when(siteManager.findSite("Corp")).thenReturn(site);

    Site out = adaptor.findByName("Corp");
    assertNotNull(out);
    assertEquals(Boolean.TRUE, out.getManagedNavigation());
  }

  @Test
  void findByName_traditionalFalseManagedNavigation() {
    PSSite site = new PSSite();
    site.setName("Bare");
    site.setGUID(siteGuid);
    put(site, PSManagedNavSiteHelper.PROP_MANAGED, "false");
    when(siteManager.findSite("Bare")).thenReturn(site);

    Site out = adaptor.findByName("Bare");
    assertEquals(Boolean.FALSE, out.getManagedNavigation());
  }

  @Test
  void getVirtualSiteProperties_404WhenMissing() {
    when(siteManager.findSite("missing")).thenReturn(null);
    // guid parse may throw or return null find
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.getVirtualSiteProperties("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void updateVirtualSiteProperties_persistsAndValidates() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("Help");
    existing.setGUID(siteGuid);

    PSSite modifiable = new PSSite();
    modifiable.setName("Help");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("Help")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("git-filesystem");
    body.setRootPath("C:/docs/product-docs");
    body.setConfigFile("_config.yaml");
    body.setSiteKey("product-docs");

    VirtualSiteProperties out = adaptor.updateVirtualSiteProperties("Help", body);
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    assertEquals("git-filesystem", out.getSourceKind());
    assertEquals("C:/docs/product-docs", out.getRootPath());

    ArgumentCaptor<PSSite> saved = ArgumentCaptor.forClass(PSSite.class);
    verify(siteManager).saveSite(saved.capture());
    PSSite persisted = saved.getValue();
    assertTrue(PSVirtualSiteHelper.isVirtual(persisted));
    assertEquals(
        "product-docs",
        PSVirtualSiteHelper.findProperty(persisted, PSVirtualSiteHelper.PROP_SITE_KEY)
            .orElse(null));
    assertEquals(100L, persisted.getSiteId());
    assertTrue(PSVirtualSiteHelper.remoteUrl(persisted).isEmpty());
    assertFalse(persisted.getProperties().isEmpty());
    for (PSSiteProperty property : persisted.getProperties()) {
      assertSame(persisted, property.getSite(), property.getName());
      assertEquals(100L, ((PSSite) property.getSite()).getSiteId());
    }
  }

  @Test
  void updateVirtualSiteProperties_csvFilesystemRoundTripsOnGet() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("CsvHelp");
    existing.setGUID(siteGuid);

    PSSite modifiable = new PSSite();
    modifiable.setName("CsvHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("CsvHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("csv-filesystem");
    body.setRootPath("C:/csv-docs");
    body.setSiteKey("csv-help");

    VirtualSiteProperties out = adaptor.updateVirtualSiteProperties("CsvHelp", body);
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    assertEquals("csv-filesystem", out.getSourceKind());
    assertEquals("C:/csv-docs", out.getRootPath());
    assertEquals("csv-help", out.getSiteKey());

    ArgumentCaptor<PSSite> saved = ArgumentCaptor.forClass(PSSite.class);
    verify(siteManager).saveSite(saved.capture());
    PSSite persisted = saved.getValue();
    assertTrue(PSVirtualSiteHelper.isVirtual(persisted));
    assertEquals(
        "csv-filesystem",
        PSVirtualSiteHelper.findProperty(persisted, PSVirtualSiteHelper.PROP_SOURCE_KIND)
            .orElse(null));

    when(siteManager.findSite("CsvHelp")).thenReturn(persisted);
    VirtualSiteProperties loaded = adaptor.getVirtualSiteProperties("CsvHelp");
    assertEquals("csv-filesystem", loaded.getSourceKind());
    assertEquals("C:/csv-docs", loaded.getRootPath());
    assertTrue(Boolean.TRUE.equals(loaded.getVirtual()));
  }

  @Test
  void updateVirtualSiteProperties_csvRejectsParentRootPath() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("CsvHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("CsvHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("CsvHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("csv-filesystem");
    body.setRootPath("../outside");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("CsvHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_csvRejectsRemoteUrl() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("CsvHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("CsvHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("CsvHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("csv-filesystem");
    body.setRootPath("C:/csv-docs");
    body.setRemoteUrl("https://git.example.com/org/docs.git");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("CsvHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_sqlDatabaseRoundTripsOnGet() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("SqlHelp");
    existing.setGUID(siteGuid);

    PSSite modifiable = new PSSite();
    modifiable.setName("SqlHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("SqlHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sql-database");
    body.setRootPath("C:/sql-docs");
    body.setSiteKey("sql-help");

    VirtualSiteProperties out = adaptor.updateVirtualSiteProperties("SqlHelp", body);
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    assertEquals("sql-database", out.getSourceKind());
    assertEquals("C:/sql-docs", out.getRootPath());
    assertEquals("sql-help", out.getSiteKey());
    assertNull(out.getRemoteUrl());

    ArgumentCaptor<PSSite> saved = ArgumentCaptor.forClass(PSSite.class);
    verify(siteManager).saveSite(saved.capture());
    PSSite persisted = saved.getValue();
    assertTrue(PSVirtualSiteHelper.isVirtual(persisted));
    assertEquals(
        "sql-database",
        PSVirtualSiteHelper.findProperty(persisted, PSVirtualSiteHelper.PROP_SOURCE_KIND)
            .orElse(null));

    when(siteManager.findSite("SqlHelp")).thenReturn(persisted);
    VirtualSiteProperties loaded = adaptor.getVirtualSiteProperties("SqlHelp");
    assertEquals("sql-database", loaded.getSourceKind());
    assertEquals("C:/sql-docs", loaded.getRootPath());
    assertTrue(Boolean.TRUE.equals(loaded.getVirtual()));
  }

  @Test
  void updateVirtualSiteProperties_sqlRejectsParentRootPath() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("SqlHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("SqlHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("SqlHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sql-database");
    body.setRootPath("../outside");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("SqlHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_sqlRejectsRemoteUrl() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("SqlHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("SqlHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("SqlHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sql-database");
    body.setRootPath("C:/sql-docs");
    body.setRemoteUrl("https://git.example.com/org/docs.git");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("SqlHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_httpJsonRoundTripsOnGet() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("HttpHelp");
    existing.setGUID(siteGuid);

    PSSite modifiable = new PSSite();
    modifiable.setName("HttpHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("HttpHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("http-json");
    body.setRootPath("C:/http-docs");
    body.setSiteKey("http-help");

    VirtualSiteProperties out = adaptor.updateVirtualSiteProperties("HttpHelp", body);
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    assertEquals("http-json", out.getSourceKind());
    assertEquals("C:/http-docs", out.getRootPath());
    assertEquals("http-help", out.getSiteKey());
    assertNull(out.getRemoteUrl());

    ArgumentCaptor<PSSite> saved = ArgumentCaptor.forClass(PSSite.class);
    verify(siteManager).saveSite(saved.capture());
    PSSite persisted = saved.getValue();
    assertTrue(PSVirtualSiteHelper.isVirtual(persisted));
    assertEquals(
        "http-json",
        PSVirtualSiteHelper.findProperty(persisted, PSVirtualSiteHelper.PROP_SOURCE_KIND)
            .orElse(null));

    when(siteManager.findSite("HttpHelp")).thenReturn(persisted);
    VirtualSiteProperties loaded = adaptor.getVirtualSiteProperties("HttpHelp");
    assertEquals("http-json", loaded.getSourceKind());
    assertEquals("C:/http-docs", loaded.getRootPath());
    assertTrue(Boolean.TRUE.equals(loaded.getVirtual()));
  }

  @Test
  void updateVirtualSiteProperties_httpJsonRejectsParentRootPath() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("HttpHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("HttpHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("HttpHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("http-json");
    body.setRootPath("../outside");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("HttpHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_httpJsonRejectsRemoteUrl() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("HttpHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("HttpHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("HttpHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("http-json");
    body.setRootPath("C:/http-docs");
    body.setRemoteUrl("https://user:secret@git.example.com/org/docs.git");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("HttpHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_objectStorageRoundTripsOnGet() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("ObjectHelp");
    existing.setGUID(siteGuid);

    PSSite modifiable = new PSSite();
    modifiable.setName("ObjectHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("ObjectHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("object-storage");
    body.setRootPath("C:/object-docs");
    body.setSiteKey("object-help");

    VirtualSiteProperties out = adaptor.updateVirtualSiteProperties("ObjectHelp", body);
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    assertEquals("object-storage", out.getSourceKind());
    assertEquals("C:/object-docs", out.getRootPath());
    assertEquals("object-help", out.getSiteKey());
    assertNull(out.getRemoteUrl());

    ArgumentCaptor<PSSite> saved = ArgumentCaptor.forClass(PSSite.class);
    verify(siteManager).saveSite(saved.capture());
    PSSite persisted = saved.getValue();
    assertTrue(PSVirtualSiteHelper.isVirtual(persisted));
    assertEquals(
        "object-storage",
        PSVirtualSiteHelper.findProperty(persisted, PSVirtualSiteHelper.PROP_SOURCE_KIND)
            .orElse(null));

    when(siteManager.findSite("ObjectHelp")).thenReturn(persisted);
    VirtualSiteProperties loaded = adaptor.getVirtualSiteProperties("ObjectHelp");
    assertEquals("object-storage", loaded.getSourceKind());
    assertEquals("C:/object-docs", loaded.getRootPath());
    assertTrue(Boolean.TRUE.equals(loaded.getVirtual()));
  }

  @Test
  void updateVirtualSiteProperties_objectStorageRejectsParentRootPath() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("ObjectHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("ObjectHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("ObjectHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("object-storage");
    body.setRootPath("../outside");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("ObjectHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_objectStorageRejectsRemoteUrl() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("ObjectHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("ObjectHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("ObjectHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("object-storage");
    body.setRootPath("C:/object-docs");
    body.setRemoteUrl("https://user:secret@git.example.com/org/docs.git");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("ObjectHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    assertFalse(String.valueOf(ex.getMessage()).contains("user:secret"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_objectStorageRejectsCloudUrl() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("ObjectHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("ObjectHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("ObjectHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("object-storage");
    body.setRootPath("s3://bucket/docs");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("ObjectHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("cloud"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_objectStorageRejectsCredentialProperty() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("ObjectHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("ObjectHelp");
    modifiable.setGUID(siteGuid);
    put(modifiable, "aws_secret_access_key", "not-a-real-secret");

    when(siteManager.findSite("ObjectHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("object-storage");
    body.setRootPath("C:/object-docs");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("ObjectHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("credential"));
    assertFalse(String.valueOf(ex.getMessage()).contains("not-a-real-secret"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_rssAtomRoundTripsOnGet() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("RssHelp");
    existing.setGUID(siteGuid);

    PSSite modifiable = new PSSite();
    modifiable.setName("RssHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("RssHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("rss-atom");
    body.setRootPath("C:/rss-docs");
    body.setSiteKey("rss-help");

    VirtualSiteProperties out = adaptor.updateVirtualSiteProperties("RssHelp", body);
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    assertEquals("rss-atom", out.getSourceKind());
    assertEquals("C:/rss-docs", out.getRootPath());
    assertEquals("rss-help", out.getSiteKey());
    assertNull(out.getRemoteUrl());

    ArgumentCaptor<PSSite> saved = ArgumentCaptor.forClass(PSSite.class);
    verify(siteManager).saveSite(saved.capture());
    PSSite persisted = saved.getValue();
    assertTrue(PSVirtualSiteHelper.isVirtual(persisted));
    assertEquals(
        "rss-atom",
        PSVirtualSiteHelper.findProperty(persisted, PSVirtualSiteHelper.PROP_SOURCE_KIND)
            .orElse(null));

    when(siteManager.findSite("RssHelp")).thenReturn(persisted);
    VirtualSiteProperties loaded = adaptor.getVirtualSiteProperties("RssHelp");
    assertEquals("rss-atom", loaded.getSourceKind());
    assertEquals("C:/rss-docs", loaded.getRootPath());
    assertTrue(Boolean.TRUE.equals(loaded.getVirtual()));
  }

  @Test
  void updateVirtualSiteProperties_rssAtomRejectsParentRootPath() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("RssHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("RssHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("RssHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("rss-atom");
    body.setRootPath("../outside");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("RssHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_rssAtomRejectsRemoteUrl() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("RssHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("RssHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("RssHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("rss-atom");
    body.setRootPath("C:/rss-docs");
    body.setRemoteUrl("https://user:secret@git.example.com/org/docs.git");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("RssHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"));
    assertFalse(String.valueOf(ex.getMessage()).contains("user:secret"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_rssAtomRejectsCloudUrl() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("RssHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("RssHelp");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("RssHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("rss-atom");
    body.setRootPath("https://feeds.example.com/rss.xml");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("RssHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("virtual.rootPath"));
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("cloud"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_rssAtomRejectsCredentialProperty() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("RssHelp");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("RssHelp");
    modifiable.setGUID(siteGuid);
    put(modifiable, "aws_secret_access_key", "not-a-real-secret");

    when(siteManager.findSite("RssHelp")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("rss-atom");
    body.setRootPath("C:/rss-docs");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("RssHelp", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("credential"));
    assertFalse(String.valueOf(ex.getMessage()).contains("not-a-real-secret"));
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_rejectsUnknownSourceKind() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("Help");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("Help");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("Help")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sql-adapter");
    body.setRootPath("C:/docs");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("Help", body));
    assertEquals(400, ex.getResponse().getStatus());
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_requiresRootWhenVirtual() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("Help");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("Help");
    modifiable.setGUID(siteGuid);

    when(siteManager.findSite("Help")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("git-filesystem");
    // rootPath missing

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("Help", body));
    assertEquals(400, ex.getResponse().getStatus());
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_persistsRemoteWithoutLocalRoot() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("Help");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("Help");
    modifiable.setGUID(siteGuid);
    when(siteManager.findSite("Help")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);
    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("git-filesystem");
    body.setRemoteUrl("https://git.example.com/org/product-docs.git");
    body.setBranch("main");
    body.setRootPath("product-docs");

    VirtualSiteProperties out = adaptor.updateVirtualSiteProperties("Help", body);
    assertEquals("https://git.example.com/org/product-docs.git", out.getRemoteUrl());
    assertEquals("main", out.getBranch());
    assertEquals("product-docs", out.getRootPath());
    ArgumentCaptor<PSSite> saved = ArgumentCaptor.forClass(PSSite.class);
    verify(siteManager).saveSite(saved.capture());
    assertEquals(
        "https://git.example.com/org/product-docs.git",
        PSVirtualSiteHelper.remoteUrl(saved.getValue()).orElse(null));
  }

  @Test
  void updateVirtualSiteProperties_omittedRemoteKeepsExisting() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("Help");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("Help");
    modifiable.setGUID(siteGuid);
    put(modifiable, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(modifiable, PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git");
    put(modifiable, PSVirtualSiteHelper.PROP_BRANCH, "main");
    put(modifiable, PSVirtualSiteHelper.PROP_ROOT_PATH, "product-docs");
    when(siteManager.findSite("Help")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("git-filesystem");
    body.setRootPath("product-docs");
    body.setSiteKey("docs");

    VirtualSiteProperties out = adaptor.updateVirtualSiteProperties("Help", body);
    assertEquals("https://git.example.com/org/docs.git", out.getRemoteUrl());
    assertEquals("main", out.getBranch());
  }

  @Test
  void updateVirtualSiteProperties_rejectsUnsafeRemote() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("Help");
    existing.setGUID(siteGuid);
    PSSite modifiable = new PSSite();
    modifiable.setName("Help");
    modifiable.setGUID(siteGuid);
    when(siteManager.findSite("Help")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);
    IPSPublishingContext preview = mock(IPSPublishingContext.class);
    when(preview.getGUID()).thenReturn(previewCtx);
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT)).thenReturn(preview);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("git-filesystem");
    body.setRemoteUrl("http://evil.example/../x");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateVirtualSiteProperties("Help", body));
    assertEquals(400, ex.getResponse().getStatus());
    verify(siteManager, never()).saveSite(any());
  }

  @Test
  void updateVirtualSiteProperties_clearToRepository() throws Exception {
    PSSite existing = new PSSite();
    existing.setName("Help");
    existing.setGUID(siteGuid);

    PSSite modifiable = new PSSite();
    modifiable.setName("Help");
    modifiable.setGUID(siteGuid);
    put(modifiable, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(modifiable, PSVirtualSiteHelper.PROP_ROOT_PATH, "C:/docs");

    when(siteManager.findSite("Help")).thenReturn(existing);
    when(siteManager.loadSiteModifiable(siteGuid)).thenReturn(modifiable);

    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("repository");

    VirtualSiteProperties out = adaptor.updateVirtualSiteProperties("Help", body);
    assertFalse(Boolean.TRUE.equals(out.getVirtual()));
    verify(siteManager).saveSite(eq(modifiable));
    assertFalse(PSVirtualSiteHelper.isVirtual(modifiable));
  }

  @Test
  void resolvePropertyContext_reusesExisting() throws Exception {
    PSSite site = new PSSite();
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    IPSGuid ctx = adaptor.resolvePropertyContext(site);
    assertEquals(previewCtx, ctx);
    verify(siteManager, never()).loadContext(any(String.class));
  }

  @Test
  void resolvePropertyContext_fallsBackToFirstWhenNoPreview() throws Exception {
    PSSite site = new PSSite();
    when(siteManager.loadContext(SitesAdaptor.DEFAULT_PROPERTY_CONTEXT))
        .thenThrow(new PSNotFoundException("no preview"));
    IPSPublishingContext other = mock(IPSPublishingContext.class);
    IPSGuid otherId = new PSGuid(PSTypeEnum.CONTEXT, 9);
    when(other.getGUID()).thenReturn(otherId);
    when(siteManager.findAllContexts()).thenReturn(List.of(other));

    assertEquals(otherId, adaptor.resolvePropertyContext(site));
  }

  @Test
  void buildVirtualSite_rejectsRepositorySite() {
    PSSite site = new PSSite();
    site.setName("Corp");
    site.setGUID(siteGuid);
    when(siteManager.findSite("Corp")).thenReturn(site);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("Corp", null));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("not a virtual"));
  }

  @Test
  void buildVirtualSite_forbiddenWhenNotAdmin() {
    SitesAdaptor denied =
        new SitesAdaptor(siteManager, () -> false, SitesAdaptor::defaultOutputRootForSiteKey, null);
    // Admin gate runs before site load — no siteManager stub required.

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> denied.buildVirtualSite("Help", null));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void buildVirtualSite_rejectsMissingRootDirectory() {
    PSSite site = new PSSite();
    site.setName("Help");
    site.setGUID(siteGuid);
    Path missing = tempDir.resolve("does-not-exist");
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, missing.toString());
    when(siteManager.findSite("Help")).thenReturn(site);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("Help", null));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains(PSVirtualSiteHelper.PROP_ROOT_PATH));
  }

  @Test
  void buildVirtualSite_runsBuildAndMapsResult() throws Exception {
    Path siteRoot = createMinimalVirtualTree(tempDir.resolve("src"));
    Path out = tempDir.resolve("out");

    PSSite site = new PSSite();
    site.setName("Help");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "sample");
    when(siteManager.findSite("Help")).thenReturn(site);

    SitesAdaptor.BuildRunner runner =
        (config, outputRoot) ->
            new PSVirtualSiteBuildResult(
                outputRoot, 2, List.of(), List.of("8.2/index.html", "link-report.txt"));

    SitesAdaptor building =
        new SitesAdaptor(
            siteManager, () -> true, key -> out, runner);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toString());

    VirtualSiteBuildResult result = building.buildVirtualSite("Help", req);
    assertEquals("Help", result.getSiteName());
    assertEquals("sample", result.getSiteKey());
    assertEquals(2, result.getPagesWritten().intValue());
    assertEquals(0, result.getLinkProblemCount().intValue());
    assertFalse(Boolean.TRUE.equals(result.getHasLinkProblems()));
    assertTrue(result.getOutputPath() != null && !result.getOutputPath().isBlank());
    assertTrue(result.getWrittenFiles().contains("link-report.txt"));
  }

  @Test
  void buildVirtualSite_realFilesystemBuild() throws Exception {
    Path siteRoot = createMinimalVirtualTree(tempDir.resolve("real-src"));
    Path out = tempDir.resolve("real-out");

    PSSite site = new PSSite();
    site.setName("Help");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "help-docs");
    when(siteManager.findSite("Help")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    VirtualSiteBuildResult result = adaptor.buildVirtualSite("Help", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertFalse(Boolean.TRUE.equals(result.getHasLinkProblems()));
    assertTrue(Files.isRegularFile(out.resolve("8.2").resolve("index.html")));
    assertTrue(Files.isRegularFile(out.resolve("link-report.txt")));
  }

  @Test
  void buildVirtualSite_remoteCheckoutThenDiscover() throws Exception {
    Path out = tempDir.resolve("remote-out");
    PSGitRemoteCheckout remote =
        new PSGitRemoteCheckout(
            (cwd, command, output) -> {
              if (command.contains("clone")) {
                Path dest = Path.of(command.get(command.size() - 1));
                try {
                  createMinimalVirtualTree(dest);
                } catch (IOException e) {
                  throw e;
                } catch (Exception e) {
                  throw new IOException(e);
                }
                Files.createDirectories(dest.resolve(".git"));
              }
              return 0;
            },
            Duration.ofSeconds(10));

    PSSite site = new PSSite();
    site.setName("Help");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(site, PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git");
    put(site, PSVirtualSiteHelper.PROP_BRANCH, "main");
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "help-docs");
    when(siteManager.findSite("Help")).thenReturn(site);

    SitesAdaptor building =
        new SitesAdaptor(
            siteManager, () -> true, key -> out, null, remote);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    VirtualSiteBuildResult result = building.buildVirtualSite("Help", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(Files.isRegularFile(out.resolve("8.2").resolve("index.html")));
  }

  @Test
  void buildVirtualSite_csvFilesystemWritesHtml() throws Exception {
    Path siteRoot = createMinimalCsvTree(tempDir.resolve("csv-src"));
    Path out = tempDir.resolve("csv-out");

    PSSite site = new PSSite();
    site.setName("CsvHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "csv-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "csv-docs");
    when(siteManager.findSite("CsvHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    VirtualSiteBuildResult result = adaptor.buildVirtualSite("CsvHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertFalse(Boolean.TRUE.equals(result.getHasLinkProblems()));
    Path html = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("CSV Home"), body);
    assertTrue(body.contains("Hello from CSV"), body);
  }

  @Test
  void buildVirtualSite_csvFilesystemOptionalConfigYaml() throws Exception {
    Path siteRoot = tempDir.resolve("csv-noconfig");
    Files.createDirectories(siteRoot.resolve("8.2"));
    Files.writeString(
        siteRoot.resolve("8.2").resolve("pages.csv"),
        "id,title,body,path,order\nhome,No Config,Body from CSV.,index.md,1\n",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("csv-noconfig-out");

    PSSite site = new PSSite();
    site.setName("CsvHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "csv-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "csv-docs");
    when(siteManager.findSite("CsvHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    VirtualSiteBuildResult result = adaptor.buildVirtualSite("CsvHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(Files.isRegularFile(out.resolve("8.2").resolve("index.html")));
  }

  @Test
  void buildVirtualSite_csvFilesystemMissingColumn400() throws Exception {
    Path siteRoot = tempDir.resolve("csv-missing-col");
    Files.createDirectories(siteRoot.resolve("8.2"));
    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: CSV
        versions:
          - id: "8.2"
            label: "8.2"
            path: 8.2
            default: true
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        siteRoot.resolve("8.2").resolve("pages.csv"),
        "id,title\nx,Y\n",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("csv-missing-col-out");

    PSSite site = new PSSite();
    site.setName("CsvHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "csv-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    when(siteManager.findSite("CsvHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("CsvHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("body"), String.valueOf(ex.getMessage()));
  }

  @Test
  void buildVirtualSite_csvFilesystemUnsafePath400() throws Exception {
    Path siteRoot = tempDir.resolve("csv-unsafe");
    Files.createDirectories(siteRoot.resolve("8.2"));
    Files.writeString(
        siteRoot.resolve("8.2").resolve("pages.csv"),
        "id,title,body,path\nesc,Esc,body,../outside.md\n",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("csv-unsafe-out");

    PSSite site = new PSSite();
    site.setName("CsvHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "csv-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    when(siteManager.findSite("CsvHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("CsvHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(
        String.valueOf(ex.getMessage()).toLowerCase().contains("path"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  void buildVirtualSite_sqlDatabaseWritesHtml() throws Exception {
    Path siteRoot = createMinimalSqlTree(tempDir.resolve("sql-src"));
    Path out = tempDir.resolve("sql-out");

    PSSite site = new PSSite();
    site.setName("SqlHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "sql-docs");
    when(siteManager.findSite("SqlHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    VirtualSiteBuildResult result = adaptor.buildVirtualSite("SqlHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertFalse(Boolean.TRUE.equals(result.getHasLinkProblems()));
    Path html = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("SQL Home"), body);
    assertTrue(body.contains("Hello from SQL"), body);
    assertFalse(body.contains("secret-sql-password-should-not-leak"), body);
    assertFalse(body.toLowerCase().contains("password"), body);
  }

  @Test
  void buildVirtualSite_sqlDatabaseQueryFileWritesHtml() throws Exception {
    Path siteRoot =
        createMinimalSqlTree(
            tempDir.resolve("sql-qfile"),
            "SELECT id, title, body, path, sort_order AS \"order\" FROM pages",
            Path.of("queries").resolve("pages.sql"));
    Path out = tempDir.resolve("sql-qfile-out");

    PSSite site = new PSSite();
    site.setName("SqlHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "sql-docs");
    when(siteManager.findSite("SqlHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    VirtualSiteBuildResult result = adaptor.buildVirtualSite("SqlHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    Path html = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("SQL Home"), body);
    assertTrue(body.contains("Hello from SQL"), body);
  }

  @Test
  void buildVirtualSite_sqlDatabaseMissingColumn400() throws Exception {
    Path siteRoot =
        createMinimalSqlTree(tempDir.resolve("sql-missing-col"), "SELECT id, title FROM pages", null);
    Path out = tempDir.resolve("sql-missing-col-out");

    PSSite site = new PSSite();
    site.setName("SqlHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    when(siteManager.findSite("SqlHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("SqlHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("body"), String.valueOf(ex.getMessage()));
  }

  @Test
  void buildVirtualSite_sqlDatabaseOracleAndMysqlUrl400() throws Exception {
    Path siteRoot = tempDir.resolve("sql-oracle");
    Files.createDirectories(siteRoot);
    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: SQL Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        sql:
          jdbcUrl: jdbc:oracle:thin:@localhost:1521:orcl
          user: cms
          query: SELECT id, title, body FROM pages
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("sql-oracle-out");

    PSSite site = new PSSite();
    site.setName("SqlHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    when(siteManager.findSite("SqlHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException oracle =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("SqlHelp", req));
    assertEquals(400, oracle.getResponse().getStatus());
    String oracleMsg = String.valueOf(oracle.getMessage()).toLowerCase();
    assertTrue(
        oracleMsg.contains("unsupported") || oracleMsg.contains("not allowed"),
        String.valueOf(oracle.getMessage()));

    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: SQL Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        sql:
          jdbcUrl: jdbc:mysql://localhost:3306/cms
          user: cms
          query: SELECT id, title, body FROM pages
        """,
        StandardCharsets.UTF_8);
    WebApplicationException mysql =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("SqlHelp", req));
    assertEquals(400, mysql.getResponse().getStatus());
  }

  @Test
  void buildVirtualSite_sqlDatabaseMissingConfig400() throws Exception {
    Path siteRoot = tempDir.resolve("sql-noconfig");
    Files.createDirectories(siteRoot);
    Path out = tempDir.resolve("sql-noconfig-out");

    PSSite site = new PSSite();
    site.setName("SqlHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    when(siteManager.findSite("SqlHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("SqlHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    String missingMsg = String.valueOf(ex.getMessage()).toLowerCase();
    assertTrue(
        missingMsg.contains("config") && missingMsg.contains("_config.yaml"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  void buildVirtualSite_httpJsonWritesHtml() throws Exception {
    Path siteRoot = createMinimalHttpJsonTree(tempDir.resolve("http-src"));
    Path out = tempDir.resolve("http-out");

    PSSite site = new PSSite();
    site.setName("HttpHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "http-docs");
    when(siteManager.findSite("HttpHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    VirtualSiteBuildResult result = adaptor.buildVirtualSite("HttpHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().normalize().toString(), result.getOutputPath());
    assertFalse(Boolean.TRUE.equals(result.getHasLinkProblems()));
    Path html = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("HTTP Home"), body);
    assertTrue(body.contains("Hello from JSON"), body);
  }

  @Test
  void buildVirtualSite_httpJsonLoopbackWritesHtml() throws Exception {
    String catalog =
        """
        {"pages":[{"id":"loop-home","path":"index.html","title":"Loopback Home","body":"Hello from loopback.","order":1}]}
        """;
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      byte[] bytes = catalog.getBytes(StandardCharsets.UTF_8);
      server.createContext(
          "/pages.json",
          exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
              os.write(bytes);
            }
          });
      server.start();
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/pages.json";
      Path siteRoot = createMinimalHttpJsonTree(tempDir.resolve("http-loop"), url);
      Path out = tempDir.resolve("http-loop-out");

      PSSite site = new PSSite();
      site.setName("HttpHelp");
      site.setGUID(siteGuid);
      put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json");
      put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
      put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "http-docs");
      when(siteManager.findSite("HttpHelp")).thenReturn(site);

      VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
      req.setOutputRoot(out.toAbsolutePath().toString());

      VirtualSiteBuildResult result = adaptor.buildVirtualSite("HttpHelp", req);
      assertEquals(1, result.getPagesWritten().intValue());
      Path html = out.resolve("8.2").resolve("index.html");
      assertTrue(Files.isRegularFile(html), "missing " + html);
      String body = Files.readString(html, StandardCharsets.UTF_8);
      assertTrue(body.contains("Loopback Home"), body);
      assertTrue(body.contains("Hello from loopback"), body);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void buildVirtualSite_httpJsonMissingId400() throws Exception {
    Path siteRoot = createMinimalHttpJsonTree(tempDir.resolve("http-missing-id"));
    Files.writeString(
        siteRoot.resolve("pages.json"),
        """
        {"pages":[{"id":"","title":"MissingId","body":"x"}]}
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("http-missing-id-out");

    PSSite site = new PSSite();
    site.setName("HttpHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    when(siteManager.findSite("HttpHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("HttpHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).contains("id"), String.valueOf(ex.getMessage()));
  }

  @Test
  void buildVirtualSite_httpJsonMissingConfig400() throws Exception {
    Path siteRoot = tempDir.resolve("http-noconfig");
    Files.createDirectories(siteRoot);
    Path out = tempDir.resolve("http-noconfig-out");

    PSSite site = new PSSite();
    site.setName("HttpHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    when(siteManager.findSite("HttpHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("HttpHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    String missingMsg = String.valueOf(ex.getMessage()).toLowerCase();
    assertTrue(
        missingMsg.contains("config") && missingMsg.contains("_config.yaml"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  void buildVirtualSite_httpJsonRemoteUrl400() throws Exception {
    Path siteRoot = createMinimalHttpJsonTree(tempDir.resolve("http-remote"));
    Path out = tempDir.resolve("http-remote-out");

    PSSite site = new PSSite();
    site.setName("HttpHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git");
    when(siteManager.findSite("HttpHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("HttpHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(
        String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  void buildVirtualSite_objectStorageWritesHtml() throws Exception {
    Path siteRoot = createMinimalObjectStorageTree(tempDir.resolve("obj-src"));
    Path out = tempDir.resolve("obj-out");

    PSSite site = new PSSite();
    site.setName("ObjectHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "obj-docs");
    when(siteManager.findSite("ObjectHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    VirtualSiteBuildResult result = adaptor.buildVirtualSite("ObjectHelp", req);
    assertTrue(result.getPagesWritten().intValue() > 0, "pagesWritten=" + result.getPagesWritten());
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().normalize().toString(), result.getOutputPath());
    assertFalse(Boolean.TRUE.equals(result.getHasLinkProblems()));
    Path html = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("Object Home"), body);
    assertTrue(body.contains("Hello from objects"), body);
  }

  @Test
  void buildVirtualSite_objectStorageMissingConfig400() throws Exception {
    Path siteRoot = tempDir.resolve("obj-noconfig");
    Files.createDirectories(siteRoot);
    Path out = tempDir.resolve("obj-noconfig-out");

    PSSite site = new PSSite();
    site.setName("ObjectHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    when(siteManager.findSite("ObjectHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("ObjectHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    String missingMsg = String.valueOf(ex.getMessage()).toLowerCase();
    assertTrue(
        missingMsg.contains("config") && missingMsg.contains("_config.yaml"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  void buildVirtualSite_objectStorageRemoteUrl400() throws Exception {
    Path siteRoot = createMinimalObjectStorageTree(tempDir.resolve("obj-remote"));
    Path out = tempDir.resolve("obj-remote-out");

    PSSite site = new PSSite();
    site.setName("ObjectHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git");
    when(siteManager.findSite("ObjectHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("ObjectHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(
        String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  void buildVirtualSite_rssAtomWritesHtml() throws Exception {
    Path siteRoot = createMinimalRssAtomTree(tempDir.resolve("rss-src"));
    Path out = tempDir.resolve("rss-out");

    PSSite site = new PSSite();
    site.setName("RssHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "rss-docs");
    when(siteManager.findSite("RssHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    VirtualSiteBuildResult result = adaptor.buildVirtualSite("RssHelp", req);
    assertTrue(result.getPagesWritten().intValue() > 0, "pagesWritten=" + result.getPagesWritten());
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().normalize().toString(), result.getOutputPath());
    assertFalse(Boolean.TRUE.equals(result.getHasLinkProblems()));
    Path html = out.resolve("8.2").resolve("home.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("RSS Home"), body);
    assertTrue(body.contains("Hello from RSS"), body);
  }

  @Test
  void buildVirtualSite_rssAtomMissingFeed400() throws Exception {
    Path siteRoot = createMinimalRssAtomTree(tempDir.resolve("rss-nofeed"));
    Files.deleteIfExists(siteRoot.resolve("feed.xml"));
    Path out = tempDir.resolve("rss-nofeed-out");

    PSSite site = new PSSite();
    site.setName("RssHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    when(siteManager.findSite("RssHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("RssHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    String missingMsg = String.valueOf(ex.getMessage()).toLowerCase();
    assertTrue(
        missingMsg.contains("feed") || missingMsg.contains("rss-atom"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  void buildVirtualSite_rssAtomMissingConfig400() throws Exception {
    Path siteRoot = tempDir.resolve("rss-noconfig");
    Files.createDirectories(siteRoot);
    Path out = tempDir.resolve("rss-noconfig-out");

    PSSite site = new PSSite();
    site.setName("RssHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    when(siteManager.findSite("RssHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("RssHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    String missingMsg = String.valueOf(ex.getMessage()).toLowerCase();
    assertTrue(
        missingMsg.contains("config") && missingMsg.contains("_config.yaml"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  void buildVirtualSite_rssAtomUnsafePath400() {
    Path out = tempDir.resolve("rss-unsafe-out");
    PSSite site = new PSSite();
    site.setName("RssHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, Path.of("a", "..", "..", "etc").toString());
    when(siteManager.findSite("RssHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("RssHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    String msg = String.valueOf(ex.getMessage());
    assertTrue(
        msg.contains("virtual.rootPath") || msg.toLowerCase().contains("unsafe"), msg);
  }

  @Test
  void buildVirtualSite_rssAtomRemoteUrl400() throws Exception {
    Path siteRoot = createMinimalRssAtomTree(tempDir.resolve("rss-remote"));
    Path out = tempDir.resolve("rss-remote-out");

    PSSite site = new PSSite();
    site.setName("RssHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git");
    when(siteManager.findSite("RssHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("RssHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(
        String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  void buildVirtualSite_rssAtomCloudRootPath400() {
    Path out = tempDir.resolve("rss-cloud-out");
    PSSite site = new PSSite();
    site.setName("RssHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, "https://feeds.example.com/blog.xml");
    when(siteManager.findSite("RssHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("RssHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    String msg = String.valueOf(ex.getMessage());
    assertTrue(msg.contains("virtual.rootPath"), msg);
    assertTrue(msg.toLowerCase().contains("cloud"), msg);
  }

  @Test
  void buildVirtualSite_rssAtomCredentialProperty400() throws Exception {
    Path siteRoot = createMinimalRssAtomTree(tempDir.resolve("rss-cred"));
    Path out = tempDir.resolve("rss-cred-out");

    PSSite site = new PSSite();
    site.setName("RssHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "rss-atom");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, "aws_secret_access_key", "not-a-real-secret");
    when(siteManager.findSite("RssHelp")).thenReturn(site);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("RssHelp", req));
    assertEquals(400, ex.getResponse().getStatus());
    String msg = String.valueOf(ex.getMessage());
    assertTrue(msg.toLowerCase().contains("credential"), msg);
    assertFalse(msg.contains("not-a-real-secret"), msg);
  }

  @Test
  void buildVirtualSite_unknownSourceKind400() {
    Path siteRoot = tempDir.resolve("sql-root");
    PSSite site = new PSSite();
    site.setName("Help");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-api");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toString());
    when(siteManager.findSite("Help")).thenReturn(site);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.buildVirtualSite("Help", null));
    assertEquals(400, ex.getResponse().getStatus());
    String msg = String.valueOf(ex.getMessage()).toLowerCase();
    assertTrue(msg.contains("unsupported") || msg.contains("sql-api"), msg);
  }

  @Test
  void publishVirtualSite_rejectsRepositorySite() {
    PSSite site = new PSSite();
    site.setName("Corp");
    site.setGUID(siteGuid);
    site.setRoot(tempDir.resolve("pub").toString());
    when(siteManager.findSite("Corp")).thenReturn(site);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.publishVirtualSite("Corp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("not a virtual"));
  }

  @Test
  void publishVirtualSite_rejectsMissingSiteRoot() {
    Path siteRoot = tempDir.resolve("src-no-root");
    PSSite site = new PSSite();
    site.setName("Help");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toString());
    when(siteManager.findSite("Help")).thenReturn(site);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.publishVirtualSite("Help"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(String.valueOf(ex.getMessage()).toLowerCase().contains("not configured"));
  }

  @Test
  void publishVirtualSite_forbiddenWhenNotAdmin() {
    SitesAdaptor denied =
        new SitesAdaptor(siteManager, () -> false, SitesAdaptor::defaultOutputRootForSiteKey, null);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> denied.publishVirtualSite("Help"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void publishVirtualSite_csvFilesystemInjectedBuildRunnerCopiesToSiteRoot() throws Exception {
    Path siteRoot = createMinimalCsvTree(tempDir.resolve("csv-pub-src"));
    Path staging = tempDir.resolve("csv-pub-staging");
    Path publishTo = tempDir.resolve("csv-pub-target");

    PSSite site = new PSSite();
    site.setName("CsvHelp");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "csv-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "csv-docs");
    when(siteManager.findSite("CsvHelp")).thenReturn(site);

    SitesAdaptor.BuildRunner runner =
        (config, outputRoot) -> {
          Files.createDirectories(outputRoot.resolve("8.2"));
          Files.writeString(
              outputRoot.resolve("8.2").resolve("index.html"),
              "<html>CSV published</html>",
              StandardCharsets.UTF_8);
          Files.createDirectories(outputRoot.resolve("_meta"));
          Files.writeString(
              outputRoot.resolve("_meta").resolve("skip.txt"),
              "not published",
              StandardCharsets.UTF_8);
          return new PSVirtualSiteBuildResult(
              outputRoot, 1, List.of(), List.of("8.2/index.html"));
        };

    SitesAdaptor publishing =
        new SitesAdaptor(siteManager, () -> true, key -> staging, runner);

    VirtualSitePublishResult result = publishing.publishVirtualSite("CsvHelp");
    assertEquals("CsvHelp", result.getSiteName());
    assertEquals("csv-docs", result.getSiteKey());
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getFilesCopied() >= 1);
    Path html = publishTo.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    assertTrue(
        Files.readString(html, StandardCharsets.UTF_8).contains("CSV published"),
        Files.readString(html, StandardCharsets.UTF_8));
    assertFalse(Files.exists(publishTo.resolve("_meta")));
    assertTrue(result.getPublishPath() != null && !result.getPublishPath().isBlank());
    assertTrue(result.getPublishPath() != null && !result.getPublishPath().isBlank());
  }

  @Test
  void publishVirtualSite_csvFilesystemBuildsThenCopiesToSiteRoot() throws Exception {
    Path siteRoot = createMinimalCsvTree(tempDir.resolve("csv-real-pub-src"));
    Path staging = tempDir.resolve("csv-real-pub-staging");
    Path publishTo = tempDir.resolve("csv-real-pub-target");

    PSSite site = new PSSite();
    site.setName("CsvHelp");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "csv-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "csv-docs");
    when(siteManager.findSite("CsvHelp")).thenReturn(site);

    SitesAdaptor publishing =
        new SitesAdaptor(siteManager, () -> true, key -> staging, null);

    VirtualSitePublishResult result = publishing.publishVirtualSite("CsvHelp");
    assertEquals("CsvHelp", result.getSiteName());
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getFilesCopied() >= 1);
    Path html = publishTo.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("CSV Home"), body);
    assertTrue(body.contains("Hello from CSV"), body);
    assertFalse(Files.exists(publishTo.resolve("_meta")));
  }

  @Test
  void publishVirtualSite_csvFilesystemRejectsUnsafeSiteRoot() {
    PSSite site = new PSSite();
    site.setName("CsvHelp");
    site.setGUID(siteGuid);
    site.setRoot(Path.of("a", "..", "..", "etc").toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "csv-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, tempDir.resolve("csv-src").toString());
    when(siteManager.findSite("CsvHelp")).thenReturn(site);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.publishVirtualSite("CsvHelp"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void publishVirtualSite_buildsThenCopiesToSiteRoot() throws Exception {
    Path siteRoot = createMinimalVirtualTree(tempDir.resolve("pub-src"));
    Path staging = tempDir.resolve("pub-staging");
    Path publishTo = tempDir.resolve("pub-target");

    PSSite site = new PSSite();
    site.setName("Help");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "help-docs");
    when(siteManager.findSite("Help")).thenReturn(site);

    SitesAdaptor publishing =
        new SitesAdaptor(siteManager, () -> true, key -> staging, null);

    VirtualSitePublishResult result = publishing.publishVirtualSite("Help");
    assertEquals("Help", result.getSiteName());
    assertEquals("help-docs", result.getSiteKey());
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getFilesCopied() >= 1);
    assertFalse(Boolean.TRUE.equals(result.getHasLinkProblems()));
    assertTrue(Files.isRegularFile(publishTo.resolve("8.2").resolve("index.html")));
    assertTrue(Files.isRegularFile(staging.resolve("8.2").resolve("index.html")));
    assertFalse(Files.exists(publishTo.resolve("_meta")));
    assertTrue(result.getPublishPath() != null && !result.getPublishPath().isBlank());
  }

  @Test
  void publishVirtualSite_sqlDatabaseBuildsThenCopiesToSiteRoot() throws Exception {
    Path siteRoot = createMinimalSqlTree(tempDir.resolve("sql-real-pub-src"));
    Path staging = tempDir.resolve("sql-real-pub-staging");
    Path publishTo = tempDir.resolve("sql-real-pub-target");

    PSSite site = new PSSite();
    site.setName("SqlHelp");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "sql-docs");
    when(siteManager.findSite("SqlHelp")).thenReturn(site);

    SitesAdaptor publishing =
        new SitesAdaptor(siteManager, () -> true, key -> staging, null);

    VirtualSitePublishResult result = publishing.publishVirtualSite("SqlHelp");
    assertEquals("SqlHelp", result.getSiteName());
    assertEquals("sql-docs", result.getSiteKey());
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getFilesCopied() >= 1);
    Path html = publishTo.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("SQL Home"), body);
    assertTrue(body.contains("Hello from SQL"), body);
    assertFalse(body.contains("secret-sql-password-should-not-leak"), body);
    assertFalse(Files.exists(publishTo.resolve("_meta")));
    assertTrue(Files.isRegularFile(staging.resolve("8.2").resolve("index.html")));
    assertTrue(result.getPublishPath() != null && !result.getPublishPath().isBlank());
  }

  @Test
  void publishVirtualSite_sqlDatabaseInjectedBuildRunnerCopiesToSiteRoot() throws Exception {
    Path siteRoot = createMinimalSqlTree(tempDir.resolve("sql-pub-src"));
    Path staging = tempDir.resolve("sql-pub-staging");
    Path publishTo = tempDir.resolve("sql-pub-target");

    PSSite site = new PSSite();
    site.setName("SqlHelp");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "sql-docs");
    when(siteManager.findSite("SqlHelp")).thenReturn(site);

    SitesAdaptor.BuildRunner runner =
        (config, outputRoot) -> {
          Files.createDirectories(outputRoot.resolve("8.2"));
          Files.writeString(
              outputRoot.resolve("8.2").resolve("index.html"),
              "<html>SQL published</html>",
              StandardCharsets.UTF_8);
          Files.createDirectories(outputRoot.resolve("_meta"));
          Files.writeString(
              outputRoot.resolve("_meta").resolve("skip.txt"),
              "not published",
              StandardCharsets.UTF_8);
          return new PSVirtualSiteBuildResult(
              outputRoot, 1, List.of(), List.of("8.2/index.html"));
        };

    SitesAdaptor publishing =
        new SitesAdaptor(siteManager, () -> true, key -> staging, runner);

    VirtualSitePublishResult result = publishing.publishVirtualSite("SqlHelp");
    assertEquals("SqlHelp", result.getSiteName());
    assertEquals("sql-docs", result.getSiteKey());
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getFilesCopied() >= 1);
    Path html = publishTo.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    assertTrue(
        Files.readString(html, StandardCharsets.UTF_8).contains("SQL published"),
        Files.readString(html, StandardCharsets.UTF_8));
    assertFalse(Files.exists(publishTo.resolve("_meta")));
    assertTrue(result.getPublishPath() != null && !result.getPublishPath().isBlank());
  }

  @Test
  void publishVirtualSite_sqlDatabaseQueryFileBuildsThenCopiesToSiteRoot() throws Exception {
    Path siteRoot =
        createMinimalSqlTree(
            tempDir.resolve("sql-qfile-pub-src"),
            "SELECT id, title, body, path, sort_order AS \"order\" FROM pages",
            Path.of("queries").resolve("pages.sql"));
    Path staging = tempDir.resolve("sql-qfile-pub-staging");
    Path publishTo = tempDir.resolve("sql-qfile-pub-target");

    PSSite site = new PSSite();
    site.setName("SqlHelp");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "sql-docs");
    when(siteManager.findSite("SqlHelp")).thenReturn(site);

    SitesAdaptor publishing =
        new SitesAdaptor(siteManager, () -> true, key -> staging, null);

    VirtualSitePublishResult result = publishing.publishVirtualSite("SqlHelp");
    assertEquals("SqlHelp", result.getSiteName());
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getFilesCopied() >= 1);
    Path html = publishTo.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("SQL Home"), body);
    assertTrue(body.contains("Hello from SQL"), body);
    assertFalse(body.contains("secret-sql-password-should-not-leak"), body);
    assertFalse(Files.exists(publishTo.resolve("_meta")));
  }

  @Test
  void publishVirtualSite_sqlDatabaseRejectsUnsafeSiteRoot() {
    PSSite site = new PSSite();
    site.setName("SqlHelp");
    site.setGUID(siteGuid);
    site.setRoot(Path.of("a", "..", "..", "etc").toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, tempDir.resolve("sql-src").toString());
    when(siteManager.findSite("SqlHelp")).thenReturn(site);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.publishVirtualSite("SqlHelp"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void publishVirtualSite_sqlDatabaseOracleAndMysqlUrl400() throws Exception {
    Path siteRoot = tempDir.resolve("sql-oracle-pub");
    Files.createDirectories(siteRoot);
    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: SQL Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        sql:
          jdbcUrl: jdbc:oracle:thin:@localhost:1521:orcl
          user: cms
          query: SELECT id, title, body FROM pages
        """,
        StandardCharsets.UTF_8);
    Path publishTo = tempDir.resolve("sql-oracle-pub-target");

    PSSite site = new PSSite();
    site.setName("SqlHelp");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    when(siteManager.findSite("SqlHelp")).thenReturn(site);

    WebApplicationException oracle =
        assertThrows(
            WebApplicationException.class, () -> adaptor.publishVirtualSite("SqlHelp"));
    assertEquals(400, oracle.getResponse().getStatus());
    String oracleMsg = String.valueOf(oracle.getMessage()).toLowerCase();
    assertTrue(
        oracleMsg.contains("h2")
            || oracleMsg.contains("unsupported")
            || oracleMsg.contains("not allowed")
            || oracleMsg.contains("jdbc"),
        String.valueOf(oracle.getMessage()));

    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: SQL Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        sql:
          jdbcUrl: jdbc:mysql://localhost:3306/cms
          user: cms
          query: SELECT id, title, body FROM pages
        """,
        StandardCharsets.UTF_8);
    WebApplicationException mysql =
        assertThrows(
            WebApplicationException.class, () -> adaptor.publishVirtualSite("SqlHelp"));
    assertEquals(400, mysql.getResponse().getStatus());
  }

  @Test
  void publishVirtualSite_httpJsonInjectedBuildRunnerCopiesToSiteRoot() throws Exception {
    Path siteRoot = createMinimalHttpJsonTree(tempDir.resolve("http-pub-src"));
    Path staging = tempDir.resolve("http-pub-staging");
    Path publishTo = tempDir.resolve("http-pub-target");

    PSSite site = new PSSite();
    site.setName("HttpHelp");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "http-docs");
    when(siteManager.findSite("HttpHelp")).thenReturn(site);

    SitesAdaptor.BuildRunner runner =
        (config, outputRoot) -> {
          Files.createDirectories(outputRoot.resolve("8.2"));
          Files.writeString(
              outputRoot.resolve("8.2").resolve("index.html"),
              "<html>HTTP JSON published</html>",
              StandardCharsets.UTF_8);
          Files.createDirectories(outputRoot.resolve("_meta"));
          Files.writeString(
              outputRoot.resolve("_meta").resolve("skip.txt"),
              "not published",
              StandardCharsets.UTF_8);
          return new PSVirtualSiteBuildResult(
              outputRoot, 1, List.of(), List.of("8.2/index.html"));
        };

    SitesAdaptor publishing =
        new SitesAdaptor(siteManager, () -> true, key -> staging, runner);

    VirtualSitePublishResult result = publishing.publishVirtualSite("HttpHelp");
    assertEquals("HttpHelp", result.getSiteName());
    assertEquals("http-docs", result.getSiteKey());
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getFilesCopied() >= 1);
    Path html = publishTo.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    assertTrue(
        Files.readString(html, StandardCharsets.UTF_8).contains("HTTP JSON published"),
        Files.readString(html, StandardCharsets.UTF_8));
    assertFalse(Files.exists(publishTo.resolve("_meta")));
    assertTrue(result.getPublishPath() != null && !result.getPublishPath().isBlank());
  }

  @Test
  void publishVirtualSite_httpJsonBuildsThenCopiesToSiteRoot() throws Exception {
    Path siteRoot = createMinimalHttpJsonTree(tempDir.resolve("http-real-pub-src"));
    Path staging = tempDir.resolve("http-real-pub-staging");
    Path publishTo = tempDir.resolve("http-real-pub-target");

    PSSite site = new PSSite();
    site.setName("HttpHelp");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "http-docs");
    when(siteManager.findSite("HttpHelp")).thenReturn(site);

    SitesAdaptor publishing =
        new SitesAdaptor(siteManager, () -> true, key -> staging, null);

    VirtualSitePublishResult result = publishing.publishVirtualSite("HttpHelp");
    assertEquals("HttpHelp", result.getSiteName());
    assertEquals("http-docs", result.getSiteKey());
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getFilesCopied() >= 1);
    Path html = publishTo.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("HTTP Home"), body);
    assertTrue(body.contains("Hello from JSON"), body);
    assertFalse(Files.exists(publishTo.resolve("_meta")));
    assertTrue(Files.isRegularFile(staging.resolve("8.2").resolve("index.html")));
    assertTrue(result.getPublishPath() != null && !result.getPublishPath().isBlank());
  }

  @Test
  void publishVirtualSite_httpJsonRejectsUnsafeSiteRoot() {
    PSSite site = new PSSite();
    site.setName("HttpHelp");
    site.setGUID(siteGuid);
    site.setRoot(Path.of("a", "..", "..", "etc").toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, tempDir.resolve("http-src").toString());
    when(siteManager.findSite("HttpHelp")).thenReturn(site);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.publishVirtualSite("HttpHelp"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void publishVirtualSite_httpJsonRemoteUrl400() throws Exception {
    Path siteRoot = createMinimalHttpJsonTree(tempDir.resolve("http-pub-remote"));
    Path publishTo = tempDir.resolve("http-pub-remote-target");
    Files.createDirectories(publishTo);

    PSSite site = new PSSite();
    site.setName("HttpHelp");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git");
    when(siteManager.findSite("HttpHelp")).thenReturn(site);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.publishVirtualSite("HttpHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(
        String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  void publishVirtualSite_objectStorageInjectedBuildRunnerCopiesToSiteRoot() throws Exception {
    Path siteRoot = createMinimalObjectStorageTree(tempDir.resolve("obj-pub-src"));
    Path staging = tempDir.resolve("obj-pub-staging");
    Path publishTo = tempDir.resolve("obj-pub-target");

    PSSite site = new PSSite();
    site.setName("ObjHelp");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "obj-docs");
    when(siteManager.findSite("ObjHelp")).thenReturn(site);

    SitesAdaptor.BuildRunner runner =
        (config, outputRoot) -> {
          Files.createDirectories(outputRoot.resolve("8.2"));
          Files.writeString(
              outputRoot.resolve("8.2").resolve("index.html"),
              "<html>Object storage published</html>",
              StandardCharsets.UTF_8);
          Files.createDirectories(outputRoot.resolve("_meta"));
          Files.writeString(
              outputRoot.resolve("_meta").resolve("skip.txt"),
              "not published",
              StandardCharsets.UTF_8);
          return new PSVirtualSiteBuildResult(
              outputRoot, 1, List.of(), List.of("8.2/index.html"));
        };

    SitesAdaptor publishing =
        new SitesAdaptor(siteManager, () -> true, key -> staging, runner);

    VirtualSitePublishResult result = publishing.publishVirtualSite("ObjHelp");
    assertEquals("ObjHelp", result.getSiteName());
    assertEquals("obj-docs", result.getSiteKey());
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getFilesCopied() >= 1);
    Path html = publishTo.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    assertTrue(
        Files.readString(html, StandardCharsets.UTF_8).contains("Object storage published"),
        Files.readString(html, StandardCharsets.UTF_8));
    assertFalse(Files.exists(publishTo.resolve("_meta")));
    assertTrue(result.getPublishPath() != null && !result.getPublishPath().isBlank());
  }

  @Test
  void publishVirtualSite_objectStorageBuildsThenCopiesToSiteRoot() throws Exception {
    Path siteRoot = createMinimalObjectStorageTree(tempDir.resolve("obj-real-pub-src"));
    Path staging = tempDir.resolve("obj-real-pub-staging");
    Path publishTo = tempDir.resolve("obj-real-pub-target");

    PSSite site = new PSSite();
    site.setName("ObjHelp");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "obj-docs");
    when(siteManager.findSite("ObjHelp")).thenReturn(site);

    SitesAdaptor publishing =
        new SitesAdaptor(siteManager, () -> true, key -> staging, null);

    VirtualSitePublishResult result = publishing.publishVirtualSite("ObjHelp");
    assertEquals("ObjHelp", result.getSiteName());
    assertEquals("obj-docs", result.getSiteKey());
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getFilesCopied() >= 1);
    Path html = publishTo.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("Object Home"), body);
    assertTrue(body.contains("Hello from objects"), body);
    assertFalse(Files.exists(publishTo.resolve("_meta")));
    assertTrue(Files.isRegularFile(staging.resolve("8.2").resolve("index.html")));
    assertTrue(result.getPublishPath() != null && !result.getPublishPath().isBlank());
  }

  @Test
  void publishVirtualSite_objectStorageRejectsUnsafeSiteRoot() {
    PSSite site = new PSSite();
    site.setName("ObjHelp");
    site.setGUID(siteGuid);
    site.setRoot(Path.of("a", "..", "..", "etc").toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, tempDir.resolve("obj-src").toString());
    when(siteManager.findSite("ObjHelp")).thenReturn(site);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.publishVirtualSite("ObjHelp"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void publishVirtualSite_objectStorageRemoteUrl400() throws Exception {
    Path siteRoot = createMinimalObjectStorageTree(tempDir.resolve("obj-pub-remote"));
    Path publishTo = tempDir.resolve("obj-pub-remote-target");
    Files.createDirectories(publishTo);

    PSSite site = new PSSite();
    site.setName("ObjHelp");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toAbsolutePath().toString());
    put(site, PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/docs.git");
    when(siteManager.findSite("ObjHelp")).thenReturn(site);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.publishVirtualSite("ObjHelp"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(
        String.valueOf(ex.getMessage()).contains("virtual.remoteUrl"),
        String.valueOf(ex.getMessage()));
  }

  @Test
  void publishVirtualSite_unknownSourceKind400() {
    Path siteRoot = tempDir.resolve("unknown-pub-src");
    Path publishTo = tempDir.resolve("unknown-pub-target");
    PSSite site = new PSSite();
    site.setName("Help");
    site.setGUID(siteGuid);
    site.setRoot(publishTo.toString());
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-api");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, siteRoot.toString());
    when(siteManager.findSite("Help")).thenReturn(site);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.publishVirtualSite("Help"));
    assertEquals(400, ex.getResponse().getStatus());
    String msg = String.valueOf(ex.getMessage()).toLowerCase();
    assertTrue(msg.contains("unsupported") || msg.contains("sql-api"), msg);
  }

  @Test
  void toWirePublishResult_mapsRestDtoWithoutSystemCopyRecord() {
    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setOutputPath(tempDir.resolve("staging").toString());
    built.setPagesWritten(3);
    built.setLinkProblemCount(1);
    built.setHasLinkProblems(true);
    built.setLinkProblems(List.of("missing id:foo"));

    Path publishRoot = tempDir.resolve("published-root");
    VirtualSitePublishResult dto =
        SitesAdaptor.toWirePublishResult("Help", "help-docs", built, publishRoot, 7);

    assertEquals("Help", dto.getSiteName());
    assertEquals("help-docs", dto.getSiteKey());
    assertEquals(publishRoot.toAbsolutePath().normalize().toString(), dto.getPublishPath());
    assertEquals(built.getOutputPath(), dto.getBuildOutputPath());
    assertEquals(3, dto.getPagesWritten().intValue());
    assertEquals(7, dto.getFilesCopied().intValue());
    assertEquals(1, dto.getLinkProblemCount().intValue());
    assertTrue(Boolean.TRUE.equals(dto.getHasLinkProblems()));
    assertEquals(List.of("missing id:foo"), dto.getLinkProblems());
  }

  @Test
  void safePathSegment_stripsSeparators() {
    assertEquals("a_b_c", SitesAdaptor.safePathSegment("a/b\\c"));
    assertEquals("default", SitesAdaptor.safePathSegment(".."));
  }

  @Test
  void requireSafeOutputRoot_rejectsTraversalAndEmpty() {
    WebApplicationException parent =
        assertThrows(
            WebApplicationException.class,
            () -> SitesAdaptor.requireSafeOutputRoot(Path.of("a", "..", "..", "etc").normalize()));
    assertEquals(400, parent.getResponse().getStatus());

    WebApplicationException empty =
        assertThrows(
            WebApplicationException.class,
            () -> SitesAdaptor.requireSafeOutputRoot(Path.of("").normalize()));
    assertEquals(400, empty.getResponse().getStatus());
  }

  @Test
  void requireSafeOutputRoot_allowsNormalizedTempChild() {
    Path ok = tempDir.resolve("virtual-out").normalize();
    assertEquals(ok, SitesAdaptor.requireSafeOutputRoot(ok));
  }

  @Test
  void resolveOutputRoot_rejectsUnsafeOverride() {
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(Path.of("a", "..", "..", "secret").toString());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.resolveOutputRoot(req, "help"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void resolveOutputRoot_acceptsSafeOverride() {
    Path out = tempDir.resolve("safe-out").normalize();
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toString());
    assertEquals(out, adaptor.resolveOutputRoot(req, "help").normalize());
  }

  @Test
  void previewStatus_missingBuildIsUnavailableNot500() {
    Path defaultOut = tempDir.resolve("preview-default-empty");
    PSSite site = virtualHelpSite();
    when(siteManager.findSite("Help")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("Help");
    assertEquals(Boolean.FALSE, status.getAvailable());
    assertTrue(status.getMessage() != null && status.getMessage().contains("No assembled"));
  }

  @Test
  void previewStatus_findsVersionHomeAfterPointer() throws Exception {
    Path defaultOut = tempDir.resolve("preview-default");
    Path built = tempDir.resolve("preview-built");
    Files.createDirectories(built.resolve("8.2"));
    Files.writeString(built.resolve("8.2").resolve("index.html"), "<html>home</html>");

    PSSite site = virtualHelpSite();
    when(siteManager.findSite("Help")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);
    previewing.recordLastOutputRoot("help-docs", built);

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("Help");
    assertEquals(Boolean.TRUE, status.getAvailable());
    assertEquals("8.2/index.html", status.getHomePath());
  }

  @Test
  void previewFile_servesHtmlAndRejectsTraversal() throws Exception {
    Path defaultOut = tempDir.resolve("preview-files");
    Files.createDirectories(defaultOut.resolve("8.2"));
    Files.writeString(
        defaultOut.resolve("8.2").resolve("index.html"), "<html><a href=\"/8.2/x.html\">x</a>");

    PSSite site = virtualHelpSite();
    when(siteManager.findSite("Help")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);

    VirtualSitePreviewFile file = previewing.previewVirtualSiteFile("Help", "8.2/index.html");
    assertTrue(file.isHtml());
    assertEquals("8.2/index.html", file.getRelativePath());
    assertTrue(new String(file.getContent(), StandardCharsets.UTF_8).contains("href="));

    WebApplicationException traversal =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("Help", "../secret.txt"));
    assertEquals(400, traversal.getResponse().getStatus());

    WebApplicationException missing =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("Help", "no-such.html"));
    assertEquals(404, missing.getResponse().getStatus());
  }

  @Test
  void preview_forbiddenWhenNotAdmin() {
    SitesAdaptor denied =
        new SitesAdaptor(siteManager, () -> false, SitesAdaptor::defaultOutputRootForSiteKey, null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> denied.getVirtualSitePreviewStatus("Help"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void preview_rejectsRepositorySite() {
    PSSite site = new PSSite();
    site.setName("Corp");
    site.setGUID(siteGuid);
    when(siteManager.findSite("Corp")).thenReturn(site);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.getVirtualSitePreviewStatus("Corp"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void preview_rejectsUnknownSourceKind() {
    PSSite site = new PSSite();
    site.setName("Mystery");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-adapter");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, tempDir.resolve("virt-root").toString());
    when(siteManager.findSite("Mystery")).thenReturn(site);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.getVirtualSitePreviewStatus("Mystery"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void previewCsvFilesystem_statusAvailableWithHomePath() throws Exception {
    Path csvRoot = Files.createDirectories(tempDir.resolve("csv-status-src"));
    Path defaultOut = tempDir.resolve("csv-preview-default");
    Path built = tempDir.resolve("csv-preview-built");
    writeAssembledPreviewTree(built, "<html><body><h1>CSV Home</h1>Hello from CSV</body></html>");

    PSSite site = virtualCsvSite(csvRoot);
    when(siteManager.findSite("CsvHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);
    previewing.recordLastOutputRoot("csv-docs", built);

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("CsvHelp");
    assertEquals(Boolean.TRUE, status.getAvailable());
    assertEquals("8.2/index.html", status.getHomePath());
  }

  @Test
  void previewCsvFilesystem_streamsLastBuildHtml() throws Exception {
    Path csvRoot = Files.createDirectories(tempDir.resolve("csv-stream-src"));
    Path defaultOut = tempDir.resolve("csv-stream-default");
    Path built = tempDir.resolve("csv-stream-built");
    writeAssembledPreviewTree(built, "<html><body><h1>CSV Home</h1>Hello from CSV</body></html>");

    PSSite site = virtualCsvSite(csvRoot);
    when(siteManager.findSite("CsvHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);
    previewing.recordLastOutputRoot("csv-docs", built);

    VirtualSitePreviewFile file = previewing.previewVirtualSiteFile("CsvHelp", "8.2/index.html");
    assertTrue(file.isHtml());
    assertEquals("8.2/index.html", file.getRelativePath());
    String html = new String(file.getContent(), StandardCharsets.UTF_8);
    assertTrue(html.contains("CSV Home"), html);
    assertTrue(html.contains("Hello from CSV"), html);
  }

  @Test
  void previewSqlDatabase_statusAvailableWithHomePath() throws Exception {
    Path sqlRoot = Files.createDirectories(tempDir.resolve("sql-status-src"));
    Path defaultOut = tempDir.resolve("sql-preview-default");
    Path built = tempDir.resolve("sql-preview-built");
    writeAssembledPreviewTree(built, "<html><body><h1>SQL Home</h1>Hello from SQL</body></html>");

    PSSite site = virtualSqlSite(sqlRoot);
    when(siteManager.findSite("SqlHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);
    previewing.recordLastOutputRoot("sql-docs", built);

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("SqlHelp");
    assertEquals(Boolean.TRUE, status.getAvailable());
    assertEquals("8.2/index.html", status.getHomePath());
  }

  @Test
  void previewSqlDatabase_streamsLastBuildHtml() throws Exception {
    Path sqlRoot = Files.createDirectories(tempDir.resolve("sql-stream-src"));
    Path defaultOut = tempDir.resolve("sql-stream-default");
    Path built = tempDir.resolve("sql-stream-built");
    writeAssembledPreviewTree(built, "<html><body><h1>SQL Home</h1>Hello from SQL</body></html>");

    PSSite site = virtualSqlSite(sqlRoot);
    when(siteManager.findSite("SqlHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);
    previewing.recordLastOutputRoot("sql-docs", built);

    VirtualSitePreviewFile file = previewing.previewVirtualSiteFile("SqlHelp", "8.2/index.html");
    assertTrue(file.isHtml());
    assertEquals("8.2/index.html", file.getRelativePath());
    String html = new String(file.getContent(), StandardCharsets.UTF_8);
    assertTrue(html.contains("SQL Home"), html);
    assertTrue(html.contains("Hello from SQL"), html);
  }

  @Test
  void previewCsvFilesystem_rejectsTraversal() throws Exception {
    Path csvRoot = Files.createDirectories(tempDir.resolve("csv-trav-src"));
    Path defaultOut = tempDir.resolve("csv-trav-default");
    Path built = tempDir.resolve("csv-trav-built");
    writeAssembledPreviewTree(built, "<html><body>ok</body></html>");

    PSSite site = virtualCsvSite(csvRoot);
    when(siteManager.findSite("CsvHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);
    previewing.recordLastOutputRoot("csv-docs", built);

    WebApplicationException traversal =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("CsvHelp", "../secret.txt"));
    assertEquals(400, traversal.getResponse().getStatus());
  }

  @Test
  void previewCsvFilesystem_missingFileIs404() throws Exception {
    Path csvRoot = Files.createDirectories(tempDir.resolve("csv-miss-src"));
    Path defaultOut = tempDir.resolve("csv-miss-default");
    Path built = tempDir.resolve("csv-miss-built");
    writeAssembledPreviewTree(built, "<html><body>ok</body></html>");

    PSSite site = virtualCsvSite(csvRoot);
    when(siteManager.findSite("CsvHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);
    previewing.recordLastOutputRoot("csv-docs", built);

    WebApplicationException missing =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("CsvHelp", "no-such.html"));
    assertEquals(404, missing.getResponse().getStatus());
  }

  /**
   * CLI assemble never writes {@code last-output-root.txt}. Preview then falls back to the default
   * output directory when that tree exists.
   */
  @Test
  void previewCsvFilesystem_defaultOutputFallbackWithoutPointer() throws Exception {
    Path csvRoot = Files.createDirectories(tempDir.resolve("csv-cli-src"));
    Path built = tempDir.resolve("csv-cli-default");
    writeAssembledPreviewTree(built, "<html><body><h1>CSV Home</h1>Hello from CSV</body></html>");

    PSSite site = virtualCsvSite(csvRoot);
    when(siteManager.findSite("CsvHelp")).thenReturn(site);
    SitesAdaptor previewing = new SitesAdaptor(siteManager, () -> true, key -> built, null);

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("CsvHelp");
    assertEquals(Boolean.TRUE, status.getAvailable());
    assertEquals("8.2/index.html", status.getHomePath());

    VirtualSitePreviewFile file = previewing.previewVirtualSiteFile("CsvHelp", "8.2/index.html");
    String html = new String(file.getContent(), StandardCharsets.UTF_8);
    assertTrue(html.contains("CSV Home"), html);
  }

  @Test
  void previewCsvFilesystem_missingBuildIsUnavailableNot500() throws Exception {
    Path csvRoot = Files.createDirectories(tempDir.resolve("csv-empty-src"));
    Path defaultOut = tempDir.resolve("csv-preview-default-empty");
    PSSite site = virtualCsvSite(csvRoot);
    when(siteManager.findSite("CsvHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("CsvHelp");
    assertEquals(Boolean.FALSE, status.getAvailable());
    assertEquals(SitesAdaptor.MISSING_PREVIEW_MESSAGE, status.getMessage());

    WebApplicationException missing =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("CsvHelp", "8.2/index.html"));
    assertEquals(404, missing.getResponse().getStatus());
  }

  @Test
  void previewSqlDatabase_afterBuildAvailableWithHtml() throws Exception {
    Path siteRoot = createMinimalSqlTree(tempDir.resolve("sql-preview-src"));
    Path defaultOut = tempDir.resolve("sql-preview-default");
    Path built = tempDir.resolve("sql-preview-built");

    PSSite site = virtualSqlSite(siteRoot);
    when(siteManager.findSite("SqlHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(built.toAbsolutePath().normalize().toString());
    VirtualSiteBuildResult result = previewing.buildVirtualSite("SqlHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("SqlHelp");
    assertEquals(Boolean.TRUE, status.getAvailable());
    assertEquals("8.2/index.html", status.getHomePath());

    VirtualSitePreviewFile file = previewing.previewVirtualSiteFile("SqlHelp", "8.2/index.html");
    assertTrue(file.isHtml());
    assertEquals("8.2/index.html", file.getRelativePath());
    String html = new String(file.getContent(), StandardCharsets.UTF_8);
    assertTrue(html.contains("SQL Home"), html);
    assertTrue(html.contains("Hello from SQL"), html);
  }

  @Test
  void previewSqlDatabase_missingBuildIsUnavailableNot500() throws Exception {
    Path siteRoot = createMinimalSqlTree(tempDir.resolve("sql-preview-empty-src"));
    Path defaultOut = tempDir.resolve("sql-preview-default-empty");
    PSSite site = virtualSqlSite(siteRoot);
    when(siteManager.findSite("SqlHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("SqlHelp");
    assertEquals(Boolean.FALSE, status.getAvailable());
    assertEquals(SitesAdaptor.MISSING_PREVIEW_MESSAGE, status.getMessage());

    WebApplicationException missing =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("SqlHelp", "8.2/index.html"));
    assertEquals(404, missing.getResponse().getStatus());
  }

  @Test
  void previewSqlDatabase_rejectsTraversalAndMissingFile() throws Exception {
    Path siteRoot = Files.createDirectories(tempDir.resolve("sql-preview-trav-src"));
    Path defaultOut = tempDir.resolve("sql-preview-trav-default");
    Path built = tempDir.resolve("sql-preview-trav-built");
    writeAssembledPreviewTree(built, "<html><body><h1>SQL Home</h1>Hello from SQL</body></html>");

    PSSite site = virtualSqlSite(siteRoot);
    when(siteManager.findSite("SqlHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);
    previewing.recordLastOutputRoot("sql-docs", built);

    WebApplicationException traversal =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("SqlHelp", "../secret.txt"));
    assertEquals(400, traversal.getResponse().getStatus());

    WebApplicationException missing =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("SqlHelp", "no-such.html"));
    assertEquals(404, missing.getResponse().getStatus());
  }

  @Test
  void previewSqlDatabase_defaultOutputFallbackWithoutPointer() throws Exception {
    Path siteRoot = Files.createDirectories(tempDir.resolve("sql-cli-src"));
    Path built = tempDir.resolve("sql-cli-default");
    writeAssembledPreviewTree(built, "<html><body><h1>SQL Home</h1>Hello from SQL</body></html>");

    PSSite site = virtualSqlSite(siteRoot);
    when(siteManager.findSite("SqlHelp")).thenReturn(site);
    SitesAdaptor previewing = new SitesAdaptor(siteManager, () -> true, key -> built, null);

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("SqlHelp");
    assertEquals(Boolean.TRUE, status.getAvailable());
    assertEquals("8.2/index.html", status.getHomePath());

    VirtualSitePreviewFile file = previewing.previewVirtualSiteFile("SqlHelp", "8.2/index.html");
    String html = new String(file.getContent(), StandardCharsets.UTF_8);
    assertTrue(html.contains("SQL Home"), html);
  }

  @Test
  void previewHttpJson_afterBuildAvailableWithHtml() throws Exception {
    Path siteRoot = createMinimalHttpJsonTree(tempDir.resolve("http-preview-src"));
    Path defaultOut = tempDir.resolve("http-preview-default");
    Path built = tempDir.resolve("http-preview-built");

    PSSite site = virtualHttpJsonSite(siteRoot);
    when(siteManager.findSite("HttpHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(built.toAbsolutePath().normalize().toString());
    VirtualSiteBuildResult result = previewing.buildVirtualSite("HttpHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("HttpHelp");
    assertEquals(Boolean.TRUE, status.getAvailable());
    assertEquals("8.2/index.html", status.getHomePath());

    VirtualSitePreviewFile file = previewing.previewVirtualSiteFile("HttpHelp", "8.2/index.html");
    assertTrue(file.isHtml());
    assertEquals("8.2/index.html", file.getRelativePath());
    String html = new String(file.getContent(), StandardCharsets.UTF_8);
    assertTrue(html.contains("HTTP Home"), html);
    assertTrue(html.contains("Hello from JSON"), html);
  }

  @Test
  void previewHttpJson_missingBuildIsUnavailableNot500() throws Exception {
    Path siteRoot = createMinimalHttpJsonTree(tempDir.resolve("http-preview-empty-src"));
    Path defaultOut = tempDir.resolve("http-preview-default-empty");
    PSSite site = virtualHttpJsonSite(siteRoot);
    when(siteManager.findSite("HttpHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("HttpHelp");
    assertEquals(Boolean.FALSE, status.getAvailable());
    assertEquals(SitesAdaptor.MISSING_PREVIEW_MESSAGE, status.getMessage());

    WebApplicationException missing =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("HttpHelp", "8.2/index.html"));
    assertEquals(404, missing.getResponse().getStatus());
  }

  @Test
  void previewHttpJson_rejectsTraversalAndMissingFile() throws Exception {
    Path siteRoot = Files.createDirectories(tempDir.resolve("http-preview-trav-src"));
    Path defaultOut = tempDir.resolve("http-preview-trav-default");
    Path built = tempDir.resolve("http-preview-trav-built");
    writeAssembledPreviewTree(
        built, "<html><body><h1>HTTP Home</h1>Hello from JSON</body></html>");

    PSSite site = virtualHttpJsonSite(siteRoot);
    when(siteManager.findSite("HttpHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);
    previewing.recordLastOutputRoot("http-docs", built);

    WebApplicationException traversal =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("HttpHelp", "../secret.txt"));
    assertEquals(400, traversal.getResponse().getStatus());

    WebApplicationException missing =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("HttpHelp", "no-such.html"));
    assertEquals(404, missing.getResponse().getStatus());
  }

  @Test
  void previewHttpJson_defaultOutputFallbackWithoutPointer() throws Exception {
    Path siteRoot = Files.createDirectories(tempDir.resolve("http-cli-src"));
    Path built = tempDir.resolve("http-cli-default");
    writeAssembledPreviewTree(
        built, "<html><body><h1>HTTP Home</h1>Hello from JSON</body></html>");

    PSSite site = virtualHttpJsonSite(siteRoot);
    when(siteManager.findSite("HttpHelp")).thenReturn(site);
    SitesAdaptor previewing = new SitesAdaptor(siteManager, () -> true, key -> built, null);

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("HttpHelp");
    assertEquals(Boolean.TRUE, status.getAvailable());
    assertEquals("8.2/index.html", status.getHomePath());

    VirtualSitePreviewFile file = previewing.previewVirtualSiteFile("HttpHelp", "8.2/index.html");
    String html = new String(file.getContent(), StandardCharsets.UTF_8);
    assertTrue(html.contains("HTTP Home"), html);
  }

  @Test
  void previewObjectStorage_afterBuildAvailableWithHtml() throws Exception {
    Path siteRoot = createMinimalObjectStorageTree(tempDir.resolve("obj-preview-src"));
    Path defaultOut = tempDir.resolve("obj-preview-default");
    Path built = tempDir.resolve("obj-preview-built");

    PSSite site = virtualObjectStorageSite(siteRoot);
    when(siteManager.findSite("ObjectHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);

    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(built.toAbsolutePath().normalize().toString());
    VirtualSiteBuildResult result = previewing.buildVirtualSite("ObjectHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("ObjectHelp");
    assertEquals(Boolean.TRUE, status.getAvailable());
    assertEquals("8.2/index.html", status.getHomePath());

    VirtualSitePreviewFile file = previewing.previewVirtualSiteFile("ObjectHelp", "8.2/index.html");
    assertTrue(file.isHtml());
    assertEquals("8.2/index.html", file.getRelativePath());
    String html = new String(file.getContent(), StandardCharsets.UTF_8);
    assertTrue(html.contains("Object Home"), html);
    assertTrue(html.contains("Hello from objects"), html);
  }

  @Test
  void previewObjectStorage_missingBuildIsUnavailableNot500() throws Exception {
    Path siteRoot = createMinimalObjectStorageTree(tempDir.resolve("obj-preview-empty-src"));
    Path defaultOut = tempDir.resolve("obj-preview-default-empty");
    PSSite site = virtualObjectStorageSite(siteRoot);
    when(siteManager.findSite("ObjectHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("ObjectHelp");
    assertEquals(Boolean.FALSE, status.getAvailable());
    assertEquals(SitesAdaptor.MISSING_PREVIEW_MESSAGE, status.getMessage());

    WebApplicationException missing =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("ObjectHelp", "8.2/index.html"));
    assertEquals(404, missing.getResponse().getStatus());
  }

  @Test
  void previewObjectStorage_rejectsTraversalAndMissingFile() throws Exception {
    Path siteRoot = Files.createDirectories(tempDir.resolve("obj-preview-trav-src"));
    Path defaultOut = tempDir.resolve("obj-preview-trav-default");
    Path built = tempDir.resolve("obj-preview-trav-built");
    writeAssembledPreviewTree(
        built, "<html><body><h1>Object Home</h1>Hello from objects</body></html>");

    PSSite site = virtualObjectStorageSite(siteRoot);
    when(siteManager.findSite("ObjectHelp")).thenReturn(site);
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);
    previewing.recordLastOutputRoot("obj-docs", built);

    WebApplicationException traversal =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("ObjectHelp", "../secret.txt"));
    assertEquals(400, traversal.getResponse().getStatus());

    WebApplicationException missing =
        assertThrows(
            WebApplicationException.class,
            () -> previewing.previewVirtualSiteFile("ObjectHelp", "no-such.html"));
    assertEquals(404, missing.getResponse().getStatus());
  }

  @Test
  void previewObjectStorage_defaultOutputFallbackWithoutPointer() throws Exception {
    Path siteRoot = Files.createDirectories(tempDir.resolve("obj-cli-src"));
    Path built = tempDir.resolve("obj-cli-default");
    writeAssembledPreviewTree(
        built, "<html><body><h1>Object Home</h1>Hello from objects</body></html>");

    PSSite site = virtualObjectStorageSite(siteRoot);
    when(siteManager.findSite("ObjectHelp")).thenReturn(site);
    SitesAdaptor previewing = new SitesAdaptor(siteManager, () -> true, key -> built, null);

    VirtualSitePreviewStatus status = previewing.getVirtualSitePreviewStatus("ObjectHelp");
    assertEquals(Boolean.TRUE, status.getAvailable());
    assertEquals("8.2/index.html", status.getHomePath());

    VirtualSitePreviewFile file = previewing.previewVirtualSiteFile("ObjectHelp", "8.2/index.html");
    String html = new String(file.getContent(), StandardCharsets.UTF_8);
    assertTrue(html.contains("Object Home"), html);
  }

  @Test
  void requireSafeRelativePreviewPath_rejectsAbsoluteAndDotDot() {
    WebApplicationException dots =
        assertThrows(
            WebApplicationException.class,
            () -> SitesAdaptor.requireSafeRelativePreviewPath("a/../../etc/passwd"));
    assertEquals(400, dots.getResponse().getStatus());
  }

  @Test
  void findHomeRelativePath_prefersRootThen82() throws Exception {
    Path out = tempDir.resolve("homes");
    Files.createDirectories(out.resolve("8.2"));
    Files.writeString(out.resolve("8.2").resolve("index.html"), "v");
    assertEquals("8.2/index.html", SitesAdaptor.findHomeRelativePath(out));
    Files.writeString(out.resolve("index.html"), "root");
    assertEquals("index.html", SitesAdaptor.findHomeRelativePath(out));
  }

  @Test
  void findHomeRelativePath_picksHighestNumericVersionNotLexical() throws Exception {
    Path out = tempDir.resolve("homes-versions");
    Files.createDirectories(out.resolve("9.0"));
    Files.createDirectories(out.resolve("10.0"));
    Files.createDirectories(out.resolve("_meta"));
    Files.writeString(out.resolve("9.0").resolve("index.html"), "nine");
    Files.writeString(out.resolve("10.0").resolve("index.html"), "ten");
    Files.writeString(out.resolve("_meta").resolve("index.html"), "meta");
    assertEquals("10.0/index.html", SitesAdaptor.findHomeRelativePath(out));
    assertTrue(SitesAdaptor.compareHomeCandidateDirectories(out.resolve("10.0"), out.resolve("9.0")) < 0);
  }

  @Test
  void recordLastOutputRoot_doesNotThrowWhenPointerUnwritable() throws Exception {
    Path defaultOut = tempDir.resolve("pointer-blocked");
    Files.createDirectories(defaultOut);
    Files.writeString(defaultOut.resolve("_meta"), "not-a-directory");
    SitesAdaptor previewing =
        new SitesAdaptor(siteManager, () -> true, key -> defaultOut, null);
    assertDoesNotThrow(() -> previewing.recordLastOutputRoot("help-docs", defaultOut));
  }

  private PSSite virtualHelpSite() {
    PSSite site = new PSSite();
    site.setName("Help");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    put(site, PSVirtualSiteHelper.PROP_ROOT_PATH, tempDir.resolve("virt-root").toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "help-docs");
    return site;
  }

  private PSSite virtualSqlSite(Path sqlRoot) {
    PSSite site = new PSSite();
    site.setName("SqlHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "sql-database");
    put(
        site,
        PSVirtualSiteHelper.PROP_ROOT_PATH,
        sqlRoot.toAbsolutePath().normalize().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "sql-docs");
    return site;
  }

  private PSSite virtualCsvSite(Path csvRoot) {
    PSSite site = new PSSite();
    site.setName("CsvHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "csv-filesystem");
    put(
        site,
        PSVirtualSiteHelper.PROP_ROOT_PATH,
        csvRoot.toAbsolutePath().normalize().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "csv-docs");
    return site;
  }

  private PSSite virtualHttpJsonSite(Path httpRoot) {
    PSSite site = new PSSite();
    site.setName("HttpHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "http-json");
    put(
        site,
        PSVirtualSiteHelper.PROP_ROOT_PATH,
        httpRoot.toAbsolutePath().normalize().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "http-docs");
    return site;
  }

  private PSSite virtualObjectStorageSite(Path objectRoot) {
    PSSite site = new PSSite();
    site.setName("ObjectHelp");
    site.setGUID(siteGuid);
    put(site, PSVirtualSiteHelper.PROP_SOURCE_KIND, "object-storage");
    put(
        site,
        PSVirtualSiteHelper.PROP_ROOT_PATH,
        objectRoot.toAbsolutePath().normalize().toString());
    put(site, PSVirtualSiteHelper.PROP_SITE_KEY, "obj-docs");
    return site;
  }

  private static Path createMinimalVirtualTree(Path siteRoot) throws Exception {
    Path versionDir = siteRoot.resolve("8.2");
    Files.createDirectories(versionDir);
    Files.createDirectories(siteRoot.resolve("_theme"));
    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: Help
        versions:
          - id: "8.2"
            label: "8.2"
            path: 8.2
            default: true
        theme:
          layout: page.html
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        siteRoot.resolve("_theme").resolve("page.html"),
        "<html><body>{{content}}</body></html>",
        StandardCharsets.UTF_8);
    Files.writeString(
        versionDir.resolve("index.md"),
        """
        ---
        id: help-home
        title: Home
        ---

        Welcome.
        """,
        StandardCharsets.UTF_8);
    return siteRoot;
  }

  private static void writeAssembledPreviewTree(Path outputRoot, String html) throws Exception {
    Files.createDirectories(outputRoot.resolve("8.2"));
    Files.writeString(
        outputRoot.resolve("8.2").resolve("index.html"), html, StandardCharsets.UTF_8);
  }

  private static Path createMinimalCsvTree(Path siteRoot) throws Exception {
    Files.createDirectories(siteRoot.resolve("8.2"));
    Files.createDirectories(siteRoot.resolve("_theme"));
    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: CSV Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: 8.2
            default: true
        theme:
          layout: page.html
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        siteRoot.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Files.writeString(
        siteRoot.resolve("8.2").resolve("pages.csv"),
        "id,title,body,path,order\ncsv-home,CSV Home,Hello from CSV.,index.md,1\n",
        StandardCharsets.UTF_8);
    return siteRoot;
  }

  /**
   * Local JSON fixture for http-json REST Build and Publish. Catalog URL/file live in {@code
   * _config.yaml}; pass a loopback {@code catalogUrl} to use {@code http.url} instead of {@code
   * http.file}.
   */
  private static Path createMinimalHttpJsonTree(Path siteRoot) throws Exception {
    return createMinimalHttpJsonTree(siteRoot, null);
  }

  private static Path createMinimalHttpJsonTree(Path siteRoot, String catalogUrl)
      throws Exception {
    Files.createDirectories(siteRoot.resolve("8.2"));
    Files.createDirectories(siteRoot.resolve("_theme"));
    String httpBlock;
    if (catalogUrl != null && !catalogUrl.isBlank()) {
      httpBlock =
          """
          http:
            url: "%s"
          """
              .formatted(catalogUrl);
    } else {
      httpBlock =
          """
          http:
            file: pages.json
          """;
      Files.writeString(
          siteRoot.resolve("pages.json"),
          """
          {"pages":[{"id":"http-home","path":"index.html","title":"HTTP Home","body":"Hello from JSON.","order":1}]}
          """,
          StandardCharsets.UTF_8);
    }
    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: HTTP Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        %s
        """
            .formatted(httpBlock),
        StandardCharsets.UTF_8);
    Files.writeString(
        siteRoot.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    return siteRoot;
  }

  /**
   * Local object-key bucket for object-storage REST Build, last-build Preview, and Publish.
   * Markdown under {@code 8.2/} plus required {@code _config.yaml}; portable NIO {@link Path} /
   * {@link Files}. No cloud URLs or credentials.
   */
  private static Path createMinimalObjectStorageTree(Path siteRoot) throws Exception {
    Files.createDirectories(siteRoot.resolve("8.2"));
    Files.createDirectories(siteRoot.resolve("_theme"));
    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: Object Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        siteRoot.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Files.writeString(
        siteRoot.resolve("8.2").resolve("index.md"),
        """
        ---
        id: home
        title: Object Home
        ---
        Hello from objects.
        """,
        StandardCharsets.UTF_8);
    return siteRoot;
  }

  /**
   * Local RSS 2.0 fixture for rss-atom REST Build. Feed file is {@code feed.xml}; {@code
   * _config.yaml} sets {@code rss.file}. Portable NIO {@link Path} / {@link Files}. No live
   * remote feeds or credentials.
   */
  private static Path createMinimalRssAtomTree(Path siteRoot) throws Exception {
    Files.createDirectories(siteRoot.resolve("8.2"));
    Files.createDirectories(siteRoot.resolve("_theme"));
    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: RSS Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        rss:
          file: feed.xml
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        siteRoot.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Files.writeString(
        siteRoot.resolve("feed.xml"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Sample</title>
            <item>
              <guid>home</guid>
              <title>RSS Home</title>
              <description>Hello from RSS.</description>
            </item>
          </channel>
        </rss>
        """,
        StandardCharsets.UTF_8);
    return siteRoot;
  }

  /**
   * In-memory H2 fixture for sql-database Build and Publish tests. JDBC user is {@code sa}. A dummy
   * {@code password} is present in {@code _config.yaml} so tests can prove it is not copied into
   * HTML. Inline SELECT by default; pass a relative {@code queryFile} (NIO {@link Path}) to prove
   * {@code sql.queryFile} REST Build / Publish.
   */
  private static Path createMinimalSqlTree(Path siteRoot) throws Exception {
    return createMinimalSqlTree(
        siteRoot,
        "SELECT id, title, body, path, sort_order AS \"order\" FROM pages",
        null);
  }

  private static Path createMinimalSqlTree(Path siteRoot, String query, Path queryFile)
      throws Exception {
    String jdbc = "jdbc:h2:mem:vsql_rest_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
    org.h2.Driver.load();
    try (Connection c =
        DriverManager.getConnection(jdbc, "sa", "secret-sql-password-should-not-leak");
        Statement st = c.createStatement()) {
      st.execute(
          "CREATE TABLE pages ("
              + "id VARCHAR(128), title VARCHAR(256), body CLOB, path VARCHAR(512),"
              + " sort_order INT)");
      st.execute(
          "INSERT INTO pages (id, title, body, path, sort_order) VALUES"
              + " ('sql-home', 'SQL Home', 'Hello from SQL.', 'index.md', 1)");
    }
    Files.createDirectories(siteRoot.resolve("8.2"));
    Files.createDirectories(siteRoot.resolve("_theme"));
    String sqlBlock;
    if (queryFile != null) {
      Path queryPath = siteRoot.resolve(queryFile);
      Path parent = queryPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(queryPath, query + "\n", StandardCharsets.UTF_8);
      String logical = queryFile.toString().replace('\\', '/');
      sqlBlock =
          """
          sql:
            jdbcUrl: "%s"
            user: sa
            password: "secret-sql-password-should-not-leak"
            queryFile: %s
          """
              .formatted(jdbc, logical);
    } else {
      sqlBlock =
          """
          sql:
            jdbcUrl: "%s"
            user: sa
            password: "secret-sql-password-should-not-leak"
            query: |
              %s
          """
              .formatted(jdbc, query);
    }
    Files.writeString(
        siteRoot.resolve("_config.yaml"),
        """
        site:
          title: SQL Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        %s
        """
            .formatted(sqlBlock),
        StandardCharsets.UTF_8);
    Files.writeString(
        siteRoot.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    return siteRoot;
  }

  private void put(PSSite site, String name, String value) {
    PSVirtualSiteHelper.putProperty(site, previewCtx, name, value);
  }
}

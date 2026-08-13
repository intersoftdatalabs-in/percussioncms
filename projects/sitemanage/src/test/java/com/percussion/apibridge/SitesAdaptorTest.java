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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.percussion.rest.sites.VirtualSiteProperties;
import com.percussion.rest.sites.VirtualSitePublishResult;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.data.PSSite;
import com.percussion.services.virtualsite.PSVirtualSiteBuildResult;
import com.percussion.services.virtualsite.PSVirtualSiteHelper;
import com.percussion.utils.guid.IPSGuid;
import jakarta.ws.rs.WebApplicationException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    VirtualSiteProperties v = SitesAdaptor.readVirtual(site);
    assertEquals("git-filesystem", v.getSourceKind().orElse(null));
    assertEquals("C:/docs/product-docs", v.getRootPath().orElse(null));
    assertEquals("custom.yaml", v.getConfigFile().orElse(null));
    assertEquals("product-docs", v.getSiteKey().orElse(null));
    assertTrue(Boolean.TRUE.equals(v.getVirtual()));
  }

  @Test
  void readVirtual_repositoryIsNotVirtual() {
    PSSite site = new PSSite();
    site.setName("Corp");
    VirtualSiteProperties v = SitesAdaptor.readVirtual(site);
    assertFalse(Boolean.TRUE.equals(v.getVirtual()));
    assertTrue(v.getSourceKind().isEmpty());
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
    assertEquals("git-filesystem", out.getSourceKind().orElse(null));
    assertEquals("C:/docs/product-docs", out.getRootPath().orElse(null));

    ArgumentCaptor<PSSite> saved = ArgumentCaptor.forClass(PSSite.class);
    verify(siteManager).saveSite(saved.capture());
    assertTrue(PSVirtualSiteHelper.isVirtual(saved.getValue()));
    assertEquals(
        "product-docs",
        PSVirtualSiteHelper.findProperty(saved.getValue(), PSVirtualSiteHelper.PROP_SITE_KEY)
            .orElse(null));
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
    assertEquals("Help", result.getSiteName().orElse(null));
    assertEquals("sample", result.getSiteKey().orElse(null));
    assertEquals(2, result.getPagesWritten().intValue());
    assertEquals(0, result.getLinkProblemCount().intValue());
    assertFalse(Boolean.TRUE.equals(result.getHasLinkProblems()));
    assertTrue(result.getOutputPath().isPresent());
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
    assertEquals("Help", result.getSiteName().orElse(null));
    assertEquals("help-docs", result.getSiteKey().orElse(null));
    assertEquals(1, result.getPagesWritten().intValue());
    assertTrue(result.getFilesCopied() >= 1);
    assertFalse(Boolean.TRUE.equals(result.getHasLinkProblems()));
    assertTrue(Files.isRegularFile(publishTo.resolve("8.2").resolve("index.html")));
    assertTrue(Files.isRegularFile(staging.resolve("8.2").resolve("index.html")));
    assertFalse(Files.exists(publishTo.resolve("_meta")));
    assertTrue(result.getPublishPath().isPresent());
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

  private void put(PSSite site, String name, String value) {
    PSVirtualSiteHelper.putProperty(site, previewCtx, name, value);
  }
}

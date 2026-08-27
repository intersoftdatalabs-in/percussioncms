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
package com.percussion.rest.sites;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("UnitTest")
public class SitesResourceTest {

  private ISiteAdaptor adaptor;
  private SitesResource resource;
  private Logger previousLog;
  private Logger mockLog;

  @TempDir Path tempDir;

  @BeforeEach
  public void setUp() {
    previousLog = SitesResource.log;
    mockLog = mock(Logger.class);
    SitesResource.log = mockLog;

    adaptor = mock(ISiteAdaptor.class);
    resource = new SitesResource(adaptor);
  }

  @AfterEach
  public void restoreLog() {
    SitesResource.log = previousLog;
  }

  @Test
  public void listSitesSuccess() {
    SiteList list = new SiteList();
    Site s = new Site();
    s.setName("Help");
    list.add(s);
    when(adaptor.findAllSites()).thenReturn(list);

    SiteList out = resource.listSites();
    assertEquals(1, out.size());
    assertEquals("Help", out.get(0).getName());
    verify(adaptor).findAllSites();
  }

  @Test
  public void listSitesNullBecomesEmpty() {
    when(adaptor.findAllSites()).thenReturn(null);
    assertTrue(resource.listSites().isEmpty());
  }

  @Test
  public void getSiteByName() {
    Site s = new Site();
    s.setName("Help");
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("git-filesystem");
    v.setRootPath("C:/docs");
    v.setVirtual(true);
    s.setVirtual(v);
    when(adaptor.findByName("Help")).thenReturn(s);

    Site out = resource.getSite("Help");
    assertEquals("Help", out.getName());
    assertTrue(out.getVirtual() != null);
    assertEquals("git-filesystem", out.getVirtual().getSourceKind());
    verify(adaptor).findByName("Help");
    verify(adaptor, never()).findByGuid(any());
  }

  @Test
  public void getSiteFallsBackToGuid() {
    Site s = new Site();
    s.setName("Help");
    when(adaptor.findByName("0-1-301")).thenReturn(null);
    when(adaptor.findByGuid("0-1-301")).thenReturn(s);

    Site out = resource.getSite("0-1-301");
    assertEquals("Help", out.getName());
    verify(adaptor).findByGuid("0-1-301");
  }

  @Test
  public void getSiteNotFound404() {
    when(adaptor.findByName("missing")).thenReturn(null);
    when(adaptor.findByGuid("missing")).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSite("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getVirtualPropertiesDelegates() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("git-filesystem");
    when(adaptor.getVirtualSiteProperties("Help")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("Help");
    assertEquals("git-filesystem", out.getSourceKind());
    verify(adaptor).getVirtualSiteProperties("Help");
  }

  @Test
  public void updateVirtualPropertiesDelegates() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("git-filesystem");
    body.setRootPath("C:/docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("git-filesystem");
    saved.setRootPath("C:/docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("Help"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("Help", body);
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("Help", body);
  }

  @Test
  public void getVirtualPropertiesRoundTripsCsvFilesystem() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("csv-filesystem");
    v.setRootPath("C:/csv-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("CsvHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("CsvHelp");
    assertEquals("csv-filesystem", out.getSourceKind());
    assertEquals("C:/csv-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("CsvHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsCsvFilesystem() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("csv-filesystem");
    body.setRootPath("C:/csv-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("csv-filesystem");
    saved.setRootPath("C:/csv-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("CsvHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("CsvHelp", body);
    assertEquals("csv-filesystem", out.getSourceKind());
    assertEquals("C:/csv-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("CsvHelp", body);
  }

  @Test
  public void getVirtualPropertiesRoundTripsSqlDatabase() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("sql-database");
    v.setRootPath("C:/sql-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("SqlHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("SqlHelp");
    assertEquals("sql-database", out.getSourceKind());
    assertEquals("C:/sql-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("SqlHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsSqlDatabase() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sql-database");
    body.setRootPath("C:/sql-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("sql-database");
    saved.setRootPath("C:/sql-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("SqlHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("SqlHelp", body);
    assertEquals("sql-database", out.getSourceKind());
    assertEquals("C:/sql-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("SqlHelp", body);
  }

  @Test
  public void getVirtualPropertiesRoundTripsHttpJson() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("http-json");
    v.setRootPath("C:/http-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("HttpHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("HttpHelp");
    assertEquals("http-json", out.getSourceKind());
    assertEquals("C:/http-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("HttpHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsHttpJson() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("http-json");
    body.setRootPath("C:/http-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("http-json");
    saved.setRootPath("C:/http-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("HttpHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("HttpHelp", body);
    assertEquals("http-json", out.getSourceKind());
    assertEquals("C:/http-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("HttpHelp", body);
  }

  @Test
  public void getVirtualPropertiesRoundTripsObjectStorage() {
    VirtualSiteProperties v = new VirtualSiteProperties();
    v.setSourceKind("object-storage");
    v.setRootPath("C:/object-docs");
    v.setVirtual(true);
    when(adaptor.getVirtualSiteProperties("ObjectHelp")).thenReturn(v);

    VirtualSiteProperties out = resource.getVirtualProperties("ObjectHelp");
    assertEquals("object-storage", out.getSourceKind());
    assertEquals("C:/object-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).getVirtualSiteProperties("ObjectHelp");
  }

  @Test
  public void updateVirtualPropertiesRoundTripsObjectStorage() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("object-storage");
    body.setRootPath("C:/object-docs");
    VirtualSiteProperties saved = new VirtualSiteProperties();
    saved.setSourceKind("object-storage");
    saved.setRootPath("C:/object-docs");
    saved.setVirtual(true);
    when(adaptor.updateVirtualSiteProperties(eq("ObjectHelp"), same(body))).thenReturn(saved);

    VirtualSiteProperties out = resource.updateVirtualProperties("ObjectHelp", body);
    assertEquals("object-storage", out.getSourceKind());
    assertEquals("C:/object-docs", out.getRootPath());
    assertTrue(Boolean.TRUE.equals(out.getVirtual()));
    verify(adaptor).updateVirtualSiteProperties("ObjectHelp", body);
  }

  @Test
  public void updateVirtualPropertiesUnknownKindPropagates400() {
    VirtualSiteProperties body = new VirtualSiteProperties();
    body.setSourceKind("sql-adapter");
    body.setRootPath("C:/docs");
    when(adaptor.updateVirtualSiteProperties(eq("Help"), same(body)))
        .thenThrow(
            new WebApplicationException(
                "Unsupported virtual.sourceKind", Response.Status.BAD_REQUEST));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateVirtualProperties("Help", body));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor).updateVirtualSiteProperties("Help", body);
  }

  @Test
  public void updateVirtualPropertiesNullBody400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.updateVirtualProperties("Help", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).updateVirtualSiteProperties(any(), any());
  }

  @Test
  public void buildVirtualSiteDelegates() {
    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("Help");
    built.setPagesWritten(3);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath("C:/tmp/out");
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot("C:/tmp/out");
    when(adaptor.buildVirtualSite(eq("Help"), same(req))).thenReturn(built);

    VirtualSiteBuildResult out = resource.buildVirtualSite("Help", req);
    assertEquals(3, out.getPagesWritten().intValue());
    assertEquals("C:/tmp/out", out.getOutputPath());
    verify(adaptor).buildVirtualSite("Help", req);
  }

  @Test
  public void buildVirtualSiteCsvFilesystemFixtureDelegates() throws Exception {
    Path csvRoot = tempDir.resolve("csv-site");
    Files.createDirectories(csvRoot.resolve("8.2"));
    Files.writeString(
        csvRoot.resolve("8.2").resolve("pages.csv"),
        "id,title,body,path,order\ncsv-home,CSV Home,Hello from CSV.,index.md,1\n",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("csv-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("CsvHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("CsvHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("CsvHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(csvRoot.resolve("8.2").resolve("pages.csv")));
    verify(adaptor).buildVirtualSite("CsvHelp", req);
  }

  @Test
  public void buildVirtualSiteSqlDatabaseFixtureDelegates() throws Exception {
    Path sqlRoot = tempDir.resolve("sql-site");
    Files.createDirectories(sqlRoot);
    Files.writeString(
        sqlRoot.resolve("_config.yaml"),
        """
        site:
          title: SQL Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        sql:
          jdbcUrl: jdbc:h2:mem:vsql_rest_fixture;DB_CLOSE_DELAY=-1
          user: sa
          query: SELECT id, title, body FROM pages
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("sql-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("SqlHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("SqlHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("SqlHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(sqlRoot.resolve("_config.yaml")));
    verify(adaptor).buildVirtualSite("SqlHelp", req);
  }

  @Test
  public void buildVirtualSiteHttpJsonFixtureDelegates() throws Exception {
    Path httpRoot = tempDir.resolve("http-site");
    Files.createDirectories(httpRoot);
    Files.writeString(
        httpRoot.resolve("_config.yaml"),
        """
        site:
          title: HTTP Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        http:
          file: pages.json
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        httpRoot.resolve("pages.json"),
        """
        {"pages":[{"id":"http-home","path":"index.html","title":"HTTP Home","body":"Hello from JSON."}]}
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("http-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("HttpHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("HttpHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("HttpHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(httpRoot.resolve("pages.json")));
    assertTrue(Files.isRegularFile(httpRoot.resolve("_config.yaml")));
    verify(adaptor).buildVirtualSite("HttpHelp", req);
  }

  @Test
  public void buildVirtualSiteObjectStorageFixtureDelegates() throws Exception {
    Path objectRoot = tempDir.resolve("object-site");
    Files.createDirectories(objectRoot.resolve("8.2"));
    Files.writeString(
        objectRoot.resolve("_config.yaml"),
        """
        site:
          title: Object Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        objectRoot.resolve("8.2").resolve("index.md"),
        """
        ---
        id: home
        title: Object Home
        ---
        Hello from objects.
        """,
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("object-out");
    Files.createDirectories(out);

    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setSiteName("ObjectHelp");
    built.setPagesWritten(1);
    built.setLinkProblemCount(0);
    built.setHasLinkProblems(false);
    built.setOutputPath(out.toAbsolutePath().toString());
    VirtualSiteBuildRequest req = new VirtualSiteBuildRequest();
    req.setOutputRoot(out.toAbsolutePath().toString());
    when(adaptor.buildVirtualSite(eq("ObjectHelp"), same(req))).thenReturn(built);

    VirtualSiteBuildResult result = resource.buildVirtualSite("ObjectHelp", req);
    assertEquals(1, result.getPagesWritten().intValue());
    assertEquals(out.toAbsolutePath().toString(), result.getOutputPath());
    assertTrue(Files.isRegularFile(objectRoot.resolve("_config.yaml")));
    assertTrue(Files.isRegularFile(objectRoot.resolve("8.2").resolve("index.md")));
    verify(adaptor).buildVirtualSite("ObjectHelp", req);
  }

  @Test
  public void buildVirtualSiteUnknownKindPropagates400() {
    when(adaptor.buildVirtualSite(eq("Help"), any()))
        .thenThrow(
            new WebApplicationException(
                "Unsupported virtual.sourceKind", Response.Status.BAD_REQUEST));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.buildVirtualSite("Help", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor).buildVirtualSite("Help", null);
  }

  @Test
  public void buildVirtualSiteAllowsNullBody() {
    VirtualSiteBuildResult built = new VirtualSiteBuildResult();
    built.setPagesWritten(1);
    when(adaptor.buildVirtualSite(eq("Help"), any())).thenReturn(built);

    VirtualSiteBuildResult out = resource.buildVirtualSite("Help", null);
    assertEquals(1, out.getPagesWritten().intValue());
    verify(adaptor).buildVirtualSite("Help", null);
  }

  @Test
  public void previewStatusDelegates() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    when(adaptor.getVirtualSitePreviewStatus("Help")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("Help");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/index.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("Help");
  }

  @Test
  public void previewStatusBlankName400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getVirtualSitePreviewStatus(" "));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).getVirtualSitePreviewStatus(any());
  }

  @Test
  public void previewFileDelegatesAndRewritesHtml() {
    byte[] html = "<a href=\"/8.2/index.html\">Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("Help"), eq("8.2/index.html")))
        .thenReturn(new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/index.html", html));

    Response out = resource.previewVirtualSiteFile("Help", "8.2/index.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(text.contains("/services/sites/Help/virtual/preview/8.2/index.html"), text);
    verify(adaptor).previewVirtualSiteFile("Help", "8.2/index.html");
  }

  @Test
  public void previewFileBlankName400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.previewVirtualSiteFile(" ", "index.html"));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).previewVirtualSiteFile(any(), any());
  }

  @Test
  public void previewStatusDelegatesSqlDatabase() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    when(adaptor.getVirtualSitePreviewStatus("SqlHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("SqlHelp");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/index.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("SqlHelp");
  }

  @Test
  public void previewStatusSqlDatabaseMissingBuildIsUnavailable() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview. Run Build Virtual Site first.");
    when(adaptor.getVirtualSitePreviewStatus("SqlHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("SqlHelp");
    assertEquals(Boolean.FALSE, out.getAvailable());
    assertTrue(out.getMessage() != null && out.getMessage().contains("No assembled"));
    verify(adaptor).getVirtualSitePreviewStatus("SqlHelp");
  }

  @Test
  public void previewFileDelegatesSqlDatabaseHtml() {
    byte[] html = "<a href=\"/8.2/index.html\">SQL Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("SqlHelp"), eq("8.2/index.html")))
        .thenReturn(new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/index.html", html));

    Response out = resource.previewVirtualSiteFile("SqlHelp", "8.2/index.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(text.contains("/services/sites/SqlHelp/virtual/preview/8.2/index.html"), text);
    assertTrue(text.contains("SQL Home"), text);
    verify(adaptor).previewVirtualSiteFile("SqlHelp", "8.2/index.html");
  }

  @Test
  public void previewStatusDelegatesHttpJson() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    when(adaptor.getVirtualSitePreviewStatus("HttpHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("HttpHelp");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/index.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("HttpHelp");
  }

  @Test
  public void previewStatusHttpJsonMissingBuildIsUnavailable() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview. Run Build Virtual Site first.");
    when(adaptor.getVirtualSitePreviewStatus("HttpHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("HttpHelp");
    assertEquals(Boolean.FALSE, out.getAvailable());
    assertTrue(out.getMessage() != null && out.getMessage().contains("No assembled"));
    verify(adaptor).getVirtualSitePreviewStatus("HttpHelp");
  }

  @Test
  public void previewFileDelegatesHttpJsonHtml() {
    byte[] html = "<a href=\"/8.2/index.html\">HTTP Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("HttpHelp"), eq("8.2/index.html")))
        .thenReturn(new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/index.html", html));

    Response out = resource.previewVirtualSiteFile("HttpHelp", "8.2/index.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(text.contains("/services/sites/HttpHelp/virtual/preview/8.2/index.html"), text);
    assertTrue(text.contains("HTTP Home"), text);
    verify(adaptor).previewVirtualSiteFile("HttpHelp", "8.2/index.html");
  }

  @Test
  public void previewStatusDelegatesObjectStorage() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    when(adaptor.getVirtualSitePreviewStatus("ObjectHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("ObjectHelp");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/index.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("ObjectHelp");
  }

  @Test
  public void previewStatusObjectStorageMissingBuildIsUnavailable() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview. Run Build Virtual Site first.");
    when(adaptor.getVirtualSitePreviewStatus("ObjectHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("ObjectHelp");
    assertEquals(Boolean.FALSE, out.getAvailable());
    assertTrue(out.getMessage() != null && out.getMessage().contains("No assembled"));
    verify(adaptor).getVirtualSitePreviewStatus("ObjectHelp");
  }

  @Test
  public void previewFileDelegatesObjectStorageHtml() {
    byte[] html = "<a href=\"/8.2/index.html\">Object Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("ObjectHelp"), eq("8.2/index.html")))
        .thenReturn(new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/index.html", html));

    Response out = resource.previewVirtualSiteFile("ObjectHelp", "8.2/index.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(text.contains("/services/sites/ObjectHelp/virtual/preview/8.2/index.html"), text);
    assertTrue(text.contains("Object Home"), text);
    verify(adaptor).previewVirtualSiteFile("ObjectHelp", "8.2/index.html");
  }

  @Test
  public void previewStatusDelegatesRssAtom() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    when(adaptor.getVirtualSitePreviewStatus("RssHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("RssHelp");
    assertEquals(Boolean.TRUE, out.getAvailable());
    assertEquals("8.2/index.html", out.getHomePath());
    verify(adaptor).getVirtualSitePreviewStatus("RssHelp");
  }

  @Test
  public void previewStatusRssAtomMissingBuildIsUnavailable() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview. Run Build Virtual Site first.");
    when(adaptor.getVirtualSitePreviewStatus("RssHelp")).thenReturn(status);

    VirtualSitePreviewStatus out = resource.getVirtualSitePreviewStatus("RssHelp");
    assertEquals(Boolean.FALSE, out.getAvailable());
    assertTrue(out.getMessage() != null && out.getMessage().contains("No assembled"));
    verify(adaptor).getVirtualSitePreviewStatus("RssHelp");
  }

  @Test
  public void previewFileDelegatesRssAtomHtml() {
    byte[] html = "<a href=\"/8.2/index.html\">RSS Home</a>".getBytes(StandardCharsets.UTF_8);
    when(adaptor.previewVirtualSiteFile(eq("RssHelp"), eq("8.2/index.html")))
        .thenReturn(new VirtualSitePreviewFile("text/html; charset=UTF-8", "8.2/index.html", html));

    Response out = resource.previewVirtualSiteFile("RssHelp", "8.2/index.html");
    assertEquals(200, out.getStatus());
    byte[] body = (byte[]) out.getEntity();
    String text = new String(body, StandardCharsets.UTF_8);
    assertTrue(text.contains("/services/sites/RssHelp/virtual/preview/8.2/index.html"), text);
    assertTrue(text.contains("RSS Home"), text);
    verify(adaptor).previewVirtualSiteFile("RssHelp", "8.2/index.html");
  }

  @Test
  public void buildVirtualSiteBlankName400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.buildVirtualSite(" ", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).buildVirtualSite(any(), any());
  }

  @Test
  public void publishVirtualSiteDelegates() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("Help");
    published.setPagesWritten(3);
    published.setFilesCopied(5);
    published.setPublishPath("C:/pub/help");
    when(adaptor.publishVirtualSite("Help")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("Help");
    assertEquals(3, out.getPagesWritten().intValue());
    assertEquals(5, out.getFilesCopied().intValue());
    assertEquals("C:/pub/help", out.getPublishPath());
    verify(adaptor).publishVirtualSite("Help");
  }

  @Test
  public void publishVirtualSiteDelegatesSqlDatabase() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("SqlHelp");
    published.setSiteKey("sql-docs");
    published.setPagesWritten(1);
    published.setFilesCopied(2);
    published.setPublishPath(tempDir.resolve("sql-pub").toString());
    when(adaptor.publishVirtualSite("SqlHelp")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("SqlHelp");
    assertEquals("SqlHelp", out.getSiteName());
    assertEquals("sql-docs", out.getSiteKey());
    assertEquals(1, out.getPagesWritten().intValue());
    assertEquals(2, out.getFilesCopied().intValue());
    assertEquals(published.getPublishPath(), out.getPublishPath());
    verify(adaptor).publishVirtualSite("SqlHelp");
  }

  @Test
  public void publishVirtualSiteDelegatesHttpJson() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("HttpHelp");
    published.setSiteKey("http-docs");
    published.setPagesWritten(1);
    published.setFilesCopied(2);
    published.setPublishPath(tempDir.resolve("http-pub").toString());
    when(adaptor.publishVirtualSite("HttpHelp")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("HttpHelp");
    assertEquals("HttpHelp", out.getSiteName());
    assertEquals("http-docs", out.getSiteKey());
    assertEquals(1, out.getPagesWritten().intValue());
    assertEquals(2, out.getFilesCopied().intValue());
    assertEquals(published.getPublishPath(), out.getPublishPath());
    verify(adaptor).publishVirtualSite("HttpHelp");
  }

  @Test
  public void publishVirtualSiteDelegatesObjectStorage() {
    VirtualSitePublishResult published = new VirtualSitePublishResult();
    published.setSiteName("ObjHelp");
    published.setSiteKey("obj-docs");
    published.setPagesWritten(1);
    published.setFilesCopied(2);
    published.setPublishPath(tempDir.resolve("obj-pub").toString());
    when(adaptor.publishVirtualSite("ObjHelp")).thenReturn(published);

    VirtualSitePublishResult out = resource.publishVirtualSite("ObjHelp");
    assertEquals("ObjHelp", out.getSiteName());
    assertEquals("obj-docs", out.getSiteKey());
    assertEquals(1, out.getPagesWritten().intValue());
    assertEquals(2, out.getFilesCopied().intValue());
    assertEquals(published.getPublishPath(), out.getPublishPath());
    verify(adaptor).publishVirtualSite("ObjHelp");
  }

  @Test
  public void publishVirtualSiteBlankName400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.publishVirtualSite(" "));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).publishVirtualSite(any());
  }

  /**
   * CodeQL #1949: Jackson/JAXB JSON/XML DTO return must keep same-line {@code // codeql[java/xss]}
   * on the updateVirtual sink (not HTML body construction).
   */
  @Test
  public void updateVirtualReturnSinkHasCodeqlXssAnnotation() throws Exception {
    Path src =
        Path.of("src/main/java/com/percussion/rest/sites/SitesResource.java");
    assertTrue(Files.isRegularFile(src), "SitesResource source must exist for annotation guard");
    String text = Files.readString(src, StandardCharsets.UTF_8);
    assertTrue(
        text.contains(
            "return requireAdaptor().updateVirtualSiteProperties(nameOrId, props); // codeql[java/xss]"),
        "updateVirtualSiteProperties return sink must carry same-line codeql[java/xss]");
    String putVirtualBlock = text.substring(text.indexOf("@Path(\"/{nameOrId}/virtual\")"));
    assertTrue(
        putVirtualBlock.contains("http-json"),
        "updateVirtualProperties OpenAPI description must mention http-json");
    assertTrue(
        text.contains(
            "return requireAdaptor().buildVirtualSite(nameOrId, request); // codeql[java/xss]"),
        "buildVirtualSite return sink must carry same-line codeql[java/xss]");
    assertTrue(
        text.contains("csv-filesystem"),
        "buildVirtualSite OpenAPI description must mention csv-filesystem");
    assertTrue(
        text.contains("sql-database"),
        "buildVirtualSite OpenAPI description must mention sql-database");
    String buildBlock = text.substring(text.indexOf("@Path(\"/{nameOrId}/virtual/build\")"));
    assertTrue(
        buildBlock.contains("http-json"),
        "buildVirtualSite OpenAPI description must mention http-json");
    assertTrue(
        buildBlock.contains("object-storage"),
        "buildVirtualSite OpenAPI description must mention object-storage");
    assertTrue(
        buildBlock.contains("jdbc:h2:mem:"),
        "buildVirtualSite OpenAPI description must mention in-memory H2 jdbc:h2:mem:");
    String publishBlock =
        text.substring(text.indexOf("@Path(\"/{nameOrId}/virtual/publish\")"));
    assertTrue(
        publishBlock.contains("csv-filesystem"),
        "publishVirtualSite OpenAPI description must mention csv-filesystem");
    assertTrue(
        publishBlock.contains("sql-database"),
        "publishVirtualSite OpenAPI description must mention sql-database");
    assertTrue(
        publishBlock.contains("http-json"),
        "publishVirtualSite OpenAPI description must mention http-json");
    assertTrue(
        publishBlock.contains("object-storage"),
        "publishVirtualSite OpenAPI description must mention object-storage");
    assertTrue(
        publishBlock.contains("jdbc:h2:mem:"),
        "publishVirtualSite OpenAPI description must mention in-memory H2 jdbc:h2:mem:");
    assertTrue(
        text.contains(
            "return requireAdaptor().getVirtualSitePreviewStatus(nameOrId); // codeql[java/xss]"),
        "getVirtualSitePreviewStatus return sink must carry same-line codeql[java/xss]");
    String previewStatusBlock =
        text.substring(text.indexOf("@Path(\"/{nameOrId}/virtual/preview\")"));
    String previewFileBlock =
        text.substring(text.indexOf("@Path(\"/{nameOrId}/virtual/preview/{relPath:.*}\")"));
    assertTrue(
        previewStatusBlock.contains("csv-filesystem"),
        "getVirtualSitePreviewStatus OpenAPI description must mention csv-filesystem");
    assertTrue(
        previewStatusBlock.contains("sql-database"),
        "getVirtualSitePreviewStatus OpenAPI description must mention sql-database");
    assertTrue(
        previewStatusBlock.contains("http-json"),
        "getVirtualSitePreviewStatus OpenAPI description must mention http-json");
    assertTrue(
        previewStatusBlock.contains("object-storage"),
        "getVirtualSitePreviewStatus OpenAPI description must mention object-storage");
    assertTrue(
        previewStatusBlock.contains("rss-atom"),
        "getVirtualSitePreviewStatus OpenAPI description must mention rss-atom");
    assertTrue(
        previewFileBlock.contains("csv-filesystem"),
        "previewVirtualSiteFile OpenAPI description must mention csv-filesystem");
    assertTrue(
        previewFileBlock.contains("sql-database"),
        "previewVirtualSiteFile OpenAPI description must mention sql-database");
    assertTrue(
        previewFileBlock.contains("http-json"),
        "previewVirtualSiteFile OpenAPI description must mention http-json");
    assertTrue(
        previewFileBlock.contains("object-storage"),
        "previewVirtualSiteFile OpenAPI description must mention object-storage");
    assertTrue(
        previewFileBlock.contains("rss-atom"),
        "previewVirtualSiteFile OpenAPI description must mention rss-atom");
    assertTrue(
        previewFileBlock.contains("20 MB"),
        "previewVirtualSiteFile OpenAPI description must mention the 20 MB size cap");
    assertTrue(
        text.contains(".build(); // codeql[java/xss]"),
        "previewVirtualSiteFile Response.ok sink must carry same-line codeql[java/xss]");
    assertTrue(
        text.contains("return requireAdaptor().publishVirtualSite(nameOrId); // codeql[java/xss]"),
        "publishVirtualSite return sink must carry same-line codeql[java/xss]");
  }

  @Test
  public void blankNameOrId400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSite("  "));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturns503() {
    SitesResource bare = new SitesResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listSites);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void listSitesWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.findAllSites()).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listSites());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }
}

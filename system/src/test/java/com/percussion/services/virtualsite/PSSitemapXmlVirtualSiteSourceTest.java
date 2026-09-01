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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.virtualsite.VirtualSiteConfig.SitemapSpec;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PSSitemapXmlVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadFromLocalSitemapFixture() throws Exception {
    Path root = writeSite(tempDir.resolve("sm-file"), "sitemap.xml");
    Files.writeString(
        root.resolve("pages").resolve("home.md"),
        "Hello from sitemap",
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("sitemap.xml"),
        urlset("pages/home.md", "2026-08-28"),
        StandardCharsets.UTF_8);
    PSSitemapXmlVirtualSiteSource source = new PSSitemapXmlVirtualSiteSource();
    assertEquals(VirtualSiteSourceType.SITEMAP_XML.wireName(), source.sourceType());
    VirtualSiteConfig cfg = config(root, "sitemap.xml");
    List<VirtualItemRef> refs = source.discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("home", refs.get(0).id());
    assertEquals("home", refs.get(0).title());
    assertEquals(Path.of("8.2", "home.html"), refs.get(0).relativePath());
    VirtualItem item = source.load(cfg, refs.get(0));
    assertEquals("", item.frontmatter().description());
    assertTrue(item.markdownBody().contains("Hello from sitemap"));
    assertTrue(item.markdownBody().contains("Last modified: 2026-08-28"), item.markdownBody());
    assertEquals("home.md", item.absolutePath().getFileName().toString());
  }

  @Test
  void omittedSitemapMappingDefaultsToSitemapXml() throws Exception {
    Path root = writeSite(tempDir.resolve("default-sm"), null);
    Files.createDirectories(root.resolve("pages"));
    Files.writeString(
        root.resolve("pages").resolve("def.md"), "from sitemap.xml", StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve(PSSitemapXmlVirtualSiteSource.DEFAULT_SITEMAP_FILE),
        urlset("pages/def.md", null),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "sm-docs");
    List<VirtualItemRef> refs = new PSSitemapXmlVirtualSiteSource().discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("def", refs.get(0).id());
  }

  @Test
  void sitemapIndexExpandsLocalChild() throws Exception {
    Path root = writeSite(tempDir.resolve("sm-index"), "sitemap.xml");
    Files.createDirectories(root.resolve("pages"));
    Files.writeString(
        root.resolve("pages").resolve("child.md"), "from child sitemap", StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("child-sitemap.xml"),
        urlset("pages/child.md", null),
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("sitemap.xml"),
        sitemapIndex("child-sitemap.xml"),
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs = new PSSitemapXmlVirtualSiteSource().discover(config(root, "sitemap.xml"));
    assertEquals(1, refs.size());
    assertEquals("child", refs.get(0).id());
  }

  @Test
  void blankLocFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("blank-loc"), "sitemap.xml");
    Files.writeString(
        root.resolve("sitemap.xml"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
          <url><loc></loc></url>
        </urlset>
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSSitemapXmlVirtualSiteSource().discover(config(root, "sitemap.xml")));
    assertTrue(ex.getMessage().contains("loc"), ex.getMessage());
  }

  @Test
  void emptyUrlsetFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("empty-sm"), "sitemap.xml");
    Files.writeString(
        root.resolve("sitemap.xml"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"></urlset>
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSSitemapXmlVirtualSiteSource().discover(config(root, "sitemap.xml")));
    assertTrue(ex.getMessage().toLowerCase().contains("url"), ex.getMessage());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("unknown-id"), "sitemap.xml");
    Files.createDirectories(root.resolve("pages"));
    Files.writeString(root.resolve("pages").resolve("known.md"), "Body", StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("sitemap.xml"), urlset("pages/known.md", null), StandardCharsets.UTF_8);
    PSSitemapXmlVirtualSiteSource source = new PSSitemapXmlVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "sitemap.xml");
    source.discover(cfg);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.html"), 0, "x");
    VirtualSiteException ex = assertThrows(VirtualSiteException.class, () -> source.load(cfg, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void duplicateLocsFailClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("dup"), "sitemap.xml");
    Files.createDirectories(root.resolve("pages"));
    Files.writeString(root.resolve("pages").resolve("same.md"), "a", StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("sitemap.xml"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
          <url><loc>pages/same.md</loc></url>
          <url><loc>pages/same.md</loc></url>
        </urlset>
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSSitemapXmlVirtualSiteSource().discover(config(root, "sitemap.xml")));
    assertTrue(ex.getMessage().toLowerCase().contains("duplicate"), ex.getMessage());
  }

  @Test
  void sitemapUrlIsRejected() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-url"), "sitemap.xml");
    Files.createDirectories(root.resolve("pages"));
    Files.writeString(root.resolve("pages").resolve("x.md"), "y", StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("sitemap.xml"), urlset("pages/x.md", null), StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        new VirtualSiteConfig(
            root,
            "Sitemap Docs",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(),
            "sm-docs",
            null,
            null,
            null,
            null,
            null,
            new SitemapSpec("https://example.com/sitemap.xml", "sitemap.xml"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> new PSSitemapXmlVirtualSiteSource().discover(cfg));
    assertTrue(ex.getMessage().contains("sitemap.url"), ex.getMessage());
    assertTrue(
        ex.getMessage().toLowerCase().contains("crawl")
            || ex.getMessage().toLowerCase().contains("remote"),
        ex.getMessage());
  }

  @Test
  void nonLoopbackHttpLocIsRejected() throws Exception {
    Path root = writeSite(tempDir.resolve("cloud-loc"), "sitemap.xml");
    Files.writeString(
        root.resolve("sitemap.xml"),
        urlset("https://example.com/pages/home.html", null),
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSSitemapXmlVirtualSiteSource().discover(config(root, "sitemap.xml")));
    assertTrue(
        ex.getMessage().toLowerCase().contains("loopback")
            || ex.getMessage().toLowerCase().contains("ssrf"),
        ex.getMessage());
  }

  @Test
  void locTraversalFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSSitemapXmlVirtualSiteSource.resolveLocFile(
                    tempDir.resolve("q-escape"), "../outside.md"));
    assertTrue(ex.getMessage().contains("loc") || ex.getMessage().contains(".."), ex.getMessage());
  }

  @Test
  void pathTraversalAndAbsolutePathFailClosed() {
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSSitemapXmlVirtualSiteSource.resolvePagePath(
                    "../outside.html", "esc", "8.2", "test"));
    assertTrue(rel.getMessage().contains("path"), rel.getMessage());
    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSSitemapXmlVirtualSiteSource.resolvePagePath(
                    "/etc/passwd.html", "esc", "8.2", "test"));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());
  }

  @Test
  void sitemapFileTraversalFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSSitemapXmlVirtualSiteSource.resolveSitemapFile(
                    tempDir.resolve("q-escape"), "../outside.xml"));
    assertTrue(ex.getMessage().contains("file") || ex.getMessage().contains(".."), ex.getMessage());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsAbsoluteSitemapFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSSitemapXmlVirtualSiteSource.resolveSitemapFile(
                    tempDir.resolve("win-root"), "C:/docs/evil.xml"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixAbsoluteSitemapFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSSitemapXmlVirtualSiteSource.resolveSitemapFile(
                    tempDir.resolve("unix-root"), "/etc/passwd.xml"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  void loopbackHttpLocAssemblesPage() throws Exception {
    HttpServer server = loopbackServer();
    try {
      serveText(server, "/home.html", "Hello loopback sitemap");
      server.start();
      String loc = loopbackUrl(server, "/home.html");
      Path root = writeSite(tempDir.resolve("sm-http"), "sitemap.xml");
      Files.writeString(root.resolve("sitemap.xml"), urlset(loc, null), StandardCharsets.UTF_8);
      PSSitemapXmlVirtualSiteSource source = new PSSitemapXmlVirtualSiteSource();
      VirtualSiteConfig cfg = config(root, "sitemap.xml");
      List<VirtualItemRef> refs = source.discover(cfg);
      assertEquals(1, refs.size());
      assertEquals("home", refs.get(0).id());
      VirtualItem item = source.load(cfg, refs.get(0));
      assertTrue(item.markdownBody().contains("Hello loopback sitemap"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void requireSafeLoopbackHttpUrlRejectsCloudAndUserinfo() {
    VirtualSiteException userinfo =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSSitemapXmlVirtualSiteSource.requireSafeLoopbackHttpUrl(
                    "http://user:secret@127.0.0.1/page.html"));
    assertTrue(userinfo.getMessage().toLowerCase().contains("userinfo"), userinfo.getMessage());
    VirtualSiteException cloud =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSSitemapXmlVirtualSiteSource.requireSafeLoopbackHttpUrl(
                    "https://example.com/page.html"));
    assertTrue(cloud.getMessage().toLowerCase().contains("loopback"), cloud.getMessage());
  }

  @Test
  void loadRereadsLocalFileWithoutCache() throws Exception {
    Path root = writeSite(tempDir.resolve("live"), "sitemap.xml");
    Files.createDirectories(root.resolve("pages"));
    Path page = root.resolve("pages").resolve("live.md");
    Files.writeString(page, "token-AAA", StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("sitemap.xml"), urlset("pages/live.md", null), StandardCharsets.UTF_8);
    PSSitemapXmlVirtualSiteSource source = new PSSitemapXmlVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "sitemap.xml");
    VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(first.markdownBody().contains("token-AAA"));
    Files.writeString(page, "token-BBB", StandardCharsets.UTF_8);
    VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(second.markdownBody().contains("token-BBB"));
    assertFalse(second.markdownBody().contains("token-AAA"));
  }

  @Test
  void factorySelectsSitemapXmlAndLeavesPeersUnchanged() throws Exception {
    IPSVirtualSiteSource source =
        PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.SITEMAP_XML);
    assertInstanceOf(PSSitemapXmlVirtualSiteSource.class, source);
    assertEquals("sitemap-xml", source.sourceType());
    IPSVirtualSiteSource byName = PSVirtualSiteSourceFactory.createFromWireName("sitemap-xml");
    assertInstanceOf(PSSitemapXmlVirtualSiteSource.class, byName);
    assertInstanceOf(
        PSGitFilesystemVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("git-filesystem"));
    assertInstanceOf(
        PSCsvFilesystemVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("csv-filesystem"));
    assertInstanceOf(
        PSSqlDatabaseVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("sql-database"));
    assertInstanceOf(
        PSHttpJsonVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("http-json"));
    assertInstanceOf(
        PSObjectStorageVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("object-storage"));
    assertInstanceOf(
        PSRssAtomVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("rss-atom"));
    assertInstanceOf(
        PSIcalendarVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("icalendar"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("sitemap-xml"));
    assertTrue(ex.getMessage().contains("icalendar"));
    assertTrue(ex.getMessage().contains("rss-atom"));
  }

  @Test
  void buildServiceFactoryWiresSitemapXmlAndEmitsHtml() throws Exception {
    Path root = writeSite(tempDir.resolve("build-sm"), "sitemap.xml");
    Files.createDirectories(root.resolve("pages"));
    Files.writeString(
        root.resolve("pages").resolve("home.md"),
        "Hello from sitemap.",
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("sitemap.xml"), urlset("pages/home.md", null), StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.SITEMAP_XML);
    assertInstanceOf(PSSitemapXmlVirtualSiteSource.class, service.source());

    PSVirtualSiteBuildResult result = service.build(root, out, "sm-docs");
    assertTrue(result.pageCount() > 0);
    Path html = out.resolve("8.2").resolve("home.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("home"), body);
    assertTrue(body.contains("Hello from sitemap"), body);
  }

  @Test
  void secondBuildAfterSitemapAndConfigEditEmitsUpdatedHtmlWithoutRestart() throws Exception {
    Path root = writeSite(tempDir.resolve("sm-rebuild"), "sitemap.xml");
    writeSitemapYaml(root, "First Site Title", "sitemap.xml");
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${siteTitle}</h1><h2>${pageTitle}</h2>${content}</body></html>",
        StandardCharsets.UTF_8);
    Files.createDirectories(root.resolve("pages"));
    Path page = root.resolve("pages").resolve("live-home.md");
    Files.writeString(page, "unique-token-AAA", StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("sitemap.xml"), urlset("pages/live-home.md", null), StandardCharsets.UTF_8);

    Path out = tempDir.resolve("sm-rebuild-out");
    PSSitemapXmlVirtualSiteSource source = new PSSitemapXmlVirtualSiteSource();
    PSVirtualSiteBuildService service =
        new PSVirtualSiteBuildService(source, new PSInMemoryVirtualParticipantService());

    PSVirtualSiteBuildResult first = service.build(root, out, "sm-docs");
    assertEquals(1, first.pageCount());
    Path html = out.resolve("8.2").resolve("live-home.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String firstHtml = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(firstHtml.contains("First Site Title"), firstHtml);
    assertTrue(firstHtml.contains("unique-token-AAA"), firstHtml);

    Files.writeString(page, "unique-token-BBB", StandardCharsets.UTF_8);
    writeSitemapYaml(root, "Second Site Title", "sitemap.xml");

    VirtualSiteConfig reloaded =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "sm-docs");
    assertEquals("Second Site Title", reloaded.siteTitle());
    assertEquals("sitemap.xml", reloaded.sitemap().file());
    VirtualItem loaded = source.load(reloaded, source.discover(reloaded).get(0));
    assertTrue(loaded.markdownBody().contains("unique-token-BBB"), loaded.markdownBody());
    assertFalse(loaded.markdownBody().contains("unique-token-AAA"), loaded.markdownBody());

    PSVirtualSiteBuildResult second = service.build(root, out, "sm-docs");
    assertEquals(1, second.pageCount());
    String secondHtml = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(secondHtml.contains("Second Site Title"), secondHtml);
    assertTrue(secondHtml.contains("unique-token-BBB"), secondHtml);
    assertFalse(secondHtml.contains("unique-token-AAA"), secondHtml);
    assertFalse(secondHtml.contains("First Site Title"), secondHtml);
    assertNotEquals(firstHtml, secondHtml);
  }

  @Test
  void yamlLoaderParsesSitemapSpec() throws Exception {
    Path root = writeSite(tempDir.resolve("yaml-sm"), "custom-sitemap.xml");
    Files.createDirectories(root.resolve("pages"));
    Files.writeString(root.resolve("pages").resolve("y.md"), "z", StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("custom-sitemap.xml"), urlset("pages/y.md", null), StandardCharsets.UTF_8);
    VirtualSiteConfig loaded =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "sm-docs");
    assertEquals("custom-sitemap.xml", loaded.sitemap().file());
  }

  @Test
  void omittedPathDefaultsToIdHtmlUnderVersion() throws Exception {
    Path relative = PSSitemapXmlVirtualSiteSource.resolvePagePath("", "plain-id", "8.2", "test");
    assertEquals(Path.of("8.2", "plain-id.html"), relative);
  }

  @Test
  void slugForPathStripsUnsafeChars() {
    assertEquals("home", PSSitemapXmlVirtualSiteSource.slugForPath("home.md"));
    assertEquals("hello-world", PSSitemapXmlVirtualSiteSource.slugForPath("hello world.html"));
    assertEquals("page", PSSitemapXmlVirtualSiteSource.slugForPath(":::"));
  }

  private static HttpServer loopbackServer() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(Executors.newCachedThreadPool());
    return server;
  }

  private static void serveText(HttpServer server, String path, String text) {
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    server.createContext(
        path,
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "text/html");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
  }

  private static String loopbackUrl(HttpServer server, String path) {
    int port = server.getAddress().getPort();
    String p = path.startsWith("/") ? path : "/" + path;
    return "http://127.0.0.1:" + port + p;
  }

  private static Path writeSite(Path root, String file) throws Exception {
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    Files.createDirectories(root.resolve("pages"));
    String smBlock;
    if (file != null && !file.isBlank()) {
      smBlock =
          """
          sitemap:
            file: %s
          """
              .formatted(file);
    } else {
      smBlock = "";
    }
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: Sitemap Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        %s
        """
            .formatted(smBlock),
        StandardCharsets.UTF_8);
    return root;
  }

  private static void writeSitemapYaml(Path root, String siteTitle, String file) throws Exception {
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: %s
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        sitemap:
          file: %s
        """
            .formatted(siteTitle, file),
        StandardCharsets.UTF_8);
  }

  private static VirtualSiteConfig config(Path root, String file) {
    SitemapSpec spec = file != null ? new SitemapSpec(null, file) : null;
    return new VirtualSiteConfig(
        root,
        "Sitemap Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "sm-docs",
        null,
        null,
        null,
        null,
        null,
        spec);
  }

  private static String urlset(String loc, String lastmod) {
    String lastmodEl =
        lastmod == null || lastmod.isBlank() ? "" : "<lastmod>" + lastmod + "</lastmod>";
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
          <url>
            <loc>%s</loc>
            %s
          </url>
        </urlset>
        """
        .formatted(loc, lastmodEl);
  }

  private static String sitemapIndex(String childLoc) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
          <sitemap>
            <loc>%s</loc>
          </sitemap>
        </sitemapindex>
        """
        .formatted(childLoc);
  }
}

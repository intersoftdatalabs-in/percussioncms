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

import com.percussion.services.virtualsite.VirtualSiteConfig.RssSpec;
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

class PSRssAtomVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadFromLocalRssFixture() throws Exception {
    Path root = writeSite(tempDir.resolve("rss-file"), null, "feed.xml");
    Files.writeString(root.resolve("feed.xml"), rssFeed("home", "Home", "Hello RSS"), StandardCharsets.UTF_8);
    PSRssAtomVirtualSiteSource source = new PSRssAtomVirtualSiteSource();
    assertEquals(VirtualSiteSourceType.RSS_ATOM.wireName(), source.sourceType());
    VirtualSiteConfig cfg = config(root, null, "feed.xml");
    List<VirtualItemRef> refs = source.discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("home", refs.get(0).id());
    assertEquals("Home", refs.get(0).title());
    assertEquals(Path.of("8.2", "home.html"), refs.get(0).relativePath());
    VirtualItem item = source.load(cfg, refs.get(0));
    assertTrue(item.markdownBody().contains("Hello RSS"));
    assertEquals("feed.xml", item.absolutePath().getFileName().toString());
  }

  @Test
  void discoverAndLoadFromLocalAtomFixture() throws Exception {
    Path root = writeSite(tempDir.resolve("atom-file"), null, "atom.xml");
    Files.writeString(
        root.resolve("atom.xml"), atomFeed("install", "Install", "See home."), StandardCharsets.UTF_8);
    PSRssAtomVirtualSiteSource source = new PSRssAtomVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, null, "atom.xml");
    List<VirtualItemRef> refs = source.discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("install", refs.get(0).id());
    VirtualItem item = source.load(cfg, refs.get(0));
    assertTrue(item.markdownBody().contains("See home."));
  }

  @Test
  void omittedRssMappingDefaultsToFeedXmlThenAtomXml() throws Exception {
    Path root = writeSite(tempDir.resolve("default-feed"), null, null);
    Files.writeString(
        root.resolve(PSRssAtomVirtualSiteSource.DEFAULT_RSS_FILE),
        rssFeed("def", "Default", "from feed.xml"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "rss-docs");
    List<VirtualItemRef> refs = new PSRssAtomVirtualSiteSource().discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("def", refs.get(0).id());
  }

  @Test
  void omittedRssMappingFallsBackToAtomXml() throws Exception {
    Path root = writeSite(tempDir.resolve("default-atom"), null, null);
    Files.writeString(
        root.resolve(PSRssAtomVirtualSiteSource.DEFAULT_ATOM_FILE),
        atomFeed("atom-home", "Atom Home", "from atom.xml"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "rss-docs");
    List<VirtualItemRef> refs = new PSRssAtomVirtualSiteSource().discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("atom-home", refs.get(0).id());
  }

  @Test
  void atomSummaryUsedWhenContentMissing() throws Exception {
    Path root = writeSite(tempDir.resolve("atom-summary"), null, "atom.xml");
    Files.writeString(
        root.resolve("atom.xml"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <title>Sample</title>
          <entry>
            <id>sum</id>
            <title>Summary Page</title>
            <summary>Only a summary.</summary>
          </entry>
        </feed>
        """,
        StandardCharsets.UTF_8);
    VirtualItem item =
        new PSRssAtomVirtualSiteSource()
            .load(config(root, null, "atom.xml"), new VirtualItemRef("sum", "8.2", Path.of("8.2", "sum.html"), 1, "x"));
    assertTrue(item.markdownBody().contains("Only a summary."));
  }

  @Test
  void discoverAndLoadFromLoopbackHttp() throws Exception {
    HttpServer server = loopbackServer();
    try {
      serveXml(server, "/feed.xml", rssFeed("loop", "Loop Home", "Hello loopback"));
      server.start();
      String url = loopbackUrl(server, "/feed.xml");
      Path root = writeSite(tempDir.resolve("rss-http"), url, null);
      PSRssAtomVirtualSiteSource source = new PSRssAtomVirtualSiteSource();
      VirtualSiteConfig cfg = config(root, url, null);
      List<VirtualItemRef> refs = source.discover(cfg);
      assertEquals(1, refs.size());
      assertEquals("loop", refs.get(0).id());
      VirtualItem item = source.load(cfg, refs.get(0));
      assertTrue(item.markdownBody().contains("Hello loopback"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void blankIdOrTitleFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("blank-id"), null, "feed.xml");
    Files.writeString(
        root.resolve("feed.xml"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0"><channel><item><title>MissingId</title><description>x</description></item></channel></rss>
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException idEx =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSRssAtomVirtualSiteSource().discover(config(root, null, "feed.xml")));
    assertTrue(idEx.getMessage().contains("id"), idEx.getMessage());

    Files.writeString(
        root.resolve("feed.xml"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0"><channel><item><guid>has-id</guid><title></title><description>x</description></item></channel></rss>
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException titleEx =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSRssAtomVirtualSiteSource().discover(config(root, null, "feed.xml")));
    assertTrue(titleEx.getMessage().contains("title"), titleEx.getMessage());
  }

  @Test
  void invalidXmlFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("bad-xml"), null, "feed.xml");
    Files.writeString(root.resolve("feed.xml"), "not-xml", StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSRssAtomVirtualSiteSource().discover(config(root, null, "feed.xml")));
    assertTrue(ex.getMessage().toLowerCase().contains("xml"), ex.getMessage());
  }

  @Test
  void doctypeIsRejected() throws Exception {
    Path root = writeSite(tempDir.resolve("dtd"), null, "feed.xml");
    Files.writeString(
        root.resolve("feed.xml"),
        """
        <?xml version="1.0"?>
        <!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
        <rss version="2.0"><channel><item><guid>x</guid><title>T</title><description>&xxe;</description></item></channel></rss>
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSRssAtomVirtualSiteSource().discover(config(root, null, "feed.xml")));
    assertFalse(ex.getMessage().contains("root:"), ex.getMessage());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("unknown-id"), null, "feed.xml");
    Files.writeString(root.resolve("feed.xml"), rssFeed("known", "Known", "Body"), StandardCharsets.UTF_8);
    PSRssAtomVirtualSiteSource source = new PSRssAtomVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, null, "feed.xml");
    source.discover(cfg);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.html"), 0, "x");
    VirtualSiteException ex = assertThrows(VirtualSiteException.class, () -> source.load(cfg, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void duplicateIdsFailClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("dup"), null, "feed.xml");
    Files.writeString(
        root.resolve("feed.xml"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0"><channel>
          <item><guid>same</guid><title>A</title><description>a</description></item>
          <item><guid>same</guid><title>B</title><description>b</description></item>
        </channel></rss>
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSRssAtomVirtualSiteSource().discover(config(root, null, "feed.xml")));
    assertTrue(ex.getMessage().toLowerCase().contains("duplicate"), ex.getMessage());
  }

  @Test
  void pathTraversalAndAbsolutePathFailClosed() {
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () -> PSRssAtomVirtualSiteSource.resolvePagePath("../outside.html", "esc", "8.2", "test"));
    assertTrue(rel.getMessage().contains("path"), rel.getMessage());
    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () -> PSRssAtomVirtualSiteSource.resolvePagePath("/etc/passwd.html", "esc", "8.2", "test"));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());
  }

  @Test
  void catalogFileTraversalFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSRssAtomVirtualSiteSource.resolveFeedFile(
                    tempDir.resolve("q-escape"), "../outside.xml"));
    assertTrue(ex.getMessage().contains("file") || ex.getMessage().contains(".."), ex.getMessage());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsAbsoluteFeedFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSRssAtomVirtualSiteSource.resolveFeedFile(
                    tempDir.resolve("win-root"), "C:/docs/evil.xml"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixAbsoluteFeedFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSRssAtomVirtualSiteSource.resolveFeedFile(
                    tempDir.resolve("unix-root"), "/etc/passwd.xml"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  void rejectsNonHttpSchemesUserinfoCloudAndMetadata() {
    VirtualSiteException file =
        assertThrows(
            VirtualSiteException.class,
            () -> PSRssAtomVirtualSiteSource.requireSafeLoopbackHttpUrl("file:///tmp/feed.xml"));
    assertTrue(file.getMessage().toLowerCase().contains("http"), file.getMessage());
    VirtualSiteException userinfo =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSRssAtomVirtualSiteSource.requireSafeLoopbackHttpUrl(
                    "http://user:secret@127.0.0.1/feed.xml"));
    assertTrue(userinfo.getMessage().toLowerCase().contains("userinfo"), userinfo.getMessage());
    VirtualSiteException cloud =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSRssAtomVirtualSiteSource.requireSafeLoopbackHttpUrl(
                    "https://example.com/feed.xml"));
    assertTrue(cloud.getMessage().toLowerCase().contains("loopback"), cloud.getMessage());
    VirtualSiteException meta =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSRssAtomVirtualSiteSource.requireSafeLoopbackHttpUrl(
                    "http://169.254.169.254/latest/meta-data/"));
    assertTrue(
        meta.getMessage().toLowerCase().contains("ssrf")
            || meta.getMessage().toLowerCase().contains("metadata")
            || meta.getMessage().toLowerCase().contains("reserved")
            || meta.getMessage().toLowerCase().contains("loopback"),
        meta.getMessage());
  }

  @Test
  void bothUrlAndFileFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("both"), "http://127.0.0.1:1/x", "feed.xml");
    Files.writeString(root.resolve("feed.xml"), rssFeed("x", "X", "y"), StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        new VirtualSiteConfig(
            root,
            "RSS Docs",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(),
            "rss-docs",
            null,
            null,
            null,
            new RssSpec("http://127.0.0.1:1/x", "feed.xml"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> new PSRssAtomVirtualSiteSource().discover(cfg));
    assertTrue(ex.getMessage().contains("not both"), ex.getMessage());
  }

  @Test
  void loadRereadsLocalFileWithoutCache() throws Exception {
    Path root = writeSite(tempDir.resolve("live"), null, "feed.xml");
    Path feed = root.resolve("feed.xml");
    Files.writeString(feed, rssFeed("live", "First", "token-AAA"), StandardCharsets.UTF_8);
    PSRssAtomVirtualSiteSource source = new PSRssAtomVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, null, "feed.xml");
    VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(first.markdownBody().contains("token-AAA"));
    Files.writeString(feed, rssFeed("live", "Second", "token-BBB"), StandardCharsets.UTF_8);
    VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
    assertEquals("Second", second.frontmatter().title());
    assertTrue(second.markdownBody().contains("token-BBB"));
    assertFalse(second.markdownBody().contains("token-AAA"));
  }

  @Test
  void factorySelectsRssAtomAndLeavesPeersUnchanged() throws Exception {
    IPSVirtualSiteSource source = PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.RSS_ATOM);
    assertInstanceOf(PSRssAtomVirtualSiteSource.class, source);
    assertEquals("rss-atom", source.sourceType());
    IPSVirtualSiteSource byName = PSVirtualSiteSourceFactory.createFromWireName("rss-atom");
    assertInstanceOf(PSRssAtomVirtualSiteSource.class, byName);
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
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("rss-atom"));
    assertTrue(ex.getMessage().contains("object-storage"));
  }

  @Test
  void buildServiceFactoryWiresRssAtomAndEmitsHtml() throws Exception {
    Path root = writeSite(tempDir.resolve("build-rss"), null, "feed.xml");
    Files.writeString(
        root.resolve("feed.xml"), rssFeed("home", "RSS Home", "Hello from RSS."), StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.RSS_ATOM);
    assertInstanceOf(PSRssAtomVirtualSiteSource.class, service.source());

    PSVirtualSiteBuildResult result = service.build(root, out, "rss-docs");
    assertTrue(result.pageCount() > 0);
    Path html = out.resolve("8.2").resolve("home.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("RSS Home"), body);
    assertTrue(body.contains("Hello from RSS"), body);
  }

  @Test
  void secondBuildAfterFeedAndConfigEditEmitsUpdatedHtmlWithoutRestart() throws Exception {
    Path root = writeSite(tempDir.resolve("rss-rebuild"), null, "feed.xml");
    writeRssYaml(root, "First Site Title", "feed.xml");
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${siteTitle}</h1><h2>${pageTitle}</h2>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path feed = root.resolve("feed.xml");
    Files.writeString(feed, rssFeed("live-home", "First Title", "unique-token-AAA"), StandardCharsets.UTF_8);

    Path out = tempDir.resolve("rss-rebuild-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.RSS_ATOM);
    PSVirtualSiteBuildResult first = service.build(root, out, "rss-docs");
    assertEquals(1, first.pageCount());
    Path html = out.resolve("8.2").resolve("live-home.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String firstHtml = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(firstHtml.contains("First Site Title"), firstHtml);
    assertTrue(firstHtml.contains("unique-token-AAA"), firstHtml);

    Files.writeString(feed, rssFeed("live-home", "Second Title", "unique-token-BBB"), StandardCharsets.UTF_8);
    writeRssYaml(root, "Second Site Title", "feed.xml");

    PSVirtualSiteBuildResult second = service.build(root, out, "rss-docs");
    assertEquals(1, second.pageCount());
    String secondHtml = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(secondHtml.contains("Second Site Title"), secondHtml);
    assertTrue(secondHtml.contains("Second Title"), secondHtml);
    assertTrue(secondHtml.contains("unique-token-BBB"), secondHtml);
    assertFalse(secondHtml.contains("unique-token-AAA"), secondHtml);
    assertNotEquals(firstHtml, secondHtml);
  }

  @Test
  void yamlLoaderParsesRssSpec() throws Exception {
    Path root = writeSite(tempDir.resolve("yaml-rss"), null, "custom.xml");
    Files.writeString(root.resolve("custom.xml"), rssFeed("y", "Y", "z"), StandardCharsets.UTF_8);
    VirtualSiteConfig loaded =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "rss-docs");
    assertEquals("custom.xml", loaded.rss().file());
  }

  @Test
  void omittedPathDefaultsToIdHtmlUnderVersion() throws Exception {
    Path relative = PSRssAtomVirtualSiteSource.resolvePagePath("", "plain-id", "8.2", "test");
    assertEquals(Path.of("8.2", "plain-id.html"), relative);
  }

  @Test
  void slugForPathStripsUrlAndUnsafeChars() {
    assertEquals("hello-world", PSRssAtomVirtualSiteSource.slugForPath("https://example.invalid/hello-world"));
    assertEquals("item", PSRssAtomVirtualSiteSource.slugForPath(":::"));
  }

  private static HttpServer loopbackServer() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(Executors.newCachedThreadPool());
    return server;
  }

  private static void serveXml(HttpServer server, String path, String xml) {
    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    server.createContext(
        path,
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "application/rss+xml");
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

  private static Path writeSite(Path root, String url, String file) throws Exception {
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    String rssBlock;
    if (url != null && !url.isBlank()) {
      rssBlock =
          """
          rss:
            url: "%s"
          """
              .formatted(url);
    } else if (file != null && !file.isBlank()) {
      rssBlock =
          """
          rss:
            file: %s
          """
              .formatted(file);
    } else {
      rssBlock = "";
    }
    Files.writeString(
        root.resolve("_config.yaml"),
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
        %s
        """
            .formatted(rssBlock),
        StandardCharsets.UTF_8);
    return root;
  }

  private static void writeRssYaml(Path root, String siteTitle, String file) throws Exception {
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
        rss:
          file: %s
        """
            .formatted(siteTitle, file),
        StandardCharsets.UTF_8);
  }

  private static VirtualSiteConfig config(Path root, String url, String file) {
    RssSpec spec = url != null || file != null ? new RssSpec(url, file) : null;
    return new VirtualSiteConfig(
        root,
        "RSS Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "rss-docs",
        null,
        null,
        null,
        spec);
  }

  private static String rssFeed(String id, String title, String body) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Sample</title>
            <item>
              <guid>%s</guid>
              <title>%s</title>
              <description>%s</description>
            </item>
          </channel>
        </rss>
        """
        .formatted(id, title, body);
  }

  private static String atomFeed(String id, String title, String body) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <title>Sample</title>
          <entry>
            <id>%s</id>
            <title>%s</title>
            <content type="html">%s</content>
          </entry>
        </feed>
        """
        .formatted(id, title, body);
  }
}

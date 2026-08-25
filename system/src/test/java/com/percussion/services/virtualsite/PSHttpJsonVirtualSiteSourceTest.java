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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.virtualsite.VirtualSiteConfig.HttpSpec;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PSHttpJsonVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadFromLoopbackHttp() throws Exception {
    String catalog =
        """
        {"pages":[
          {"id":"home","path":"index.html","title":"Home","body":"<p>Hello HTTP</p>","order":1},
          {"id":"install","path":"getting-started/install.html","title":"Install","body":"See [Home](id:home).","order":2}
        ]}
        """;
    HttpServer server = loopbackServer();
    try {
      serveJson(server, "/pages.json", catalog);
      server.start();
      String url = loopbackUrl(server, "/pages.json");
      Path root = writeSite(tempDir.resolve("http-site"), url, null);

      PSHttpJsonVirtualSiteSource source = new PSHttpJsonVirtualSiteSource();
      assertEquals(VirtualSiteSourceType.HTTP_JSON.wireName(), source.sourceType());

      VirtualSiteConfig config = config(root, url, null);
      List<VirtualItemRef> refs = source.discover(config);
      assertEquals(2, refs.size());
      assertEquals("home", refs.get(0).id());
      assertEquals("Home", refs.get(0).title());
      assertEquals(1, refs.get(0).order());
      assertEquals(Path.of("8.2", "index.html"), refs.get(0).relativePath());
      assertEquals(Path.of("8.2", "getting-started", "install.html"), refs.get(1).relativePath());

      VirtualItem home = source.load(config, refs.get(0));
      assertEquals("home", home.frontmatter().id());
      assertEquals("Home", home.frontmatter().title());
      assertTrue(home.markdownBody().contains("Hello HTTP"));
      VirtualItem install = source.load(config, refs.get(1));
      assertTrue(install.markdownBody().contains("id:home"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void discoverAndLoadFromLocalFixtureFile() throws Exception {
    Path root = writeSite(tempDir.resolve("file-site"), null, "catalog.json");
    Files.writeString(
        root.resolve("catalog.json"),
        """
        {"pages":[{"id":"plain","title":"Plain","body":"From fixture."}]}
        """,
        StandardCharsets.UTF_8);
    PSHttpJsonVirtualSiteSource source = new PSHttpJsonVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, null, "catalog.json");
    List<VirtualItemRef> refs = source.discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("plain", refs.get(0).id());
    assertEquals(Path.of("8.2", "plain.html"), refs.get(0).relativePath());
    VirtualItem item = source.load(cfg, refs.get(0));
    assertTrue(item.markdownBody().contains("From fixture."));
    assertEquals("catalog.json", item.absolutePath().getFileName().toString());
  }

  @Test
  void omittedHttpMappingDefaultsToPagesJson() throws Exception {
    Path root = writeSite(tempDir.resolve("default-file"), null, null);
    Files.writeString(
        root.resolve(PSHttpJsonVirtualSiteSource.DEFAULT_CATALOG_FILE),
        """
        {"pages":[{"id":"def","title":"Default","body":"pages.json"}]}
        """,
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "http-docs");
    List<VirtualItemRef> refs = new PSHttpJsonVirtualSiteSource().discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("def", refs.get(0).id());
  }

  @Test
  void omittedPathDefaultsToIdHtmlUnderVersion() throws Exception {
    Path relative =
        PSHttpJsonVirtualSiteSource.resolvePagePath("", "plain-id", "8.2", "test");
    assertEquals(Path.of("8.2", "plain-id.html"), relative);
  }

  @Test
  void blankIdOrTitleFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("blank-id"), null, "pages.json");
    Files.writeString(
        root.resolve("pages.json"),
        """
        {"pages":[{"id":"","title":"MissingId","body":"x"}]}
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException idEx =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSHttpJsonVirtualSiteSource().discover(config(root, null, "pages.json")));
    assertTrue(idEx.getMessage().contains("id"), idEx.getMessage());

    Files.writeString(
        root.resolve("pages.json"),
        """
        {"pages":[{"id":"has-id","title":"","body":"x"}]}
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException titleEx =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSHttpJsonVirtualSiteSource().discover(config(root, null, "pages.json")));
    assertTrue(titleEx.getMessage().contains("title"), titleEx.getMessage());
  }

  @Test
  void missingPagesArrayFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("no-pages"), null, "pages.json");
    Files.writeString(root.resolve("pages.json"), "{\"items\":[]}", StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSHttpJsonVirtualSiteSource().discover(config(root, null, "pages.json")));
    assertTrue(ex.getMessage().contains("pages"), ex.getMessage());
  }

  @Test
  void invalidJsonFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("bad-json"), null, "pages.json");
    Files.writeString(root.resolve("pages.json"), "not-json", StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSHttpJsonVirtualSiteSource().discover(config(root, null, "pages.json")));
    assertTrue(ex.getMessage().toLowerCase().contains("json"), ex.getMessage());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("unknown-id"), null, "pages.json");
    Files.writeString(
        root.resolve("pages.json"),
        """
        {"pages":[{"id":"known","title":"Known","body":"Body"}]}
        """,
        StandardCharsets.UTF_8);
    PSHttpJsonVirtualSiteSource source = new PSHttpJsonVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, null, "pages.json");
    source.discover(cfg);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.html"), 0, "x");
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> source.load(cfg, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void pathTraversalAndAbsolutePathFailClosed() {
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSHttpJsonVirtualSiteSource.resolvePagePath(
                    "../outside.html", "esc", "8.2", "test"));
    assertTrue(rel.getMessage().contains("path"), rel.getMessage());

    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSHttpJsonVirtualSiteSource.resolvePagePath(
                    "/etc/passwd.html", "esc", "8.2", "test"));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());
  }

  @Test
  void duplicateIdsFailClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("dup"), null, "pages.json");
    Files.writeString(
        root.resolve("pages.json"),
        """
        {"pages":[
          {"id":"same","title":"A","body":"a"},
          {"id":"same","title":"B","body":"b"}
        ]}
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSHttpJsonVirtualSiteSource().discover(config(root, null, "pages.json")));
    assertTrue(ex.getMessage().toLowerCase().contains("duplicate"), ex.getMessage());
  }

  @Test
  void loadRereadsLocalFileWithoutCache() throws Exception {
    Path root = writeSite(tempDir.resolve("live"), null, "pages.json");
    Path catalog = root.resolve("pages.json");
    Files.writeString(
        catalog,
        """
        {"pages":[{"id":"live","title":"First","body":"token-AAA"}]}
        """,
        StandardCharsets.UTF_8);
    PSHttpJsonVirtualSiteSource source = new PSHttpJsonVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, null, "pages.json");
    VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(first.markdownBody().contains("token-AAA"));

    Files.writeString(
        catalog,
        """
        {"pages":[{"id":"live","title":"Second","body":"token-BBB"}]}
        """,
        StandardCharsets.UTF_8);
    VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
    assertEquals("Second", second.frontmatter().title());
    assertTrue(second.markdownBody().contains("token-BBB"));
    assertFalse(second.markdownBody().contains("token-AAA"));
  }

  @Test
  void catalogFileTraversalFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSHttpJsonVirtualSiteSource.resolveCatalogFile(
                    tempDir.resolve("q-escape"), "../outside.json"));
    assertTrue(
        ex.getMessage().contains("file") || ex.getMessage().contains(".."), ex.getMessage());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsAbsoluteCatalogFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSHttpJsonVirtualSiteSource.resolveCatalogFile(
                    tempDir.resolve("win-root"), "C:/docs/evil.json"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixAbsoluteCatalogFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSHttpJsonVirtualSiteSource.resolveCatalogFile(
                    tempDir.resolve("unix-root"), "/etc/passwd.json"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  void rejectsNonHttpSchemesUserinfoAndMetadata() {
    VirtualSiteException file =
        assertThrows(
            VirtualSiteException.class,
            () -> PSHttpJsonVirtualSiteSource.requireSafeHttpUrl("file:///tmp/pages.json"));
    assertTrue(file.getMessage().toLowerCase().contains("http"), file.getMessage());
    VirtualSiteException ftp =
        assertThrows(
            VirtualSiteException.class,
            () -> PSHttpJsonVirtualSiteSource.requireSafeHttpUrl("ftp://example.com/pages.json"));
    assertTrue(ftp.getMessage().toLowerCase().contains("http"), ftp.getMessage());
    VirtualSiteException userinfo =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSHttpJsonVirtualSiteSource.requireSafeHttpUrl(
                    "http://user:secret@127.0.0.1/pages.json"));
    assertTrue(userinfo.getMessage().toLowerCase().contains("userinfo"), userinfo.getMessage());
    VirtualSiteException meta =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSHttpJsonVirtualSiteSource.requireSafeHttpUrl(
                    "http://169.254.169.254/latest/meta-data/"));
    assertTrue(
        meta.getMessage().toLowerCase().contains("ssrf")
            || meta.getMessage().toLowerCase().contains("metadata")
            || meta.getMessage().toLowerCase().contains("reserved"),
        meta.getMessage());
  }

  @Test
  void rejectsOffLoopbackRedirect() throws Exception {
    HttpServer server = loopbackServer();
    try {
      server.createContext(
          "/redir",
          exchange -> {
            exchange.getResponseHeaders().add("Location", "http://example.com/evil.json");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
          });
      server.start();
      String url = loopbackUrl(server, "/redir");
      Path root = writeSite(tempDir.resolve("redir-off"), url, null);
      VirtualSiteException ex =
          assertThrows(
              VirtualSiteException.class,
              () -> new PSHttpJsonVirtualSiteSource().discover(config(root, url, null)));
      String msg = ex.getMessage().toLowerCase();
      assertTrue(msg.contains("redirect") || msg.contains("loopback") || msg.contains("ssrf"), msg);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void followsSingleLoopbackRedirect() throws Exception {
    String catalog =
        """
        {"pages":[{"id":"redir-home","title":"Redirected","body":"via loopback"}]}
        """;
    HttpServer server = loopbackServer();
    try {
      serveJson(server, "/pages.json", catalog);
      server.createContext(
          "/redir",
          exchange -> {
            String loc = loopbackUrl(server, "/pages.json");
            exchange.getResponseHeaders().add("Location", loc);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
          });
      server.start();
      String url = loopbackUrl(server, "/redir");
      Path root = writeSite(tempDir.resolve("redir-on"), url, null);
      List<VirtualItemRef> refs =
          new PSHttpJsonVirtualSiteSource().discover(config(root, url, null));
      assertEquals(1, refs.size());
      assertEquals("redir-home", refs.get(0).id());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void bothUrlAndFileFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("both"), "http://127.0.0.1:1/x", "pages.json");
    Files.writeString(
        root.resolve("pages.json"),
        """
        {"pages":[{"id":"x","title":"X","body":"y"}]}
        """,
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        new VirtualSiteConfig(
            root,
            "HTTP Docs",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(),
            "http-docs",
            null,
            new HttpSpec("http://127.0.0.1:1/x", "pages.json"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> new PSHttpJsonVirtualSiteSource().discover(cfg));
    assertTrue(ex.getMessage().contains("not both"), ex.getMessage());
  }

  @Test
  void factorySelectsHttpJsonAndLeavesPeersUnchanged() throws Exception {
    IPSVirtualSiteSource source =
        PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.HTTP_JSON);
    assertInstanceOf(PSHttpJsonVirtualSiteSource.class, source);
    assertEquals("http-json", source.sourceType());
    IPSVirtualSiteSource byName = PSVirtualSiteSourceFactory.createFromWireName("http-json");
    assertInstanceOf(PSHttpJsonVirtualSiteSource.class, byName);
    IPSVirtualSiteSource git = PSVirtualSiteSourceFactory.createFromWireName("git-filesystem");
    assertInstanceOf(PSGitFilesystemVirtualSiteSource.class, git);
    IPSVirtualSiteSource csv = PSVirtualSiteSourceFactory.createFromWireName("csv-filesystem");
    assertInstanceOf(PSCsvFilesystemVirtualSiteSource.class, csv);
    IPSVirtualSiteSource sql = PSVirtualSiteSourceFactory.createFromWireName("sql-database");
    assertInstanceOf(PSSqlDatabaseVirtualSiteSource.class, sql);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("http-json"));
  }

  @Test
  void buildServiceFactoryWiresHttpJsonAndEmitsHtml() throws Exception {
    Path root = writeSite(tempDir.resolve("build-http"), null, "pages.json");
    Files.writeString(
        root.resolve("pages.json"),
        """
        {"pages":[{"id":"home","path":"index.html","title":"HTTP Home","body":"Hello from JSON."}]}
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.HTTP_JSON);
    assertInstanceOf(PSHttpJsonVirtualSiteSource.class, service.source());

    PSVirtualSiteBuildResult result = service.build(root, out, "http-docs");
    assertEquals(1, result.pageCount());
    Path html = out.resolve("8.2").resolve("index.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("HTTP Home"), body);
    assertTrue(body.contains("Hello from JSON"), body);
  }

  @Test
  void yamlLoaderParsesHttpSpec() throws Exception {
    HttpServer server = loopbackServer();
    try {
      serveJson(
          server,
          "/catalog.json",
          """
          {"pages":[{"id":"y","title":"Y","body":"z"}]}
          """);
      server.start();
      String url = loopbackUrl(server, "/catalog.json");
      Path root = writeSite(tempDir.resolve("yaml-http"), url, null);
      VirtualSiteConfig loaded =
          VirtualSiteConfigLoader.load(
              root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "http-docs");
      assertEquals(url, loaded.http().url());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void loopbackFetchIsNotCachedAcrossHttpBodies() throws Exception {
    AtomicInteger hits = new AtomicInteger();
    HttpServer server = loopbackServer();
    try {
      server.createContext(
          "/pages.json",
          exchange -> {
            int n = hits.incrementAndGet();
            String json =
                "{\"pages\":[{\"id\":\"live\",\"title\":\"T"
                    + n
                    + "\",\"body\":\"token-"
                    + n
                    + "\"}]}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
              os.write(bytes);
            }
          });
      server.start();
      String url = loopbackUrl(server, "/pages.json");
      Path root = writeSite(tempDir.resolve("http-live"), url, null);
      PSHttpJsonVirtualSiteSource source = new PSHttpJsonVirtualSiteSource();
      VirtualSiteConfig cfg = config(root, url, null);
      VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
      VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
      assertTrue(hits.get() >= 4, "discover+load should refetch: hits=" + hits.get());
      assertFalse(
          first.markdownBody().equals(second.markdownBody()),
          "expected distinct catalog bodies, first="
              + first.markdownBody()
              + " second="
              + second.markdownBody());
    } finally {
      server.stop(0);
    }
  }

  private static HttpServer loopbackServer() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(Executors.newCachedThreadPool());
    return server;
  }

  private static void serveJson(HttpServer server, String path, String json) {
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    server.createContext(
        path,
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "application/json");
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
    String httpBlock;
    if (url != null && !url.isBlank()) {
      httpBlock =
          """
          http:
            url: "%s"
          """
              .formatted(url);
    } else if (file != null && !file.isBlank()) {
      httpBlock =
          """
          http:
            file: %s
          """
              .formatted(file);
    } else {
      httpBlock = "";
    }
    Files.writeString(
        root.resolve("_config.yaml"),
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
    return root;
  }

  private static VirtualSiteConfig config(Path root, String url, String file) {
    HttpSpec spec = url != null || file != null ? new HttpSpec(url, file) : null;
    return new VirtualSiteConfig(
        root,
        "HTTP Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "http-docs",
        null,
        spec);
  }
}

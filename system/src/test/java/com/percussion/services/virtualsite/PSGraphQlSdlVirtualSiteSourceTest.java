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

import com.percussion.services.virtualsite.VirtualSiteConfig.GraphQlSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PSGraphQlSdlVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadFromLocalGraphQlFixture() throws Exception {
    Path root = writeSite(tempDir.resolve("gql-file"), "schema.graphql");
    Files.writeString(
        root.resolve("schema.graphql"),
        sampleSdl("user", "Hello-from-graphql"),
        StandardCharsets.UTF_8);
    PSGraphQlSdlVirtualSiteSource source = new PSGraphQlSdlVirtualSiteSource();
    assertEquals(VirtualSiteSourceType.GRAPHQL_SDL.wireName(), source.sourceType());
    VirtualSiteConfig cfg = config(root, "schema.graphql");
    List<VirtualItemRef> refs = source.discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("user-1", refs.get(0).id());
    assertEquals("Query.user", refs.get(0).title());
    assertEquals(Path.of("8.2", "user-1.html"), refs.get(0).relativePath());
    VirtualItem item = source.load(cfg, refs.get(0));
    assertEquals("", item.frontmatter().description());
    assertTrue(item.markdownBody().contains("Hello-from-graphql") || item.markdownBody().contains("user"),
        item.markdownBody());
    assertTrue(item.markdownBody().contains("Query"), item.markdownBody());
    assertEquals("schema.graphql", item.absolutePath().getFileName().toString());
  }

  @Test
  void omittedGraphQlMappingDefaultsToSchemaGraphql() throws Exception {
    Path root = writeSite(tempDir.resolve("default-gql"), null);
    Files.writeString(
        root.resolve(PSGraphQlSdlVirtualSiteSource.DEFAULT_GRAPHQL_FILE),
        sampleSdl("guide", "notes"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "graphql-docs");
    List<VirtualItemRef> refs = new PSGraphQlSdlVirtualSiteSource().discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("guide-1", refs.get(0).id());
  }

  @Test
  void typesOnlyFixtureStillEmitsOnePage() throws Exception {
    Path root = writeSite(tempDir.resolve("types-only"), "schema.graphql");
    Files.writeString(
        root.resolve("schema.graphql"),
        """
        \"\"\"Product catalog schema\"\"\"
        type User {
          id: ID!
          name: String
        }
        """,
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSGraphQlSdlVirtualSiteSource().discover(config(root, "schema.graphql"));
    assertEquals(1, refs.size());
    assertEquals("graphql", refs.get(0).id());
    assertEquals("Product catalog schema", refs.get(0).title());
    VirtualItem item =
        new PSGraphQlSdlVirtualSiteSource().load(config(root, "schema.graphql"), refs.get(0));
    assertTrue(item.markdownBody().contains("Product catalog schema"), item.markdownBody());
  }

  @Test
  void mapsQueryAndMutationFieldsToPages() throws Exception {
    Path root = writeSite(tempDir.resolve("multi-op"), "schema.graphql");
    Files.writeString(
        root.resolve("schema.graphql"),
        """
        type Query {
          user(id: ID!): User
          users: [User!]!
        }
        type Mutation {
          createUser(name: String!): User
        }
        type User {
          id: ID!
        }
        """,
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSGraphQlSdlVirtualSiteSource().discover(config(root, "schema.graphql"));
    assertEquals(3, refs.size());
    assertEquals("user-1", refs.get(0).id());
    assertEquals("users-2", refs.get(1).id());
    assertEquals("createUser-3", refs.get(2).id());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("unknown-id"), "schema.graphql");
    Files.writeString(root.resolve("schema.graphql"), sampleSdl("user", ""), StandardCharsets.UTF_8);
    PSGraphQlSdlVirtualSiteSource source = new PSGraphQlSdlVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "schema.graphql");
    source.discover(cfg);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.html"), 0, "x");
    VirtualSiteException ex = assertThrows(VirtualSiteException.class, () -> source.load(cfg, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void graphqlUrlIsRejected() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-url"), "schema.graphql");
    Files.writeString(root.resolve("schema.graphql"), sampleSdl("user", ""), StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        new VirtualSiteConfig(
            root,
            "GraphQL Docs",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(),
            "graphql-docs",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new GraphQlSpec("https://example.com/graphql", "schema.graphql"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> new PSGraphQlSdlVirtualSiteSource().discover(cfg));
    assertTrue(ex.getMessage().contains("graphql.url"), ex.getMessage());
    assertTrue(
        ex.getMessage().toLowerCase().contains("live") || ex.getMessage().contains("remote"),
        ex.getMessage());
  }

  @Test
  void remoteImportFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-import"), "schema.graphql");
    Files.writeString(
        root.resolve("schema.graphql"),
        """
        #import "https://example.com/schema.graphql"
        type Query {
          user: User
        }
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSGraphQlSdlVirtualSiteSource().discover(config(root, "schema.graphql")));
    assertTrue(ex.getMessage().toLowerCase().contains("import"), ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("live"), ex.getMessage());
  }

  @Test
  void nonSdlFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("not-sdl"), "schema.graphql");
    Files.writeString(root.resolve("schema.graphql"), "not a graphql schema", StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSGraphQlSdlVirtualSiteSource().discover(config(root, "schema.graphql")));
    assertTrue(ex.getMessage().toLowerCase().contains("graphql"), ex.getMessage());
  }

  @Test
  void pathTraversalAndAbsolutePathFailClosed() {
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSGraphQlSdlVirtualSiteSource.resolvePagePath(
                    "../outside.html", "esc", "8.2", "test"));
    assertTrue(rel.getMessage().contains("path"), rel.getMessage());
    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSGraphQlSdlVirtualSiteSource.resolvePagePath(
                    "/etc/passwd.html", "esc", "8.2", "test"));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());
  }

  @Test
  void graphqlFileTraversalFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSGraphQlSdlVirtualSiteSource.resolveGraphQlFile(
                    tempDir.resolve("q-escape"), "../outside.graphql"));
    assertTrue(ex.getMessage().contains("file") || ex.getMessage().contains(".."), ex.getMessage());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsAbsoluteGraphQlFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSGraphQlSdlVirtualSiteSource.resolveGraphQlFile(
                    tempDir.resolve("win-root"), "C:/docs/evil.graphql"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixAbsoluteGraphQlFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSGraphQlSdlVirtualSiteSource.resolveGraphQlFile(
                    tempDir.resolve("unix-root"), "/etc/passwd.graphql"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  void loadRereadsLocalFileWithoutCache() throws Exception {
    Path root = writeSite(tempDir.resolve("live"), "schema.graphql");
    Path spec = root.resolve("schema.graphql");
    Files.writeString(spec, sampleSdl("firstOp", "token-AAA"), StandardCharsets.UTF_8);
    PSGraphQlSdlVirtualSiteSource source = new PSGraphQlSdlVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "schema.graphql");
    VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(first.markdownBody().contains("firstOp") || first.frontmatter().title().contains("firstOp"));
    Files.writeString(spec, sampleSdl("secondOp", "token-BBB"), StandardCharsets.UTF_8);
    VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
    assertEquals("Query.secondOp", second.frontmatter().title());
    assertTrue(second.markdownBody().contains("secondOp"));
    assertFalse(second.markdownBody().contains("firstOp"));
  }

  @Test
  void factorySelectsGraphQlSdlAndLeavesPeersUnchanged() throws Exception {
    IPSVirtualSiteSource source =
        PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.GRAPHQL_SDL);
    assertInstanceOf(PSGraphQlSdlVirtualSiteSource.class, source);
    assertEquals("graphql-sdl", source.sourceType());
    IPSVirtualSiteSource byName = PSVirtualSiteSourceFactory.createFromWireName("graphql-sdl");
    assertInstanceOf(PSGraphQlSdlVirtualSiteSource.class, byName);
    assertInstanceOf(
        PSAsyncApiYamlVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("asyncapi-yaml"));
    assertInstanceOf(
        PSOpenApiYamlVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("openapi-yaml"));
    assertInstanceOf(
        PSGitFilesystemVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("git-filesystem"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("graphql-sdl"));
    assertTrue(ex.getMessage().contains("asyncapi-yaml"));
  }

  @Test
  void buildServiceFactoryWiresGraphQlSdlAndEmitsHtml() throws Exception {
    Path root = writeSite(tempDir.resolve("build-gql"), "schema.graphql");
    Files.writeString(
        root.resolve("schema.graphql"),
        sampleSdl("user", "Hello-from-graphql"),
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.GRAPHQL_SDL);
    assertInstanceOf(PSGraphQlSdlVirtualSiteSource.class, service.source());

    PSVirtualSiteBuildResult result = service.build(root, out, "graphql-docs");
    assertTrue(result.pageCount() > 0);
    Path html = out.resolve("8.2").resolve("user-1.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("Query.user") || body.contains("user"), body);
  }

  @Test
  void secondBuildAfterGraphQlAndConfigEditEmitsUpdatedHtmlWithoutRestart() throws Exception {
    Path root = writeSite(tempDir.resolve("gql-rebuild"), "schema.graphql");
    writeGraphQlYaml(root, "First Site Title", "schema.graphql");
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${siteTitle}</h1><h2>${pageTitle}</h2>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path spec = root.resolve("schema.graphql");
    Files.writeString(spec, sampleSdl("firstOp", "unique-token-AAA"), StandardCharsets.UTF_8);

    Path out = tempDir.resolve("gql-rebuild-out");
    PSGraphQlSdlVirtualSiteSource source = new PSGraphQlSdlVirtualSiteSource();
    PSVirtualSiteBuildService service =
        new PSVirtualSiteBuildService(source, new PSInMemoryVirtualParticipantService());

    PSVirtualSiteBuildResult first = service.build(root, out, "graphql-docs");
    assertEquals(1, first.pageCount());
    Path firstHtmlPath = out.resolve("8.2").resolve("firstOp-1.html");
    assertTrue(Files.isRegularFile(firstHtmlPath), "missing " + firstHtmlPath);
    String firstHtml = Files.readString(firstHtmlPath, StandardCharsets.UTF_8);
    assertTrue(firstHtml.contains("First Site Title"), firstHtml);

    Files.writeString(spec, sampleSdl("secondOp", "unique-token-BBB"), StandardCharsets.UTF_8);
    writeGraphQlYaml(root, "Second Site Title", "schema.graphql");

    VirtualSiteConfig reloaded =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "graphql-docs");
    assertEquals("Second Site Title", reloaded.siteTitle());
    assertEquals("schema.graphql", reloaded.graphql().file());
    List<VirtualItemRef> refs = source.discover(reloaded);
    assertEquals(1, refs.size());
    assertEquals("secondOp-1", refs.get(0).id());
    VirtualItem loaded = source.load(reloaded, refs.get(0));
    assertEquals("Query.secondOp", loaded.frontmatter().title());

    PSVirtualSiteBuildResult second = service.build(root, out, "graphql-docs");
    assertEquals(1, second.pageCount());
    Path secondHtmlPath = out.resolve("8.2").resolve("secondOp-1.html");
    assertTrue(Files.isRegularFile(secondHtmlPath), "missing " + secondHtmlPath);
    String secondHtml = Files.readString(secondHtmlPath, StandardCharsets.UTF_8);
    assertTrue(secondHtml.contains("Second Site Title"), secondHtml);
    assertFalse(
        Files.exists(firstHtmlPath), "stale " + firstHtmlPath + " should be cleared on full rebuild");
    assertFalse(secondHtml.contains("First Site Title"), secondHtml);
    assertNotEquals(firstHtml, secondHtml);
  }

  @Test
  void yamlLoaderParsesGraphQlSpec() throws Exception {
    Path root = writeSite(tempDir.resolve("yaml-gql"), "custom-schema.graphql");
    Files.writeString(
        root.resolve("custom-schema.graphql"),
        sampleSdl("guide", ""),
        StandardCharsets.UTF_8);
    VirtualSiteConfig loaded =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "graphql-docs");
    assertEquals("custom-schema.graphql", loaded.graphql().file());
  }

  @Test
  void omittedPathDefaultsToIdHtmlUnderVersion() throws Exception {
    Path relative = PSGraphQlSdlVirtualSiteSource.resolvePagePath("", "plain-id", "8.2", "test");
    assertEquals(Path.of("8.2", "plain-id.html"), relative);
  }

  @Test
  void slugForPathStripsUnsafeChars() {
    assertEquals("user", PSGraphQlSdlVirtualSiteSource.slugForPath("user"));
    assertEquals("Query.user", PSGraphQlSdlVirtualSiteSource.slugForPath("Query.user"));
    assertEquals("graphql", PSGraphQlSdlVirtualSiteSource.slugForPath(":::"));
  }

  private static Path writeSite(Path root, String file) throws Exception {
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    String graphqlBlock;
    if (file != null && !file.isBlank()) {
      graphqlBlock =
          """
          graphql:
            file: %s
          """
              .formatted(file);
    } else {
      graphqlBlock = "";
    }
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: GraphQL Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        %s
        """
            .formatted(graphqlBlock),
        StandardCharsets.UTF_8);
    return root;
  }

  private static void writeGraphQlYaml(Path root, String siteTitle, String file) throws Exception {
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
        graphql:
          file: %s
        """
            .formatted(siteTitle, file),
        StandardCharsets.UTF_8);
  }

  private static VirtualSiteConfig config(Path root, String file) {
    GraphQlSpec spec = file != null ? new GraphQlSpec(null, file) : null;
    return new VirtualSiteConfig(
        root,
        "GraphQL Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "graphql-docs",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        spec);
  }

  private static String sampleSdl(String fieldName, String description) {
    String comment = description == null || description.isBlank() ? "" : "# " + description + "\n";
    return """
        %stype Query {
          %s(id: ID!): User
        }
        type User {
          id: ID!
        }
        """
        .formatted(comment, fieldName);
  }
}

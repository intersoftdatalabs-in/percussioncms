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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Behavioral tests for virtual participant registry (upsert / miss / reset / durable). */
class PSInMemoryVirtualParticipantServiceTest {

  @TempDir Path tempDir;

  @Test
  void upsertAndFindRoundTrip() {
    PSInMemoryVirtualParticipantService reg = new PSInMemoryVirtualParticipantService();
    reg.upsert(sample("site-a", "page-one", "8.2/page-one.html"));

    Optional<VirtualParticipant> found = reg.find("site-a", "page-one");
    assertTrue(found.isPresent());
    assertEquals("8.2/page-one.html", found.get().publishedPath());
    assertEquals("8.2", found.get().versionId());
  }

  @Test
  void lookupMissingTargetIsEmpty() {
    PSInMemoryVirtualParticipantService reg = new PSInMemoryVirtualParticipantService();
    reg.upsert(sample("site-a", "page-one", "8.2/page-one.html"));

    assertTrue(reg.find("site-a", "does-not-exist").isEmpty());
    assertTrue(reg.find("other-site", "page-one").isEmpty());
    assertTrue(reg.list("ghost").isEmpty());
  }

  @Test
  void clearResetsSiteAndClearAllResetsRegistry() throws Exception {
    PSInMemoryVirtualParticipantService reg = new PSInMemoryVirtualParticipantService();
    reg.upsert(sample("site-a", "a1", "a.html"));
    reg.upsert(sample("site-b", "b1", "b.html"));

    reg.clear("site-a");
    assertTrue(reg.find("site-a", "a1").isEmpty());
    assertTrue(reg.find("site-b", "b1").isPresent());

    reg.clearAll();
    assertTrue(reg.find("site-b", "b1").isEmpty());
    assertTrue(reg.snapshot().isEmpty());
  }

  @Test
  void durableFlushReloadAndClearDeletesStoreFile() throws Exception {
    Path store = tempDir.resolve("meta");
    PSInMemoryVirtualParticipantService first = new PSInMemoryVirtualParticipantService(store);
    first.upsert(sample("docs", "install-overview", "8.2/getting-started/install.html"));
    first.upsert(
        new VirtualParticipant(
            "docs",
            "path-with-quote",
            "8.2",
            "8.2/path\"with.html",
            "8.2/path\"with.md"));
    first.flush("docs");

    Path jsonl = store.resolve("participants-docs.jsonl");
    assertTrue(Files.isRegularFile(jsonl), "flush must write JSONL under store Path");

    // New process-scoped instance loads from the same portable Path base.
    PSInMemoryVirtualParticipantService second = new PSInMemoryVirtualParticipantService(store);
    Optional<VirtualParticipant> reloaded = second.find("docs", "install-overview");
    assertTrue(reloaded.isPresent());
    assertEquals(
        "8.2/getting-started/install.html", reloaded.get().publishedPath().replace('\\', '/'));
    Optional<VirtualParticipant> quoted = second.find("docs", "path-with-quote");
    assertTrue(quoted.isPresent());
    assertEquals("8.2/path\"with.html", quoted.get().publishedPath());

    second.clear("docs");
    assertFalse(Files.exists(jsonl), "clear must delete durable site file");
    assertTrue(second.find("docs", "install-overview").isEmpty());
  }

  @Test
  void secondBuildReplacesSiteWithoutLosingCurrentIds() throws Exception {
    Path store = tempDir.resolve("meta-rebuild");
    PSInMemoryVirtualParticipantService reg = new PSInMemoryVirtualParticipantService(store);
    reg.upsert(sample("docs", "old-page", "8.2/old.html"));
    reg.upsert(sample("docs", "keep-page", "8.2/keep.html"));
    reg.flush("docs");

    // Simulate full rebuild: clear site, upsert current tree only, flush.
    reg.clear("docs");
    reg.upsert(sample("docs", "keep-page", "8.2/keep.html"));
    reg.upsert(sample("docs", "new-page", "8.2/new.html"));
    reg.flush("docs");

    assertTrue(reg.find("docs", "old-page").isEmpty(), "removed page must not linger");
    assertTrue(reg.find("docs", "keep-page").isPresent());
    assertTrue(reg.find("docs", "new-page").isPresent());

    PSInMemoryVirtualParticipantService reloaded = new PSInMemoryVirtualParticipantService(store);
    assertTrue(reloaded.find("docs", "old-page").isEmpty());
    assertTrue(reloaded.find("docs", "new-page").isPresent());
  }

  @Test
  void jsonLineEscapingRoundTrip() {
    VirtualParticipant p =
        new VirtualParticipant(
            "s\\k", "id\"1", "v", "path\\with\\sep", "src\"x");
    String line = PSInMemoryVirtualParticipantService.toJsonLine(p);
    VirtualParticipant back = PSInMemoryVirtualParticipantService.fromJsonLine(line);
    assertEquals(p.siteKey(), back.siteKey());
    assertEquals(p.stableId(), back.stableId());
    assertEquals(p.publishedPath(), back.publishedPath());
    assertEquals(p.sourcePath(), back.sourcePath());
  }

  @Test
  void sanitizeKeepsSafeFileNames() {
    assertEquals("product-docs", PSInMemoryVirtualParticipantService.sanitize("product-docs"));
    assertEquals("a_b", PSInMemoryVirtualParticipantService.sanitize("a/b"));
  }

  @Test
  void requireSafeStoreDirectoryRejectsTraversalAndEmpty() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSInMemoryVirtualParticipantService.requireSafeStoreDirectory(Path.of("")));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSInMemoryVirtualParticipantService.requireSafeStoreDirectory(Path.of(".")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSInMemoryVirtualParticipantService.requireSafeStoreDirectory(
                Path.of("meta/../../etc")));
    Path ok = tempDir.resolve("meta-ok");
    assertEquals(
        ok.normalize(), PSInMemoryVirtualParticipantService.requireSafeStoreDirectory(ok));
  }

  @Test
  void ctorRejectsUnsafeStoreDirectory() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSInMemoryVirtualParticipantService(Path.of(".")));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSInMemoryVirtualParticipantService(Path.of("a/../../outside")));
  }

  private static VirtualParticipant sample(String site, String id, String published) {
    return new VirtualParticipant(site, id, "8.2", published, published.replace(".html", ".md"));
  }
}

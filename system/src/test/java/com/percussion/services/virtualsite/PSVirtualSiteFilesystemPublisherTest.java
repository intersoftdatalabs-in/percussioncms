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

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.data.PSSite;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("UnitTest")
class PSVirtualSiteFilesystemPublisherTest {

  @TempDir Path tempDir;

  @Test
  void selectFilesystemTarget_requiresConfiguredRoot() {
    PSSite site = virtualSite(tempDir.resolve("src"), null);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteFilesystemPublisher.selectFilesystemTarget(site));
    assertTrue(ex.getMessage().toLowerCase().contains("not configured"));
  }

  @Test
  void selectFilesystemTarget_rejectsUnsafeRoot() {
    PSSite site = virtualSite(tempDir.resolve("src"), Path.of("a", "..", "..", "etc").toString());
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteFilesystemPublisher.selectFilesystemTarget(site));
    assertTrue(ex.getMessage().contains(".."));
  }

  @Test
  void selectFilesystemTarget_rejectsSameAsVirtualSource() {
    Path src = tempDir.resolve("docs").normalize();
    PSSite site = virtualSite(src, src.toString());
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteFilesystemPublisher.selectFilesystemTarget(site));
    assertTrue(ex.getMessage().contains("virtual.rootPath"));
  }

  @Test
  void selectFilesystemTarget_returnsNormalizedSiteRoot() throws Exception {
    Path pub = tempDir.resolve("pub").resolve("site").normalize();
    PSSite site = virtualSite(tempDir.resolve("src"), pub.toString());
    Path selected = PSVirtualSiteFilesystemPublisher.selectFilesystemTarget(site);
    assertEquals(pub, selected);
  }

  @Test
  void copyBuildToTarget_copiesHtmlAndSkipsMeta() throws Exception {
    Path build = tempDir.resolve("build");
    Path pub = tempDir.resolve("published");
    Path html = build.resolve("8.2").resolve("index.html");
    Files.createDirectories(html.getParent());
    Files.writeString(html, "<html>ok</html>", StandardCharsets.UTF_8);
    Files.writeString(build.resolve("link-report.txt"), "clean", StandardCharsets.UTF_8);
    Path meta = build.resolve("_meta").resolve("participants.jsonl");
    Files.createDirectories(meta.getParent());
    Files.writeString(meta, "{}", StandardCharsets.UTF_8);

    PSVirtualSitePublishCopyResult result =
        PSVirtualSiteFilesystemPublisher.copyBuildToTarget(build, pub);

    assertEquals(2, result.filesCopied());
    assertTrue(Files.isRegularFile(pub.resolve("8.2").resolve("index.html")));
    assertEquals(
        "<html>ok</html>",
        Files.readString(pub.resolve("8.2").resolve("index.html"), StandardCharsets.UTF_8)
            .replace("\r\n", "\n"));
    assertTrue(Files.isRegularFile(pub.resolve("link-report.txt")));
    assertFalse(Files.exists(pub.resolve("_meta")));
    assertEquals(pub.toAbsolutePath().normalize(), result.publishRoot());
  }

  @Test
  void copyBuildToTarget_rejectsMissingBuildDir() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSVirtualSiteFilesystemPublisher.copyBuildToTarget(
                    tempDir.resolve("missing-build"), tempDir.resolve("pub")));
    assertTrue(ex.getMessage().toLowerCase().contains("build output"));
  }

  @Test
  void copyBuildToTarget_rejectsTargetThatIsAFile() throws Exception {
    Path build = tempDir.resolve("b");
    Files.createDirectories(build);
    Files.writeString(build.resolve("index.html"), "x", StandardCharsets.UTF_8);
    Path fileTarget = tempDir.resolve("not-a-dir");
    Files.writeString(fileTarget, "nope", StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteFilesystemPublisher.copyBuildToTarget(build, fileTarget));
    assertTrue(ex.getMessage().toLowerCase().contains("not a directory"));
  }

  @Test
  void copyBuildToTarget_rejectsOverlappingTrees() throws Exception {
    Path build = tempDir.resolve("overlap");
    Files.createDirectories(build);
    Files.writeString(build.resolve("index.html"), "x", StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSVirtualSiteFilesystemPublisher.copyBuildToTarget(
                    build, build.resolve("nested-pub")));
    assertTrue(ex.getMessage().toLowerCase().contains("overlap"));
  }

  @Test
  void isMetaTree_onlyTopLevelMeta() {
    assertTrue(PSVirtualSiteFilesystemPublisher.isMetaTree(Path.of("_meta")));
    assertTrue(PSVirtualSiteFilesystemPublisher.isMetaTree(Path.of("_meta", "x.jsonl")));
    assertFalse(PSVirtualSiteFilesystemPublisher.isMetaTree(Path.of("8.2", "index.html")));
    assertFalse(PSVirtualSiteFilesystemPublisher.isMetaTree(null));
  }

  private static PSSite virtualSite(Path virtualRoot, String publishRoot) {
    PSGuid ctx = new PSGuid(PSTypeEnum.CONTEXT, 1L);
    PSSite site = new PSSite();
    site.setName("Help");
    site.setRoot(publishRoot);
    PSVirtualSiteHelper.putProperty(
        site, ctx, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    PSVirtualSiteHelper.putProperty(
        site, ctx, PSVirtualSiteHelper.PROP_ROOT_PATH, virtualRoot.toString());
    return site;
  }
}

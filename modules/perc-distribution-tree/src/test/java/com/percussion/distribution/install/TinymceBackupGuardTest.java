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
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for #7264: restarting a QA cell replays the installer in upgrade mode
 * ({@code clean.install=false, upgrade=true}) and the unconditional {@code <copy>} in
 * {@code backupTinymceRxresources} failed with {@code rx_resources/tinymce does not exist}
 * because fresh installs no longer ship the tree (retired with modules/perc-tinymce, #5734).
 *
 * <p>The target must guard the copy/delete pair on the directory existing, so legacy installs
 * still get the backup while absent trees skip. Uses relative {@link Path} only (cross-platform).
 */
@Tag("UnitTest")
class TinymceBackupGuardTest {

  private static final Path INSTALL_XML =
      Path.of("src", "main", "resources", "distribution", "rxconfig", "Installer", "install.xml");

  @Test
  @DisplayName("backupTinymceRxresources guards the tinymce copy on dir existence (#7264)")
  void backupTargetSkipsWhenTinymceTreeAbsent() throws IOException {
    assertTrue(Files.isRegularFile(INSTALL_XML), "missing " + INSTALL_XML);
    String body = Files.readString(INSTALL_XML, StandardCharsets.UTF_8);

    int target = body.indexOf("name=\"backupTinymceRxresources\"");
    assertTrue(target >= 0, "install.xml must define backupTinymceRxresources target");
    // Bound the target body at the next target definition.
    int nextTarget = body.indexOf("<target", target + 1);
    String block = nextTarget > target ? body.substring(target, nextTarget) : body.substring(target);

    assertTrue(
        block.contains("rx_resources/tinymce_811Upgrade_BAK"),
        "backup target must keep the legacy backup copy");

    // The existence guard must reference the legacy source tree (not the BAK dir).
    int available = block.indexOf("<available");
    assertTrue(available >= 0, "backup target must guard on existence (#7264)");
    int availableEnd = block.indexOf('>', available);
    String availableTag = block.substring(available, availableEnd);
    assertTrue(
        availableTag.contains("rx_resources/tinymce"),
        "guard must reference rx_resources/tinymce (#7264)");
    assertTrue(
        !availableTag.contains("BAK"), "guard must check the source tree, not the backup dir");
    assertTrue(
        availableTag.contains("type=\"dir\"") || availableTag.contains("type='dir'"),
        "existence guard must check for a directory");

    // Structural ordering: guard -> then -> copy -> delete -> /then -> else/skip.
    // (Live Ant proof for both branches is recorded in the PR body build evidence.)
    int thenIdx = block.indexOf("<then>", availableEnd);
    int copyIdx = block.indexOf("<copy", thenIdx);
    int deleteIdx = block.indexOf("<delete", copyIdx);
    int thenEnd = block.indexOf("</then>", deleteIdx);
    int elseIdx = block.indexOf("<else>", thenEnd);
    assertTrue(
        thenIdx > 0 && copyIdx > thenIdx && deleteIdx > copyIdx && thenEnd > deleteIdx,
        "copy/delete pair must sit inside the guarded then-branch");
    assertTrue(elseIdx > thenEnd, "absent tree must take an else/skip branch");
    String thenBlock = block.substring(thenIdx, thenEnd);
    assertTrue(
        thenBlock.contains("tinymce_811Upgrade_BAK"),
        "backup copy must be inside the guarded branch");
    assertTrue(
        block.substring(elseIdx).contains("Skipping tinymce backup"),
        "else branch must log the skip");
  }

  @Test
  @DisplayName("upgrade chain still invokes the tinymce backup/cleanup pair")
  void upgradeChainKeepsBackupWiring() throws IOException {
    assertTrue(Files.isRegularFile(INSTALL_XML), "missing " + INSTALL_XML);
    String body = Files.readString(INSTALL_XML, StandardCharsets.UTF_8);

    assertTrue(body.contains("backupTinymceRxresources"), "backup target must remain");
    assertTrue(
        body.contains("removeRetiredTinymceShippedTree"),
        "retired shipped-tree cleanup must remain wired");
    assertTrue(body.contains("upgrade.chain"), "upgrade.chain target must remain");
  }
}

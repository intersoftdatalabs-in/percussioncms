/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * RET-06 / issue #1820 packaging verification prep for JSF Publishing Design and Runtime deep-page
 * retirement.
 *
 * <p><strong>Hard gate:</strong> this class does <em>not</em> delete product JSPs. Deep-page
 * removal is owned by #1819 (Design) and #1818 (Runtime) after #1371 UAT or explicit human ack on
 * parent #1372. Tests here must stay green on {@code main} while residual deep pages remain
 * packaged.
 *
 * <p>Coverage:
 *
 * <ul>
 *   <li>Entry redirect stubs {@code ui/publishing/index.jsp} and {@code ui/pubruntime/index.jsp}
 *       remain and target modern Publishing shell paths.
 *   <li>Current residual JSP inventory is frozen (exact basename set) so accidental adds/removes
 *       surface in packaging tests.
 *   <li>{@link Disabled} absence assertions ready to enable after #1819/#1818 land.
 *   <li>Installer peer still lists {@code publishing-faces-config.xml} for upgrade cleanup
 *       (lockstep with {@link ObsoleteWebInfArtifactsCleanupTest}).
 * </ul>
 *
 * <p>Paths use {@link Path#of(String, String...)} / {@link Path#resolve(String)} only (portable
 * Windows/Unix). Redirect targets are URL strings with {@code '/'} separators only.
 *
 * <p>Inventory checklist: {@code specs/990-unified-publishing-ui/checklists/removal-inventory.md}.
 */
@Tag("UnitTest")
class PublishingDeepPagePackagingTest {

  /** Relative to module CWD when Surefire runs standalone {@code clean install}. */
  private static final Path WEB_UI_WEBAPP = Path.of("..", "..", "WebUI", "src", "main", "webapp");

  private static final Path DESIGN_DIR = WEB_UI_WEBAPP.resolve(Path.of("ui", "publishing"));

  private static final Path RUNTIME_DIR = WEB_UI_WEBAPP.resolve(Path.of("ui", "pubruntime"));

  private static final String INSTALL_XML = "/distribution/rxconfig/Installer/install.xml";

  /** Modern shell targets — must match the redirect stubs on disk. */
  static final String DESIGN_REDIRECT_TARGET = "/cm/app/?view=publish&section=design";

  static final String RUNTIME_REDIRECT_TARGET = "/cm/app/?view=publish&section=runtime";

  /**
   * Frozen inventory of Design-tree JSPs still packaged on {@code main} (audit #1817 / inventory
   * 2026-08-04). Update only when #1819 deletes exclusive deep pages (or intentional KEEP changes).
   */
  static final Set<String> FROZEN_DESIGN_JSPS =
      Set.of(
          "AddContextVariable.jsp",
          "AssociateContentlist.jsp",
          "ContentlistEditor.jsp",
          "ContentlistView.jsp",
          "ContextEditor.jsp",
          "ContextList.jsp",
          "DeliveryTypeEditor.jsp",
          "DeliveryTypeList.jsp",
          "EditionEditor.jsp",
          "EditionList.jsp",
          "error.jsp",
          "index.jsp",
          "ItemBrowser.jsp",
          "LocationSchemeEditor.jsp",
          "LocationSchemeLegacyEditor.jsp",
          "LocationSchemeParamEditor.jsp",
          "menu.jsp",
          "NoSchemeParameterSelectionWarning.jsp",
          "NoSelectionWarning.jsp",
          "PubDesignAuthentication.jsp",
          "publish.jsp",
          "RemoveConfirmation.jsp",
          "RemoveLocationScheme.jsp",
          "SaveChildSchemeChangesWarning.jsp",
          "SelectEditionFromOtherSite.jsp",
          "SiteEditor.jsp",
          "SiteList.jsp",
          "SiteRootBrowser.jsp");

  /**
   * Frozen inventory of Runtime-tree JSPs still packaged on {@code main} (audit #1817 / inventory
   * 2026-08-04). Update only when #1818 deletes exclusive deep pages.
   */
  static final Set<String> FROZEN_RUNTIME_JSPS =
      Set.of(
          "ActiveJobStatus.jsp",
          "AllPubLogs.jsp",
          "DeleteSiteItemLogsWarning.jsp",
          "DemandPublish.jsp",
          "ErrorMessage.jsp",
          "index.jsp",
          "ItemPubLog.jsp",
          "JobPubLog.jsp",
          "NoSelectionWarning.jsp",
          "PubRuntimeAuthentication.jsp",
          "RuntimeEdition.jsp",
          "RuntimeEditionList.jsp",
          "SitePubLogs.jsp");

  /**
   * Basenames that must remain after Design deep-page retirement (#1819). Today only the modern
   * Design redirect stub is a hard KEEP; {@code publish.jsp} is optional KEEP per inventory.
   */
  static final Set<String> POST_RETIREMENT_DESIGN_KEEP = Set.of("index.jsp");

  /** Basenames that must remain after Runtime deep-page retirement (#1818). */
  static final Set<String> POST_RETIREMENT_RUNTIME_KEEP = Set.of("index.jsp");

  // --- Redirect stubs ---------------------------------------------------

  @Test
  @DisplayName("ui/publishing/index.jsp redirect stub targets modern Design shell")
  void designIndexRedirectStubTargetsModernShell() throws IOException {
    Path index = DESIGN_DIR.resolve("index.jsp");
    assertTrue(Files.isRegularFile(index), "missing Design redirect stub: " + abs(index));
    String text = Files.readString(index, StandardCharsets.UTF_8);
    assertTrue(
        text.contains(DESIGN_REDIRECT_TARGET),
        "Design index.jsp must redirect to "
            + DESIGN_REDIRECT_TARGET
            + "; content snippet="
            + snippet(text));
    assertTrue(text.contains("301") || text.contains("setStatus(301)"), "must send HTTP 301");
    assertFalse(text.contains("\\"), "redirect Location must use URL '/' separators only");
  }

  @Test
  @DisplayName("ui/pubruntime/index.jsp redirect stub targets modern Runtime shell")
  void runtimeIndexRedirectStubTargetsModernShell() throws IOException {
    Path index = RUNTIME_DIR.resolve("index.jsp");
    assertTrue(Files.isRegularFile(index), "missing Runtime redirect stub: " + abs(index));
    String text = Files.readString(index, StandardCharsets.UTF_8);
    assertTrue(
        text.contains(RUNTIME_REDIRECT_TARGET),
        "Runtime index.jsp must redirect to "
            + RUNTIME_REDIRECT_TARGET
            + "; content snippet="
            + snippet(text));
    assertTrue(text.contains("301") || text.contains("setStatus(301)"), "must send HTTP 301");
    assertFalse(text.contains("\\"), "redirect Location must use URL '/' separators only");
  }

  // --- Inventory freeze (must pass on main today) -----------------------

  @Test
  @DisplayName("Design deep-page inventory matches frozen expected set (no accidental drift)")
  void designDeepPageInventoryIsFrozen() throws IOException {
    assertInventoryFreeze(DESIGN_DIR, FROZEN_DESIGN_JSPS, "ui/publishing");
  }

  @Test
  @DisplayName("Runtime deep-page inventory matches frozen expected set (no accidental drift)")
  void runtimeDeepPageInventoryIsFrozen() throws IOException {
    assertInventoryFreeze(RUNTIME_DIR, FROZEN_RUNTIME_JSPS, "ui/pubruntime");
  }

  // --- Installer faces-config peer --------------------------------------

  @Test
  @DisplayName("install.xml still deletes publishing-faces-config.xml on upgrade (peer cleanup)")
  void installXmlStillCleansPublishingFacesConfig() throws IOException {
    try (InputStream in = getClass().getResourceAsStream(INSTALL_XML)) {
      assertNotNull(in, INSTALL_XML + " must be on the test classpath");
      String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(
          xml.contains("publishing-faces-config.xml"),
          "install.xml must keep upgrade delete of publishing-faces-config.xml"
              + " (peer ObsoleteWebInfArtifactsCleanupTest / #1817)");
      assertTrue(
          xml.contains("deleteObsoleteRhythmyxWebInfArtifacts"),
          "install.xml must retain deleteObsoleteRhythmyxWebInfArtifacts target");
    }
  }

  // --- Future absence (enable after #1819 / #1818) ----------------------

  /**
   * Enable after #1819 lands: exclusive Design deep pages must be gone; only intentional KEEP files
   * (redirect stub, optional publish.jsp) remain.
   */
  @Test
  @Disabled(
      "Enable after #1819 Design deep-page delete lands; until then inventory freeze above is the"
          + " live contract. Parent #1372 / UAT #1371 gate product deletes.")
  @DisplayName("AFTER #1819: exclusive Design deep pages absent; redirect stub remains")
  void afterDesignRetirementOnlyKeepFilesRemain() throws IOException {
    assertOnlyKeepFilesRemain(DESIGN_DIR, POST_RETIREMENT_DESIGN_KEEP, "ui/publishing");
    // Redirect stub still targets modern Design
    designIndexRedirectStubTargetsModernShell();
  }

  /**
   * Enable after #1818 lands: exclusive Runtime deep pages must be gone; only the Runtime redirect
   * stub remains.
   */
  @Test
  @Disabled(
      "Enable after #1818 Runtime deep-page delete lands; until then inventory freeze above is the"
          + " live contract. Parent #1372 / UAT #1371 gate product deletes.")
  @DisplayName("AFTER #1818: exclusive Runtime deep pages absent; redirect stub remains")
  void afterRuntimeRetirementOnlyKeepFilesRemain() throws IOException {
    assertOnlyKeepFilesRemain(RUNTIME_DIR, POST_RETIREMENT_RUNTIME_KEEP, "ui/pubruntime");
    runtimeIndexRedirectStubTargetsModernShell();
  }

  // --- helpers ----------------------------------------------------------

  private static void assertInventoryFreeze(Path dir, Set<String> expected, String label)
      throws IOException {
    assertTrue(Files.isDirectory(dir), "missing " + label + " tree: " + abs(dir));
    Set<String> actual = listJspBasenames(dir);
    Set<String> missing = new TreeSet<>(expected);
    missing.removeAll(actual);
    Set<String> unexpected = new TreeSet<>(actual);
    unexpected.removeAll(expected);
    if (!missing.isEmpty() || !unexpected.isEmpty()) {
      fail(
          label
              + " JSP inventory drifted from freeze (issue #1820 / removal-inventory.md)."
              + " missing="
              + missing
              + " unexpected="
              + unexpected
              + " actual="
              + actual
              + " — update freeze only with intentional #1819/#1818 or KEEP changes.");
    }
    assertEquals(expected.size(), actual.size(), label + " inventory size must match freeze");
  }

  private static void assertOnlyKeepFilesRemain(Path dir, Set<String> keep, String label)
      throws IOException {
    assertTrue(Files.isDirectory(dir), "missing " + label + " tree: " + abs(dir));
    Set<String> actual = listJspBasenames(dir);
    for (String required : keep) {
      assertTrue(
          actual.contains(required),
          label + " must retain KEEP file " + required + "; actual=" + actual);
    }
    Set<String> extras = new TreeSet<>(actual);
    extras.removeAll(keep);
    // publish.jsp is an optional Design KEEP per inventory — allow it without failing absence.
    if ("ui/publishing".equals(label)) {
      extras.remove("publish.jsp");
    }
    assertTrue(
        extras.isEmpty(),
        label
            + " must not package exclusive deep pages after retirement; leftovers="
            + extras
            + " (keep="
            + keep
            + ")");
  }

  private static Set<String> listJspBasenames(Path dir) throws IOException {
    try (Stream<Path> stream = Files.list(dir)) {
      return stream
          .filter(Files::isRegularFile)
          .map(p -> p.getFileName().toString())
          .filter(name -> name.endsWith(".jsp"))
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }
  }

  private static String abs(Path p) {
    return p.toAbsolutePath().normalize().toString();
  }

  private static String snippet(String text) {
    String oneLine = text.replace('\r', ' ').replace('\n', ' ').trim();
    return oneLine.length() <= 160 ? oneLine : oneLine.substring(0, 160) + "...";
  }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Widens the zero-webjar/zero-CDN guard pins of slice 3 (issue #7167,
 * {@code webjarBuildCopiedAudit.test.js} under WebUI) to the distribution-tree
 * build-copied surface, audited under slice 4C of parent audit #5627 (issue
 * #7366).
 *
 * <p>{@code perc-distribution-tree} assembles the shippable CMS distribution: it unpacks the
 * WARs, copies the npm-built jslib trees into {@code web_resources/cm} and the feature
 * gadgets, and lays the {@code InstallDir} + {@code Packages} payloads verbatim into the
 * image. The 4C audit verified every JavaScript copy entry in {@code
 * installDistributionFiles.xml} reads from an npm-built tree (jQuery, jQuery UI,
 * jquery-migrate, bowser es5, jquery.cookie, FancyTree, jQuery Validate, FontAwesome —
 * each with an npm-migration issue reference in the surrounding XML comments) and found
 * zero WebJar-unpack steps, zero CDN loads, and zero checked-in {@code .js} of its own
 * under {@code src/main/resources/distribution}. This test pins that verified_no_change
 * state so any future WebJar/CDN-stitched copy fails loudly at build time instead of
 * shipping silently.
 *
 * <p>Explicitly out of scope (no double-count): the copied bytes themselves are owned by
 * their source guards — WebUI jslib/cm trees (slice 1, #7165), the installer {@code .js}
 * payload + JSP CDN pins (slice 2, #6069, plus the 4C installer guard {@code
 * InstallerPayloadCdnGuardTest}), the perc-packages widget payloads (#6319, plus the 4C
 * packages guard {@code WidgetPayloadCdnGuardTest}), and the war/api stays-deleted set
 * (slice 3, #7167/#7280).
 */
@Tag("UnitTest")
class BuildCopiedZeroCdnWebjarGuardTest {

  private static final Path ASSEMBLY_XML =
      Path.of("src", "main", "resources", "installDistributionFiles.xml");

  private static final Path DISTRIBUTION_RESOURCES =
      Path.of("src", "main", "resources", "distribution");

  /** CDN hosts that must never appear in a build-copied load on this surface. */
  private static final List<String> BANNED_CDN_HOSTS =
      List.of(
          "ajax.googleapis.com",
          "code.jquery.com",
          "unpkg.com",
          "cdnjs.cloudflare.com",
          "cdn.jsdelivr.net");

  private static final Pattern SCRIPT_SRC_REMOTE =
      Pattern.compile(
          "<script\\b[^>]*\\bsrc\\s*=\\s*[\"']((?:https?:)?//[^\"']+)[\"']",
          Pattern.CASE_INSENSITIVE);

  private static void assertModuleLayout() {
    assertTrue(
        Files.isRegularFile(ASSEMBLY_XML),
        "installDistributionFiles.xml must exist (test runs from the module directory)");
    assertTrue(
        Files.isDirectory(DISTRIBUTION_RESOURCES),
        "src/main/resources/distribution must exist (test runs from the module directory)");
  }

  @Test
  void noCheckedInJavaScriptInDistributionResources() throws IOException {
    // This surface copies JS; it must not author any. A checked-in .js here
    // would ship to customers invisible to npm audit/Dependabot.
    assertModuleLayout();
    List<String> js;
    try (Stream<Path> walk = Files.walk(DISTRIBUTION_RESOURCES)) {
      js =
          walk.filter(Files::isRegularFile)
              .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".js"))
              .map(p -> DISTRIBUTION_RESOURCES.relativize(p).toString().replace(java.io.File.separatorChar, '/'))
              .sorted()
              .collect(Collectors.toList());
    }
    assertEquals(
        List.of(),
        js,
        "No checked-in .js may live under src/main/resources/distribution "
            + "(build-copied JS arrives only via the assembly copy entries; see #5627).");
  }

  @Test
  void assemblyHasNoWebjarOrCdnStitching() throws IOException {
    // The assembly must never unpack a WebJar or fetch/point at a CDN: every
    // JS copy entry reads from an npm-built tree inside the assembly image.
    assertModuleLayout();
    String xml = Files.readString(ASSEMBLY_XML, StandardCharsets.UTF_8);
    String lower = xml.toLowerCase(Locale.ROOT);
    List<String> violations = new ArrayList<>();
    if (lower.contains("webjar") || lower.contains("org.webjars")) {
      violations.add("webjar token in installDistributionFiles.xml");
    }
    Matcher m = SCRIPT_SRC_REMOTE.matcher(xml);
    while (m.find()) {
      violations.add("remote <script src> in assembly: " + m.group(1));
    }
    for (String host : BANNED_CDN_HOSTS) {
      if (lower.contains(host)) {
        violations.add("banned CDN host in assembly: " + host);
      }
    }
    assertEquals(
        List.of(),
        violations,
        "installDistributionFiles.xml must stitch only npm-built trees (see #5627).");
  }

  @Test
  void installerXmlsCarryNoRemoteScriptLoads() throws IOException {
    // The Installer/*.xml descriptors ship Ant install logic, not web pages;
    // a remote <script src> or CDN host here would be a supply-chain load.
    assertModuleLayout();
    List<String> violations = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(DISTRIBUTION_RESOURCES)) {
      for (Path file :
          walk.filter(Files::isRegularFile)
              .filter(
                  p -> {
                    String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    return name.endsWith(".xml") || name.endsWith(".xsl");
                  })
              .sorted()
              .collect(Collectors.toList())) {
        String rel =
            DISTRIBUTION_RESOURCES.relativize(file).toString().replace(java.io.File.separatorChar, '/');
        String text = Files.readString(file, StandardCharsets.UTF_8);
        Matcher m = SCRIPT_SRC_REMOTE.matcher(text);
        while (m.find()) {
          violations.add(rel + " remote <script src>: " + m.group(1));
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String host : BANNED_CDN_HOSTS) {
          if (lower.contains(host)) {
            violations.add(rel + " banned CDN host: " + host);
          }
        }
      }
    }
    assertEquals(
        List.of(),
        violations,
        "distribution Installer XMLs must carry no remote script loads (see #5627).");
  }

  @Test
  void noWebjarDependencyInModulePom() throws IOException {
    // Widens the slice-3 zero-org.webjars pin (#7167, enforced on the root
    // pom by webjarBuildCopiedAudit.test.js) to this build-copied surface:
    // the distribution assembles npm-built trees, never WebJar artifacts.
    assertModuleLayout();
    Path pom = Path.of("pom.xml");
    assertTrue(Files.isRegularFile(pom), "modules/perc-distribution-tree/pom.xml must exist");
    String text = Files.readString(pom, StandardCharsets.UTF_8);
    assertTrue(
        !text.contains("org.webjars"),
        "modules/perc-distribution-tree must not declare org.webjars dependencies "
            + "(distribution JS arrives via npm-built trees; see #5627).");
  }
}

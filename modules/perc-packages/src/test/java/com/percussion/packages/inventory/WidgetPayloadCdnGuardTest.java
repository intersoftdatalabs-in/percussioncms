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
package com.percussion.packages.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Widens the zero-webjar/zero-CDN guard pins of slice 3 (issue #7167,
 * {@code webjarBuildCopiedAudit.test.js} under WebUI) to the perc-packages
 * build-copied surface, audited under slice 4C of parent audit #5627 (issue
 * #7366).
 *
 * <p>The {@code Packages} tree under {@code modules/perc-packages/src/main/resources} installs
 * verbatim onto every CMS and publishes to live sites, so any {@code <script src="http(s)://...>}
 * load checked in here fetches third-party code from a CDN at render time — invisible to
 * {@code npm audit} and Dependabot. The {@code .js} payload inventory itself stays owned by
 * {@link WidgetPayloadVendoredJsAuditTest} (#6319); this test pins only the CDN-load surface.
 *
 * <p>The 4C audit found exactly one live CDN script load: {@code
 * perc.widget.form/.../widgets/form/js/PercFormView.js} injects {@code
 * https://www.google.com/recaptcha/api.js} (async + defer) when the operator enables reCAPTCHA
 * on a form widget. That load is a documented functional exception, not a vendored library:
 * the reCAPTCHA API is key-bound to Google's endpoint and cannot be self-hosted or npm-served,
 * so no migration issue is filed for it — this test pins it as the single allowlisted row so
 * any <em>additional</em> CDN load fails loudly. Documentation hyperlinks ({@code <a href>}
 * to support.google.com / console.developers.google.com) and code comments that mention CDN
 * hostnames (notably the qTip build notes) are not script loads and are excluded by matching
 * only {@code <script ... src=} occurrences.
 *
 * <p>Explicitly out of scope (no double-count): WebUI jslib/cm trees (slice 1, #7165), DTS
 * web resources + installer payloads (slice 2, #7166, plus the 4C installer guard {@code
 * InstallerPayloadCdnGuardTest}), and the dist-tree assembly copies (4C guard {@code
 * BuildCopiedZeroCdnWebjarGuardTest}).
 */
public class WidgetPayloadCdnGuardTest {

  /**
   * The single allowlisted live CDN {@code <script src>} load in the widget payload, as
   * {@code relative/path -> url} (line-independent on purpose: unrelated edits above the pin
   * must not trip the guard; only the inventory matters). Pinned by the 4C audit (#7366);
   * a second row means a new CDN dependency that must go through audit #5627 first.
   */
  private static final List<String> EXPECTED_CDN_SCRIPT_REFS =
      List.of(
          "perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormView.js -> https://www.google.com/recaptcha/api.js");

  /** File types that can carry a {@code <script src>} load in this payload. */
  private static final Set<String> SCANNED_EXTENSIONS =
      Set.of(".js", ".xsl", ".vm", ".xml", ".html", ".htm", ".jsp", ".jspx");

  private static final Pattern SCRIPT_SRC_CDN =
      Pattern.compile(
          "<script\\b[^>]*\\bsrc\\s*=\\s*[\"']((?:https?:)?//[^\"']+)[\"']",
          Pattern.CASE_INSENSITIVE);

  private static Path packagesRoot() {
    List<Path> candidates =
        List.of(
            Path.of(System.getProperty("user.dir"), "src", "main", "resources", "Packages"),
            Path.of(
                System.getProperty("user.dir"),
                "modules",
                "perc-packages",
                "src",
                "main",
                "resources",
                "Packages"));
    for (Path candidate : candidates) {
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
    }
    fail("Packages payload not found under user.dir=" + System.getProperty("user.dir"));
    throw new IllegalStateException("unreachable");
  }

  private static List<String> collectCdnScriptRefs(Path root) throws IOException {
    List<String> refs = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(root)) {
      for (Path file :
          walk.filter(Files::isRegularFile)
              .filter(
                  p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    int dot = name.lastIndexOf('.');
                    return dot >= 0 && SCANNED_EXTENSIONS.contains(name.substring(dot));
                  })
              .sorted()
              .collect(Collectors.toList())) {
        String rel = root.relativize(file).toString().replace(java.io.File.separatorChar, '/');
        // Match against the whole file, not per line: a <script> tag split
        // across lines (ordinary HTML formatting) must not bypass the guard
        // (Codex review on #7366, [P2] — fixed before PR).
        String content = Files.readString(file, StandardCharsets.UTF_8);
        Matcher m = SCRIPT_SRC_CDN.matcher(content);
        while (m.find()) {
          refs.add(rel + " -> " + m.group(1));
        }
      }
    }
    return refs.stream().sorted().collect(Collectors.toList());
  }

  @Test
  void cdnScriptInventoryMatchesAudit7366() throws IOException {
    List<String> actual = collectCdnScriptRefs(packagesRoot());
    assertEquals(
        EXPECTED_CDN_SCRIPT_REFS.stream().sorted().collect(Collectors.toList()),
        actual,
        "Widget-payload CDN <script src> inventory drifted from the #7366 audit. "
            + "Only the operator-gated reCAPTCHA API load is allowlisted; any new CDN load "
            + "must go through audit #5627 (migration issue + npm tracking) first.");
  }

  @Test
  void recaptchaExceptionStaysOperatorGated() throws IOException {
    // The allowlisted row must keep its operator gate: the reCAPTCHA script
    // is injected only inside the `IsRecaptchaEnabled === 1` branch, async +
    // defer so it never blocks render. Structural check, not an identifier
    // spot-check (Codex review on #7366, [P2]): flipping the condition to
    // `!== 1` or hoisting the injection out of the branch must fail loudly.
    Path formView =
        packagesRoot()
            .resolve("perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormView.js");
    assertTrue(Files.isRegularFile(formView), "form widget PercFormView.js must still ship");
    String content = Files.readString(formView, StandardCharsets.UTF_8);
    assertTrue(
        !content.contains("IsRecaptchaEnabled !=="),
        "reCAPTCHA gate must not be negated (script would load when disabled)");
    Matcher gatedBranch =
        Pattern.compile(
                "if\\s*\\([^)]*IsRecaptchaEnabled\\s*===\\s*1[^)]*\\)\\s*\\{([^}]*)\\}",
                Pattern.DOTALL)
            .matcher(content);
    assertTrue(
        gatedBranch.find(), "reCAPTCHA injection must sit in an `IsRecaptchaEnabled === 1` branch");
    assertTrue(
        gatedBranch.group(1).contains("https://www.google.com/recaptcha/api.js"),
        "reCAPTCHA API load must stay inside the enabled branch");
    assertTrue(
        gatedBranch.group(1).contains("async defer"),
        "reCAPTCHA script tag must stay async + defer");
  }

  @Test
  void multilineScriptTagsDoNotBypassTheGuard() {
    // Regression lock for the Codex [P2] on #7366: the scanner matches whole
    // file contents, so a <script> tag split across lines trips it.
    String sample =
        "<html>\n<head>\n<script\n  src=\"https://cdn.jsdelivr.net/npm/jquery/dist/jquery.min.js\">\n"
            + "</script>\n</head>\n</html>";
    Matcher m = SCRIPT_SRC_CDN.matcher(sample);
    assertTrue(m.find(), "multiline <script src> tag must be detected");
    assertEquals("https://cdn.jsdelivr.net/npm/jquery/dist/jquery.min.js", m.group(1));
  }

  @Test
  void noWebjarDependencyInModulePom() throws IOException {
    // Widens the slice-3 zero-org.webjars pin (#7167, enforced on the root
    // pom by webjarBuildCopiedAudit.test.js) to this build-copied surface:
    // widget payload JS arrives as checked-in files, never via WebJar
    // unpack, so this module must not declare a WebJar dependency.
    Path moduleDir = packagesRoot().getParent().getParent().getParent().getParent();
    Path pom = moduleDir.resolve("pom.xml");
    assertTrue(Files.isRegularFile(pom), "modules/perc-packages/pom.xml must exist");
    String text = Files.readString(pom, StandardCharsets.UTF_8);
    assertTrue(
        !text.contains("org.webjars"),
        "modules/perc-packages must not declare org.webjars dependencies "
            + "(widget payload JS is checked in, never WebJar-unpacked; see #5627).");
  }
}

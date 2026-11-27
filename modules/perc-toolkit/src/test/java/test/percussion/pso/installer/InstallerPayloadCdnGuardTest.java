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
package test.percussion.pso.installer;

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
 * Pins the installer-payload CDN inventory audited under slice 4C of parent
 * audit #5627 (issue #7366).
 *
 * <p>The {@code InstallDir} tree under {@code modules/perc-toolkit/src/main/resources} is
 * assembled verbatim into the CMS distribution by {@code perc-distribution-tree}, so any
 * {@code <script src="http(s)://...>} or {@code src="//..."} load checked in here fetches
 * third-party code from a CDN at install/admin time — invisible to {@code npm audit} and
 * Dependabot, and (for the {@code http://} pins below) mixed-content over plain HTTP. The
 * sibling slice-3 guard ({@code webjarBuildCopiedAudit.test.js} under WebUI, issue #7167) pins
 * the war/api stays-deleted set plus the zero-{@code org.webjars} and zero-CDN state of the
 * openapi viewer; this test widens the same zero-webjar/zero-CDN pins to the installer
 * payload surface.
 *
 * <p>The 4C audit found 2 straggler files (3 live CDN script loads, all plain {@code http://}):
 * {@code webapp/WEB-INF/pages/importtest_post.jsp} loaded jQuery 1.2.6 from
 * {@code ajax.googleapis.com}, and {@code user/pages/DispatchTemplateGenerator.jsp} loaded
 * jQuery 1.9.0 plus jquery-migrate 1.0.0 from {@code code.jquery.com}. Migration issue
 * #7369 removed all three loads by de-jquerying both pages to dependency-free
 * vanilla JS (no replacement bytes: the Dispatch page used only
 * {@code $(document).ready}, and the import-test helper's jQuery calls map 1:1
 * to {@code DOMContentLoaded} / {@code fetch} / {@code textContent}); the
 * inventory below is now empty and this test is the tripwire against
 * re-introduction. No migration lands in #7366 itself.
 *
 * <p>Explicitly out of scope (no double-count): the {@code .js} payload inventory itself is
 * owned by {@link InstallerPayloadVendoredJsAuditTest} (slice 2, #6069); DTS web resources by
 * #6071; the perc-packages widget payloads by {@code WidgetPayloadVendoredJsAuditTest}
 * (#6319); WebUI jslib/cm trees by slice 1 (#7165).
 */
public class InstallerPayloadCdnGuardTest {

  /**
   * Live CDN {@code <script src>} loads in the installer payload, as
   * {@code relative/path -> url} (line-independent on purpose: unrelated edits
   * above a pin must not trip the guard; only the inventory matters). Pinned by
   * the 4C audit (#7366) and emptied by the migration (#7369, which
   * de-jqueried both straggler pages to vanilla JS with no replacement
   * bytes). A legitimate future migration that <em>adds</em> a CDN load must
   * go through audit #5627 and grows this list in the same PR; anything else
   * fails loudly.
   */
  private static final List<String> EXPECTED_CDN_SCRIPT_REFS = List.of();

  /** File types that can carry a {@code <script src>} load in this payload. */
  private static final Set<String> SCANNED_EXTENSIONS =
      Set.of(".jsp", ".jspx", ".html", ".htm", ".js", ".xsl", ".vm", ".xml");

  private static final Pattern SCRIPT_SRC_CDN =
      Pattern.compile(
          "<script\\b[^>]*\\bsrc\\s*=\\s*[\"']((?:https?:)?//[^\"']+)[\"']",
          Pattern.CASE_INSENSITIVE);

  private static Path installDir() {
    List<Path> candidates =
        List.of(
            Path.of(System.getProperty("user.dir"), "src", "main", "resources", "InstallDir"),
            Path.of(
                System.getProperty("user.dir"),
                "modules",
                "perc-toolkit",
                "src",
                "main",
                "resources",
                "InstallDir"));
    for (Path candidate : candidates) {
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
    }
    fail("InstallDir payload not found under user.dir=" + System.getProperty("user.dir"));
    throw new IllegalStateException("unreachable");
  }

  private static Path moduleDir(Path installDir) {
    // InstallDir is <module>/src/main/resources/InstallDir in both layouts.
    return installDir.getParent().getParent().getParent().getParent();
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
  public void cdnScriptInventoryMatchesAudit7366() throws IOException {
    Path root = installDir();
    List<String> actual = collectCdnScriptRefs(root);
    assertEquals(
        EXPECTED_CDN_SCRIPT_REFS.stream().sorted().collect(Collectors.toList()),
        actual,
        "Installer-payload CDN <script src> inventory drifted from the #7366 audit as "
            + "emptied by migration #7369. New CDN loads must go through audit #5627 "
            + "(migration issue + npm tracking) and grow EXPECTED_CDN_SCRIPT_REFS "
            + "in the same PR.");
  }

  @Test
  public void noPlainHttpScriptLoadsBeyondPinned() throws IOException {
    // Every former 4C straggler was plain http:// — a mixed-content downgrade
    // on admin pages. The #7369 migration eliminated http:// loads entirely;
    // none may ever come back.
    List<String> actual = collectCdnScriptRefs(installDir());
    List<String> plainHttp =
        actual.stream()
            .filter(r -> r.toLowerCase().contains(" -> http://"))
            .collect(Collectors.toList());
    assertEquals(
        EXPECTED_CDN_SCRIPT_REFS.stream().sorted().collect(Collectors.toList()),
        plainHttp,
        "Plain-http <script src> loads must be exactly the pinned #7366 stragglers.");
  }

  @Test
  public void multilineScriptTagsDoNotBypassTheGuard() {
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
  public void noWebjarDependencyInModulePom() throws IOException {
    // Widens the slice-3 zero-org.webjars pin (#7167, enforced on the root
    // pom by webjarBuildCopiedAudit.test.js) to this build-copied surface:
    // installer payload JS arrives as checked-in files, never via WebJar
    // unpack, so this module must not declare a WebJar dependency.
    Path pom = moduleDir(installDir()).resolve("pom.xml");
    assertTrue(Files.isRegularFile(pom), "modules/perc-toolkit/pom.xml must exist");
    String text = Files.readString(pom, StandardCharsets.UTF_8);
    assertTrue(
        !text.contains("org.webjars"),
        "modules/perc-toolkit must not declare org.webjars dependencies "
            + "(installer payload JS is checked in, never WebJar-unpacked; see #5627).");
  }
}

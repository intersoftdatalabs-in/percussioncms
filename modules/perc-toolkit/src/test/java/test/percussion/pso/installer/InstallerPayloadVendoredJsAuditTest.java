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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Pins the installer-payload JavaScript inventory audited under issue #6069
 * (parent audit #5627).
 *
 * <p>The {@code InstallDir} tree under
 * {@code modules/perc-toolkit/src/main/resources} is assembled verbatim into
 * the CMS distribution by {@code perc-distribution-tree}, so any third-party
 * {@code .js} checked in here ships to customers invisible to {@code npm
 * audit} and Dependabot. The #6069 audit found 8 files — 5 third-party, 3
 * first-party PSO glue — with migration follow-ons #6273 (image-crop cluster:
 * Jcrop + jquery-color) and #6274 (editor legacy cluster: Interface Elements +
 * jquery-form).
 *
 * <p>This test fails when a {@code .js} file is added to or removed from the
 * payload, or when a pinned version banner changes, so the inventory can only
 * drift together with an audit update. A migration PR that legitimately
 * removes a vendored copy must update {@link #EXPECTED_JS_FILES} and the
 * banner pins in the same change.
 */
public class InstallerPayloadVendoredJsAuditTest {

  /** Third-party .js shipped in the installer payload (paths relative to InstallDir). */
  private static final Set<String> EXPECTED_THIRD_PARTY_JS =
      Set.of(
          "rx_resources/js/jquery.Jcrop.min.js",
          "rx_resources/js/jquery.color.js",
          "rx_resources/addins/psoimageeditor/js/iutil.js",
          "rx_resources/addins/psoimageeditor/js/iresizable.js",
          "rx_resources/addins/psoimageeditor/js/form.js");

  /** First-party PSO glue in the payload — recorded so additions stand out, not migrated. */
  private static final Set<String> EXPECTED_FIRST_PARTY_JS =
      Set.of(
          "rx_resources/js/cropper_custom.js",
          "rx_resources/addins/psoimageeditor/js/image_editor_general.js",
          "rx_resources/addins/psoimageeditor/js/psoimageeditor.js");

  private static final Set<String> EXPECTED_JS_FILES =
      Stream.concat(EXPECTED_THIRD_PARTY_JS.stream(), EXPECTED_FIRST_PARTY_JS.stream())
          .collect(Collectors.toUnmodifiableSet());

  /** The only minified bundle the audit allows; any other *.min.js is a new vendored lib. */
  private static final String ALLOWED_MINIFIED_BUNDLE = "rx_resources/js/jquery.Jcrop.min.js";

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

  private static Set<String> collectPayloadJs(Path root) throws IOException {
    Set<String> found = new TreeSet<>();
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().endsWith(".js"))
          .map(root::relativize)
          .map(p -> p.toString().replace(java.io.File.separatorChar, '/'))
          .forEach(found::add);
    }
    return found;
  }

  private static String readPayloadFile(Path file) throws IOException {
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  @Test
  public void payloadJsInventoryMatchesAudit6069() throws IOException {
    Path root = installDir();
    Set<String> actual = collectPayloadJs(root);
    assertEquals(
        new TreeSet<>(EXPECTED_JS_FILES),
        actual,
        "Installer-payload .js inventory drifted from the #6069 audit. "
            + "New third-party .js must go through audit #5627 (migration issue + npm tracking); "
            + "legitimate migrations (#6273/#6274) update EXPECTED_JS_FILES in the same PR.");
  }

  @Test
  public void thirdPartyVersionBannersArePinned() throws IOException {
    Path root = installDir();

    String jcrop = readPayloadFile(root.resolve("rx_resources/js/jquery.Jcrop.min.js"));
    assertTrue(jcrop.contains("v0.9.12"), "Jcrop banner must still identify v0.9.12");
    assertTrue(jcrop.contains("MIT License"), "Jcrop banner must still record the MIT license");

    String color = readPayloadFile(root.resolve("rx_resources/js/jquery.color.js"));
    assertTrue(color.contains("v2.0pre"), "jquery.color banner must still identify v2.0pre");
    assertTrue(
        color.contains("MIT or GPL"), "jquery.color banner must still record the MIT-or-GPL grant");

    for (String iface :
        List.of(
            "rx_resources/addins/psoimageeditor/js/iutil.js",
            "rx_resources/addins/psoimageeditor/js/iresizable.js")) {
      String content = readPayloadFile(root.resolve(iface));
      assertTrue(
          content.contains("Interface Elements for jQuery"),
          iface + " must still identify as Interface Elements");
      assertTrue(
          content.contains("Stefan Petre"), iface + " must still record the upstream author");
    }

    String form = readPayloadFile(root.resolve("rx_resources/addins/psoimageeditor/js/form.js"));
    assertTrue(
        form.contains("ajaxSubmit"),
        "form.js must still be the jQuery Form Plugin (ajaxSubmit identity pin; "
            + "no version banner exists in-file, see #6274)");
  }

  @Test
  public void noNewMinifiedBundlesBeyondJcrop() throws IOException {
    Path root = installDir();
    Set<String> minified =
        collectPayloadJs(root).stream()
            .filter(p -> p.endsWith(".min.js"))
            .collect(Collectors.toCollection(TreeSet::new));
    assertEquals(
        Set.of(ALLOWED_MINIFIED_BUNDLE),
        minified,
        "Only jquery.Jcrop.min.js may ship minified in the installer payload; "
            + "any other *.min.js is an un-audited vendored bundle (see #5627).");
  }
}

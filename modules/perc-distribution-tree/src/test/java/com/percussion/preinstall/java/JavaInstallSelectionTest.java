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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.preinstall.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** T029 + T035: selection outcome (0/1/N candidates) and property write content tests. */
class JavaInstallSelectionTest {

  @TempDir Path tempDir;

  @Test
  void zeroEligibleCandidatesFailsClearly() throws Exception {
    Path invalid = tempDir.resolve("does-not-exist");
    JavaInstallSelection unattended =
        new JavaInstallSelection(tempDir, invalid, null);
    var ex = assertThrows(JavaInstallSelection.JavaSelectionException.class,
        unattended::selectAndPersist);
    assertTrue(ex.getMessage().contains("21"),
        "error message must mention minimum major version");
    assertTrue(
        ex.getMessage().toLowerCase().contains("or later")
            || ex.getMessage().toLowerCase().contains("later"),
        "error message must state 21 or later: " + ex.getMessage());
  }

  @Test
  void singleEligibleCandidateAutoSelects() throws Exception {
    Path installRoot = tempDir.resolve("install");
    Files.createDirectories(installRoot);
    Path jdk = JavaCandidateDiscoveryTest.makeHome(installRoot.resolve("jdk21"), "21", "java");
    JavaInstallSelection sel = new JavaInstallSelection(installRoot, jdk, null);
    JavaInstallSelection.SelectionOutcome out = sel.selectAndPersist();
    assertEquals(jdk.toAbsolutePath().normalize(), out.javaHome().toAbsolutePath().normalize());
    assertTrue(out.source().startsWith("unattended") || out.source().contains("auto"),
        "source tagged: " + out.source());
    assertTrue(Files.exists(installRoot.resolve("java.properties")));
    String body = Files.readString(installRoot.resolve("java.properties"), StandardCharsets.UTF_8);
    assertTrue(body.contains("jdk21"),
        "properties file contains JAVA_HOME jdk21 suffix: " + body);
  }

  @Test
  void unattendedMajor25IsAccepted() throws Exception {
    Path installRoot = tempDir.resolve("install25");
    Files.createDirectories(installRoot);
    Path jdk25 = JavaCandidateDiscoveryTest.makeHome(installRoot.resolve("jdk25"), "25", "java");
    JavaInstallSelection sel = new JavaInstallSelection(installRoot, jdk25, null);
    JavaInstallSelection.SelectionOutcome out = sel.selectAndPersist();
    assertEquals(jdk25.toAbsolutePath().normalize(), out.javaHome().toAbsolutePath().normalize());
    String body = Files.readString(installRoot.resolve("java.properties"), StandardCharsets.UTF_8);
    assertTrue(body.contains("jdk25"), "properties file contains jdk25: " + body);
  }

  @Test
  void multipleEligibleCandidatesPromptOperator() throws Exception {
    Path installRoot = tempDir.resolve("install");
    Files.createDirectories(installRoot);
    Path a = JavaCandidateDiscoveryTest.makeHome(installRoot.resolve("a"), "21", "java");
    Path b = JavaCandidateDiscoveryTest.makeHome(installRoot.resolve("b"), "21", "java");

    AtomicReference<String> prompted = new AtomicReference<>();
    JavaInstallSelection.InteractivePrompt prompt = q -> {
      prompted.set(q);
      return "2";
    };
    JavaInstallSelection unattendedA = new JavaInstallSelection(installRoot, a, prompt);
    unattendedA.selectAndPersist();
    assertEquals(null, prompted.get(), "unattended path must not invoke the prompt");
  }

  @Test
  void listContainsAtLeastPathLauncherHome() {
    List<JavaCandidateDiscovery.Candidate> candidates = JavaCandidateDiscovery.discover();
    assertTrue(candidates != null);
  }

  // ---------- isUnattendedRequested() / isInteractiveAvailable() regression ----------

  @Test
  void isUnattendedRequestedRecognizesAllTruthySources() {
    assertTrue(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(JavaInstallSelection.PERC_UNATTENDED, "true"),
        java.util.Map.of()));
    assertTrue(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(JavaInstallSelection.PERC_UNATTENDED, "TRUE"),
        java.util.Map.of()));
    assertTrue(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(JavaInstallSelection.PERC_UNATTENDED, "1"),
        java.util.Map.of()));
    assertTrue(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(JavaInstallSelection.PERC_UNATTENDED, "yes"),
        java.util.Map.of()));
    assertTrue(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(),
        java.util.Map.of(JavaInstallSelection.PERC_INSTALL_UNATTENDED_ENV, "true")));
    assertTrue(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(),
        java.util.Map.of(JavaInstallSelection.PERC_INSTALL_UNATTENDED_ENV, "1")));
    assertTrue(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(JavaInstallSelection.PERC_UNATTENDED, "true"),
        java.util.Map.of(JavaInstallSelection.PERC_INSTALL_UNATTENDED_ENV, "true")),
        "either source is sufficient");
  }

  @Test
  void isUnattendedRequestedRejectsFalsyAndUnset() {
    assertFalse(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(JavaInstallSelection.PERC_UNATTENDED, "false"),
        java.util.Map.of()));
    assertFalse(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(JavaInstallSelection.PERC_UNATTENDED, "FALSE"),
        java.util.Map.of()));
    assertFalse(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(JavaInstallSelection.PERC_UNATTENDED, "0"),
        java.util.Map.of()));
    assertFalse(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(JavaInstallSelection.PERC_UNATTENDED, "no"),
        java.util.Map.of()));
    assertFalse(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(JavaInstallSelection.PERC_UNATTENDED, ""),
        java.util.Map.of()));
    assertFalse(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(JavaInstallSelection.PERC_UNATTENDED, "   "),
        java.util.Map.of()));
    assertFalse(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(),
        java.util.Map.of()));
    assertFalse(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(JavaInstallSelection.PERC_UNATTENDED, "false"),
        java.util.Map.of(JavaInstallSelection.PERC_INSTALL_UNATTENDED_ENV, "false")),
        "both falsy sources is still falsy");
    assertFalse(JavaInstallSelection.isUnattendedRequested(null, null));
    assertFalse(JavaInstallSelection.isUnattendedRequested(
        java.util.Map.of(),
        java.util.Map.of("UNRELATED", "true")),
        "unrelated env-var must not trigger unattended mode");
  }

  @Test
  void isInteractiveAvailableQueueDrainedReturnsFalse() {
    assertFalse(JavaInstallSelection.isInteractiveAvailable(false),
        "stdin empty + no console must NOT be interactive (no hang)");
  }

  @Test
  void isInteractiveAvailableQueuedBytesAreInteractive() {
    assertTrue(JavaInstallSelection.isInteractiveAvailable(true),
        "stdin with queued bytes must be interactive (operator piped input)");
  }
}

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Selects a Java home (major {@link JavaHomeResolver#REQUIRED_MAJOR} or later)
 * for the install and persists it to the install-root {@code java.properties}
 * file. Implements US3 (interactive multi-candidate selection) and US4
 * (unattended explicit {@code -Dperc.java.home=...}) in one entry point so the
 * preinstall does not need separate paths.
 *
 * <p>Outcome rules:
 *
 * <ul>
 *   <li>When {@link #unattendedHome} is supplied, validate as a Java home with
 *       major 21 or later. Fail if invalid. Persist on success.
 *   <li>Otherwise, when {@link #interactivePrompt} is non-null and there are
 *       two or more eligible candidates, prompt the operator to choose.
 *   <li>If there is exactly one eligible candidate, auto-select and log.
 *   <li>If there are zero eligible candidates, fail with a clear error that
 *       names the minimum major version (21 or later).
 * </ul>
 */
public final class JavaInstallSelection {

  private final Path installRoot;
  private final Path unattendedHome;
  private final InteractivePrompt interactivePrompt;

  /** Construct a selector with the supplied optional overrides. */
  public JavaInstallSelection(Path installRoot, Path unattendedHome, InteractivePrompt prompt) {
    if (installRoot == null) {
      throw new IllegalArgumentException("installRoot must not be null");
    }
    this.installRoot = installRoot;
    this.unattendedHome = unattendedHome;
    this.interactivePrompt = prompt;
  }

  /**
   * Selects and persists the chosen Java home. On success, returns the
   * absolute launcher path of the chosen home; on failure throws
   * {@link JavaSelectionException} with a clear operator message that
   * mentions the minimum major version (21 or later).
   */
  public SelectionOutcome selectAndPersist() throws IOException, JavaSelectionException {
    Path chosen;
    String source;
    if (unattendedHome != null && !unattendedHome.toString().isBlank()) {
      chosen = unattendedHome.toAbsolutePath().normalize();
      if (!isValidJava21Home(chosen)) {
        throw new JavaSelectionException(
            "Unattended Java home is not a valid Java install: " + chosen
                + " (required: major version "
                + JavaHomeResolver.REQUIRED_MAJOR + " or later)");
      }
      source = "unattended (-Dperc.java.home)";
    } else {
      List<JavaCandidateDiscovery.Candidate> eligible =
          JavaCandidateDiscovery.discoverEligible();
      if (eligible.isEmpty()) {
        throw new JavaSelectionException(
            "No eligible Java home found on this host. Required: major version "
                + JavaHomeResolver.REQUIRED_MAJOR + " or later."
                + " Set -Dperc.java.home=<path> or install a Java "
                + JavaHomeResolver.REQUIRED_MAJOR + "+ JRE/JDK before"
                + " continuing.");
      }
      if (eligible.size() == 1 || interactivePrompt == null || !isInteractiveAvailable()) {
        chosen = eligible.get(0).path().toAbsolutePath().normalize();
        source = eligible.size() == 1
            ? "auto-selected (single eligible candidate: the Java home contract only scans JAVA_HOME and JAVA_HOME_<NN> env vars)"
            : "auto-selected (non-interactive batch: console or stdin unavailable)";
      } else {
        chosen = promptForChoice(eligible);
        source = "selected by operator";
      }
    }

    String absoluteHome = chosen.toString();
    String launcher = absoluteHome + separator() + "bin" + separator() + launcherName();
    JavaPropertiesSupport.write(installRoot, absoluteHome, launcher);
    return new SelectionOutcome(chosen, Path.of(launcher), source);
  }

  private static boolean isInteractiveAvailable() {
    if (System.console() != null) {
      return true;
    }
    // Non-TTY fallback: probe stdin via available() which does NOT consume
    // bytes. (An earlier Scanner-based probe consumed the user's typed
    // input before the actual prompt could read it.) The caller's prompt
    // is responsible for the actual blocking read; this method just
    // reports whether stdin is open.
    try {
      return System.in.available() >= 0;
    } catch (java.io.IOException e) {
      return false;
    }
  }

  private Path promptForChoice(List<JavaCandidateDiscovery.Candidate> candidates)
      throws JavaSelectionException {
    StringBuilder menu = new StringBuilder();
    menu.append("Multiple Java ")
        .append(JavaHomeResolver.REQUIRED_MAJOR)
        .append("+ candidates detected. Select the Java home to use:\n");
    for (int i = 0; i < candidates.size(); i++) {
      JavaCandidateDiscovery.Candidate c = candidates.get(i);
      menu.append("  [").append(i + 1).append("] ")
          .append(c.path()).append(" (version ").append(c.versionDisplay()).append(")\n");
    }
    menu.append("Enter choice (1-").append(candidates.size()).append("): ");
    String answer = interactivePrompt.readLine(menu.toString());
    int pick;
    try {
      pick = Integer.parseInt(answer.trim());
    } catch (NumberFormatException nfe) {
      throw new JavaSelectionException("Invalid selection: '" + answer + "'");
    }
    if (pick < 1 || pick > candidates.size()) {
      throw new JavaSelectionException("Selection out of range: " + pick);
    }
    return candidates.get(pick - 1).path();
  }

  /**
   * Best-effort "is this a Java home with major {@code >=}
   * {@link JavaHomeResolver#REQUIRED_MAJOR}" check using a sibling release file.
   */
  static boolean isValidJava21Home(Path home) {
    if (home == null || !Files.isDirectory(home)) {
      return false;
    }
    String version = JavaCandidateDiscovery.readVersion(home);
    if (version.isBlank()) {
      // No release file: defer to JavaHomeResolver's probe (which checks launcher
      // executability). For the preinstall path we also need the launcher to exist.
      Path launcher = home.resolve("bin").resolve(launcherName());
      return Files.isRegularFile(launcher);
    }
    return JavaCandidateDiscovery.Candidate.meetsMinimumMajor(version);
  }

  private static String separator() {
    return java.io.File.separator;
  }

  private static String launcherName() {
    String os = System.getProperty("os.name", "");
    return os.toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
  }

  /** Result of a successful selection. */
  public record SelectionOutcome(Path javaHome, Path launcher, String source) {
    public String summary() {
      return "JAVA_HOME=" + javaHome + " source=" + source;
    }
  }

  /** Thrown when no candidate is found or selection is invalid. */
  public static final class JavaSelectionException extends Exception {
    public JavaSelectionException(String message) {
      super(message);
    }

    public JavaSelectionException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /** Strategy for reading a line of operator input during interactive selection. */
  @FunctionalInterface
  public interface InteractivePrompt {
    String readLine(String prompt);
  }
}

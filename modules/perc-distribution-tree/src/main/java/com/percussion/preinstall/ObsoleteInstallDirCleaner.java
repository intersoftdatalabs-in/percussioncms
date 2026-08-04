/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.preinstall;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Optional early-upgrade cleanup of obsolete install-root directories (issue #1157).
 *
 * <p>Deletes only a curated MVP set when the operator confirms interactively or passes {@code
 * --clean-install-dir}. Failures are best-effort (warn-and-continue).
 */
public final class ObsoleteInstallDirCleaner {

  /** CLI flag name ({@code --clean-install-dir}) that forces silent cleanup of obsolete paths. */
  public static final String FLAG_KEY = "clean-install-dir";

  /** System property equivalent of {@link #FLAG_KEY} ({@code -Dclean.install.dir=...}). */
  public static final String FLAG_SYSTEM_PROPERTY = "clean.install.dir";

  static final String PRE_INSTALL = "PreInstall";
  static final String PERCUSSION_INSTALLATION = "_Percussion_Installation";
  static final String PERCUSSION_INSTALLATION_ALT = "_Percussion_installation";
  static final String JBOSS_SERVER_XML_BAK = "JBossServerXML_BAK";
  static final String APP_SERVER = "AppServer";

  private static final LinkOption[] NO_FOLLOW = {LinkOption.NOFOLLOW_LINKS};

  private ObsoleteInstallDirCleaner() {}

  /**
   * One obsolete directory candidate discovered under the install root.
   *
   * @param relativeName path relative to the install root.
   * @param absolutePath absolute, normalized path on disk.
   * @param sizeBytes best-effort size estimate in bytes.
   */
  public record Candidate(String relativeName, Path absolutePath, long sizeBytes) {}

  /**
   * A candidate whose deletion failed; surfaced in {@link CleanupResult#failed()}.
   *
   * @param path path that could not be deleted.
   * @param message human-readable failure reason.
   */
  public record FailedPath(Path path, String message) {}

  /**
   * Outcome of the cleanup pass for an upgrade install root.
   *
   * @param candidates all candidates discovered (deleted + retained + failed).
   * @param proceeded {@code true} when the deletion step actually ran.
   * @param decisionSource human-readable label describing how the decision was made.
   * @param deleted paths that were successfully deleted.
   * @param failed paths that could not be deleted (with their failure reason).
   * @param retained candidates that were intentionally kept.
   */
  public record CleanupResult(
      List<Candidate> candidates,
      boolean proceeded,
      String decisionSource,
      List<Path> deleted,
      List<FailedPath> failed,
      List<Candidate> retained) {

    /** Canonical constructor: defensively copies all list fields. */
    public CleanupResult {
      candidates = List.copyOf(candidates);
      deleted = List.copyOf(deleted);
      failed = List.copyOf(failed);
      retained = List.copyOf(retained);
    }

    /**
     * Returns whether the upgrade may proceed after cleanup.
     *
     * @return {@code true} always — cleanup is best-effort and never blocks the upgrade.
     */
    public boolean continueUpgrade() {
      return true;
    }
  }

  /**
   * True when the install root looks like an existing product install (upgrade target).
   *
   * @param installRoot candidate install root directory; may be {@code null}.
   * @return {@code true} when {@code installRoot} contains either {@code Version.properties} or an
   *     {@code ObjectStore} directory; {@code false} otherwise.
   */
  public static boolean isUpgradeInstallRoot(Path installRoot) {
    if (installRoot == null || !Files.isDirectory(installRoot, NO_FOLLOW)) {
      return false;
    }
    if (Files.isRegularFile(installRoot.resolve("Version.properties"), NO_FOLLOW)) {
      return true;
    }
    return Files.isDirectory(installRoot.resolve("ObjectStore"), NO_FOLLOW);
  }

  /**
   * Parse {@code --clean-install-dir} from CLI options and optional system property. Default false.
   *
   * @param cliOptions CLI option map (key=&gt;value); may be {@code null}.
   * @return {@code true} when the flag is present with a truthy value; {@code false} otherwise.
   */
  public static boolean parseCleanInstallDirFlag(java.util.Map<String, String> cliOptions) {
    String fromCli =
        cliOptions == null
            ? null
            : firstNonBlank(cliOptions.get(FLAG_KEY), cliOptions.get("clean.install.dir"));
    String fromSys = System.getProperty(FLAG_SYSTEM_PROPERTY);
    String raw = firstNonBlank(fromCli, fromSys);
    if (raw == null || raw.isBlank()) {
      return false;
    }
    if ("true".equalsIgnoreCase(raw) || "yes".equalsIgnoreCase(raw) || "1".equals(raw)) {
      return true;
    }
    if ("false".equalsIgnoreCase(raw) || "no".equalsIgnoreCase(raw) || "0".equals(raw)) {
      return false;
    }
    // bare --clean-install-dir yields "true" from parseArgs
    return Boolean.parseBoolean(raw);
  }

  /**
   * List eligible obsolete candidates under the install root.
   *
   * @param installRoot directory to scan; must resolve to an existing product install.
   * @param majorVersion existing product major, or 0 if unknown
   * @param minorVersion existing product minor, or 0 if unknown
   * @return immutable list of obsolete candidates; never {@code null}.
   * @throws IOException if scanning the install root fails.
   */
  public static List<Candidate> listEligibleCandidates(
      Path installRoot, int majorVersion, int minorVersion) throws IOException {
    Objects.requireNonNull(installRoot, "installRoot");
    Path root = installRoot.toAbsolutePath().normalize();
    List<Candidate> found = new ArrayList<>();

    addIfCandidate(found, root, PRE_INSTALL);
    // Prefer exact casing that exists (case-sensitive filesystems)
    if (existsAsDirOrSymlink(root.resolve(PERCUSSION_INSTALLATION))) {
      addIfCandidate(found, root, PERCUSSION_INSTALLATION);
    } else if (existsAsDirOrSymlink(root.resolve(PERCUSSION_INSTALLATION_ALT))) {
      addIfCandidate(found, root, PERCUSSION_INSTALLATION_ALT);
    }

    if (isJBossBakEligible(root, majorVersion, minorVersion)
        && existsAsDirOrSymlink(root.resolve(JBOSS_SERVER_XML_BAK))) {
      addIfCandidate(found, root, JBOSS_SERVER_XML_BAK);
    }

    return List.copyOf(found);
  }

  /**
   * JBoss bak is not eligible when on 5.3-era upgrade path without AppServer (cannot recreate bak).
   *
   * @param installRoot install root directory being upgraded.
   * @param majorVersion existing product major version.
   * @param minorVersion existing product minor version.
   * @return {@code true} when the JBoss bak directory can be safely deleted.
   */
  public static boolean isJBossBakEligible(Path installRoot, int majorVersion, int minorVersion) {
    boolean oldFiveThree = majorVersion == 5 && minorVersion < 4;
    if (!oldFiveThree) {
      return true;
    }
    return Files.isDirectory(installRoot.resolve(APP_SERVER), NO_FOLLOW);
  }

  /**
   * Best-effort size estimate of the file or directory tree at {@code path}. Does not follow
   * directory symlinks; symlinks contribute their own size only.
   *
   * @param path path to measure; may be {@code null}.
   * @return size in bytes; {@code 0} when {@code path} does not exist.
   * @throws IOException if a filesystem traversal fails.
   */
  public static long estimateSizeBytes(Path path) throws IOException {
    if (path == null || !Files.exists(path, NO_FOLLOW)) {
      return 0L;
    }
    if (Files.isSymbolicLink(path) || Files.isRegularFile(path, NO_FOLLOW)) {
      // Symlink or file: report link/file size only (do not follow)
      try {
        return Files.size(path);
      } catch (IOException e) {
        return 0L;
      }
    }
    if (!Files.isDirectory(path, NO_FOLLOW)) {
      return 0L;
    }
    final long[] total = {0L};
    Files.walkFileTree(
        path,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            // Default walk does not follow dir symlinks; size regular files / links as nodes
            if (attrs.isRegularFile() || attrs.isSymbolicLink()) {
              total[0] += attrs.size();
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
          }
        });
    return total[0];
  }

  /**
   * Formats a byte count using the largest unit (B/KB/MB/GB) that yields a value &lt; 1024.
   *
   * @param bytes size in bytes; negative values are treated as zero.
   * @return human-readable size string.
   */
  public static String formatSize(long bytes) {
    if (bytes < 0) {
      bytes = 0;
    }
    if (bytes < 1024) {
      return bytes + " B";
    }
    double kb = bytes / 1024.0;
    if (kb < 1024) {
      return String.format(Locale.ROOT, "%.1f KB", kb);
    }
    double mb = kb / 1024.0;
    if (mb < 1024) {
      return String.format(Locale.ROOT, "%.1f MB", mb);
    }
    double gb = mb / 1024.0;
    return String.format(Locale.ROOT, "%.2f GB", gb);
  }

  /**
   * True if {@code path} is a strict child of {@code installRoot} after normalization (no {@code
   * ..} escape).
   *
   * @param installRoot parent install root directory; may be {@code null}.
   * @param path candidate path to test; may be {@code null}.
   * @return {@code true} when {@code path} resolves to an entry strictly below {@code installRoot}
   *     without any {@code ..} segments.
   */
  public static boolean isUnderInstallRoot(Path installRoot, Path path) {
    if (installRoot == null || path == null) {
      return false;
    }
    Path root = installRoot.toAbsolutePath().normalize();
    Path target = path.toAbsolutePath().normalize();
    if (target.equals(root)) {
      return false;
    }
    if (!target.startsWith(root)) {
      return false;
    }
    Path relative = root.relativize(target);
    for (Path part : relative) {
      if ("..".equals(part.toString())) {
        return false;
      }
    }
    return !relative.isAbsolute();
  }

  /**
   * Decision matrix: whether to delete without prompting, require prompt, or retain.
   *
   * @param upgrade {@code true} when the installer is performing an upgrade.
   * @param cleanFlag {@code true} when {@code --clean-install-dir} was supplied.
   * @param interactive {@code true} when the installer is running interactively.
   * @param hasCandidates {@code true} when at least one obsolete candidate was found.
   * @return {@code PROCEED}, {@code PROMPT}, or {@code RETAIN}
   */
  public static Decision decide(
      boolean upgrade, boolean cleanFlag, boolean interactive, boolean hasCandidates) {
    if (!upgrade || !hasCandidates) {
      return Decision.RETAIN;
    }
    if (cleanFlag) {
      return Decision.PROCEED;
    }
    if (interactive) {
      return Decision.PROMPT;
    }
    return Decision.RETAIN;
  }

  /** Outcome of the cleanup decision matrix. */
  public enum Decision {
    /** Delete the obsolete directories without prompting. */
    PROCEED,
    /** Prompt the operator before deleting the obsolete directories. */
    PROMPT,
    /** Keep the obsolete directories in place. */
    RETAIN
  }

  /**
   * Interpret interactive answer; empty/default is no.
   *
   * @param answer raw answer supplied by the operator; may be {@code null}.
   * @return {@code true} when {@code answer} is {@code y} or {@code yes} (case-insensitive).
   */
  public static boolean isAffirmativeAnswer(String answer) {
    if (answer == null) {
      return false;
    }
    String a = answer.trim();
    return "y".equalsIgnoreCase(a) || "yes".equalsIgnoreCase(a);
  }

  /**
   * Builds the operator-facing prompt for the obsolete-directory cleanup pass.
   *
   * @param installRoot install root directory whose obsolete candidates will be summarized.
   * @param candidates list of obsolete candidates to display.
   * @return multi-line prompt terminated with {@code [y/N]: }.
   */
  public static String buildPromptText(Path installRoot, List<Candidate> candidates) {
    StringBuilder sb = new StringBuilder();
    sb.append("The following obsolete directories were found under ")
        .append(installRoot.toAbsolutePath())
        .append(":\n");
    long total = 0L;
    for (Candidate c : candidates) {
      total += c.sizeBytes();
      sb.append("  ")
          .append(padRight(c.relativeName(), 28))
          .append(" ~ ")
          .append(formatSize(c.sizeBytes()))
          .append('\n');
    }
    sb.append("Total approximate space: ~ ").append(formatSize(total)).append("\n\n");
    sb.append("These are not required by Percussion CMS 8.x. Back up anything you still need\n");
    sb.append("before continuing.\n\n");
    sb.append("Remove these directories now? [y/N]: ");
    return sb.toString();
  }

  /**
   * Formats a cleanup result as a multi-line report suitable for installer logs.
   *
   * @param result cleanup pass result to render.
   * @return multi-line report; never {@code null}.
   */
  public static String formatCleanupReport(CleanupResult result) {
    StringBuilder sb = new StringBuilder();
    sb.append("--- Obsolete install directory cleanup ---\n");
    sb.append("Decision: ").append(result.decisionSource()).append('\n');
    if (result.candidates().isEmpty()) {
      sb.append("No obsolete candidate directories found.\n");
      return sb.toString();
    }
    if (!result.deleted().isEmpty()) {
      sb.append("Deleted:\n");
      for (Path p : result.deleted()) {
        sb.append("  ").append(p).append('\n');
      }
    }
    if (!result.retained().isEmpty()) {
      sb.append("Retained:\n");
      for (Candidate c : result.retained()) {
        sb.append("  ")
            .append(c.absolutePath())
            .append(" (~")
            .append(formatSize(c.sizeBytes()))
            .append(")\n");
      }
    }
    if (!result.failed().isEmpty()) {
      sb.append("Failed (still on disk; upgrade will continue):\n");
      for (FailedPath f : result.failed()) {
        sb.append("  ").append(f.path()).append(": ").append(f.message()).append('\n');
      }
    }
    long total = result.candidates().stream().mapToLong(Candidate::sizeBytes).sum();
    sb.append("Approximate size of candidates considered: ~ ")
        .append(formatSize(total))
        .append('\n');
    return sb.toString();
  }

  /**
   * Run the cleanup flow for an upgrade install root.
   *
   * @param installRoot directory to scan; must be a product install root.
   * @param majorVersion existing product major version, or 0 if unknown.
   * @param minorVersion existing product minor version, or 0 if unknown.
   * @param cleanFlag {@code true} when {@code --clean-install-dir} was supplied.
   * @param interactive {@code true} when the installer is running interactively.
   * @param lineReader if non-null and decision is PROMPT, called with prompt text to read answer
   * @return summary of candidates that were deleted, retained, or failed.
   * @throws IOException if filesystem traversal or deletion fails.
   */
  public static CleanupResult run(
      Path installRoot,
      int majorVersion,
      int minorVersion,
      boolean cleanFlag,
      boolean interactive,
      Function<String, String> lineReader)
      throws IOException {
    if (!isUpgradeInstallRoot(installRoot)) {
      return new CleanupResult(List.of(), false, "not-upgrade", List.of(), List.of(), List.of());
    }

    List<Candidate> candidates = listEligibleCandidates(installRoot, majorVersion, minorVersion);
    if (candidates.isEmpty()) {
      return new CleanupResult(List.of(), false, "no-candidates", List.of(), List.of(), List.of());
    }

    Decision decision = decide(true, cleanFlag, interactive, true);
    boolean proceed;
    String source;

    switch (decision) {
      case PROCEED -> {
        proceed = true;
        source = "flag";
      }
      case PROMPT -> {
        String prompt = buildPromptText(installRoot, candidates);
        String answer = lineReader != null ? lineReader.apply(prompt) : "";
        proceed = isAffirmativeAnswer(answer);
        source = proceed ? "interactive-yes" : "interactive-no";
      }
      default -> {
        proceed = false;
        source = "default-retain";
      }
    }

    if (!proceed) {
      return new CleanupResult(candidates, false, source, List.of(), List.of(), candidates);
    }

    List<Path> deleted = new ArrayList<>();
    List<FailedPath> failed = new ArrayList<>();
    List<Candidate> stillPresent = new ArrayList<>();
    for (Candidate c : candidates) {
      try {
        deleteRecursivelyConfined(installRoot, c.absolutePath());
        if (Files.exists(c.absolutePath(), NO_FOLLOW)) {
          failed.add(new FailedPath(c.absolutePath(), "Path still exists after delete attempt"));
          stillPresent.add(c);
        } else {
          deleted.add(c.absolutePath());
        }
      } catch (Exception e) {
        failed.add(
            new FailedPath(
                c.absolutePath(),
                e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        if (Files.exists(c.absolutePath(), NO_FOLLOW)) {
          stillPresent.add(c);
        }
      }
    }
    return new CleanupResult(candidates, true, source, deleted, failed, stillPresent);
  }

  static void deleteRecursivelyConfined(Path installRoot, Path target) throws IOException {
    if (!isUnderInstallRoot(installRoot, target)) {
      throw new IOException("Refusing to delete path outside install root: " + target);
    }
    if (!Files.exists(target, NO_FOLLOW)) {
      return;
    }
    // Top-level symlink: delete the link only (never follow outside the install root)
    if (Files.isSymbolicLink(target)) {
      Files.deleteIfExists(target);
      return;
    }
    if (Files.isRegularFile(target, NO_FOLLOW)) {
      Files.deleteIfExists(target);
      return;
    }
    Files.walkFileTree(
        target,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            if (!isUnderInstallRoot(installRoot, dir)) {
              throw new IOException("Refusing to delete directory outside install root: " + dir);
            }
            // Do not descend into symlink directories
            if (attrs.isSymbolicLink() || Files.isSymbolicLink(dir)) {
              Files.deleteIfExists(dir);
              return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            if (!isUnderInstallRoot(installRoot, file)) {
              throw new IOException("Refusing to delete file outside install root: " + file);
            }
            Files.deleteIfExists(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) {
              throw exc;
            }
            if (!isUnderInstallRoot(installRoot, dir)) {
              throw new IOException("Refusing to delete directory outside install root: " + dir);
            }
            Files.deleteIfExists(dir);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private static boolean existsAsDirOrSymlink(Path p) {
    if (!Files.exists(p, NO_FOLLOW)) {
      return false;
    }
    return Files.isDirectory(p, NO_FOLLOW) || Files.isSymbolicLink(p);
  }

  private static void addIfCandidate(List<Candidate> found, Path root, String relative)
      throws IOException {
    Path p = root.resolve(relative).normalize();
    if (!isUnderInstallRoot(root, p)) {
      return;
    }
    if (!existsAsDirOrSymlink(p)) {
      return;
    }
    long size = estimateSizeBytes(p);
    found.add(new Candidate(relative, p, size));
  }

  private static String padRight(String s, int width) {
    if (s.length() >= width) {
      return s;
    }
    return s + " ".repeat(width - s.length());
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.trim().isEmpty()) {
        return v.trim();
      }
    }
    return null;
  }
}

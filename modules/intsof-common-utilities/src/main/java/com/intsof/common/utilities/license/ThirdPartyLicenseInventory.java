/*
 * Copyright 2026 Intersoft Data Labs (https://intsof.com)
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
package com.intsof.common.utilities.license;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Product-agnostic helpers for building a <strong>merged</strong> third-party license inventory
 * that combines:
 *
 * <ul>
 *   <li>an existing Maven-oriented inventory text file (for example the output of {@code
 *       org.codehaus.mojo:license-maven-plugin:aggregate-add-third-party}), and
 *   <li>production npm packages declared in one or more npm {@code package-lock.json} files
 *       (lockfileVersion 2 or 3 {@code packages} map).
 * </ul>
 *
 * <p>This type is intentionally free of product names and build-system coupling so any Intersoft
 * (or other) multi-module project can reuse it. Callers supply paths and optional heading text.
 *
 * <h2>Typical Maven layout</h2>
 *
 * <pre>
 * target/generated-sources/license/
 *   THIRD-PARTY-MAVEN.txt   ← produced by license-maven-plugin
 *   THIRD-PARTY-NPM.txt     ← intermediate npm section (optional)
 *   THIRD-PARTY.txt         ← merged inventory (ship / publish this file)
 * </pre>
 *
 * <h2>CLI</h2>
 *
 * <p>A {@link #main(String[])} entry point supports {@code exec-maven-plugin:java} (or direct
 * {@code java -cp …}). See {@link #main(String[])} for flags.
 *
 * <p>Paths use {@link java.nio.file} only (Windows / Linux / macOS). Written files use UTF-8 and LF
 * line endings for stable cross-platform diffs.
 *
 * @since 0.0.1
 */
public final class ThirdPartyLicenseInventory {

  /** Default Maven-only inventory file name. */
  public static final String DEFAULT_MAVEN_FILE_NAME = "THIRD-PARTY-MAVEN.txt";

  /** Default intermediate npm-only inventory file name. */
  public static final String DEFAULT_NPM_FILE_NAME = "THIRD-PARTY-NPM.txt";

  /** Default merged inventory file name (the file most products should ship). */
  public static final String DEFAULT_MERGED_FILE_NAME = "THIRD-PARTY.txt";

  private ThirdPartyLicenseInventory() {}

  /**
   * One production npm package taken from a package-lock {@code packages} entry.
   *
   * @param name package name (for example {@code react} or {@code @scope/pkg})
   * @param version resolved version string
   * @param license SPDX-ish license expression or {@code "Unknown license"}
   * @param sourceLabel human-readable origin (typically a repo-relative lockfile path)
   */
  public record NpmPackage(String name, String version, String license, String sourceLabel)
      implements Comparable<NpmPackage> {

    /**
     * Creates a validated package coordinate.
     *
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code name} or {@code version} is blank
     */
    public NpmPackage {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(version, "version");
      Objects.requireNonNull(license, "license");
      Objects.requireNonNull(sourceLabel, "sourceLabel");
      if (name.isBlank()) {
        throw new IllegalArgumentException("name must not be blank");
      }
      if (version.isBlank()) {
        throw new IllegalArgumentException("version must not be blank");
      }
      if (license.isBlank()) {
        license = "Unknown license";
      }
    }

    /**
     * Formats a single inventory line in a style compatible with common Maven THIRD-PARTY listings.
     *
     * @return one inventory line (no trailing newline)
     */
    public String toInventoryLine() {
      return "     ("
          + license
          + ") "
          + name
          + " (npm:"
          + name
          + ":"
          + version
          + " - "
          + sourceLabel
          + ")";
    }

    @Override
    public int compareTo(NpmPackage other) {
      int byName = name.compareToIgnoreCase(other.name);
      if (byName != 0) {
        return byName;
      }
      return version.compareTo(other.version);
    }
  }

  /**
   * Reads production (non-dev) packages from an npm {@code package-lock.json} file.
   *
   * <p>Only lockfileVersion 2/3 style documents with a top-level {@code packages} object are
   * supported. Entries marked {@code "dev": true} or {@code "devOptional": true} are skipped.
   * Nested installs under {@code node_modules/…/node_modules/…} are included under the nested
   * package name (last {@code node_modules/} segment).
   *
   * @param packageLockJson path to {@code package-lock.json}
   * @param sourceRoot optional project root used to label {@link NpmPackage#sourceLabel()} as a
   *     relative path; may be {@code null} to use the absolute lock path
   * @return sorted, de-duplicated production packages (name+version)
   * @throws IOException if the file cannot be read
   * @throws IllegalArgumentException if the document is not a supported package-lock
   * @throws NullPointerException if {@code packageLockJson} is null
   */
  public static List<NpmPackage> readProductionPackagesFromLockFile(
      Path packageLockJson, Path sourceRoot) throws IOException {
    Objects.requireNonNull(packageLockJson, "packageLockJson");
    if (!Files.isRegularFile(packageLockJson)) {
      throw new IllegalArgumentException("package-lock.json not found: " + packageLockJson);
    }
    String json = Files.readString(packageLockJson, StandardCharsets.UTF_8);
    Object root = MinimalJson.parse(json);
    if (!(root instanceof Map<?, ?> rootMap)) {
      throw new IllegalArgumentException(
          "package-lock root must be a JSON object: " + packageLockJson);
    }
    Object packagesNode = rootMap.get("packages");
    if (!(packagesNode instanceof Map<?, ?> packages)) {
      throw new IllegalArgumentException(
          "Unsupported package-lock (missing packages map): " + packageLockJson);
    }

    String sourceLabel = labelFor(packageLockJson, sourceRoot);
    Map<String, NpmPackage> byKey = new TreeMap<>();
    for (Map.Entry<?, ?> entry : packages.entrySet()) {
      String key = String.valueOf(entry.getKey());
      if (!(entry.getValue() instanceof Map<?, ?> meta)) {
        continue;
      }
      if (isTruthy(meta.get("dev")) || isTruthy(meta.get("devOptional"))) {
        continue;
      }
      String name = packageNameFromLockKey(key);
      if (name == null) {
        continue;
      }
      String version = stringOrEmpty(meta.get("version"));
      if (version.isBlank()) {
        continue;
      }
      String license = licenseFromMeta(meta.get("license"));
      NpmPackage pkg = new NpmPackage(name, version, license, sourceLabel);
      byKey.put(name.toLowerCase(Locale.ROOT) + "@" + version, pkg);
    }
    List<NpmPackage> list = new ArrayList<>(byKey.values());
    Collections.sort(list);
    return List.copyOf(list);
  }

  /**
   * Result of collecting production npm packages from a lock-list file.
   *
   * @param packages sorted union of production packages
   * @param missingLockFiles absolute paths listed in the lock-list that are not regular files
   * @param lockListFileMissing {@code true} when the lock-list path itself is not a regular file
   */
  public record NpmCollectionResult(
      List<NpmPackage> packages, List<Path> missingLockFiles, boolean lockListFileMissing) {

    /** Creates an immutable result. */
    public NpmCollectionResult {
      packages = List.copyOf(Objects.requireNonNull(packages, "packages"));
      missingLockFiles = List.copyOf(Objects.requireNonNull(missingLockFiles, "missingLockFiles"));
    }
  }

  /**
   * Result of writing a merged inventory.
   *
   * @param mergedPath path to the merged {@code THIRD-PARTY.txt} (or equivalent)
   * @param npmPackageCount number of production npm packages included
   * @param mavenPresent whether the Maven inventory file was present
   * @param missingLockFiles listed package-lock paths that were missing (empty when none)
   */
  public record GenerateResult(
      Path mergedPath, int npmPackageCount, boolean mavenPresent, List<Path> missingLockFiles) {

    /** Creates an immutable result. */
    public GenerateResult {
      Objects.requireNonNull(mergedPath, "mergedPath");
      missingLockFiles = List.copyOf(Objects.requireNonNull(missingLockFiles, "missingLockFiles"));
    }
  }

  /**
   * Reads every lockfile listed in {@code lockListFile} and unions production packages.
   *
   * <p>List file format (UTF-8): one path per line, relative to {@code projectRoot}. Blank lines
   * and lines whose first non-whitespace character is {@code #} are ignored. Paths may use {@code
   * /} or {@code \} separators; they are resolved with {@link Path}.
   *
   * <p>Missing listed lockfiles are recorded in {@link NpmCollectionResult#missingLockFiles()} —
   * they are not silently ignored without a trace. Use {@link
   * #requireCompleteNpmSources(NpmCollectionResult, Path)} to fail the build when sources are
   * incomplete.
   *
   * @param projectRoot root directory for resolving relative lock paths and source labels
   * @param lockListFile list of package-lock.json paths
   * @return packages plus any missing listed lock paths
   * @throws IOException if the list file exists but cannot be read, or a present lockfile cannot be
   *     parsed
   */
  public static NpmCollectionResult collectProductionPackagesFromLockList(
      Path projectRoot, Path lockListFile) throws IOException {
    Objects.requireNonNull(projectRoot, "projectRoot");
    Objects.requireNonNull(lockListFile, "lockListFile");
    if (!Files.isRegularFile(lockListFile)) {
      return new NpmCollectionResult(List.of(), List.of(), true);
    }
    List<Path> locks = readLockList(projectRoot, lockListFile);
    Map<String, NpmPackage> byKey = new TreeMap<>();
    List<Path> missing = new ArrayList<>();
    for (Path lock : locks) {
      if (!Files.isRegularFile(lock)) {
        missing.add(lock);
        continue;
      }
      for (NpmPackage pkg : readProductionPackagesFromLockFile(lock, projectRoot)) {
        byKey.put(pkg.name().toLowerCase(Locale.ROOT) + "@" + pkg.version(), pkg);
      }
    }
    List<NpmPackage> list = new ArrayList<>(byKey.values());
    Collections.sort(list);
    return new NpmCollectionResult(list, missing, false);
  }

  /**
   * Convenience wrapper: collects packages and fails if the lock-list file or any listed lockfile
   * is missing.
   *
   * @param projectRoot project root
   * @param lockListFile lock list
   * @return sorted production packages
   * @throws IOException on I/O failure
   * @throws IllegalStateException if the lock list or any listed lockfile is missing
   */
  public static List<NpmPackage> readProductionPackagesFromLockList(
      Path projectRoot, Path lockListFile) throws IOException {
    NpmCollectionResult result = collectProductionPackagesFromLockList(projectRoot, lockListFile);
    requireCompleteNpmSources(result, lockListFile);
    return result.packages();
  }

  /**
   * Fails when npm sources are incomplete (missing lock-list file or missing listed lockfiles).
   *
   * @param result collection result
   * @param lockListFile path used for the error message
   * @throws IllegalStateException if sources are incomplete
   */
  public static void requireCompleteNpmSources(NpmCollectionResult result, Path lockListFile) {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(lockListFile, "lockListFile");
    if (result.lockListFileMissing()) {
      throw new IllegalStateException(
          "npm package-lock list file is missing: "
              + lockListFile
              + ". Create the list (one package-lock.json path per line) or pass --lock-list.");
    }
    if (!result.missingLockFiles().isEmpty()) {
      StringBuilder sb = new StringBuilder("Missing package-lock.json file(s) listed in ");
      sb.append(lockListFile).append(':');
      for (Path p : result.missingLockFiles()) {
        sb.append("\n  - ").append(p);
      }
      throw new IllegalStateException(sb.toString());
    }
  }

  /**
   * Parses a lock-list file into absolute lockfile paths.
   *
   * @param projectRoot root for relative entries
   * @param lockListFile list file (must exist as a regular file)
   * @return absolute paths in list order (may include paths that do not yet exist on disk)
   * @throws IOException if the list file cannot be read
   * @throws IllegalStateException if the list file is not a regular file
   */
  public static List<Path> readLockList(Path projectRoot, Path lockListFile) throws IOException {
    Objects.requireNonNull(projectRoot, "projectRoot");
    Objects.requireNonNull(lockListFile, "lockListFile");
    if (!Files.isRegularFile(lockListFile)) {
      throw new IllegalStateException("npm package-lock list file is missing: " + lockListFile);
    }
    List<Path> out = new ArrayList<>();
    for (String raw : Files.readAllLines(lockListFile, StandardCharsets.UTF_8)) {
      String line = raw.strip();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      String normalized = line.replace('\\', '/');
      Path rel = Path.of("");
      for (String segment : normalized.split("/")) {
        if (!segment.isEmpty()) {
          rel = rel.resolve(segment);
        }
      }
      out.add(projectRoot.resolve(rel).normalize().toAbsolutePath());
    }
    return List.copyOf(out);
  }

  /**
   * Formats the npm section body (title line + package lines).
   *
   * @param packages production packages
   * @return section text ending with a trailing newline
   */
  public static String formatNpmSection(List<NpmPackage> packages) {
    Objects.requireNonNull(packages, "packages");
    StringBuilder sb = new StringBuilder();
    sb.append("Lists of ")
        .append(packages.size())
        .append(" third-party npm dependencies (production).\n\n");
    for (NpmPackage pkg : packages) {
      sb.append(pkg.toInventoryLine()).append('\n');
    }
    return sb.toString();
  }

  /**
   * Merges Maven inventory text and npm section text into a single document with clear section
   * headers.
   *
   * @param mavenInventoryText contents of the Maven inventory (may be blank)
   * @param npmInventoryText contents of the npm section (may be blank)
   * @param documentTitle first heading line (for example product name + “third-party dependency
   *     license inventory”); if null or blank a generic title is used
   * @return merged UTF-8 text ending with a trailing newline
   */
  public static String mergeMavenAndNpm(
      String mavenInventoryText, String npmInventoryText, String documentTitle) {
    String title =
        (documentTitle == null || documentTitle.isBlank())
            ? "Third-party dependency license inventory"
            : documentTitle.strip();
    String maven =
        mavenInventoryText == null || mavenInventoryText.isBlank()
            ? "(no Maven inventory provided)"
            : mavenInventoryText.strip();
    String npm =
        npmInventoryText == null || npmInventoryText.isBlank()
            ? "(no npm production dependencies found)"
            : npmInventoryText.strip();

    StringBuilder sb = new StringBuilder();
    sb.append(title).append('\n');
    sb.append("Generated at build time — do not hand-edit.\n\n");
    sb.append("================================================================================\n");
    sb.append("Maven third-party dependencies\n");
    sb.append(
        "================================================================================\n\n");
    sb.append(maven).append("\n\n");
    sb.append("================================================================================\n");
    sb.append("npm third-party dependencies (production)\n");
    sb.append(
        "================================================================================\n\n");
    sb.append(npm).append('\n');
    return sb.toString();
  }

  /**
   * Reads the Maven inventory and package-lock list, writes npm intermediate and merged outputs.
   *
   * <p>When {@code requireCompleteSources} is {@code true} (typical for product builds), both the
   * Maven inventory and the full npm lock-list (file present and every listed package-lock present)
   * are required. When {@code false}, missing Maven inventory yields an empty Maven section;
   * incomplete npm sources still fail only if the lock-list file is required by {@link
   * #requireCompleteNpmSources} — callers should pass {@code true} for CI.
   *
   * @param projectRoot project / repository root
   * @param outDir output directory (created if missing)
   * @param mavenFileName Maven inventory file name under {@code outDir}
   * @param npmFileName intermediate npm file name under {@code outDir}
   * @param mergedFileName merged file name under {@code outDir}
   * @param lockListFile package-lock list file
   * @param documentTitle title line for the merged document; may be null
   * @param requireCompleteSources if true, fail when Maven inventory or any npm source is missing
   * @return generate result including package count (no second lockfile pass required)
   * @throws IOException on I/O failure
   * @throws IllegalStateException if required sources are incomplete
   */
  public static GenerateResult generateMergedInventory(
      Path projectRoot,
      Path outDir,
      String mavenFileName,
      String npmFileName,
      String mergedFileName,
      Path lockListFile,
      String documentTitle,
      boolean requireCompleteSources)
      throws IOException {
    Objects.requireNonNull(projectRoot, "projectRoot");
    Objects.requireNonNull(outDir, "outDir");
    Objects.requireNonNull(mavenFileName, "mavenFileName");
    Objects.requireNonNull(npmFileName, "npmFileName");
    Objects.requireNonNull(mergedFileName, "mergedFileName");
    Objects.requireNonNull(lockListFile, "lockListFile");

    Files.createDirectories(outDir);
    Path mavenPath = outDir.resolve(mavenFileName);
    Path npmPath = outDir.resolve(npmFileName);
    Path mergedPath = outDir.resolve(mergedFileName);

    boolean mavenPresent = Files.isRegularFile(mavenPath);
    String mavenText;
    if (mavenPresent) {
      mavenText = Files.readString(mavenPath, StandardCharsets.UTF_8);
    } else if (requireCompleteSources) {
      throw new IllegalStateException(
          "Maven inventory missing: "
              + mavenPath
              + ". Run the Maven license aggregate goal first.");
    } else {
      mavenText = "";
    }

    NpmCollectionResult npmResult =
        collectProductionPackagesFromLockList(projectRoot, lockListFile);
    if (requireCompleteSources) {
      requireCompleteNpmSources(npmResult, lockListFile);
    }

    String npmText = formatNpmSection(npmResult.packages());
    writeUtf8Lf(npmPath, npmText);

    String merged = mergeMavenAndNpm(mavenText, npmText, documentTitle);
    writeUtf8Lf(mergedPath, merged);
    return new GenerateResult(
        mergedPath, npmResult.packages().size(), mavenPresent, npmResult.missingLockFiles());
  }

  /**
   * Command-line entry point for build integration ({@code exec-maven-plugin:java} or direct
   * invocation).
   *
   * <p>Flags:
   *
   * <ul>
   *   <li>{@code --root <dir>} — project root (required)
   *   <li>{@code --out-dir <dir>} — output directory (default: {@code
   *       <root>/target/generated-sources/license})
   *   <li>{@code --lock-list <file>} — package-lock list (default: {@code
   *       <root>/src/license/npm-package-locks.txt})
   *   <li>{@code --title <text>} — merged document title
   *   <li>{@code --require-maven} — fail if Maven inventory <em>or</em> npm lock-list / listed
   *       package-lock files are missing (strict product-build mode)
   *   <li>{@code --maven-name}, {@code --npm-name}, {@code --merged-name} — override file names
   * </ul>
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    int code = runMain(args, System.out, System.err);
    if (code != 0) {
      System.exit(code);
    }
  }

  /**
   * Testable {@link #main(String[])} implementation.
   *
   * @param args CLI args
   * @param out stdout
   * @param err stderr
   * @return process exit code (0 success)
   */
  static int runMain(String[] args, PrintStream out, PrintStream err) {
    Path root = null;
    Path outDir = null;
    Path lockList = null;
    String title = null;
    boolean requireCompleteSources = false;
    String mavenName = DEFAULT_MAVEN_FILE_NAME;
    String npmName = DEFAULT_NPM_FILE_NAME;
    String mergedName = DEFAULT_MERGED_FILE_NAME;

    for (int i = 0; i < args.length; i++) {
      String a = args[i];
      try {
        switch (a) {
          case "--root" -> root = Path.of(requireValue(args, ++i, a));
          case "--out-dir" -> outDir = Path.of(requireValue(args, ++i, a));
          case "--lock-list" -> lockList = Path.of(requireValue(args, ++i, a));
          case "--title" -> title = requireValue(args, ++i, a);
          case "--require-maven" -> requireCompleteSources = true;
          case "--maven-name" -> mavenName = requireValue(args, ++i, a);
          case "--npm-name" -> npmName = requireValue(args, ++i, a);
          case "--merged-name" -> mergedName = requireValue(args, ++i, a);
          case "--help", "-h" -> {
            printUsage(out);
            return 0;
          }
          default -> {
            err.println("Unknown argument: " + a);
            printUsage(err);
            return 2;
          }
        }
      } catch (IllegalArgumentException ex) {
        err.println(ex.getMessage());
        printUsage(err);
        return 2;
      }
    }

    if (root == null) {
      err.println("--root is required");
      printUsage(err);
      return 2;
    }

    root = root.toAbsolutePath().normalize();
    if (outDir == null) {
      outDir = root.resolve("target").resolve("generated-sources").resolve("license");
    } else {
      outDir = outDir.toAbsolutePath().normalize();
    }
    if (lockList == null) {
      lockList = root.resolve("src").resolve("license").resolve("npm-package-locks.txt");
    } else {
      lockList = lockList.toAbsolutePath().normalize();
    }

    try {
      GenerateResult result =
          generateMergedInventory(
              root,
              outDir,
              mavenName,
              npmName,
              mergedName,
              lockList,
              title,
              requireCompleteSources);
      if (!requireCompleteSources && !result.missingLockFiles().isEmpty()) {
        err.println("WARNING: missing package-lock.json file(s) (npm inventory incomplete):");
        for (Path p : result.missingLockFiles()) {
          err.println("  - " + p);
        }
      }
      out.println(
          "Wrote "
              + result.mergedPath()
              + " (Maven present="
              + result.mavenPresent()
              + ", npm packages="
              + result.npmPackageCount()
              + ")");
      return 0;
    } catch (IllegalStateException | IllegalArgumentException ex) {
      err.println("ERROR: " + ex.getMessage());
      return 1;
    } catch (IOException ex) {
      err.println("ERROR: " + ex.getMessage());
      return 1;
    }
  }

  private static void printUsage(PrintStream out) {
    out.println(
        "Usage: ThirdPartyLicenseInventory --root <dir> [--out-dir <dir>] [--lock-list <file>]");
    out.println("       [--title <text>] [--require-maven] [--maven-name name] [--npm-name name]");
    out.println("       [--merged-name name]");
    out.println(
        "  --require-maven  Fail if Maven inventory or npm lock-list / listed locks are missing");
  }

  private static String requireValue(String[] args, int index, String flag) {
    if (index >= args.length) {
      throw new IllegalArgumentException("Missing value for " + flag);
    }
    return args[index];
  }

  private static void writeUtf8Lf(Path path, String content) throws IOException {
    String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
    if (!normalized.endsWith("\n")) {
      normalized = normalized + "\n";
    }
    Files.writeString(path, normalized, StandardCharsets.UTF_8);
  }

  private static String labelFor(Path packageLockJson, Path sourceRoot) {
    Path abs = packageLockJson.toAbsolutePath().normalize();
    if (sourceRoot != null) {
      try {
        return sourceRoot
            .toAbsolutePath()
            .normalize()
            .relativize(abs)
            .toString()
            .replace('\\', '/');
      } catch (IllegalArgumentException ignored) {
        // different roots
      }
    }
    return abs.toString().replace('\\', '/');
  }

  /**
   * Maps a package-lock {@code packages} key to an npm package name, or {@code null} if the key is
   * not a {@code node_modules} install path.
   */
  static String packageNameFromLockKey(String key) {
    if (key == null || key.isBlank() || ".".equals(key)) {
      return null;
    }
    String norm = key.replace('\\', '/');
    String marker = "node_modules/";
    int idx = norm.lastIndexOf(marker);
    if (idx < 0) {
      return null;
    }
    String name = norm.substring(idx + marker.length());
    return name.isBlank() ? null : name;
  }

  private static boolean isTruthy(Object value) {
    if (value instanceof Boolean b) {
      return b;
    }
    if (value instanceof String s) {
      return "true".equalsIgnoreCase(s);
    }
    return false;
  }

  private static String stringOrEmpty(Object value) {
    return value == null ? "" : String.valueOf(value).strip();
  }

  private static String licenseFromMeta(Object licenseNode) {
    if (licenseNode == null) {
      return "Unknown license";
    }
    if (licenseNode instanceof String s) {
      return s.isBlank() ? "Unknown license" : s;
    }
    if (licenseNode instanceof Map<?, ?> map) {
      Object type = map.get("type");
      if (type == null) {
        type = map.get("name");
      }
      String s = stringOrEmpty(type);
      return s.isBlank() ? "Unknown license" : s;
    }
    if (licenseNode instanceof List<?> list) {
      StringBuilder sb = new StringBuilder();
      for (Object o : list) {
        if (sb.length() > 0) {
          sb.append(" OR ");
        }
        sb.append(o);
      }
      return sb.length() == 0 ? "Unknown license" : sb.toString();
    }
    return String.valueOf(licenseNode);
  }

  /**
   * Minimal JSON parser sufficient for npm package-lock documents. Not a general-purpose JSON
   * library; kept dependency-free for this module.
   */
  static final class MinimalJson {
    private MinimalJson() {}

    static Object parse(String json) {
      return new Parser(json).parseValue();
    }

    private static final class Parser {
      private final String s;
      private int i;

      Parser(String s) {
        this.s = s;
      }

      Object parseValue() {
        skipWs();
        if (i >= s.length()) {
          throw new IllegalArgumentException("Unexpected end of JSON");
        }
        char c = s.charAt(i);
        if (c == '{') {
          return parseObject();
        }
        if (c == '[') {
          return parseArray();
        }
        if (c == '"') {
          return parseString();
        }
        if (c == 't' || c == 'f') {
          return parseBoolean();
        }
        if (c == 'n') {
          return parseNull();
        }
        if (c == '-' || (c >= '0' && c <= '9')) {
          return parseNumber();
        }
        throw new IllegalArgumentException("Unexpected character at " + i + ": " + c);
      }

      private Map<String, Object> parseObject() {
        expect('{');
        skipWs();
        Map<String, Object> map = new LinkedHashMap<>();
        if (peek('}')) {
          i++;
          return map;
        }
        while (true) {
          skipWs();
          String key = parseString();
          skipWs();
          expect(':');
          Object value = parseValue();
          map.put(key, value);
          skipWs();
          if (peek('}')) {
            i++;
            return map;
          }
          expect(',');
        }
      }

      private List<Object> parseArray() {
        expect('[');
        skipWs();
        List<Object> list = new ArrayList<>();
        if (peek(']')) {
          i++;
          return list;
        }
        while (true) {
          list.add(parseValue());
          skipWs();
          if (peek(']')) {
            i++;
            return list;
          }
          expect(',');
        }
      }

      private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
          char c = s.charAt(i++);
          if (c == '"') {
            return sb.toString();
          }
          if (c == '\\') {
            if (i >= s.length()) {
              throw new IllegalArgumentException("Unterminated escape");
            }
            char e = s.charAt(i++);
            sb.append(
                switch (e) {
                  case '"', '\\', '/' -> e;
                  case 'b' -> '\b';
                  case 'f' -> '\f';
                  case 'n' -> '\n';
                  case 'r' -> '\r';
                  case 't' -> '\t';
                  case 'u' -> {
                    if (i + 4 > s.length()) {
                      throw new IllegalArgumentException("Bad unicode escape");
                    }
                    int code = Integer.parseInt(s.substring(i, i + 4), 16);
                    i += 4;
                    yield (char) code;
                  }
                  default -> throw new IllegalArgumentException("Bad escape: \\" + e);
                });
          } else {
            sb.append(c);
          }
        }
        throw new IllegalArgumentException("Unterminated string");
      }

      private Boolean parseBoolean() {
        if (s.startsWith("true", i)) {
          i += 4;
          return Boolean.TRUE;
        }
        if (s.startsWith("false", i)) {
          i += 5;
          return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Invalid boolean at " + i);
      }

      private Object parseNull() {
        if (s.startsWith("null", i)) {
          i += 4;
          return null;
        }
        throw new IllegalArgumentException("Invalid null at " + i);
      }

      private Number parseNumber() {
        int start = i;
        if (peek('-')) {
          i++;
        }
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
          i++;
        }
        if (peek('.')) {
          i++;
          while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
          }
        }
        if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
          i++;
          if (peek('+') || peek('-')) {
            i++;
          }
          while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
          }
        }
        String num = s.substring(start, i);
        if (num.contains(".") || num.contains("e") || num.contains("E")) {
          return Double.valueOf(num);
        }
        try {
          return Long.valueOf(num);
        } catch (NumberFormatException ex) {
          return Double.valueOf(num);
        }
      }

      private void skipWs() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
          i++;
        }
      }

      private boolean peek(char c) {
        return i < s.length() && s.charAt(i) == c;
      }

      private void expect(char c) {
        skipWs();
        if (i >= s.length() || s.charAt(i) != c) {
          throw new IllegalArgumentException("Expected '" + c + "' at " + i);
        }
        i++;
      }
    }
  }
}

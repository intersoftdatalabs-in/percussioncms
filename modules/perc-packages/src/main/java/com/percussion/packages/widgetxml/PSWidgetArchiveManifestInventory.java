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

package com.percussion.packages.widgetxml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Inventory and install-time rewrite for Widget definition XML paths in product package archive
 * descriptors ({@code psx_archiveInfo.xml} / {@code psx_archiveManifest.xml}).
 *
 * <p>Phase 3 leftover of parent #2630 / issue #3582: product packages with modern {@code
 * widgets/} roots must not <em>author</em> {@code rxconfig/Widgets/*.xml} user-dependencies. Package
 * build still materializes install Widget XML and re-injects those archive entries so deployer /
 * {@code PSWidgetDao} keep the legacy wire format. Waiver set is empty after {@code perc.Test}
 * ship-exit (#3736).
 *
 * <p>Archive/ZIP logical paths use {@code /}. On-disk I/O uses {@link Path#resolve(String)}.
 *
 * @see PSWidgetXmlInstallEmitter
 * @see PSWidgetDefinitionXmlInventory
 */
public final class PSWidgetArchiveManifestInventory {

  /** Same explicit waiver as committed Widget XML files (empty after #3736). */
  public static final Set<String> WAIVED_PACKAGE_DIRS =
      PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS;

  public static final String ARCHIVE_INFO_FILE_NAME = "psx_archiveInfo.xml";
  public static final String ARCHIVE_MANIFEST_FILE_NAME = "psx_archiveManifest.xml";

  /** Install-relative Widget definition path prefix (ZIP / archive / URL form). */
  public static final String WIDGET_XML_INSTALL_PREFIX = "rxconfig/Widgets/";

  /** Encoded archive-folder token for {@code rxconfig/Widgets} (package-build layout). */
  public static final String WIDGET_XML_ENCODED_TOKEN = "rxconfig_Widgets_";

  private static final Pattern ARCHIVE_INFO_WIDGET_DEP =
      Pattern.compile(
          "<PSXUserDependency\\b[^>]*\\bpath\\s*=\\s*\"rxconfig[/\\\\]Widgets/[^\"]+\"[^>]*>.*?</PSXUserDependency>",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  private static final Pattern ARCHIVE_MANIFEST_WIDGET_DEP =
      Pattern.compile(
          "<PSXDepFilesIdTypes\\b[^>]*DependencyKey\\s*=\\s*\"[^\"]*"
              + Pattern.quote(WIDGET_XML_ENCODED_TOKEN)
              + "[^\"]+\"[^>]*>.*?</PSXDepFilesIdTypes>",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  private static final Pattern ARCHIVE_INFO_PATH_ATTR =
      Pattern.compile(
          "\\b(?:path|dependencyId)\\s*=\\s*\"(rxconfig[/\\\\]Widgets/[^\"]+)\"",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern ARCHIVE_MANIFEST_KEY_ATTR =
      Pattern.compile(
          "DependencyKey\\s*=\\s*\"([^\"]*"
              + Pattern.quote(WIDGET_XML_ENCODED_TOKEN)
              + "[^\"]+)\"",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern RXFILE_WIDGET_XML =
      Pattern.compile(
          "<RxFile>\\s*[^<]*rxconfig[/\\\\]Widgets[/\\\\]([^<]+?)\\s*</RxFile>",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern SYS_USER_DEP_DEPENDENCIES =
      Pattern.compile(
          "(dependencyId\\s*=\\s*\"sys_UserDependency\"[\\s\\S]*?<Dependencies>)",
          Pattern.CASE_INSENSITIVE);

  private PSWidgetArchiveManifestInventory() {
    // utility
  }

  /**
   * One authored Widget definition XML path in an archive descriptor.
   *
   * @param packageDirName immediate child under Packages
   * @param descriptorFile {@link #ARCHIVE_INFO_FILE_NAME} or {@link #ARCHIVE_MANIFEST_FILE_NAME}
   * @param excerpt path, dependencyId, or DependencyKey
   * @param waived whether the package is waived
   */
  public record Finding(
      String packageDirName, String descriptorFile, String excerpt, boolean waived) {}

  /**
   * Scan result for a Packages root.
   *
   * @param all waived + non-waived findings, sorted
   * @param nonWaived findings whose package is not waived
   * @param waived findings whose package is waived
   */
  public record Report(List<Finding> all, List<Finding> nonWaived, List<Finding> waived) {

    public boolean isClean() {
      return nonWaived.isEmpty();
    }
  }

  /**
   * Scan every immediate package directory for authored Widget def XML archive paths.
   *
   * @param packagesRoot {@code modules/perc-packages/src/main/resources/Packages}
   * @return report; never null
   */
  public static Report scan(Path packagesRoot) throws IOException {
    Objects.requireNonNull(packagesRoot, "packagesRoot");
    if (!Files.isDirectory(packagesRoot)) {
      throw new IllegalArgumentException("Packages root is not a directory: " + packagesRoot);
    }

    List<Finding> all = new ArrayList<>();
    try (DirectoryStream<Path> packages = Files.newDirectoryStream(packagesRoot)) {
      for (Path packageDir : packages) {
        if (!Files.isDirectory(packageDir)) {
          continue;
        }
        Path namePath = packageDir.getFileName();
        if (namePath == null) {
          continue;
        }
        String packageDirName = namePath.toString();
        boolean waived = isWaivedPackage(packageDirName);
        all.addAll(scanPackage(packageDir, packageDirName, waived));
      }
    }

    all.sort(
        Comparator.comparing((Finding f) -> f.packageDirName().toLowerCase(Locale.ROOT))
            .thenComparing(f -> f.descriptorFile().toLowerCase(Locale.ROOT))
            .thenComparing(f -> f.excerpt().toLowerCase(Locale.ROOT)));

    List<Finding> nonWaived = all.stream().filter(f -> !f.waived()).collect(Collectors.toList());
    List<Finding> waivedOnly = all.stream().filter(Finding::waived).collect(Collectors.toList());
    return new Report(
        Collections.unmodifiableList(all),
        Collections.unmodifiableList(nonWaived),
        Collections.unmodifiableList(waivedOnly));
  }

  /**
   * Fail-fast Surefire / CI gate: non-waived product archive manifests must not author Widget def
   * XML paths.
   */
  public static void assertNoNonWaivedWidgetXmlArchivePaths(Path packagesRoot) throws IOException {
    Report report = scan(packagesRoot);
    if (report.isClean()) {
      return;
    }
    String detail =
        report.nonWaived().stream()
            .map(f -> f.packageDirName() + " " + f.descriptorFile() + " -> " + f.excerpt())
            .collect(Collectors.joining(System.lineSeparator() + "  "));
    throw new IllegalStateException(
        "Archive-manifest Widget XML gate (#3582): non-waived product package authored "
            + "rxconfig/Widgets/*.xml paths under "
            + packagesRoot
            + " (only waived package dirs: "
            + WAIVED_PACKAGE_DIRS
            + "):"
            + System.lineSeparator()
            + "  "
            + detail);
  }

  public static boolean isWaivedPackage(String packageDirName) {
    return PSWidgetDefinitionXmlInventory.isWaivedPackage(packageDirName);
  }

  /**
   * Whether {@code xml} authors a Widget definition XML install path or encoded archive token.
   */
  public static boolean containsWidgetXmlArchivePath(String xml) {
    if (xml == null || xml.isBlank()) {
      return false;
    }
    return !listWidgetXmlArchiveExcerpts(xml).isEmpty();
  }

  /** Distinct path / DependencyKey excerpts for Widget def XML in an archive descriptor. */
  public static List<String> listWidgetXmlArchiveExcerpts(String xml) {
    if (xml == null || xml.isBlank()) {
      return List.of();
    }
    LinkedHashSet<String> found = new LinkedHashSet<>();
    Matcher paths = ARCHIVE_INFO_PATH_ATTR.matcher(xml);
    while (paths.find()) {
      found.add(normalizeInstallPath(paths.group(1)));
    }
    Matcher keys = ARCHIVE_MANIFEST_KEY_ATTR.matcher(xml);
    while (keys.find()) {
      found.add(keys.group(1));
    }
    Matcher rxFiles = RXFILE_WIDGET_XML.matcher(xml);
    while (rxFiles.find()) {
      found.add(WIDGET_XML_INSTALL_PREFIX + rxFiles.group(1).trim());
    }
    return List.copyOf(found);
  }

  /**
   * Remove authored Widget def XML user-dependency blocks from archive-info or archive-manifest
   * XML. Other user-dependencies are left unchanged.
   */
  public static String stripWidgetXmlArchivePaths(String xml) {
    if (xml == null || xml.isBlank()) {
      return xml;
    }
    String stripped = ARCHIVE_INFO_WIDGET_DEP.matcher(xml).replaceAll("");
    stripped = ARCHIVE_MANIFEST_WIDGET_DEP.matcher(stripped).replaceAll("");
    return stripped;
  }

  /**
   * Strip Widget def XML archive paths from a package's source descriptors when the package is
   * non-waived and has modern {@code widgets/} roots. No-op for waived packages (none after #3736)
   * and packages without modern roots.
   *
   * @return number of descriptor files rewritten
   */
  public static int stripAuthoredWidgetXmlArchivePaths(Path packageDir) throws IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    if (!Files.isDirectory(packageDir)) {
      return 0;
    }
    Path namePath = packageDir.getFileName();
    String name = namePath != null ? namePath.toString() : "";
    if (isWaivedPackage(name)) {
      return 0;
    }
    if (!PSWidgetXmlDualShip.hasModernWidgetSources(packageDir)) {
      return 0;
    }
    int rewritten = 0;
    for (String fileName : List.of(ARCHIVE_INFO_FILE_NAME, ARCHIVE_MANIFEST_FILE_NAME)) {
      Path file = packageDir.resolve(fileName);
      if (!Files.isRegularFile(file)) {
        continue;
      }
      String original = Files.readString(file, StandardCharsets.UTF_8);
      String stripped = stripWidgetXmlArchivePaths(original);
      if (!stripped.equals(original)) {
        Files.writeString(file, stripped, StandardCharsets.UTF_8);
        rewritten++;
      }
    }
    return rewritten;
  }

  /**
   * Ensure staged archive descriptors list install Widget XML user-dependencies for each stem.
   * Idempotent. Creates a minimal {@code psx_archiveManifest.xml} when missing; updates {@code
   * psx_archiveInfo.xml} only when that file already exists.
   *
   * @return number of descriptor files written or updated
   */
  public static int ensureInstallWidgetXmlArchivePaths(Path packageDir, List<String> widgetStems)
      throws IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    if (widgetStems == null || widgetStems.isEmpty()) {
      return 0;
    }
    List<String> stems = new ArrayList<>();
    LinkedHashSet<String> seen = new LinkedHashSet<>();
    for (String stem : widgetStems) {
      if (stem == null || stem.isBlank()) {
        continue;
      }
      String trimmed = stem.trim();
      if (seen.add(trimmed)) {
        stems.add(trimmed);
      }
    }
    if (stems.isEmpty()) {
      return 0;
    }

    int updated = 0;
    Path info = packageDir.resolve(ARCHIVE_INFO_FILE_NAME);
    if (Files.isRegularFile(info)) {
      String original = Files.readString(info, StandardCharsets.UTF_8);
      String next = injectArchiveInfoEntries(original, stems);
      if (!next.equals(original)) {
        Files.writeString(info, next, StandardCharsets.UTF_8);
        updated++;
      }
    }

    Path manifest = packageDir.resolve(ARCHIVE_MANIFEST_FILE_NAME);
    String originalManifest =
        Files.isRegularFile(manifest)
            ? Files.readString(manifest, StandardCharsets.UTF_8)
            : "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n\n<PSXArchiveManifest>\n</PSXArchiveManifest>\n";
    String nextManifest = injectArchiveManifestEntries(originalManifest, stems);
    if (!Files.isRegularFile(manifest) || !nextManifest.equals(originalManifest)) {
      Files.writeString(manifest, nextManifest, StandardCharsets.UTF_8);
      updated++;
    }
    return updated;
  }

  /**
   * Archive-folder name used by {@link com.percussion.packages.PSPackageBuilder} for a Widget XML
   * user-dependency ({@code sys__UserDependency--rxconfig_Widgets_&lt;stem&gt;-xml}). ZIP logical
   * path — always {@code /} in the file name portion.
   */
  public static String encodeWidgetXmlArchiveFolder(String widgetFileName) {
    Objects.requireNonNull(widgetFileName, "widgetFileName");
    String fileName = widgetFileName.trim();
    if (fileName.isEmpty()) {
      throw new IllegalArgumentException("widgetFileName is blank");
    }
    // Match PSPackageBuilder user-dependency encoding (ZIP layout, not OS separators).
    String encodedPath = "rxconfig/Widgets".replace("_", "__").replace(".", "-").replace("/", "_");
    String encodedSuffix = fileName.replace("-", "--").replace(".", "-").replace("_", "__");
    return "sys__UserDependency--" + encodedPath + "_" + encodedSuffix;
  }

  public static String widgetXmlInstallPath(String stem) {
    return WIDGET_XML_INSTALL_PREFIX + requireStem(stem) + ".xml";
  }

  /**
   * CLI: {@code scan <packagesRoot>} or {@code strip <packagesRoot>}.
   *
   * @param args command + packages root
   */
  public static void main(String[] args) throws IOException {
    if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
      System.err.println("Usage: PSWidgetArchiveManifestInventory scan|strip <packagesRoot>");
      System.exit(2);
      return;
    }
    String cmd = args[0].trim().toLowerCase(Locale.ROOT);
    Path root = Path.of(args[1]).toAbsolutePath().normalize();
    if ("strip".equals(cmd)) {
      int rewritten = 0;
      try (DirectoryStream<Path> packages = Files.newDirectoryStream(root)) {
        for (Path packageDir : packages) {
          if (Files.isDirectory(packageDir)) {
            rewritten += stripAuthoredWidgetXmlArchivePaths(packageDir);
          }
        }
      }
      System.out.println("Stripped Widget XML archive paths from " + rewritten + " descriptor(s)");
    }
    Report report = scan(root);
    System.out.println(
        "Widget XML archive-manifest inventory under "
            + root
            + ": total="
            + report.all().size()
            + " waived="
            + report.waived().size()
            + " nonWaived="
            + report.nonWaived().size()
            + " waivedPackages="
            + WAIVED_PACKAGE_DIRS);
    for (Finding f : report.all()) {
      System.out.println(
          (f.waived() ? "WAIVED " : "FAIL   ")
              + f.packageDirName()
              + " "
              + f.descriptorFile()
              + " "
              + f.excerpt());
    }
    if (!report.isClean()) {
      System.err.println("FAIL: non-waived Widget XML archive-manifest paths present (#3582)");
      System.exit(1);
    }
    System.out.println("PASS: zero non-waived Widget XML archive-manifest paths");
  }

  static String injectArchiveInfoEntries(String xml, List<String> stems) {
    String working = xml;
    String nl = working.contains("\r\n") ? "\r\n" : "\n";
    List<String> missing = new ArrayList<>();
    for (String stem : stems) {
      String path = widgetXmlInstallPath(stem);
      if (!containsExactInstallPath(working, path)) {
        missing.add(stem);
      }
    }
    if (missing.isEmpty()) {
      return working;
    }
    StringBuilder block = new StringBuilder();
    for (String stem : missing) {
      block.append(archiveInfoUserDependencyBlock(stem, nl));
    }
    Matcher parent = SYS_USER_DEP_DEPENDENCIES.matcher(working);
    if (parent.find()) {
      return working.substring(0, parent.end()) + block + working.substring(parent.end());
    }
    int packagesClose = indexOfIgnoreCase(working, "</Packages>");
    if (packagesClose < 0) {
      throw new IllegalStateException(
          "Cannot inject Widget XML user-dependencies: no sys_UserDependency <Dependencies> "
              + "and no </Packages> in psx_archiveInfo.xml");
    }
    return working.substring(0, packagesClose)
        + sysUserDependencyElement(block.toString(), nl)
        + working.substring(packagesClose);
  }

  static String injectArchiveManifestEntries(String xml, List<String> stems) {
    String working = xml;
    String nl = working.contains("\r\n") ? "\r\n" : "\n";
    List<String> missing = new ArrayList<>();
    for (String stem : stems) {
      String folder = encodeWidgetXmlArchiveFolder(stem + ".xml");
      if (!working.contains(folder)) {
        missing.add(stem);
      }
    }
    if (missing.isEmpty()) {
      return working;
    }
    StringBuilder block = new StringBuilder();
    for (String stem : missing) {
      block.append(archiveManifestDepBlock(stem, nl));
    }
    int close = indexOfIgnoreCase(working, "</PSXArchiveManifest>");
    if (close < 0) {
      throw new IllegalStateException(
          "Cannot inject Widget XML archive-manifest entries: missing </PSXArchiveManifest>");
    }
    return working.substring(0, close) + block + working.substring(close);
  }

  private static List<Finding> scanPackage(Path packageDir, String packageDirName, boolean waived)
      throws IOException {
    List<Finding> findings = new ArrayList<>();
    for (String fileName : List.of(ARCHIVE_INFO_FILE_NAME, ARCHIVE_MANIFEST_FILE_NAME)) {
      Path file = packageDir.resolve(fileName);
      if (!Files.isRegularFile(file)) {
        continue;
      }
      String xml = Files.readString(file, StandardCharsets.UTF_8);
      for (String excerpt : listWidgetXmlArchiveExcerpts(xml)) {
        findings.add(new Finding(packageDirName, fileName, excerpt, waived));
      }
    }
    return findings;
  }

  private static String archiveInfoUserDependencyBlock(String stem, String nl) {
    String fileName = requireStem(stem) + ".xml";
    String path = WIDGET_XML_INSTALL_PREFIX + fileName;
    return nl
        + "\t\t\t\t\t\t\t<PSXUserDependency"
        + nl
        + "\t\t\t\t\t\t\t\tparentId=\"sys_UserDependency\""
        + nl
        + "\t\t\t\t\t\t\t\tparentKey=\"Custom-sys_UserDependency\" parentType=\"Custom\""
        + nl
        + "\t\t\t\t\t\t\t\tpath=\""
        + path
        + "\">"
        + nl
        + "\t\t\t\t\t\t\t\t<PSXDeployableObject>"
        + nl
        + "\t\t\t\t\t\t\t\t\t<PSXDependency autoDep=\"no\" autoExpand=\"yes\""
        + nl
        + "\t\t\t\t\t\t\t\t\t\tdependencyId=\""
        + path
        + "\""
        + nl
        + "\t\t\t\t\t\t\t\t\t\tdependencyType=\"User\" displayName=\""
        + fileName
        + "\""
        + nl
        + "\t\t\t\t\t\t\t\t\t\tisAssociation=\"no\" isIncluded=\"yes\""
        + nl
        + "\t\t\t\t\t\t\t\t\t\tobjectType=\"sys_UserDependency\""
        + nl
        + "\t\t\t\t\t\t\t\t\t\tobjectTypeName=\"User Dependency\" supportsIdMapping=\"no\""
        + nl
        + "\t\t\t\t\t\t\t\t\t\tsupportsIdTypes=\"no\" supportsParentId=\"no\""
        + nl
        + "\t\t\t\t\t\t\t\t\t\tsupportsUserDependencies=\"no\">"
        + nl
        + "\t\t\t\t\t\t\t\t\t\t<Dependencies />"
        + nl
        + "\t\t\t\t\t\t\t\t\t</PSXDependency>"
        + nl
        + "\t\t\t\t\t\t\t\t\t<RequiredClasses />"
        + nl
        + "\t\t\t\t\t\t\t\t</PSXDeployableObject>"
        + nl
        + "\t\t\t\t\t\t\t</PSXUserDependency>";
  }

  private static String sysUserDependencyElement(String innerDeps, String nl) {
    return "\t\t\t\t<PSXDeployableElement>"
        + nl
        + "\t\t\t\t\t<PSXDependency autoDep=\"no\" autoExpand=\"yes\""
        + nl
        + "\t\t\t\t\t\tdependencyId=\"sys_UserDependency\" dependencyType=\"Shared\""
        + nl
        + "\t\t\t\t\t\tdisplayName=\"User Dependency\" isAssociation=\"yes\" isIncluded=\"yes\""
        + nl
        + "\t\t\t\t\t\tobjectType=\"Custom\" objectTypeName=\"Custom\" supportsIdMapping=\"no\""
        + nl
        + "\t\t\t\t\t\tsupportsIdTypes=\"no\" supportsParentId=\"no\""
        + nl
        + "\t\t\t\t\t\tsupportsUserDependencies=\"yes\">"
        + nl
        + "\t\t\t\t\t\t<Dependencies>"
        + innerDeps
        + nl
        + "\t\t\t\t\t\t</Dependencies>"
        + nl
        + "\t\t\t\t\t</PSXDependency>"
        + nl
        + "\t\t\t\t\t<Description />"
        + nl
        + "\t\t\t\t</PSXDeployableElement>"
        + nl;
  }

  private static String archiveManifestDepBlock(String stem, String nl) {
    String fileName = requireStem(stem) + ".xml";
    String folder = encodeWidgetXmlArchiveFolder(fileName);
    String installPath = WIDGET_XML_INSTALL_PREFIX + fileName;
    // ArchiveFile is a ZIP entry path — always '/'.
    return "\t<PSXDepFilesIdTypes"
        + nl
        + "\t\tDependencyKey=\""
        + folder
        + "\">"
        + nl
        + "\t\t<PSXDependencyFile fileType=\"SUPPORT_FILE\">"
        + nl
        + "\t\t\t<RxFile>"
        + installPath
        + "</RxFile>"
        + nl
        + "\t\t\t<ArchiveFile>"
        + folder
        + "/"
        + fileName
        + "</ArchiveFile>"
        + nl
        + "\t\t</PSXDependencyFile>"
        + nl
        + "\t</PSXDepFilesIdTypes>"
        + nl;
  }

  private static boolean containsExactInstallPath(String xml, String path) {
    return xml.contains("path=\"" + path + "\"") || xml.contains("dependencyId=\"" + path + "\"");
  }

  private static String normalizeInstallPath(String raw) {
    return raw.replace('\\', '/');
  }

  private static String requireStem(String stem) {
    if (stem == null || stem.isBlank()) {
      throw new IllegalArgumentException("widget stem is blank");
    }
    String trimmed = stem.trim();
    if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..")) {
      throw new IllegalArgumentException("widget stem must be a single path segment: " + stem);
    }
    return trimmed;
  }

  private static int indexOfIgnoreCase(String haystack, String needle) {
    return haystack.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT));
  }
}

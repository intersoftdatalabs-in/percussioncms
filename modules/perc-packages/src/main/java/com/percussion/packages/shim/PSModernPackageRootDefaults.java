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

package com.percussion.packages.shim;

import com.percussion.packages.widgetxml.PSWidgetXmlDualShip;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Default modern package roots for product / H2 installs so dual-run widget selection is
 * modern-first without manual Spring property surgery (issue #3130 / parent #2630).
 *
 * <p>Well-known install relative directory: {@value #RELATIVE_MODERN_ROOTS_DIR}. When {@code
 * widgetDao.modernPackageRoots} is blank, {@link #resolve(String, Path, ClassLoader)} discovers
 * package roots under that directory (after optionally materializing modern {@code widgets/} trees
 * from the product classpath {@code Packages/&lt;pkg&gt;/widgets/…}). Explicit {@link
 * File#pathSeparator} lists still override defaults. Missing modern trees leave an empty list so
 * legacy Widgets XML remains the fallback via {@link PSLegacyDefinitionXmlShim}.
 *
 * <p>Portable {@link Path} / {@link Files} only (Windows / Linux / macOS).
 */
public final class PSModernPackageRootDefaults {

  /**
   * Install-relative directory holding product modern package roots for dual-run selection.
   *
   * <p>Layout: {@code Packages/Modern/&lt;packageName&gt;/widgets/&lt;stem&gt;/component-package.json}
   */
  public static final String RELATIVE_MODERN_ROOTS_DIR = "Packages/Modern";

  /** Classpath prefix for product package sources inside {@code perc-packages}. */
  public static final String CLASSPATH_PACKAGES_PREFIX = "Packages/";

  private PSModernPackageRootDefaults() {
    // utility
  }

  /**
   * Resolves modern package roots for dual-run selection.
   *
   * <ol>
   *   <li>When {@code rootsProperty} is non-blank: parse {@link File#pathSeparator}-separated
   *       paths (explicit operator override).
   *   <li>When blank: discover under {@code rxDeployDir}/{@value #RELATIVE_MODERN_ROOTS_DIR},
   *       materializing from the product classpath when the install tree is empty/missing and a
   *       class loader is provided.
   * </ol>
   *
   * @param rootsProperty Spring {@code widgetDao.modernPackageRoots} value (may be blank)
   * @param rxDeployDir install root ({@code ${rxdeploydir}}); may be null when property is
   *     explicit
   * @param classLoader optional loader used to materialize classpath modern widgets; may be null
   *     to skip materialize
   * @return unmodifiable list of normalized package-root directories (never null; may be empty)
   * @throws IOException if discovery or materialize I/O fails
   */
  public static List<Path> resolve(String rootsProperty, Path rxDeployDir, ClassLoader classLoader)
      throws IOException {
    if (rootsProperty != null && !rootsProperty.isBlank()) {
      return parsePathSeparatorList(rootsProperty);
    }
    if (rxDeployDir == null) {
      return List.of();
    }
    Path modernDir = rxDeployDir.toAbsolutePath().normalize().resolve(RELATIVE_MODERN_ROOTS_DIR);
    List<Path> existing = discoverPackageRoots(modernDir);
    if (!existing.isEmpty()) {
      return existing;
    }
    if (classLoader != null) {
      int written = materializeFromClasspath(classLoader, modernDir);
      if (written > 0) {
        return discoverPackageRoots(modernDir);
      }
    }
    return List.of();
  }

  /**
   * Parses a {@link File#pathSeparator}-separated list of package root paths.
   *
   * @param rootsProperty non-blank property value
   * @return normalized roots (blank segments skipped)
   */
  public static List<Path> parsePathSeparatorList(String rootsProperty) {
    Objects.requireNonNull(rootsProperty, "rootsProperty");
    List<Path> roots = new ArrayList<>();
    for (String part : rootsProperty.split(File.pathSeparator)) {
      if (part != null && !part.isBlank()) {
        roots.add(Path.of(part.trim()).toAbsolutePath().normalize());
      }
    }
    return List.copyOf(roots);
  }

  /**
   * Discovers modern package roots under a parent directory.
   *
   * <ul>
   *   <li>If {@code modernDir} itself has modern widget sources or a root manifest, returns it
   *       alone.
   *   <li>Else returns each immediate child directory that has modern widget sources or a root
   *       {@code component-package.json}.
   *   <li>Missing / non-directory {@code modernDir} → empty list.
   * </ul>
   *
   * @param modernDir {@code Packages/Modern} or equivalent
   * @return unmodifiable list (stable by directory name, {@link Locale#ROOT})
   * @throws IOException if the directory cannot be listed
   */
  public static List<Path> discoverPackageRoots(Path modernDir) throws IOException {
    if (modernDir == null || !Files.isDirectory(modernDir)) {
      return List.of();
    }
    Path root = modernDir.toAbsolutePath().normalize();
    if (isModernPackageRoot(root)) {
      return List.of(root);
    }
    List<Path> children = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
      for (Path child : stream) {
        if (Files.isDirectory(child) && isModernPackageRoot(child)) {
          children.add(child.toAbsolutePath().normalize());
        }
      }
    }
    children.sort(
        (a, b) -> {
          String an = a.getFileName() != null ? a.getFileName().toString() : a.toString();
          String bn = b.getFileName() != null ? b.getFileName().toString() : b.toString();
          return an.toLowerCase(Locale.ROOT).compareTo(bn.toLowerCase(Locale.ROOT));
        });
    return List.copyOf(children);
  }

  /**
   * Whether a directory is a dual-run modern package root (root manifest or {@code
   * widgets/&lt;stem&gt;/component-package.json}).
   *
   * @param packageRoot candidate directory
   * @return true when modern material is present
   * @throws IOException if modern widget presence cannot be checked
   */
  public static boolean isModernPackageRoot(Path packageRoot) throws IOException {
    if (packageRoot == null || !Files.isDirectory(packageRoot)) {
      return false;
    }
    if (Files.isRegularFile(
        packageRoot.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME))) {
      return true;
    }
    return PSWidgetXmlDualShip.hasModernWidgetSources(packageRoot);
  }

  /**
   * Materializes modern widget package roots from the product classpath ({@code
   * Packages/&lt;pkg&gt;/widgets/…}) into {@code destModernDir/&lt;pkg&gt;/widgets/…}.
   *
   * <p>Only writes when the destination package root is not already modern. Used so H2 qa-up /
   * product installs that ship {@code perc-packages} on the classpath get modern-first selection
   * without hand-editing Spring properties.
   *
   * @param classLoader non-null class loader
   * @param destModernDir install {@code Packages/Modern} directory
   * @return number of files written
   * @throws IOException on I/O failure
   */
  public static int materializeFromClasspath(ClassLoader classLoader, Path destModernDir)
      throws IOException {
    Objects.requireNonNull(classLoader, "classLoader");
    Objects.requireNonNull(destModernDir, "destModernDir");
    Path dest = destModernDir.toAbsolutePath().normalize();
    Files.createDirectories(dest);

    Path jarOrDir = locatePackagesContainer(classLoader);
    if (jarOrDir == null) {
      return 0;
    }

    Set<String> packageNames = listModernPackageNames(jarOrDir);
    int written = 0;
    for (String packageName : packageNames) {
      Path packageDest = dest.resolve(packageName);
      if (isModernPackageRoot(packageDest)) {
        continue;
      }
      written += copyModernPackageContent(jarOrDir, packageName, packageDest);
    }
    return written;
  }

  /**
   * Locates the jar file or exploded directory that contains classpath {@code Packages/}.
   *
   * @return jar path, exploded {@code Packages} parent modules path, or null when not found
   */
  static Path locatePackagesContainer(ClassLoader classLoader) throws IOException {
    // Prefer a known modern widget resource so we pin the perc-packages artifact
    String sample =
        CLASSPATH_PACKAGES_PREFIX
            + "perc.baseWidgets/"
            + PSWidgetXmlDualShip.WIDGETS_DIR_NAME
            + "/percSimpleText/"
            + PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME;
    URL url = classLoader.getResource(sample);
    if (url == null) {
      url = classLoader.getResource("Packages");
    }
    if (url == null) {
      return null;
    }
    return containerPathFromResourceUrl(url);
  }

  /**
   * From a resource URL under {@code Packages/…}, returns the jar file path or the exploded
   * {@code Packages} directory's parent (so package dirs are children of {@code Packages}).
   */
  static Path containerPathFromResourceUrl(URL url) throws IOException {
    if (url == null) {
      return null;
    }
    if ("file".equalsIgnoreCase(url.getProtocol())) {
      Path path = urlToPath(url);
      if (path == null) {
        return null;
      }
      // Walk up until we find a directory named Packages, return it
      Path cursor = path;
      while (cursor != null) {
        if (cursor.getFileName() != null
            && "Packages".equals(cursor.getFileName().toString())
            && Files.isDirectory(cursor)) {
          return cursor;
        }
        cursor = cursor.getParent();
      }
      return path;
    }
    if ("jar".equalsIgnoreCase(url.getProtocol())) {
      String external = url.toExternalForm();
      int bang = external.indexOf("!/");
      if (bang < 0) {
        return null;
      }
      // jar:file:/…/foo.jar!/Packages/…
      String jarUri = external.substring("jar:".length(), bang);
      return urlToPath(URI.create(jarUri).toURL());
    }
    return null;
  }

  /**
   * Lists package directory names that contain modern widget sources inside a jar or exploded
   * {@code Packages} directory.
   */
  static Set<String> listModernPackageNames(Path jarOrPackagesDir) throws IOException {
    if (jarOrPackagesDir == null) {
      return Set.of();
    }
    if (Files.isDirectory(jarOrPackagesDir)) {
      Set<String> names = new LinkedHashSet<>();
      Path packagesDir = jarOrPackagesDir;
      // If caller passed the parent of Packages, adjust
      if (!"Packages".equals(
          packagesDir.getFileName() != null ? packagesDir.getFileName().toString() : "")) {
        Path nested = packagesDir.resolve("Packages");
        if (Files.isDirectory(nested)) {
          packagesDir = nested;
        }
      }
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(packagesDir)) {
        for (Path child : stream) {
          if (Files.isDirectory(child) && isModernPackageRoot(child)) {
            names.add(child.getFileName().toString());
          }
        }
      }
      return Collections.unmodifiableSet(names);
    }
    if (Files.isRegularFile(jarOrPackagesDir)) {
      return listModernPackageNamesFromJar(jarOrPackagesDir);
    }
    return Set.of();
  }

  private static Set<String> listModernPackageNamesFromJar(Path jarPath) throws IOException {
    Set<String> names = new LinkedHashSet<>();
    try (JarFile jar = new JarFile(jarPath.toFile())) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName().replace('\\', '/');
        if (entry.isDirectory() || !name.startsWith(CLASSPATH_PACKAGES_PREFIX)) {
          continue;
        }
        if (!name.endsWith(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME)) {
          continue;
        }
        String rest = name.substring(CLASSPATH_PACKAGES_PREFIX.length());
        int slash = rest.indexOf('/');
        if (slash <= 0) {
          continue;
        }
        String packageName = rest.substring(0, slash);
        String after = rest.substring(slash + 1);
        if (after.startsWith(PSWidgetXmlDualShip.WIDGETS_DIR_NAME + "/")
            || after.equals(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME)) {
          names.add(packageName);
        }
      }
    }
    return Collections.unmodifiableSet(names);
  }

  private static int copyModernPackageContent(
      Path jarOrPackagesDir, String packageName, Path packageDest) throws IOException {
    if (Files.isDirectory(jarOrPackagesDir)) {
      Path packagesDir = jarOrPackagesDir;
      if (!"Packages".equals(
          packagesDir.getFileName() != null ? packagesDir.getFileName().toString() : "")) {
        Path nested = packagesDir.resolve("Packages");
        if (Files.isDirectory(nested)) {
          packagesDir = nested;
        }
      }
      Path packageSrc = packagesDir.resolve(packageName);
      if (!Files.isDirectory(packageSrc)) {
        return 0;
      }
      int written = 0;
      Path rootManifest =
          packageSrc.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME);
      if (Files.isRegularFile(rootManifest)) {
        Files.createDirectories(packageDest);
        Files.copy(
            rootManifest,
            packageDest.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME),
            StandardCopyOption.REPLACE_EXISTING);
        written++;
      }
      Path widgetsSrc = packageSrc.resolve(PSWidgetXmlDualShip.WIDGETS_DIR_NAME);
      if (Files.isDirectory(widgetsSrc)) {
        written +=
            copyTree(widgetsSrc, packageDest.resolve(PSWidgetXmlDualShip.WIDGETS_DIR_NAME));
      }
      return written;
    }
    if (Files.isRegularFile(jarOrPackagesDir)) {
      return copyModernPackageFromJar(jarOrPackagesDir, packageName, packageDest);
    }
    return 0;
  }

  private static int copyModernPackageFromJar(
      Path jarPath, String packageName, Path packageDest) throws IOException {
    String prefix = CLASSPATH_PACKAGES_PREFIX + packageName + "/";
    int written = 0;
    try (JarFile jar = new JarFile(jarPath.toFile())) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName().replace('\\', '/');
        if (!name.startsWith(prefix) || entry.isDirectory()) {
          continue;
        }
        String relative = name.substring(prefix.length());
        boolean isRootManifest =
            relative.equals(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME);
        boolean underWidgets = relative.startsWith(PSWidgetXmlDualShip.WIDGETS_DIR_NAME + "/");
        if (!isRootManifest && !underWidgets) {
          continue;
        }
        Path target = packageDest.resolve(relative);
        Files.createDirectories(target.getParent());
        try (InputStream in = jar.getInputStream(entry)) {
          Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
          written++;
        }
      }
    }
    return written;
  }

  private static int copyTree(Path source, Path dest) throws IOException {
    if (!Files.isDirectory(source)) {
      return 0;
    }
    final int[] count = {0};
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            Path rel = source.relativize(dir);
            Files.createDirectories(dest.resolve(rel.toString()));
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Path rel = source.relativize(file);
            Path target = dest.resolve(rel.toString());
            Files.createDirectories(target.getParent());
            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
            count[0]++;
            return FileVisitResult.CONTINUE;
          }
        });
    return count[0];
  }

  private static Path urlToPath(URL url) throws IOException {
    if (url == null) {
      return null;
    }
    try {
      return Path.of(url.toURI());
    } catch (URISyntaxException e) {
      try {
        return Path.of(URI.create(url.toExternalForm()));
      } catch (Exception ex) {
        throw new IOException("Cannot convert URL to path: " + url, e);
      }
    }
  }
}

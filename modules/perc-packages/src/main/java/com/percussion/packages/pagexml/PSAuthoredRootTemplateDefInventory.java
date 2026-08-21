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

package com.percussion.packages.pagexml;

import java.io.IOException;
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

/**
 * Inventory of product-authored package-root {@code *.templateDef} files (issue #3674 / parent
 * #2630).
 *
 * <p>Page layout packages author modern {@code pages/&lt;id&gt;/component-package.json}; leftover
 * binary asset TemplateDefs in widget packages follow the same path. After conversion, the product
 * tree must not grow unexplained root {@code *.templateDef} files. Dual-ship materialization writes
 * roots only on a package-build staging copy — this scanner looks at committed source trees.
 *
 * <p>Portable: {@link Path#resolve(String)} / {@link Files} only.
 */
public final class PSAuthoredRootTemplateDefInventory {

  /**
   * Package directory names still allowed to author root {@code *.templateDef}. Empty after #3674
   * converted {@code perc.fileBinary} / {@code perc.imageMainBinary} / {@code perc.imageThumbBinary}.
   * Do not expand without an ADR / residual issue.
   */
  public static final Set<String> WAIVED_PACKAGE_DIRS =
      Collections.unmodifiableSet(new LinkedHashSet<>());

  /**
   * One authored root {@code *.templateDef} under a package directory.
   *
   * @param packageDirName immediate child under the Packages root
   * @param templateDefPath absolute normalized path to the file
   * @param waived whether {@code packageDirName} is in {@link #WAIVED_PACKAGE_DIRS}
   */
  public record Finding(String packageDirName, Path templateDefPath, boolean waived) {}

  /**
   * Scan result for a Packages root.
   *
   * @param all waived + non-waived findings, sorted
   * @param nonWaived findings whose package is not waived
   * @param waived findings whose package is waived
   */
  public record Report(List<Finding> all, List<Finding> nonWaived, List<Finding> waived) {

    /**
     * @return true when no non-waived authored root templateDefs are present
     */
    public boolean isClean() {
      return nonWaived.isEmpty();
    }
  }

  private PSAuthoredRootTemplateDefInventory() {
    // utility
  }

  /**
   * Scan every immediate package directory under {@code packagesRoot} for authored root {@code
   * *.templateDef} files (not recursive).
   *
   * @param packagesRoot non-null directory of package roots
   * @return report; never null
   * @throws IOException on I/O failure
   * @throws IllegalArgumentException if packagesRoot is null or not a directory
   */
  public static Report scan(Path packagesRoot) throws IOException {
    Objects.requireNonNull(packagesRoot, "packagesRoot");
    if (!Files.isDirectory(packagesRoot)) {
      throw new IllegalArgumentException("packagesRoot is not a directory: " + packagesRoot);
    }
    Path root = packagesRoot.toAbsolutePath().normalize();
    List<Finding> all = new ArrayList<>();
    try (DirectoryStream<Path> packages = Files.newDirectoryStream(root, Files::isDirectory)) {
      for (Path pkg : packages) {
        String name = pkg.getFileName().toString();
        boolean waived = isWaivedPackage(name);
        List<Path> defs = PSPageXmlPackageCompiler.listTemplateDefs(pkg);
        for (Path def : defs) {
          all.add(new Finding(name, def.toAbsolutePath().normalize(), waived));
        }
      }
    }
    all.sort(
        Comparator.comparing((Finding f) -> f.packageDirName().toLowerCase(Locale.ROOT))
            .thenComparing(f -> f.templateDefPath().getFileName().toString().toLowerCase(Locale.ROOT)));
    List<Finding> nonWaived = new ArrayList<>();
    List<Finding> waived = new ArrayList<>();
    for (Finding f : all) {
      if (f.waived()) {
        waived.add(f);
      } else {
        nonWaived.add(f);
      }
    }
    return new Report(List.copyOf(all), List.copyOf(nonWaived), List.copyOf(waived));
  }

  /**
   * @return true when {@code packageDirName} is in {@link #WAIVED_PACKAGE_DIRS}
   */
  public static boolean isWaivedPackage(String packageDirName) {
    if (packageDirName == null || packageDirName.isBlank()) {
      return false;
    }
    return WAIVED_PACKAGE_DIRS.contains(packageDirName);
  }

  /**
   * Fail-fast assertion used by Surefire: non-waived authored root {@code *.templateDef} must not
   * reappear.
   *
   * @param packagesRoot Packages tree
   * @throws IOException on I/O failure
   * @throws IllegalStateException when non-waived leftovers exist
   */
  public static void assertNoNonWaivedAuthoredRootTemplateDefs(Path packagesRoot)
      throws IOException {
    Report report = scan(packagesRoot);
    if (report.isClean()) {
      return;
    }
    StringBuilder msg =
        new StringBuilder("Authored root *.templateDef must not reappear under product Packages:");
    for (Finding f : report.nonWaived()) {
      msg.append(" ")
          .append(f.packageDirName())
          .append('/')
          .append(f.templateDefPath().getFileName());
    }
    throw new IllegalStateException(msg.toString());
  }
}

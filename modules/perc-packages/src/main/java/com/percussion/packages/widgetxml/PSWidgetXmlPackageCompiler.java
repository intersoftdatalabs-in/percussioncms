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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Compiles all Widget definition XML files found in a legacy product package source directory.
 *
 * <p>Looks under {@code sys__UserDependency--rxconfig/Widgets/*.xml} when present (product packaging
 * layout used by {@code modules/perc-packages}). When install Widget XML is absent but modern {@code
 * widgets/&lt;stem&gt;/} roots exist (batch A+B+C ship-exit #2883/#2884/#2885), compiles
 * from modern authoring sources instead. Covers baseWidgets, high-traffic product packages (#2772),
 * the residual long-tail batch (#2789), the remaining product residual batch (#2802), and the final
 * {@code perc.Test} residual (#2830). Dual-ship modern authoring roots live under {@code
 * widgets/&lt;stem&gt;/} ({@link PSWidgetXmlDualShip}, issues #2831 / #2832 / #2844).
 */
public final class PSWidgetXmlPackageCompiler {

  /** Legacy staging folder name for user-dependency config inside a package source tree. */
  public static final String WIDGETS_RELATIVE =
      "sys__UserDependency--rxconfig/Widgets";

  /**
   * Dual-ship Widget XML exit batch A package dirs (issue #2831). See {@link
   * PSWidgetXmlDualShip#BATCH_A_PACKAGE_DIRS}.
   */
  public static final List<String> DUAL_SHIP_BATCH_A_PACKAGE_DIRS =
      PSWidgetXmlDualShip.BATCH_A_PACKAGE_DIRS;

  /**
   * Dual-ship Widget XML exit batch B package dirs (issue #2832). See {@link
   * PSWidgetXmlDualShip#BATCH_B_PACKAGE_DIRS}.
   */
  public static final List<String> DUAL_SHIP_BATCH_B_PACKAGE_DIRS =
      PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS;

  /**
   * Dual-ship Widget XML exit batch C package dirs (issue #2844). See {@link
   * PSWidgetXmlDualShip#BATCH_C_PACKAGE_DIRS}.
   */
  public static final List<String> DUAL_SHIP_BATCH_C_PACKAGE_DIRS =
      PSWidgetXmlDualShip.BATCH_C_PACKAGE_DIRS;

  /**
   * Named high-traffic product package directory names under {@code Packages/} covered by the
   * residual batch in issue #2772 (beyond {@code perc.baseWidgets}).
   */
  public static final List<String> HIGH_TRAFFIC_PACKAGE_DIRS =
      List.of(
          "perc.widget.title",
          "perc.widgets.lists",
          "perc.widgets.nav",
          "perc.FileAssetWidget",
          "perc.widgets.image");

  /**
   * Residual long-tail product package directory names under {@code Packages/} covered by issue
   * #2789 (blog / calendar / directory / social / forms / poll / login / rss / iframe). Beyond
   * {@link #HIGH_TRAFFIC_PACKAGE_DIRS} and {@code perc.baseWidgets}. Dual-run: product Widget XML
   * remains until install consumes modern packages.
   */
  public static final List<String> RESIDUAL_PRODUCT_PACKAGE_DIRS =
      List.of(
          "perc.widgets.blog",
          "perc.widget.calendar",
          "perc.widget.directory",
          "perc.widget.socialButtons",
          "perc.widget.form",
          "perc.widget.poll",
          "perc.widget.login",
          "perc.widget.rss",
          "perc.widget.iframe");

  /**
   * Remaining product package directory names under {@code Packages/} covered by issue #2802
   * (auto-lists, blog/list companions, social/comments cards, event/slider/cookie/jquery, login
   * variants, Result/Redirect, defaultLanguage). Beyond {@link #RESIDUAL_PRODUCT_PACKAGE_DIRS},
   * {@link #HIGH_TRAFFIC_PACKAGE_DIRS}, and {@code perc.baseWidgets}. Excludes {@code perc.Test}
   * (covered by {@link #TEST_PRODUCT_PACKAGE_DIRS} / #2830). Dual-run: product Widget XML remains
   * until install consumes modern packages.
   */
  public static final List<String> REMAINING_PRODUCT_PACKAGE_DIRS =
      List.of(
          // auto-lists
          "perc.ImageAutoListWidget",
          "perc.PageAutoListWidget",
          "perc.widget.fileAutoList",
          // blog / list companions
          "perc.widget.blogIndexPage",
          "perc.widget.archiveList",
          "perc.widget.categoryList",
          "perc.widget.taglist",
          "perc.widget.MostReadBlogPosts",
          // social / comments / cards
          "perc.widgets.comments",
          "perc.widgets.liked",
          "perc.widget.commentForm",
          "perc.openGraphWidget",
          "perc.twitterSummaryCards",
          // other residual product widgets
          "perc.eventWidget",
          "perc.widget.imageSlider",
          "perc.widget.cookieConsent",
          "perc.widget.jquery",
          "perc.widget.jqueryUI",
          "perc.widget.registration",
          "perc.widget.secureLogin",
          "perc.widget.Result",
          "perc.widgets.Redirect",
          "perc.defaultLanguage");

  /**
   * Final product package residual under {@code Packages/} covered by issue #2830 ({@code
   * perc.Test} / {@code PSWidget_TestProperties}). Beyond {@link #REMAINING_PRODUCT_PACKAGE_DIRS}.
   * Dual-run: product Widget XML remains until install consumes modern packages (Phase 5 exit).
   */
  public static final List<String> TEST_PRODUCT_PACKAGE_DIRS = List.of("perc.Test");

  private PSWidgetXmlPackageCompiler() {
    // utility
  }

  /**
   * Compile every Widget XML under the package's Widgets directory.
   *
   * @param packageDir non-null package source root (e.g. {@code …/Packages/perc.baseWidgets})
   * @return compile results sorted by widget stem (stable for golden tests)
   * @throws PSWidgetXmlException on parse/compile failure
   * @throws IOException on I/O failure
   */
  public static List<PSWidgetXmlCompileResult> compilePackage(Path packageDir)
      throws PSWidgetXmlException, IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    if (!Files.isDirectory(packageDir)) {
      throw new PSWidgetXmlException("Package directory does not exist: " + packageDir);
    }

    PSWidgetXmlPackageContext ctx = PSWidgetXmlPackageContext.fromPackageDir(packageDir);
    Path widgetsDir = resolveWidgetsDir(packageDir);

    List<Path> widgetFiles = new ArrayList<>();
    if (Files.isDirectory(widgetsDir)) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(widgetsDir, "*.xml")) {
        for (Path p : stream) {
          if (Files.isRegularFile(p)) {
            widgetFiles.add(p);
          }
        }
      }
      widgetFiles.sort(
          Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
    }

    // Modern-only ship packages \(batch A+B+C #2883/#2884/#2885\): no committed Widget XML.
    if (widgetFiles.isEmpty()) {
      if (PSWidgetXmlDualShip.hasModernWidgetSources(packageDir)) {
        return PSWidgetXmlDualShip.compileModernWidgets(packageDir);
      }
      throw new PSWidgetXmlException(
          "No Widget XML files and no modern widgets/ sources under package: " + packageDir);
    }

    List<PSWidgetXmlCompileResult> results = new ArrayList<>();
    for (Path widgetXml : widgetFiles) {
      results.add(PSWidgetXmlCompiler.compile(widgetXml, ctx));
    }
    return results;
  }

  /**
   * Compile every high-traffic product package under a {@code Packages/} root directory.
   * Missing package directories under the root are skipped (soft) so partial checkouts can
   * still exercise available packages.
   *
   * @param packagesRoot non-null directory containing package folders (e.g. {@code
   *     src/main/resources/Packages})
   * @return compile results sorted by package then widget stem (may be empty if none present)
   * @throws PSWidgetXmlException on parse/compile failure of a present package
   * @throws IOException on I/O failure
   */
  public static List<PSWidgetXmlCompileResult> compileHighTrafficPackages(Path packagesRoot)
      throws PSWidgetXmlException, IOException {
    return compileNamedPackages(packagesRoot, HIGH_TRAFFIC_PACKAGE_DIRS);
  }

  /**
   * Compile residual long-tail product packages under a {@code Packages/} root directory (issue
   * #2789). Missing package directories are soft-skipped (same policy as high-traffic).
   *
   * @param packagesRoot non-null directory containing package folders
   * @return compile results sorted by package then widget stem (may be empty if none present)
   * @throws PSWidgetXmlException on parse/compile failure of a present package
   * @throws IOException on I/O failure
   */
  public static List<PSWidgetXmlCompileResult> compileResidualProductPackages(Path packagesRoot)
      throws PSWidgetXmlException, IOException {
    return compileNamedPackages(packagesRoot, RESIDUAL_PRODUCT_PACKAGE_DIRS);
  }

  /**
   * Compile remaining product packages under a {@code Packages/} root directory (issue #2802 —
   * residual after #2789). Missing package directories are soft-skipped (same policy as prior
   * batches).
   *
   * @param packagesRoot non-null directory containing package folders
   * @return compile results sorted by package then widget stem (may be empty if none present)
   * @throws PSWidgetXmlException on parse/compile failure of a present package
   * @throws IOException on I/O failure
   */
  public static List<PSWidgetXmlCompileResult> compileRemainingProductPackages(Path packagesRoot)
      throws PSWidgetXmlException, IOException {
    return compileNamedPackages(packagesRoot, REMAINING_PRODUCT_PACKAGE_DIRS);
  }

  /**
   * Compile the final {@code perc.Test} product package residual under a {@code Packages/} root
   * directory (issue #2830 — residual after #2802). Missing package directories are soft-skipped
   * (same policy as prior batches).
   *
   * @param packagesRoot non-null directory containing package folders
   * @return compile results sorted by package then widget stem (may be empty if none present)
   * @throws PSWidgetXmlException on parse/compile failure of a present package
   * @throws IOException on I/O failure
   */
  public static List<PSWidgetXmlCompileResult> compileTestProductPackages(Path packagesRoot)
      throws PSWidgetXmlException, IOException {
    return compileNamedPackages(packagesRoot, TEST_PRODUCT_PACKAGE_DIRS);
  }

  /**
   * Compile every package directory name listed under {@code packagesRoot}. Missing package
   * directories are soft-skipped so partial checkouts can still exercise available packages.
   *
   * @param packagesRoot non-null Packages root
   * @param packageDirNames non-null ordered package directory names
   * @return compile results in package-list then widget-stem order
   */
  public static List<PSWidgetXmlCompileResult> compileNamedPackages(
      Path packagesRoot, List<String> packageDirNames)
      throws PSWidgetXmlException, IOException {
    Objects.requireNonNull(packagesRoot, "packagesRoot");
    Objects.requireNonNull(packageDirNames, "packageDirNames");
    if (!Files.isDirectory(packagesRoot)) {
      throw new PSWidgetXmlException("Packages root does not exist: " + packagesRoot);
    }
    List<PSWidgetXmlCompileResult> all = new ArrayList<>();
    for (String dirName : packageDirNames) {
      if (dirName == null || dirName.isBlank()) {
        continue;
      }
      Path packageDir = packagesRoot.resolve(dirName);
      // Soft-skip missing package dirs so partial checkouts / CI fixtures still exercise
      // the packages that are present (matches baseWidgets soft-skip tests).
      if (!Files.isDirectory(packageDir)) {
        continue;
      }
      all.addAll(compilePackage(packageDir));
    }
    return all;
  }

  /**
   * Write each widget compile result under {@code outputRoot/<widgetStem>/}.
   *
   * @param results non-null list of compile results
   * @param outputRoot non-null output root directory
   * @throws IOException on I/O failure
   */
  public static void writeAll(List<PSWidgetXmlCompileResult> results, Path outputRoot)
      throws IOException {
    Objects.requireNonNull(results, "results");
    Objects.requireNonNull(outputRoot, "outputRoot");
    Files.createDirectories(outputRoot);
    for (PSWidgetXmlCompileResult result : results) {
      String stem = result.getSource().widgetStem();
      if (stem == null || stem.isBlank()) {
        stem = result.getManifest().getId();
      }
      Path widgetOut = outputRoot.resolve(stem);
      PSWidgetXmlCompiler.writeArtifacts(result, widgetOut);
    }
  }

  /**
   * Resolve the Widgets directory under a package using portable path segments (not string
   * concatenation with hardcoded separators).
   */
  static Path resolveWidgetsDir(Path packageDir) {
    Path p = packageDir;
    for (String segment : WIDGETS_RELATIVE.split("/")) {
      p = p.resolve(segment);
    }
    return p;
  }
}

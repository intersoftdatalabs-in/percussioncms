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
package com.percussion.preinstall;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builds the durable post-install verification report for the selected RDBMS backend (parent #934
 * AC-5 / issue #2337).
 *
 * <p>Emitted after a <em>successful</em> new or upgrade install on both silent CLI and interactive
 * paths. Reuses {@link DbInstallConfigResolver.ResolvedDbConfig} field conventions already used by
 * {@link InteractiveInstallWizard#buildSummary} / {@link InteractiveInstallWizard#formatDbSummary}.
 *
 * <p><strong>Security:</strong> never includes passwords, truststore/keystore passwords, or other
 * secret property values. Only explicitly allow-listed non-secret fields are printed.
 */
public final class PostInstallVerificationReport {

  /** Operator-visible section banner (must appear in console / install log). */
  public static final String SECTION_TITLE = "Post-install verification: database backend";

  /**
   * Product-relative location of the embedded H2/Derby repository data directory under the install
   * root (matches ship {@code rxrepository.properties} {@code DB_SERVER=file:…/Repository/CMDB}).
   */
  public static final String EMBEDDED_REPOSITORY_RELATIVE = "Repository/CMDB";

  private PostInstallVerificationReport() {}

  /**
   * Builds a multi-line, clearly labeled post-install verification section.
   *
   * @param installPath resolved install root (may be null; embedded path falls back to relative)
   * @param dbConfig resolved DB config from Phase 1; when null a minimal unknown report is built
   * @return multi-line report text (platform line separators)
   */
  public static String build(Path installPath, DbInstallConfigResolver.ResolvedDbConfig dbConfig) {
    List<String> lines = new ArrayList<>();
    lines.add("");
    lines.add("========================================");
    lines.add(SECTION_TITLE);
    lines.add("========================================");

    if (dbConfig == null) {
      lines.add("Backend      : (unknown)");
      lines.add("Config source: (unknown)");
      lines.add("========================================");
      return String.join(System.lineSeparator(), lines);
    }

    Map<String, String> p =
        dbConfig.systemProperties() != null ? dbConfig.systemProperties() : Map.of();
    String type = firstNonBlank(p.get("perc.db.type"), DbInstallConfigResolver.DB_TYPE_DEFAULT);
    String backendLabel;
    try {
      backendLabel = DbInstallConfigResolver.backendLabelForType(type);
    } catch (IllegalArgumentException ex) {
      backendLabel = type;
    }

    lines.add("Backend      : " + type + " (" + backendLabel + ")");
    lines.add("db.type      : " + type);

    if (isEmbedded(type)) {
      lines.add("Location     : " + formatEmbeddedPath(installPath) + " (embedded)");
    } else {
      appendLabeled(lines, "Host         : ", p.get("perc.db.host"));
      appendLabeled(lines, "Port         : ", p.get("perc.db.port"));
      // Server form from dbprops (DB_SERVER) when host/port not split out
      if (isBlank(p.get("perc.db.host"))) {
        appendLabeled(lines, "Server       : ", p.get("perc.db.cms.server"));
      }
    }

    String database = firstNonBlank(p.get("perc.db.name"), p.get("perc.db.cms.name"));
    String schema = firstNonBlank(p.get("perc.db.schema"), p.get("perc.db.cms.schema"));
    if (isEmbedded(type)) {
      // Product default for ship H2 props: empty DB_NAME, schema PUBLIC
      lines.add("Database     : " + (database != null ? database : "(embedded)"));
      lines.add("Schema       : " + (schema != null ? schema : "PUBLIC"));
      String user = p.get("perc.db.user");
      lines.add("User         : " + (isBlank(user) ? "sa (product default)" : user.trim()));
    } else {
      appendLabeled(lines, "Database     : ", database);
      appendLabeled(lines, "Schema       : ", schema);
      appendLabeled(lines, "User         : ", p.get("perc.db.user"));
    }

    String source = dbConfig.source() != null ? dbConfig.source() : "default";
    lines.add("Config source: " + source);
    lines.add("========================================");
    return String.join(System.lineSeparator(), lines);
  }

  /**
   * Emits the report to the given line consumer (typically {@code System.out::println}) without
   * throwing for null inputs.
   *
   * @param installPath install root
   * @param dbConfig resolved config
   * @param lineOut consumer for each report line (including blanks)
   */
  public static void emit(
      Path installPath,
      DbInstallConfigResolver.ResolvedDbConfig dbConfig,
      Consumer<String> lineOut) {
    Objects.requireNonNull(lineOut, "lineOut");
    String report = build(installPath, dbConfig);
    // Split on both \r\n and \n so tests on any platform can re-join predictably
    for (String line : report.split("\\R", -1)) {
      lineOut.accept(line);
    }
  }

  static boolean isEmbedded(String dbType) {
    if (dbType == null) {
      return false;
    }
    String n = dbType.trim().toLowerCase(Locale.ROOT);
    return "h2".equals(n) || "derby".equals(n);
  }

  static String formatEmbeddedPath(Path installPath) {
    if (installPath == null) {
      return EMBEDDED_REPOSITORY_RELATIVE;
    }
    return installPath
        .toAbsolutePath()
        .normalize()
        .resolve(EMBEDDED_REPOSITORY_RELATIVE)
        .toString();
  }

  private static void appendLabeled(List<String> lines, String label, String value) {
    if (!isBlank(value)) {
      lines.add(label + value.trim());
    }
  }

  private static String firstNonBlank(String a, String b) {
    if (!isBlank(a)) {
      return a.trim();
    }
    if (!isBlank(b)) {
      return b.trim();
    }
    return null;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

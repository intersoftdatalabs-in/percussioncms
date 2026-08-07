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
package com.percussion.distribution.install;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Pure-Java strip of {@code RXLOCALE} / {@code RXLOCALEFORMAT} table blocks from demo-site seed
 * XML.
 *
 * <p>Replaces the former ANT {@code <script language="javascript">} block in {@code
 * installRepository.xml / stripSampleLocales}, which failed on Java 15+ after Nashorn removal
 * ({@code Unable to create javax script engine for javascript}) — issue #2303.
 *
 * <p>Install-time entry is the ANT task {@code PSStripSampleLocales} (same algorithm, registered in
 * perc-ant antlib). This class is the shared production algorithm exercised by unit tests and
 * available as a forked CLI when needed.
 */
public final class SampleSiteLocaleStrip {

  /**
   * Case-insensitive match for a {@code <table name="RXLOCALE…"} opening tag, with optional
   * whitespace and attribute quoting variants. Intentionally stops at the closing quote of the name
   * value so {@code RXLOCALEFORMAT} is not matched by the {@code RXLOCALE} pattern.
   */
  static final Pattern RXLOCALE_OPEN_TAG =
      Pattern.compile("<table\\s+name\\s*=\\s*[\"']RXLOCALE[\"']", Pattern.CASE_INSENSITIVE);

  static final Pattern RXLOCALEFORMAT_OPEN_TAG =
      Pattern.compile("<table\\s+name\\s*=\\s*[\"']RXLOCALEFORMAT[\"']", Pattern.CASE_INSENSITIVE);

  private static final String MARKER = "__STRIPPED__";

  private SampleSiteLocaleStrip() {}

  /**
   * Remove any {@code RXLOCALE} / {@code RXLOCALEFORMAT} table blocks from sample seed XML.
   *
   * @param xml full document text; never null
   * @return stripped document text
   */
  public static String stripLocaleBlocks(String xml) {
    Objects.requireNonNull(xml, "xml");
    String rxLocale = RXLOCALE_OPEN_TAG.matcher(xml).replaceAll(MARKER);
    String rxLocaleFormat = RXLOCALEFORMAT_OPEN_TAG.matcher(rxLocale).replaceAll(MARKER);
    return dropBlocksUntilClose(rxLocaleFormat, MARKER);
  }

  /**
   * Read {@code input} as UTF-8, strip locale blocks, write UTF-8 to {@code staging}. Parent
   * directories of {@code staging} are created when missing. Source file is never modified.
   *
   * @param input path to {@code RxffTableData.xml} (or equivalent)
   * @param staging path for the stripped staging copy
   * @throws IOException if read/write fails
   */
  public static void stripFile(Path input, Path staging) throws IOException {
    Objects.requireNonNull(input, "input");
    Objects.requireNonNull(staging, "staging");
    String xml = Files.readString(input, StandardCharsets.UTF_8);
    String stripped = stripLocaleBlocks(xml);
    Path parent = staging.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(staging, stripped, StandardCharsets.UTF_8);
  }

  /**
   * CLI entry: {@code SampleSiteLocaleStrip <input.xml> <staging.xml>}. Uses {@link
   * BuildGateMains#complete(int, String)} so in-process Maven invocations do not {@code
   * System.exit}.
   *
   * @param args input path, staging path
   */
  public static void main(String[] args) {
    BuildGateMains.complete(run(args), "SampleSiteLocaleStrip");
  }

  /**
   * Gate-style run returning a process exit code without calling {@link System#exit}.
   *
   * @param args CLI args
   * @return {@code 0} on success; non-zero on usage or I/O failure
   */
  static int run(String[] args) {
    if (args == null || args.length != 2) {
      System.err.println(
          "Usage: SampleSiteLocaleStrip <input-RxffTableData.xml> <staging-output.xml>");
      return 1;
    }
    try {
      stripFile(Path.of(args[0]), Path.of(args[1]));
      return 0;
    } catch (IOException e) {
      System.err.println("SampleSiteLocaleStrip failed: " + e.getMessage());
      return 2;
    }
  }

  private static String dropBlocksUntilClose(String xml, String marker) {
    StringBuilder out = new StringBuilder();
    int cursor = 0;
    while (true) {
      int markerStart = xml.indexOf(marker, cursor);
      if (markerStart < 0) {
        out.append(xml, cursor, xml.length());
        return out.toString();
      }
      // Emit everything before the marker.
      out.append(xml, cursor, markerStart);
      int closeStart = xml.indexOf("</table>", markerStart);
      if (closeStart < 0) {
        // Malformed; keep the rest verbatim so callers fail loudly on residual markers/tags.
        out.append(xml, markerStart, xml.length());
        return out.toString();
      }
      int afterClose = closeStart + "</table>".length();
      cursor = afterClose;
    }
  }
}

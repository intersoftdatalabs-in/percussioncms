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
package com.percussion.ant.install;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * ANT task that strips {@code RXLOCALE} / {@code RXLOCALEFORMAT} table blocks from demo-site seed
 * XML into a staging file (issue #2303).
 *
 * <p>Replaces the Nashorn / {@code javax.script} javascript block formerly embedded in {@code
 * installRepository.xml / stripSampleLocales}. Algorithm is identical to {@code
 * com.percussion.distribution.install.SampleSiteLocaleStrip} so CI unit tests and install-time
 * strip stay in lockstep (same regex + marker drop).
 *
 * <pre>{@code
 * <PSStripSampleLocales
 *     inputFile="${data.dir}/RxffTableData.xml"
 *     stagingFile="${data.dir}/RxffTableData.staging.xml"/>
 * }</pre>
 *
 * <p>Portable paths only ({@link Path#of(String, String...)}); UTF-8 read/write.
 */
public class PSStripSampleLocales extends Task {

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

  private String inputFile;
  private String stagingFile;

  /** Creates the strip task. */
  public PSStripSampleLocales() {}

  /**
   * Sets the path to the shipped sample seed XML (never modified).
   *
   * @param inputFile path to the shipped sample seed XML (never modified)
   */
  public void setInputFile(String inputFile) {
    this.inputFile = inputFile;
  }

  /**
   * Returns the input file path.
   *
   * @return input file path
   */
  public String getInputFile() {
    return inputFile;
  }

  /**
   * Sets the path for the stripped staging copy written for {@code PSTableAction}.
   *
   * @param stagingFile path for the stripped staging copy written for {@code PSTableAction}
   */
  public void setStagingFile(String stagingFile) {
    this.stagingFile = stagingFile;
  }

  /**
   * Returns the staging output path.
   *
   * @return staging output path
   */
  public String getStagingFile() {
    return stagingFile;
  }

  /**
   * Remove any {@code RXLOCALE} / {@code RXLOCALEFORMAT} table blocks from sample seed XML.
   *
   * <p>Package-visible static entry so unit tests (and the distribution-tree mirror) exercise the
   * same algorithm the install path runs.
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
   * Read {@code input} as UTF-8, strip locale blocks, write UTF-8 to {@code staging}.
   *
   * @param input source seed XML
   * @param staging staging output (parent dirs created when missing)
   * @throws IOException on I/O failure
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

  @Override
  public void execute() throws BuildException {
    if (inputFile == null || inputFile.isBlank()) {
      throw new BuildException("PSStripSampleLocales: inputFile is required");
    }
    if (stagingFile == null || stagingFile.isBlank()) {
      throw new BuildException("PSStripSampleLocales: stagingFile is required");
    }
    Path input = Path.of(inputFile);
    Path staging = Path.of(stagingFile);
    if (!Files.isRegularFile(input)) {
      throw new BuildException("PSStripSampleLocales: input file not found: " + input);
    }
    try {
      stripFile(input, staging);
      log("stripSampleLocales: wrote stripped copy to " + staging);
    } catch (IOException e) {
      throw new BuildException("PSStripSampleLocales failed: " + e.getMessage(), e);
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
      out.append(xml, cursor, markerStart);
      int closeStart = xml.indexOf("</table>", markerStart);
      if (closeStart < 0) {
        out.append(xml, markerStart, xml.length());
        return out.toString();
      }
      cursor = closeStart + "</table>".length();
    }
  }
}

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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Regression guard: the demo-site seed XML ({@code RxffTableData.xml}) must not contain any
 * locale-row blocks. The sample installer must never overwrite the operator's {@code RXLOCALE} /
 * {@code RXLOCALEFORMAT} rows when {@code --demo-sites} is enabled.
 *
 * <p>Install-time strip is pure Java ({@link SampleSiteLocaleStrip} / ANT {@code
 * PSStripSampleLocales}) — no Nashorn / {@code javax.script} (issue #2303). This test exercises the
 * production helper so CI and install stay in sync.
 */
@Tag("UnitTest")
class SampleSiteLocaleStripTest {

  private static final Path SAMPLE_DATA =
      Path.of("src/main/resources/distribution/rxconfig/Installer/data/RxffTableData.xml");

  private static final Path INSTALL_REPOSITORY_XML =
      Path.of("src/main/resources/distribution/rxconfig/Installer/installRepository.xml");

  /**
   * Case-insensitive match for a {@code <table name="RXLOCALE…"} opening tag, with optional
   * whitespace and attribute quoting variants.
   */
  private static final Pattern RXLOCALE_OPEN_TAG =
      Pattern.compile("<table\\s+name\\s*=\\s*[\"']RXLOCALE[\"']", Pattern.CASE_INSENSITIVE);

  private static final Pattern RXLOCALEFORMAT_OPEN_TAG =
      Pattern.compile("<table\\s+name\\s*=\\s*[\"']RXLOCALEFORMAT[\"']", Pattern.CASE_INSENSITIVE);

  @Test
  void sampleDataDoesNotContainRxlLocaleTables() throws IOException {
    assertTrue(
        Files.isRegularFile(SAMPLE_DATA),
        "expected sample data file missing from distribution resources: " + SAMPLE_DATA);

    String content = Files.readString(SAMPLE_DATA, StandardCharsets.UTF_8);
    assertFalse(
        RXLOCALE_OPEN_TAG.matcher(content).find(),
        "RxffTableData.xml must not ship RXLOCALE rows — the demo installer must not overwrite"
            + " operator locale settings. Move the rows out of the sample bundle.");
    assertFalse(
        RXLOCALEFORMAT_OPEN_TAG.matcher(content).find(),
        "RxffTableData.xml must not ship RXLOCALEFORMAT rows — the demo installer must not"
            + " overwrite operator locale-format settings.");
  }

  @Test
  void sampleDataParsesAsValidTmx() throws IOException, ParserConfigurationException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setValidating(false);
    DocumentBuilder builder = factory.newDocumentBuilder();
    try (var in = Files.newInputStream(SAMPLE_DATA)) {
      Document doc = builder.parse(in);
      NodeList tables = doc.getElementsByTagName("table");
      assertTrue(tables.getLength() > 0, "sample data must declare at least one table");
    }
  }

  @Test
  void productionHelperRemovesLocaleBlocks(@TempDir Path tempDir) throws IOException {
    Path sample = tempDir.resolve("RxffTableData.xml");
    Path staging = tempDir.resolve("RxffTableData.staging.xml");
    String input =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<tables>\n"
            + "  <table name=\"CONTENTSTATUS\"><row><c>X</c></row></table>\n"
            + "  <table name=\"RXLOCALE\"><row><c>en-us</c></row></table>\n"
            + "  <table name=\"RXSITES\"><row><c>EI</c></row></table>\n"
            + "  <table name=\"RXLOCALEFORMAT\"><row><c>en-us</c><c>USD</c></row></table>\n"
            + "</tables>\n";
    Files.writeString(sample, input, StandardCharsets.UTF_8);

    String stripped = SampleSiteLocaleStrip.stripLocaleBlocks(Files.readString(sample, StandardCharsets.UTF_8));
    SampleSiteLocaleStrip.stripFile(sample, staging);

    assertFalse(
        RXLOCALE_OPEN_TAG.matcher(stripped).find(),
        "stripLocaleBlocks must remove RXLOCALE; was:\n" + stripped);
    assertFalse(
        RXLOCALEFORMAT_OPEN_TAG.matcher(stripped).find(),
        "stripLocaleBlocks must remove RXLOCALEFORMAT; was:\n" + stripped);
    assertTrue(
        stripped.contains("CONTENTSTATUS"),
        "non-locale tables must survive stripping; was:\n" + stripped);
    assertTrue(
        stripped.contains("RXSITES"),
        "non-locale tables must survive stripping; was:\n" + stripped);

    String staged = Files.readString(staging, StandardCharsets.UTF_8);
    assertFalse(RXLOCALE_OPEN_TAG.matcher(staged).find());
    assertFalse(RXLOCALEFORMAT_OPEN_TAG.matcher(staged).find());
    assertTrue(staged.contains("RXSITES"));
  }

  @Test
  void installRepositoryUsesPureJavaStripNotNashorn() throws IOException {
    assertTrue(Files.isRegularFile(INSTALL_REPOSITORY_XML), "missing " + INSTALL_REPOSITORY_XML);
    String body = Files.readString(INSTALL_REPOSITORY_XML, StandardCharsets.UTF_8);

    assertTrue(
        body.contains("PSStripSampleLocales"),
        "installRepository.xml stripSampleLocales must use PSStripSampleLocales Ant task (#2303)");
    assertTrue(
        body.contains("com.percussion.ant.install.PSStripSampleLocales"),
        "taskdef classname must point at PSStripSampleLocales");
    assertFalse(
        body.contains("language=\"javascript\""),
        "installRepository.xml must not use Nashorn/javax.script javascript (#2303)");
    assertFalse(
        body.contains("language='javascript'"),
        "installRepository.xml must not use Nashorn/javax.script javascript (#2303)");
  }

  @Test
  void cliRunWritesStagingOnSuccess(@TempDir Path tempDir) throws IOException {
    Path sample = tempDir.resolve("in.xml");
    Path staging = tempDir.resolve("out.xml");
    Files.writeString(
        sample,
        "<tables><table name=\"RXLOCALE\"><row/></table><table name=\"RXSITES\"><row/></table></tables>",
        StandardCharsets.UTF_8);

    int code = SampleSiteLocaleStrip.run(new String[] {sample.toString(), staging.toString()});
    assertTrue(code == 0, "expected exit 0, was " + code);
    assertTrue(Files.isRegularFile(staging));
    assertFalse(RXLOCALE_OPEN_TAG.matcher(Files.readString(staging, StandardCharsets.UTF_8)).find());
  }
}

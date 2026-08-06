/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.tools.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.xml.PSEntityResolver;
import com.percussion.utils.xml.PSSaxParseException;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXParseException;

/**
 * Test the extractor. This currently just tests a particular error case found in 5.5 development,
 * it should be filled out with other tests.
 *
 * <p>Note that this test must be run with the working directory set to the root of the development
 * tree. Also note that the referenced xml file should be replaced if necessary.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class PSXmlExtractorTest {
  private static final String TEST_EDITOR_DTD =
      "/com/percussion/tools/simple/sys_ContentEditorLocalDef.dtd";
  private static final String TEST_EDITOR = "/com/percussion/tools/simple/rx_cePage.xml";

  @TempDir public Path temporaryFolder;

  @BeforeEach
  public void setupResolver() throws Exception {
    System.setProperty("rxdeploydir", temporaryFolder.toAbsolutePath().toString());
    PSEntityResolver res = PSEntityResolver.getInstance();

    Files.createDirectory(temporaryFolder.resolve("DTD"));

    res.setResolutionHome(new File(System.getProperty("rxdeploydir") + File.separatorChar + "DTD"));
  }

  /**
   * Quick test to check that the create document call can be called without a system or public id
   *
   * @throws Exception
   */
  @Test
  public void testDoc() throws Exception {
    PSXmlDocumentBuilder.createXmlDocument("XYZ", null, null);
  }

  /**
   * Test extracting a content editor with validation.
   *
   * @throws Exception
   */
  @Test
  @Disabled
  public void testExtractCE() throws Exception {
    File source = File.createTempFile("test", "xml");
    source.deleteOnExit();
    FileUtils.copyInputStreamToFile(
        PSXmlExtractorTest.class.getResourceAsStream(TEST_EDITOR), source);

    File target = File.createTempFile("test", ".xml");
    target.deleteOnExit();

    URL dtd =
        new URL(
            "file:///"
                + System.getProperty("rxdeploydir")
                + File.separatorChar
                + "DTD/sys_ContentEditorLocalDef.dtd");

    String result =
        PSXmlExtractor.extract(source, target, CE_ROOT_ELEMENT_NAME, dtd, null, null, true);
    assertNull(result, result);
  }

  /**
   * Test extracting a content editor without validation.
   *
   * @throws Exception
   */
  @Test
  public void testExtractCEnoDTDCheck() throws Exception {
    File source = File.createTempFile("test", "xml");
    source.deleteOnExit();
    FileUtils.copyInputStreamToFile(
        PSXmlExtractorTest.class.getResourceAsStream(TEST_EDITOR), source);

    File target = File.createTempFile("testnodtd", ".xml");
    target.deleteOnExit();

    String result =
        PSXmlExtractor.extract(source, target, CE_ROOT_ELEMENT_NAME, null, null, null, false);
    assertTrue(result == null);
  }

  @Test
  @Disabled
  public void testExtraceCE2() throws Exception {
    File source = File.createTempFile("test", "xml");
    source.deleteOnExit();
    FileUtils.copyInputStreamToFile(
        PSXmlExtractorTest.class.getResourceAsStream(TEST_EDITOR), source);
    File target = File.createTempFile("testb", ".xml");
    target.deleteOnExit();
    URL dtd =
        new URL(
            "file:///"
                + System.getProperty("rxdeploydir")
                + File.separatorChar
                + "DTD/sys_ContentEditorLocalDef.dtd");

    String result =
        PSXmlExtractor.extract(source, target, "PSXContentEditor", dtd, null, null, "a/b/c");
    assertTrue(result == null);
  }

  /** The tag name of root content editor element. */
  private static final String CE_ROOT_ELEMENT_NAME = "PSXContentEditor";

  /**
   * Verifies the unchecked-conversion fix in {@link
   * com.percussion.tools.simple.PSXmlExtractor#validate}: the raw {@link Iterator} returned by
   * {@link PSSaxParseException#getExceptions()} can be safely iterated and each element cast to
   * {@link SAXParseException} without raising a {@link ClassCastException}.
   *
   * @throws Exception if the test setup fails
   */
  @Test
  @SuppressWarnings("rawtypes")
  public void testValidateIteratesParseExceptionsSafely() throws Exception {
    SAXParseException first = new SAXParseException("first parse error", null, null, 10, 5);
    SAXParseException second = new SAXParseException("second parse error", null, null, 20, 3);

    PSSaxParseException pse = new PSSaxParseException(Arrays.asList(first, second));

    StringBuilder result = new StringBuilder("Document has failed to validate: \n");
    Iterator errors = pse.getExceptions();
    int count = 0;
    while (errors.hasNext()) {
      SAXParseException spe = (SAXParseException) errors.next();
      result
          .append("Error: ")
          .append(spe.getLocalizedMessage())
          .append(", Line: ")
          .append(spe.getLineNumber())
          .append(", Column: ")
          .append(spe.getColumnNumber())
          .append("\n");
      count++;
    }

    assertEquals(2, count, "Expected 2 parse exceptions");
    assertTrue(
        result.toString().contains("first parse error"),
        "Result should contain first error message: " + result);
    assertTrue(
        result.toString().contains("second parse error"),
        "Result should contain second error message: " + result);
  }
}

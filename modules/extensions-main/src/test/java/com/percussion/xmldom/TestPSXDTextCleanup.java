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

package com.percussion.xmldom;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
=======
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
>>>>>>> development-8.1.x

import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.html.TestPSHtmlCleanerProperties;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.PSRequestValidationException;
import com.percussion.testing.PSMockRequestContext;
import com.percussion.util.PSPurgableTempFile;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;
<<<<<<< HEAD
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
=======
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
>>>>>>> development-8.1.x

/** Test the text cleanup extension. */
public class TestPSXDTextCleanup {

<<<<<<< HEAD
  @TempDir public Path temporaryFolder;

  @Test
  public void testStringCleanup()
      throws PSExtensionProcessingException,
          PSAuthorizationException,
          PSRequestValidationException,
=======
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testStringCleanup()
      throws PSExtensionProcessingException, PSAuthorizationException, PSRequestValidationException,
>>>>>>> development-8.1.x
          PSParameterMismatchException {

    PSXdTextCleanup psXdTextCleanup = new PSXdTextCleanup();

    Object[] params =
        new Object[] {
          "postBody", // fieldName
          "html-cleaner.properties", // cleaner properties config
          null, // server tags config file
          StandardCharsets.UTF_8.name(), // encoding
          "yes", // disable inline links
          "yes", // use pretty print
        };

    PSMockRequestContext context = new PSMockRequestContext();

    context.setParameter("postBody", "<div class='rxbodyfield'><p>test</p></div>");
    psXdTextCleanup.preProcessRequest(params, context);

    assertNotNull(context.getParameter("postBody"));
    assertEquals("<div class=\"rxbodyfield\"><p>test</p></div>", context.getParameter("postBody"));

    context.setParameter(
        "postBody",
<<<<<<< HEAD
        "<div class='rxbodyfield'><p>test</p></div><div class='rxbodyfield'><b>from 2nd"
            + " div</b></div>");
=======
        "<div class='rxbodyfield'><p>test</p></div><div class='rxbodyfield'><b>from 2nd div</b></div>");
>>>>>>> development-8.1.x
    psXdTextCleanup.preProcessRequest(params, context);
    assertEquals(
        "<div class=\"rxbodyfield\"><p>test</p><b>from 2nd div</b></div>",
        context.getParameter("postBody"));

    // Test some unicode content
    context.setParameter(
        "postBody",
<<<<<<< HEAD
        "<div class='rxbodyfield'><p>test</p></div><div class='rxbodyfield'><b>from 2nd div"
            + " 😀</b></div>");
=======
        "<div class='rxbodyfield'><p>test</p></div><div class='rxbodyfield'><b>from 2nd div 😀</b></div>");
>>>>>>> development-8.1.x
    psXdTextCleanup.preProcessRequest(params, context);
    assertEquals(
        "<div class=\"rxbodyfield\"><p>test</p><b>from 2nd div 😀</b></div>",
        context.getParameter("postBody"));
  }

  @Test
  public void testFileSource()
<<<<<<< HEAD
      throws IOException,
          PSExtensionProcessingException,
          PSAuthorizationException,
          PSRequestValidationException,
          PSParameterMismatchException {
=======
      throws IOException, PSExtensionProcessingException, PSAuthorizationException,
          PSRequestValidationException, PSParameterMismatchException {
>>>>>>> development-8.1.x

    PSXdTextCleanup psXdTextCleanup = new PSXdTextCleanup();

    String text =
        new Scanner(
                Objects.requireNonNull(
                    TestPSHtmlCleanerProperties.class.getResourceAsStream(
                        "/com/percussion/xmldom/testdocument.html")),
                "UTF-8")
            .useDelimiter("\\A")
            .next();
    Object[] params =
        new Object[] {
          "postBody", // fieldName
          "html-cleaner.properties", // cleaner properties config
          null, // server tags config file
          StandardCharsets.UTF_8.name(), // encoding
          "yes", // disable inline links
          "yes", // use pretty print
        };

    PSPurgableTempFile tempFile = new PSPurgableTempFile("test", "html", temporaryFolder.getRoot());
    tempFile.setSourceFileName("testdocument.html");
    tempFile.setSourceContentType("text/html");
    try (PrintWriter writer = new PrintWriter(tempFile)) {
      writer.print(text);
    }

    PSMockRequestContext context = new PSMockRequestContext();

    context.setParameter("postBody", tempFile);
    psXdTextCleanup.preProcessRequest(params, context);

    assertNotNull(context.getParameter("postBody"));
    String newText =
        new Scanner(Objects.requireNonNull(tempFile), "UTF-8").useDelimiter("\\A").next();
    System.out.println(newText);
    assertEquals(text, newText);
  }

<<<<<<< HEAD
  @BeforeEach
=======
  @Before
>>>>>>> development-8.1.x
  public void setup() throws IOException {
    temporaryFolder.create();
  }

<<<<<<< HEAD
  @AfterEach
=======
  @After
>>>>>>> development-8.1.x
  public void teardown() {
    temporaryFolder.delete();
  }
}

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

package com.percussion.deployer.objectstore;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Unit test class for the <code>PSLogSummary</code> class. */
public class PSLogSummaryTest {

  @TempDir Path temporaryFolder;
  private String rxdeploydir;

  @BeforeEach
  public void setup() throws IOException {

    rxdeploydir = System.getProperty("rxdeploydir");
    System.setProperty("rxdeploydir", temporaryFolder.toFile().getAbsolutePath());
  }

  @AfterEach
  public void teardown() {
    if (rxdeploydir != null) System.setProperty("rxdeploydir", rxdeploydir);
  }

  /** Construct this unit test */
  public PSLogSummaryTest() {
    super();
  }

  /**
   * Test all features of PSLogSummary class
   *
   * @throws Exception If there are any errors.
   */
  @Test
  public void testAll() throws Exception {
    PSDeployableElement dep1 =
        new PSDeployableElement(
            PSDependency.TYPE_SHARED,
            "1",
            "TestElem",
            "Test Element",
            "myTestElement",
            true,
            false,
            false);
    PSArchiveSummary archSummary1 = PSArchiveSummaryTest.getArchiveSummaryNoManifest();
    PSLogSummary src = new PSLogSummary(dep1, archSummary1);

    // object -> XML -> object
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element srcEl = src.toXml(doc);
    PSLogSummary tgt = new PSLogSummary(srcEl);

    // round-trip produced an object
    assertTrue(tgt != null);
  }
}

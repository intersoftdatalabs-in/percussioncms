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

package test.percussion.pso.relationshipbuilder.exit;

import static org.custommonkey.xmlunit.XMLAssert.*;

import com.percussion.error.PSException;
import com.percussion.pso.relationshipbuilder.IPSRelationshipBuilder;
import com.percussion.pso.relationshipbuilder.exit.PSExtensionHelper;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.custommonkey.xmlunit.XMLUnit;
<<<<<<< HEAD
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
=======
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
>>>>>>> development-8.1.x
import org.w3c.dom.Document;

public class PSExtensionHelperTest {

  private Set<Integer> m_output;

<<<<<<< HEAD
  // JUnit 5 setup
  // ...existing code...
  @BeforeEach
=======
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  @Before
>>>>>>> development-8.1.x
  public void setUp() throws Exception {
    m_output = new HashSet<Integer>();
    XMLUnit.setControlParser("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
    XMLUnit.setTestParser("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
    XMLUnit.setSAXParserFactory("org.apache.xerces.jaxp.SAXParserFactoryImpl");
    XMLUnit.setTransformerFactory(
        "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
    XMLUnit.setIgnoreWhitespace(true);
  }

<<<<<<< HEAD
  @Test
=======
>>>>>>> development-8.1.x
  public void testConvertRejectsNullOutput() {
    boolean threw = false;
    try {
      PSExtensionHelper.convert(null, null);
    } catch (IllegalArgumentException e) {
      threw = true;
    }
    assertTrue(threw);
  }

<<<<<<< HEAD
  @Test
=======
>>>>>>> development-8.1.x
  public void testConvertHandlesNullInput() {
    Collection<Object> invalids = PSExtensionHelper.convert(null, m_output);
    assertNotNull(invalids);
    assertEquals(0, invalids.size());
  }

<<<<<<< HEAD
  @Test
=======
>>>>>>> development-8.1.x
  public void testConvertHandlesAllNullsInInput() {
    Object[] input = new Object[] {null, null};
    Collection<Object> invalids = PSExtensionHelper.convert(input, m_output);
    assertNotNull(invalids);
    assertEquals(2, invalids.size());
  }

<<<<<<< HEAD
  @Test
=======
>>>>>>> development-8.1.x
  public void testConvertHandlesNullsInInput() {
    Object[] input = new Object[] {"700", null, 301};
    Collection<Object> invalids = PSExtensionHelper.convert(input, m_output);
    assertNotNull(invalids);
    assertEquals(1, invalids.size());
    assertEquals(2, m_output.size());
  }

<<<<<<< HEAD
  @Test
=======
>>>>>>> development-8.1.x
  public void testConvertHandlesEmptysInInput() {
    Object[] input = new Object[] {"700", "", 301};
    Collection<Object> invalids = PSExtensionHelper.convert(input, m_output);
    assertNotNull(invalids);
    assertEquals(1, invalids.size());
    assertEquals(2, m_output.size());
  }

<<<<<<< HEAD
  @Test
=======
>>>>>>> development-8.1.x
  public void testConvertSingleEmptyString() {
    Object[] input = new Object[] {""};
    Collection<Object> invalids = PSExtensionHelper.convert(input, m_output);
    assertNotNull(invalids);
    assertEquals(1, invalids.size());
    assertEquals(0, m_output.size());
    if (invalids.size() == 1 && invalids.contains("")) {

    } else {
      fail();
    }
  }

  @Test
<<<<<<< HEAD
  @Disabled("Test is failing") // TODO: Fix me
=======
  @Ignore("Test is failing") // TODO: Fix me
>>>>>>> development-8.1.x
  public void testUpdateDisplayChoicesSelectAll() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    params.put(PSExtensionHelper.IDS_FIELD_NAME, "tree");
    params.put(IPSHtmlParameters.SYS_CONTENTID, "100");
    IPSRelationshipBuilder builder =
        new IPSRelationshipBuilder() {
          public Collection<Integer> retrieve(int sourceId)
              throws PSAssemblyException, PSException {
            return null;
          }

          public void synchronize(int sourceId, Set<Integer> targetIds)
              throws PSAssemblyException, PSException {}

          public void addRelationships(Collection<Integer> ids)
              throws PSAssemblyException, PSException {}
        };
    PSExtensionHelper helper = new PSExtensionHelper(builder, params, null);
    String basePath = "src/test/percussion/pso/relationshipbuilder/exit";
    Document actual =
        PSXmlDocumentBuilder.createXmlDocument(
            new FileInputStream(new File(basePath + "/" + "BeforeCe.xml")), false);
    Document expected =
        PSXmlDocumentBuilder.createXmlDocument(
            new FileInputStream(new File(basePath + "/" + "ExpectedSelectAllCe.xml")), false);
    helper.updateDisplayChoices(actual, true);
    assertXMLEqual(expected, actual);
  }

  @Test
<<<<<<< HEAD
  @Disabled("Test is failing") // TODO: Fix me
=======
  @Ignore("Test is failing") // TODO: Fix me
>>>>>>> development-8.1.x
  public void testUpdateDisplayChoices() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    params.put(PSExtensionHelper.IDS_FIELD_NAME, "tree");
    params.put(IPSHtmlParameters.SYS_CONTENTID, "100");

    IPSRelationshipBuilder stub =
        new IPSRelationshipBuilder() {

          public Collection<Integer> retrieve(int sourceId)
              throws PSAssemblyException, PSException {
            if (sourceId != 100) throw new IllegalStateException("Content id is wrong");
            return Arrays.asList(307, 318);
          }

          public void synchronize(int sourceId, Set<Integer> targetIds)
              throws PSAssemblyException, PSException {
            throw new IllegalStateException("Should not be called");
          }

          public void addRelationships(Collection<Integer> ids)
              throws PSAssemblyException, PSException {}
        };

    PSExtensionHelper helper = new PSExtensionHelper(stub, params, null);
    String basePath = "src/test/percussion/pso/relationshipbuilder/exit";
    Document actual =
        PSXmlDocumentBuilder.createXmlDocument(
            new FileInputStream(new File(basePath + "/" + "BeforeCe.xml")), false);
    Document expected =
        PSXmlDocumentBuilder.createXmlDocument(
            new FileInputStream(new File(basePath + "/" + "ExpectedCe.xml")), false);
    helper.updateDisplayChoices(actual, false);

    assertXMLEqual(expected, actual);
    // 307, 318

  }
}

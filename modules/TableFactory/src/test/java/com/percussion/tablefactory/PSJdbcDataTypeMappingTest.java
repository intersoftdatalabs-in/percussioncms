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

package com.percussion.tablefactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.StringReader;
<<<<<<< HEAD
import org.junit.jupiter.api.Test;
=======
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
>>>>>>> development-8.1.x
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Unit tests for <code>PSJdbcDataTypeMapping</code>. */
<<<<<<< HEAD
public class PSJdbcDataTypeMappingTest {
=======
public class PSJdbcDataTypeMappingTest extends TestCase {
  public PSJdbcDataTypeMappingTest(String name) {
    super(name);
  }
>>>>>>> development-8.1.x

  /**
   * Tests the various permutations of illegal parameters to the ctor to make sure they all throw
   * IllegalArgumentExceptions.
   */
<<<<<<< HEAD
  @Test
=======
>>>>>>> development-8.1.x
  public void testIllegalCtors() throws Exception {
    testIllegalCtor("", "BIT", null, null, null);
    testIllegalCtor("BIT", "", null, null, null);
    testIllegalCtor("BIT", "BIT", "", null, null);
    testIllegalCtor("BIT", "BIT", "5", "", null);
    testIllegalCtor("BIT", "BIT", "5", "5", "");

    Document doc;
    // misspelled root node
    doc =
        PSXmlDocumentBuilder.createXmlDocument(
            new StringReader(
<<<<<<< HEAD
                "<DataTypeOops jdbc=\"VARBINARY\" native=\"VARCHAR\" defaultSize=\"1\" suffix=\"FOR"
                    + " BIT DATA\"/>"),
=======
                "<DataTypeOops jdbc=\"VARBINARY\" native=\"VARCHAR\" defaultSize=\"1\" suffix=\"FOR BIT DATA\"/>"),
>>>>>>> development-8.1.x
            false);
    testIllegalCtor(doc.getDocumentElement());

    // missing @jdbc
    doc =
        PSXmlDocumentBuilder.createXmlDocument(
            new StringReader(
                "<DataType native=\"VARCHAR\" defaultSize=\"1\" suffix=\"FOR BIT DATA\"/>"),
            false);
    testIllegalCtor(doc.getDocumentElement());

    // missing @native
    doc =
        PSXmlDocumentBuilder.createXmlDocument(
            new StringReader(
                "<DataType jdbc=\"VARBINARY\" defaultSize=\"1\" suffix=\"FOR BIT DATA\"/>"),
            false);
    testIllegalCtor(doc.getDocumentElement());
  }

  /**
   * Constructs a PSJdbcDataTypeMapping with arguments assumed to be illegal and makes sure an
   * IllegalArgumentException is thrown.
   */
  private void testIllegalCtor(
      String jdbc, String nativeStr, String defaultSize, String defaultScale, String suffix) {
    boolean didThrow = false;
    try {
      PSJdbcDataTypeMapping mapping =
          new PSJdbcDataTypeMapping(jdbc, nativeStr, defaultSize, defaultScale, suffix);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
<<<<<<< HEAD
    assertTrue(didThrow, "Expected IllegalArgumentException for invalid ctor args");
=======
    assertTrue(didThrow);
>>>>>>> development-8.1.x
  }

  /**
   * Constructs a PSJdbcDataTypeMapping from an XML representation assumed to be invalid and makes
   * sure an PSJdbcTableFactoryException is thrown.
   */
  private void testIllegalCtor(Element sourceNode) {
    boolean didThrow = false;
    try {
      PSJdbcDataTypeMapping mapping = new PSJdbcDataTypeMapping(sourceNode);
    } catch (PSJdbcTableFactoryException e) {
      didThrow = true;
    }
<<<<<<< HEAD
    assertTrue(didThrow, "Expected PSJdbcTableFactoryException for invalid XML ctor");
  }

  /** Tests the ctor with a various parameters and makes sure the parameters are assigned. */
  @Test
=======
    assertTrue(didThrow);
  }

  /** Tests the ctor with a various parameters and makes sure the parameters are assigned. */
>>>>>>> development-8.1.x
  public void testCtorAndGetters() throws Exception {
    testCtorsAndGetters("BIT", "BIT", null, null, null);
    testCtorsAndGetters("BIT", "CHAR", "1", null, null);
    testCtorsAndGetters("VARBINARY", "VARCHAR", "20", null, "FOR BIT DATA");
    testCtorsAndGetters("FLOAT", "NUMBER", "10", "5", null);
  }

  /**
   * Constructs a PSJdbcDataTypeMapping with the supplied parameters and makes sure the getters
   * methods return the values assigned. Also performs an XML representation copy and make sure the
   * copy's getters methods return the same values.
   */
  private void testCtorsAndGetters(
      String jdbc, String nativeStr, String defaultSize, String defaultScale, String suffix)
      throws PSJdbcTableFactoryException {
    PSJdbcDataTypeMapping mapping =
        new PSJdbcDataTypeMapping(jdbc, nativeStr, defaultSize, defaultScale, suffix);
    assertInstanceValues(mapping, jdbc, nativeStr, defaultSize, defaultScale, suffix);

    PSJdbcDataTypeMapping mappingCopy =
        new PSJdbcDataTypeMapping(mapping.toXml(PSXmlDocumentBuilder.createXmlDocument()));
    assertInstanceValues(mappingCopy, jdbc, nativeStr, defaultSize, defaultScale, suffix);
  }

  /** Tests a particular instance to make sure its getter methods return the supplied values. */
  private static void assertInstanceValues(
      PSJdbcDataTypeMapping dataType,
      String jdbc,
      String nativeStr,
      String defaultSize,
      String defaultScale,
      String suffix) {
    assertEquals(jdbc, dataType.getJdbc());
    assertEquals(nativeStr, dataType.getNative());
    assertEquals(defaultSize, dataType.getDefaultSize());
    assertEquals(defaultScale, dataType.getDefaultScale());
    assertEquals(suffix, dataType.getSuffix());
  }
<<<<<<< HEAD
=======

  /** Collect all tests into a TestSuite and returns it */
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new PSJdbcDataTypeMappingTest("testIllegalCtors"));
    suite.addTest(new PSJdbcDataTypeMappingTest("testCtorAndGetters"));
    return suite;
  }
>>>>>>> development-8.1.x
}

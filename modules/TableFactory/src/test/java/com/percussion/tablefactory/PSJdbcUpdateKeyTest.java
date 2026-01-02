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
import java.util.ArrayList;
import java.util.List;
<<<<<<< HEAD
import org.junit.jupiter.api.Test;
=======
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
>>>>>>> development-8.1.x
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Unit test for PSJdbcUpdateKey. */
<<<<<<< HEAD
public class PSJdbcUpdateKeyTest {

  /** Test the def */
  @Test
  public void testDef() throws Exception {
    // build a def with a dupe name
    List<String> cols = new ArrayList<>();
=======
public class PSJdbcUpdateKeyTest extends TestCase {
  public PSJdbcUpdateKeyTest(String name) {
    super(name);
  }

  /** Test the def */
  public void testDef() throws Exception {
    // build a def with a dupe name
    List cols = new ArrayList();
>>>>>>> development-8.1.x
    cols.add("col1");
    cols.add("col2");
    cols.add("col1");

    boolean caught = false;
    try {
      PSJdbcUpdateKey uc = new PSJdbcUpdateKey(cols.iterator());
    } catch (PSJdbcTableFactoryException e) {
      caught = true;
    }
    assertTrue(caught);

    // build def with null name
<<<<<<< HEAD
    cols = new ArrayList<>();
=======
    cols = new ArrayList();
>>>>>>> development-8.1.x
    cols.add("col1");
    cols.add(null);

    caught = false;
    try {
      PSJdbcUpdateKey uc = new PSJdbcUpdateKey(cols.iterator());
    } catch (PSJdbcTableFactoryException e) {
      caught = true;
    }
    assertTrue(caught);

    // build def with empty name
<<<<<<< HEAD
    cols = new ArrayList<>();
=======
    cols = new ArrayList();
>>>>>>> development-8.1.x
    cols.add("col1");
    cols.add("");

    caught = false;
    try {
      PSJdbcUpdateKey uc = new PSJdbcUpdateKey(cols.iterator());
    } catch (PSJdbcTableFactoryException e) {
      caught = true;
    }
    assertTrue(caught);

    // build def with empty list
<<<<<<< HEAD
    cols = new ArrayList<>();
=======
    cols = new ArrayList();
>>>>>>> development-8.1.x

    caught = false;
    try {
      PSJdbcUpdateKey uc = new PSJdbcUpdateKey(cols.iterator());
    } catch (IllegalArgumentException e) {
      caught = true;
    }
    assertTrue(caught);

    // build valid def
<<<<<<< HEAD
    cols = new ArrayList<>();
=======
    cols = new ArrayList();
>>>>>>> development-8.1.x
    cols.add("col1");
    cols.add("col2");
    cols.add("col3");

    PSJdbcUpdateKey uc = new PSJdbcUpdateKey(cols.iterator());

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = uc.toXml(doc);

    PSJdbcUpdateKey uc2 = new PSJdbcUpdateKey(el);
    assertEquals(uc, uc2);
  }
<<<<<<< HEAD
=======

  // collect all tests into a TestSuite and return it
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new PSJdbcUpdateKeyTest("testDef"));
    return suite;
  }
>>>>>>> development-8.1.x
}

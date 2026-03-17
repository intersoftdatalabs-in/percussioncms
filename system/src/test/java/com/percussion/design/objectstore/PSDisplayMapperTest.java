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
package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.util.PSCollection;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// Test case
public class PSDisplayMapperTest {
  @Test
  public void testEquals() throws Exception {}

  @Test
  public void testXml() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, "Test");

    // create test object
    PSBackEndCredential cred = new PSBackEndCredential("credential_1");
    cred.setDataSource("rxdefault");

    PSTableLocator tl = new PSTableLocator(cred);
    PSTableRef tr = new PSTableRef("tableName_1", "tableAlias_1");
    PSTableSet ts = new PSTableSet(tl, tr);
    PSCollection tsCol = new PSCollection(ts.getClass());
    tsCol.add(ts);
    PSContainerLocator loc = new PSContainerLocator(tsCol);
    PSBackEndTable table = new PSBackEndTable("RXARTICLE");
    PSField f = new PSField("field_1", new PSBackEndColumn(table, "DISPLAYTITLE"));
    PSFieldSet fs = new PSFieldSet("fieldSet_1", f);
    PSUISet uiSet = new PSUISet();
    PSParam param = new PSParam("param_1", new PSTextLiteral("value_1"));
    PSCollection parameters = new PSCollection(param.getClass());
    parameters.add(param);
    uiSet.setControl(new PSControlRef("sys_EditBox", parameters));
    PSDisplayMapping mapping = new PSDisplayMapping("DISPLAYTITLE", uiSet);

    PSDisplayMapper testTo = new PSDisplayMapper("dispMapper_1");
    testTo.add(mapping);
    testTo.add(mapping);
    testTo.add(mapping);
    Element elem = testTo.toXml(doc);
    root.appendChild(elem);

    // create a new object and populate it from our testTo element
    PSDisplayMapper testFrom = new PSDisplayMapper(elem, null, null);
    assertEquals(testTo, testFrom);
  }

  // JUnit 5 uses test discovery; explicit suite() is not required and has been removed.

}

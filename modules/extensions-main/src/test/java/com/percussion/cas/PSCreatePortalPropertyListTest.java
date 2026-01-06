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
package com.percussion.cas;

import com.percussion.data.PSConversionException;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * @author DougRand
 *     <p>To change the template for this generated type comment go to
 *     Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
@Tag("UnitTest")
public class PSCreatePortalPropertyListTest {
  private static final Logger log = LogManager.getLogger(PSCreatePortalPropertyListTest.class);

  /** Test parameters for the test */
  Object[] params = {
    "METADATA",
    "searchKeywords",
    "string",
    "",
    "foo, bar",
    "excludeFromSearch",
    "string",
    "",
    "no",
    "categories",
    "string",
    ",",
    "white,yellow,red",
    "regions",
    "integer",
    "",
    "1, 2",
    "locale",
    "string",
    "",
    "en-us",
    "creationDate",
    "date",
    "yyyyMMdd",
    "20030612",
    "emptyValue",
    "date",
    "",
    "",
    "number",
    "integer",
    "",
    "123"
  };
  String resultDoc =
      "<METADATA>"
          + "<Property name=\"searchKeywords\" pattern=\"\" type=\"string\">foo, bar</Property>"
          + "<Property name=\"excludeFromSearch\" pattern=\"\" type=\"string\">no</Property>"
          + "<Property name=\"categories\" pattern=\"\" type=\"string\">white</Property>"
          + "<Property name=\"categories\" pattern=\"\" type=\"string\">yellow</Property>"
          + "<Property name=\"categories\" pattern=\"\" type=\"string\">red</Property>"
          + "<Property name=\"regions\" pattern=\"\" type=\"integer\">1</Property>"
          + "<Property name=\"regions\" pattern=\"\" type=\"integer\">2</Property>"
          + "<Property name=\"locale\" pattern=\"\" type=\"string\">en-us</Property>"
          + "<Property name=\"creationDate\" pattern=\"yyyyMMdd\" type=\"date\">20030612</Property>"
          + "<Property name=\"number\" pattern=\"\" type=\"integer\">123</Property>"
          + "</METADATA>";

  public PSCreatePortalPropertyListTest() {}

  @Test
  public void testList() throws PSConversionException {
    PSCreatePortalPropertyList test = new PSCreatePortalPropertyList();

    Document doc = (Document) test.processUdf(params, null);
    log.info(PSXmlDocumentBuilder.toString(doc));
  }
}

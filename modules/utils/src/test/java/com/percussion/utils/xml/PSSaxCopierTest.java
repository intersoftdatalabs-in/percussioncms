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
package com.percussion.utils.xml;

import static com.percussion.util.PSResourceUtils.getResourcePath;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Tests the copier by parsing an external file and comparing the original
 * document with the document created by the sax copier. The external file
 * should be maintained to include all types of elements copied.
 *
 * Note that the comparison method doesn't allow certain elements to be tested
 * so don't add this. The current known list:
 * <ul>
 * <li>Comments
 * </ul>
 *
 * @author dougrand
 */
public class PSSaxCopierTest {
  /**
   * Test file in the unit resources tree
   */
  public static final File ms_testFile =
      new File(
          getResourcePath(PSSaxCopierTest.class, "/com/percussion/utils/xml/SaxCopierInput.xml"));

  /**
   * Do the test
   *
   * @throws Exception
   */
  @Test
  public void testCopier() throws Exception {
    DocumentBuilderFactory dbf =
        PSSecureXMLUtils.getSecuredDocumentBuilderFactory(
            new PSXmlSecurityOptions(true, true, true, false, true, false));

    SAXParserFactory spf =
        PSSecureXMLUtils.getSecuredSaxParserFactory(
            new PSXmlSecurityOptions(true, true, true, false, true, false));
    StringWriter stringWriter = new StringWriter();

    DocumentBuilder db = dbf.newDocumentBuilder();
    SAXParser sp = spf.newSAXParser();

    Document doc = db.parse(ms_testFile);

    XMLOutputFactory ofact = XMLOutputFactory.newInstance();
    XMLStreamWriter writer = ofact.createXMLStreamWriter(stringWriter);
    PSSaxCopier copier = new PSSaxCopier(writer, new HashMap<String, String>(), true);
    sp.parse(ms_testFile, copier);
    writer.close();
    stringWriter.close();

    Document doc2 = db.parse(new ByteArrayInputStream(stringWriter.toString().getBytes()));
    String first = PSXmlDocumentBuilder.toString(doc);
    String second = PSXmlDocumentBuilder.toString(doc2);
    assertEquals(first, second);
  }

  private static final String ms_tcomments =
      "<!-- a comment --><document>" + "<![CDATA[some cdata characters]]></document>";

  /**
   * Do the test
   *
   * @throws Exception
   */
  @Test
  public void testHelper() throws Exception {
    Map<String, String> renames = new HashMap<String, String>();
    String output = PSSaxHelper.parseWithXMLWriter(ms_tcomments, PSSaxCopier.class, renames, true);
    assertEquals(ms_tcomments, output);
  }
}

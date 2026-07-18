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
/*
 * test.percussion.pso.preview SimpleXmlViewTest.java
 *
 * @author DavidBenua
 *
 */
package test.percussion.pso.preview;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.preview.SimpleXmlView;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class SimpleXmlViewTest {
  private static final Logger log = LogManager.getLogger(SimpleXmlViewTest.class);
  SimpleXmlView cut;
  Map<String, Object> model;

  @BeforeEach
  public void setUp() {
    cut = new SimpleXmlView();
    model = new HashMap<String, Object>();
    cut.setEncoding("UTF-8");
  }

  @Test
  void testRenderMergedOutputModelMapHttpServletRequestHttpServletResponse() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(doc, "root");

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    cut.setResultKey("foo");
    model.put("foo", doc);

    try {
      cut.render(model, request, response);
      byte[] output = response.getContentAsByteArray();
      String oString = new String(output, "UTF-8");
      assertNotNull(oString);
      assertTrue(oString.contains("root"));
      log.info("output is " + oString);
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception");
    }
  }

  @Test
  void testRenderMergedOutputWrongTypeWritesGenericError() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    cut.setResultKey("foo");
    model.put("foo", "doc");

    cut.render(model, request, response);
    assertEquals(500, response.getStatus());
    String body = response.getContentAsString();
    assertTrue(body.contains("An error occurred while rendering the response"));
  }

  @Test
  void testRenderMergedOutputWrongNameWritesGenericError() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    cut.setResultKey("faz");
    model.put("foo", "doc");

    cut.render(model, request, response);
    assertEquals(500, response.getStatus());
    String body = response.getContentAsString();
    assertTrue(body.contains("An error occurred while rendering the response"));
  }
}

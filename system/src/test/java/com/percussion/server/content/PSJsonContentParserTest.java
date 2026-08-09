/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.server.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.content.IPSMimeContentTypes;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestParsingException;
import com.percussion.util.PSInputStreamReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Unit tests for {@link PSJsonContentParser}. */
class PSJsonContentParserTest {

  private final PSJsonContentParser parser = new PSJsonContentParser();

  @Test
  void supportsApplicationJsonOnly() {
    String[] types = parser.getSupportedContentTypes();
    assertEquals(1, types.length);
    assertEquals(IPSMimeContentTypes.MIME_TYPE_JSON, types[0]);
    assertTrue(parser.isSupportedContentType(IPSMimeContentTypes.MIME_TYPE_JSON));
  }

  @Test
  void parse_setsInputDocument() throws Exception {
    PSRequest request = new PSRequest(null, null, null, null);
    String json = "{\"Properties\":{\"Type\":\"workflow\",\"Name\":\"wf1\"}}";
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    PSInputStreamReader reader =
        new PSInputStreamReader(
            new ByteArrayInputStream(bytes), false, PSContentParser.MIN_PUSHBACK_BUF_SIZE);

    parser.parse(
        request, IPSMimeContentTypes.MIME_TYPE_JSON, "UTF-8", reader, bytes.length);

    Document doc = request.getInputDocument();
    assertNotNull(doc);
    Element root = doc.getDocumentElement();
    assertEquals("Properties", root.getNodeName());
    assertEquals("workflow", textChild(root, "Type"));
    assertEquals("wf1", textChild(root, "Name"));
  }

  @Test
  void parse_emptyBody_leavesInputDocumentUnset() throws Exception {
    PSRequest request = new PSRequest(null, null, null, null);
    PSInputStreamReader reader =
        new PSInputStreamReader(
            new ByteArrayInputStream(new byte[0]), false, PSContentParser.MIN_PUSHBACK_BUF_SIZE);

    parser.parse(request, IPSMimeContentTypes.MIME_TYPE_JSON, "UTF-8", reader, 0);

    assertNull(request.getInputDocument());
  }

  @Test
  void parse_malformedJson_throws() throws Exception {
    PSRequest request = new PSRequest(null, null, null, null);
    byte[] bytes = "{bad".getBytes(StandardCharsets.UTF_8);
    PSInputStreamReader reader =
        new PSInputStreamReader(
            new ByteArrayInputStream(bytes), false, PSContentParser.MIN_PUSHBACK_BUF_SIZE);

    assertThrows(
        PSRequestParsingException.class,
        () ->
            parser.parse(
                request, IPSMimeContentTypes.MIME_TYPE_JSON, "UTF-8", reader, bytes.length));
  }

  private static String textChild(Element parent, String name) {
    return parent.getElementsByTagName(name).item(0).getTextContent();
  }
}

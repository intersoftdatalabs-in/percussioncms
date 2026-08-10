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
import com.percussion.data.PSXmlFieldExtractor;
import com.percussion.server.IPSServerErrors;
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
    parseJson(request, json);

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

    PSRequestParsingException ex =
        assertThrows(
            PSRequestParsingException.class,
            () ->
                parser.parse(
                    request, IPSMimeContentTypes.MIME_TYPE_JSON, "UTF-8", reader, bytes.length));
    assertEquals(IPSServerErrors.JSON_PARSER_ERROR, ex.getErrorCode());
  }

  @Test
  void parse_negativeContentLength_throws() {
    PSRequest request = new PSRequest(null, null, null, null);
    PSInputStreamReader reader =
        new PSInputStreamReader(
            new ByteArrayInputStream(new byte[0]), false, PSContentParser.MIN_PUSHBACK_BUF_SIZE);

    PSRequestParsingException ex =
        assertThrows(
            PSRequestParsingException.class,
            () ->
                parser.parse(
                    request, IPSMimeContentTypes.MIME_TYPE_JSON, "UTF-8", reader, -1));
    assertTrue(ex.getMessage().contains("Invalid Content-Length"));
  }

  @Test
  void parse_contentLengthShorterThanStream_parsesAvailableBytes() throws Exception {
    PSRequest request = new PSRequest(null, null, null, null);
    // Body is well-formed JSON; Content-Length claims fewer bytes than available (peer can send
    // extra). Parser must only consume the declared length and still parse successfully.
    String json = "{\"Order\":{\"Sku\":\"A\"}}";
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    byte[] padded = new byte[bytes.length + 32];
    System.arraycopy(bytes, 0, padded, 0, bytes.length);
    PSInputStreamReader reader =
        new PSInputStreamReader(
            new ByteArrayInputStream(padded), false, PSContentParser.MIN_PUSHBACK_BUF_SIZE);

    parser.parse(
        request, IPSMimeContentTypes.MIME_TYPE_JSON, "UTF-8", reader, bytes.length);

    Document doc = request.getInputDocument();
    assertNotNull(doc);
    assertEquals("Order", doc.getDocumentElement().getNodeName());
    assertEquals("A", textChild(doc.getDocumentElement(), "Sku"));
  }

  @Test
  void parse_contentLengthLongerThanStream_warnsAndParsesTruncatedBodyWhenValid()
      throws Exception {
    PSRequest request = new PSRequest(null, null, null, null);
    String json = "{\"Item\":{\"Id\":\"1\"}}";
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    PSInputStreamReader reader =
        new PSInputStreamReader(
            new ByteArrayInputStream(bytes), false, PSContentParser.MIN_PUSHBACK_BUF_SIZE);

    // Declared length exceeds available data (socket closed early). Temp-file length is shorter;
    // still parse what was read (same warning path as PSXmlContentParser).
    parser.parse(
        request, IPSMimeContentTypes.MIME_TYPE_JSON, "UTF-8", reader, bytes.length + 50);

    Document doc = request.getInputDocument();
    assertNotNull(doc);
    assertEquals("Item", doc.getDocumentElement().getNodeName());
    assertEquals("1", textChild(doc.getDocumentElement(), "Id"));
  }

  @Test
  void parse_contentLengthLongerThanStream_invalidTruncatedJson_throwsJsonParserError() {
    PSRequest request = new PSRequest(null, null, null, null);
    // Incomplete JSON body with Content-Length claiming more bytes than available. Temp-file path
    // must still surface JSON_PARSER_ERROR (not an unexpected exception) after the short read.
    byte[] bytes = "{\"Item\":{\"Id\":".getBytes(StandardCharsets.UTF_8);
    PSInputStreamReader reader =
        new PSInputStreamReader(
            new ByteArrayInputStream(bytes), false, PSContentParser.MIN_PUSHBACK_BUF_SIZE);

    PSRequestParsingException ex =
        assertThrows(
            PSRequestParsingException.class,
            () ->
                parser.parse(
                    request,
                    IPSMimeContentTypes.MIME_TYPE_JSON,
                    "UTF-8",
                    reader,
                    bytes.length + 50));
    assertEquals(IPSServerErrors.JSON_PARSER_ERROR, ex.getErrorCode());
  }

  @Test
  void parse_moderateBody_viaTempFilePath() throws Exception {
    PSRequest request = new PSRequest(null, null, null, null);
    // Large enough to span multiple 2KB read chunks in readContentIntoPurgableTempFile.
    StringBuilder items = new StringBuilder();
    items.append("{\"Catalog\":{\"Item\":[");
    for (int i = 0; i < 200; i++) {
      if (i > 0) {
        items.append(',');
      }
      items
          .append("{\"Sku\":\"SKU-")
          .append(String.format("%04d", i))
          .append("\",\"Desc\":\"item description padding ")
          .append("x".repeat(40))
          .append("\"}");
    }
    items.append("]}}");
    String json = items.toString();
    assertTrue(json.length() > 4096, "body should exceed single 2KB temp-file chunk");

    parseJson(request, json);

    Document doc = request.getInputDocument();
    assertNotNull(doc);
    assertEquals("Catalog", doc.getDocumentElement().getNodeName());
    assertEquals(
        200, doc.getDocumentElement().getElementsByTagName("Item").getLength());
  }

  @Test
  void parse_rejectsEmbeddedFileUrlAttribute() throws Exception {
    PSRequest request = new PSRequest(null, null, null, null);
    // JSON attribute form is "@name" → XML attribute (codec ATTR_PREFIX).
    String attrKey = "@" + PSXmlFieldExtractor.XML_URL_REFERENCE_ATTRIBUTE;
    String json =
        "{\"Doc\":{\"" + attrKey + "\":\"file:///etc/passwd\",\"Name\":\"x\"}}";
    parseJson(request, json);

    // Same security rule as PSXmlContentParser: document cleared when file URL attr present.
    assertNull(request.getInputDocument());
  }

  @Test
  void parse_nullCharset_usesDefaultAndSucceeds() throws Exception {
    PSRequest request = new PSRequest(null, null, null, null);
    String json = "{\"Root\":{\"V\":\"ok\"}}";
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    PSInputStreamReader reader =
        new PSInputStreamReader(
            new ByteArrayInputStream(bytes), false, PSContentParser.MIN_PUSHBACK_BUF_SIZE);

    parser.parse(request, IPSMimeContentTypes.MIME_TYPE_JSON, null, reader, bytes.length);

    assertNotNull(request.getInputDocument());
    assertEquals("ok", textChild(request.getInputDocument().getDocumentElement(), "V"));
  }

  @Test
  void parse_unsupportedContentType_throws() {
    PSRequest request = new PSRequest(null, null, null, null);
    PSInputStreamReader reader =
        new PSInputStreamReader(
            new ByteArrayInputStream(new byte[0]), false, PSContentParser.MIN_PUSHBACK_BUF_SIZE);

    PSRequestParsingException ex =
        assertThrows(
            PSRequestParsingException.class,
            () ->
                parser.parse(
                    request, "text/plain", "UTF-8", reader, 0));
    assertEquals(IPSServerErrors.PARSER_UNSUPPORTED_CONTENT_TYPE, ex.getErrorCode());
  }

  private void parseJson(PSRequest request, String json) throws Exception {
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    PSInputStreamReader reader =
        new PSInputStreamReader(
            new ByteArrayInputStream(bytes), false, PSContentParser.MIN_PUSHBACK_BUF_SIZE);
    parser.parse(
        request, IPSMimeContentTypes.MIME_TYPE_JSON, "UTF-8", reader, bytes.length);
  }

  private static String textChild(Element parent, String name) {
    return parent.getElementsByTagName(name).item(0).getTextContent();
  }
}

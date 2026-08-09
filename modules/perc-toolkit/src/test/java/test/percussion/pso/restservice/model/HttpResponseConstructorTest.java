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
package test.percussion.pso.restservice.model;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.restservice.model.HttpDOMResponse;
import com.percussion.pso.restservice.model.HttpHtmlResponse;
import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;
import org.dom4j.DocumentHelper;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

/** Single-shot constructor coverage for HTTP response models. */
public class HttpResponseConstructorTest {

  private static HttpHeaders sampleHeaders() {
    return HttpHeaders.of(
        Map.of("ETag", List.of("\"abc\""), "Last-Modified", List.of("Wed, 01 Jan 2020 00:00:00 GMT")),
        (n, v) -> true);
  }

  @Test
  void domResponseConstructorSetsDocumentAndHeaders() {
    org.dom4j.Document doc = DocumentHelper.createDocument();
    doc.addElement("root").addText("ok");
    HttpHeaders headers = sampleHeaders();

    HttpDOMResponse response = new HttpDOMResponse(doc, headers);

    assertSame(doc, response.getDocument());
    assertSame(headers, response.getHeaders());
    assertEquals("\"abc\"", response.getETag());
  }

  @Test
  void htmlResponseConstructorSetsDocumentAndHeaders() {
    org.jsoup.nodes.Document doc = Jsoup.parse("<html><body>hi</body></html>");
    HttpHeaders headers = sampleHeaders();

    HttpHtmlResponse response = new HttpHtmlResponse(doc, headers);

    assertSame(doc, response.getDocument());
    assertSame(headers, response.getHeaders());
    assertTrue(response.getLastModified().contains("2020"));
  }
}

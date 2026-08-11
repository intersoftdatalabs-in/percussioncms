/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.pso.restservice.model;

import java.net.http.HttpHeaders;
import org.jsoup.nodes.Document;

/**
 * HTTP response holding an HTML document payload.
 */
public class HttpHtmlResponse extends BaseHttpResponse {

  private Document document;

  /**
   * Creates a new HttpHtmlResponse.
   */
  public HttpHtmlResponse() {}

  /**
   * Sets the document.
   *
   * @param document the document
   */
  public void setDocument(Document document) {
    this.document = document;
  }

  /**
   * Returns the document.
   *
   * @return the result
   */
  public Document getDocument() {
    return document;
  }

  /***
   * Single Shot Constructor
   * @param doc the doc
   * @param head the head
   */
  public HttpHtmlResponse(Document doc, HttpHeaders head) {
    // Direct field assignment (headers is package-visible on base) — no this-escape.
    this.headers = head;
    this.document = doc;
  }
}

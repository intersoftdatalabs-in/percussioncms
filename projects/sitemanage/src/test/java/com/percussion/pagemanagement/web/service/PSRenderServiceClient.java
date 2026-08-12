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
// REFACTORED: CP-JAVA11

package com.percussion.pagemanagement.web.service;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRenderResult;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.share.test.PSObjectRestClient;

public class PSRenderServiceClient extends PSObjectRestClient {
  private final String path = "/Rhythmyx/services/pagemanagement/render";

  public PSRenderServiceClient() {
    super();
    seedAccepts();
  }

  public PSRenderServiceClient(String baseUrl) {
    super(baseUrl);
    seedAccepts();
  }

  private void seedAccepts() {
    // Direct header mutation after super — no this-escape method dispatch.
    String accept = requestHeaders.get("Accept");
    if (accept == null || accept.isBlank()) {
      requestHeaders.put("Accept", "text/html");
    } else {
      requestHeaders.put("Accept", accept + ",text/html");
    }
  }

  public String getPath() {
    return path;
  }

  public PSRenderResult renderRegion(PSPage page, String regionId) {
    return postObjectToPath(concatPath(getPath(), "page", regionId), page, PSRenderResult.class);
  }

  public PSRenderResult renderRegion(PSTemplate template, String regionId) {
    return postObjectToPath(
        concatPath(getPath(), "template", regionId), template, PSRenderResult.class);
  }

  public String renderPage(String id) {
    return GET(concatPath(path, "page", id));
  }

  public String renderPageForEdit(String id) {
    return GET(concatPath(path, "page/editmode", id));
  }

  public String renderTemplate(String id) {
    return GET(concatPath(path, "template", id));
  }

  public PSRegion parse(String html) {
    var response = POST(concatPath(path, "parse"), html);
    return objectFromResponseBody(response, PSRegion.class);
  }
}

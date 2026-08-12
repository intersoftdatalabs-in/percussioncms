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

package com.percussion.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSDataSet;
import com.percussion.design.objectstore.PSRequestor;
import com.percussion.design.objectstore.PSTextLiteral;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Regression: classic app request page matching must accept built-in document extensions (.xml /
 * .txt / .json) even when the requestor MimeProperties only list html/htm (#2665 / #2660).
 */
class PSRequestPageMapTest {

  @Test
  void isMatch_acceptsBuiltInDocumentExtensionsWhenMimePropsAreHtmlOnly() throws Exception {
    PSRequestPageMap map = buildMapWithHtmlMimeOnly("usercommunities");

    assertTrue(map.isMatch(null, requestFor("/Rhythmyx/sys_commSupport/usercommunities.xml")));
    assertTrue(map.isMatch(null, requestFor("/Rhythmyx/sys_commSupport/usercommunities.txt")));
    assertTrue(
        map.isMatch(null, requestFor("/Rhythmyx/sys_commSupport/usercommunities.json")),
        ".json must match like .xml/.txt so live classic JSON I/O is reachable");
  }

  @Test
  void isMatch_rejectsUnknownExtensionNotInMimeProps() throws Exception {
    PSRequestPageMap map = buildMapWithHtmlMimeOnly("usercommunities");
    assertFalse(map.isMatch(null, requestFor("/Rhythmyx/sys_commSupport/usercommunities.bin")));
  }

  @Test
  void isMatch_acceptsExtensionListedInMimeProps() throws Exception {
    PSRequestPageMap map = buildMapWithHtmlMimeOnly("usercommunities");
    assertTrue(map.isMatch(null, requestFor("/Rhythmyx/sys_commSupport/usercommunities.html")));
  }

  private static PSRequestPageMap buildMapWithHtmlMimeOnly(String requestPage) throws Exception {
    PSRequestor requestor = new PSRequestor();
    requestor.setRequestPage(requestPage);
    HashMap<String, PSTextLiteral> mime = new HashMap<>();
    mime.put("html", new PSTextLiteral("text/html"));
    mime.put("htm", new PSTextLiteral("text/html"));
    requestor.setMimeProperties(mime);

    PSDataSet dataSet = new PSDataSet("testDs");
    dataSet.setRequestor(requestor);

    return new PSRequestPageMap(dataSet, new NoopRequestHandler());
  }

  private static PSRequest requestFor(String fileUrl) {
    MockHttpServletRequest servletReq = new MockHttpServletRequest("GET", fileUrl);
    MockHttpServletResponse servletRes = new MockHttpServletResponse();
    PSRequest request = new PSRequest(servletReq, servletRes, null, null);
    request.setRequestFileURL(fileUrl);
    return request;
  }

  /** Minimal handler so map construction does not need a full data pipeline. */
  private static final class NoopRequestHandler implements IPSRequestHandler {
    @Override
    public void processRequest(PSRequest request) {
      // no-op for unit test
    }

    @Override
    public void shutdown() {
      // no-op
    }
  }
}

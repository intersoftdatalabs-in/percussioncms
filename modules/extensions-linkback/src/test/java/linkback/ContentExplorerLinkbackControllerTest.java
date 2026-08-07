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
 * test.percussion.soln.linkback ContentExplorerLinkbackControllerTest.java
 *
 * @author JasonChu
 *
 */
package linkback;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.soln.linkback.codec.impl.StringLinkBackTokenImpl;
import com.percussion.soln.linkback.servlet.ContentExplorerLinkbackController;
import com.percussion.soln.linkback.utils.LinkbackUtils;
import com.percussion.system.utils.IPSHtmlParameters;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class ContentExplorerLinkbackControllerTest {

  private static final Logger log = LogManager.getLogger(ActionPanelLinkbackControllerTest.class);

  ContentExplorerLinkbackController cut;
  String token_nocontentid;

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  public void setUp() throws Exception {
    cut = new ContentExplorerLinkbackController();
    cut.setHelpViewName("helpview");
    cut.setErrorViewName("errorview");

    Map<String, Object> pmap = new HashMap<String, Object>();
    // deliberately missing contentid
    pmap.put(IPSHtmlParameters.SYS_REVISION, "1");
    pmap.put(IPSHtmlParameters.SYS_TEMPLATE, "301");
    pmap.put(IPSHtmlParameters.SYS_FOLDERID, "196");
    pmap.put(IPSHtmlParameters.SYS_SITEID, "102");
    StringLinkBackTokenImpl impl = new StringLinkBackTokenImpl();
    token_nocontentid = impl.encode(pmap);
  }

  @Test
  public final void testConstructorSeedsContentExplorerDefaults() {
    assertTrue(Modifier.isFinal(ContentExplorerLinkbackController.class.getModifiers()));
    assertEquals("/sys_cx/mainpage.html", cut.getRedirectPath());
    assertEquals(List.of(IPSHtmlParameters.SYS_CONTENTID), cut.getRequiredParameterNames());
  }

  @Test
  public final void testHandleRequestMissingContentId() {
    log.debug("Starting testing missing contentid");
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    request.addParameter(LinkbackUtils.LINKBACK_PARAM_NAME, token_nocontentid);
    request.setMethod("GET");

    try {
      ModelAndView mav = cut.handleRequest(request, response);
      assertNotNull(mav);
      assertEquals("errorview", mav.getViewName());
      log.debug(mav.getModel().get("message"));

    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception");
    }
  }
}

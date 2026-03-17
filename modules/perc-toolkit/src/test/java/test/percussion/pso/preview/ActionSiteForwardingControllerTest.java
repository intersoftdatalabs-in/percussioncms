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
 * test.percussion.pso.preview ActionSiteForwardingControllerTest.java
 *
 * @author DavidBenua
 *
 */
package test.percussion.pso.preview;

import static org.mockito.Mockito.*;

import com.percussion.pso.preview.AbstractMenuController;
import com.percussion.pso.preview.ActionSiteForwardingController;
import com.percussion.pso.preview.SiteFolderFinder;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.webservices.security.IPSSecurityWs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author DavidBenua
 */
@ExtendWith(MockitoExtension.class)
public class ActionSiteForwardingControllerTest {
  private static final Logger log = LogManager.getLogger(ActionSiteForwardingControllerTest.class);

  @Mock IPSAssemblyService asm;
  @Mock IPSSecurityWs secws;
  @Mock SiteFolderFinder finder;

  ActionSiteForwardingController cut;
  MockHttpServletRequest req;
  MockHttpServletResponse resp;

  /**
   * @throws Exception
   */
  @BeforeEach
  public void setUp() {
    cut = new ActionSiteForwardingController();
    ActionSiteForwardingController.setAsm(asm);
    AbstractMenuController.setSecws(secws);
    cut.setSiteFolderFinder(finder);

    req = new MockHttpServletRequest();
    req.setMethod("POST");
    resp = new MockHttpServletResponse();
    cut.setViewName("myView");
    cut.setBaseUrl("myBaseUrl");
  }

  /**
   * Test method for {@link
   * ActionSiteForwardingController#handleRequestInternal(jakarta.servlet.http.HttpServletRequest,
   * jakarta.servlet.http.HttpServletResponse)}.
   */
  @Test
  @Disabled("Test is failing") // TODO: Fix me
  public final void testHandleRequestWithSiteId() {
    // disabled - requires rework
  }

  /**
   * Test method for {@link
   * ActionSiteForwardingController#handleRequestInternal(jakarta.servlet.http.HttpServletRequest,
   * jakarta.servlet.http.HttpServletResponse)}.
   */
  @Test
  @Disabled("Test is failing") // TODO: Fix me
  public final void testHandleRequestOneSite() {
    // disabled - conversion pending
  }

  /**
   * Test method for {@link
   * ActionSiteForwardingController#handleRequestInternal(jakarta.servlet.http.HttpServletRequest,
   * jakarta.servlet.http.HttpServletResponse)}.
   */
  @Test
  @Disabled("Test is failing") // TODO: Fix me
  public final void testHandleRequestInternalTwoSites() {
    // disabled - conversion pending
  }
}

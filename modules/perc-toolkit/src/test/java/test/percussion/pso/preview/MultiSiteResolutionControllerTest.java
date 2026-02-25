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
 * test.percussion.pso.preview MultiSiteResolutionControllerTest.java
 *
 * @author DavidBenua
 *
 */
package test.percussion.pso.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.pso.preview.MultiSiteResolutionController;
import com.percussion.pso.preview.PreviewLocation;
import com.percussion.pso.preview.SiteFolderFinder;
import com.percussion.pso.preview.SiteFolderLocation;
import com.percussion.pso.preview.UrlBuilder;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author DavidBenua
 */
@ExtendWith(MockitoExtension.class)
public class MultiSiteResolutionControllerTest {
  private static final Logger log = LogManager.getLogger(MultiSiteResolutionControllerTest.class);
  MultiSiteResolutionController cut;
  MockHttpServletRequest req;
  MockHttpServletResponse resp;
  @Mock
  IPSGuidManager gmgr;
  @Mock
  IPSAssemblyService asm;
  @Mock
  SiteFolderFinder finder;
  @Mock
  UrlBuilder builder;

  /**
   * @throws Exception
   */
  @BeforeEach
  public void setUp() {
    cut = new MultiSiteResolutionController();
    req = new MockHttpServletRequest();
    req.setMethod("POST");
    resp = new MockHttpServletResponse();
    MultiSiteResolutionController.setGmgr(gmgr);
    MultiSiteResolutionController.setAsm(asm);
    cut.setSiteFolderFinder(finder);
    cut.setUrlBuilder(builder);
  }

  /**
   * Test method for {@link
   * MultiSiteResolutionController#handleRequestInternal(jakarta.servlet.http.HttpServletRequest,
   * jakarta.servlet.http.HttpServletResponse)}.
   */
  @Test
  @SuppressWarnings("unchecked")
  @Disabled("Test is failing") // TODO: Fix me
  public final void testHandleRequestInternalHttpServletRequestHttpServletResponse() {
    // disabled - needs rewrite with Mockito
  }
}

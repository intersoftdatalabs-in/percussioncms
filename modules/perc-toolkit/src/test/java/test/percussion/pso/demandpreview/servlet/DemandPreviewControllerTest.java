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

package test.percussion.pso.demandpreview.servlet;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSException;
import com.percussion.pso.demandpreview.service.DemandPublisherService;
import com.percussion.pso.demandpreview.service.ItemTemplateService;
import com.percussion.pso.demandpreview.service.LinkBuilderService;
import com.percussion.pso.demandpreview.service.SiteEditionHolder;
import com.percussion.pso.demandpreview.service.SiteEditionLookUpService;
import com.percussion.pso.demandpreview.servlet.DemandPreviewController;
import com.percussion.pso.utils.IPSOItemSummaryFinder;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.utils.guid.IPSGuid;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DemandPreviewControllerTest {

  private static final Logger log = LogManager.getLogger(DemandPreviewControllerTest.class);

  @Mock
  IPSGuidManager gmgr;
  @Mock
  DemandPublisherService demandSvc;
  @Mock
  LinkBuilderService linkBuilder;
  @Mock
  ItemTemplateService itemTemplate;
  @Mock
  SiteEditionLookUpService siteLookup;
  @Mock
  IPSOItemSummaryFinder isFinder;

  TestableDemandPreviewController cut;

  @BeforeEach
  public void setUp() {
    cut = new TestableDemandPreviewController();
    cut.setGmgr(gmgr);
    cut.setDemandPublisherService(demandSvc);
    cut.setLinkBuilderService(linkBuilder);
    cut.setItemTemplateService(itemTemplate);
    cut.setSiteEditionLookUpService(siteLookup);
    cut.setIsFinder(isFinder);
  }

  @Test
  @Disabled
  // TODO: rewrite test with Mockito once implementation is clarified
  public void testDoPublishForPreview() {
    // test currently disabled; conversion from JMock required
  }

  private class TestableDemandPreviewController extends DemandPreviewController {

    @Override
    public void setGmgr(IPSGuidManager gmgr) {
      super.setGmgr(gmgr);
    }

    @Override
    public String doPublishForPreview(String contentId, String folderId, String siteId)
        throws PSAssemblyException, TimeoutException, PSException {
      return super.doPublishForPreview(contentId, folderId, siteId);
    }

    @Override
    public void setIsFinder(IPSOItemSummaryFinder isFinder) {
      super.setIsFinder(isFinder);
    }
  }
}

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

package test.percussion.pso.demandpreview.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.pso.demandpreview.exception.SiteLookUpException;
import com.percussion.pso.demandpreview.service.impl.DefaultPageTemplateBean;
import com.percussion.pso.jexl.IPSOObjectFinder;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateService;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.utils.guid.IPSGuid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultPageTemplateBeanTest {
  private static final Logger log = LogManager.getLogger(DefaultPageTemplateBeanTest.class);
  TestableDefaultPageTemplateBean cut;
  @Mock IPSOObjectFinder objFinder;
  @Mock IPSTemplateService tempSvc;

  @BeforeEach
  public void setUp() throws Exception {
    cut = new TestableDefaultPageTemplateBean();
    cut.setObjFinder(objFinder);
    cut.setTempSvc(tempSvc);
  }

  @Test
  public void testFindTemplateSingle() {
    IPSAssemblyTemplate t1 = mock(IPSAssemblyTemplate.class);
    List<IPSAssemblyTemplate> tlist =
        new ArrayList<IPSAssemblyTemplate>() {
          {
            add(t1);
          }
        };
    IPSGuid t1ID = new PSGuid(PSTypeEnum.TEMPLATE, 1L);
    IPSGuid ctypeId = new PSGuid(PSTypeEnum.NODEDEF, 42L);
    // PSComponentSummary is final (this-escape #2700) — do not double-brace subclass.
    PSComponentSummary summ = new PSComponentSummary();
    summ.setContentTypeId(42L);
    IPSSite site = mock(IPSSite.class);
    IPSGuid contentId = mock(IPSGuid.class);

    log.info("Testing findTemplate");
    try {
      when(objFinder.getComponentSummary(contentId)).thenReturn(summ);
      when(t1.getGUID()).thenReturn(t1ID);
      when(site.getAssociatedTemplates()).thenReturn(Collections.singleton(t1));
      when(tempSvc.findTemplatesByContentType(ctypeId)).thenReturn(tlist);
      when(t1.getPublishWhen()).thenReturn(IPSAssemblyTemplate.PublishWhen.Default);
      when(t1.getOutputFormat()).thenReturn(IPSAssemblyTemplate.OutputFormat.Page);
      when(t1.getName()).thenReturn("TestTemplate1");

      IPSAssemblyTemplate result = cut.findTemplate(site, contentId);
      assertNotNull(result);
      assertEquals(t1, result);

      verify(objFinder).getComponentSummary(contentId);
      verify(t1, atLeastOnce()).getGUID();
      verify(site).getAssociatedTemplates();
      verify(tempSvc).findTemplatesByContentType(ctypeId);
      verify(t1).getPublishWhen();
      verify(t1).getOutputFormat();
      verify(t1, atLeastOnce()).getName();

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      fail("Exception caught");
    }
  }

  @Test
  public void testFindTemplateMulti() {
    IPSAssemblyTemplate t1 = mock(IPSAssemblyTemplate.class);
    IPSAssemblyTemplate t2 = mock(IPSAssemblyTemplate.class);
    IPSAssemblyTemplate t3 = mock(IPSAssemblyTemplate.class);
    List<IPSAssemblyTemplate> tlist =
        new ArrayList<IPSAssemblyTemplate>() {
          {
            add(t3);
            add(t2);
            add(t1);
          }
        };
    IPSGuid t1ID = new PSGuid(PSTypeEnum.TEMPLATE, 1L);
    IPSGuid t2ID = new PSGuid(PSTypeEnum.TEMPLATE, 2L);
    IPSGuid t3ID = new PSGuid(PSTypeEnum.TEMPLATE, 3L);
    IPSGuid ctypeId = new PSGuid(PSTypeEnum.NODEDEF, 42L);
    // PSComponentSummary is final (this-escape #2700) — do not double-brace subclass.
    PSComponentSummary summ = new PSComponentSummary();
    summ.setContentTypeId(42L);
    IPSSite site = mock(IPSSite.class);
    IPSGuid contentId = mock(IPSGuid.class);
    Set<IPSAssemblyTemplate> siteTemplates =
        new HashSet<IPSAssemblyTemplate>() {
          {
            add(t3);
            add(t1);
          }
        };

    log.info("Testing findTemplate Multiple");
    try {
      when(objFinder.getComponentSummary(contentId)).thenReturn(summ);
      when(t1.getGUID()).thenReturn(t1ID);
      when(site.getAssociatedTemplates()).thenReturn(siteTemplates);
      when(tempSvc.findTemplatesByContentType(ctypeId)).thenReturn(tlist);
      when(t1.getPublishWhen()).thenReturn(IPSAssemblyTemplate.PublishWhen.Default);
      when(t1.getOutputFormat()).thenReturn(IPSAssemblyTemplate.OutputFormat.Page);
      when(t1.getName()).thenReturn("TestTemplate1");
      when(t2.getPublishWhen()).thenReturn(IPSAssemblyTemplate.PublishWhen.Default);
      when(t2.getOutputFormat()).thenReturn(IPSAssemblyTemplate.OutputFormat.Page);
      when(t2.getName()).thenReturn("TestTemplate2");
      when(t2.getGUID()).thenReturn(t2ID);
      when(t3.getPublishWhen()).thenReturn(IPSAssemblyTemplate.PublishWhen.Never);
      when(t3.getOutputFormat()).thenReturn(IPSAssemblyTemplate.OutputFormat.Binary);
      when(t3.getName()).thenReturn("TestTemplate3");
      when(t3.getGUID()).thenReturn(t3ID);

      IPSAssemblyTemplate result = cut.findTemplate(site, contentId);
      assertNotNull(result);
      assertEquals(t1, result);

      verify(objFinder).getComponentSummary(contentId);
      verify(t1, atLeastOnce()).getGUID();
      verify(site).getAssociatedTemplates();
      verify(tempSvc).findTemplatesByContentType(ctypeId);
      verify(t1).getPublishWhen();
      verify(t1).getOutputFormat();
      verify(t1, atLeastOnce()).getName();
      verify(t2).getPublishWhen();
      verify(t2).getOutputFormat();
      verify(t2, atLeastOnce()).getName();
      verify(t2, atLeastOnce()).getGUID();
      verify(t3).getPublishWhen();
      // do not verify outputFormat on t3 because it's never checked when PublishWhen.Never
      verify(t3, atLeastOnce()).getGUID();

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      fail("Exception caught");
    }
  }

  @Test
  public void testFindTemplateNotFound() {
    IPSAssemblyTemplate t1 = mock(IPSAssemblyTemplate.class);
    IPSAssemblyTemplate t2 = mock(IPSAssemblyTemplate.class);
    IPSAssemblyTemplate t3 = mock(IPSAssemblyTemplate.class);
    List<IPSAssemblyTemplate> tlist =
        new ArrayList<IPSAssemblyTemplate>() {
          {
            add(t3);
            add(t2);
            add(t1);
          }
        };
    IPSGuid t1ID = new PSGuid(PSTypeEnum.TEMPLATE, 1L);
    IPSGuid t2ID = new PSGuid(PSTypeEnum.TEMPLATE, 2L);
    IPSGuid t3ID = new PSGuid(PSTypeEnum.TEMPLATE, 3L);
    IPSGuid ctypeId = new PSGuid(PSTypeEnum.NODEDEF, 42L);
    // PSComponentSummary is final (this-escape #2700) — do not double-brace subclass.
    PSComponentSummary summ = new PSComponentSummary();
    summ.setContentTypeId(42L);
    IPSSite site = mock(IPSSite.class);
    IPSGuid contentId = mock(IPSGuid.class);
    Set<IPSAssemblyTemplate> siteTemplates =
        new HashSet<IPSAssemblyTemplate>() {
          {
            add(t3);
            add(t1);
          }
        };

    log.info("Testing findTemplate Not Found");
    try {
      when(objFinder.getComponentSummary(contentId)).thenReturn(summ);
      when(t1.getGUID()).thenReturn(t1ID);
      when(site.getAssociatedTemplates()).thenReturn(siteTemplates);
      when(tempSvc.findTemplatesByContentType(ctypeId)).thenReturn(tlist);
      when(t1.getPublishWhen()).thenReturn(IPSAssemblyTemplate.PublishWhen.Default);
      when(t1.getOutputFormat()).thenReturn(IPSAssemblyTemplate.OutputFormat.Snippet);
      when(t1.getName()).thenReturn("TestTemplate1");
      when(t2.getPublishWhen()).thenReturn(IPSAssemblyTemplate.PublishWhen.Default);
      when(t2.getOutputFormat()).thenReturn(IPSAssemblyTemplate.OutputFormat.Page);
      when(t2.getName()).thenReturn("TestTemplate2");
      when(t2.getGUID()).thenReturn(t2ID);
      when(t3.getPublishWhen()).thenReturn(IPSAssemblyTemplate.PublishWhen.Never);
      when(t3.getOutputFormat()).thenReturn(IPSAssemblyTemplate.OutputFormat.Binary);
      when(t3.getName()).thenReturn("TestTemplate3");
      when(t3.getGUID()).thenReturn(t3ID);
      when(site.getName()).thenReturn("Mock Site");

      assertThrows(SiteLookUpException.class, () -> cut.findTemplate(site, contentId));

      verify(objFinder).getComponentSummary(contentId);
      verify(t1, atLeastOnce()).getGUID();
      verify(site).getAssociatedTemplates();
      verify(tempSvc).findTemplatesByContentType(ctypeId);
      verify(t1).getPublishWhen();
      verify(t1).getOutputFormat();
      verify(t1, atLeastOnce()).getName();
      verify(t2).getPublishWhen();
      verify(t2).getOutputFormat();
      verify(t2, atLeastOnce()).getName();
      verify(t2, atLeastOnce()).getGUID();
      verify(t3).getPublishWhen();
      // outputFormat not invoked for t3 when publishWhen=Nev er
      verify(t3, atLeastOnce()).getName();
      verify(t3, atLeastOnce()).getGUID();
      verify(site).getName();

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      fail("Exception caught");
    }
  }

  private class TestableDefaultPageTemplateBean extends DefaultPageTemplateBean {

    @Override
    public void setObjFinder(IPSOObjectFinder objFinder) {
      super.setObjFinder(objFinder);
    }

    @Override
    public void setTempSvc(IPSTemplateService tempSvc) {
      super.setTempSvc(tempSvc);
    }
  }
}

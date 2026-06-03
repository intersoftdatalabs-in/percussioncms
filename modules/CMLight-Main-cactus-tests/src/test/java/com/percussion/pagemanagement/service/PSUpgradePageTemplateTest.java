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

package com.percussion.pagemanagement.service;

import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSTemplateDao;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRegionWidgets;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.services.content.data.PSItemStatus;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.test.PSServletTestCase;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


class PSUpgradePageTemplateTest extends PSServletTestCase {

  private final Map<String, String> widgetMap = new HashMap<>();

  private IPSPageService pageService;
  private IPSIdMapper idMapper;
  private IPSSystemWs systemWs;
  private IPSPageDao pageDao;
  private IPSTemplateDao templateDao;
  private IPSSecurityWs securityWs;
  private IPSContentWs contentWs;

  @BeforeEach
  void setUpTest() throws Exception {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
    securityWs.login("Admin", "demo", "Default", null);

    widgetMap.put("event", "percEvent");
    widgetMap.put("file", "percFile");
    widgetMap.put("image", "percImage");
    widgetMap.put("navBar", "percNavBar");
    widgetMap.put("navBreadcrumb", "percNavBreadcrumb");
    widgetMap.put("PageAutoList", "percPageAutoList");
    widgetMap.put("PSWidget_RawHtml", "percRawHtml");
    widgetMap.put("PSWidget_RichText", "percRichText");
    widgetMap.put("PSWidget_SimpleText", "percSimpleText");
    super.setUp();
  }

  @Test
  void testUpdateAllTemplates() throws Exception {
    var updateTemplates = new ArrayList<PSTemplate>();
    for (var template : templateDao.findAll()) {
      var widgets = template.getRegionTree().getRegionWidgetAssociations();
      if (needToResetWidgets(widgets)) {
        updateTemplates.add(template);
      }
    }
    for (var template : updateTemplates) {
      templateDao.save(template);
      System.out.println("Updated page \"" + template.getName() + "\"");
    }
  }

  @Test
  void testUpdateAllPages() throws Exception {
    var updatePages = new ArrayList<PSPage>();
    for (var page : pageDao.findAll()) {
      var branches = page.getRegionBranches();
      var widgets = branches.getRegionWidgetAssociations();
      if (needToResetWidgets(widgets)) {
        updatePages.add(page);
      }
    }
    for (var page : updatePages) {
      IPSGuid id = idMapper.getGuid(page.getId());
      PSItemStatus status = contentWs.prepareForEdit(id);
      pageDao.save(page);
      contentWs.releaseFromEdit(status, false);
      System.out.println("Updated page \"" + page.getName() + "\"");
    }
  }

  private boolean needToResetWidgets(Set<PSRegionWidgets> widgets) {
    var isUpdated = false;
    for (var ws : widgets) {
      for (var w : ws.getWidgetItems()) {
        var oldName = w.getDefinitionId();
        var newName = widgetMap.get(oldName);
        if (newName != null) {
          isUpdated = true;
          w.setDefinitionId(newName);
          System.out.println("Change widget definition: " + oldName + " -> " + newName);
        }
      }
    }
    return isUpdated;
  }

  public IPSPageService getPageService() {
    return pageService;
  }

  public void setPageService(IPSPageService pageService) {
    this.pageService = pageService;
  }

  public IPSIdMapper getIdMapper() {
    return idMapper;
  }

  public void setIdMapper(IPSIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  public IPSSystemWs getSystemWs() {
    return systemWs;
  }

  public void setSystemWs(IPSSystemWs systemWs) {
    this.systemWs = systemWs;
  }

  public IPSTemplateDao getTemplateDao() {
    return templateDao;
  }

  public void setTemplateDao(IPSTemplateDao templateDao) {
    this.templateDao = templateDao;
  }

  public IPSPageDao getPageDao() {
    return pageDao;
  }

  public void setPageDao(IPSPageDao pageDao) {
    this.pageDao = pageDao;
  }

  public IPSSecurityWs getSecurityWs() {
    return securityWs;
  }

  public void setSecurityWs(IPSSecurityWs securityWs) {
    this.securityWs = securityWs;
  }

  public IPSContentWs getContentWs() {
    return contentWs;
  }

  public void setContentWs(IPSContentWs contentWs) {
    this.contentWs = contentWs;
  }
}

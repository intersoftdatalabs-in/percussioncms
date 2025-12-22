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

import static com.percussion.share.test.PSRestTestCase.baseUrl;
import static com.percussion.share.test.PSRestTestCase.setupClient;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.apache.commons.lang3.Validate.notEmpty;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.web.service.PSAssetServiceRestClient;
import com.percussion.itemmanagement.web.service.PSItemWorkflowServiceRestClient;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.pathmanagement.web.service.PSPathServiceRestClient;
import com.percussion.share.service.PSAsyncJobStatusRestClient;
import com.percussion.share.test.PSTestDataCleaner;
import com.percussion.sitemanage.data.PSCreateSiteSection;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteSection;
import com.percussion.sitemanage.data.PSUpdateSectionLink;
import com.percussion.sitemanage.service.AssignTemplate;
import com.percussion.sitemanage.service.PSSiteTemplates;
import com.percussion.sitemanage.service.PSSiteTemplates.CreateTemplate;
import com.percussion.sitemanage.web.service.PSSiteRestClient;
import com.percussion.sitemanage.web.service.PSSiteSectionRestClient;
import com.percussion.sitemanage.web.service.PSSiteTemplateRestClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Sets up two test sites and two test template items for integration testing.
 *
 * <p>Sunny Sal says: "A good test setup is like a good pizza base—everything else is just
 * toppings!"
 */
public class PSTestSiteData {

  private PSSiteTemplateRestClient siteTemplateRestClient;
  private PSTemplateServiceClient templateServiceClient;
  private PSPageRestClient pageRestClient;
  private PSSiteRestClient siteRestClient;
  private PSAssetServiceRestClient assetRestClient;
  private PSSiteSectionRestClient sectionClient;
  private PSPathServiceRestClient pathClient;
  private PSItemWorkflowServiceRestClient workflowClient;
  private List<PSTemplateSummary> siteTemplates;
  private PSRenderServiceClient renderServiceClient;
  private PSAsyncJobStatusRestClient asyncJobStatusRestClient;

  public PSSite site1;
  public PSSite site2;
  public String baseTemplateId;
  public PSTemplateSummary template1;
  public PSTemplateSummary template2;

  public static final String TEST_WIDGET_DEFINITION = "PSWidget_TestProperties";

  private static final Logger log = LogManager.getLogger(PSTestSiteData.class);

  // --- Data Cleaners ---

  public final PSTestDataCleaner<String> siteCleaner = new SiteCleaner();
  public final PSTestDataCleaner<String> templateCleaner = new TemplateCleaner();
  public final PSTestDataCleaner<String> pageCleaner = new PageCleaner();
  public final PSTestDataCleaner<String> assetCleaner = new AssetCleaner();
  public final PSTestDataCleaner<String> sectionCleaner = new SectionCleaner();

  private class SiteCleaner extends PSTestDataCleaner<String> {
    @Override
    protected void clean(String name) throws Exception {
      siteRestClient.delete(name);
    }
  }

  private class TemplateCleaner extends PSTestDataCleaner<String> {
    @Override
    protected void clean(String name) throws Exception {
      var id = templateNameToId(name);
      if (id != null) {
        templateServiceClient.deleteTemplate(id);
      }
    }

    private String templateNameToId(String name) {
      return templateServiceClient.findAll().stream()
          .filter(sum -> ObjectUtils.equals(sum.getName(), name))
          .map(PSTemplateSummary::getId)
          .findFirst()
          .orElse(null);
    }
  }

  private class PageCleaner extends PSTestDataCleaner<String> {
    @Override
    protected void clean(String folderPath) throws Exception {
      var page = pageRestClient.findPageByFullFolderPath(folderPath);
      pageRestClient.delete(page.getId());
    }
  }

  private class AssetCleaner extends PSTestDataCleaner<String> {
    @Override
    protected void clean(String id) throws Exception {
      assetRestClient.delete(id);
    }
  }

  private class SectionCleaner extends PSTestDataCleaner<String> {
    @Override
    protected void clean(String sectionId) throws Exception {
      sectionClient.delete(sectionId);
    }

    @Override
    protected List<String> getDataIds() {
      var ids = new ArrayList<>(super.getDataIds());
      Collections.reverse(ids);
      return ids;
    }
  }

  // --- Setup and Teardown ---

  public void setUpClients() throws Exception {
    siteTemplateRestClient = new PSSiteTemplateRestClient();
    templateServiceClient = new PSTemplateServiceClient(baseUrl);
    siteRestClient = new PSSiteRestClient(baseUrl);
    pageRestClient = new PSPageRestClient(baseUrl);
    assetRestClient = new PSAssetServiceRestClient(baseUrl);
    sectionClient = new PSSiteSectionRestClient(baseUrl);
    pathClient = new PSPathServiceRestClient(baseUrl);
    workflowClient = new PSItemWorkflowServiceRestClient(baseUrl);
    renderServiceClient = new PSRenderServiceClient();
    asyncJobStatusRestClient = new PSAsyncJobStatusRestClient();

    setupClient(siteTemplateRestClient);
    setupClient(templateServiceClient);
    setupClient(siteRestClient);
    setupClient(pageRestClient);
    setupClient(assetRestClient);
    setupClient(sectionClient);
    setupClient(pathClient);
    setupClient(workflowClient);
    setupClient(renderServiceClient);
    setupClient(asyncJobStatusRestClient);
  }

  public void setUp() throws Exception {
    log.info("!!!!!!!!!!!!!!  Started Setup  !!!!!!!!!!!!!");
    setUpClients();
    var sum = templateServiceClient.findAllReadOnly().get(0);
    baseTemplateId = sum.getId();
    var t1 = "SiteTemplateServiceTest1" + System.currentTimeMillis();
    var t2 = "SiteTemplateServiceTest2" + System.currentTimeMillis();

    site1 = createSite(t1, sum.getName());
    template1 = siteTemplateRestClient.findTemplatesBySite(site1.getId()).get(0);
    log.debug("Created template: {}", template1);
    site2 = createSite(t2, sum.getName());
    template2 = siteTemplateRestClient.findTemplatesBySite(site2.getId()).get(0);
    log.debug("Created template: {}", template2);

    var s = new PSSiteTemplates();
    var a1 = new AssignTemplate();
    var a2 = new AssignTemplate();

    a1.setTemplateId(template1.getId());
    a1.setSiteIds(asList(site1.getName()));
    a2.setTemplateId(template2.getId());
    a2.setSiteIds(asList(site2.getName()));

    var c1 = new CreateTemplate();
    c1.setName("SiteTemplateServiceCreated1");
    c1.setSiteIds(asList(site1.getName()));
    c1.setSourceTemplateId(sum.getId());

    s.setAssignTemplates(asList(a1, a2));
    s.setCreateTemplates(asList(c1));

    var templates = siteTemplateRestClient.save(s);

    var json = siteTemplateRestClient.objectToJson(s);
    log.debug("JSON of site templates: {}", json);
    log.debug("Saved templates: {}", templates);
    assertEquals("Number of templates", 3, templates.size());

    this.siteTemplates = templates;
    log.info("!!!!!!!!!!!!!!  Finished Setup  !!!!!!!!!!!!!");
  }

  public void tearDown() throws Exception {
    log.info("!!!!!!!!!!!!!!  Started Tear Down  !!!!!!!!!!!!!");
    setUpClients();
    PSTestDataCleaner.runCleaners(
        sectionCleaner, pageCleaner, siteCleaner, templateCleaner, assetCleaner);
    log.info("!!!!!!!!!!!!!!  Finished Tear Down  !!!!!!!!!!!!!");
  }

  // --- Utility Methods ---

  public PSTemplateSummary createTemplate(String name) {
    templateCleaner.add(name);
    return templateServiceClient.createTemplate(name, baseTemplateId);
  }

  public PSWidgetItem createWidgetItem(String name, String definition) {
    var widgetItem = new PSWidgetItem();
    widgetItem.setName(name);
    widgetItem.setDefinitionId(definition);
    return widgetItem;
  }

  public List<PSTemplateSummary> assignTemplatesToSite(String siteId, String... templateIds) {
    var s = new PSSiteTemplates();
    var assigns = new ArrayList<AssignTemplate>();
    for (var tid : templateIds) {
      var a = new AssignTemplate();
      a.setSiteIds(asList(siteId));
      a.setTemplateId(tid);
      assigns.add(a);
    }
    s.setAssignTemplates(assigns);
    return siteTemplateRestClient.save(s);
  }

  public PSSite createSite(String name, String baseTemplateName) {
    siteCleaner.add(name);
    var site = new PSSite();
    site.setName(name);
    site.setLabel("My test site");
    site.setHomePageTitle("homePageTitle");
    site.setNavigationTitle("navigationTitle");
    site.setBaseTemplateName(baseTemplateName);
    site.setTemplateName(baseTemplateName + System.currentTimeMillis());
    return siteRestClient.save(site);
  }

  public String createPage(String name, String folderPath, String templateId) throws Exception {
    notEmpty(templateId);
    var pageNew = new PSPage();
    pageNew.setName(name);
    pageNew.setTitle(name);
    pageNew.setFolderPath(folderPath);
    pageNew.setTemplateId(templateId);
    pageNew.setLinkTitle("dummy");
    var r = pageRestClient.save(pageNew);
    var fullPath = folderPath + "/" + name;
    pageCleaner.add(fullPath);
    return r.getId();
  }

  public PSAsset saveAsset(PSAsset asset) {
    var rvalue = assetRestClient.save(asset);
    assetCleaner.add(rvalue.getId());
    return rvalue;
  }

  public PSSiteSection createSection(PSCreateSiteSection req) {
    var section = sectionClient.create(req);
    sectionCleaner.add(section.getId());
    return section;
  }

  public PSSiteSection createSectionLink(String targetSectionGuid, String parentSectionGuid) {
    return sectionClient.createSectionLink(targetSectionGuid, parentSectionGuid);
  }

  /**
   * Removes the site section from the clean up list if already deleted by the unit test.
   *
   * @param section the section that does not need to be cleaned up, not null.
   */
  public void removeSectionFromCleaner(PSSiteSection section) {
    sectionCleaner.remove(section.getId());
  }

  // --- Getters and Setters ---

  public PSSiteSectionRestClient getSectionClient() {
    return sectionClient;
  }

  public PSItemWorkflowServiceRestClient getWorkflowClient() {
    return workflowClient;
  }

  public PSPathServiceRestClient getPathRestClient() {
    return pathClient;
  }

  public PSAssetServiceRestClient getAssetRestClient() {
    return assetRestClient;
  }

  public void setAssetRestClient(PSAssetServiceRestClient assetRestClient) {
    this.assetRestClient = assetRestClient;
  }

  public PSSiteTemplateRestClient getSiteTemplateRestClient() {
    return siteTemplateRestClient;
  }

  public void setSiteTemplateRestClient(PSSiteTemplateRestClient siteTemplateRestClient) {
    this.siteTemplateRestClient = siteTemplateRestClient;
  }

  public PSTemplateServiceClient getTemplateServiceClient() {
    return templateServiceClient;
  }

  public void setTemplateServiceClient(PSTemplateServiceClient templateServiceClient) {
    this.templateServiceClient = templateServiceClient;
  }

  public PSSiteRestClient getSiteRestClient() {
    return siteRestClient;
  }

  public void setSiteRestClient(PSSiteRestClient siteRestClient) {
    this.siteRestClient = siteRestClient;
  }

  public PSRenderServiceClient getRenderServiceClient() {
    return renderServiceClient;
  }

  public void setRenderServiceClient(PSRenderServiceClient renderServiceClient) {
    this.renderServiceClient = renderServiceClient;
  }

  public List<PSTemplateSummary> getSiteTemplates() {
    return siteTemplates;
  }

  public void setSiteTemplates(List<PSTemplateSummary> siteTemplates) {
    this.siteTemplates = siteTemplates;
  }

  public PSAsyncJobStatusRestClient getAsyncJobStatusRestClient() {
    return asyncJobStatusRestClient;
  }

  public void setAsyncJobStatusRestClient(PSAsyncJobStatusRestClient asyncJobStatusRestClient) {
    this.asyncJobStatusRestClient = asyncJobStatusRestClient;
  }

  public PSPageRestClient getPageRestClient() {
    return pageRestClient;
  }

  public void setPageRestClient(PSPageRestClient pageRestClient) {
    this.pageRestClient = pageRestClient;
  }

  public PSTestDataCleaner<String> getSiteCleaner() {
    return siteCleaner;
  }

  public PSTestDataCleaner<String> getTemplateCleaner() {
    return templateCleaner;
  }

  public PSTestDataCleaner<String> getPageCleaner() {
    return pageCleaner;
  }

  public PSTestDataCleaner<String> getSectionCleaner() {
    return sectionCleaner;
  }

  public PSTestDataCleaner<String> getAssetCleaner() {
    return assetCleaner;
  }

  /**
   * Updates a section link and returns the result of the update.
   *
   * @param updateRequest {@link PSUpdateSectionLink} request, assumed not null.
   * @return {@link PSSiteSection} never null.
   */
  public PSSiteSection updateSectionLink(PSUpdateSectionLink updateRequest) {
    return sectionClient.updateSectionLink(updateRequest);
  }
}

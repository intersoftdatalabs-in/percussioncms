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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSAssetWidgetRelationship;
import com.percussion.assetmanagement.data.PSAssetWidgetRelationship.PSAssetResourceType;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRegionTree;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplate.PSTemplateTypeEnum;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.share.service.IPSDataService.DataServiceSaveException;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.test.PSServletTestCase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


class PSTemplateServiceTest extends PSServletTestCase {

  private IPSTemplateService templateService;
  private PSSiteDataServletTestCaseFixture fixture;
  private IPSAssetService assetService;
  private IPSSystemWs systemWs;
  private IPSIdMapper idMapper;
  private IPSPageDaoHelper pageDaoHelper;
  private IPSContentDesignWs contentDesignWs;
  private IPSItemWorkflowService itemWorkflowService;

  // ...getters and setters unchanged...

  @BeforeEach
  void setUpTest() throws Exception {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
    fixture = new PSSiteDataServletTestCaseFixture(request, response);
    fixture.setUp();
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    fixture.tearDown();
  }

  @Test
  void testFindTemplates() throws Exception {
    var srv = getTemplateService();
    var preAllBaseNum = srv.findBaseTemplates("base").size();
    var summaries = srv.findBaseTemplates("base");
    assertEquals(preAllBaseNum, summaries.size(), "This is no Base Template in current community");
    fixture.createBasePageTemplate(TEMPLATE_NAME_PREFIX + "_1");
    summaries = srv.findBaseTemplates("base");
    assertEquals(1 + preAllBaseNum, summaries.size());
    fixture.createBasePageTemplate(TEMPLATE_NAME_PREFIX + "_2");
    summaries = srv.findBaseTemplates("base");
    assertEquals(2 + preAllBaseNum, summaries.size());
  }

  @Test
  void testCreateTypedTemplate() throws Exception {
    var srv = getTemplateService();
    var templateName = "templateUnassigned";
    var templateSummary =
        srv.createTemplate(
            templateName,
            fixture.baseTemplateId,
            fixture.site1.getId(),
            PSTemplateTypeEnum.UNASSIGNED);
    assertNotNull(templateSummary);
    var template = templateService.load(templateSummary.getId());
    assertEquals(templateName, template.getName());
    assertEquals(PSTemplateTypeEnum.UNASSIGNED.getLabel(), template.getType());
  }

  @Test
  void testLoadUnassignedTemplate() throws Exception {
    var srv = getTemplateService();
    var templateName = "Template1";
    var templateSummary = fixture.createTemplate(templateName);
    var item = templateService.load(templateSummary.getId());
    item.setType(PSTemplateTypeEnum.UNASSIGNED.getLabel());
    srv.save(item);
    item = templateService.load(templateSummary.getId());
    assertEquals(templateName, item.getName());
    assertEquals(PSTemplateTypeEnum.UNASSIGNED.getLabel(), item.getType());
  }

  @Test
  void testFindUserTemplatesByType() throws Exception {
    var srv = getTemplateService();
    var preAllNum = srv.findAll().size();
    var preUserNum = srv.findAllUserTemplates().size();
    var templateName = "Template1";
    var template2Name = "Template2";
    var template3Name = "Template3";
    var template4Name = "Template4";
    var template5Name = "Template5";
    var templateSummary = fixture.createTemplate(templateName);
    var item = templateService.load(templateSummary.getId());
    item.setType(PSTemplateTypeEnum.NORMAL.getLabel());
    srv.save(item);
    var templateSummary2 = fixture.createTemplate(template2Name);
    var item2 = srv.load(templateSummary2.getId());
    item2.setType(PSTemplateTypeEnum.NORMAL.getLabel());
    srv.save(item2);
    var templateSummary3 = fixture.createTemplate(template3Name);
    var item3 = templateService.load(templateSummary3.getId());
    item3.setType(PSTemplateTypeEnum.UNASSIGNED.getLabel());
    srv.save(item3);
    var templateSummary4 = fixture.createTemplate(template4Name);
    var item4 = templateService.load(templateSummary4.getId());
    item4.setType(PSTemplateTypeEnum.NORMAL.getLabel());
    srv.save(item4);
    fixture.createTemplate(template5Name);
    var userTemplates = srv.findAllUserTemplates();
    var allTemplates = srv.findAll();
    assertEquals(
        preUserNum + 4, userTemplates.size(), "4 new user templates (1 untyped, 3 types normal): ");
    assertEquals(
        preAllNum + 4,
        allTemplates.size(),
        "5 new total templates (1 untyped, 3 types normal, 1 unassigned): ");
  }

  @Test
  void testFindPageIdsByTemplateInRecentRevision_usingPagination() {
    var fakeIds = new ArrayList<Integer>();
    for (int i = 0; i < 1500; i++) {
      fakeIds.add(i);
    }
    try {
      getPageDaoHelper().findTemplateUsedByCurrentRevisionOfPages(fakeIds);
    } catch (Exception e) {
      fail("The query failed for 1500 ids.");
    }
  }

  @Test
  void testTemplateItem() throws Exception {
    PSTemplateSummary sum1 = null;
    PSTemplateSummary sum2 = null;
    var preAllNum = templateService.findAll().size();
    var preSystemNum = templateService.findBaseTemplates("base").size();
    var preUserNum = templateService.findAllUserTemplates().size();
    sum1 = fixture.createTemplateWithSite("testTemplateItem_1", fixture.site1.getId());
    var item = templateService.load(sum1.getId());
    assertNotNull(item, "Find created item");
    assertTrue(StringUtils.isNotBlank(item.getCssRegion()));
    var asset = new PSAsset();
    asset.getFields().put("sys_title", "SharedAsset");
    asset.setFolderPaths(asList("//Folders/Assets"));
    asset.setType("percRawHtmlAsset");
    asset.getFields().put("html", "TestHTML");
    asset = assetService.save(asset);
    fixture.assetCleaner.add(asset.getId());
    assertNotNull(asset);
    var assetId = asset.getId();
    assertNotNull(assetId);
    var awRel = new PSAssetWidgetRelationship(sum1.getId(), 5, "widget5", assetId, 1);
    awRel.setResourceType(PSAssetResourceType.shared);
    assetService.createAssetWidgetRelationship(awRel);
    sum2 = fixture.createTemplateFromTemplate("testTemplateItem_2", sum1.getId());
    assertEquals(preAllNum + 2, templateService.findAll().size(), "should have one more template");
    assertEquals(
        preUserNum + 2,
        templateService.findAllUserTemplates().size(),
        "should have one more template");
    assertEquals(
        preSystemNum,
        templateService.findBaseTemplates("base").size(),
        "no change on number of system templates");
    var filter = new PSRelationshipFilter();
    filter.limitToOwnerRevision(true);
    filter.setName(PSWidgetAssetRelationshipService.SHARED_ASSET_WIDGET_REL_FILTER);
    filter.setOwner(idMapper.getLocator(sum2.getId()));
    assertEquals(1, systemWs.loadRelationships(filter).size());
    var temp2 = templateService.load(sum2.getId());
    templateService.save(temp2);
    assertTrue(systemWs.loadRelationships(filter).isEmpty());
    var itemSum = templateService.find(sum1.getId());
    assertNotNull(itemSum);
    templateService.delete(sum1.getId());
    var sum = templateService.find(sum1.getId());
    assertNull(sum, "Should not find deleted item");
    fixture.templateCleaner.remove(sum1.getName());
    templateService.delete(sum2.getId());
    sum = templateService.find(sum2.getId());
    assertNull(sum, "Should not find deleted item_2");
    fixture.templateCleaner.remove(sum2.getName());
    assertEquals(preAllNum, templateService.findAll().size(), "should have one more template");
    assertEquals(
        preUserNum, templateService.findAllUserTemplates().size(), "should have one more template");
  }

  @Test
  void testExportTemplate() throws Exception {
    PSTemplate templateToExport = null;
    PSTemplateSummary sum1 = null;
    sum1 = fixture.createTemplateWithSite("testTemplateItem_1", fixture.site1.getId());
    var item = templateService.load(sum1.getId());
    assertNotNull(item, "Find created item");
    assertTrue(StringUtils.isNotBlank(item.getName()));
    assertTrue(StringUtils.isNotBlank(item.getCssRegion()));
    templateToExport = templateService.exportTemplate(item.getId(), item.getName());
    assertNotNull(templateToExport, "Template to export");
    assertTrue(StringUtils.isNotBlank(templateToExport.getName()));
    assertTrue(StringUtils.isNotBlank(templateToExport.getCssRegion()));
    assertNull(templateToExport.getId(), "Should not find template ID");
    templateService.delete(sum1.getId());
    var sum = templateService.find(sum1.getId());
    assertNull(sum, "Should not find deleted item");
    fixture.templateCleaner.remove(sum1.getName());
  }

  // fixme_testImportTemplate intentionally left as is (not migrated to JUnit5)

  @Test
  void testGetWidgets() throws Exception {
    var sum1 = fixture.createTemplateWithSite("testTemplateItem_1", fixture.site1.getId());
    var item = templateService.load(sum1.getId());
    assertEquals(0, item.getWidgets().size());
    var region = new PSRegion();
    region.setRegionId("region");
    var widgets = new ArrayList<PSWidgetItem>();
    var widget1 = new PSWidgetItem();
    widget1.setDefinitionId("widget1");
    widgets.add(widget1);
    var widget2 = new PSWidgetItem();
    widget2.setDefinitionId("widget2");
    widgets.add(widget2);
    var regTree = new PSRegionTree();
    regTree.setRegionWidgets(region.getRegionId(), widgets);
    item.setRegionTree(regTree);
    assertEquals(2, item.getWidgets().size());
  }

  @Test
  void testHasWidget() throws Exception {
    var sum1 = fixture.createTemplateWithSite("testTemplateItem_1", fixture.site1.getId());
    var item = templateService.load(sum1.getId());
    assertFalse(item.hasWidget("widget1"));
    assertFalse(item.hasWidget("widget2"));
    var region = new PSRegion();
    region.setRegionId("region");
    var widgets = new ArrayList<PSWidgetItem>();
    var widget1 = new PSWidgetItem();
    widget1.setDefinitionId("widget1");
    widgets.add(widget1);
    var widget2 = new PSWidgetItem();
    widget2.setDefinitionId("widget2");
    widgets.add(widget2);
    var regTree = new PSRegionTree();
    regTree.setRegionWidgets(region.getRegionId(), widgets);
    item.setRegionTree(regTree);
    assertTrue(item.hasWidget("widget1"));
    assertTrue(item.hasWidget("widget2"));
  }

  @Test
  void testUpdateTemplateVersion() throws Exception {
    var sum1 = fixture.createTemplateWithSite("testTemplateVersion", fixture.site1.getId());
    assertEquals("0", sum1.getContentMigrationVersion());
    var item = templateService.load(sum1.getId());
    assertEquals("0", item.getContentMigrationVersion());
    var name = "page1";
    var page1 = createPage(sum1, name);
    assertNotNull(page1);
    fixture.pageCleaner.add(page1.getId());
    item = templateService.save(item);
    assertEquals("0", item.getContentMigrationVersion());
    itemWorkflowService.checkIn(page1.getId());
    boolean didThrow = false;
    try {
      templateService.save(item, null, page1.getId());
    } catch (DataServiceSaveException e) {
      didThrow = true;
    }
    assertTrue(didThrow);
    itemWorkflowService.checkOut(page1.getId());
    item = templateService.save(item, null, page1.getId());
    assertEquals("1", item.getContentMigrationVersion());
    item = templateService.save(item);
    assertEquals("1", item.getContentMigrationVersion());
    item = templateService.save(item, null, page1.getId());
    assertEquals("2", item.getContentMigrationVersion());
    fixture.getPageService().delete(page1.getId());
    try {
      templateService.save(item, null, page1.getId());
    } catch (DataServiceSaveException e) {
      didThrow = true;
    }
  }

  private PSPage createPage(PSTemplateSummary sum1, String name) throws PSDataServiceException {
    var page = new PSPage();
    page.setFolderPath(fixture.site1.getFolderPath());
    page.setName(name);
    page.setTitle(name);
    page.setTemplateId(sum1.getId());
    page.setLinkTitle(name);
    page.setNoindex("true");
    page.setDescription(name);
    return fixture.createPage(page);
  }

  public static final String TEMPLATE_NAME_PREFIX = "perc.base.testPageTemplate";
}

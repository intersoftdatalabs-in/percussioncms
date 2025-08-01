/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.sitemanage.service;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSAssetWidgetRelationship;
import com.percussion.assetmanagement.data.PSAssetWidgetRelationship.PSAssetResourceType;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.data.PSTemplate.PSTemplateTypeEnum;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture;
import com.percussion.share.IPSSitemanageConstants;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.webservices.system.IPSSystemWs;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration tests for site template service.
 * // REFACTORED: CP-JAVA11
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSSiteTemplateServiceTest {

    private IPSTemplateService templateService;
    private PSSiteDataServletTestCaseFixture fixture;
    private IPSAssetService assetService;
    private IPSSystemWs systemWs;
    private IPSIdMapper idMapper;
    private IPSSiteTemplateService siteTemplateService;
    private IPSSiteDataService siteDataService;

    public IPSSiteDataService getSiteDataService() {
        return siteDataService;
    }

    public void setSiteDataService(IPSSiteDataService siteDataService) {
        this.siteDataService = siteDataService;
    }

    public IPSSiteTemplateService getSiteTemplateService() {
        return siteTemplateService;
    }

    public void setSiteTemplateService(IPSSiteTemplateService siteTemplateService) {
        this.siteTemplateService = siteTemplateService;
    }

    public IPSTemplateService getTemplateService() {
        return templateService;
    }

    public void setTemplateService(IPSTemplateService templateService) {
        this.templateService = templateService;
    }

    public IPSAssetService getAssetService() {
        return assetService;
    }

    public void setAssetService(IPSAssetService assetService) {
        this.assetService = assetService;
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

    @BeforeEach
    public void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        fixture = new PSSiteDataServletTestCaseFixture(request, response);
        fixture.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        fixture.tearDown();
    }

    @Test
    public void testFindTemplatesBySiteAndType() throws Exception {
        PSTemplateSummary sum1;
        PSTemplateSummary sum2;
        PSTemplateSummary sum3;

        var site1Id = fixture.site1.getId();
        var nonTypedAmount = siteTemplateService.findTemplatesBySite(site1Id).size();

        sum1 = fixture.createTemplateWithSite("testTemplateItem_1", site1Id);
        var item = templateService.load(sum1.getId());
        item.setType(PSTemplateTypeEnum.NORMAL.getLabel());
        templateService.save(item);

        sum2 = fixture.createTemplateWithSite("testTemplateItem_2", site1Id);
        var item2 = templateService.load(sum2.getId());
        item2.setType(PSTemplateTypeEnum.NORMAL.getLabel());
        templateService.save(item2);

        sum3 = fixture.createTemplateWithSite("testTemplateItem_3", site1Id);
        var item3 = templateService.load(sum3.getId());
        item3.setType(PSTemplateTypeEnum.UNASSIGNED.getLabel());
        templateService.save(item3);

        assertEquals(nonTypedAmount + 2,
                siteTemplateService.findTypedTemplatesBySite(site1Id, PSTemplateTypeEnum.NORMAL).size());
        assertEquals(1,
                siteTemplateService.findTypedTemplatesBySite(site1Id, PSTemplateTypeEnum.UNASSIGNED).size());
    }

    @Test
    public void testCopyTemplates() throws Exception {
        PSTemplateSummary origSum1;
        PSTemplateSummary origSum2;
        PSTemplateSummary copySum1;

        // add two templates to the fixture site
        var site1Id = fixture.site1.getId();
        origSum1 = fixture.createTemplateWithSite("testTemplateItem_1", site1Id);
        origSum2 = fixture.createTemplateWithSite("testTemplateItem_2", site1Id);

        // create an asset
        var asset = new PSAsset();
        asset.getFields().put("sys_title", "SharedAsset");
        asset.setFolderPaths(asList("//Folders/Assets"));
        asset.setType("percRawHtmlAsset");
        asset.getFields().put("html", "TestHTML");
        asset = assetService.save(asset);
        fixture.assetCleaner.add(asset.getId());
        var assetId = asset.getId();

        // add the asset to the templates as a shared resource
        var awRel = new PSAssetWidgetRelationship(origSum1.getId(), 5, "widget5", assetId, 1);
        awRel.setResourceType(PSAssetResourceType.shared);
        assetService.createAssetWidgetRelationship(awRel);

        awRel = new PSAssetWidgetRelationship(origSum2.getId(), 5, "widget5", assetId, 1);
        awRel.setResourceType(PSAssetResourceType.shared);
        assetService.createAssetWidgetRelationship(awRel);

        // create another site
        var site2 = new PSSite();
        site2.setName(this.getClass().getSimpleName() + "Site");
        site2.setLabel(site2.getName());
        site2.setHomePageTitle("Home");
        site2.setNavigationTitle("Home");
        site2.setDescription("This is " + site2.getName());
        site2.setBaseTemplateName(IPSSitemanageConstants.PLAIN_BASE_TEMPLATE_NAME);
        site2.setTemplateName(site2.getName() + "PageTemplate");
        site2 = siteDataService.save(site2);
        var site2Id = site2.getId();
        fixture.siteCleaner.add(site2Id);

        // add template to the new site with same name as original site template and one additional template
        copySum1 = fixture.createTemplateWithSite(origSum1.getName(), site2Id);
        fixture.createTemplateWithSite("testTemplateItem_3", site2Id);

        // new site should now have three templates
        assertEquals(3, siteTemplateService.findTemplatesBySite(site2Id).size());

        // copy templates, all but one of the original site's templates should be copied
        var site1Templates = siteTemplateService.findTemplatesBySite(site1Id);
        var tempMap = siteTemplateService.copyTemplates(site1Id, site2Id);
        assertNotNull(tempMap);
        assertEquals(site1Templates.size(), tempMap.size());
        for (var tempId : tempMap.keySet()) {
            assertNotEquals(tempId, tempMap.get(tempId));
        }

        // site 2 should now have two more templates than site 1
        var site2Templates = siteTemplateService.findTemplatesBySite(site2Id);
        assertEquals(site1Templates.size() + 2, site2Templates.size());
        var site2TempMap = new HashMap<String, String>();
        for (var site2Temp : site2Templates) {
            site2TempMap.put(site2Temp.getName(), site2Temp.getId());
        }

        // site 2 should now have all site 1 templates by name
        for (var site1Temp : site1Templates) {
            assertTrue(site2TempMap.containsKey(site1Temp.getName()));
            assertNotEquals(site2TempMap.get(site1Temp.getName()), site1Temp.getId());
        }

        // make sure the asset was copied to the second template
        var filter = new PSRelationshipFilter();
        filter.limitToOwnerRevision(true);
        filter.setName(PSWidgetAssetRelationshipService.SHARED_ASSET_WIDGET_REL_FILTER);
        filter.setOwner(idMapper.getLocator(site2TempMap.get(origSum2.getName())));
        assertEquals(1, systemWs.loadRelationships(filter).size());

        // the existing template should not have been updated
        filter.setOwner(idMapper.getLocator(site2TempMap.get(copySum1.getName())));
        assertTrue(systemWs.loadRelationships(filter).isEmpty());
    }
}

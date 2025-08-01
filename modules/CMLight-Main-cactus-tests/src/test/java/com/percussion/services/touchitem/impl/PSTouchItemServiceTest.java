// REFACTORED: CP-JAVA11
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
package com.percussion.services.touchitem.impl;

import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.fastforward.managednav.PSManagedNavServiceLocator;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequestContext;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.touchitem.IPSTouchItemService;
import com.percussion.services.touchitem.PSTouchItemConfigBean;
import com.percussion.services.touchitem.PSTouchItemConfiguration;
import com.percussion.services.touchitem.PSTouchItemLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.security.IPSSecurityWs;
import com.percussion.webservices.security.PSSecurityWsLocator;
import com.percussion.webservices.system.IPSSystemWs;
import com.percussion.webservices.system.PSSystemWsLocator;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the Touch Item Service.
 */
@Tag("IntegrationTest")
public class PSTouchItemServiceTest {

    private IPSSecurityWs svc;
    private IPSTouchItemService touchService;
    private PSTouchItemConfiguration config;
    private IPSRequestContext requestContext;
    private IPSManagedNavService managedNavService;
    private IPSCmsObjectMgr cmsObjMgr;
    private IPSSystemWs systemWs;
    private IPSGuidManager guidMgr;

    @BeforeEach
    public void setUp() throws Exception {
        svc = PSSecurityWsLocator.getSecurityWebservice();
        svc.login("admin1", "demo", null, null);
        touchService = PSTouchItemLocator.getTouchItemService();
        config = new PSTouchItemConfiguration();
        config.getTouchDescendantNavProps().put("enabled", "true");
        touchService.setConfiguration(config);
        requestContext = new PSRequestContext(PSWebserviceUtils.getRequest());
        managedNavService = PSManagedNavServiceLocator.getContentWebservice();
        cmsObjMgr = PSCmsObjectMgrLocator.getObjectManager();
        systemWs = PSSystemWsLocator.getSystemWebservice();
        guidMgr = PSGuidManagerLocator.getGuidMgr();
    }

    @Test
    void testTouchConfiguration() {
        var conf = new PSTouchItemConfiguration();

        assertFalse(conf.isTouchItemEnabled());
        assertFalse(conf.isTouchLandingPages());

        conf.getTouchDescendantNavProps().put("enabled", "false");

        assertFalse(conf.isTouchItemEnabled());

        conf.getTouchDescendantNavProps().put("enabled", "true");

        assertTrue(conf.isTouchItemEnabled());
        assertFalse(conf.isTouchLandingPages());

        conf.getTouchDescendantNavProps().put("touchLandingPages", "false");

        assertFalse(conf.isTouchLandingPages());

        conf.getTouchDescendantNavProps().put("touchLandingPages", "true");

        assertTrue(conf.isTouchLandingPages());

        Set<String> srcTypes = new HashSet<>();
        srcTypes.add("rffHome");
        Set<String> tgtTypes = new HashSet<>();
        tgtTypes.add("rffGeneric");

        var configBean = new PSTouchItemConfigBean();
        configBean.setSourceTypes(srcTypes);
        configBean.setTargetTypes(tgtTypes);

        conf = new PSTouchItemConfiguration();
        conf.getTouchItemConfig().add(configBean);
        assertTrue(conf.isTouchItemEnabled());

        // fully configured
        conf = new PSTouchItemConfiguration();
        conf.getTouchDescendantNavProps().put("enabled", "true");
        conf.getTouchDescendantNavProps().put("touchLandingPages", "true");
        conf.getTouchItemConfig().add(configBean);
        assertTrue(conf.isTouchItemEnabled());
        assertTrue(conf.isTouchLandingPages());
    }

    @Test
    void testTouchItemsByItem() throws Exception {
        // navtree
        IPSGuid homeId = getItemGuid(319);

        Map<Integer, Long> navModMap = new HashMap<>();
        Map<Integer, Long> lpModMap = new HashMap<>();
        List<IPSGuid> navIds = managedNavService.findDescendantNavonIds(homeId);
        for (IPSGuid navId : navIds) {
            int id = ((PSLegacyGuid) navId).getContentId();
            navModMap.put(id,
                    cmsObjMgr.loadComponentSummary(id).getContentLastModifiedDate().getTime());

            IPSGuid lpId = managedNavService.getLandingPageFromNavnode(navId);
            id = ((PSLegacyGuid) lpId).getContentId();
            lpModMap.put(id,
                    cmsObjMgr.loadComponentSummary(id).getContentLastModifiedDate().getTime());
        }

        int count = touchService.touchItems(homeId);
        assertTrue(count >= navModMap.size());

        // make sure navons were touched
        for (Integer id : navModMap.keySet()) {
            long mod = cmsObjMgr.loadComponentSummary(id).getContentLastModifiedDate().getTime();
            assertTrue(mod > navModMap.get(id));
            navModMap.put(id, mod);
        }

        // make sure landing pages were not touched
        for (Integer id : lpModMap.keySet()) {
            assertEquals(lpModMap.get(id), cmsObjMgr.loadComponentSummary(id).getContentLastModifiedDate().getTime());
        }

        // now touch landing pages
        config.getTouchDescendantNavProps().put("touchLandingPages", "true");

        count = touchService.touchItems(homeId);
        assertTrue(count >= lpModMap.size());

        // make sure landing pages were touched
        for (Integer id : lpModMap.keySet()) {
            assertTrue(cmsObjMgr.loadComponentSummary(id).getContentLastModifiedDate().getTime() > lpModMap.get(id));
        }

        // make sure navons were not touched
        for (Integer id : navModMap.keySet()) {
            assertEquals(navModMap.get(id), cmsObjMgr.loadComponentSummary(id).getContentLastModifiedDate().getTime());
        }

        // touch grand parent item
        Set<String> srcTypes = new HashSet<>();
        srcTypes.add("rffGeneric");
        Set<String> tgtTypes = new HashSet<>();
        tgtTypes.add("rffHome");

        var configBean1 = new PSTouchItemConfigBean();
        configBean1.setSourceTypes(srcTypes);
        configBean1.setTargetTypes(tgtTypes);
        configBean1.setLevel(-1);

        config.getTouchItemConfig().add(configBean1);

        IPSGuid landingPage = getItemGuid(335);
        count = touchService.touchItems(landingPage);
        assertEquals(1, count);

        // testing touch siblings
        srcTypes = new HashSet<>();
        srcTypes.add("rffImage");
        tgtTypes = new HashSet<>();
        tgtTypes.add("rffGeneric");

        var configBean2 = new PSTouchItemConfigBean();
        configBean2.setSourceTypes(srcTypes);
        configBean2.setTargetTypes(tgtTypes);
        configBean2.setLevel(0);

        // clear out config map to force reload
        config.getTouchItemConfigMap().clear();

        config.getTouchItemConfig().add(configBean2);

        // 481 is the under of "About EI HomePage Image (NYSE Papers).jpg" under above folder
        IPSGuid imageItem = getItemGuid(481);
        int imgCount = touchService.touchItems(imageItem);
        assertEquals(1, imgCount);

        // find direct AA parents of the target item
        var filter = new PSRelationshipFilter();
        filter.setName(PSRelationshipFilter.FILTER_NAME_ACTIVE_ASSEMBLY);
        List<IPSGuid> parentIds = systemWs.findOwners(getItemGuid(329), filter);

        // get the content id's and de-dupe
        List<Integer> contentIds = guidMgr.extractContentIds(parentIds);

        Set<Integer> parentSet = new HashSet<>(contentIds);

        // testing touch AA parent items
        configBean2.setTouchAAParents(true);
        count = touchService.touchItems(imageItem);
        assertEquals(imgCount + parentSet.size(), count);
    }

    @Test
    void testTouchItemsByRelationship() throws Exception {
        config = new PSTouchItemConfiguration();
        config.getTouchDescendantNavProps().put("enabled", "true");
        touchService.setConfiguration(config);

        // dependent is an item
        // 301 is //Sites/EnterpriseInvestments
        // 466 is the home page under above folder
        var rel = getRelationship(301, 466);
        int count = touchService.touchItems(requestContext, rel);
        assertEquals(0, count);

        Set<String> srcTypes = new HashSet<>();
        srcTypes.add("percNavTree");
        Set<String> tgtTypes = new HashSet<>();
        tgtTypes.add("rffHome");

        var configBean1 = new PSTouchItemConfigBean();
        configBean1.setSourceTypes(srcTypes);
        configBean1.setTargetTypes(tgtTypes);
        configBean1.setLevel(0);

        config.getTouchItemConfig().add(configBean1);

        // dependent is an item
        // 301 is //Sites/EnterpriseInvestments
        // 319 is the navtree under above folder
        rel = getRelationship(301, 319);
        count = touchService.touchItems(requestContext, rel);
        assertEquals(1, count);

        // dependent is a folder
        // 301 is //Sites/EnterpriseInvestments
        // 302 is the folder of //Sites/EnterpriseInvestments/Files
        rel = getRelationship(301, 302);
        count = touchService.touchItems(requestContext, rel);
        assertEquals(0, count);

        srcTypes = new HashSet<>();
        srcTypes.add("rffContacts");
        tgtTypes = new HashSet<>();
        tgtTypes.add("rffHome");
        tgtTypes.add("rffFile");

        var configBean2 = new PSTouchItemConfigBean();
        configBean2.setSourceTypes(srcTypes);
        configBean2.setTargetTypes(tgtTypes);
        configBean2.setLevel(-1);

        config = new PSTouchItemConfiguration();
        config.getTouchDescendantNavProps().put("enabled", "true");
        touchService.setConfiguration(config);

        config.getTouchItemConfig().add(configBean1);
        config.getTouchItemConfig().add(configBean2);

        // dependent is a folder
        // 301 is //Sites/EnterpriseInvestments
        // 302 is the folder of //Sites/EnterpriseInvestments/Files
        rel = getRelationship(301, 302);
        count = touchService.touchItems(requestContext, rel);
        assertEquals(1, count);

        config = new PSTouchItemConfiguration();
        config.getTouchDescendantNavProps().put("enabled", "true");
        configBean1.setLevel(-1);
        configBean2.setLevel(0);
        config.getTouchItemConfig().add(configBean1);
        config.getTouchItemConfig().add(configBean2);
        touchService.setConfiguration(config);

        // siblings should not be touched
        count = touchService.touchItems(requestContext, rel);
        assertEquals(0, count);

        // dependent is a folder with subfolders
        // 301 is //Sites/EnterpriseInvestments
        // 303 is the folder of //Sites/EnterpriseInvestments/Images
        rel = getRelationship(301, 303);
        count = touchService.touchItems(requestContext, rel);
        assertEquals(0, count);

        srcTypes = new HashSet<>();
        srcTypes.add("percNavImage");
        tgtTypes = new HashSet<>();
        tgtTypes.add("rffHome");
        tgtTypes.add("percNavTree");

        var configBean3 = new PSTouchItemConfigBean();
        configBean3.setSourceTypes(srcTypes);
        configBean3.setTargetTypes(tgtTypes);
        configBean3.setLevel(-2);

        config = new PSTouchItemConfiguration();
        config.getTouchDescendantNavProps().put("enabled", "true");
        touchService.setConfiguration(config);

        config.getTouchItemConfig().add(configBean1);
        config.getTouchItemConfig().add(configBean2);
        config.getTouchItemConfig().add(configBean3);

        // dependent is a folder with subfolders
        // 301 is //Sites/EnterpriseInvestments
        // 303 is the folder of //Sites/EnterpriseInvestments/Images
        rel = getRelationship(301, 303);
        count = touchService.touchItems(requestContext, rel);
        assertTrue(count >= 4);
    }

    private IPSGuid getItemGuid(int contentId) {
        return new PSLegacyGuid(contentId, 1);
    }

    private PSRelationship getRelationship(int ownerId, int dependentId) throws Exception {
        var filter = new PSRelationshipFilter();
        filter.setName(PSRelationshipConfig.TYPE_FOLDER_CONTENT);
        filter.setOwnerId(ownerId);
        filter.setDependentId(dependentId);
        filter.setCommunityFiltering(false);

        List<PSRelationship> rels = systemWs.loadRelationships(filter);
        return rels.get(0);
    }
}

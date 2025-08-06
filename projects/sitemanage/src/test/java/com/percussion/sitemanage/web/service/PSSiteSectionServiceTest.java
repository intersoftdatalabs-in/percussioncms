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
package com.percussion.sitemanage.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.web.service.PSTemplateServiceClient;
import com.percussion.pagemanagement.web.service.PSTestSiteData;
import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.pathmanagement.data.PSFolderPermission.Principal;
import com.percussion.pathmanagement.data.PSFolderPermission.PrincipalType;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.test.PSObjectRestClient.DataRestClientException;
import com.percussion.share.test.PSRestTestCase;
import com.percussion.sitemanage.data.PSCreateSiteSection;
import com.percussion.sitemanage.data.PSMoveSiteSection;
import com.percussion.sitemanage.data.PSReplaceLandingPage;
import com.percussion.sitemanage.data.PSSectionNode;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteBlogProperties;
import com.percussion.sitemanage.data.PSSiteSection;
import com.percussion.sitemanage.data.PSSiteSection.PSSectionTypeEnum;
import com.percussion.sitemanage.data.PSSiteSectionProperties;
import com.percussion.sitemanage.data.PSUpdateSectionLink;
import com.percussion.sitemanage.service.impl.PSSiteSectionService;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
/**
 * JUnit test case to test service {@link PSSiteSectionService}.
 * // REFACTORED: CP-JAVA11
 * @author Santiago M. Murchio (modernized by Sunny Sal)
 */
public class PSSiteSectionServiceTest extends PSRestTestCase<PSSiteSectionRestClient> {

    private static PSTestSiteData testSiteData;
    private static PSTemplateSummary renameTemplate;
    private static int sectionCounter = 0;
    static PSSiteTemplateRestClient siteTemplateRestClient;
    static PSTemplateServiceClient templateRestClient;

    @BeforeEach All
    public static void setUp() throws Exception {
        testSiteData = new PSTestSiteData();
        testSiteData.setUp();
        renameTemplate = testSiteData.createTemplate("TestTemplateRename");
        testSiteData.assignTemplatesToSite(testSiteData.site1.getId(), renameTemplate.getId());
        testSiteData.getTemplateCleaner().remove("TestTemplateRename");
        siteTemplateRestClient = new PSSiteTemplateRestClient();
        templateRestClient = new PSTemplateServiceClient(baseUrl);
        setupClient(siteTemplateRestClient);
        setupClient(templateRestClient);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        testSiteData.tearDown();
    }

    @Test
    @Disabled("Ignored by default. Uncomment to run large navigation tree test.")
    public void test10CreateLargeNavigationTree() {
        var site = testSiteData.site1;
        var templateId = testSiteData.template1.getId();
        var client = testSiteData.getSectionClient();
        var root = client.loadRoot(site.getName());
        assertNotNull(root);
        assertTrue(root.getChildIds().isEmpty());

        var children = createSubSections(site.getFolderPath(), templateId, 30, 0);
        for (var child : children) {
            createSubSections(child.getFolderPath(), templateId, 5, 1);
        }
    }

    private List<PSSiteSection> createSubSections(String parentPath, String templateId, int numSections, int numLevels) {
        var childSections = new ArrayList<PSSiteSection>();
        for (int i = 0; i < numSections; i++) {
            var childSection = createSection(parentPath, templateId, null);
            childSections.add(childSection);
            System.out.println("Created section " + childSection.getTitle());
        }
        if (numLevels > 0) {
            numLevels--;
            for (var section : childSections) {
                createSubSections(section.getFolderPath(), templateId, numSections, numLevels);
            }
        }
        return childSections;
    }

    @Test
    @Order(20)
    public void test20SiteSection() throws Exception {
        var site = testSiteData.site1;
        var templateId = testSiteData.template1.getId();
        var client = testSiteData.getSectionClient();
        var root = client.loadRoot(site.getName());
        assertNotNull(root);
        assertTrue(root.getChildIds().isEmpty());

        var section = createSection(site.getFolderPath(), templateId, null);
        assertNotNull(section);

        root = client.loadRoot(site.getName());
        assertEquals(1, root.getChildIds().size());

        var linkTitle = section.getTitle();
        var sections = client.loadChildSections(root);
        assertEquals(1, sections.size());
        assertEquals(linkTitle, sections.get(0).getTitle());

        var section2 = createSection(site.getFolderPath(), templateId, null);
        root = client.loadRoot(site.getName());
        assertEquals(2, root.getChildIds().size());
        sections = client.loadChildSections(root);
        assertEquals(2, sections.size());

        var tree = client.loadTree(site.getName());
        assertEquals(root.getTitle(), tree.getTitle());
        assertEquals(2, tree.getChildNodes().size());

        moveSectionTest(site.getName(), client, templateId);
        updateSectionTest(section, client);

        client.delete(sections.get(0).getId());
        testSiteData.removeSectionFromCleaner(sections.get(0));

        root = client.loadRoot(site.getName());
        sections = client.loadChildSections(root);
        assertEquals(1, sections.size());

        var childSection = createSection(section2.getFolderPath(), templateId, null);
        childSection = client.get(childSection.getId());
        client.delete(section2.getId());
        testSiteData.removeSectionFromCleaner(childSection);
        testSiteData.removeSectionFromCleaner(section2);

        assertThrows(Exception.class, () -> client.get(childSection.getId()));

        replaceLandingPageTest(site.getFolderPath(), client, templateId);
    }

    @Test
    @Order(30)
    public void test30MoveSectionLinkTest() {
        var site = testSiteData.site1;
        var templateId = testSiteData.template1.getId();
        var client = testSiteData.getSectionClient();
        var root = client.loadRoot(site.getName());

        var section1 = createSection(site.getFolderPath(), templateId, null);
        var section2 = createSection(site.getFolderPath(), templateId, null);
        var section3 = createSection(site.getFolderPath(), templateId, null);

        var section = testSiteData.createSectionLink(section2.getId(), section2.getId());
        assertNull(section2.getDisplayTitlePath());

        var expectedDisplayPath = "/" + root.getTitle() + "/" + section2.getTitle();
        assertEquals(expectedDisplayPath, section.getDisplayTitlePath());

        var req1 = new PSMoveSiteSection();
        req1.setSourceId(section.getId());
        req1.setTargetId(root.getId());
        var expectedMessage = "Section and a link to it or duplicate section links at the same level are not allowed";
        var ex = assertThrows(DataRestClientException.class, () -> client.move(req1));
        assertEquals(expectedMessage, ex.getMessage());

        root = client.loadRoot(site.getName());
        var req = new PSMoveSiteSection();
        req.setSourceId(section.getId());
        req.setTargetId(section3.getId());
        req.setTargetIndex(-1);
        var newRoot = client.move(req);
        assertEquals(1, newRoot.getChildIds().size());
    }

    @Test
    @Order(40)
    public void test40CreateSectionLink_navTreeElement() {
        var site = testSiteData.site1;
        var templateId = testSiteData.template1.getId();
        var client = testSiteData.getSectionClient();
        var root = client.loadRoot(site.getName());
        var section2 = createSection(site.getFolderPath(), templateId, null);

        assertThrows(DataRestClientException.class, () -> testSiteData.createSectionLink(root.getId(), section2.getId()));
    }

    @Test
    @Order(50)
    public void test50UpdateSectionLink_navTreeElement() {
        var site = testSiteData.site1;
        var templateId = testSiteData.template1.getId();
        var client = testSiteData.getSectionClient();
        var root = client.loadRoot(site.getName());
        var section2 = createSection(site.getFolderPath(), templateId, null);

        var section = testSiteData.createSectionLink(section2.getId(), section2.getId());

        var updateRequest = new PSUpdateSectionLink();
        updateRequest.setNewSectionId(root.getId());
        updateRequest.setOldSectionId(section2.getId());
        updateRequest.setParentSectionId(section2.getId());

        assertThrows(DataRestClientException.class, () -> testSiteData.updateSectionLink(updateRequest));
    }

    @Test
    @Order(60)
    public void test60BlogSection() {
        var site = testSiteData.site1;
        var templateId = testSiteData.template1.getId();
        var client = testSiteData.getSectionClient();
        var root = client.loadRoot(site.getName());
        assertNotNull(root);

        String blogIndexTempId = null;
        try {
            var section = createSection(site.getFolderPath(), templateId, null);
            var sectionLevel1 = createSection(site.getFolderPath(), templateId, null);
            var sectionLevel2 = createSection(section.getFolderPath(), templateId, null);
            var sectionLevel3 = createSection(sectionLevel2.getFolderPath(), templateId, null);

            blogIndexTempId = createBlogTemplate("test", templateId, site.getId());
            var blogSection = createSection(sectionLevel3.getFolderPath(), templateId, blogIndexTempId);
            assertEquals(PSSectionTypeEnum.blog, blogSection.getSectionType());

            var blogs = client.getBlogsForSite(site.getName());
            assertEquals(1, blogs.size());

            var sectionLevel41 = createSection(sectionLevel3.getFolderPath(), templateId, null);
            var sectionLevel42 = createSection(sectionLevel3.getFolderPath(), templateId, null);
            var blogSection2 = createSection(sectionLevel42.getFolderPath(), templateId, blogIndexTempId);
            assertEquals(PSSectionTypeEnum.blog, blogSection.getSectionType());

            blogs = client.getBlogsForSite(site.getName());
            assertEquals(2, blogs.size());

            client.delete(blogSection2.getId());
            client.delete(sectionLevel42.getId());
            client.delete(sectionLevel41.getId());
            client.delete(blogSection.getId());
            client.delete(sectionLevel3.getId());
            client.delete(sectionLevel2.getId());
            client.delete(sectionLevel1.getId());
            client.delete(section.getId());

            testSiteData.removeSectionFromCleaner(blogSection2);
            testSiteData.removeSectionFromCleaner(sectionLevel42);
            testSiteData.removeSectionFromCleaner(sectionLevel41);
            testSiteData.removeSectionFromCleaner(blogSection);
            testSiteData.removeSectionFromCleaner(sectionLevel3);
            testSiteData.removeSectionFromCleaner(sectionLevel2);
            testSiteData.removeSectionFromCleaner(sectionLevel1);
            testSiteData.removeSectionFromCleaner(section);
        } catch (DataRestClientException e) {
            assertTrue(true);
        } catch (Exception e) {
            fail("A 500 response should have been thrown.");
        }
    }

    private PSSiteSection createSection(String parentFolder, String templateId, String blogPostTemplateId) {
        var name = "Section_" + sectionCounter++ + "_" + System.currentTimeMillis() / 1000;
        var linkTitle = name + " navon title";
        var req = new PSCreateSiteSection();
        req.setFolderPath(parentFolder);
        req.setPageName(name);
        req.setPageTitle(name + " title");
        req.setTemplateId(templateId);
        req.setPageLinkTitle(linkTitle);
        req.setPageUrlIdentifier(name);
        req.setCopyTemplates(true);
        if (blogPostTemplateId != null) {
            req.setSectionType(PSSectionTypeEnum.blog);
            req.setBlogPostTemplateId(blogPostTemplateId);
        }
        return testSiteData.createSection(req);
    }

    @Override
    protected PSSiteSectionRestClient getRestClient(String baseUrl) {
        if (restClient == null) {
            restClient = new PSSiteSectionRestClient(baseUrl);
        }
        return restClient;
    }

    private void moveSectionTest(String siteName, PSSiteSectionRestClient client, String templateId) {
        var root = client.loadRoot(siteName);
        var childIds = root.getChildIds();
        assertEquals(2, childIds.size());
        var id0 = childIds.get(0);
        var id1 = childIds.get(1);

        var req = new PSMoveSiteSection();
        req.setSourceId(id0);
        req.setSourceParentId(root.getId());
        req.setTargetId(root.getId());
        req.setTargetIndex(-1);

        var newRoot = client.move(req);
        childIds = newRoot.getChildIds();
        assertEquals(id0, childIds.get(1));
        assertEquals(id1, childIds.get(0));

        var section3 = createSection(root.getFolderPath(), templateId, null);
        root = client.loadRoot(siteName);
        assertEquals(3, root.getChildIds().size());

        req.setSourceId(section3.getId());
        req.setTargetIndex(1);
        newRoot = client.move(req);
        childIds = newRoot.getChildIds();
        assertEquals(section3.getId(), childIds.get(1));

        req.setSourceId(section3.getId());
        req.setTargetId(id0);
        req.setTargetIndex(-1);

        var section0 = client.move(req);
        testSiteData.removeSectionFromCleaner(section3);

        assertEquals(1, section0.getChildIds().size());
        section0 = client.get(id0);
        assertEquals(1, section0.getChildIds().size());
        root = client.loadRoot(siteName);
        assertEquals(2, root.getChildIds().size());
    }

    private void updateSectionTest(PSSiteSection section, PSSiteSectionRestClient client) {
        var properties = client.getSectionProperties(section.getId());
        assertEquals(PSFolderPermission.Access.WRITE, properties.getFolderPermission().getAccessLevel());

        var title = section.getTitle();
        var newTitle = title + "-New" + System.currentTimeMillis();
        var folderName = getFolderNameFromPath(section.getFolderPath());
        var newFolderName = folderName + "-New" + System.currentTimeMillis();

        var updateReq = new PSSiteSectionProperties();
        updateReq.setId(section.getId());
        updateReq.setTitle(newTitle);
        updateReq.setFolderName(newFolderName);
        var permission = new PSFolderPermission();
        permission.setAccessLevel(PSFolderPermission.Access.READ);
        var writeUsers = new ArrayList<Principal>();
        var writer = new Principal();
        writer.setName("writer");
        writer.setType(PrincipalType.USER);
        writeUsers.add(writer);
        permission.setWritePrincipals(writeUsers);
        var adminUsers = new ArrayList<Principal>();
        var admin = new Principal();
        admin.setName("admin");
        admin.setType(PrincipalType.USER);
        adminUsers.add(admin);
        permission.setAdminPrincipals(adminUsers);
        var readUsers = new ArrayList<Principal>();
        var reader = new Principal();
        reader.setName("reader");
        reader.setType(PrincipalType.USER);
        readUsers.add(reader);
        permission.setReadPrincipals(readUsers);
        updateReq.setFolderPermission(permission);

        section = client.update(updateReq);

        assertEquals(newTitle, section.getTitle());
        assertEquals(newFolderName, getFolderNameFromPath(section.getFolderPath()));

        properties = client.getSectionProperties(section.getId());
        var propsPerm = properties.getFolderPermission();
        assertEquals(PSFolderPermission.Access.READ, propsPerm.getAccessLevel());
        assertEquals(1, propsPerm.getAdminPrincipals().size());
        assertEquals(admin, propsPerm.getAdminPrincipals().get(0));
        assertEquals(1, propsPerm.getWritePrincipals().size());
        assertEquals(writer, propsPerm.getWritePrincipals().get(0));
        assertEquals(1, propsPerm.getReadPrincipals().size());
        assertEquals(reader, propsPerm.getReadPrincipals().get(0));

        updateReq.setTitle(title);
        updateReq.setFolderName(folderName);
        updateReq.setFolderPermission(new PSFolderPermission());
        client.update(updateReq);
    }

    private String getFolderNameFromPath(String path) {
        int i = path.lastIndexOf("/");
        return path.substring(i + 1);
    }

    private void replaceLandingPageTest(String siteFolder, PSSiteSectionRestClient client, String templateId)
            throws Exception {
        replaceLandingPageSameFolderTest(siteFolder, client, templateId);
        replaceLandingPageDifferentFolderTest(siteFolder, client, templateId);
    }

    private void replaceLandingPageSameFolderTest(String siteFolder, PSSiteSectionRestClient client, String templateId)
            throws Exception {
        var section = createSection(siteFolder, templateId, null);
        var sectionFolderPath = section.getFolderPath();
        var newLandingPageId = testSiteData.createPage("New-Landing-Page", sectionFolderPath, templateId);
        testSiteData.getPageCleaner().remove(sectionFolderPath + "/New-Landing-Page");
        testReplaceLandingPage(section, newLandingPageId, client);
    }

    private void replaceLandingPageDifferentFolderTest(String siteFolder, PSSiteSectionRestClient client,
            String templateId) throws Exception {
        var newLandingPageId = testSiteData.createPage("New-Landing-Page", siteFolder, templateId);
        testSiteData.getPageCleaner().remove(siteFolder + "/New-Landing-Page");
        testSiteData.getWorkflowClient().transition(newLandingPageId, IPSItemWorkflowService.TRANSITION_TRIGGER_APPROVE);

        var section = createSection(siteFolder, templateId, null);
        int index = section.getFolderPath().lastIndexOf('/');
        var ldPageName = section.getFolderPath().substring(index + 1);
        var landingPagePath = section.getFolderPath().replaceAll("//Sites", PSPathUtils.SITES_FINDER_ROOT) + "/"
                + ldPageName;

        var itemProps = testSiteData.getPathRestClient().findItemProperties(landingPagePath);
        assertEquals("Draft", itemProps.getStatus());
        testSiteData.getWorkflowClient().transition(itemProps.getId(), IPSItemWorkflowService.TRANSITION_TRIGGER_APPROVE);
        itemProps = testSiteData.getPathRestClient().findItemProperties(landingPagePath);
        assertEquals("Pending", itemProps.getStatus());

        testReplaceLandingPage(section, newLandingPageId, client);

        itemProps = testSiteData.getPathRestClient().findItemProperties(landingPagePath);
        assertEquals("Quick Edit", itemProps.getStatus());

        itemProps = testSiteData.getPathRestClient().findItemProperties(landingPagePath + "-1");
        assertEquals("Quick Edit", itemProps.getStatus());
    }

    private void testReplaceLandingPage(PSSiteSection section, String newLandingPageId, PSSiteSectionRestClient client) {
        var sectionProps = client.getSectionProperties(section.getId());
        var sectionFolderName = sectionProps.getFolderName();

        var req = new PSReplaceLandingPage();
        req.setNewLandingPageId(newLandingPageId);
        req.setSectionId(section.getId());
        var resp = client.replaceLandingPage(req);

        assertEquals(sectionFolderName, resp.getNewLandingPageName());
        assertEquals(sectionFolderName + "-1", resp.getOldLandingPageName());

        var nlp = testSiteData.getPageRestClient().get(newLandingPageId);
        assertEquals(sectionFolderName, nlp.getName());
        assertEquals(section.getTitle(), nlp.getLinkTitle());
        assertEquals(section.getFolderPath(), nlp.getFolderPath());
    }

    private String createBlogTemplate(String name, String srcId, String siteId) throws Exception {
        PSTemplate tempId = null;
        var tempSrc = templateRestClient.loadTemplate(srcId);
        if (tempSrc != null) {
            var templateName = name.replaceAll("[\\\\\\\\|/<>?\":*#;% ]", "");
            var tempBaseName = templateName + "-" + tempSrc.getName();
            var tempName = tempBaseName;
            boolean tempExists = false;
            int i = 2;
            var siteTemps = siteTemplateRestClient.findTemplatesBySite(siteId);
            while (!tempExists) {
                for (var siteTempSum : siteTemps) {
                    if (siteTempSum.getName().equals(tempName)) {
                        tempExists = true;
                        break;
                    }
                }
                if (tempExists) {
                    tempName = tempBaseName + "-" + i++;
                    tempExists = false;
                } else {
                    break;
                }
            }
            tempId = templateRestClient.createTemplate(tempName, srcId);
        }
        if (tempId != null)
            return tempId.getId();
        return null;
    }
}

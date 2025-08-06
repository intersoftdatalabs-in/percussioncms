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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRegionTree;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.pagemanagement.web.service.PSTemplateServiceClient;
import com.percussion.pagemanagement.web.service.PSTestSiteData;
import com.percussion.share.async.IPSAsyncJob;
import com.percussion.share.async.PSAsyncJobStatus;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.test.PSRestTestCase;
import com.percussion.sitemanage.data.PSCreateSiteSection;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteBlogPosts;
import com.percussion.sitemanage.data.PSSiteBlogProperties;
import com.percussion.sitemanage.data.PSSiteSection;
import com.percussion.sitemanage.data.PSSiteSection.PSSectionTypeEnum;
import com.percussion.sitemanage.data.PSSiteSectionProperties;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.PSSiteTemplates;
import com.percussion.sitemanage.service.AssignTemplate;
import com.percussion.sitemanage.service.PSSiteTemplates.ImportTemplate;

import java.util.ArrayList;
import java.util.List;

import com.percussion.utils.testing.IntegrationTest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
/**
 * Test saving site to template association.
 * // REFACTORED: CP-JAVA11
 * @author adamgent (modernized by Sunny Sal)
 */
public class PSSiteTemplateServiceTest extends PSRestTestCase<PSSiteTemplateRestClient> {

    private static PSTestSiteData testSiteData;
    private static PSTemplateSummary renameTemplate;
    private static int sectionCounter = 0;
    private static final Logger log = LogManager.getLogger(PSSiteTemplateServiceTest.class);

    @BeforeEach All
    public static void setUp() throws Exception {
        testSiteData = new PSTestSiteData();
        testSiteData.setUp();
        renameTemplate = testSiteData.createTemplate("TestTemplateRename");
        testSiteData.assignTemplatesToSite(testSiteData.site1.getId(), renameTemplate.getId());
        testSiteData.getTemplateCleaner().remove("TestTemplateRename");
    }

    @AfterAll
    public static void tearDown() throws Exception {
        testSiteData.tearDown();
    }

    @Override
    protected PSSiteTemplateRestClient getRestClient(String baseUrl) {
        var restClient = new PSSiteTemplateRestClient();
        restClient.setUrl(baseUrl);
        return restClient;
    }

    private PSTemplateServiceClient getTemplateRestClient() throws Exception {
        var client = new PSTemplateServiceClient(baseUrl);
        setupClient(client);
        return client;
    }

    @Test
    @Order(10)
    public void test010FindSitesByTemplate() throws Exception {
        log.debug("testFindSitesByTemplate");
        var sw = new StopWatch();
        sw.start();
        var templateId = testSiteData.template1.getId();
        var sites = testSiteData.getSiteTemplateRestClient().findSitesByTemplate(templateId);
        sw.stop();
        assertFalse(sites.isEmpty(), "Should have sites associated to a template.");
        log.info("testFindSitesByTemplate took: " + sw);
    }

    @Test
    @Order(20)
    public void test020FindTemplatesBySite() throws Exception {
        log.debug("testFindTemplatesBySite");
        var siteId = testSiteData.site1.getId();
        var templates = testSiteData.getSiteTemplateRestClient().findTemplatesBySite(siteId);
        assertFalse(templates.isEmpty(), "Should have templates associated to a site.");
    }

    @Test
    @Order(30)
    public void test030FindTemplatesWithWidget() throws Exception {
        log.debug("testFindTemplatesWithWidget");
        var siteId = testSiteData.site1.getId();
        var templates = testSiteData.getSiteTemplateRestClient().findTemplatesBySite(siteId, "percRawHtml");
        assertTrue(templates.isEmpty(), "Should not have templates with 'widget' associated to a site.");

        var template1 = getTemplateRestClient().loadTemplate(testSiteData.template1.getId());
        var widget = new PSWidgetItem();
        widget.setDefinitionId("percRawHtml");

        var regTree = template1.getRegionTree();
        regTree.setRegionWidgets(regTree.getRootRegion().getRegionId(), asList(widget));
        template1.setRegionTree(regTree);

        getTemplateRestClient().save(template1);

        var template2 = getTemplateRestClient().createTemplate("fooTemplate", template1.getId());
        template2 = getTemplateRestClient().save(template2);
        testSiteData.assignTemplatesToSite(siteId, template2.getId());

        templates = testSiteData.getSiteTemplateRestClient().findTemplatesBySite(siteId, "percRawHtml");
        assertEquals(2, templates.size());

        templates = testSiteData.getSiteTemplateRestClient().findTemplatesBySite(siteId, "foo");
        assertEquals(0, templates.size());
    }

    @Test
    @Order(40)
    public void test040RenameTemplate() throws Exception {
        log.debug("testRenameTemplate");
        var assignTemplate = new AssignTemplate();
        var newName = "TestTemplateRenamedNow";
        assignTemplate.setName(newName);
        assignTemplate.setTemplateId(renameTemplate.getId());
        var siteTemplates = new PSSiteTemplates();
        siteTemplates.setAssignTemplates(asList(assignTemplate));
        var templates = testSiteData.getSiteTemplateRestClient().save(siteTemplates);
        assertEquals(1, templates.size(), "Should only returned one template");
        assertEquals(newName, templates.get(0).getName(), "Template should have new name");
    }

    @Test
    @Order(50)
    public void test050Blog() throws Exception {
        log.debug("testBlog");
        var site = testSiteData.site1;
        var template1Id = testSiteData.template1.getId();
        var template2Id = testSiteData.template2.getId();

        var siteTemplates = testSiteData.getSiteTemplateRestClient().findTemplatesBySite(site.getId());
        int templateCount = siteTemplates.size();

        var section = createSection(site.getFolderPath(), template1Id, template2Id);
        assertNotNull(section, "A blog is created");

        var sectionProps = testSiteData.getSectionClient().getSectionProperties(section.getId());
        var blogTitle = sectionProps.getFolderName() + "title";

        PSTemplateSummary blogIndexTemplate = null;
        PSTemplateSummary blogPostTemplate = null;
        siteTemplates = testSiteData.getSiteTemplateRestClient().findTemplatesBySite(site.getId());
        assertEquals(templateCount + 2, siteTemplates.size());
        for (var siteTemplate : siteTemplates) {
            var siteTemplateName = siteTemplate.getName();
            if (siteTemplateName.equals(blogTitle + "-" + testSiteData.template1.getName())) {
                blogIndexTemplate = siteTemplate;
            } else if (siteTemplateName.equals(blogTitle + "-" + testSiteData.template2.getName())) {
                blogPostTemplate = siteTemplate;
            }
        }
        assertNotNull(blogIndexTemplate);
        assertNotNull(blogPostTemplate);
        assertEquals(testSiteData.template1.getSourceTemplateName(), blogIndexTemplate.getSourceTemplateName());
        assertEquals(testSiteData.template2.getSourceTemplateName(), blogPostTemplate.getSourceTemplateName());
    }

    @Test
    @Order(60)
    public void test060GetBlogs() throws Exception {
        log.debug("testGetBlogsForSite");
        var site = testSiteData.site2;
        var blogList = testSiteData.getSectionClient().getBlogsForSite(site.getName());
        if (blogList.size() != 0) {
            testSiteData.getSiteCleaner().runCleaners(testSiteData.getSectionCleaner());
            blogList = testSiteData.getSectionClient().getBlogsForSite(site.getName());
        }
        var template1Id = testSiteData.template1.getId();
        var template2Id = testSiteData.template2.getId();

        var section1 = createSection(site.getFolderPath(), template1Id, null);
        var section2 = createSection(site.getFolderPath(), template1Id, null);
        var blogSection1 = createSection(site.getFolderPath(), template1Id, template2Id);

        blogList = testSiteData.getSectionClient().getBlogsForSite(site.getName());
        assertEquals(1, blogList.size(), "There is one child section under root");

        var blogProperties = blogList.get(0);
        assertEquals(blogSection1.getTitle(), blogProperties.getTitle());
        assertNotEquals(blogSection1.getFolderPath(), blogProperties.getPath());
        assertEquals(0, blogProperties.getBlogPostcount(), "There are no blog posts for this blog");
        assertEquals("", blogProperties.getLastPublishDate());
        assertEquals(blogSection1.getId(), blogProperties.getId());

        var indexPage = testSiteData.getPageRestClient().get(blogProperties.getPageId());
        assertEquals(blogSection1.getFolderPath(), indexPage.getFolderPath());
        assertEquals(0, blogProperties.getBlogPostcount());

        var postId = testSiteData.createPage("blogPost1", blogSection1.getFolderPath(),
                blogProperties.getBlogPostTemplateId());
        var post = testSiteData.getPageRestClient().get(postId);
        testSiteData.getPageCleaner().remove(post.getFolderPath() + '/' + post.getName());

        assertEquals(1, testSiteData.getSectionClient().getBlogsForSite(site.getName()).get(0).getBlogPostcount());

        var blogSection2 = createSection(site.getFolderPath(), template1Id, template2Id);
        var blogSection3 = createSection(blogSection2.getFolderPath(), template1Id, template2Id);
        testSiteData.getSectionCleaner().remove(blogSection3.getId());
        var subSection = createSection(blogSection3.getFolderPath(), template1Id, null);
        testSiteData.getSectionCleaner().remove(subSection.getId());

        blogList = testSiteData.getSectionClient().getBlogsForSite(site.getName());
        assertEquals(3, blogList.size(), "There are two child sections under root");

        var sectionLink = testSiteData.createSectionLink(blogSection3.getId(), blogSection3.getId());
        blogList = testSiteData.getSectionClient().getBlogsForSite(site.getName());
        assertEquals(3, blogList.size(), "There are two child sections under root");

        var allBlogsList = testSiteData.getSectionClient().getAllBlogs();
        int count = 0;
        for (var b : allBlogsList) {
            if (b.getPath().startsWith(testSiteData.site1.getFolderPath().substring(1))
                    || b.getPath().startsWith(testSiteData.site2.getFolderPath().substring(1))) {
                count++;
            }
        }
        assertEquals(4, count, "There are four blogs for all sites");
    }

    @Test
    @Order(70)
    public void test070GetBlogPosts() throws Exception {
        log.debug("testGetBlogPosts");
        var site = testSiteData.site1;
        var template1Id = testSiteData.template1.getId();
        var template2Id = testSiteData.template2.getId();

        var blogSection = createSection(site.getFolderPath(), template1Id, template2Id);
        assertNotNull(blogSection, "A blog is created");

        var blogPosts = testSiteData.getSectionClient().getBlogPosts(blogSection.getId());
        assertNull(blogPosts.getPosts());
        assertEquals(blogSection.getTitle(), blogPosts.getBlogTitle());
        assertEquals(blogSection.getFolderPath(), blogPosts.getBlogSectionPath());

        String blogPostTemplateId = null;
        var siteBlogProps = testSiteData.getSectionClient().getBlogsForSite(site.getName());
        for (var blogProps : siteBlogProps) {
            if (blogProps.getId().equals(blogSection.getId())) {
                blogPostTemplateId = blogProps.getBlogPostTemplateId();
                break;
            }
        }
        assertNotNull(blogPostTemplateId);

        var post1Id = testSiteData.createPage("blogPost1", blogSection.getFolderPath(), blogPostTemplateId);
        var postPage = testSiteData.getPageRestClient().get(post1Id);
        testSiteData.getPageCleaner().remove(postPage.getFolderPath() + '/' + postPage.getName());
        var post2Id = testSiteData.createPage("blogPost2", blogSection.getFolderPath(), blogPostTemplateId);
        postPage = testSiteData.getPageRestClient().get(post2Id);
        testSiteData.getPageCleaner().remove(postPage.getFolderPath() + '/' + postPage.getName());

        var pageId = testSiteData.createPage("page1", blogSection.getFolderPath(), template2Id);
        var page = testSiteData.getPageRestClient().get(pageId);
        testSiteData.getPageCleaner().remove(page.getFolderPath() + '/' + page.getName());

        boolean post1Found = false;
        boolean post2Found = false;
        blogPosts = testSiteData.getSectionClient().getBlogPosts(blogSection.getId());
        var posts = blogPosts.getPosts();
        assertNotNull(posts);
        assertEquals(2, posts.size());
        for (var post : posts) {
            if (post.getId().equals(post1Id)) {
                post1Found = true;
            } else if (post.getId().equals(post2Id)) {
                post2Found = true;
            }
        }
        assertTrue(post1Found);
        assertTrue(post2Found);
    }

    private PSSiteSection createSection(String parentFolder, String templateId, String blogPostTemplateId) {
        var req = new PSCreateSiteSection();
        var name = "Section_" + sectionCounter++ + "_" + System.currentTimeMillis() / 1000;
        req.setFolderPath(parentFolder);
        req.setPageName(name);
        req.setPageTitle(name + " title");
        req.setTemplateId(templateId);
        var linkTitle = name + " navon title";
        req.setPageLinkTitle(linkTitle);
        req.setPageUrlIdentifier(name);
        req.setCopyTemplates(true);
        if (blogPostTemplateId != null) {
            req.setSectionType(PSSectionTypeEnum.blog);
            req.setBlogPostTemplateId(blogPostTemplateId);
        }
        return testSiteData.createSection(req);
    }

    @Test
    @Order(80)
    @Disabled("Broken")
    public void test080FindTemplatesWithNoSite() throws Exception {
        log.debug("testFindTemplatesWitNoSite");
        var temps = testSiteData.getSiteTemplates();
        String createdId = null;
        for (var ts : temps) {
            if (StringUtils.equals(ts.getName(), "SiteTemplateServiceCreated1")) {
                createdId = ts.getId();
            }
        }
        assertNotNull(createdId, "Created template should exist");
        var s = new PSSiteTemplates();
        var at = new AssignTemplate();
        at.setSiteIds(new ArrayList<>());
        at.setTemplateId(createdId);
        s.setAssignTemplates(asList(at));
        var savedTemplates = restClient.save(s);
        assertEquals(1, savedTemplates.size(), "Should have 1 saved template");
        var orphans = restClient.findTemplatesWithNoSite();
        boolean match = false;
        for (var ts : orphans) {
            if (StringUtils.equals("SiteTemplateServiceCreated1", ts.getName())) {
                match = true;
            }
        }
        assertTrue(match, "Should have found an orphan template");
    }

    @Test
    @Order(90)
    public void test090CreateTemplateFromUrl() {
        var siteTemplates = new PSSiteTemplates();
        var importTemplate = new ImportTemplate();
        importTemplate.setUrl("http://samples.percussion.com/products/index.html");
        var siteNames = new ArrayList<String>();
        siteNames.add(testSiteData.site1.getName());
        importTemplate.setSiteIds(siteNames);
        siteTemplates.setImportTemplate(importTemplate);
        var template = testSiteData.getSiteTemplateRestClient().createTemplateFromUrl(siteTemplates);
        assertNotNull(template);
    }

    @Test
    @Order(100)
    @Disabled("junit.framework.AssertionFailedError:Line 478")
    public void test100CreateTemplateFromUrlAsync() {
        var siteTemplates = new PSSiteTemplates();
        var importTemplate = new ImportTemplate();
        importTemplate.setUrl("http://samples.percussion.com");
        var siteNames = new ArrayList<String>();
        siteNames.add(testSiteData.site1.getName());
        importTemplate.setSiteIds(siteNames);
        siteTemplates.setImportTemplate(importTemplate);

        long jobId = testSiteData.getSiteTemplateRestClient().createTemplateFromUrlAsync(siteTemplates);
        assertTrue(jobId > 0);

        PSAsyncJobStatus jobStatus;
        do {
            jobStatus = testSiteData.getAsyncJobStatusRestClient().getStatus(Long.toString(jobId));
            assertNotNull(jobStatus);
        } while (!jobStatus.getStatus().equals(IPSAsyncJob.COMPLETE_STATUS)
                && !jobStatus.getStatus().equals(IPSAsyncJob.ABORT_STATUS));

        assertEquals(IPSAsyncJob.COMPLETE_STATUS, jobStatus.getStatus());
        var importedTemplate = restClient.getImportedTemplate(jobId);
        assertNotNull(importedTemplate);
    }
}

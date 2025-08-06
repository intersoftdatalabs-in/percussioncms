// REFACTORED: CP-JAVA11
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

package com.percussion.pagemanagement.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pagemanagement.dao.impl.PSMetadataDocTypeUtils;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplate.PSTemplateTypeEnum;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.test.PSServletTestCase;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.List;

/**
 * Tests the template DAO functionality.
 * Sunny Sal says: "Templates tested, Bollywood style!"
 */
@Tag("IntegrationTest")
public class PSTemplateDaoTest extends PSServletTestCase {

    private PSSiteDataServletTestCaseFixture fixture;
    private IPSTemplateDao templateDao;

    @BeforeEach
    public void setUp() throws Exception {
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
    public void testSaveTypePropertyNull() throws PSDataServiceException {
        var templateName = "Template1";
        var template = createTemplate(templateName);
        template = templateDao.save(template, fixture.site1.getId());
        var retrievedTemplate = templateDao.find(template.getId());
        assertEquals(templateName, retrievedTemplate.getName());
        assertNull(retrievedTemplate.getType());
    }

    @Test
    public void testSaveTypeProperty() throws PSDataServiceException {
        var templateName = "Template1";
        var templateNormalName = "TemplateNormal";
        var template = createTemplate(templateName, PSTemplateTypeEnum.UNASSIGNED.getLabel());
        var templateNormal = createTemplate(templateNormalName, PSTemplateTypeEnum.NORMAL.getLabel());
        template = templateDao.save(template, fixture.site1.getId());
        templateNormal = templateDao.save(templateNormal, fixture.site1.getId());
        var retrievedTemplate = templateDao.find(template.getId());
        assertEquals(templateName, retrievedTemplate.getName());
        assertEquals(PSTemplateTypeEnum.UNASSIGNED.getLabel(), retrievedTemplate.getType());
        var retrievedTemplateNormal = templateDao.find(templateNormal.getId());
        assertEquals(templateNormalName, retrievedTemplateNormal.getName());
        assertEquals(PSTemplateTypeEnum.NORMAL.getLabel(), retrievedTemplateNormal.getType());
    }

    @Test
    public void testFindTemplatesByType() throws PSDataServiceException {
        var templateName = "Template1";
        var template2Name = "Template2";
        var template3Name = "Template3";
        var template4Name = "Template4";
        var template = createTemplate(templateName, PSTemplateTypeEnum.UNASSIGNED.getLabel());
        template = templateDao.save(template, fixture.site1.getId());
        var template2 = createTemplate(template2Name, PSTemplateTypeEnum.UNASSIGNED.getLabel());
        template2 = templateDao.save(template2, fixture.site1.getId());
        var template3 = createTemplate(template3Name, PSTemplateTypeEnum.NORMAL.getLabel());
        template3 = templateDao.save(template3, fixture.site1.getId());
        var template4 = createTemplate(template4Name);
        template4 = templateDao.save(template4, fixture.site1.getId());

        var retrievedTemplates = templateDao.findUserTemplatesByType(PSTemplateTypeEnum.UNASSIGNED);
        assertTrue(retrievedTemplates.contains(template));
        assertTrue(retrievedTemplates.contains(template2));
        assertFalse(retrievedTemplates.contains(template3));
        assertFalse(retrievedTemplates.contains(template4));

        retrievedTemplates = templateDao.findUserTemplatesByType(PSTemplateTypeEnum.NORMAL);
        assertFalse(retrievedTemplates.contains(template));
        assertFalse(retrievedTemplates.contains(template2));
        assertTrue(retrievedTemplates.contains(template3));
        assertTrue(retrievedTemplates.contains(template4));

        retrievedTemplates = templateDao.findUserTemplatesByType(null);
        assertFalse(retrievedTemplates.contains(template));
        assertFalse(retrievedTemplates.contains(template2));
        assertTrue(retrievedTemplates.contains(template3));
        assertTrue(retrievedTemplates.contains(template4));
    }

    @Test
    public void testContentMigrationVersion() throws PSDataServiceException {
        var template = createTemplate("Template1", PSTemplateTypeEnum.NORMAL.getLabel());
        template = templateDao.save(template, fixture.site1.getId());
        template = templateDao.find(template.getId());
        assertNotNull(template);
        assertEquals("0", template.getContentMigrationVersion());
        template.setContentMigrationVersion("1");
        template = templateDao.save(template, fixture.site1.getId());
        template = templateDao.find(template.getId());
        assertNotNull(template);
        assertEquals("1", template.getContentMigrationVersion());

        var template2 = createTemplate("Template2", PSTemplateTypeEnum.NORMAL.getLabel());
        template2.setContentMigrationVersion("1");
        template2 = templateDao.save(template2, fixture.site1.getId());
        template2 = templateDao.find(template2.getId());
        assertNotNull(template2);
        assertEquals("1", template2.getContentMigrationVersion());
    }

    private PSTemplate createTemplate(String name) throws PSDataServiceException {
        return createTemplate(name, null);
    }

    private PSTemplate createTemplate(String name, String type) throws PSDataServiceException {
        var fixtureTemplate = templateDao.find(fixture.template1.getId());
        var template = new PSTemplate();
        template.setName(name);
        template.setType(type);
        template.setReadOnly(false);
        template.setDocType(PSMetadataDocTypeUtils.getDefaultDocType());
        template.setImageThumbPath(fixtureTemplate.getImageThumbPath());
        template.setHtmlHeader(fixtureTemplate.getHtmlHeader());
        template.setLabel(name);
        template.setDescription(name);
        template.setTheme(fixtureTemplate.getTheme());
        template.setSourceTemplateName(fixtureTemplate.getSourceTemplateName());
        return template;
    }

    public IPSTemplateDao getTemplateDao() {
        return templateDao;
    }

    public void setTemplateDao(IPSTemplateDao templateDao) {
        this.templateDao = templateDao;
    }
}

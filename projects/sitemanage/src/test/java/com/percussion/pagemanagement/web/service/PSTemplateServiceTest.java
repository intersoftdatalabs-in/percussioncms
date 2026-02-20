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

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pagemanagement.data.PSHtmlMetadata;
import com.percussion.pagemanagement.data.PSMetadataDocType;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.share.test.PSRestTestCase;
import com.percussion.share.test.PSTestDataCleaner;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

/**
 * Test template service through REST.
 *
 * <p>Sunny Sal says: "Testing templates is like checking your pizza base before adding
 * toppings—crucial!"
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSTemplateServiceTest extends PSRestTestCase<PSTemplateServiceClient> {

  private String baseTemplateId;

  PSTestDataCleaner<String> templateCleaner =
      new PSTestDataCleaner<>() {
        @Override
        protected void clean(String name) throws Exception {
          var id = getTemplateId(name);
          if (id != null) restClient.deleteTemplate(id);
        }

        private String getTemplateId(String name) {
          return restClient.findAll().stream()
              .filter(sum -> !sum.isReadOnly() && StringUtils.equals(name, sum.getName()))
              .map(PSTemplateSummary::getId)
              .findFirst()
              .orElse(null);
        }
      };

  {
    templateCleaner.setFailOnErrors(true);
  }

  @BeforeEach
  public void setup() {
    var readOnlySums = restClient.findAllReadOnly();
    assertNotNull(readOnlySums);
    assertFalse(readOnlySums.isEmpty());
    baseTemplateId = readOnlySums.get(0).getId();
  }

  @Override
  protected PSTemplateServiceClient getRestClient(String url) {
    return new PSTemplateServiceClient(url);
  }

  @Test
  public void testFindAllTemplates() {
    var sums = restClient.findAll();
    assertNotNull(sums);
  }

  @Test
  public void testTemplateMetadata() {
    templateCleaner.add("TestMetadataTemplate");
    var template = restClient.createTemplate("TestMetadataTemplate", baseTemplateId);
    var head = "headContent";
    var bodyA = "afterBody";
    var bodyB = "beforeBody";

    var metadataToSet = new PSHtmlMetadata();
    metadataToSet.setId(template.getId());
    metadataToSet.setAdditionalHeadContent(head);
    metadataToSet.setAfterBodyStartContent(bodyA);
    metadataToSet.setBeforeBodyCloseContent(bodyB);

    restClient.saveHtmlMetadata(metadataToSet);

    var metadataSaved = restClient.loadHtmlMetadata(template.getId());
    assertEquals(head, metadataSaved.getAdditionalHeadContent());
    assertEquals(bodyB, metadataSaved.getBeforeBodyCloseContent());
    assertEquals(bodyA, metadataSaved.getAfterBodyStartContent());
  }

  @Test
  public void testCreateTemplate() {
    templateCleaner.add("TestTemplate1");
    templateCleaner.add("TestTemplate2");

    var newTemplate = restClient.createTemplate("TestTemplate1", baseTemplateId);
    assertNotNull(newTemplate.getRegionTree());
    var newSum1 = restClient.findTemplate(newTemplate.getId());
    assertNotNull(newSum1);
    assertEquals("TestTemplate1", newSum1.getName());

    var newSum2 = restClient.createTemplate("TestTemplate2", newSum1.getId());
    var newSum2Loaded = restClient.findTemplate(newSum2.getId());
    assertNotNull(newSum2Loaded);
    assertEquals(newSum1.getDescription(), newSum2Loaded.getDescription());
    assertEquals(newSum1.getImageThumbPath(), newSum2Loaded.getImageThumbPath());
    assertEquals("TestTemplate2", newSum2Loaded.getName());
    assertEquals(newSum1.getLabel(), newSum2Loaded.getLabel());
  }

  @Test
  public void testSaveWithPage() {
    var name = "TestSaveTemplateWithPage";
    templateCleaner.add(name);

    var newTemplate = restClient.createTemplate(name, baseTemplateId);
    assertNotNull(newTemplate);

    var badPageId = "nosuchpageid";
    var didThrow = false;
    try {
      restClient.save(newTemplate, badPageId);
    } catch (Exception e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    restClient.save(newTemplate);
  }

  @Test
  public void testCreateTemplateWithDefaultDocType() {
    templateCleaner.add("TestTemplateDocType1");

    var newTemplate = restClient.createTemplate("TestTemplateDocType1", baseTemplateId);
    assertNotNull(newTemplate.getRegionTree());
    var newSum1 = restClient.findTemplate(newTemplate.getId());
    assertNotNull(newSum1);
    assertEquals("TestTemplateDocType1", newSum1.getName());
    assertEquals("html5", newTemplate.getDocType().getSelected());
  }

  @Test
  public void testTemplateChangeDocType() {
    templateCleaner.add("TestTemplateDocType2");

    var newTemplate = restClient.createTemplate("TestTemplateDocType2", baseTemplateId);
    assertNotNull(newTemplate.getRegionTree());
    var newSum1 = restClient.findTemplate(newTemplate.getId());
    assertNotNull(newSum1);
    assertEquals("TestTemplateDocType2", newSum1.getName());
    assertEquals("html5", newTemplate.getDocType().getSelected());

    var docType = new PSMetadataDocType();
    docType.setSelected("xhtml");

    var metadataToSet = new PSHtmlMetadata();
    metadataToSet.setId(newTemplate.getId());
    metadataToSet.setDocType(docType);

    restClient.saveHtmlMetadata(metadataToSet);

    var metadataSaved = restClient.loadHtmlMetadata(newTemplate.getId());
    assertEquals("xhtml", metadataSaved.getDocType().getSelected());
  }

  @AfterEach
  public void cleanUp() {
    templateCleaner.clean();
  }
}

/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.share.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.analytics.data.PSAnalyticsProviderConfig;
import com.percussion.contentmigration.service.PSContentMigrationException;
import com.percussion.foldermanagement.data.PSFolderItem;
import com.percussion.foldermanagement.data.PSFolders;
import com.percussion.sitemanage.importer.utils.PSFileDownLoadJobRunner;
import com.percussion.sitemanage.importer.utils.PSFileDownloadJob;
import com.percussion.sitemanage.service.impl.PSSiteSectionMetaDataService.SectionPath;
import com.percussion.utils.service.impl.PSSiteConfigUtils.SecureXmlData;
import com.percussion.workflow.data.PSUiWorkflow;
import com.percussion.workflow.data.PSUiWorkflowStep;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for sitemanage main-source misc Xlint residual (#3107): equals/hashCode
 * contracts, concrete serializable collection fields, and typed list/map copy constructors.
 */
@Tag("UnitTest")
class PSXlintMiscResidualTest {

  @Test
  void secureXmlDataEqualsHashCodeContract() {
    var a = new SecureXmlData();
    a.setSitename("site-a");
    a.setLoginPage("/login");
    a.setUseHttpsForSecureSite(true);
    a.addSecureOrMemberSection("/members/", "editors");

    var b = new SecureXmlData();
    b.setSitename("site-a");
    b.setLoginPage("/login");
    b.setUseHttpsForSecureSite(true);
    b.addSecureOrMemberSection("/members/", "editors");

    var c = new SecureXmlData();
    c.setSitename("site-b");
    c.setLoginPage("/login");
    c.setUseHttpsForSecureSite(true);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
  }

  @Test
  void sectionPathEqualsHashCodeContract() {
    var a = new SectionPath("//Sites/a");
    var b = new SectionPath("//Sites/a");
    var c = new SectionPath("//Sites/b");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
  }

  @Test
  void fileDownloadJobRunnerEqualsHashCodeContract() {
    // Protected ctor — anonymous subclass from test package.
    var job = new PSFileDownloadJob("file.css", "https://example.test/file.css", false) {};
    var a = new PSFileDownLoadJobRunner(job, Map.of());
    var b =
        new PSFileDownLoadJobRunner(
            new PSFileDownloadJob("file.css", "https://example.test/file.css", false) {}, Map.of());
    var c =
        new PSFileDownLoadJobRunner(
            new PSFileDownloadJob("other.css", "https://example.test/other.css", true) {}, Map.of());
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
  }

  @Test
  void mapWrapperEntriesFieldIsConcreteHashMap() throws Exception {
    var field = PSMapWrapper.class.getDeclaredField("entries");
    assertEquals(HashMap.class, field.getType());
    assertFalse(Modifier.isTransient(field.getModifiers()));

    var wrap = new PSMapWrapper();
    wrap.setEntries(Map.of("k", "v"));
    assertInstanceOf(HashMap.class, wrap.getEntries());
    assertEquals("v", wrap.getEntries().get("k"));
  }

  @Test
  void foldersCopiesListIntoArrayList() {
    List<PSFolderItem> src = List.of(new PSFolderItem());
    var folders = new PSFolders(src);
    assertInstanceOf(ArrayList.class, folders.getChildren());
    assertEquals(1, folders.getChildren().size());

    folders.setChildren(null);
    // getter returns empty list when field is null
    assertTrue(folders.getChildren().isEmpty());
  }

  @Test
  void analyticsConfigCopiesExtraParamsMap() {
    Map<String, String> params = Map.of("account", "123");
    var cfg = new PSAnalyticsProviderConfig("user", "pass", false, params);
    // getExtraParamsMap rebuilds from ExtraParamsClass populated by the ctor.
    assertInstanceOf(HashMap.class, cfg.getExtraParamsMap());
    assertEquals("123", cfg.getExtraParamsMap().get("account"));
    cfg.setExtraParams(null);
    assertTrue(cfg.getExtraParamsMap().isEmpty());
  }

  @Test
  void contentMigrationExceptionCopiesFailedItems() {
    var ex = new PSContentMigrationException("fail");
    ex.setFailedItems(Map.of("page-1", "missing template"));
    assertInstanceOf(HashMap.class, ex.getFailedItems());
    assertEquals("missing template", ex.getFailedItems().get("page-1"));
    ex.setFailedItems(null);
    assertNull(ex.getFailedItems());
  }

  @Test
  void uiWorkflowCopiesStepsIntoArrayList() {
    var step = new PSUiWorkflowStep();
    var wf = new PSUiWorkflow("Default", List.of(step));
    assertInstanceOf(ArrayList.class, wf.getWorkflowSteps());
    assertEquals(1, wf.getWorkflowSteps().size());
    wf.setWorkflowSteps(null);
    assertNull(wf.getWorkflowSteps());
  }
}

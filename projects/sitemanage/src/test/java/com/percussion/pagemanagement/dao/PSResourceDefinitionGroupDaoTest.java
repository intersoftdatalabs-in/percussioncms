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

import static com.percussion.test.TestAssertions.*;

import com.percussion.pagemanagement.dao.impl.PSResourceDefinitionGroupDao;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup;
import com.percussion.share.IPSSitemanageConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for resource definition group DAO. Sunny Sal says: "Resources defined, Bollywood style!"
 */
public class PSResourceDefinitionGroupDaoTest {

  private PSResourceDefinitionGroupDao dao;

  @BeforeEach
  public void setup() {
    dao = new PSResourceDefinitionGroupDao();
    dao.setRepositoryDirectory("src/test/resources/resourceDefinitions");
  }

  @Test
  public void shouldFindGroup() throws Exception {
    var widget = dao.find("percSystem");
    assertResourceGroup(widget);
  }

  @Test
  public void shouldFindResource() throws Exception {
    var resource = dao.findResource("percSystem.page");
    assertNotNull(resource);

    var resourceXml = dao.findResource("percSystem.pageXml");
    assertNotNull(resourceXml);
  }

  @Test
  public void shouldFindAllResources() throws Exception {
    var resources = dao.findAllResources();
    assertEquals(7L, resources.size());
  }

  @Test
  public void shouldFindDeps() throws Exception {
    var resource = dao.findResource("percSystem.blah_css");
    assertTrue(resource.getDependencies().size() > 0);
  }

  @Test
  public void shouldFindAssetResourceForContentType() throws Exception {
    assertNotNull(dao.findAssetResourceForType("percPage"));
  }

  @Test
  public void shouldFindAllGroups() throws Exception {
    var widgets = dao.findAll();
    assertEquals(1L, widgets.size());
  }

  @Test
  public void shouldPoll() throws Exception {
    dao.poll();
    dao.poll();
  }

  @Test
  public void shouldNotSupportDelete() throws Exception {
    assertThrows(UnsupportedOperationException.class, () -> dao.delete("fail"));
  }

  @Test
  public void shouldNotSupportSave() throws Exception {
    assertThrows(
        UnsupportedOperationException.class, () -> dao.save(new PSResourceDefinitionGroup()));
  }

  private void assertResourceGroup(PSResourceDefinitionGroup rdg) {
    assertEquals(
        IPSSitemanageConstants.PLAIN_BASE_TEMPLATE_NAME,
        rdg.getAssetResources().get(0).getLegacyTemplate());
    assertEquals("percPage", rdg.getAssetResources().get(0).getContentType());
  }
}

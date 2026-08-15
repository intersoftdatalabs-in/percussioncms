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
package com.percussion.pathmanagement.service;

import static com.percussion.test.TestAssertions.*;

import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import com.percussion.pathmanagement.service.impl.PSSitePathItemService;
import com.percussion.pathmanagement.service.impl.PSSitePathItemService.SiteIdAndFolderPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PSSitePathItemServicePathParseTest {

  TestSitePathItemService ps;
  String siteFolderPath = "//Sites/Site1";

  @BeforeEach
  public void setup() {
    ps = new TestSitePathItemService();
  }

  @Test
  public void shouldExtractSiteIdAndPath() throws Exception {
    assertExtraction("/site1/b/c/", "site1", "//Sites/Site1/b/c/");
    assertExtraction("/site1/b/", "site1", "//Sites/Site1/b/");
    assertExtraction("/site1/", "site1", "//Sites/Site1/");
  }

  @Test
  public void shouldSayIfItHasOnlyTheSiteId() throws Exception {
    var sfp = ps.getSiteIdAndFolderPath("/site3/");
    assertTrue(sfp.isOnlySiteId());
  }

  @Test
  public void shouldSayIfItHasTheFolderPathWithTheSiteId() throws Exception {
    var sfp = ps.getSiteIdAndFolderPath("/site3/b/");
    assertFalse(sfp.isOnlySiteId());
  }

  @Test
  public void shouldFailOnRootPathAsThatIsHandledElseWhere() {
    assertThrows(PSPathNotFoundServiceException.class, () -> ps.getSiteIdAndFolderPath("/"));
  }

  @Test
  public void shouldTreatSiteNameWithoutTrailingSlashAsOnlySiteId() throws Exception {
    var sfp = ps.getSiteIdAndFolderPath("/CorporateInvestments");
    assertTrue(sfp.isOnlySiteId());
    assertEquals("CorporateInvestments", sfp.getSiteId());
    assertEquals("//Sites/Site1/", sfp.getFullFolderPath(siteFolderPath));
  }

  @Test
  public void shouldMatchSitenameToFolderRootLeaf() {
    assertTrue(
        PSSitePathItemService.siteFolderNameMatches(
            "CorporateInvestments", "CorporateInvestments"));
    assertTrue(
        PSSitePathItemService.siteFolderNameMatches(
            "Corporate_Investments", "CorporateInvestments"));
    assertTrue(
        PSSitePathItemService.siteFolderNameMatches(
            "Corporate Investments", "CorporateInvestments"));
    assertFalse(
        PSSitePathItemService.siteFolderNameMatches(
            "CorporateInvestments", "EnterpriseInvestments"));
  }

  @Test
  public void shouldFailOnNoMatch() {
    assertThrows(PSPathNotFoundServiceException.class, () -> ps.getSiteIdAndFolderPath("noslash"));
    assertThrows(PSPathNotFoundServiceException.class, () -> ps.getSiteIdAndFolderPath(""));
  }

  public void assertExtraction(String path, String expectedSiteId, String expectedFolderPath)
      throws PSPathNotFoundServiceException {
    var sfp = ps.getSiteIdAndFolderPath(path);
    assertEquals(expectedSiteId, sfp.getSiteId(), "Site Id");
    assertEquals(expectedFolderPath, sfp.getFullFolderPath(siteFolderPath), "Folder path");
  }

  public static class TestSitePathItemService extends PSSitePathItemService {
    public TestSitePathItemService() {
      super(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public SiteIdAndFolderPath getSiteIdAndFolderPath(String path)
        throws PSPathNotFoundServiceException {
      return super.getSiteIdAndFolderPath(path);
    }
  }
}

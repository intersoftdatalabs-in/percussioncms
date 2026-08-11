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
/*
 * test.percussion.pso.jexl SiteFolderFinderImplTest.java
 *
 * @author DavidBenua
 *
 */
package test.percussion.pso.preview;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.pso.jexl.PSOObjectFinder;
import com.percussion.pso.preview.SiteFolderFinderImpl;
import com.percussion.pso.preview.SiteFolderLocation;
import com.percussion.pso.preview.SiteLoader;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author DavidBenua
 */
@ExtendWith(MockitoExtension.class)
public class SiteFolderFinderImplTest {
  private static final Logger log = LogManager.getLogger(SiteFolderFinderImplTest.class);

  SiteFolderFinderImpl cut;

  PSLocator loc;
  @Mock IPSGuidManager gmgr;
  @Mock IPSContentWs cws;
  @Mock IPSSiteManager siteMgr;
  @Mock IPSSecurityWs secws;
  @Mock SiteLoader siteLoader;
  @Mock PSOObjectFinder finder;

  /**
   * @throws Exception
   */
  @BeforeEach
  public void setUp() {
    cut = new SiteFolderFinderImpl();
    SiteFolderFinderImpl.setGmgr(gmgr);
    SiteFolderFinderImpl.setCws(cws);
    cut.setSiteLoader(siteLoader);
    SiteFolderFinderImpl.setSecws(secws);
    SiteFolderFinderImpl.setFinder(finder);
  }

  /**
   * Test method for {@link SiteFolderFinderImpl#findSiteFolderLocations(String, String, String)}.
   */
  @Test
  public final void testFindSiteFolderLocationsWithFolderId() {
    log.debug("testing site folder previews");
    final PSFolder myFolder = mock(PSFolder.class);

    final IPSGuid folderGuid = mock(IPSGuid.class);
    final IPSSite mySite = mock(IPSSite.class);

    final PSLocator folderLoc = new PSLocator(2);
    final Node myNode = mock(Node.class);
    final Property myProperty = mock(Property.class);

    final PSComponentSummary summary = mock(PSComponentSummary.class);

    cut.setTestCommunityVisibility(false);

    try {
      when(gmgr.makeGuid(any(PSLocator.class))).thenReturn(folderGuid);
      when(cws.findFolderPaths(any(IPSGuid.class)))
          .thenReturn(new String[] {"//Sites/foo/bar/baz"});
      when(finder.getComponentSummaryById("2")).thenReturn(summary);
      when(summary.getName()).thenReturn("foo");
      when(siteLoader.findAllSites()).thenReturn(Arrays.asList(new IPSSite[] {mySite}));
      when(mySite.getFolderRoot()).thenReturn("//Sites/foo");
      when(mySite.getName()).thenReturn("foo");

      List<SiteFolderLocation> locs = cut.findSiteFolderLocations("1", "2", null);

      assertNotNull(locs);
      assertEquals(1, locs.size());

      SiteFolderLocation rloc = locs.get(0);
      assertEquals("//Sites/foo/bar/baz/foo", rloc.getFolderPath());
      verify(gmgr, atLeast(1)).makeGuid(any(PSLocator.class));

    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception caught");
    }

    assertTrue(true);
  }

  @Test
  public final void testFindSiteFolderLocationsNoFolderId() {
    log.debug("testing site previews no folder id");
    final PSFolder myFolder = mock(PSFolder.class);

    final PSLocator myFolderLoc = new PSLocator(2, 0);

    final IPSGuid folderGuid = mock(IPSGuid.class);
    final IPSSite mySite = mock(IPSSite.class);

    cut.setTestCommunityVisibility(false);

    try {
      when(gmgr.makeGuid(any(PSLocator.class))).thenReturn(folderGuid);
      when(cws.findFolderPaths(any(IPSGuid.class)))
          .thenReturn(new String[] {"//Sites/foo/bar/baz"});
      when(siteLoader.findAllSites()).thenReturn(Arrays.asList(new IPSSite[] {mySite}));
      when(mySite.getFolderRoot()).thenReturn("//Sites/foo");
      when(mySite.getName()).thenReturn("foo");
      when(gmgr.makeLocator(folderGuid)).thenReturn(myFolderLoc);
      when(cws.findPathIds("//Sites/foo/bar/baz"))
          .thenReturn(Arrays.asList(new IPSGuid[] {folderGuid}));

      List<SiteFolderLocation> locs = cut.findSiteFolderLocations("1", null, null);

      assertNotNull(locs);
      assertEquals(1, locs.size());

      SiteFolderLocation rloc = locs.get(0);
      assertEquals("//Sites/foo/bar/baz", rloc.getFolderPath());

      verify(gmgr).makeGuid(any(PSLocator.class));

    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception caught");
    }

    assertTrue(true);
  }

  /**
   * Test method for {@link SiteFolderFinderImpl#findSiteFolderLocations(String, String, String)}.
   */
  @Test
  public final void testFindSiteFolderLocationsCommunityFiltering() {
    log.debug("testing site folder with community filtering");
    final PSFolder myFolder = mock(PSFolder.class);

    final IPSGuid folderGuid = mock(IPSGuid.class);
    final IPSSite mySite = mock(IPSSite.class);

    final PSLocator folderLoc = new PSLocator(2);
    final Node myNode = mock(Node.class);
    final Property myProperty = mock(Property.class);

    final PSComponentSummary summary = mock(PSComponentSummary.class);

    cut.setTestCommunityVisibility(true);

    try {
      when(gmgr.makeGuid(any(PSLocator.class))).thenReturn(folderGuid);
      when(cws.findFolderPaths(any(IPSGuid.class)))
          .thenReturn(new String[] {"//Sites/foo/bar/baz"});
      when(finder.getComponentSummaryById("2")).thenReturn(summary);
      when(summary.getName()).thenReturn("foo");
      when(siteLoader.findAllSites()).thenReturn(Arrays.asList(new IPSSite[] {mySite}));
      when(mySite.getFolderRoot()).thenReturn("//Sites/foo");
      when(mySite.getName()).thenReturn("foo");
      when(mySite.getGUID()).thenReturn(new PSLegacyGuid(300, 1));
      when(secws.filterByRuntimeVisibility(anyList()))
          .thenReturn(Collections.singletonList(new PSLegacyGuid(300, 1)));

      List<SiteFolderLocation> locs = cut.findSiteFolderLocations("1", "2", null);

      assertNotNull(locs);
      assertEquals(1, locs.size());

      SiteFolderLocation rloc = locs.get(0);
      assertEquals("//Sites/foo/bar/baz/foo", rloc.getFolderPath());
      verify(gmgr).makeGuid(any(PSLocator.class));
      verify(secws).filterByRuntimeVisibility(anyList());
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception caught");
    }

    assertTrue(true);
  }
}

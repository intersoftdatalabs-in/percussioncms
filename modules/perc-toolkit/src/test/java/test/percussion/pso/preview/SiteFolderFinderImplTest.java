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
/*
 * test.percussion.pso.jexl SiteFolderFinderImplTest.java
 *  
 * @author DavidBenua
 *
 */
package test.percussion.pso.preview;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// ...existing code...

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.percussion.cms.objectstore.PSComponentSummary;
// ...existing code...
import com.percussion.design.objectstore.PSLocator;
import com.percussion.pso.jexl.PSOObjectFinder;
import com.percussion.pso.preview.SiteFolderFinderImpl;
import com.percussion.pso.preview.SiteFolderLocation;
import com.percussion.pso.preview.SiteLoader;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;

/**
 * 
 *
 * @author DavidBenua
 *
 */
@ExtendWith(MockitoExtension.class)
public class SiteFolderFinderImplTest {
    private static final Logger log = LogManager.getLogger(SiteFolderFinderImplTest.class);

    @InjectMocks
    SiteFolderFinderImpl cut = new SiteFolderFinderImpl();

    @Mock
    IPSGuidManager gmgr;
    @Mock
    IPSContentWs cws;
    @Mock
    IPSSiteManager siteMgr;
    @Mock
    IPSSecurityWs secws;
    @Mock
    SiteLoader siteLoader;
    @Mock
    PSOObjectFinder finder;

    @BeforeEach
    public void setUp() throws Exception {
        SiteFolderFinderImpl.setGmgr(gmgr);
        SiteFolderFinderImpl.setCws(cws);
        SiteFolderFinderImpl.setSecws(secws);
        SiteFolderFinderImpl.setFinder(finder);
        cut.setSiteLoader(siteLoader);
    }
   
   /**
    * Test method for {@link SiteFolderFinderImpl#findSiteFolderLocations(String, String, String)}.
    */
    @Test
    public final void testFindSiteFolderLocationsWithFolderId() {
        log.debug("testing site folder previews");
        IPSGuid folderGuid = Mockito.mock(IPSGuid.class);
        IPSSite mySite = Mockito.mock(IPSSite.class);
        PSComponentSummary summary = Mockito.mock(PSComponentSummary.class);

        cut.setTestCommunityVisibility(false);

        try {
            Mockito.when(gmgr.makeGuid(Mockito.any(PSLocator.class))).thenReturn(folderGuid);
            Mockito.when(cws.findFolderPaths(Mockito.any(IPSGuid.class))).thenReturn(new String[]{"//Sites/foo/bar/baz"});
            Mockito.when(finder.getComponentSummaryById("2")).thenReturn(summary);
            Mockito.when(summary.getName()).thenReturn("foo");
            Mockito.when(siteLoader.findAllSites()).thenReturn(Arrays.asList(mySite));
            Mockito.when(mySite.getFolderRoot()).thenReturn("//Sites/foo");
            Mockito.when(mySite.getName()).thenReturn("foo");

            List<SiteFolderLocation> locs = cut.findSiteFolderLocations("1", "2", null);
            assertNotNull(locs);
            assertEquals(1, locs.size());
            SiteFolderLocation rloc = locs.get(0);
            assertEquals("//Sites/foo/bar/baz/foo", rloc.getFolderPath());

        } catch (Exception ex) {
            log.error("Unexpected Exception " + ex, ex);
            fail("Exception caught");
        }
        assertTrue(true);
    }
   
    @Test
    public final void testFindSiteFolderLocationsNoFolderId() {
        log.debug("testing site previews no folder id");
        PSLocator myFolderLoc = new PSLocator(2, 0);
        IPSGuid folderGuid = Mockito.mock(IPSGuid.class);
        IPSSite mySite = Mockito.mock(IPSSite.class);

        cut.setTestCommunityVisibility(false);

        try {
            Mockito.when(gmgr.makeGuid(Mockito.any(PSLocator.class))).thenReturn(folderGuid);
            Mockito.when(cws.findFolderPaths(Mockito.any(IPSGuid.class))).thenReturn(new String[]{"//Sites/foo/bar/baz"});
            Mockito.when(siteLoader.findAllSites()).thenReturn(Arrays.asList(mySite));
            Mockito.when(mySite.getFolderRoot()).thenReturn("//Sites/foo");
            Mockito.when(mySite.getName()).thenReturn("foo");
            Mockito.when(gmgr.makeLocator(folderGuid)).thenReturn(myFolderLoc);
            Mockito.when(cws.findPathIds("//Sites/foo/bar/baz")).thenReturn(Arrays.asList(folderGuid));

            List<SiteFolderLocation> locs = cut.findSiteFolderLocations("1", null, null);
            assertNotNull(locs);
            assertEquals(1, locs.size());
            SiteFolderLocation rloc = locs.get(0);
            assertEquals("//Sites/foo/bar/baz", rloc.getFolderPath());

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
        IPSGuid folderGuid = Mockito.mock(IPSGuid.class);
        IPSSite mySite = Mockito.mock(IPSSite.class);
        PSComponentSummary summary = Mockito.mock(PSComponentSummary.class);

        cut.setTestCommunityVisibility(true);

        try {
            Mockito.when(gmgr.makeGuid(Mockito.any(PSLocator.class))).thenReturn(folderGuid);
            Mockito.when(cws.findFolderPaths(Mockito.any(IPSGuid.class))).thenReturn(new String[]{"//Sites/foo/bar/baz"});
            Mockito.when(finder.getComponentSummaryById("2")).thenReturn(summary);
            Mockito.when(summary.getName()).thenReturn("foo");
            Mockito.when(siteLoader.findAllSites()).thenReturn(Arrays.asList(mySite));
            Mockito.when(mySite.getFolderRoot()).thenReturn("//Sites/foo");
            Mockito.when(mySite.getName()).thenReturn("foo");
            Mockito.when(mySite.getGUID()).thenReturn(new PSGuid(300L));
            Mockito.when(secws.filterByRuntimeVisibility(Mockito.anyList())).thenReturn(Collections.singletonList(new PSGuid(300L)));

            List<SiteFolderLocation> locs = cut.findSiteFolderLocations("1", "2", null);
            assertNotNull(locs);
            assertEquals(1, locs.size());
            SiteFolderLocation rloc = locs.get(0);
            assertEquals("//Sites/foo/bar/baz/foo", rloc.getFolderPath());

        } catch (Exception ex) {
            log.error("Unexpected Exception " + ex, ex);
            fail("Exception caught");
        }
        assertTrue(true);
    }
}

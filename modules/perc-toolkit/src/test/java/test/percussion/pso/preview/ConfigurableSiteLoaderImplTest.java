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
package test.percussion.pso.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.percussion.pso.preview.CachingSiteLoaderImpl;
import com.percussion.pso.preview.ConfigurableSiteLoaderImpl;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConfigurableSiteLoaderImplTest {

  private static final Logger log = LogManager.getLogger(ConfigurableSiteLoaderImplTest.class);

  private ConfigurableSiteLoaderImpl cut;

  @Mock IPSSiteManager siteMgr;

  @BeforeEach
  public void setUp() {
    cut = new ConfigurableSiteLoaderImpl();
    CachingSiteLoaderImpl.setSiteMgr(siteMgr);
  }

  @Test
  public void testLoadAllSites() throws PSSiteManagerException {
    final IPSSite site1 = mock(IPSSite.class);
    final IPSSite site2 = mock(IPSSite.class);
    final List<IPSSite> sites = new ArrayList<>();
    sites.add(site1);
    sites.add(site2);
    final List<String> allowed = new ArrayList<>();
    allowed.add("site1");

    when(site1.getName()).thenReturn("site1");
    when(site2.getName()).thenReturn("site2");
    when(siteMgr.findAllSites()).thenReturn(sites);

    cut.setAllowedSites(allowed);
    List<IPSSite> results = cut.findAllSites();
    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals("site1", results.get(0).getName());

    verify(siteMgr).findAllSites();
  }
}

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
 * test.percussion.pso.preview CachingSiteLoaderImplTest.java
 *
 * @author DavidBenua
 *
 */
package test.percussion.pso.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.pso.preview.CachingSiteLoaderImpl;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CachingSiteLoaderImplTest {
  private static final Logger log = LogManager.getLogger(CachingSiteLoaderImplTest.class);

  private CachingSiteLoaderImpl cut;

  @Mock
  IPSSiteManager siteMgr;

  public CachingSiteLoaderImplTest() {}

  @BeforeEach
  public void setUp() {
    cut = new CachingSiteLoaderImpl();
    CachingSiteLoaderImpl.setSiteMgr(siteMgr);
  }

  @Test
  public final void testFindAllSites() throws Exception {
    cut.setSiteReloadDelay(0L);
    final IPSSite site1 = mock(IPSSite.class);
    final List<IPSSite> sites = new ArrayList<>();
    sites.add(site1);

    when(siteMgr.findAllSites()).thenReturn(sites);

    cut.afterPropertiesSet();

    List<IPSSite> results = cut.findAllSites();
    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals(site1, results.get(0));

    verify(siteMgr).findAllSites();
  }
}

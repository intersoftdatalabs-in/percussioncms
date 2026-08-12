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
package com.percussion.sitemanage.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.share.data.PSMapWrapper;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSSiteSummary;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * {@link PSSiteDataService#findAll()} must return every persisted site. Rhythmyx sample sites are
 * not page-based; CM1 sites are. Listing must not require page-based, a pub server, or a nav tree
 * (#2989 / PR #3209 review).
 */
public class PSSiteDataServiceFindAllTest {

  @Test
  void findAllIncludesRhythmyxAndCm1Sites() throws Exception {
    IPSiteDao dao = mock(IPSiteDao.class);
    when(dao.findAllSummaries())
        .thenReturn(List.of(summary("Enterprise_Investments", false), summary("CM1_Site", true)));

    PSSiteDataService service = mock(PSSiteDataService.class);
    setSiteDao(service, dao);
    when(service.getCopySiteInfo()).thenReturn(new PSMapWrapper());
    when(service.findAll(false)).thenCallRealMethod();

    List<PSSiteSummary> listed = service.findAll(false);
    List<String> names = listed.stream().map(PSSiteSummary::getName).collect(Collectors.toList());
    assertEquals(2, listed.size());
    assertTrue(names.contains("Enterprise_Investments"), names.toString());
    assertTrue(names.contains("CM1_Site"), names.toString());
    assertEquals(
        false,
        listed.stream()
            .filter(s -> "Enterprise_Investments".equals(s.getName()))
            .findFirst()
            .orElseThrow()
            .isPageBased());
    assertEquals(
        true,
        listed.stream()
            .filter(s -> "CM1_Site".equals(s.getName()))
            .findFirst()
            .orElseThrow()
            .isPageBased());
  }

  @Test
  void findAllOmitsOnlyInProgressCopyTarget() throws Exception {
    IPSiteDao dao = mock(IPSiteDao.class);
    when(dao.findAllSummaries())
        .thenReturn(List.of(summary("Keep_Me", false), summary("Copying_Site", true)));

    PSMapWrapper copy = new PSMapWrapper();
    copy.getEntries().put("Target", "Copying_Site");

    PSSiteDataService service = mock(PSSiteDataService.class);
    setSiteDao(service, dao);
    when(service.getCopySiteInfo()).thenReturn(copy);
    when(service.findAll(false)).thenCallRealMethod();

    List<PSSiteSummary> listed = service.findAll(false);
    assertEquals(1, listed.size());
    assertEquals("Keep_Me", listed.get(0).getName());
  }

  private static PSSiteSummary summary(String name, boolean pageBased) {
    PSSiteSummary sum = new PSSiteSummary();
    sum.setName(name);
    sum.setPageBased(pageBased);
    return sum;
  }

  private static void setSiteDao(PSSiteDataService service, IPSiteDao dao) throws Exception {
    Field field = PSSiteDataService.class.getDeclaredField("siteDao");
    field.setAccessible(true);
    field.set(service, dao);
  }
}

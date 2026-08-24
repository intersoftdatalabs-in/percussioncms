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
package com.percussion.pagemanagement.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import static org.mockito.Mockito.mock;

import com.percussion.services.catalog.IPSCatalogSummary;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Assembly/snippet templates (perc.pageDatabase) have no CM1 thumbs. Page
 * create must not fail with an empty-list {@code get(0)} (#3726).
 */
class PSTemplateCopyHelpersTest {

  @Test
  void firstThumbPathSkipsEmptyAndBlank() {
    assertEquals("", PSTemplateDao.firstThumbPath(null));
    assertEquals("", PSTemplateDao.firstThumbPath(Collections.emptyList()));
    assertEquals("", PSTemplateDao.firstThumbPath(List.of("", "  ")));
    assertEquals("/cm/thumbs/a.png", PSTemplateDao.firstThumbPath(List.of("", "/cm/thumbs/a.png")));
  }

  @Test
  void firstCatalogSummaryReturnsNullWhenEmpty() {
    assertNull(PSTemplateDao.firstCatalogSummary(null));
    assertNull(PSTemplateDao.firstCatalogSummary(Collections.emptyList()));
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    assertSame(sum, PSTemplateDao.firstCatalogSummary(List.of(sum)));
  }
}

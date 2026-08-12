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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.percussion.services.error.PSNotFoundException;
import com.percussion.sitemanage.data.PSSectionNode;
import com.percussion.sitemanage.service.IPSSiteSectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * GET /section/tree/{site} must return an empty 200 tree for missing NavTree (#3218).
 */
class PSSiteSectionRestServiceLoadTreeEmptyTest {

  @Mock private PSSiteSectionService siteSectionService;

  private PSSiteSectionRestService rest;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    rest = new PSSiteSectionRestService(siteSectionService);
  }

  @Test
  void loadTreeReturnsServiceEmptyTree() throws Exception {
    when(siteSectionService.loadTree("Demo"))
        .thenReturn(PSSectionNode.emptyTree("Demo", "//Sites/Demo"));

    PSSectionNode tree = rest.loadTree("Demo");
    assertNotNull(tree);
    assertNull(tree.getId());
    assertTrue(tree.getChildNodes().isEmpty());
    assertEquals("Demo", tree.getTitle());
  }

  @Test
  void loadTreeMapsMissingNavTreeExceptionToEmpty() throws Exception {
    when(siteSectionService.loadTree("Demo"))
        .thenThrow(
            new IPSSiteSectionService.PSSiteSectionException(
                "Cannot find navigation tree for site: Demo"));

    PSSectionNode tree = rest.loadTree("Demo");
    assertNotNull(tree);
    assertNull(tree.getId());
    assertTrue(tree.getChildNodes().isEmpty());
  }

  @Test
  void loadTreeRethrowsUnknownSite() throws Exception {
    when(siteSectionService.loadTree("Missing"))
        .thenThrow(new PSNotFoundException("site Missing"));
    assertThrows(PSNotFoundException.class, () -> rest.loadTree("Missing"));
  }

  @Test
  void loadTreeRethrowsUnrelatedSectionErrors() throws Exception {
    when(siteSectionService.loadTree("Demo"))
        .thenThrow(new IPSSiteSectionService.PSSiteSectionException("database unavailable"));
    assertThrows(
        IPSSiteSectionService.PSSiteSectionException.class, () -> rest.loadTree("Demo"));
  }
}

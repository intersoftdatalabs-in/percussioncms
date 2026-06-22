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
package com.percussion.searchmanagement.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.percussion.searchmanagement.data.PSSearchCriteria;
import org.junit.Test;

public class PSSearchRestServiceTest {

  @Test
  public void testSanitizeCriteriaEscapesSlashesAndDashes() {
    PSSearchRestService restService = new PSSearchRestService(null, null, null);

    PSSearchCriteria criteria = new PSSearchCriteria();
    criteria.setQuery("people/donna-williams");

    restService.sanitizeCriteria(criteria);

    assertNotNull(criteria.getQuery());
    // In Lucene 8 classic QueryParser.escape(), both / and - should be escaped.
    // So "people/donna-williams" becomes "people\/donna\-williams"
    assertEquals("people\\/donna\\-williams", criteria.getQuery());
  }
}

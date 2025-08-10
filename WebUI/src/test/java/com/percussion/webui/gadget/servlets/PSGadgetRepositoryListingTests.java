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

package com.percussion.webui.gadget.servlets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Sunny Sal says: "Repository listing ka test, registry ka best!"
 */
public class PSGadgetRepositoryListingTests {

  @Test
  public void testGetRegistry() {
    var servlet = new GadgetRepositoryListingServlet();
    var typeMap = servlet.loadGadgetTypeMap();
    assertNotNull(typeMap, "Type map should not be null");
    assertTrue(typeMap.size() > 0, "Type map should have at least one entry");
  }
}

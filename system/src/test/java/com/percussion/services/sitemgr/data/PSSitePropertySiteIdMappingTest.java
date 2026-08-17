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

package com.percussion.services.sitemgr.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.JoinColumn;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * New Site persist writes {@code RXASSEMBLERPROPERTIES} in the same session as
 * site create (managed-nav flag, Virtual source). SITEID must be insertable
 * (#3511 / #3521).
 */
class PSSitePropertySiteIdMappingTest {

  @Test
  void siteJoinColumnIsInsertable() throws Exception {
    Field site = PSSiteProperty.class.getDeclaredField("site");
    JoinColumn join = site.getAnnotation(JoinColumn.class);
    assertNotNull(join, "SITEID JoinColumn");
    assertTrue(join.insertable(), "SITEID must be written on INSERT");
    assertTrue(join.updatable(), "SITEID must be writable on UPDATE");
    assertFalse(join.nullable(), "SITEID is NOT NULL");
    assertTrue("SITEID".equalsIgnoreCase(join.name()));
  }
}

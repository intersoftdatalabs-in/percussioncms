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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.assembly.PreviewLocation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class AssemblyAdaptorTest {

  @Test
  void buildPreviewUrl_includesTemplateAndPreviewFilter() {
    String url = AssemblyAdaptor.buildPreviewUrl("/Rhythmyx", 42, 7, 3);
    assertTrue(url.startsWith("/Rhythmyx/assembler/render?"));
    assertTrue(url.contains("sys_contentid=42"));
    assertTrue(url.contains("sys_template=7"));
    assertTrue(url.contains("sys_revision=3"));
    assertTrue(url.contains("sys_context=0"));
    assertTrue(url.contains("sys_itemfilter=preview"));
  }

  @Test
  void buildPreviewUrl_stripsTrailingSlashOnRoot() {
    String url = AssemblyAdaptor.buildPreviewUrl("/Rhythmyx/", 1, 2, 1);
    assertTrue(url.startsWith("/Rhythmyx/assembler/render?"));
  }

  @Test
  void previewLocation_usesLookupWhenRevisionOmitted() {
    AssemblyAdaptor adaptor = new AssemblyAdaptor("", id -> 5);
    PreviewLocation loc = adaptor.previewLocation(10, 20, null);
    assertEquals(10, loc.getContentId());
    assertEquals(20, loc.getTemplateId());
    assertEquals(5, loc.getRevision());
    assertTrue(loc.getPreviewUrl().contains("sys_revision=5"));
  }

  @Test
  void previewLocation_returnsNullWhenItemMissing() {
    AssemblyAdaptor adaptor = new AssemblyAdaptor("", id -> null);
    assertNull(adaptor.previewLocation(99, 1, null));
  }

  @Test
  void previewLocation_usesExplicitRevision() {
    AssemblyAdaptor adaptor =
        new AssemblyAdaptor(
            "",
            id -> {
              throw new AssertionError("lookup should not run");
            });
    PreviewLocation loc = adaptor.previewLocation(10, 20, 8);
    assertEquals(8, loc.getRevision());
  }
}

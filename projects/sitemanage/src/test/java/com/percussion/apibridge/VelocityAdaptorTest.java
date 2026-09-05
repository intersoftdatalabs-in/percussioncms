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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.velocity.VelocitySnippet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class VelocityAdaptorTest {

  private VelocityAdaptor adaptor;

  @BeforeEach
  void setUp() {
    adaptor = new VelocityAdaptor();
  }

  @Test
  void listSnippetsCoversAppendixCCategoriesAndStableIds() {
    List<VelocitySnippet> list = adaptor.listSnippets();
    assertFalse(list.isEmpty());

    Set<String> ids = list.stream().map(VelocitySnippet::getId).collect(Collectors.toSet());
    assertEquals(list.size(), ids.size(), "catalog ids must be unique");

    // Field macros (Appendix C)
    assertTrue(ids.contains("field.displayfield"));
    assertTrue(ids.contains("field.field"));
    assertTrue(ids.contains("field.datefield"));
    assertTrue(ids.contains("field.field_if_set"));
    assertTrue(ids.contains("field.datefield_if_set"));
    assertTrue(ids.contains("field.fieldLink"));

    // Slot macros
    assertTrue(ids.contains("slot.slot_simple"));
    assertTrue(ids.contains("slot.slot_wrapped"));
    assertTrue(ids.contains("slot.slot"));
    assertTrue(ids.contains("slot.slot_page"));
    assertTrue(ids.contains("slot.raw_slot_loop"));
    assertTrue(ids.contains("slot.node_slot"));

    // Misc / examples
    assertTrue(ids.contains("misc.inner"));
    assertTrue(ids.contains("misc.children"));
    assertTrue(ids.contains("misc.pager"));
    assertTrue(ids.contains("misc.sample_html_page_skeleton"));
    assertTrue(ids.contains("misc.linkback_head"));
    assertTrue(ids.contains("misc.lclamp_global_template_sample"));
    assertTrue(ids.contains("misc.nav_samples"));

    Set<String> categories = new HashSet<>();
    for (VelocitySnippet s : list) {
      assertNotNull(s.getTitle());
      assertFalse(s.getTitle().isBlank());
      assertNotNull(s.getInsertText());
      assertFalse(s.getInsertText().isBlank());
      categories.add(s.getCategory());
    }
    assertTrue(categories.contains(VelocityAdaptor.CATEGORY_FIELD));
    assertTrue(categories.contains(VelocityAdaptor.CATEGORY_SLOT));
    assertTrue(categories.contains(VelocityAdaptor.CATEGORY_MISC));
  }

  @Test
  void findSnippetByIdIsCaseInsensitive() {
    VelocitySnippet upper = adaptor.findSnippetById("FIELD.DISPLAYFIELD");
    assertNotNull(upper);
    assertEquals("field.displayfield", upper.getId());
    assertTrue(upper.getInsertText().contains("#displayfield"));

    assertNull(adaptor.findSnippetById("missing.id"));
    assertNull(adaptor.findSnippetById(" "));
    assertNull(adaptor.findSnippetById(null));
  }

  @Test
  void buildBuiltinCatalogIsImmutableView() {
    List<VelocitySnippet> built = VelocityAdaptor.buildBuiltinCatalog();
    assertEquals(19, built.size());
  }

  @Test
  void snippetNormalizesAndValidates() {
    VelocitySnippet s =
        VelocityAdaptor.snippet("  field.x  ", "  Title  ", " FIELD ", "  #field(\"rx:x\")  ");
    assertEquals("field.x", s.getId());
    assertEquals("Title", s.getTitle());
    assertEquals(VelocityAdaptor.CATEGORY_FIELD, s.getCategory());
    assertEquals("#field(\"rx:x\")", s.getInsertText());

    assertThrows(
        IllegalArgumentException.class,
        () -> VelocityAdaptor.snippet(" ", "t", VelocityAdaptor.CATEGORY_FIELD, "#x()"));
    assertThrows(
        IllegalArgumentException.class,
        () -> VelocityAdaptor.snippet("id", " ", VelocityAdaptor.CATEGORY_FIELD, "#x()"));
    assertThrows(
        IllegalArgumentException.class,
        () -> VelocityAdaptor.snippet("id", "t", VelocityAdaptor.CATEGORY_FIELD, " "));
    assertThrows(
        IllegalArgumentException.class, () -> VelocityAdaptor.snippet("id", "t", " ", "#x()"));
    assertThrows(
        IllegalArgumentException.class,
        () -> VelocityAdaptor.snippet("id", "t", "unknown", "#x()"));
  }
}

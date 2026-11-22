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

package com.percussion.packages.pagexml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Canonical assembler-id normalization for the single-template render path (issue #7049 / parent
 * #2626).
 *
 * <p>One assembler declaration must resolve to exactly one registered catalog key no matter how
 * it is spelled — exact id, case variant, surrounding whitespace, legacy alias, or full
 * extension-path leaf — so every render/install path agrees on the assembler.
 */
class PSPageXmlTemplateDefEmitterTest {

  @Test
  void canonicalId_exactRegisteredId() {
    assertEquals("htmlAssembler", PSPageXmlTemplateDefEmitter.canonicalAssemblerId("htmlAssembler"));
    assertEquals(
        "markdownAssembler",
        PSPageXmlTemplateDefEmitter.canonicalAssemblerId("markdownAssembler"));
    assertEquals(
        "velocityAssembler",
        PSPageXmlTemplateDefEmitter.canonicalAssemblerId("velocityAssembler"));
  }

  @Test
  void canonicalId_caseAndWhitespaceVariants() {
    assertEquals("htmlAssembler", PSPageXmlTemplateDefEmitter.canonicalAssemblerId("HTMLASSEMBLER"));
    assertEquals(
        "htmlAssembler", PSPageXmlTemplateDefEmitter.canonicalAssemblerId("  htmlAssembler  "));
    assertEquals(
        "pageAssembler", PSPageXmlTemplateDefEmitter.canonicalAssemblerId("PageAssembler"));
  }

  @Test
  void canonicalId_legacyAlias() {
    assertEquals(
        "legacyAssembler", PSPageXmlTemplateDefEmitter.canonicalAssemblerId("xslAssembler"));
    assertEquals(
        "legacyAssembler", PSPageXmlTemplateDefEmitter.canonicalAssemblerId("XSLASSEMBLER"));
  }

  @Test
  void canonicalId_fullExtensionPathLeaf() {
    assertEquals(
        "htmlAssembler",
        PSPageXmlTemplateDefEmitter.canonicalAssemblerId(
            "Java/global/percussion/assembly/htmlAssembler"));
    assertEquals(
        "legacyAssembler",
        PSPageXmlTemplateDefEmitter.canonicalAssemblerId(
            "Java/global/percussion/assembly/xslAssembler"));
  }

  @Test
  void canonicalId_blankOrUnregisteredIsNull() {
    assertNull(PSPageXmlTemplateDefEmitter.canonicalAssemblerId(null));
    assertNull(PSPageXmlTemplateDefEmitter.canonicalAssemblerId("   "));
    assertNull(PSPageXmlTemplateDefEmitter.canonicalAssemblerId("customAssembler"));
    assertNull(
        PSPageXmlTemplateDefEmitter.canonicalAssemblerId(
            "Java/global/percussion/assembly/customAssembler"));
    assertNull(PSPageXmlTemplateDefEmitter.canonicalAssemblerId("Java/global/percussion/"));
  }

  @Test
  void canonicalId_agreesWithStrictResolution() {
    for (String id : PSPageXmlTemplateDefEmitter.REGISTERED_ASSEMBLER_PATHS.keySet()) {
      String canonical = PSPageXmlTemplateDefEmitter.canonicalAssemblerId(id);
      assertEquals(id, canonical);
      assertEquals(
          PSPageXmlTemplateDefEmitter.REGISTERED_ASSEMBLER_PATHS.get(id),
          PSPageXmlTemplateDefEmitter.resolveRegisteredAssemblerPath(id));
    }
  }
}

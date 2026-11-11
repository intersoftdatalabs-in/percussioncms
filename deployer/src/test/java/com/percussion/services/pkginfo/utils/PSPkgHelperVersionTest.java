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
package com.percussion.services.pkginfo.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Covers the pure version-resolution helpers extracted from {@code getVersion} (issue #6042). */
public class PSPkgHelperVersionTest {

  @Test
  public void stripLeadingSeparator_removesSingleLeadingSlash() {
    assertEquals("sys_resources/file.xml", PSPkgHelper.stripLeadingSeparator("/sys_resources/file.xml"));
  }

  @Test
  public void stripLeadingSeparator_removesSingleLeadingBackslash() {
    assertEquals("sys_resources\\file.xml", PSPkgHelper.stripLeadingSeparator("\\sys_resources\\file.xml"));
  }

  @Test
  public void stripLeadingSeparator_preservesBareSeparator() {
    assertEquals("/", PSPkgHelper.stripLeadingSeparator("/"));
    assertEquals("\\", PSPkgHelper.stripLeadingSeparator("\\"));
  }

  @Test
  public void stripLeadingSeparator_leavesPlainNameUntouched() {
    assertEquals("sys_resources/file.xml", PSPkgHelper.stripLeadingSeparator("sys_resources/file.xml"));
  }

  @Test
  public void normalizeForFileLookup_convertsBackslashesOnUnix() {
    String normalized = PSPkgHelper.normalizeForFileLookup("a\\b\\c.xml");
    String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
    if (os.contains("win")) {
      assertEquals("a\\b\\c.xml", normalized);
    } else {
      assertEquals("a/b/c.xml", normalized);
    }
  }

  @Test
  public void normalizeForFileLookup_leavesForwardSlashesUntouched() {
    assertEquals("a/b/c.xml", PSPkgHelper.normalizeForFileLookup("a/b/c.xml"));
  }
}

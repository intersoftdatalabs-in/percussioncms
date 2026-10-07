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
package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class PSItemPropertiesDisplayTitleClearTest {

  @AfterEach
  void clearScope() {
    PSItemPropertiesDisplayTitleClear.open().close();
  }

  @Test
  void htmlParameterAloneDoesNotSkipRequiredDisplayTitle() {
    assertFalse(PSItemPropertiesDisplayTitleClear.isActive());
    assertFalse(PSItemPropertiesDisplayTitleClear.skipRequiredCheck("displaytitle", "yes"));
    assertFalse(PSItemPropertiesDisplayTitleClear.skipRequiredCheck("DisplayTitle", "YES"));
  }

  @Test
  void activeScopeSkipsOnlyDisplayTitleWhenClearParamIsYes() {
    PSItemPropertiesDisplayTitleClear scope = PSItemPropertiesDisplayTitleClear.open();
    try {
      assertTrue(PSItemPropertiesDisplayTitleClear.skipRequiredCheck("displaytitle", "yes"));
      assertTrue(PSItemPropertiesDisplayTitleClear.skipRequiredCheck("DisplayTitle", "YES"));
      assertFalse(PSItemPropertiesDisplayTitleClear.skipRequiredCheck("displaytitle", null));
      assertFalse(PSItemPropertiesDisplayTitleClear.skipRequiredCheck("displaytitle", "no"));
      assertFalse(PSItemPropertiesDisplayTitleClear.skipRequiredCheck("sys_title", "yes"));
      assertFalse(PSItemPropertiesDisplayTitleClear.skipRequiredCheck(null, "yes"));
    } finally {
      scope.close();
    }
    assertFalse(PSItemPropertiesDisplayTitleClear.skipRequiredCheck("displaytitle", "yes"));
  }

  @Test
  void openIsActiveUntilClose() {
    assertFalse(PSItemPropertiesDisplayTitleClear.isActive());
    PSItemPropertiesDisplayTitleClear scope = PSItemPropertiesDisplayTitleClear.open();
    try {
      assertTrue(PSItemPropertiesDisplayTitleClear.isActive());
    } finally {
      scope.close();
    }
    assertFalse(PSItemPropertiesDisplayTitleClear.isActive());
  }
}

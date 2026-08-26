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
package com.percussion.webservices.content.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import org.junit.jupiter.api.Test;

/**
 * CD-13: design save must pass {@link PSItemDefinition#isEnabled()} into the application save, not
 * a hardcoded {@code true}.
 */
class PSContentDesignWsSaveEnabledTest {

  @Test
  void contentTypeSaveAppEnabledFollowsItemDef() {
    PSItemDefinition enabled = mock(PSItemDefinition.class);
    when(enabled.isEnabled()).thenReturn(true);
    assertTrue(PSContentDesignWs.contentTypeSaveAppEnabled(enabled));

    PSItemDefinition disabled = mock(PSItemDefinition.class);
    when(disabled.isEnabled()).thenReturn(false);
    assertFalse(PSContentDesignWs.contentTypeSaveAppEnabled(disabled));
  }

  @Test
  void contentTypeSaveAppEnabledNullIsFalse() {
    assertFalse(PSContentDesignWs.contentTypeSaveAppEnabled(null));
  }
}

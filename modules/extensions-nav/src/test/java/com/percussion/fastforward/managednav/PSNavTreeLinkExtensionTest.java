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
package com.percussion.fastforward.managednav;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Lightweight behavioral checks for {@link PSNavTreeLinkExtension} after real generics cleanup
 * (issue #2034). Full tree assembly requires a live CMS stack and is covered by integration tests
 * outside this module.
 */
class PSNavTreeLinkExtensionTest {

  @Test
  void canModifyStyleSheetIsFalse() {
    assertFalse(new PSNavTreeLinkExtension().canModifyStyleSheet());
  }
}

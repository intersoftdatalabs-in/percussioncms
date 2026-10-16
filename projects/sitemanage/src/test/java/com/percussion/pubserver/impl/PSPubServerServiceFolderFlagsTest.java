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
package com.percussion.pubserver.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Folder flags from the Publishing shell must keep a typed folder and skip an empty base. */
class PSPubServerServiceFolderFlagsTest {

  @Test
  void typedFolderWithoutFlagsIsOwnServer() {
    assertTrue(PSPubServerService.treatExplicitFolderAsOwnServer(false, false, "/night-ftp"));
  }

  @Test
  void explicitFlagsWinOverATypedFolder() {
    assertFalse(PSPubServerService.treatExplicitFolderAsOwnServer(false, true, "/night-ftp"));
    assertFalse(PSPubServerService.treatExplicitFolderAsOwnServer(true, false, "/night-ftp"));
  }

  @Test
  void blankFolderDoesNotBecomeOwnServer() {
    assertFalse(PSPubServerService.treatExplicitFolderAsOwnServer(false, false, "  "));
  }

  @Test
  void blankFolderDoesNotDeriveAPublishingBase() {
    assertFalse(PSPubServerService.derivePublishingBase(false, ""));
    assertFalse(PSPubServerService.derivePublishingBase(false, "   "));
  }

  @Test
  void storedFolderDerivesAPublishingBaseWhenNotOwnServer() {
    assertTrue(PSPubServerService.derivePublishingBase(false, "/Corporate_Investmentsapps/ROOT"));
    assertFalse(PSPubServerService.derivePublishingBase(true, "/night-ftp"));
  }
}

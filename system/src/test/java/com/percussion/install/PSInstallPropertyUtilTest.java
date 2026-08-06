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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Shared install property helpers (#548 / PR #1498). */
@Tag("UnitTest")
public class PSInstallPropertyUtilTest {

  @Test
  void isTruthy_acceptsCommonAffirmatives() {
    assertTrue(PSInstallPropertyUtil.isTruthy("true"));
    assertTrue(PSInstallPropertyUtil.isTruthy("TRUE"));
    assertTrue(PSInstallPropertyUtil.isTruthy(" yes "));
    assertTrue(PSInstallPropertyUtil.isTruthy("1"));
  }

  @Test
  void isTruthy_rejectsNullBlankAndOther() {
    assertFalse(PSInstallPropertyUtil.isTruthy(null));
    assertFalse(PSInstallPropertyUtil.isTruthy(""));
    assertFalse(PSInstallPropertyUtil.isTruthy("   "));
    assertFalse(PSInstallPropertyUtil.isTruthy("false"));
    assertFalse(PSInstallPropertyUtil.isTruthy("0"));
    assertFalse(PSInstallPropertyUtil.isTruthy("no"));
  }
}

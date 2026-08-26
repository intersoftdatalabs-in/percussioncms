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
package com.percussion.sitemanage.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Importer leftover HTTP comparisons use typed {@link HttpErrorCodes} (issue #3846). Status codes
 * are protocol, not security dual-write.
 */
@Tag("UnitTest")
class PSSiteImporterHttpErrorCodesTest {

  @Test
  void redirectAndOkCodesMatchLegacyHttpIntsAndSkipAudit() {
    assertEquals(200, HttpErrorCodes.HTTP_OK.numericCode());
    assertEquals(301, HttpErrorCodes.HTTP_MOVED_PERMANENTLY.numericCode());
    assertEquals(302, HttpErrorCodes.HTTP_MOVED_TEMPORARILY.numericCode());
    assertFalse(HttpErrorCodes.HTTP_OK.isAuditable());
    assertFalse(HttpErrorCodes.HTTP_MOVED_PERMANENTLY.isAuditable());
    assertFalse(HttpErrorCodes.HTTP_MOVED_TEMPORARILY.isAuditable());
  }
}

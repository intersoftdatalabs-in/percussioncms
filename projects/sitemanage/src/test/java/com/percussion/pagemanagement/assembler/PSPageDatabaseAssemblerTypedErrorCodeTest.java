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
package com.percussion.pagemanagement.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;
import com.percussion.services.assembly.PSAssemblyException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Page assembler leftover throw sites use typed {@link AssemblyErrorCodes} (issue #3846). {@link
 * PSPageDatabaseAssembler} needs a Spring context to construct; {@link PSAssemblyException} is the
 * production exception type.
 */
@Tag("UnitTest")
class PSPageDatabaseAssemblerTypedErrorCodeTest {

  @Test
  void unknownErrorUsesTypedAssemblyException() {
    PSAssemblyException ex =
        new PSAssemblyException(
            AssemblyErrorCodes.UNKNOWN_ERROR.numericCode(),
            new RuntimeException("boom"),
            "Failed to create page assembly context ($perc).");
    assertEquals(AssemblyErrorCodes.UNKNOWN_ERROR.numericCode(), ex.getErrorCode());
    assertEquals(5, ex.getErrorCode());
    assertFalse(AssemblyErrorCodes.UNKNOWN_ERROR.isAuditable());
  }

  @Test
  void missingFinderUsesTypedAssemblyCode() {
    PSAssemblyException ex =
        new PSAssemblyException(AssemblyErrorCodes.MISSING_FINDER.numericCode(), (Object) null);
    assertEquals(AssemblyErrorCodes.MISSING_FINDER.numericCode(), ex.getErrorCode());
    assertEquals(12, ex.getErrorCode());
    assertFalse(AssemblyErrorCodes.MISSING_FINDER.isAuditable());
  }
}

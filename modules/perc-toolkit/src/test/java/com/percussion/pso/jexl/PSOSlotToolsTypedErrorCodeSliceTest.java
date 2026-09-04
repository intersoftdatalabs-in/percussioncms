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

package com.percussion.pso.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.extension.PSExtensionException;
import com.percussion.services.assembly.IPSAssemblyErrors;
import com.percussion.services.assembly.PSAssemblyException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4264 (parent #2616 leftover): perc-toolkit leftover production sites use typed assembly /
 * extension error codes. Catalog codes are non-auditable.
 */
@Tag("UnitTest")
class PSOSlotToolsTypedErrorCodeSliceTest {

  @Test
  void leftoverCatalogsMatchLegacyIntsAndSkipAudit() {
    assertEquals(IPSAssemblyErrors.MISSING_FINDER, AssemblyErrorCodes.MISSING_FINDER.numericCode());
    assertFalse(AssemblyErrorCodes.MISSING_FINDER.isAuditable());
    assertEquals(IPSExtensionErrors.EXT_INIT_FAILED, ExtensionErrorCodes.EXT_INIT_FAILED.numericCode());
    assertFalse(ExtensionErrorCodes.EXT_INIT_FAILED.isAuditable());
  }

  @Test
  void typedExceptionsRetainCatalogCodes() {
    PSAssemblyException asm = new PSAssemblyException(AssemblyErrorCodes.MISSING_FINDER, "finder");
    assertSame(AssemblyErrorCodes.MISSING_FINDER, asm.getTypedErrorCode());
    assertEquals(AssemblyErrorCodes.MISSING_FINDER.numericCode(), asm.getErrorCode());
    assertFalse(asm.isAuditable());

    PSExtensionException ext =
        new PSExtensionException(ExtensionErrorCodes.EXT_INIT_FAILED, "mode required");
    assertSame(ExtensionErrorCodes.EXT_INIT_FAILED, ext.getTypedErrorCode());
    assertEquals(ExtensionErrorCodes.EXT_INIT_FAILED.numericCode(), ext.getErrorCode());
    assertFalse(ext.isAuditable());
  }
}

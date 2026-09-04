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
package com.percussion.soln.relationshipbuilder.exit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionRef;
import com.percussion.server.IPSRequestContext;
import com.percussion.soln.relationshipbuilder.IPSRelationshipBuilder;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4271 (parent #2616 leftover): segmentation-rx relationship-builder init throws typed
 * {@link ExtensionErrorCodes#EXT_INIT_FAILED} (dual-write skip via {@code isAuditable()==false}).
 */
@Tag("UnitTest")
class PSAbstractBuildRelationshipsExtensionTypedErrorCodeSliceTest {

  private static final String MODE_INIT_PARAM =
      "com.percussion.extension.relationshipbuilder.mode";

  @Test
  void leftoverCatalogMatchesLegacyIntAndSkipsDualWrite() {
    assertEquals(
        IPSExtensionErrors.EXT_INIT_FAILED, ExtensionErrorCodes.EXT_INIT_FAILED.numericCode());
    assertFalse(ExtensionErrorCodes.EXT_INIT_FAILED.isAuditable());
  }

  @Test
  void missingModeInitParamThrowsTypedExtInitFailed() {
    PSExtensionDef def = modeDef(null);
    TestableExtension exit = new TestableExtension();

    PSExtensionException ex =
        assertThrows(PSExtensionException.class, () -> exit.init(def, new File(".")));
    assertSame(ExtensionErrorCodes.EXT_INIT_FAILED, ex.getTypedErrorCode());
    assertEquals(ExtensionErrorCodes.EXT_INIT_FAILED.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void invalidModeInitParamThrowsTypedExtInitFailed() {
    PSExtensionDef def = modeDef("NOT_A_MODE");
    TestableExtension exit = new TestableExtension();

    PSExtensionException ex =
        assertThrows(PSExtensionException.class, () -> exit.init(def, new File(".")));
    assertSame(ExtensionErrorCodes.EXT_INIT_FAILED, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void buildModeInitSucceeds() throws Exception {
    PSExtensionDef def = modeDef("BUILD");
    TestableExtension exit = new TestableExtension();
    exit.init(def, new File("."));
  }

  private static PSExtensionDef modeDef(String mode) {
    PSExtensionDef def =
        new PSExtensionDef(
            new PSExtensionRef("Java", "global/", "testRelBuilder"),
            List.of(IPSResultDocumentProcessor.class.getName()).iterator(),
            null,
            null,
            null);
    if (mode != null) {
      def.setInitParameter(MODE_INIT_PARAM, mode);
    }
    return def;
  }

  /** Minimal concrete subclass so {@code init} can be exercised in isolation. */
  private static final class TestableExtension extends PSAbstractBuildRelationshipsExtension {
    @Override
    public IPSRelationshipBuilder createRelationshipBuilder(
        Map<String, String> paramMap, IPSRequestContext request, Mode mode) {
      throw new UnsupportedOperationException("not used by init slice tests");
    }
  }
}

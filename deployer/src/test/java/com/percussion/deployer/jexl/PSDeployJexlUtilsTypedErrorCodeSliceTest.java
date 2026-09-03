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
package com.percussion.deployer.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;
import com.percussion.error.IPSDeploymentErrors;
import com.percussion.error.PSDeployException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4196 (parent #2616 leftover): {@link PSDeployJexlUtils} production parse failures throw
 * typed {@link DeploymentErrorCodes#UNEXPECTED_ERROR}. That catalog code is non-auditable and skips
 * dual-write. {@link IPSDeploymentErrors} remains the numeric bridge.
 */
@Tag("UnitTest")
public class PSDeployJexlUtilsTypedErrorCodeSliceTest {

  @Test
  public void unexpectedErrorNumericBridgeMatchesLegacyInt() {
    assertEquals(
        IPSDeploymentErrors.UNEXPECTED_ERROR,
        DeploymentErrorCodes.UNEXPECTED_ERROR.numericCode());
    assertFalse(DeploymentErrorCodes.UNEXPECTED_ERROR.isAuditable());
  }

  @Test
  public void invalidBindingUsesTypedNonAuditableUnexpectedError() {
    PSDeployException ex =
        assertThrows(
            PSDeployException.class, () -> PSDeployJexlUtils.getIdsFromBinding("not a valid ("));
    assertEquals(DeploymentErrorCodes.UNEXPECTED_ERROR.numericCode(), ex.getErrorCode());
    assertSame(DeploymentErrorCodes.UNEXPECTED_ERROR, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
    Object[] args = ex.getErrorArguments();
    assertTrue(args != null && args.length >= 1);
    assertEquals("Unable to create JEXL expression from the binding", String.valueOf(args[0]));
  }

  @Test
  public void blankBindingThrowsIllegalArgumentNotDeployException() {
    assertThrows(IllegalArgumentException.class, () -> PSDeployJexlUtils.getIdsFromBinding(""));
    assertThrows(IllegalArgumentException.class, () -> PSDeployJexlUtils.getIdsFromBinding("   "));
    assertThrows(IllegalArgumentException.class, () -> PSDeployJexlUtils.getIdsFromBinding(null));
  }

  @Test
  public void validBindingExtractsNumericIds() throws Exception {
    List<String> ids =
        PSDeployJexlUtils.getIdsFromBinding(
            "$rx.codec.decodeFromXml(\"222\", \"301\",\"222\", 123);");
    assertEquals(4, ids.size());
    assertTrue(ids.contains("222"));
    assertTrue(ids.contains("301"));
    assertTrue(ids.contains("123"));
  }
}

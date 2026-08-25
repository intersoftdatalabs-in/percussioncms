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
package com.percussion.deployer.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;
import com.percussion.deployer.catalog.server.PSCatalogHandler;
import com.percussion.deployer.objectstore.PSIdMap;
import com.percussion.deployer.objectstore.PSIdMapping;
import com.percussion.error.PSDeployException;
import com.percussion.server.PSRequest;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Issue #3739 (parent #2616 slice 1): leftover deployer catalog / client / objectstore /
 * PSDeployService production sites throw typed {@link DeploymentErrorCodes} (non-auditable leftover
 * codes skip dual-write). Server/handlers remain on later slices.
 */
@Tag("UnitTest")
public class PSDeployerCatalogClientTypedErrorCodeSliceTest {

  @Test
  public void catalogHandlerNullInputDocUsesTypedNonAuditableCode() throws Exception {
    PSRequest req = mock(PSRequest.class);
    when(req.getInputDocument()).thenReturn(null);

    PSDeployException ex =
        assertThrows(PSDeployException.class, () -> PSCatalogHandler.processRequest(req));
    assertEquals(DeploymentErrorCodes.NULL_INPUT_DOC.numericCode(), ex.getErrorCode());
    assertSame(DeploymentErrorCodes.NULL_INPUT_DOC, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void catalogHandlerInvalidRequestTypeUsesTypedNonAuditableCode() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(doc, "NotACatalogRequest");
    PSRequest req = mock(PSRequest.class);
    when(req.getInputDocument()).thenReturn(doc);

    PSDeployException ex =
        assertThrows(PSDeployException.class, () -> PSCatalogHandler.processRequest(req));
    assertEquals(DeploymentErrorCodes.INVALID_REQUEST_TYPE.numericCode(), ex.getErrorCode());
    assertSame(DeploymentErrorCodes.INVALID_REQUEST_TYPE, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void idMapMissingAndIncompleteUseTypedNonAuditableCodes() throws Exception {
    PSIdMap map = new PSIdMap("driver:server:db:origin");
    PSDeployException missing =
        assertThrows(PSDeployException.class, () -> map.getNewId("99", "widget"));
    assertEquals(DeploymentErrorCodes.MISSING_ID_MAPPING.numericCode(), missing.getErrorCode());
    assertSame(DeploymentErrorCodes.MISSING_ID_MAPPING, missing.getTypedErrorCode());
    assertFalse(missing.isAuditable());

    map.addMapping(new PSIdMapping("1", "name", "widget"));
    PSDeployException incomplete =
        assertThrows(PSDeployException.class, () -> map.getNewId("1", "widget"));
    assertEquals(DeploymentErrorCodes.INCOMPLETE_ID_MAPPING.numericCode(), incomplete.getErrorCode());
    assertSame(DeploymentErrorCodes.INCOMPLETE_ID_MAPPING, incomplete.getTypedErrorCode());
    assertFalse(incomplete.isAuditable());
  }

  @Test
  public void idMapNonNumericTargetUsesTypedInvalidTarget() throws Exception {
    PSIdMap map = new PSIdMap("driver:server:db:origin");
    PSIdMapping mapping = new PSIdMapping("1", "name", "widget");
    mapping.setTarget("not-an-int", "targetName");
    map.addMapping(mapping);

    PSDeployException ex =
        assertThrows(PSDeployException.class, () -> map.getNewIdInt("1", "widget"));
    assertEquals(DeploymentErrorCodes.INVALID_ID_MAPPING_TARGET.numericCode(), ex.getErrorCode());
    assertSame(DeploymentErrorCodes.INVALID_ID_MAPPING_TARGET, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }
}

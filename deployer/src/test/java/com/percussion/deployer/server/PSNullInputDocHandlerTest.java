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
package com.percussion.deployer.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;
import com.percussion.deployer.catalog.server.PSCatalogHandler;
import com.percussion.error.IPSDeploymentErrors;
import com.percussion.error.PSDeployException;
import com.percussion.server.PSRequest;
import org.junit.jupiter.api.Test;

/**
 * Unit tests locking the {@code NULL_INPUT_DOC} (error 8) throw sites that Package Installer can hit
 * when the multipart XML part is not bound as the request input document.
 *
 * <p>Slice 2 of #955 / #2265 — does not remove these guards; documents expected failure when {@link
 * PSRequest#getInputDocument()} is null.
 */
public class PSNullInputDocHandlerTest {

  @Test
  public void catalogHandlerThrowsNullInputDocWhenDocumentMissing() {
    PSRequest req = mock(PSRequest.class);
    when(req.getInputDocument()).thenReturn(null);

    PSDeployException ex =
        assertThrows(PSDeployException.class, () -> PSCatalogHandler.processRequest(req));
    assertEquals(IPSDeploymentErrors.NULL_INPUT_DOC, ex.getErrorCode());
    assertEquals(DeploymentErrorCodes.NULL_INPUT_DOC.numericCode(), ex.getErrorCode());
    assertSame(DeploymentErrorCodes.NULL_INPUT_DOC, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void connectThrowsNullInputDocWhenDocumentMissing() {
    PSRequest req = mock(PSRequest.class);
    when(req.getInputDocument()).thenReturn(null);

    PSDeploymentHandler handler = new PSDeploymentHandler();
    PSDeployException ex =
        assertThrows(PSDeployException.class, () -> handler.connect(req));
    assertEquals(IPSDeploymentErrors.NULL_INPUT_DOC, ex.getErrorCode());
  }

  @Test
  public void validateArchiveThrowsNullInputDocWhenDocumentMissing() {
    PSRequest req = mock(PSRequest.class);
    when(req.getInputDocument()).thenReturn(null);

    PSDeploymentHandler handler = new PSDeploymentHandler();
    PSDeployException ex =
        assertThrows(PSDeployException.class, () -> handler.validateArchive(req));
    assertEquals(IPSDeploymentErrors.NULL_INPUT_DOC, ex.getErrorCode());
  }

  @Test
  public void connectRejectsNullRequest() {
    PSDeploymentHandler handler = new PSDeploymentHandler();
    assertThrows(IllegalArgumentException.class, () -> handler.connect(null));
  }
}

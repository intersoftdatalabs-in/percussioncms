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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.server.PSRequest;
import com.percussion.server.PSResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Document;

/**
 * Locks the behavior of the {@code processRequest} dispatch extracted for #6039:
 * request-type validation, unknown sub-type rejection, and XML error rendering
 * with HTTP 500 on the failure path.
 */
public class PSDeploymentHandlerDispatchTest {

  @Test
  public void processRequestWithNullRequestTypeRendersErrorAnd500() {
    PSRequest req = mockDeployRequest(null);
    PSResponse resp = req.getResponse();

    new PSDeploymentHandler().processRequest(req);

    verify(resp).setStatus(500);
    assertNotNull(errorDocument(resp));
  }

  @Test
  public void processRequestWithNonDeployRequestTypeRendersErrorAnd500() {
    PSRequest req = mockDeployRequest("bogus-type");
    PSResponse resp = req.getResponse();

    new PSDeploymentHandler().processRequest(req);

    verify(resp).setStatus(500);
    assertNotNull(errorDocument(resp));
  }

  @Test
  public void processRequestWithUnknownSubRequestTypeRendersErrorAnd500() {
    PSRequest req = mockDeployRequest("deploy-noSuchOperation");
    PSResponse resp = req.getResponse();

    new PSDeploymentHandler().processRequest(req);

    verify(resp).setStatus(500);
    assertNotNull(errorDocument(resp));
  }

  @Test
  public void errorDocumentHasRootElement() {
    PSRequest req = mockDeployRequest(null);
    PSResponse resp = req.getResponse();

    new PSDeploymentHandler().processRequest(req);

    Document errDoc = errorDocument(resp);
    assertNotNull(errDoc);
    assertNotNull(errDoc.getDocumentElement());
    assertEquals("PSXDeployException", errDoc.getDocumentElement().getTagName());
  }

  private static PSRequest mockDeployRequest(String reqType) {
    PSRequest req = mock(PSRequest.class);
    PSResponse resp = mock(PSResponse.class);
    when(req.getCgiVariable(any())).thenReturn(reqType);
    when(req.getInputDocument()).thenReturn(null);
    when(req.getResponse()).thenReturn(resp);
    return req;
  }

  private static Document errorDocument(PSResponse resp) {
    ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
    verify(resp).setContent(captor.capture());
    return captor.getValue();
  }
}

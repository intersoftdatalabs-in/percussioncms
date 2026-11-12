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
package com.percussion.deployer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;
import com.percussion.deployer.client.PSDeploymentServerConnection.ConnectionTransport;
import com.percussion.error.PSDeployException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ConnectionTransport#create(String, String, int, String, String)}.
 *
 * <p>Behavioral coverage for the constructor extraction in #6146: protocol validation, base-URI
 * derivation, and the preemptive Basic {@code Authorization} header the legacy transport
 * provided.
 */
@DisplayName("PSDeploymentServerConnection ConnectionTransport (#6146)")
public class PSDeploymentServerConnectionTransportTest {

  @Test
  public void createBuildsHttpBaseUriAndBasicAuthHeader() throws Exception {
    ConnectionTransport transport = ConnectionTransport.create("http", "cms-host", 9992, "u", "p");

    assertEquals("http://cms-host:9992", transport.baseUri().toString());
    String expected =
        "Basic "
            + Base64.getEncoder()
                .encodeToString("u:p".getBytes(StandardCharsets.ISO_8859_1));
    assertEquals(expected, transport.basicAuthHeader());
    assertNotNull(transport.httpClient());
  }

  @Test
  public void createAcceptsHttpsRegardlessOfCase() throws Exception {
    ConnectionTransport transport =
        ConnectionTransport.create("HTTPS", "cms-host", 443, "u", "p");

    assertEquals("https", transport.baseUri().getScheme());
    assertTrue(transport.basicAuthHeader().startsWith("Basic "));
  }

  @Test
  public void createHandlesNullPasswordAsEmpty() throws Exception {
    // The constructor normalizes a null password to "" before building the transport.
    ConnectionTransport transport =
        ConnectionTransport.create("http", "cms-host", 9992, "u", "");

    String expected =
        "Basic "
            + Base64.getEncoder()
                .encodeToString("u:".getBytes(StandardCharsets.ISO_8859_1));
    assertEquals(expected, transport.basicAuthHeader());
  }

  @Test
  public void createRejectsUnsupportedProtocol() {
    PSDeployException e =
        assertThrows(
            PSDeployException.class,
            () -> ConnectionTransport.create("ftp", "cms-host", 21, "u", "p"));

    assertEquals(DeploymentErrorCodes.UNEXPECTED_ERROR.numericCode(), e.getErrorCode());
  }
}

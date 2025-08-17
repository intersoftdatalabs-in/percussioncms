/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.xml.bind.DatatypeConverter;

/** JAX-RS client filter for HTTP Basic Authentication. // REFACTORED: CP-JAVA11 */
public class HTTPBasicAuthFilter implements ClientRequestFilter {

  private final String user;
  private final String password;

  public HTTPBasicAuthFilter(String user, String password) {
    this.user = user;
    this.password = password;
  }

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    var headers = requestContext.getHeaders();
    final var basicAuthentication = getBasicAuthentication();
    headers.add("Authorization", basicAuthentication);
  }

  private String getBasicAuthentication() {
    var token = this.user + ":" + this.password;
    return "Basic " + DatatypeConverter.printBase64Binary(token.getBytes(StandardCharsets.UTF_8));
  }
}

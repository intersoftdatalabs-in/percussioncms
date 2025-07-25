/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.delivery.utils.security;

import com.percussion.security.ToDoVulnerability;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * Common helper class to return an HTTP client.
 * <p>
 * WARNING: This implementation disables SSL hostname verification and trusts all certificates.
 * This is NOT OWASP compliant and should only be used for internal, self-signed services.
 * Refactor before production use.
 *
 * @author leonardohildt
 */
@ToDoVulnerability
@Deprecated
public class PSHttpClient {

    public PSHttpClient() {
        // KISS: No state, no problem.
    }

    /**
     * Creates and returns an SSL-enabled client.
     *
     * @return the client, never {@code null}.
     * @throws Exception if any error occurs.
     */
    public Client getSSLClient() throws Exception {
        var ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[] { new PSSimpleTrustManager(null) }, null);

        // OWASP WARNING: This disables hostname verification and trusts all SSL certificates.
        var client = ClientBuilder.newBuilder()
                .sslContext(ctx)
                .hostnameVerifier((String s, SSLSession sslSession) -> true)
                .build();

        return client;
    }
}

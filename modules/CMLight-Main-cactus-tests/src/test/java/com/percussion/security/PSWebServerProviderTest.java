// REFACTORED: CP-JAVA11
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
package com.percussion.security;

import com.percussion.design.objectstore.PSSecurityProviderInstance;
import com.percussion.design.objectstore.PSServerConfiguration;
import com.percussion.utils.server.IPSCgiVariables;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.services.security.PSServletRequestWrapper;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the web server security provider.  Assumes one is configured.
 * Note, nightly integration build automatically creates a web security provider
 * for this test.
 */
@Tag("IntegrationTest")
public class PSWebServerProviderTest {

    /**
     * Test authentication.
     *
     * @throws Exception if there are any errors.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testAuthentication() throws Exception {
        var req = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);

        // this should create a mock servlet request so we can fudge the headers
        var newReq = req.cloneRequest();

        MockHttpServletRequest mockReq = null;
        var servletReq = newReq.getServletRequest();
        if (!(servletReq instanceof MockHttpServletRequest)) {
            if (servletReq instanceof PSServletRequestWrapper) {
                var psServletReq = (PSServletRequestWrapper) servletReq;
                servletReq = (HttpServletRequest) psServletReq.getRequest();
                if (servletReq instanceof MockHttpServletRequest) {
                    mockReq = (MockHttpServletRequest) servletReq;
                }
            }
        } else {
            mockReq = (MockHttpServletRequest) servletReq;
        }

        assertNotNull(mockReq, "no mock request found from cloned request");

        // now add headers
        PSSecurityProviderInstance wssp = null;
        PSServerConfiguration serverConfig = PSServer.getServerConfiguration();
        Iterator<?> spInsts = serverConfig.getSecurityProviderInstances().iterator();
        while (spInsts.hasNext()) {
            var inst = (PSSecurityProviderInstance) spInsts.next();
            if (inst.getType() == PSSecurityProvider.SP_TYPE_WEB_SERVER) {
                wssp = inst;
                break;
            }
        }

        assertNotNull(wssp, "no webserver security provider configured");

        Properties props = wssp.getProperties();
        final String authUserHeader = props.getProperty(PSWebServerProvider.AUTHENTICATED_USER_HEADER);
        final String roleListHeader = props.getProperty(PSWebServerProvider.USER_ROLE_LIST_HEADER);
        final String roleDelim = props.getProperty(PSWebServerProvider.ROLE_LIST_DELIMITER);

        mockReq.addHeader(authUserHeader, "author1");
        mockReq.addHeader(roleListHeader, "Author" + roleDelim + "Editor" + roleDelim + "QA");
        mockReq.addHeader(IPSCgiVariables.CGI_AUTH_TYPE, "Basic");

        final HttpServletRequest authReq = PSSecurityFilter.authenticate(mockReq,
                newReq.getServletResponse(), null, null);

        assertTrue(authReq.isUserInRole("Author"));
        assertTrue(authReq.isUserInRole("Editor"));
        assertTrue(authReq.isUserInRole("QA"));
    }
}

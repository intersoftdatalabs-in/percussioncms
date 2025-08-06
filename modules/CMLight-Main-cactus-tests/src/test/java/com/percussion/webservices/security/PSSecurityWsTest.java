// REFACTORED: CP-JAVA11
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
package com.percussion.webservices.security;

import com.percussion.security.PSSecurityToken;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequest;
import com.percussion.services.security.PSJaasUtils;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.request.PSRequestInfo;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import javax.security.auth.Subject;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for methods of the {@link IPSSecurityWs} class that are not exposed via
 * web services and thus not covered by the web service unit tests.
 */
@Tag("IntegrationTest")
class PSSecurityWsTest {

    @Test
    @DisplayName("Authentication and Session Context")
    void testAuthentication() throws Exception {
        var svc = PSSecurityWsLocator.getSecurityWebservice();
        // Assert we are anonymous
        validateAnonymous(svc.getRequestContext());

        // Log in as editor1
        var user = "editor1";
        svc.login(user, "demo", null, null);

        // Validate all user thread and session info
        var ctx = svc.getRequestContext();
        validateUser(user, ctx);

        // Login as admin1
        user = "admin1";
        svc.login(user, "demo", null, null);
        validateUser(user, svc.getRequestContext());

        // Restore context
        svc.restoreRequestContext(ctx);
        validateUser("editor1", ctx);

        var tok = svc.getSecurityToken();
        final Exception[] exArr = new Exception[] {null};

        // Launch thread and test session reconnect
        Runnable test = () -> {
            try {
                validateAnonymous(svc.getRequestContext());
                svc.reconnectSession(tok);
                validateUser("editor1", svc.getRequestContext());
            } catch (Exception e) {
                exArr[0] = e;
            }
        };

        var t = new Thread(test);
        t.setDaemon(true);
        t.start();
        t.join();
        if (exArr[0] != null)
            throw new RuntimeException("Runnable test failed", exArr[0]);
    }

    /**
     * Validates that the current user thread represents an anonymous user.
     *
     * @param ctx The request context to use, if {@code null}, assumes that
     *            the current user thread has no request info or session associated with it.
     */
    private void validateAnonymous(IPSRequestContext ctx) {
        if (ctx == null)
            return;

        var user = ctx.getUserName();
        assertTrue(StringUtils.isBlank(user), "Current session should not be authenticated");
        assertEquals(user, PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER));
        var sub = (Subject) PSRequestInfo.getRequestInfo(PSRequestInfo.SUBJECT);
        assertTrue(sub == null || PSJaasUtils.subjectToPrincipal(sub) == null);
    }

    /**
     * Validate the specified user is represented correctly by the supplied request context.
     *
     * @param user The user to check for, assumed not {@code null} or empty.
     * @param ctx  The context to check, assumes a {@code null} value indicates no request context associated with the current thread.
     */
    private void validateUser(String user, IPSRequestContext ctx) {
        assertNotNull(ctx);
        assertEquals(user, ctx.getUserName());
        var req = PSRequest.getRequest(ctx);
        assertEquals(user, req.getUserSession().getRealAuthenticatedUserEntry());
        assertSame(req, PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST));
        assertEquals(user, PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER));
        var sub = (Subject) PSRequestInfo.getRequestInfo(PSRequestInfo.SUBJECT);
        assertNotNull(sub);
        Principal userPrincipal = PSJaasUtils.subjectToPrincipal(sub);
        assertNotNull(userPrincipal);
        assertEquals(user, userPrincipal.getName());
        assertEquals(req.getUserSession().getSessionObject(IPSHtmlParameters.SYS_LANG),
                PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_LOCALE));
    }
}

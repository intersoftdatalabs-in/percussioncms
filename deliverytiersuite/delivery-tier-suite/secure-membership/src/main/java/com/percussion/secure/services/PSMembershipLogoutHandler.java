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
// REFACTORED: CP-JAVA11
package com.percussion.secure.services;

import com.percussion.secure.data.PSMembershipConfiguration;
import org.json.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Handles logging out of membership and session cookie cleanup.
 *
 * @author Jay Seletz
 */
public class PSMembershipLogoutHandler extends SimpleUrlLogoutSuccessHandler {
    private static final Client msClient = ClientBuilder.newClient();
    private PSMembershipConfiguration membershipConfig;

    public void setMembershipConfig(PSMembershipConfiguration membershipConfig) {
        this.membershipConfig = membershipConfig;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        try {
            // Log out of membership service
            var sessionId = getSessionId(request);
            if (sessionId != null && !sessionId.isEmpty()) {
                var webTarget = msClient.target(membershipConfig.getBaseUrl() +
                        "/perc-membership-services/membership/logout/" + sessionId);
                var invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
                var resp = invocationBuilder.get();
                var queryResponse = resp.readEntity(String.class);
                if (resp.getStatus() != 200) {
                    logger.error("Logout call to membership service failed : " + resp.getStatus());
                } else {
                    var resultObj = new JSONObject(queryResponse);
                    var status = resultObj.getString("status");
                    var message = resultObj.getString("message");
                    if (!"SUCCESS".equals(status)) {
                        logger.error("Error logging out of membership service, status: " + status + ", message: " + message);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error logging out of membership service {}", e);
            logger.debug(e.getMessage(), e);
        }
        // Let superclass handle the redirect
        super.handle(request, response, authentication);
    }

    /**
     * Gets the current session id from the cookie in the request.
     *
     * @param request Assumed not null
     * @return The id, null if not found.
     */
    private String getSessionId(HttpServletRequest request) {
        String sessionId = null;
        var sessionCookieName = membershipConfig.getMembershipSessionCookieName();
        var cookies = request.getCookies();
        for (var cookie : cookies) {
            if (sessionCookieName.equals(cookie.getName())) {
                sessionId = cookie.getValue();
                break;
            }
        }
        return sessionId;
    }
}

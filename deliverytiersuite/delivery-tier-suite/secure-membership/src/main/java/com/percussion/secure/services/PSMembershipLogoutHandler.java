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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import org.json.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

/**
 * Handles logging out of membership and session cookie cleanup.
 *
 * @author Jay Seletz
 * @deprecated This class is part of the deprecated secure-membership module.
 */
@Deprecated
public class PSMembershipLogoutHandler extends SimpleUrlLogoutSuccessHandler {
  private static final Client msClient = ClientBuilder.newClient();
  private PSMembershipConfiguration membershipConfig;

  /** No-op default constructor. */
  public PSMembershipLogoutHandler() {}

  /**
   * Sets the membership-service configuration this handler reads to call the membership logout
   * endpoint.
   *
   * @param membershipConfig the membership-service configuration, assumed not {@code null}.
   */
  public void setMembershipConfig(PSMembershipConfiguration membershipConfig) {
    this.membershipConfig = membershipConfig;
  }

  @Override
  public void onLogoutSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    try {
      // Log out of membership service
      var sessionId = getSessionId(request);
      if (sessionId != null && !sessionId.isEmpty()) {
        var webTarget =
            msClient.target(
                membershipConfig.getBaseUrl()
                    + "/perc-membership-services/membership/logout/"
                    + sessionId);
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
            logger.error(
                "Error logging out of membership service, status: "
                    + status
                    + ", message: "
                    + message);
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

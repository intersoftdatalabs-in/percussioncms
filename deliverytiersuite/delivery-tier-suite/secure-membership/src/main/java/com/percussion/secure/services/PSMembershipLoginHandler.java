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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Handles setting the membership session cookie after successful authentication.
 *
 * @author Jay Seletz
 * @deprecated This class is part of the deprecated secure-membership module.
 */
@Deprecated
public class PSMembershipLoginHandler extends SavedRequestAwareAuthenticationSuccessHandler {
  private PSMembershipConfiguration membershipConfig;

  public void setMembershipConfig(PSMembershipConfiguration membershipConfig) {
    this.membershipConfig = membershipConfig;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    if (membershipConfig.getUseLdap() == null
        || membershipConfig.getUseLdap().equalsIgnoreCase("no")) {
      // Get the session id and set the cookie
      var sessionId = PSMembershipAuthProvider.getAuthenticatedSessionId();
      if (sessionId != null) {
        var cookie = new Cookie(membershipConfig.getMembershipSessionCookieName(), sessionId);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
      }
    } else {
      super.setUseReferer(false);
    }
    // Let handling pass through to base class
    super.onAuthenticationSuccess(request, response, authentication);
  }
}

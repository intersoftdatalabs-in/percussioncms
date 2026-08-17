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
package com.percussion.servlets;

import static com.percussion.servlets.PSSecurityFilter.initRequest;
import static com.percussion.servlets.PSSecurityFilter.isAuthenticated;

import com.percussion.cms.PSAuthenticateUserUtils;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestContext;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.PSRoleMgrLocator;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.request.PSRequestInfo;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PSSetCommunityFilter implements Filter {
  private static final Logger log = LogManager.getLogger(PSSetCommunityFilter.class);

  /**
   * Filter the request, if the session of the request has been authenticated, then make sure the
   * community ID is set on the session. Do nothing if the session has not been authenticated.
   *
   * @param request servlet request, never <code>null</code>
   * @param response servlet response, never <code>null</code>
   * @param chain the next request in the chain, never <code>null</code>.
   * @throws IOException
   * @throws ServletException
   */
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    setCommunityIfNeeded(request, response);
    chain.doFilter(request, response);
  }

  /**
   * Sets the community ID if the session of the request has been authenticated and the community ID
   * has not been set to the session.
   *
   * @param request the servlet request, assumed not <code>null</code>.
   * @param response the servlet response, assumed not <code>null</code>.
   */
  private void setCommunityIfNeeded(ServletRequest request, ServletResponse response) {
    if (!(request instanceof HttpServletRequest)) {
      return;
    }

    HttpServletRequest httpReq = (HttpServletRequest) request;
    HttpServletResponse httpResp = (HttpServletResponse) response;

    if (!isAuthenticated(httpReq)) return;

    boolean needsReset = false;
    try {
      if (!PSRequestInfo.isInited()) {
        PSRequestInfo.initRequestInfo(httpReq);
        needsReset = true;
        log.info("Need to reset Request ID: {}", httpReq.getRemoteUser());
      }

      PSRequest psReq = initRequest(httpReq, httpResp);
      PSRequestContext reqCtx = new PSRequestContext(psReq);

      String communityId = (String) reqCtx.getSessionPrivateObject(IPSHtmlParameters.SYS_COMMUNITY);
      if (StringUtils.isNotBlank(communityId) && NumberUtils.toInt(communityId) > 0) {
        return; // community ID has already been set on the session.
      }

      String communityName;
      try {
        communityName = PSAuthenticateUserUtils.resolveDefaultCommunityName(reqCtx);
      } catch (Exception e) {
        log.debug("Cannot resolve default community for user: {}", reqCtx.getUserName(), e);
        return;
      }
      if (StringUtils.isBlank(communityName)) {
        log.debug("Cannot find a default community for user: {}", reqCtx.getUserName());
        return; // do nothing if cannot find default community.
      }

      // Profile-stored default applies only while the user still has access.
      if (psReq.getUserSession() != null) {
        List<String> allowed = psReq.getUserSession().getUserCommunityNames(psReq);
        if (allowed != null
            && !allowed.isEmpty()
            && !PSAuthenticateUserUtils.isCommunityAllowed(communityName, allowed)) {
          log.debug(
              "Stored default community {} is not allowed for user {}",
              communityName,
              reqCtx.getUserName());
          return;
        }
      }

      IPSBackEndRoleMgr mgr = PSRoleMgrLocator.getBackEndRoleManager();
      List<PSCommunity> communities = mgr.findCommunitiesByName(communityName);
      if (communities.isEmpty()) {
        log.error("Cannot find the community named: {}", communityName);
        return; // do nothing if cannot find default community.
      }

      reqCtx.setSessionPrivateObject(
          IPSHtmlParameters.SYS_COMMUNITY, "" + communities.get(0).getGUID().getUUID());
    } catch (Exception e) {
      log.error("Failed to set community to the session.", e);
    } finally {
      if (needsReset) {
        PSRequestInfo.resetRequestInfo();
      }
    }
  }

  /*
   * //see base interface method for details
   */
  public void init(FilterConfig config) throws ServletException {
    // do nothing for now.
  }

  /*
   * //see base interface method for details
   */
  public void destroy() {
    // NOOP
  }
}

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
// REFACTORED: CP-JAVA11
package com.percussion.utils.security;

import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.PSDeliveryInfoServiceLocator;
import com.percussion.delivery.service.impl.PSDeliveryInfoService;
import com.percussion.utils.tools.PSPatternMatcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Java 11 refactored: Security header filter for servlet requests.
 * <p>
 * Uses Google Java Style and modern Java 11 features.
 * <p>
 * All fields are immutable where possible. Thread-safe and stateless.
 */
public class PSSecurityHeaderFilter implements Filter {
    private PSDeliveryInfoService psDeliveryInfoService;
    private List<PSDeliveryInfo> psDeliveryInfoServiceList;
    private String policy = null;
    static PSPatternMatcher ms_matcher = new PSPatternMatcher('?', '*', null);
    private final PSSecurityUtility securityUtil = new PSSecurityUtility();

    /**
     * Does nothing.
     */
    @Override
    public void destroy() {
        // No-op
    }

    /**
     * Applies security headers to the HTTP response.
     * @param request the servlet request
     * @param response the servlet response
     * @param next the filter chain
     * @throws IOException if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain next) throws IOException, ServletException {
        var httpReq = (HttpServletRequest) request;
        var httpResp = (HttpServletResponse) response;

        if (securityUtil.xFrameOptionsRequired()) {
            if (!httpResp.containsHeader(PSSecurityUtility.HEADER_XFRAMEOPTIONS)) {
                // add the header in the response
                httpResp.addHeader(PSSecurityUtility.HEADER_XFRAMEOPTIONS, securityUtil.getXFrameOptions());
            }
        }

        if (securityUtil.xXSSProtectionRequired()) {
            if (!httpResp.containsHeader(PSSecurityUtility.HEADER_XSSPROTECTION)) {
                // add the header in the response
                httpResp.addHeader(PSSecurityUtility.HEADER_XSSPROTECTION, securityUtil.getXXSSProtection());
            }
        }

        if (securityUtil.xContentTypeOptionsRequired()) {
            if (!httpResp.containsHeader(PSSecurityUtility.HEADER_XCONTENTTYPEOPTIONS)) {
                // add the header in the response
                httpResp.addHeader(PSSecurityUtility.HEADER_XCONTENTTYPEOPTIONS, securityUtil.getXContentTypeOptions());
            }
        }

        if (securityUtil.contentSecurityPolicyRequired()) {
            if (!httpResp.containsHeader(PSSecurityUtility.HEADER_CONTENTSECURITY_POLICY)) {
                // add the header in the response

                if (policy == null) {
                    policy = securityUtil.getContentSecurityPolicy();
                    psDeliveryInfoService = (PSDeliveryInfoService) PSDeliveryInfoServiceLocator.getDeliveryInfoService();
                    psDeliveryInfoServiceList = psDeliveryInfoService.findAll();
                    policy = PSContentSecurityPolicyUtils.editContentSecurityPolicy(psDeliveryInfoServiceList, policy);
                }

                httpResp.addHeader(PSSecurityUtility.HEADER_CONTENTSECURITY_POLICY, policy);
            }
        }

        if (securityUtil.httpsRequired() && httpReq.isSecure()) {
            if (securityUtil.strictTransportSecurityRequired()) {

                if (!httpResp.containsHeader(PSSecurityUtility.HEADER_STRICTTRANSPORTSECURITY)) {
                    // add the header in the response
                    httpResp.addHeader(PSSecurityUtility.HEADER_STRICTTRANSPORTSECURITY, PSSecurityUtility.HEADER_MAXAGE + "=" + securityUtil.getStrictTransportSecurityMaxAge());
                }
            }
        }

        boolean statusRequest = ms_matcher.doesMatchPattern("/sessioncheck", httpReq.getServletPath());

        if (statusRequest) {
            //Always turn off caching on session url
            httpResp.setHeader(PSSecurityUtility.HEADER_CACHE_CONTROL, PSSecurityUtility.CACHE_CONTROL_SESSION);
        } else {
            if (securityUtil.isCacheControlRequired())
                httpResp.setHeader(PSSecurityUtility.HEADER_CACHE_CONTROL, securityUtil.getCacheControl());
        }

        next.doFilter(request, response);
    }

    /**
     * Initializes the filter configuration.
     * @param filterConfig the filter configuration
     * @throws ServletException if initialization fails
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No-op
    }
}

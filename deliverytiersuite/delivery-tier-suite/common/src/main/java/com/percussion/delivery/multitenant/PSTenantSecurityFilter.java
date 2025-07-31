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
package com.percussion.delivery.multitenant;

import com.percussion.delivery.multitenant.IPSTenantAuthorization.Status;
import com.percussion.error.PSExceptionUtils;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * Intercepts requests, extracts the tenant ID, authorizes it, and sets it on the tenant context.
 * Caches authorizations to ensure request performance.
 */
@Deprecated
public class PSTenantSecurityFilter implements Filter {

    private static final Logger log = LogManager.getLogger(PSTenantSecurityFilter.class);

    private IPSTenantAuthorization tenantAuth;
    private IPSTenantCache cache;

    public static final String TENANTID_PARAM_NAME = "perc-tid";
    public static final String DEFAULT_COMPANY = "Percussion Software";

    /**
     * Creates a new security filter.
     *
     * @param tenantAuth cannot be {@code null}
     * @param tenantCache cannot be {@code null}
     */
    public PSTenantSecurityFilter(IPSTenantAuthorization tenantAuth, IPSTenantCache tenantCache) {
        if (tenantAuth == null)
            log.warn("tenantAuth cannot be null. Skipping authorization filter.");
        else {
            this.tenantAuth = tenantAuth;
            log.debug("Tenant Authorization Initialized.");
        }

        if (tenantCache == null)
            log.warn("tenantCache cannot be null. Skipping authorization filter.");
        else {
            this.cache = tenantCache;
            log.debug("Tenant Authorization Caching Initialized.");
        }
    }

    @Override
    public void destroy() {
        log.debug("Tenant Security Filter Destroyed.");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        var tenantid = extractTenantId(req);
        PSTenantInfo t = null;

        if (tenantAuth != null && cache != null) {
            if (tenantid == null)
                tenantid = "not-specified";

            log.debug("Applying filter to tenant {}", tenantid);

            boolean netsuite = false;
            if (req instanceof HttpServletRequest) {
                var pathInfo = ((HttpServletRequest) req).getPathInfo();
                if (pathInfo != null && (pathInfo.contains("/netsuite/") || tenantid.equals("1")))
                    netsuite = true;
                else if (tenantid.equals("1"))
                    netsuite = true;
            }

            if (!netsuite) {
                t = (PSTenantInfo) cache.get(tenantid, req);

                if (t == null) {
                    if (log.isDebugEnabled())
                        log.debug("Authorizing tenant {}", tenantid);

                    var ret = tenantAuth.authorize(tenantid, 1, req);
                    if (ret.getStatusCode() == Status.SUCCESS || ret.getStatusCode() == Status.UNEXPECTED_ERROR) {
                        if (log.isDebugEnabled())
                            log.debug("Authorized tenant {}", tenantid);

                        if (ret.getStatusCode() == Status.SUCCESS) {
                            t = new PSTenantInfo();
                            t.setAPIUsageStart(new Date());
                            t.setLastAuthorizationCheckDate(new Date());
                            t.setTenantId(tenantid);
                            t.clearAPIUsage();
                            t.addAPIUsage(1);
                            t.setLicenseStatus(ret);

                            if (log.isDebugEnabled())
                                log.debug("Caching tenant authorization for tenant {}", tenantid);
                            cache.put(t);
                        }

                        if (log.isDebugEnabled())
                            log.debug("Setting tenant context to {}", tenantid);

                        PSThreadLocalTenantContext.setTenantId(tenantid);
                    } else {
                        if (log.isDebugEnabled())
                            log.debug("Authorization failed for tenant {} Status is {}", tenantid, ret.getLicenseStatus());

                        if (resp instanceof HttpServletResponse) {
                            ((HttpServletResponse) resp).sendError(HttpServletResponse.SC_FORBIDDEN, ret.getLicenseStatus());
                            return;
                        }

                        resp.getWriter().println("403 Forbidden: " + ret.getLicenseStatus());
                        resp.flushBuffer();
                        return;
                    }
                } else {
                    logUsage(t, req);
                    PSThreadLocalTenantContext.setTenantId(tenantid);

                    if (log.isDebugEnabled())
                        log.debug("Setting Tenant Context to {}", tenantid);
                }
            } else {
                log.debug("Skipping authorization for Percussion Tenant or NetSuite Service for tenant {}", tenantid);
                PSThreadLocalTenantContext.setTenantId(tenantid);
                log.debug("Tenant Context set to {}", tenantid);
            }
        } else {
            PSThreadLocalTenantContext.clearTenantId();
        }

        chain.doFilter(req, resp);
    }

    @Override
    public void init(FilterConfig config) {
        log.info("Tenant Security Filter initialized.");
    }

    /**
     * Extracts the tenant ID from the request header or query string parameter.
     * Header takes precedence.
     *
     * @param req servlet request
     * @return tenant ID if found, or {@code null} if not found
     */
    private String extractTenantId(ServletRequest req) {
        String tenantid = null;
        if (req instanceof HttpServletRequest) {
            tenantid = ((HttpServletRequest) req).getHeader(TENANTID_PARAM_NAME);
        }
        if (tenantid == null)
            tenantid = req.getParameter(TENANTID_PARAM_NAME);

        if (tenantid == null || tenantid.trim().isEmpty())
            tenantid = null;

        return tenantid;
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            log.debug("It's a number {}", str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * Records usage with the Metrics service.
     */
    private void logUsage(IPSTenantInfo t, ServletRequest req) {
        try {
            Validate.notNull(t);
            Validate.notNull(req);
            String method = "";

            if (req instanceof HttpServletRequest) {
                var r = (HttpServletRequest) req;
                if (r.getPathInfo() != null)
                    method = r.getPathInfo().replace("/", ".");

                if (!method.isEmpty() && isNumeric(method.substring(method.lastIndexOf(".") + 1))) {
                    method = method.substring(0, method.lastIndexOf("."));
                }

                if (t.getLicenseStatus().getCompany() == null || t.getLicenseStatus().getCompany().isEmpty())
                    t.getLicenseStatus().setCompany(DEFAULT_COMPANY);
            }
        } catch (Exception e) {
            log.debug("Error logging metrics: {}", PSExceptionUtils.getMessageForLog(e));
        }
    }

    /**
     * Returns the authorization cache used by the filter.
     */
    public IPSTenantCache getCache() {
        return cache;
    }

    /**
     * Sets the cache used by the filter.
     */
    public void setCache(IPSTenantCache cache) {
        this.cache = cache;
    }
}

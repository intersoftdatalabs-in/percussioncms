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
package com.percussion.tomcat.filters;

import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Component
public class PSSecurityFilter extends GenericFilterBean {

    private static final Logger log = LogManager.getLogger(PSSecurityFilter.class);

    private static final String PERC_SECURITY_PROPS_ROOT = "/conf/perc/perc-security.properties";
    private static final String CONTENT_SECURITY_POLICY_NAME = "contentSecurityPolicy";
    private static final String CATALINA_BASE = "catalina.base";
    private String contentSecurityPolicyValue = "default-src 'self'";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            var httpResp = (HttpServletResponse) response;
            httpResp.addHeader(CONTENT_SECURITY_POLICY_NAME, contentSecurityPolicyValue);
            chain.doFilter(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    protected void initFilterBean() throws ServletException {
        var props = new Properties();
        var tomcatBase = System.getProperty(CATALINA_BASE);
        if (tomcatBase != null) {
            try (var in = new FileInputStream(tomcatBase + PERC_SECURITY_PROPS_ROOT)) {
                props.load(in);
            } catch (IOException e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
        }

        var val = props.getProperty(CONTENT_SECURITY_POLICY_NAME);
        if (val != null && !val.isBlank()) {
            contentSecurityPolicyValue = val;
        }
    }

    private Properties readPropertiesFile(String fileName) throws IOException {
        var prop = new Properties();
        try (var fis = new FileInputStream(fileName)) {
            prop.load(fis);
        } catch (IOException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return prop;
    }
}

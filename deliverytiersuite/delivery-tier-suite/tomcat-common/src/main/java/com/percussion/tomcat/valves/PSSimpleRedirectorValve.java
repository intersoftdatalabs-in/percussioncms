package com.percussion.tomcat.valves;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * Simple redirector filter for Jakarta Servlet environments.
 */
public class PSSimpleRedirectorValve implements Filter
{
    private static final Logger log = LogManager.getLogger(PSSimpleRedirectorValve.class);

    private String targetHost = "localhost";
    private String serviceNames;
    private String[] servletUrls;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        String targetHostParam = filterConfig.getInitParameter("targetHost");
        if (targetHostParam != null && !targetHostParam.trim().isEmpty()) {
            this.targetHost = targetHostParam.trim();
        }
        String serviceNamesParam = filterConfig.getInitParameter("serviceNames");
        if (serviceNamesParam != null && !serviceNamesParam.trim().isEmpty()) {
            this.serviceNames = serviceNamesParam.trim();
        }

        if (serviceNames == null) {
            servletUrls = new String[0];
            return;
        }

        StringTokenizer toker = new StringTokenizer(serviceNames, ",");
        Collection<String> urls = new ArrayList<>();
        while (toker.hasMoreTokens()) {
            String s = toker.nextToken().trim();
            if (!s.startsWith("/"))
                s = "/" + s;
            if (!s.endsWith("/"))
                s = s + "/";
            urls.add(s);
        }
        servletUrls = urls.toArray(new String[0]);
        log.info("Redirecting to " + targetHost + " for the following paths: " + Arrays.toString(servletUrls));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String serverName = httpRequest.getServerName();
        String path = httpRequest.getRequestURI();

        boolean matched = false;
        if (!targetHost.equals(serverName)) {
            for (String servletUrl : servletUrls) {
                if (path.startsWith(servletUrl)) {
                    matched = true;
                    String redirectUrl = httpRequest.getScheme() + "://" + targetHost + path;
                    if (httpRequest.getQueryString() != null) {
                        redirectUrl += "?" + httpRequest.getQueryString();
                    }
                    log.info("Redirecting request from {} to {}", serverName, redirectUrl);
                    httpResponse.sendRedirect(redirectUrl);
                    return;
                }
            }
        }

        if (!matched) {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy()
    {
        // No resources to clean up
    }
}
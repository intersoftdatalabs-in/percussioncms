package com.percussion.tomcat.valves;

import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import static jakarta.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;

/**
 * A filter that performs redirection of requests for a specified context
 * to a different application context based upon the version of the
 * application.
 *
 * The mappings are controlled by the contents of the version-map.properties
 * file specified by the mappingFile attribute of the Filter.
 */
public class PSMultiAppVersionRedirectorValve implements Filter {

    public static String PERC_VERSION_HEADER = "perc-version";

    private static final Logger log = LogManager.getLogger(PSMultiAppVersionRedirectorValve.class);

    //Contains a file system pointer to the mapping configuration file
    private String mappingFile;

    //Contains the mapping properties.
    private Properties properties = new Properties();

    //When true routing logic is attempted, when false it is skipped.
    protected ThreadLocal<Boolean> pipelining = new ThreadLocal<>();
    private PSVersionRoutingTable routingTable = new PSVersionRoutingTable();

    boolean started;

    public boolean isStarted() {
        return started;
    }

    public String getMappingFile() {
        return this.mappingFile;
    }

    public void setMappingFile(String mappingFile) {
        this.mappingFile = mappingFile;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        started = false;
        log.debug("init");
        log.info("Starting Multi App Version Redirector Filter");

        String mappingFileParam = filterConfig.getInitParameter("mappingFile");
        if (mappingFileParam != null) {
            this.mappingFile = mappingFileParam;
        }

        if (mappingFile != null) {
            try {
                File file = new File(mappingFile);
                try (FileInputStream fis = new FileInputStream(file)) {
                    properties.load(fis);
                }
            } catch (FileNotFoundException e) {
                log.warn("Could not find the version Mapping file specified: {} Multi Version Routing is disabled. Error: {}",
                        mappingFile,
                        PSExceptionUtils.getMessageForLog(e));
            } catch (IOException e) {
                log.warn("Could not access the version Mapping file specified: {}. Error: {}. Multi Version Routing is disabled.",
                        mappingFile,
                        PSExceptionUtils.getMessageForLog(e));
            }

            //Try to parse out the property file.
            try {
                Enumeration<?> e = properties.propertyNames();

                while (e.hasMoreElements()) {

                    String context = (String) e.nextElement();
                    String[] map = properties.getProperty(context).split(",");

                    routingTable.addServiceContextVersionMap(context,
                            map[0],
                            map[1]);
                }

                //if we got this far then we have a valid routine table.
                started = true;
                log.info("Routing Table initialized");

            } catch (Exception e) {
                log.error("Unable to initialize routing tables.", e);
            }
        }
        started = true;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        log.debug("doFilter");

        if (!(servletRequest instanceof HttpServletRequest) || !(servletResponse instanceof HttpServletResponse)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (pipelining.get() == Boolean.TRUE) {
            chain.doFilter(request, response);
            pipelining.remove();
            return;
        }

        //Only apply routing logic if the filter is properly initialized.
        if (started) {
            pipelining.set(Boolean.TRUE);
            String context = routingTable.determineRoute(request.getContextPath(),
                    request.getHeader(PERC_VERSION_HEADER));

            if (!context.startsWith("/"))
                context = "/" + context;

            //Make sure we don't re-route if the context is the same as the target.
            if (!context.equals(request.getContextPath())) {

                StringBuffer sbUrl = request.getRequestURL();
                String sQueryString = request.getQueryString();

                if (sQueryString != null) {
                    sbUrl.append("?");
                    sbUrl.append(sQueryString);
                }

                String sUrl = sbUrl.toString().replace(request.getContextPath(), context);

                response.setStatus(SC_MOVED_PERMANENTLY);
                response.setHeader("Location",
                        response.encodeRedirectURL(sUrl));
                pipelining.remove();
                return;
            }

            chain.doFilter(request, response);
        } else {
            chain.doFilter(request, response);
        }

        //Make sure thread local is cleared.
        pipelining.remove();
    }

    @Override
    public void destroy() {
        started = false;
    }
}
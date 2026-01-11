/*
 * Minimal CXF-backed helper scaffold for migration from Axis.
 */
package com.percussion.integration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URL;

import javax.xml.ws.Service;

/**
 * Temporary CXF-backed helper scaffold. Initial implementation is a placeholder
 * and will be expanded to call generated CXF client stubs.
 */
public class PSWsHelperCxf extends PSWsHelperBase implements IPSWsHelper
{
    public PSWsHelperCxf(ServletContext context, HttpServletRequest req, HttpServletResponse resp) throws Exception
    {
        init(context, req, resp, null);
    }

    public PSWsHelperCxf(ServletContext context, HttpServletRequest req, HttpServletResponse resp, URL targetEndpoint) throws Exception
    {
        init(context, req, resp, targetEndpoint);
    }

    // TODO: implement CXF-based client initialization and port wiring using
    // generated stubs from modules/webservices. For now throw to indicate
    // methods are not yet migrated.

}
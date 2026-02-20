package com.percussion.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.percussion.system.utils.IPSHtmlParameters;
import org.junit.jupiter.api.Test;

public class PSWsHelperCxfTest
{
    @Test
    public void testExecuteCallDirect_get() throws Exception
    {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/sys_webServicesHandler/test", exchange -> {
            String resp = "OK";
            exchange.sendResponseHeaders(200, resp.length());
            exchange.getResponseBody().write(resp.getBytes());
            exchange.close();
        });
        server.start();
        try
        {
            int port = server.getAddress().getPort();
            PSWsHelperCxf helper = new PSWsHelperCxf();
            helper.setPortURL(new URL("http://localhost:" + port + "/sys_webServicesHandler"));

            String r = helper.executeCallDirect((String) null, (String) null, "/test", new HashMap());
            assertTrue(r.contains("OK"));
        }
        finally
        {
            server.stop(0);
        }
    }

    @Test
    public void testGetInbox_parses_items() throws Exception
    {
        String inboxXml = "<View>" +
            "<Item sys_contentid=\"101\"/>" +
            "<Item sys_contentid=\"102\"/>" +
            "</View>";
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/sys_cxViews/inbox.xml", exchange -> {
            byte[] resp = inboxXml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.start();
        try
        {
            int port = server.getAddress().getPort();
            PSWsHelperCxf helper = new PSWsHelperCxf();
            helper.setPortURL(new URL("http://localhost:" + port + "/sys_webServicesHandler"));

            // Use username/password variant
            PSSearch results = helper.getInbox("u", "p", null, true);
            assertTrue(results.getItems().size() == 2);
            PSItem first = (PSItem) results.getItems().get(0);
            assertTrue(first.getContentId() == 101);
        }
        finally
        {
            server.stop(0);
        }
    }

    @Test
    public void testSearch_with_contentid() throws Exception
    {
        PSWsHelperCxf helper = new PSWsHelperCxf();
        PSSearch s = new PSSearch();
        PSItem q = new PSItem();
        q.addQueryField(IPSHtmlParameters.SYS_CONTENTID, "201,202", null, null);
        s.addItem(q);

        PSSearch out = helper.search((String) null, (String) null, s, null, true);
        assertTrue(out.getItems().size() == 2);
        PSItem i1 = (PSItem) out.getItems().get(0);
        assertTrue(i1.getContentId() == 201);
    }

    @Test
    public void testFormatEditorUrl_with_string_and_session() throws Exception
    {
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        req.setScheme("http");
        req.setServerName("localhost");
        req.setServerPort(8080);

        PSWsHelperCxf helper = new PSWsHelperCxf();
        java.util.Map types = new java.util.HashMap();
        types.put("mytype", "../sys_edit/mytype");

        String url = helper.formatEditorUrl(req, types, "mytype");
        // Should include edit command and session param
        assertTrue(url.contains("sys_command=edit"));
        assertTrue(url.contains(IPSHtmlParameters.SYS_SESSIONID));
    }

    @Test
    public void testFormatContentTypeList_and_isFtsEnabled() throws Exception
    {
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        req.setScheme("http");
        req.setServerName("localhost");
        req.setServerPort(8080);

        PSWsHelperCxf helper = new PSWsHelperCxf();
        java.util.Map mapSorted = helper.formatContentTypeList(req, true);
        assertTrue(mapSorted instanceof java.util.TreeMap);
        assertTrue(mapSorted.isEmpty());

        boolean fts = helper.isFtsEnabled(req);
        assertTrue(!fts);
    }
}

/*
 * Minimal CXF-backed helper scaffold for migration from Axis.
 */
package com.percussion.integration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URL;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Map;

import javax.xml.ws.Service;

import com.percussion.system.utils.IPSHtmlParameters;

/**
 * Temporary CXF-backed helper scaffold. Initial implementation is a placeholder
 * and will be expanded to call generated CXF client stubs.
 */
public class PSWsHelperCxf extends PSWsHelperBase implements IPSWsHelper
{
    public PSWsHelperCxf() throws Exception
    {
        init(null, null, null, null);
    }

    public PSWsHelperCxf(ServletContext context, HttpServletRequest req, HttpServletResponse resp) throws Exception
    {
        init(context, req, resp, null);
    }

    public PSWsHelperCxf(ServletContext context, HttpServletRequest req, HttpServletResponse resp, URL targetEndpoint) throws Exception
    {
        init(context, req, resp, targetEndpoint);
    }

    private void init(ServletContext context, HttpServletRequest req, HttpServletResponse resp, URL targetEndpoint)
        throws Exception
    {
        // Load properties and default target endpoint
        m_targetEndpoint = loadProps(targetEndpoint);

        // Initialize DOM builder
        try {
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            m_db = dbf.newDocumentBuilder();
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new Exception("Failed to initialize XML parser", e);
        }

        // Store cookies from request if provided
        if (req != null)
            setAuthCookies(req);

        // Other initialization (handlers/interceptors) can go here
    }

    @Override
    public void setImagePath(String imagePath)
    {
        m_props.setProperty(IMAGE_PATH, imagePath);
    }

    @Override
    public String executeCallDirect(String username, String password, String appLocation, Map paramMap)
        throws RemoteException, Exception
    {
        // Build URL
        StringBuilder urlStr = new StringBuilder();
        urlStr.append(getPortURL().toExternalForm());
        if (!appLocation.startsWith("/"))
            urlStr.append('/');
        urlStr.append(appLocation);

        // Append params as query string (GET)
        if (paramMap != null && !paramMap.isEmpty())
        {
            boolean first = !urlStr.toString().contains("?");
            for (Object okey : paramMap.entrySet())
            {
                Map.Entry e = (Map.Entry) okey;
                if (first)
                {
                    urlStr.append('?');
                    first = false;
                }
                else
                {
                    urlStr.append('&');
                }
                urlStr.append(java.net.URLEncoder.encode(e.getKey().toString(), java.nio.charset.StandardCharsets.UTF_8.name()));
                urlStr.append('=');
                urlStr.append(java.net.URLEncoder.encode(String.valueOf(e.getValue()), java.nio.charset.StandardCharsets.UTF_8.name()));
            }
        }

        java.net.HttpURLConnection con = null;
        java.io.InputStream is = null;
        try
        {
            URL url = new URL(urlStr.toString());
            con = (java.net.HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setConnectTimeout(10_000);
            con.setReadTimeout(20_000);

            // Add cookies if available
            Cookie[] cookies = getAuthCookies();
            if (cookies != null && cookies.length > 0)
            {
                StringBuilder cookieHdr = new StringBuilder();
                for (int i=0; i<cookies.length; i++)
                {
                    if (i > 0)
                        cookieHdr.append("; ");
                    cookieHdr.append(cookies[i].getName()).append("=").append(cookies[i].getValue());
                }
                con.setRequestProperty("Cookie", cookieHdr.toString());
            }

            // Basic Auth if provided
            if (username != null && password != null)
            {
                String auth = username + ":" + password;
                String encoded = java.util.Base64.getEncoder().encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                con.setRequestProperty("Authorization", "Basic " + encoded);
            }

            int rc = con.getResponseCode();
            if (rc >= 200 && rc < 300)
            {
                is = con.getInputStream();
            }
            else
            {
                is = con.getErrorStream();
            }

            if (is == null)
                return null;

            java.io.BufferedReader rdr = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder resp = new StringBuilder();
            String line;
            while ((line = rdr.readLine()) != null)
            {
                resp.append(line).append('\n');
            }
            return resp.toString();
        }
        finally
        {
            if (is != null) try { is.close(); } catch(Exception ignore) {}
            if (con != null) con.disconnect();
        }
    }

    @Override
    public String executeCallDirect(HttpServletRequest req, String appLocation, Map paramMap)
        throws RemoteException, Exception
    {
        // Preserve cookies from request
        if (req != null)
            setAuthCookies(req);

        return executeCallDirect(null, null, appLocation, paramMap);
    }

    // Other IPSWsHelper methods are not yet implemented via dynamic client.
    // Throw RemoteException for now to indicate unimplemented.

    @Override
    public PSSearch getInbox(HttpServletRequest req, java.util.List fieldList, boolean includeActionMenu) throws RemoteException, Exception
    {
        if (req == null)
            throw new IllegalArgumentException("req may not be null");
        // preserve cookies
        setAuthCookies(req);
        String data = executeCallDirect((String) null, (String) null, "../sys_cxViews/inbox.xml", null);
        if (data == null)
            data = "<View></View>";
        org.w3c.dom.Document doc = toDOM(data);
        org.w3c.dom.Element el = doc.getDocumentElement();
        org.w3c.dom.NodeList nl = el.getElementsByTagName("Item");
        PSSearch ret = new PSSearch();
        for (int i = 0; i < nl.getLength(); i++)
        {
            org.w3c.dom.Element itemEl = (org.w3c.dom.Element) nl.item(i);
            String idStr = itemEl.getAttribute(IPSHtmlParameters.SYS_CONTENTID);
            try
            {
                int id = Integer.parseInt(idStr);
                PSItem item = new PSItem();
                item.addResultField(IPSHtmlParameters.SYS_CONTENTID, Integer.toString(id));
                if (includeActionMenu)
                {
                    String sessionid = getRhythmyxSession(req);
                    String val = getActionPageLink(id, sessionid);
                    item.addActionPageField(val);
                }
                ret.addItem(item);
            }
            catch (NumberFormatException ignore) {}
        }
        return ret;
    }
    @Override
    public PSSearch getInbox(String username, String password, java.util.List fieldList, boolean includeActionMenu) throws RemoteException, Exception
    {
        if (username == null || username.trim().length() == 0)
            throw new IllegalArgumentException("username may not be null or empty");
        if (password == null || password.trim().length() == 0)
            throw new IllegalArgumentException("password may not be null or empty");

        String data = executeCallDirect(username, password, "../sys_cxViews/inbox.xml", null);
        if (data == null)
            data = "<View></View>";
        org.w3c.dom.Document doc = toDOM(data);
        org.w3c.dom.Element el = doc.getDocumentElement();
        org.w3c.dom.NodeList nl = el.getElementsByTagName("Item");
        PSSearch ret = new PSSearch();
        for (int i = 0; i < nl.getLength(); i++)
        {
            org.w3c.dom.Element itemEl = (org.w3c.dom.Element) nl.item(i);
            String idStr = itemEl.getAttribute(IPSHtmlParameters.SYS_CONTENTID);
            try
            {
                int id = Integer.parseInt(idStr);
                PSItem item = new PSItem();
                item.addResultField(IPSHtmlParameters.SYS_CONTENTID, Integer.toString(id));
                if (includeActionMenu)
                {
                    String val = getActionPageLink(id);
                    item.addActionPageField(val);
                }
                ret.addItem(item);
            }
            catch (NumberFormatException ignore) {}
        }
        return ret;
    }
    @Override
    public String executeCallDirect(String username, String password, String appLocation, Map paramMap, boolean dummy) throws RemoteException, Exception
    {
        return executeCallDirect(username, password, appLocation, paramMap);
    }
    @Override
    public PSSearch search(HttpServletRequest req, PSSearch search, IPSSearchFilter filter, boolean includeActionMenu) throws RemoteException, Exception
    {
        if (req == null)
            throw new IllegalArgumentException("req may not be null");
        if (search == null)
            throw new IllegalArgumentException("search may not be null");
        setAuthCookies(req);
        // Delegate to username/password variant using session-less auth
        return search((String) null, (String) null, search, filter, includeActionMenu);
    }
    @Override
    public PSSearch search(String username, String password, PSSearch search, IPSSearchFilter filter, boolean includeActionMenu) throws RemoteException, Exception
    {
        if (search == null)
            throw new IllegalArgumentException("search may not be null");

        // Very small, pragmatic implementation: if the search contains a query
        // field for sys_contentid with operator 'in', return a PSSearch that
        // contains PSItems for each id. This unblocks inbox-based searches.
        java.util.List fields = search.getQuerySearchFields();
        PSSearch ret = new PSSearch();
        for (Object of : fields)
        {
            PSField f = (PSField) of;
            if (IPSHtmlParameters.SYS_CONTENTID.equalsIgnoreCase(f.getName()))
            {
                String val = f.getValue();
                if (val != null)
                {
                    String[] parts = val.split(",");
                    for (String p : parts)
                    {
                        try
                        {
                            int id = Integer.parseInt(p.trim());
                            PSItem item = new PSItem();
                            item.addResultField(IPSHtmlParameters.SYS_CONTENTID, Integer.toString(id));
                            if (includeActionMenu)
                            {
                                String sid = null;
                                if (username == null)
                                {
                                    // no session available
                                }
                                item.addActionPageField(getActionPageLink(id, sid));
                            }
                            ret.addItem(item);
                        }
                        catch (NumberFormatException ignore) {}
                    }
                }
                break;
            }
        }
        return ret;
    }
    @Override
    public Map formatContentTypeList(HttpServletRequest request, boolean sort) throws Exception, RemoteException
    {
        // Practical fallback: return an empty map (sorted or not) to avoid
        // depending on generated SOAP stubs. Callers should handle empty maps.
        if (sort)
            return new java.util.TreeMap();
        return new java.util.HashMap();
    }
    @Override
    public String formatEditorUrl(HttpServletRequest req, Map types, String type) throws MalformedURLException
    {
        // Try to pull content editor URL from the types map if possible.
        if (types != null)
        {
            Object o = types.get(type);
            if (o instanceof com.percussion.integration.webservices.design.ContentTypeListResponseContentType)
            {
                com.percussion.integration.webservices.design.ContentTypeListResponseContentType ct = (com.percussion.integration.webservices.design.ContentTypeListResponseContentType) o;
                return formatEditorUrl(req, ct);
            }
            else if (o instanceof String)
            {
                String url = (String) o;
                String formatted = formContentEditorURL(req, url);
                String sessionid = getRhythmyxSession(req);
                return formatted + "&" + IPSHtmlParameters.SYS_SESSIONID + "=" + sessionid;
            }
        }
        // Fallback: construct a default editor URL
        String formatted = getExternalUrl(req) + "/sys_edit/" + type + "?sys_command=edit&sys_view=sys_All";
        String sessionid = getRhythmyxSession(req);
        return formatted + "&" + IPSHtmlParameters.SYS_SESSIONID + "=" + sessionid;
    }
    @Override
    public String getTypeDescription(String type, Map types)
    {
        if (types == null)
            return null;
        Object o = types.get(type);
        if (o instanceof com.percussion.integration.webservices.design.ContentTypeListResponseContentType)
        {
            return ((com.percussion.integration.webservices.design.ContentTypeListResponseContentType) o).get_value();
        }
        else if (o instanceof String)
        {
            return (String) o;
        }
        return null;
    }
    @Override
    public boolean isFtsEnabled(HttpServletRequest request) throws SOAPException, RemoteException
    {
        // Conservative default: don't depend on SOAP search configuration during migration.
        return false;
    }

}
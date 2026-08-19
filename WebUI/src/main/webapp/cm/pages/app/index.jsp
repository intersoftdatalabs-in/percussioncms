<%@ page import="com.percussion.maintenance.service.impl.PSMaintenanceManager,com.percussion.pathmanagement.data.PSPathItem,com.percussion.pathmanagement.service.impl.PSPathService" %>
<%@ page import="com.percussion.role.service.impl.PSRoleService,com.percussion.sitemanage.data.PSSiteSummary" %>
<%@ page import="com.percussion.sitemanage.service.impl.PSSiteDataService" %>
<%@ page import="com.percussion.user.data.PSCurrentUser" %>
<%@ page import="com.percussion.user.service.impl.PSUserService" %>
<%@ page import="com.percussion.webui.util.PSDefaultLandingView" %>

<%@ page import="com.percussion.utils.PSSpringBeanProvider" %>
<%@ page import="com.percussion.utils.container.IPSConnector" %>
<%@ page import="com.percussion.utils.container.PSContainerUtilsFactory" %>

<%@ page import="com.percussion.widgetbuilder.service.PSWidgetBuilderService" %>
<%@ page import="org.apache.commons.lang3.ArrayUtils"  %>

<%@ page import="org.json.JSONArray" %>
<%@ page import="jakarta.servlet.http.Cookie" %>

<%@ page import="java.io.BufferedReader" %>

<%@ page import ="java.io.IOException" %>

<%@ page import=" java.io.InputStream" %>

<%@ page import="java.io.InputStreamReader" %>
<%@ page import="java.net.HttpURLConnection" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.percussion.server.PSServer" %>



<%
    setCurrentUserInfo(request, response);
    setSitesInfo(request, response);
    setWidgetBuilderActiveInfo(request, response);
    String linkback = request.getParameter("perc_linkback_id");

    String view = request.getParameter("view");
    String site = request.getParameter("site");
    String path = request.getParameter("path");
    String mode = request.getParameter("mode");
    String popuppage = request.getParameter("popuppage");
    String memento = request.getParameter("memento");
    String debug = request.getParameter("debug");
    // The default view to use if not specified
    String defaultView = getDefaultView(request, response);


    String proxyURL ="";
    if(PSServer.isRequestBehindProxy(request)) {
        proxyURL = PSServer.getProxyURL(request,true);
    }

    // Set attribute indicating  we have gone through the dispatcher.
    request.setAttribute("dispatched", "true");

    // Legacy full-page exits only. Modern views redirect to spa.jsp?entry=…
    // PR-7: dash is no longer legacy — gadgets live on Home (see dash → SPA below).
    Map<String, String> legacyViews = new HashMap<String, String>();
    legacyViews.put("editAsset", "editAsset.jsp");
    // design → SPA entry=design (#3306); admin.jsp hard-redirects (editTemplate.jsp stays)
    // arch / Architecture → SPA entry=architecture (#3094); siteArchitecture.jsp retired (#3587)
    legacyViews.put("editor", "webmgt.jsp");
    legacyViews.put("editTemplate", "editTemplate.jsp");

    // Modern SPA entries (query contract — never hash). *Modern.jsp is not product path.
    // "dash" is handled as SPA Home gadgets (PR-7 product lock).
    // "arch" / "architecture" → Architecture SPA shell (#3094).
    // "design" → Design SPA template library (#3306 / parent #2631).
    String[] spaViews = new String[]{
            "home",
            "publish",
            "workflow",
            "admin",
            "widgetbuilder",
            "developer",
            "explorer",
            "arch",
            "architecture",
            "navigation",
            "design",
            "dash"
    };

    // List of views requiring admin role.
    // home / dash / explorer are ungated spaViews — they must stay off this list
    // so Contributor and Designer stored landings are not reset (issue #3536).
    String[] adminViews = new String[]{
            "design",
            "arch",
            "architecture",
            "navigation",
            "publish",
            "workflow",
            "widgetbuilder",
            "developer",
            "admin"
    };

    // Subset of adminViews that Designer may open. Only consulted inside the
    // isAdminView gate below — listing an ungated view (explorer) here is a no-op.
    String[] designerViews = new String[]{
            "design",
            "arch",
            "architecture",
            "navigation",
            "publish",
            "widgetbuilder",
            "developer"
    };

    boolean isAdminView = (view != null && ArrayUtils.contains(adminViews, view));
    boolean isDesignerView =  (view != null && ArrayUtils.contains(designerViews, view));

    if(isAdminView && !(Boolean)request.getAttribute(IS_ADMIN_KEY))
    {
        if (isDesignerView)
        {
            if (!(Boolean)request.getAttribute(IS_DESIGNER_KEY))
                view = null; //reset view sending user to default view
        }
        else
        {
            // no designer access to workflow/admin view, send to default view
            view = null;
        }
    }



    String forwardTo = MAINT_ERROR_PAGE_URL;
    // Add the default view and redirect so it shows up in the url
    if (hasMaintenanceFailed(request, response))
    {
        response.sendRedirect(MAINT_ERROR_PAGE_URL);
    }
    else if (isMaintenanceInProgress(request, response))
    {
        response.sendRedirect(MAINT_PAGE_URL);
    }
    else if(view == null)
    {
        response.setHeader( "Pragma", "no-cache" );
        response.setHeader( "Cache-Control", "no-cache" );
        response.setDateHeader( "Expires", 0 );
        // Default homepage Home → SPA; dash/editor stay legacy exits
        if (ArrayUtils.contains(spaViews, defaultView))
        {
            response.sendRedirect(buildSpaEntryRedirect(proxyURL, defaultView, request));
        }
        else
        {
            Enumeration paramNames = request.getParameterNames();
            StringBuilder buff = new StringBuilder();
            int count = 0;
            while(paramNames.hasMoreElements())
            {
                String key = (String)paramNames.nextElement();
                // Skip null/blank keys (produces "?null=" garbage in redirect URL)
                if(key == null || key.isBlank() || "null".equalsIgnoreCase(key) || "view".equals(key))
                    continue;
                String value = request.getParameter(key);
                if(value == null)
                    value = "";
                buff.append(count == 0 ? "" : "&");
                buff.append(URLEncoder.encode(key, "UTF-8"));
                buff.append("=");
                buff.append(URLEncoder.encode(value, "UTF-8"));
                count++;

            }


            String sep = buff.length() == 0 ? "" : "&";
            // Canonical app shell is /cm/app/ (static assets + relative ../cssMin paths live there)
            String url = proxyURL+"/cm/app/?" + buff.toString() + sep + "view=" + defaultView;
            response.sendRedirect(url);
        }
    }
    else if(view.equals("popup") && popuppage != null)
    {
        String url = proxyURL+"/cm/app/popups/" + popuppage;
        response.sendRedirect(url);
    }
    else if(view.equals("editor") && linkback != null)
    {
        String url = proxyURL+"/cm/app/?view=editor";
        Map params = getItemEditorInfo(request,response);
        for (Object key : params.keySet())
        {
            url += "&" + key.toString() + "=" + params.get(key);
        }
        response.sendRedirect(url);
    }
    else if (ArrayUtils.contains(spaViews, view))
    {
        // Aggressive cutover: modern ?view= → proxyURL + spa.jsp?entry=… (query only)
        response.setHeader( "Pragma", "no-cache" );
        response.setHeader( "Cache-Control", "no-cache" );
        response.setDateHeader( "Expires", 0 );
        response.sendRedirect(buildSpaEntryRedirect(proxyURL, view, request));
    }
    else
    {
        // Known legacy views forward to mapped JSPs; unmapped/retired → SPA unavailable
        String temp = legacyViews.get(view);
        if(temp != null)
        {
            forwardTo = temp;
            pageContext.forward(forwardTo);
        }
        else
        {
            response.setHeader( "Pragma", "no-cache" );
            response.setHeader( "Cache-Control", "no-cache" );
            response.setDateHeader( "Expires", 0 );
            response.sendRedirect(buildSpaEntryRedirect(proxyURL, "unavailable", request));
        }
    }
%>
<%-- Define methods --%>
<%!

    /**
     * Build proxyURL-aware SPA entry redirect (query contract only — never #).
     * Allowlists deep-link params in lockstep with WebUI parseEntryQuery / allowlists.ts.
     */
    protected String buildSpaEntryRedirect(String proxyURL, String view, HttpServletRequest request)
            throws java.io.UnsupportedEncodingException
    {
        String entry;
        if ("widgetbuilder".equals(view))
            entry = "widget-builder";
        else if ("unavailable".equals(view))
            entry = "unavailable";
        else if ("dash".equals(view))
            // PR-7: former peer dashboard → Home gadgets (not spa entry=dashboard)
            entry = "home";
        else if ("workflow".equals(view))
            // #3088: fold legacy Workflow admin view into unified Admin shell
            entry = "admin";
        else if ("arch".equals(view) || "architecture".equals(view)
                || "navigation".equals(view))
            // #3094 / #3219: Architecture homepage / Navigation → SPA shell
            entry = "architecture";
        else if ("design".equals(view))
            // #3306: Design template list → SPA shell
            entry = "design";
        else if ("home".equals(view) || "publish".equals(view)
                || "admin".equals(view)
                || "developer".equals(view)
                || "explorer".equals(view))
            entry = view;
        else
            entry = "home";

        StringBuilder qs = new StringBuilder();
        qs.append("entry=").append(URLEncoder.encode(entry, "UTF-8"));

        if ("dash".equals(view))
        {
            qs.append("&section=").append(URLEncoder.encode("gadgets", "UTF-8"));
        }
        else if ("home".equals(view))
        {
            String section = firstAllowlisted(
                    request.getParameter("section"),
                    request.getParameter("initialScreen"),
                    HOME_SECTIONS,
                    HOME_SECTION_ALIASES);
            if (section != null)
                qs.append("&section=").append(URLEncoder.encode(section, "UTF-8"));
        }
        else if ("publish".equals(view))
        {
            String section = firstAllowlisted(
                    request.getParameter("section"),
                    null,
                    PUBLISH_SECTIONS,
                    PUBLISH_SECTION_ALIASES);
            if (section != null)
                qs.append("&section=").append(URLEncoder.encode(section, "UTF-8"));
            String siteId = allowId(request.getParameter("siteId"));
            if (siteId != null)
                qs.append("&siteId=").append(URLEncoder.encode(siteId, "UTF-8"));
            String serverId = allowId(request.getParameter("serverId"));
            if (serverId != null)
                qs.append("&serverId=").append(URLEncoder.encode(serverId, "UTF-8"));
        }
        else if ("workflow".equals(view))
        {
            // Map to Admin tab; default Workflow tab when no tab query
            String tab = firstAllowlisted(
                    request.getParameter("tab"),
                    request.getParameter("section"),
                    WORKFLOW_TABS,
                    null);
            if (tab == null)
                tab = "workflow";
            qs.append("&tab=").append(URLEncoder.encode(tab, "UTF-8"));
        }
        else if ("admin".equals(view))
        {
            String tab = firstAllowlisted(
                    request.getParameter("tab"),
                    request.getParameter("section"),
                    ADMIN_TABS,
                    null);
            if (tab != null)
                qs.append("&tab=").append(URLEncoder.encode(tab, "UTF-8"));
        }
        else if ("developer".equals(view))
        {
            String section = firstAllowlisted(
                    request.getParameter("section"),
                    request.getParameter("tab"),
                    DEVELOPER_SECTIONS,
                    DEVELOPER_SECTION_ALIASES);
            if (section != null)
                qs.append("&section=").append(URLEncoder.encode(section, "UTF-8"));
        }
        else if ("arch".equals(view) || "architecture".equals(view)
                || "navigation".equals(view))
        {
            // Optional site context for Architecture SPA (#3094 / #3219)
            String site = request.getParameter("site");
            if (site != null && !site.isBlank() && site.length() <= 128
                    && !site.contains("://") && !site.contains("..") && !site.contains("/"))
            {
                qs.append("&site=").append(URLEncoder.encode(site.trim(), "UTF-8"));
            }
        }
        else if ("design".equals(view))
        {
            // #3306: optional Design section (templates is the accepted list)
            String section = firstAllowlisted(
                    request.getParameter("section"),
                    request.getParameter("tab"),
                    DESIGN_SECTIONS,
                    DESIGN_SECTION_ALIASES);
            if (section != null)
                qs.append("&section=").append(URLEncoder.encode(section, "UTF-8"));
        }

        // Canonical SPA document lives under /cm/app/ (both trees redirect here)
        return (proxyURL == null ? "" : proxyURL) + "/cm/app/spa.jsp?" + qs.toString();
    }

    private String firstAllowlisted(String primary, String secondary, String[] allowed,
            Map<String, String> aliases)
    {
        String v = allowToken(primary, allowed, aliases);
        if (v != null)
            return v;
        return allowToken(secondary, allowed, aliases);
    }

    private String allowToken(String raw, String[] allowed, Map<String, String> aliases)
    {
        if (raw == null)
            return null;
        String n = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (n.isEmpty())
            return null;
        if (ArrayUtils.contains(allowed, n))
            return n;
        if (aliases != null && aliases.containsKey(n))
            return aliases.get(n);
        return null;
    }

    private String allowId(String raw)
    {
        if (raw == null)
            return null;
        String t = raw.trim();
        if (t.isEmpty() || t.length() > 128)
            return null;
        for (int i = 0; i < t.length(); i++)
        {
            char c = t.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-'))
                return null;
        }
        return t;
    }

    protected boolean isMaintenanceInProgress(HttpServletRequest request, HttpServletResponse response) throws JspException
    {
        try
        {
            PSMaintenanceManager maintenanceManager = (PSMaintenanceManager) PSSpringBeanProvider.getBean("maintenanceManager");
            return maintenanceManager.isWorkInProgress();
        }
        catch(Exception e)
        {
            throw new JspException(e);
        }
    }
    /**
     * Effective default {@code view=} for login / app entry when {@code view} is omitted.
     *
     * <p>Uses {@link PSRoleService#getUserHomepage()} (user override when set — issue #2209 — else
     * role resolve else Home), maps product homepage types to view keys, and fails closed to
     * {@code home} when the target is role-gated and the current user lacks Admin/Designer rights
     * (issue #2210 / parent #959 slice 3). Explicit {@code ?view=} deep links are unchanged.
     */
    protected String getDefaultView(HttpServletRequest request, HttpServletResponse response) throws JspException
    {
        try
        {
            PSRoleService roleService = (PSRoleService) PSSpringBeanProvider.getBean("roleService");
            String uhp = roleService.getUserHomepage();
            boolean isAdmin = Boolean.TRUE.equals(request.getAttribute(IS_ADMIN_KEY));
            boolean isDesigner = Boolean.TRUE.equals(request.getAttribute(IS_DESIGNER_KEY));
            return PSDefaultLandingView.resolveAuthorizedView(uhp, isAdmin, isDesigner);
        }
        catch(Exception e)
        {
            throw new JspException(e);
        }
    }

    protected boolean hasMaintenanceFailed(HttpServletRequest request, HttpServletResponse response) throws JspException
    {
        try
        {
            PSMaintenanceManager maintenanceManager = (PSMaintenanceManager) PSSpringBeanProvider.getBean("maintenanceManager");
            return maintenanceManager.hasFailures();
        }
        catch(Exception e)
        {
            throw new JspException(e);
        }
    }

    /**
     * Retrieves and sets user info in the request and in a session cookie.
     * @param request the servlet request, assumed not <code>null</code>.
     * @param response the servlet response, assumed not <code>null</code>.
     */
    protected void setCurrentUserInfo(HttpServletRequest request, HttpServletResponse response) throws JspException
    {
        String name = null;
        Boolean isAdmin = Boolean.FALSE;
        Boolean isDesigner = Boolean.FALSE;
        Boolean isAccessibilityUser = Boolean.FALSE;

        try
        {
            PSUserService userService = (PSUserService) PSSpringBeanProvider.getBean("userService");
            PSCurrentUser user = userService.getCurrentUser();

            name = user.getName();

            isAccessibilityUser = user.isAccessibilityUser();
            isAdmin = user.isAdminUser();
            isDesigner = user.isDesignerUser();


            List<String> roles  = user.getRoles();
            if(roles == null)
                  roles = new ArrayList<String>();

            request.setAttribute(CURRENT_USER_NAME_KEY, name);
            request.setAttribute(CURRENT_USER_ROLES_KEY, roles.toString());
            request.setAttribute(IS_ADMIN_KEY, isAdmin);
            request.setAttribute(IS_DESIGNER_KEY, isDesigner);
        } catch (Exception e) {
            throw new JspException(e);
        }

        setCookie(request, response, "perc_userName", name);
        setCookie(request, response, "perc_isAdmin", isAdmin.toString());
        setCookie(request, response, "perc_isDesigner", isDesigner.toString());
        setCookie(request, response, "perc_isAccessibilityUser", isAccessibilityUser.toString());
    }

    private void setCookie(HttpServletRequest request, HttpServletResponse response, String cookieName, String cookieValue) {
        String secure = "";
        if (request.isSecure())
            secure = " Secure;";
        response.addHeader("Set-Cookie", cookieName + "=" + cookieValue + "; SameSite=Lax;" + secure);
    }


    /**
     * Retrieves and sets information about item editor.
     * @param request the servlet request, assumed not <code>null</code>.
     * @param response the servlet response, assumed not <code>null</code>.
     */
    protected Map getItemEditorInfo(HttpServletRequest request, HttpServletResponse response) throws JspException {
        Map urlParams = new HashMap();
        try
        {
            PSPathService pathService = (PSPathService) PSSpringBeanProvider.getBean("pathService");
            PSPathItem item = pathService.findById(request.getParameter("perc_linkback_id"));

            Object temp = item.getName();
            String pageName = temp != null?temp.toString():"";
            List fpaths = item.getFolderPaths();
            String path = fpaths != null && !fpaths.isEmpty()?fpaths.get(0) + "/" + pageName:"";
            path = path.replace("//Sites/","/Sites/");
            String siteName = path.replace("/Sites/","").split("/")[0];
            path = URLEncoder.encode(path,"UTF-8");
            pageName = URLEncoder.encode(pageName,"UTF-8");
            temp = item.getId();
            String pageId = temp != null?temp.toString():"";
            urlParams.put("site", siteName);
            urlParams.put("mode", "readonly");
            urlParams.put("id", pageId);
            urlParams.put("name", pageName);
            urlParams.put("path", path);
            urlParams.put("pathType", "page");
        }
        catch(Exception e)
        {
            //TODO: I18N below
            urlParams.put("warningMessage", "The page you are attempting to reach, does not exist in the CMS.");
        }
        return urlParams;
    }

    /**
     * Retrieves and sets information about sites.
     * @param request the servlet request, assumed not <code>null</code>.
     * @param response the servlet response, assumed not <code>null</code>.
     */
    protected void setSitesInfo(HttpServletRequest request, HttpServletResponse response) throws JspException
    {
        try
        {
            PSSiteDataService siteService = (PSSiteDataService) PSSpringBeanProvider.getBean("siteDataService");
            List<PSSiteSummary> sites = siteService.findAll();
            JSONArray siteArray = new JSONArray(sites);
            boolean hasSites = siteArray.length() > 0;
            request.setAttribute(HAS_SITES_KEY, hasSites);
        }
        catch(Exception e)
        {
            throw new JspException(e);
        }
    }

    protected void setWidgetBuilderActiveInfo(HttpServletRequest request, HttpServletResponse response) throws JspException
    {
        try
        {
            PSWidgetBuilderService widgetBuilderService = (PSWidgetBuilderService) PSSpringBeanProvider.getBean("widgetBuilderService");
            request.setAttribute(IS_WIDGET_BUILDER_ACTIVE, new Boolean(widgetBuilderService.isWidgetBuilderEnabled()).toString());
        }
        catch(Exception e)
        {
            throw new JspException(e);
        }
    }

    /**
     * Make a get request to the server and get back the response code
     * @param theUrl url string, assumed not <code>null</code> or empty.
     * @param request the http request object, assumed not <code>null</code>.
     * @return The response code
     * @throws IOException upon any error.
     */
    protected int getResponseCode(String theUrl, HttpServletRequest request) throws IOException
    {
        StringBuilder rUrl = new StringBuilder(theUrl);
        rUrl.append(theUrl.indexOf("?") == -1 ? "?" : "&");
        rUrl.append(PSSESSIONID);
        rUrl.append("=");
        rUrl.append(getPSSessionId(request));

        IPSConnector connector = PSContainerUtilsFactory.getInstance().getConnectorInfo().getHttpConnector().get();
        URL url = new URL("http",
                connector.getCallbackHost(),
                connector.getPort(),
                rUrl.toString());
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();

        try
        {
            conn.connect();
            return conn.getResponseCode();
        }
        finally
        {
            if(conn != null)
                conn.disconnect();
        }
    }

    /**
     * Make a get request to the server.
     * @param theUrl url string, assumed not <code>null</code> or empty.
     * @param request the http request object, assumed not <code>null</code>.
     * @throws IOException upon any error.
     */
    protected String makeRequest(String theUrl, HttpServletRequest request)
            throws IOException
    {

        StringBuilder rUrl = new StringBuilder(theUrl);
        rUrl.append(theUrl.indexOf("?") == -1 ? "?" : "&");
        rUrl.append(PSSESSIONID);
        rUrl.append("=");
        rUrl.append(getPSSessionId(request));
        IPSConnector connector = PSContainerUtilsFactory.getInstance().getConnectorInfo().getHttpConnector().get();

        URL url = new URL("http",
                connector.getCallbackHost(),
                connector.getPort(),
                rUrl.toString());
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();

        try
        {
            conn.connect();
            InputStream in = (InputStream)conn.getContent();
            String content = convertStreamToString(in);
            return content;
        }
        finally
        {
            if(conn != null)
                conn.disconnect();
        }
    }

    /**
     * Retrieve the pssessionid value from the request header.
     * @param request the request assumed not <code>null</code>.
     * @return the pssessionid value or <code>null</code> if not found.
     */
    protected String getPSSessionId(HttpServletRequest request)
    {
        Cookie[] cookies = request.getCookies();
        for(Cookie cookie : cookies)
        {
            if(cookie.getName().equals(PSSESSIONID))
                return cookie.getValue();
        }
        return null;
    }


    /**
     * Converts an input stream to a utf-8 string.
     * @param is the input stream, assumed not <code>null</code>.
     */
    private  String convertStreamToString(InputStream is) throws IOException {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        if (is != null)
        {
            StringBuilder sb = new StringBuilder();
            String line;

            try
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                is.close();
            }
            return sb.toString();
        } else {
            return "";
        }
    }
    //Constants
    private static final String PSSESSIONID = "pssessionid";
    private static final String CURRENT_USER_NAME_KEY = "currentUserName";
    private static final String CURRENT_USER_ROLES_KEY = "currentUserRoles";
    private static final String HAS_SITES_KEY = "hasSites";
    private static final String IS_ADMIN_KEY = "isAdmin";
    private static final String IS_DESIGNER_KEY = "isDesigner";
    private static final String ADMIN_ROLE = "Admin";
    private static final String IS_ACCESSIBILITY_USER = "isAccessibilityUser";
    private static final String IS_WIDGET_BUILDER_ACTIVE = "isWidgetBuilderActive";
    private static final String MAINT_PAGE_URL = "/maintenance.jsp";
    private static final String MAINT_ERROR_PAGE_URL = "/maintenance-errors.jsp";

    // SPA deep-link allowlists (sync with WebUI/src/main/ts/app/deepLinks/allowlists.ts)
    private static final String[] HOME_SECTIONS = new String[]{
            "recent", "bookmarks", "library", "search", "create", "gadgets"
    };
    private static final Map<String, String> HOME_SECTION_ALIASES = Map.of(
            "list", "recent",
            "newitem", "create",
            "bookmark", "bookmarks",
            "dash", "gadgets",
            "dashboard", "gadgets",
            "widgets", "gadgets",
            "gadget", "gadgets"
    );
    private static final String[] PUBLISH_SECTIONS = new String[]{
            "sites", "status", "logs", "design", "runtime", "editions"
    };
    private static final Map<String, String> PUBLISH_SECTION_ALIASES = Map.of(
            "site", "sites",
            "log", "logs",
            "edition", "editions"
    );
    private static final String[] WORKFLOW_TABS = new String[]{
            "workflow", "roles", "users", "categories"
    };
    /** Unified Admin shell tabs (#3088) — includes former Workflow admin tabs. */
    private static final String[] ADMIN_TABS = new String[]{
            "tasks", "logs", "notifications", "tools",
            "workflow", "roles", "users", "categories"
    };
    private static final String[] DEVELOPER_SECTIONS = new String[]{
            "content-types", "templates", "slots", "keywords", "communities", "pipelines"
    };
    private static final Map<String, String> DEVELOPER_SECTION_ALIASES = Map.of(
            "contenttypes", "content-types",
            "content", "content-types",
            "ctypes", "content-types",
            "pipeline", "pipelines",
            "applications", "pipelines"
    );
    /** Design SPA sections — lockstep with allowlists.ts DESIGN_SECTIONS (#3306). */
    private static final String[] DESIGN_SECTIONS = new String[]{
            "templates"
    };
    private static final Map<String, String> DESIGN_SECTION_ALIASES = Map.of(
            "template", "templates",
            "tpl", "templates",
            "library", "templates",
            "template-library", "templates"
    );

    //Bad Developers ... NO Coffee for you....
    //Don't ever do this again... calling HTTP internally causes port exhaustion

    //private static final String CURRENT_USER_ROLES_URL = "/Rhythmyx/services/user/user/current";
    //private static final String SITES_URL = "/Rhythmyx/services/sitemanage/site";
    //private static final String PATH_ITEM_URL = "/Rhythmyx/services/pathmanagement/path/item/id/";
    //private static final String WIDGET_BUILDER_URL = "/Rhythmyx/services/widgetmanagement/widgetbuilder/active";

    private static final String MAINT_STATUS_SERVER_URL = "/Rhythmyx/services/maintenance/manager/status/server";
    private static final String MAINT_STATUS_PROC_URL = "/Rhythmyx/services/maintenance/manager/status/process";

%>

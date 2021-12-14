/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.servlets;

import com.percussion.auditlog.PSActionOutcome;
import com.percussion.auditlog.PSAuditLogService;
import com.percussion.auditlog.PSAuthenticationEvent;
import com.percussion.cms.IPSConstants;
import com.percussion.design.objectstore.PSServerConfiguration;
import com.percussion.error.PSExceptionUtils;
import com.percussion.i18n.PSI18nUtils;
import com.percussion.security.PSSecurityProvider;
import com.percussion.security.PSSecurityToken;
import com.percussion.security.PSUserEntry;
import com.percussion.security.SecureStringUtils;
import com.percussion.server.IPSCgiVariables;
import com.percussion.server.PSApplicationHandler;
import com.percussion.server.PSBaseResponse;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestContext;
import com.percussion.server.PSServer;
import com.percussion.server.PSUserSession;
import com.percussion.server.PSUserSessionManager;
import com.percussion.services.security.PSJaasUtils;
import com.percussion.services.security.PSRoleMgrLocator;
import com.percussion.services.security.PSServletRequestWrapper;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.utils.security.PSRemoteUserCallback;
import com.percussion.utils.security.PSRequestHeadersCallback;
import com.percussion.utils.security.PSSecurityUtility;
import com.percussion.utils.servlet.PSServletUtils;
import com.percussion.utils.tools.PSPatternMatcher;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.util.WebUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Provide security for Rhythmyx including redirection to form based
 * authentication as appropriate. The filter examines the request to determine
 * the correct form of authentication.
 * <P>
 * Requests that arrive from external servers are examined for the correct
 * headers that pass user and role information into Rhythmyx. These headers are
 * taken at face value.
 * 
 * @author dougrand
 */
public class PSSecurityFilter implements Filter
{
   /**
    * The system property name for which header to use to get the  REAL IP address (for proxy).
    */
   protected static final String NON_SECURE_HTTP_BIND_HEADER = "perc.http.bind.header";

   /**
    * The exception address to let through if SSL is on.
    * If this property is non <code>null</code> then that indicates SSL is on.
    */
   protected static final String NON_SECURE_HTTP_BIND_ADDRESS = "perc.http.bind.address";

   private final PSSecurityUtility  securityUtil = new PSSecurityUtility();
   private  final PSAuditLogService psAuditLogService=PSAuditLogService.getInstance();

   private static final String ERROR_HEADER_NULL="headers may not be null";
   private static final String THREAD="thread ";
   private static final String ERROR_REQUEST_NULL= "request may not be null";
   private static final String ERROR_RESPONSE_NULL="response may not be null";

   /**
    * Represent the kind of authentication to use
    */
   enum AuthType 
   {
      /**
       * Unauthenticated
       */
      ANONYMOUS,
      /**
       * Use HTTP basic authentication
       */
      BASIC,
      /**
       * Use HTTP form based authentication
       */
      FORM
   }
   
   /**
    * Represent a single entry from the security configuration files. 
    */
   static class SecurityEntry
   {      
      /**
       * If <code>true</code>, this entry is a user entry, otherwise a system
       * entry.
       */
      private boolean mi_userEntry;
      /**
       * If <code>true</code>, this entry is a url path pattern, otherwise a
       * method pattern.
       */
      private boolean mi_isPath;
      /**
       * The type for the entry if the entry matches. Never <code>null</code>
       * after ctor.
       */
      private AuthType mi_type;
      /**
       * The pattern, never <code>null</code> or empty after ctor.
       */
      private String mi_pattern;
      
      /**
       * Ctor
       * 
       * @param userEntry <code>true</code> if this is a user entry
       * @param isPath <code>true</code> if this represents a path, otherwise
       *           it represents an HTTP method.
       * @param type the type to use if this matches, never <code>null</code>.
       * @param pattern the pattern to match upon, never <code>null</code> or
       *           empty.
       */
      public SecurityEntry(boolean userEntry, boolean isPath, AuthType type, 
            String pattern)
      {
         if (type == null)
         {
            throw new IllegalArgumentException("type may not be null");
         }
         if (StringUtils.isBlank(pattern))
         {
            throw new IllegalArgumentException("pattern may not be null or empty");
         }
         mi_userEntry = userEntry;
         mi_isPath = isPath;
         mi_type = type;
         mi_pattern = pattern;
      }
      
      /**
       * @return the isPath
       */
      public boolean isPath()
      {
         return mi_isPath;
      }

      /**
       * @return the pattern
       */
      public String getPattern()
      {
         return mi_pattern;
      }

      /**
       * @return the type
       */
      public AuthType getType()
      {
         return mi_type;
      }

      /**
       * @return the userEntry
       */
      public boolean isUserEntry()
      {
         return mi_userEntry;
      }
      
      /**
       * Match the given string
       * @param string the string, never <code>null</code> or empty.
       * @param isSys the call is matching a system resource, so if 
       * <code>true</code> then user entries are ignored
       * @return <code>true</code> if this matches the pattern.
       */
      public boolean match(String string, boolean isSys)
      {
         // Never match a system path or method if we're a user entry
         if (isSys && mi_userEntry) return false;
         
         if (string == null)
         {
            throw new IllegalArgumentException("string may not be null");
         }
         return ms_matcher.doesMatchPattern(mi_pattern, string);
      }
      
      @Override
      public String toString()
      {
         return "{" + (isPath() ? "path " : "method ") 
            + (isUserEntry() ? "user " : "system ")
            + getType().toString() + " pattern='"
            + getPattern() + "'}\n";
      }
      
      @Override
      public boolean equals(Object b)
      {
         return EqualsBuilder.reflectionEquals(this, b);
      }
      
      @Override
      public int hashCode()
      {
         return mi_pattern.hashCode();
      }
   }

   /**
    * log to use, never <code>null</code>.
    */
    private static final Logger ms_log = LogManager.getLogger(PSSecurityFilter.class);

   /**
    * Identify the configured login policy. This can be overridden by the filter
    * configuration
    */
   public static String ms_policy = "rx.policy";

   /**
    * The login form to use for form based authentication. May be overridden by
    * the filter configuration. May be initialized during 
    * {@link #init(FilterConfig)}, may be <code>null</code> until first call to 
    * {@link #getLoginUrl(HttpServletRequest)}
    */
   private static String ms_loginForm = null;

   /**
    * The login form to use for secure form based authentication. May be
    * overridden by the filter configuration
    */
   private static String ms_secureloginForm = null;

   /**
    * If <code>true</code> then allow users to be authenticated by using the
    * supplied headers for SSO.
    */
   public static boolean ms_allowSSO = true;

   /**
    * If this is set on the session, it contains the Subject information for the
    * authenticated user. This is of type {@link javax.security.auth.Subject}
    */
   public static final String SUBJECT = "RX_AUTHENTICATED_SUBJECT";

   /**
    * These entries are created from the system and user security 
    * configurations.
    */
   private List<SecurityEntry> m_configuredEntries = null;
   
   /**
    * Determines if redirection to the login servlet should use https.
    */
   private boolean m_forceSecureLogin = false;

   /**
    * The left hand substrings listed here are for urls that should attempt
    * basic authentication and then send a 200 OK back to the client if it
    * succeeds rather.
    */
   private static List<String> ms_loginRequests = new ArrayList<>();
   
   /**
    * Matcher for use in this class
    */
   static PSPatternMatcher ms_matcher = new PSPatternMatcher('?', '*', null);
   
   static
   {
      ms_loginRequests.add("/sys_login");
   }
   
   /**
    * This is a string which matches CMS-API requests
    */
   private static List<String> ms_externalApiRequests = new ArrayList<>();

   static
   {
      ms_externalApiRequests.add("/v8");
   }

   /**
    * The system security config file reference, initialized by
    * {@link #initSecurityConfiguration(String)}, never <code>null</code> or
    * modified after that.
    */
   private File m_systemSecurityConfig = null;

   /**
    * The user security config file reference, initialized by
    * {@link #initSecurityConfiguration(String)}, never <code>null</code> or
    * modified after that.
    */
   private File m_userSecurityConfig = null;

   /**
    * The time the system security config was lastmodified, updated each time
    * {@link #loadConfigs()} is called.
    */
   private long m_systemConfigLastModified = 0;

   /**
    * The time the user security config was last modified, updated each time
    * {@link #loadConfigs()} is called.
    */
   private long m_userConfigLastModified = 0;
   
   /**
    * STORY-403 An exception IP address to allow through if SSL is on.
    */
   private String m_nonSecureHttpBindAddress = null;
   /**
    * STORY-403 If not null use the header.
    * This is needed for proxies such as Nginx or Apache that are in front of CM System.
    */
   private String m_nonSecureHttpBindAddressHeader = null;

   private Boolean isHTTPSRequired = null;
   
   /**
    * 
    */
   public PSSecurityFilter() {
      super();
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
    */
   public void init(FilterConfig config) throws ServletException
   {
      String p = config.getInitParameter("policy");
      String l = config.getInitParameter("loginform");
      
      m_nonSecureHttpBindAddress = System.getProperty(NON_SECURE_HTTP_BIND_ADDRESS);
      m_nonSecureHttpBindAddressHeader = System.getProperty(NON_SECURE_HTTP_BIND_HEADER);

      if (!StringUtils.isEmpty(p))
         ms_policy = p;
      if (!StringUtils.isEmpty(l))
         ms_loginForm = l;
      

      String asso = config.getInitParameter("allowSSO");
      if (!StringUtils.isEmpty(asso))
      {
         ms_allowSSO = asso.equalsIgnoreCase("true");
      }

      initSecurityConfiguration(new File(config.getServletContext().getRealPath("/WEB-INF")).getParentFile().getAbsolutePath());

      allowedOrigins = getAllowedOriginHosts();

   }

   /**
    * Initializes the security configuration file references and loads the
    * configs. Method has package access for unit testing only.
    * 
    * @param rootDir The path to the root servlet directory, assumed not
    *           <code>null</code> or empty.
    * 
    * @throws ServletException If there is an error.
    */
   void initSecurityConfiguration(String rootDir) throws ServletException
   {
      // locate config files
      File configDir = new File(new File(rootDir, PSServletUtils.WEB_INF),
            PSServletUtils.SYS_CONFIG_DIR);
      File userConfigDir = new File(configDir, PSServletUtils.USER_CONFIG_DIR);
      m_systemSecurityConfig = new File(configDir,
            "security/system-security-conf.xml");
      m_userSecurityConfig = new File(userConfigDir,
            "security/user-security-conf.xml");
      if (!m_userSecurityConfig.exists())
         ms_log.info("User security config file not found: {}",
                m_userSecurityConfig);
      loadConfigs();
   }

   /**
    * Loads the configurations if they've never been loaded or if they've been
    * modified since the last time they were loaded.
    * 
    * @throws ServletException If the system security config is not found or a
    *            config cannot be loaded.
    */
   private void loadConfigs() throws ServletException
   {
      if (!m_systemSecurityConfig.exists())
         throw new ServletException("System security config file not found: "
               + m_systemSecurityConfig);

      boolean doLoadSystem = false;
      if (m_systemSecurityConfig.lastModified() != m_systemConfigLastModified)
      {
         doLoadSystem = true;
         m_systemConfigLastModified = m_systemSecurityConfig.lastModified();
      }

      boolean doLoadUser = (m_userSecurityConfig.exists());
      if (doLoadUser
            && m_userSecurityConfig.lastModified() != m_userConfigLastModified)
      {
         doLoadUser = true;
         m_userConfigLastModified = m_userSecurityConfig.lastModified();
      }
      
      if (doLoadSystem || doLoadUser)
      {
         m_configuredEntries = new CopyOnWriteArrayList<>();
         loadConfig(m_systemSecurityConfig, false, m_configuredEntries);
         m_forceSecureLogin = loadConfig(m_userSecurityConfig, true, 
               m_configuredEntries);
      }
   }

   /**
    * Loads the config specified by the file and adds specified request pattern
    * strings to the list of configured requests {@link #m_configuredEntries},
    * which must be initialized outside of this method. Method has package
    * access for unit testing only.
    * 
    * @param securityConfig The config file, assumed not <code>null</code> and
    *           to exist.
    * @param isUser <code>true</code> if we're loading the user configuration. 
    * @param configs The configured entries, assumed never <code>null</code>.
    * @return <code>true</code> if the config specifies forceSecureLogin,
    *         <code>false</code> otherwise.
    * 
    * @throws ServletException if there are any errors.
    */
   static boolean loadConfig(File securityConfig, boolean isUser, 
         List<SecurityEntry> configs)
         throws ServletException {

      try (InputStream in = new FileInputStream(securityConfig)) {
         Document doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
         PSXmlTreeWalker tree = new PSXmlTreeWalker(doc);
         Element root = (Element) tree.getCurrent();

         boolean forceSecure = "yes".equals(root
                 .getAttribute("forceSecureLogin"));

         NodeList paths = root.getChildNodes();
         int len = paths.getLength();
         for (int i = 0; i < len; i++) {
            Node n = paths.item(i);
            if (!(n instanceof Element)) continue;
            Element path = (Element) n;
            String pathVal = PSXmlTreeWalker.getElementData(path);
            if (StringUtils.isEmpty(pathVal))
               continue;
            String name = path.getNodeName();
            String type = path.getAttribute("authType");
            SecurityEntry entry = new SecurityEntry(isUser,
                    name.equals("path"), AuthType.valueOf(type.toUpperCase()),
                    pathVal);
            configs.add(entry);
         }

         return forceSecure;
      } catch (SAXException |IOException e) {
         throw new ServletException(e);
      }


   }

   /**
    * Filter the request, looking for authentication information to be present.
    * If it is not present, redirect to the login form or present basic
    * authentication (configured based on the request).
    * 
    * @param request servlet request, never <code>null</code>
    * @param response servlet response, never <code>null</code>
    * @param chain the next request in the chain, never <code>null</code>
    * @throws IOException
    * @throws ServletException
    */
   public void doFilter(ServletRequest request, ServletResponse response,
         FilterChain chain) throws IOException, ServletException
   {
      if (request == null)
      {
         throw new IllegalArgumentException(ERROR_REQUEST_NULL);
      }
      if (response == null)
      {
         throw new IllegalArgumentException(ERROR_RESPONSE_NULL);
      }
      if (chain == null)
      {
         throw new IllegalArgumentException("chain may not be null");
      }

      boolean needsReset = false;
      boolean ssorequest = false;
      try
      {
         PSRequest psReq = null;
         
         // as long as this is an HTTP request, filter the request (we don't
         // wrap the response object)
         if (request instanceof HttpServletRequest)
         {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            HttpServletResponse httpResp = (HttpServletResponse) response;
            /*
             * If HTTPS is on only allow certain IP address through.
             */
            if ( ! isNonSecureHttpRequestAllowed(httpReq) ) {
               httpResp.sendError(HttpServletResponse.SC_FORBIDDEN, "Only TLS/SSL connections allowed");
               return;
            }

            if(!isValidHostHeader(httpReq)){
               httpResp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid host header detected");
               return;
            }
            //If the request is not secure, check whether http is allowed or not before we proceed.
             if(!httpReq.isSecure() && !isHttpAllowed(httpReq) ){
               String oldPath = httpReq.getRequestURL().toString();
               oldPath += httpReq.getQueryString() == null? "" : "?" + httpReq.getQueryString();
               URL oldUrl = new URL(oldPath);
               int sslport = PSServer.getSslListenerPort()==443?-1:PSServer.getSslListenerPort();
               URL newUrl = new URL("https", oldUrl.getHost(), sslport, oldUrl.getFile());
               httpResp.sendRedirect(newUrl.toExternalForm());
               return;
            }
            updateSessionTimeout(httpReq);
            
            if (! PSRequestInfoBase.isInited())
            {
               PSRequestInfo.initRequestInfo(httpReq);
               needsReset = true;
            }
            HttpServletRequest newreq = authorize(httpReq, httpResp);
            if (newreq == null)
            {
               // If the authorize method was done with the request, e.g.
               // we redirected to the login form, we just return here. We'll
               // be back later after the form has authenticated.
               return;
            }
            httpReq = newreq;
      request = httpReq;

      // ensure we've got a request
      psReq = initRequest(httpReq, httpResp);
      if (httpReq.getServletPath().equals("/__validateSession__"))
         ssorequest = true;
   }
         if (!ssorequest) {
            chain.doFilter(request, response);
         }

         if (psReq != null && psReq.getUserSession() != null) {
            psReq.getUserSession().requestFinished();
         }
}
      finally
      {
         if (needsReset)
            PSRequestInfoBase.resetRequestInfo();
      }
   }

   /**
    * This method returns false only if the following conditions satisfy.
    * server properties exist and the
    * sHTTPSRequired property is set to true (determined by either true or yes value) and
    * the request url exists and 
    * then if the url doesn't start with http://127.0.0.1 or http://localhost.
    * @param request assumed not <code>null</code>
    * @return <code>true</code> if http is allowed otherwise <code>false</code>.
    */
   private boolean isHttpAllowed(HttpServletRequest request)
   {
      boolean isAllowed = true;
      try {
         if (!securityUtil.httpsRequired()) {
            return true;
         }
         StringBuffer urlBuf = request.getRequestURL();
         if (urlBuf != null) {
            String url = urlBuf.toString().toLowerCase();
            if (!(url.startsWith("http://127.0.0.1:") || url.startsWith("http://127.0.0.1/") ||
                    url.startsWith("http://localhost:") || url.startsWith("http://localhost/"))) {
               isAllowed = false;
            }
         }
      }catch (IllegalArgumentException e){
         //NOOP
      }
      return isAllowed;
   }

   /**
    * If requireHTTPS server property exists and its value is set to true or yes. The value is locally stored in a variable,
    * instead of reading from a property each time.
    * @return <code>true</code> if it is a set to true.
    */
   private boolean httpsRequired()
   {
      if(isHTTPSRequired != null){
         ms_log.debug("Using requireHTTPS value of: {}" , isHTTPSRequired);
         return isHTTPSRequired;
      }
      boolean result = false;
      Properties serverProps = PSServer.getServerProps();
      if(serverProps != null){
         String httpsProperty = serverProps.getProperty("requireHTTPS", "");
         if(StringUtils.isNotBlank(httpsProperty) && (httpsProperty.equalsIgnoreCase("true") || httpsProperty.equalsIgnoreCase("yes"))){
            ms_log.debug("requireHTTPS is set to true.");
            isHTTPSRequired = Boolean.TRUE;
            result = true;
         }
         else{
            ms_log.debug("requireHTTPS settings are set to false.");
            isHTTPSRequired = Boolean.FALSE;
         }
      }
      return result;
   }

   /**
    * If {@link #m_nonSecureHttpBindAddress} is set and the request is not SSL
    * then this will check if the request remote address matches.
    * If {@link #m_nonSecureHttpBindAddress} is not set then <code>true</code> will always be returned.
    * <p>
    * See STORY-403
    * @param httpReq not <code>null</code>.
    * @return true if the request is allowed to proceed.
    * @see #m_nonSecureHttpBindAddressHeader
    * @author adamgent
    */
   protected final boolean isNonSecureHttpRequestAllowed(HttpServletRequest httpReq)
   {
      boolean allow = true;
      if ( ! httpReq.isSecure() && isNotBlank(m_nonSecureHttpBindAddress)) 
      {
         String addr;
         if (isNotBlank(m_nonSecureHttpBindAddressHeader)) 
         {
            addr = httpReq.getHeader(m_nonSecureHttpBindAddressHeader);
         }
         else 
         {
            addr = httpReq.getRemoteAddr();
         }
         if (! m_nonSecureHttpBindAddress.equals(addr)) 
         {
            allow = false;
         }
      }
      return allow;
   }

   /**
    * Sets the http session timeout to match what's defined for rx user 
    * sessions.
    * 
    * @param httpReq The current request, assumed not <code>null</code>.
    */
   private void updateSessionTimeout(HttpServletRequest httpReq)
   {
      int sessionTimeOut = PSServerConfiguration.DEFAULT_SESSION_TIMEOUT;

      try {
        sessionTimeOut =  PSServer.getServerConfiguration().getUserSessionTimeout();
      }catch(Exception e){
         //Ignore any exceptions and return the default value.
      }
      httpReq.getSession().setMaxInactiveInterval(sessionTimeOut);
   }


   /**
    * Handle authorization by either redirecting to the login form, sending back
    * a basic auth, or using the information that has already been made
    * available
    * 
    * @param request the request, assumed never <code>null</code>
    * @param response the response, assumed never <code>null</code>
    * @return a new request if the user is authenticated, the existing request
    *         if this request does not need authentication, or <code>null</code>
    *         if the user has been redirected
    * @throws IOException If there is an error redirecting to the login servlet.
    * @throws ServletException If there are any other errors.
    */
   private HttpServletRequest authorize(HttpServletRequest request,
         HttpServletResponse response) throws IOException, ServletException
   {
      ms_log.debug("enter authorize on thread {}"
            , Thread.currentThread().getName());
      HttpSession sess = request.getSession();
      
      Object mutex = WebUtils.getSessionMutex(sess);

      synchronized (mutex)
      {         
         /* 
          * 1. Check http request against Referer header...
          * 2. Check https request against Origin header... 
          */ 
         Subject sub = (Subject) sess.getAttribute(SUBJECT);
         boolean isBehindProxy = PSServer.isRequestBehindProxy(request);
         if (PSServer.getServerProps().getProperty("disableCrossSiteRequestForgeryCheck", "false").equalsIgnoreCase("true")
               || !checkForCrossSiteRequestForgery(request)  || isBehindProxy)
         {
        
            if (handleSessionCheck(request, response))
               return null;
            
            // anonymous access could have created an empty subject, so check for
            // that as well
            
            if (isAuthenticated(sub))
            {
               return handleAuthenticatedSubject(request, response, sub);
            }
          
            String sessId = getSessionIdFromRequest(request);
            if (!StringUtils.isBlank(sessId))
            {
               HttpServletRequest authReq = handleExistingUserSession(request, 
                  response, sessId);
               if (authReq == null)
                  sendAuthenticationError(request, response);
            
               return authReq;
             }

         }
         // reload configs if they've changed
         loadConfigs();

         AuthType authType = calculateAuthType(request);
         
         // check for anonymous
         if (authType.equals(AuthType.ANONYMOUS))
         {
            // Create empty subject and return the wrapper
            if (sub == null)
               sess.setAttribute(SUBJECT, new Subject());
            
            
            updateUserSession(request, response, false);
            
            return new PSServletRequestWrapper(request, sub);
         }
        
         /*
          * see if request uses basic auth, or if header is supplied to
          * indicate basic auth
          */
         if (authType.equals(AuthType.BASIC))
         {
            return handleBasicAuth(request, response);
         }
         else if (isExternallyAuthenticated(request))
         {
            return handleExternalAuth(request, response);
         }
         else
         {
            if (request.getMethod().equalsIgnoreCase("POST"))
            {
               // not supported
               sendAuthenticationError(request, response);
               return null;               
            }

            handleFormAuth(request, response);
            return null;
         }
      }
   }
   
   /**
    * Get sessionid from request parameter first 
    * if empty then try request attribute
    * @return
    */
   private String getSessionIdFromRequest(HttpServletRequest request)
   {
      String sessionId =
              SecureStringUtils.srp(
                      request.getParameter(IPSHtmlParameters.SYS_SESSIONID));

      if (StringUtils.isEmpty(sessionId))
      {
         sessionId =
                 SecureStringUtils.srp(
                         (String) request.getAttribute(IPSHtmlParameters.SYS_SESSIONID));
      }

      return sessionId;
   }

   /**
    * Returns a json response containing the remaining miliseconds until session timeout.  
    * This Will also turn on the NOSESSIONTOUCH request property that will prevent the session time
    * being updated during the request.  This method must be called early enough that no other calls to 
    * getUserSession have been made that would cause the touch before this.
    * 
    * @param request
    * @param response
    * @return
    * @throws IOException
    */
   private boolean handleSessionCheck(HttpServletRequest request, HttpServletResponse response) throws IOException
   {
      // Check for client session timeout.  
      // Invalidate sessions if time expired,  
      // update client timeout if not check request.
      // /sessionstatus.js

      // When using default servlet PathInfo is null and we get from servletPath

      String checkString = request.getPathInfo();
      if (checkString==null)
         checkString=request.getServletPath();

      boolean statusRequest = ms_matcher.doesMatchPattern("/sessioncheck", checkString);
      
      boolean statusSupportFiles = ms_matcher.doesMatchPattern("/cm/themes/*", checkString);
     
      if (statusRequest || statusSupportFiles) {
         // Force session not to be updated during request.
         PSRequest psrequest = initRequest(request, response);

         long lastActivity = NumberUtils.toLong(
                 SecureStringUtils.stripAllLineBreaks(
                         request.getParameter("lastActivity")), -1);

            PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_NOSESSIONTOUCH, Boolean.TRUE);

         if (statusRequest)
         {
           
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Cache-Control", "no-cache,must-revalidate");
        
            // Careful not to touch session during request which would extend
            PSUserSession userSession = psrequest.getUserSession();

            if (lastActivity>=0)
               userSession.setIdleOffset(lastActivity);


            long remaining = 0L;
            
            if (userSession != null && userSession.hasAuthenticatedUserEntries())
            {
               long idleSince = userSession.getIdleSince();
               long current = System.currentTimeMillis();
               long timeout = PSServer.getServerConfiguration().getUserSessionTimeout()*1000L;
               remaining = idleSince + timeout - current;

            }
            if (remaining<0)
            {
               PSUserSessionManager.releaseUserSession(userSession);
               remaining = 0;
            }
             if(remaining<500){
                PSAuthenticationEvent psAuthenticationEvent = new PSAuthenticationEvent(PSActionOutcome.SUCCESS.name(), PSAuthenticationEvent.AuthenticationEventActions.revoke, request, request.getRemoteUser());
                 psAuthenticationEvent.setActivity("revoke");
                 psAuditLogService.logAuthenticationEvent(psAuthenticationEvent);
             }
          
            int warning_s = PSServer.getServerConfiguration().getUserSessionWarning();
            long warning = warning_s > 0 ?  PSServer.getServerConfiguration().getUserSessionWarning()*1000L
                  : -1;
            
            String json ="{\"expiry\":"+remaining+",\"warning\":"+warning+"}";
            
            try {
               response.getWriter().write(json);
               return true;
             } catch (IOException e) {
                ms_log.error("Invalid json object for sessioncheck. Error: {}", PSExceptionUtils.getMessageForLog(e));
                sendAuthenticationError(request, response);
                return true;
             }    
            
         }
      }
   
      return false;
   }

   /**
    * Attempts to determine if the request is a potential cross site forgery attack.
    *
    * @param request The current http request, never null
    * @return when true, the request may be an attack.
    */
   private boolean checkForCrossSiteRequestForgery(HttpServletRequest request)
   {
      StringBuffer reqURL = request.getRequestURL();
      URI reqURI;

      try {
         reqURI = new URI(reqURL.toString());
      } catch (URISyntaxException e) {
         ms_log.warn("Invalid URL detected in request: URI: {} Error: {}. NOTE: These warnings may be generated by an active application security scan.",
                 reqURL, PSExceptionUtils.getMessageForLog(e));
         return true;
      }

      if (reqURI.getPath().equals(request.getContextPath() + "/login"))
         return false;
      
      // requested login from the CMS-API
      if (!(reqURI.getPath().equals(request.getContextPath() + "/v8/security/session"))
            && request.getServletPath().startsWith(ms_externalApiRequests.get(0)))
         return false;

      // If Referer exists and does not match request then go to login.
      if (!requestHeaderMatchesRequest(reqURI, request.getHeader("Referer")))
         return true;

      // If Origin exists and does not match request then go to login.
      return !requestHeaderMatchesRequest(reqURI, request.getHeader("Origin"));
   }
   
   /**
    * Match request header scheme, host, and port with request URI.  
    * 
    * @param requestURI The current request <code>null</code>.
    * @param reqHeader The header to mtch against the request <code>null</code>.
    * @return true if request matches header or header is missing. false if header does not match request.
    */
   private boolean requestHeaderMatchesRequest(URI requestURI, String reqHeader)
   {
      if (reqHeader == null)
          reqHeader = "";
      
      if (!reqHeader.isEmpty() && StringUtils.isNotBlank(reqHeader))
      {
            URI headerURI = URI.create(SecureStringUtils.stripUrlParams(reqHeader));
         return requestURI.getScheme().equals(headerURI.getScheme()) &&
                 requestURI.getHost().equals(headerURI.getHost()) &&
                 (requestURI.getPort() == headerURI.getPort());
      }
      return true;
   }

   /**
    * Calculate the right auth type based on the request and the configured
    * overrides from the system and user. Made package visible to allow unit 
    * testing.
    * 
    * @param request the servlet request, assumed never <code>null</code>.
    * @return the auth type, never <code>null</code>.
    * @throws ServletException Exception
    */
   AuthType calculateAuthType(HttpServletRequest request) 
   throws ServletException
   {
      // see if a login request
      if (isLoginRequest(request))
         return AuthType.BASIC;

      // see if header specifies it
      if (request.getHeader(IPSHtmlParameters.SYS_USE_BASIC_AUTH) != null)
         return AuthType.BASIC;
      
      // see if shindig request
      boolean isShindigRequestor = request.getHeader("x-shindig-dos") != null;
      if (isShindigRequestor)
         return AuthType.ANONYMOUS;
      
      // Now check each entry in the configuration for a match. The matcher
      // will skip user entries if the request is for a system resource
      String url = request.getServletPath();
      if (request.getPathInfo()!=null)
         url = url+request.getPathInfo();
      if (url.startsWith("/Rhythmyx"))
          url = StringUtils.substringAfter(url,"/Rhythmyx");
      
      String method = request.getMethod();
      boolean isSys = isSystemRequest(url);
      for(SecurityEntry entry : m_configuredEntries)
      {
         String str = entry.isPath() ? url : method;
         if (entry.match(str, isSys))
         {
            return entry.getType();
         }
      }
      
      return AuthType.FORM;
   }
   
   
   /**
    * Authenticates the user if the supplied session id denotes a valid rx user 
    * session.  
    * 
    * @param request The current request, assumed not <code>null</code>.
    * @param response The current response, assumed not <code>null</code>.
    * @param sessId The rx user session id, assumed not <code>null</code> or 
    * empty.
    * 
    * @return The wrapped authenticated request if the user is authenticated, or
    * <code>null</code> if not.
    */
   private HttpServletRequest handleExistingUserSession(
      HttpServletRequest request, HttpServletResponse response, String sessId) 
   {
      ms_log.debug("{}{} found user session id specified by html param: {}" ,
              THREAD , Thread.currentThread().getName(),
              sessId);
      
      initRequest(request, response);
      try
      {
         authenticate(request, sessId);
      }
      catch (LoginException e)
      {
         ms_log.debug("{}{} invalid user session id specified by html param: {}" ,
                 THREAD ,
                 Thread.currentThread().getName(),
                 sessId);
         return null;
      }
      
      Subject sub = (Subject) request.getSession().getAttribute(SUBJECT);
      HttpServletRequest newReq = new PSServletRequestWrapper(request, sub);
      updateUserSession(newReq, response, false);
      
      return newReq;
   }


   /**
    * Sends a response that specifies a redirect to a url that returns an error
    * code 500 and displays an authentication error.
    * 
    * @param request The current request, assumed not <code>null</code>.
    * @param response The current response, assumed not <code>null</code>.
    * 
    * @throws IOException If there is an error sending the response.
    */
   private void sendAuthenticationError(HttpServletRequest request, 
      HttpServletResponse response) throws IOException
   {
      String errorType = "Not Authenticated";
      int errorCode = 500;
      String[] errorMessages = 
         new String[] {
            "This request requires authentication, but the current " +
            "session is not authenticated.  The user session may " +
            "have expired or the server may have been restarted.  " +
            "You must log back into Rhythmyx to continue."};               
      redirectToErrorPage(request, response, errorCode, errorType, 
         errorMessages);
   }
   
   /**
    * Sends a redirect to the default error page with the specified error 
    * details
    * 
    * @param request The current request, may not be <code>null</code>. 
    * @param response The current response, may not be <code>null</code>.
    * @param errorCode The HTTP error code to reutrn.
    * @param errorType The error type, may not be <code>null</code> or empty.
    * @param errorMessages An array of messages, may not be <code>null</code> or 
    * empty.
    * 
    * @throws IOException If the redirect fails.
    */
   public static void redirectToErrorPage(HttpServletRequest request, 
      HttpServletResponse response, int errorCode, String errorType, 
      String[] errorMessages) throws IOException
   {
      if (request == null)
         throw new IllegalArgumentException(ERROR_REQUEST_NULL);
      if (response == null)
         throw new IllegalArgumentException(ERROR_RESPONSE_NULL);
      if (StringUtils.isBlank(errorType))
         throw new IllegalArgumentException(
            "errorType may not be null or empty");
      if (errorMessages == null || errorMessages.length == 0)
         throw new IllegalArgumentException("errorMessages may not be null");
      
      HttpSession sess = request.getSession();
      sess.setAttribute("errorCode", errorCode);
      sess.setAttribute("errorType", errorType);
      sess.setAttribute("errorMessages", errorMessages);
      response.sendRedirect(PSServer.getRequestRoot() + "/ui/error.jsp");
   }

   /**
    *
    *
    *
    *
    *
    *
    * Performs authentication against the login context without a known userid
    * or password. If it fails, returns a 401. Should only be called if
    * {@link #isExternallyAuthenticated(HttpServletRequest)} returns
    * <code>true</code>.
    * 
    * @param request The current request, assumed not <code>null</code>.
    * @param response The current response, assumed not <code>null</code>.
    * 
    * @return The updated request, or <code>null</code> if the response has
    *         been handled by this method.
    * 
    * @throws IOException If there are any IO errors.
    */
   private HttpServletRequest handleExternalAuth(HttpServletRequest request,
         HttpServletResponse response) throws IOException
   {
      try
      {
         HttpServletRequest authReq = authenticate(request, response, null,
               null);
         ms_log.debug("Successfully authenticated subject on thread {} using external authentication information",
                 Thread.currentThread().getName());
         return authReq;
      }
      catch (LoginException e)
      {
         response.sendError(401, "Authentication Failed: "
               + e.getLocalizedMessage());
         return null;
      }
   }

   /**
    * Determines if the current request has been externally authenticated
    * 
    * @param request The current request, assumed not <code>null</code>.
    * 
    * @return <code>true</code> if it is determined to have been externally
    *         authenticated, <code>false</code> if not.
    */
   private boolean isExternallyAuthenticated(HttpServletRequest request)
   {
      Map<String, String> headers = new HashMap<>();
      Enumeration<String> e = request.getHeaderNames();
      while (e.hasMoreElements())
      {
         String name = e.nextElement();
         headers.put(name, request.getHeader(name));
      }
      
      return !StringUtils.isBlank(getCertAuth(headers)) || !StringUtils.isBlank(
         getAuthType(headers));
   }
   
   /**
    * Get the header value specifying the authentication type.
    * 
    * @param headers The headers to check, not <code>null</code>, key is the
    * header name, value is the header value.
    * 
    * @return The value or <code>null</code> if not specified.
    */
   public static String getAuthType(Map<String, String> headers)
   {
      if (headers == null)
         throw new IllegalArgumentException(ERROR_HEADER_NULL);
      
      return getNormalizedHeader(IPSCgiVariables.CGI_AUTH_TYPE, headers);
   }

   /**
    * Get the header value specifying certificate authentication.
    * 
    * @param headers The headers to check, not <code>null</code>, key is the
    * header name, value is the header value.
    * 
    * @return The value or <code>null</code> if not specified.
    */
   public static String getCertAuth(Map<String, String> headers)
   {
      if (headers == null)
         throw new IllegalArgumentException(ERROR_HEADER_NULL);
      
      return getNormalizedHeader(IPSCgiVariables.CGI_CERT_SUBJECT, headers);
   }
   
   /**
    * Get the header value specifying the certificate issuer.
    * 
    * @param headers The headers to check, not <code>null</code>, key is the
    * header name, value is the header value.
    * 
    * @return The value or <code>null</code> if not specified.
    */
   public static String getCertIssuer(Map<String, String> headers)
   {
      if (headers == null)
         throw new IllegalArgumentException(ERROR_HEADER_NULL);
      
      return getNormalizedHeader(IPSCgiVariables.CGI_CERT_ISSUER, headers);
   }   

   /**
    * Return the value of the specified header, comparing the header name to
    * those in the supplied map normalized using 
    * {@link #normalizeHeaderName(String)}.
    * 
    * @param name The header name, assumed not <code>null</code> or empty.
    * @param headers The map of header names to header values, may not be 
    * <code>null</code>, may be empty.
    * 
    * @return The value from the map if a match is found, or <code>null</code> 
    * otherwise.
    */
   public static String getNormalizedHeader(String name, Map<String, 
      String> headers)
   {
      // walk the headers
      String normalizedName = normalizeHeaderName(name);
      for (Map.Entry<String, String> header : headers.entrySet())
      {
         if (normalizedName.equals(normalizeHeaderName(header.getKey())))
            return header.getValue();
      }
      
      return null;      
   }

   /**
    * Convert header name to uppercase and replace all hyphens with 
    * underscores in order to support headernames used prior to v6.0.
    * 
    * @param name The name to normalize, assumed not <code>null</code> or empty.
    * 
    * @return The normalized name, not <code>null</code> or empty.
    */
   private static String normalizeHeaderName(String name)
   {
      return name.toUpperCase().replace('-', '_');
   }

   /**
    * Handles case where we already have an authenticated subject in the http
    * session.
    * 
    * @param request The current request, assumed not <code>null</code>.
    * @param response The current response, assumed not <code>null</code>.
    * @param sub The authenticated subject, assumed not <code>null</code>.
    * 
    * @return The wrapped request, never <code>null</code>.
    */
   private HttpServletRequest handleAuthenticatedSubject(
         HttpServletRequest request, HttpServletResponse response, Subject sub)
   {
      if (ms_log.isDebugEnabled()) 
      {
         ms_log.debug("{}{} found 'public credentials': {}" ,
                 THREAD , Thread.currentThread().getName(), sub.getPublicCredentials());
      }
      HttpServletRequest newReq = new PSServletRequestWrapper(request, sub);
      updateUserSession(newReq, response, false);
      return newReq;
   }

   /**
    * Determine if the session of the specified request contains authenticated 
    * subject.
    * 
    * @param request the request in question, not <code>null</code>.
    * 
    * @return <code>true</code> if the subject is not <code>null</code> and
    *         has a non-empty set of principals, <code>false</code> otherwise.
    */
   public static boolean isAuthenticated(HttpServletRequest request)
   {
      if (request == null)
         throw new IllegalArgumentException("request may not be null.");
      
      HttpSession sess = request.getSession();
      Subject sub = (Subject) sess.getAttribute(SUBJECT);
      return isAuthenticated(sub);
   }
   
   /**
    * Determine if the supplied subject has been authenticated.
    * 
    * @param sub The subject to check, may be <code>null</code>.
    * 
    * @return <code>true</code> if the subject is not <code>null</code> and
    *         has a non-empty set of principals, <code>false</code> otherwise.
    */
   private static boolean isAuthenticated(Subject sub)
   {
      return sub != null && !sub.getPrincipals().isEmpty();
   }

   /**
    * Handles form based authentication.
    * 
    * @param request The current request, assumed not <code>null</code> and to
    *           contain a session.
    * @param response The current response, assumed not <code>null</code>.
    * 
    * @throws IOException If there are any IO errors.
    */
   private void handleFormAuth(HttpServletRequest request,
         HttpServletResponse response) throws IOException
   {

      HttpSession sess = request.getSession();      
      try
      {
         String loginUrl = getLoginUrl(request);
         boolean isBehindProxy = PSServer.isRequestBehindProxy(request);
         if(isBehindProxy  && request.getMethod().equalsIgnoreCase("GET"))  {
            String proxyUrl = PSServer.getProxyURL(request,true);
            if(StringUtils.isEmpty(proxyUrl)){
               proxyUrl = loginUrl ;
            }
            response.sendRedirect(proxyUrl+ loginUrl);
         }else{
            response.sendRedirect(loginUrl);
         }

         ms_log.debug("Redirected authentication to login servlet on thread {}"
               , Thread.currentThread().getName());
      }
      catch (IllegalStateException e)
      {
         ms_log.error("IllegalState on session: {} Error: {}" , sess.getId(),
                 PSExceptionUtils.getMessageForLog(e));
      }
      catch (NullPointerException e)
      {
         ms_log.error("Error on session: {} Error: {}" , sess.getId(),
                 PSExceptionUtils.getMessageForLog(e));
      }
   }

   /**
    * Determine if this is an explicit login request.
    * 
    * @param request The current request, assumed not <code>null</code>.
    * 
    * @return <code>true</code> if it is a login request, <code>false</code>
    *         otherwise.
    * 
    * @throws ServletException If the security configuration is not properly
    *            initialized.
    */
   private boolean isLoginRequest(HttpServletRequest request)
         throws ServletException
   {
      return matching(request.getServletPath(), ms_loginRequests);
   }
   
   /**
    * Determine if this is an explicit API request.
    * 
    * @param request The current request, assumed not <code>null</code>.
    * 
    * @return <code>true</code> if it is a CMS-API request with path /v8 <code>false</code>
    *         if otherwise and it is not a CMS-API login request (which should be anonymous).
    * 
    * @throws ServletException If the security configuration is not properly
    *            initialized.
    */
   private boolean isExternalApiRequest(HttpServletRequest request)
         throws ServletException
   {
      return (matching(request.getServletPath(), ms_externalApiRequests) && !request.getRequestURI().equals("/v8/security/session"));
   }

   /**
    * Determines if the supplied request contains a Rhythmyx role.
    * 
    * @param request The request as a {@link PSServletRequestWrapper} object,
    * assumed not <code>null</code>.
    * 
    * @return <code>true</code> if the request contains at least one Rhythmyx
    * role, <code>false</code> otherwise.
    */
   private static boolean hasRhythmyxRole(PSServletRequestWrapper request)
   {
      Collection<String> reqRoles = request.getRoles();
      List<String> rxRoles = 
         PSRoleMgrLocator.getBackEndRoleManager().getRhythmyxRoles();
      
      for (String role : reqRoles)
      {
         if (rxRoles.contains(role))
         {
            return true;
         }
      }
   
      return false;
   }
      
   /**
    * Handles basic authentication.
    * 
    * @param request The current request, assumed not <code>null</code>.
    * @param response The current response, assumed not <code>null</code>.
    * 
    * @return The updated request, or <code>null</code> if the response has
    *         been handled by this method.
    * 
    * @throws IOException If there are any IO errors.
    * @throws ServletException If the security configuration is not properly
    *            initialized.
    */
   private HttpServletRequest handleBasicAuth(HttpServletRequest request,
         HttpServletResponse response) throws IOException, ServletException
   {
      // Check for authorization and send basic request
      // if missing
      String auth = request.getHeader("Authorization");
      if (!StringUtils.isEmpty(auth))
      {
         int space = auth.indexOf(' ');
         if (space > 0)
         {
            String scheme = auth.substring(0, space);
            if (!"Basic".equalsIgnoreCase(scheme))
            {
               response.sendError(500, "Only Basic Authorization supported");
               return null;
            }
            auth = auth.substring(space);
            byte[] decoded = Base64.decodeBase64(auth.getBytes(StandardCharsets.UTF_8));
            String decodedAuth = new String(decoded);
            int colonIndex = decodedAuth.indexOf(":");
            
            if (colonIndex>0)
            {
               HttpServletRequest authReq;
               try
               {
                  authReq = authenticate(request, response, decodedAuth.substring(0, colonIndex), decodedAuth.substring(colonIndex+1));
               }
               catch (LoginException e)
               {
                  sendMustAuthenticateError(response);
                  return null;
               }

               // if a login request, just return success
               if (isLoginRequest(request))
               {
                  response.setStatus(200);
                  ms_log.debug("Doing Basic.Return 200 response on thread {}"
                        , Thread.currentThread().getName() );
                  return null;
               }
               
               ms_log.debug("Doing Basic: Successfully authenticated subject on thread {}"
                     , Thread.currentThread().getName() );
               return authReq;
            }
         }
      }

      // Send WWW-Authenticate and return
      sendMustAuthenticateError(response);

      return null;
   }

   /**
    * Returns the "WWW-Authenticate" header and a 401 response code.
    * 
    * @param response The response to use, assumed not <code>null</code>.
    * 
    * @throws IOException If there is a problem sending the error response.
    */
   private void sendMustAuthenticateError(HttpServletResponse response) 
      throws IOException
   {
      // Has to use EMPTY realm here; otherwise it will break current 
      // HTTPClient API which is used by Workbench, ECC and MSM
      response.addHeader(PSBaseResponse.RHDR_WWW_AUTH, "Basic realm=\"\"");
      response.sendError(401, "Must authenticate");
      ms_log.debug("Return 401 response on thread {} doing basic",
              Thread.currentThread().getName());
   }

   /**
    * Gets the redirect path based on the current setting for secure login
    * 
    * @param request The current request, assumed not <code>null</code>.
    * 
    * @return The new login page, never <code>null</code> or empty.
    */
   private String getLoginUrl(HttpServletRequest request)
   {
      if (ms_loginForm == null)
      {
         ms_loginForm = request.getContextPath() + "/login";
      }
      
      String loginPage = ms_loginForm;
      if (m_forceSecureLogin)
      {
         if (ms_secureloginForm == null)
         {
            try
            {
               String reqUrl = request.getRequestURL().toString();
               URL url = new URL(reqUrl);

               // check if already secure, otherwise convert
               String secureProtocol = "https";
               if (!url.getProtocol().equalsIgnoreCase(secureProtocol))
               {
                  // get the port
                  int sslPort = PSServer.getSslListenerPort();
                  if (sslPort != 0)
                  {
                     URL newUrl = new URL(secureProtocol, url.getHost(),
                           sslPort, ms_loginForm);
                     ms_secureloginForm = newUrl.toExternalForm();
                  }
               }
            }
            catch (MalformedURLException e)
            {
               ms_log.error("Failed to determine url to force secure login. Error: {}",
                       PSExceptionUtils.getMessageForLog(e));
            }
         }

         if (ms_secureloginForm != null)
         {
            loginPage = ms_secureloginForm;
         }
      }
      
      loginPage = PSLoginServlet.addRedirect(request, loginPage);

      return loginPage;
   }

   /**
    * Determine if the supplied path specifies a system resource.
    * 
    * @param url The servlet path to check, assumed not <code>null</code>.
    * 
    * @return <code>true</code> if it is a system resource, <code>false</code>
    *         if not.
    */
   private boolean isSystemRequest(String url)
   {
      boolean isSystemApp = true;
      
      if (url.startsWith("/user"))
      {
         isSystemApp = false;
      }
      else
      {
         // see if it's a reserved app name
         String appRoot = url;
         if (url.startsWith("/"))
            appRoot = StringUtils.substringAfter(url, "/");
         appRoot = StringUtils.substringBefore(appRoot, "/");

         PSApplicationHandler ah = PSServer.getApplicationHandler(appRoot);
         if (ah != null)
         {
            String appName = ah.getName();
            if (!(appName.startsWith("/sys_")
                  || appName.startsWith("/administration")
                  || appName.startsWith("/docs") || appName.startsWith("/dtd")))
            {
               isSystemApp = false;
            }
         }
      }

      return isSystemApp;
   }

   /**
    * Handle authentication and wraps the request
    * 
    * @param request the request, may not be <code>null</code>, may be
    *           wrapped on return.
    * @param response the response, may not be <code>null</code>.
    * @param userId The user id to use, may be <code>null</code> or empty.
    * @param password The password to use, may be <code>null</code> or empty.
    * 
    * @return a new request, never <code>null</code>.
    *
    * @throws LoginException If authentication does not succeed or if the user
    *            is not assigned to at least one Rhythmyx role.
    */
   @SuppressWarnings("unused")
   public static HttpServletRequest authenticate(HttpServletRequest request,
         HttpServletResponse response, String userId, String password)
         throws LoginException
   {
      if (request == null)
         throw new IllegalArgumentException(ERROR_REQUEST_NULL);
      if (response == null)
         throw new IllegalArgumentException(ERROR_RESPONSE_NULL);

      Subject sub;
      
      // trim any whitespace from supplied user id
      if (userId != null)
         userId = userId.trim();
      
      LoginContext lc = authenticate(request, userId, password);
      sub = lc.getSubject();
      PSServletRequestWrapper tmpReq = new PSServletRequestWrapper(request,
            sub);
      if (!hasRhythmyxRole(tmpReq))
      {
         String msg = "Authorization failed.  User \"" + userId + "\" is not a "
               + "member of a valid role.";
         ms_log.info(msg);
         throw new PSMissingRoleException(msg);
      }
      
      request.getSession().setAttribute(SUBJECT, sub);
      request = tmpReq;
             
      updateUserSession(request, response, true);

      String secure = "";
      if (request.isSecure()) {
         secure = " Secure;";
      }

      String sameSite = " SameSite=";
      //Create the pssessionid session
      response.addHeader("Set-Cookie",IPSHtmlParameters.SYS_SESSIONID +
              "=" + initRequest(request, response).getUserSessionId() +";"+
              "Path=/;" + secure + " HttpOnly; SameSite=" +
              PSServer.getProperty("sameSiteCookieMode","Strict"));

      return request;
   }

   /**
    * Authenticate the rhythmyx session supplied with the request.
    * 
    * @param request the request, may not be <code>null</code>, may be
    *    wrapped on return.
    * @param response the response, may not be <code>null</code>.
    * @throws LoginException if the supplied request does not contain a valid 
    *    rhythmyx session.
    */
   public static void authenticate(HttpServletRequest request, 
      HttpServletResponse response) throws LoginException
   {
      if (request == null)
         throw new IllegalArgumentException(ERROR_REQUEST_NULL);

      if (response == null)
         throw new IllegalArgumentException(ERROR_RESPONSE_NULL);

      PSRequest rxRequest = initRequest(request, response);
      PSUserSession rxSession = rxRequest.getUserSession();
      if (rxSession == null)
         throw new LoginException(
            "The supplied request does not contain a valid rhythmyx session.");
      
      updateHttpSession(request, rxSession);
   }
   
   /**
    * Authenticate against the supplied rhythmyx session id and update the
    * requests HTTP session if the rhythmyx session id was valid.
    * 
    * @param request the request to update the HTTP session for if the 
    *    supplied rhythmyx session is valid, not <code>null</code>.
    * @param sessionId the rhythmyx session id to authenticate against, not 
    *    <code>null</code> or empty.
    * @throws LoginException if the supplied rhythmyx session is invalid.
    */
   public static void authenticate(HttpServletRequest request, 
      String sessionId) throws LoginException
   {
      if (request == null)
         throw new IllegalArgumentException(ERROR_REQUEST_NULL);
      
      if (StringUtils.isBlank(sessionId))
         throw new IllegalArgumentException("sessionId cannot be null or empty");

      PSUserSession rxSession = PSUserSessionManager.getUserSession(sessionId);
      if (rxSession == null)
         throw new LoginException("The supplied rhythmyx session is invalid.");
      
      updateHttpSession(request, rxSession);
      
      // now update the request
      PSRequest rxReq = getCurrentRequest();
      if (rxReq == null)
         throw new IllegalStateException("Rx request must be initialized");
      
      // this will update the requests user session for us
      rxReq.refreshUserSession();
      updateRequestInfo(rxReq);
   }
   
   /**
    * Logout the user for the supplied rhythmyx session. This releases the
    * rhythmyx user session if found and invalidates the HTTP session 
    * supplied with the request.
    * 
    * @param request the request to invalidate the HTTP session for, not
    *    <code>null</code>. 
    * @param sessionId the id of the rhythmyx session to be released, may be
    *    <code>null</code> or empty.
    */
   public static void logout(HttpServletRequest request, String sessionId)
   {
      if (request == null)
         throw new IllegalArgumentException(ERROR_REQUEST_NULL);

      PSUserSession rxSession = PSUserSessionManager.getUserSession(sessionId);
      if (rxSession != null)
         PSUserSessionManager.releaseUserSession(rxSession);
      
      request.getSession().invalidate();
   }
   
   /**
    * Updates the http session from the supplied request with the information
    * from the supplied user session.
    * 
    * @param req The request to update, may not be <code>null</code>.
    * @param sess The user session to use, may not be <code>null</code>.
    */
   public static void updateHttpSession(HttpServletRequest req,
         PSUserSession sess)
   {
      if (req == null)
         throw new IllegalArgumentException(ERROR_REQUEST_NULL);
      if (sess == null)
         throw new IllegalArgumentException("sess may not be null");

      Subject subject = null;
      HttpSession httpSession = req.getSession(true);
      PSUserEntry[] userEntries = sess.getAuthenticatedUserEntries();
      if (userEntries.length > 0)
      {
         subject = PSJaasUtils.userEntryToSubject(userEntries[0], null);
         httpSession.setAttribute(SUBJECT, subject);
      }
      
      // update the wrappers subject
      if (req instanceof PSServletRequestWrapper)
         ((PSServletRequestWrapper) req).setSubject(subject);
      
      if (PSRequestInfoBase.isInited())
         PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.SUBJECT, subject);
      
      httpSession.setAttribute(IPSHtmlParameters.SYS_SESSIONID, sess.getId());
   }
   
   /**
    * Connect the specified user session to the current thread and initialize
    * the request context.  It is a noop if the supplied token 
    * already references the current session and <code>true</code> is 
    * returned.
    * 
    * @param token The security token representing the session to connect, may 
    * not be <code>null</code>.
    * 
    * @return <code>true</code> if the specified session is connected,
    * <code>false</code> if the specified session is not valid.
    */
   public static boolean connectSession(PSSecurityToken token)
   {
      if (token == null)
         throw new IllegalArgumentException("token may not be null");
      
      PSUserSession sess = PSUserSessionManager.getUserSession(
         token.getUserSessionId());
      
      if (sess == null)
         return false;
      
      if (!PSRequestInfoBase.isInited())
         PSRequestInfoBase.initRequestInfo(new HashMap<>());
      
      PSRequest psreq = getCurrentRequest();
      if (psreq != null && psreq.hasUserSession() && 
         psreq.getUserSessionId().equals(sess.getId()))
      {
         // same session already setup on this thread, nothing to do
         return true;
      }
      
      psreq = new PSRequest(token);
      psreq.setServletRequest(new PSServletRequestWrapper(
         psreq.getServletRequest(), null));
      updateHttpSession(psreq.getServletRequest(), sess);
      updateRequestInfo(psreq);
      
      return true;
   }
   
   /**
    * Set this request as the current request.  It is a noop if the supplied
    * request is already the current request.
    * 
    * @param req The request to set, may not be <code>null</code>.
    */
   public static void setRequest(PSRequest req)
   {
      if (req == null)
         throw new IllegalArgumentException("req may not be null");
      
      PSRequest curReq = (PSRequest) PSRequestInfoBase.getRequestInfo(
              PSRequestInfoBase.KEY_PSREQUEST);
      if (req == curReq)
         return;

      PSRequestInfoBase.resetRequestInfo();
      PSRequestInfo.initRequestInfo(req.getServletRequest());
      updateRequestInfo(req);
      
      HttpSession httpSession = req.getServletRequest().getSession(true);
      Subject s = (Subject) httpSession.getAttribute(PSSecurityFilter.SUBJECT);
      PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.SUBJECT, s);
   }

   /**
    * Initializes the {@link PSRequest} but does not parse the request body.
    * Creates a {@link PSRequestContext} and adds it to the request's
    * attributes. If a request is already found in the request info and
    * {@link PSRequest#getServletRequest()} returns the same instance as the
    * supplied <code>req</code>, the method returns the already created
    * request.
    * 
    * @param req The request, not <code>null</code>.
    * @param res The response, not <code>null</code>.
    * 
    * @return The request, never <code>null</code>.
    */
   public static PSRequest initRequest(HttpServletRequest req,
         HttpServletResponse res)
   {
      if (req == null)
         throw new IllegalArgumentException("req may not be null");
      if (res == null)
         throw new IllegalArgumentException("res may not be null");

      PSRequest psreq = getCurrentRequest();

      if (psreq == null || psreq.getServletRequest() != req)
      {
         if (psreq != null && isSameRequest(psreq.getServletRequest(), req))
         {
            psreq.setServletRequest(req);
         }
         else
         {
            // Create a PSRequest
            psreq = new PSRequest(req, res, null, null);
            PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_PSREQUEST, psreq);
            req.setAttribute("RX_REQUEST_CONTEXT", new PSRequestContext(psreq));
         }
      }

      return psreq;
   }

   /**
    * Get the request for the current thread.
    * 
    * @return The request, may be <code>null</code> if the request has not
    * been initialized for the current thread.
    */
   public static PSRequest getCurrentRequest()
   {
      if (!PSRequestInfoBase.isInited())
         return null;
      
      return  (PSRequest) PSRequestInfoBase.getRequestInfo(
              PSRequestInfoBase.KEY_PSREQUEST);
   }
   
   /**
    * Determine if two requests are the same, handling the fact the one or both
    * of the requests may have been wrapped one or more times.
    * 
    * @param req The request to check, assumed not <code>null</code>.
    * @param other The request to compare with, assumed not <code>null</code>.
    * 
    * @return <code>true</code> if they are the same instance or wrap the same
    * instance, <code>false</code> otherwise.
    */
   private static boolean isSameRequest(HttpServletRequest req, 
      HttpServletRequest other)
   {
      return getNestedRequest(req) == getNestedRequest(other);
   }
   
   /**
    * Get the original request if the supplied request is an instance of 
    * {@link ServletRequestWrapper}. Handles multiple levels of wrapping.
    * 
    * @param req The request to unwrap, assumed not <code>null</code>.
    * 
    * @return The original request, never <code>null</code>.
    */
   private static ServletRequest getNestedRequest(ServletRequest req)
   {
      while (req instanceof ServletRequestWrapper)
      {
         req = ((ServletRequestWrapper)req).getRequest();
      }
      
      return req;
   }

   /**
    * Calls {@link #initRequest(HttpServletRequest, HttpServletResponse)} and
    * then updates the rx user session from the current subject.
    * 
    * @param req The request, assumed not <code>null</code>.
    * @param res The response, assumed not <code>null</code>.
    * @param replace <code>true</code> if any current user entries in the rx
    *           session should be replaced with information in the current
    *           request's subject, <code>false</code> if any current user
    *           entries should be left as is.
    */
   private static void updateUserSession(HttpServletRequest req,
         HttpServletResponse res, boolean replace)
   {
      HttpSession httpSession = req.getSession(true);
      Subject s = (Subject) httpSession.getAttribute(PSSecurityFilter.SUBJECT);
      String locale = (String) httpSession.getAttribute(PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG);

      PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.SUBJECT, s);
      
      if(locale!=null)
         PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_LOCALE, locale);
       
      
      PSRequest psreq = initRequest(req, res);

      updateRequestInfo(psreq);

      // Handle security information
      PSUserSession sess = psreq.getUserSession();
      if (sess != null && (replace || !sess.hasAuthenticatedUserEntries()))
      {
         sess.clearAuthenticatedUserEntries();
         String username = req.getRemoteUser();
         
         if (!StringUtils.isBlank(username))
         {
            PSUserEntry entry = PSJaasUtils.subjectToUserEntry(s, username, 
               null);
            sess.addAuthenticatedUserEntry(entry);
         }
      }
   }
   
   /**
    * Updates the {@link PSRequestInfo} object with the supplied request.
    * 
    * @param req The request, assumed not <code>null</code>.
    */
   private static void updateRequestInfo(PSRequest req)
   {
      PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_PSREQUEST, req);
      
      if (req.hasUserSession())
      {
         PSUserSession sess = req.getUserSession();
         String locale = (String) sess.getPrivateObject(
            PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG);
         
         String httpSessionLocale = (String)req.getServletRequest().getSession().getAttribute(PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG);
         if (httpSessionLocale != null && (locale == null || !locale.equals(httpSessionLocale))){
            //Check to see if the locale is on the underlying HTTP session in the event that this is a new User session
          locale = httpSessionLocale;
          sess.setPrivateObject(
                PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG,locale);
         }
         
         if(locale!= null){
            PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_LOCALE, locale);
         }
      }
      String username = req.getServletRequest().getRemoteUser();
      if (!StringUtils.isBlank(username))
         PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, username);
   }

   /**
    * Authenticate a user and password against the configured login provider
    * 
    * @param req The requset, assumed not <code>null</code>.
    * @param user the username, may be <code>null</code> or empty
    * @param pass the password, may be <code>null</code> or empty
    * @return the authenticated login context if the login is successful
    * @throws LoginException if the login does not succeed
    */
   private static LoginContext authenticate(final HttpServletRequest req,
         final String user, final String pass) throws LoginException
   {
      // don't allow authentication using internal user name
      if (PSSecurityProvider.INTERNAL_USER_NAME.equals(user))
      {
         ms_log.warn("Attempt to login using internal user name: {}" , user);
         throw new LoginException("Reserved user name: " + user);
      }

      LoginContext lc = new LoginContext(ms_policy, callbacks -> {
         for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
               NameCallback nc = (NameCallback) callback;
               nc.setName(user);
            } else if (callback instanceof PasswordCallback) {
               PasswordCallback pc = (PasswordCallback) callback;
               char[] pw;
               if (pass == null)
                  pw = new char[0];
               else {
                  pw = new char[pass.length()];
                  pass.getChars(0, pass.length(), pw, 0);
               }
               pc.setPassword(pw);
            } else if (callback instanceof PSRequestHeadersCallback) {
               Map<String, String> headers = new HashMap<>();
               // Enumeration<Object> Enumeration<String> for servlet 3.1
               Enumeration<String> names = req.getHeaderNames();
               while (names.hasMoreElements()) {
                  String name = names.nextElement();
                  headers.put(name, req.getHeader(name));
               }

               PSRequestHeadersCallback rhc = (PSRequestHeadersCallback) callback;
               rhc.setHeaders(headers);
            } else if (callback instanceof PSRemoteUserCallback) {
               PSRemoteUserCallback ruc = (PSRemoteUserCallback) callback;
               ruc.setRemoteUser(req.getRemoteUser());
            } else {
               throw new UnsupportedCallbackException(callback,
                       "Unrecognized Callback");
            }
         }
      });
      lc.login();
      return lc;
   }

   /**
    * Check to see if the request url has a pattern match on any of the request
    * strings passed into the method. Method has package access for unit testing
    * only.
    * 
    * @param url The request url, assumed not <code>null</code> or empty.
    * @param patterns the list of possible request patterns, assumed not empty
    *           or <code>null</code>. A <code>*</code> is the only
    *           supported wildcard.
    * 
    * @return <code>true</code> if a match is found, <code>false</code>
    *         otherwise
    * 
    * @throws ServletException if the supplied string array is <code>null</code>
    *            as this indicates that the configuration is invalid.
    */
   static boolean matching(String url, List<String> patterns)
         throws ServletException
   {
      if (patterns == null)
         throw new ServletException("Invalid security configuration");

      for (String pattern : patterns)
      {
         if (ms_matcher.doesMatchPattern(pattern, url))
            return true;
      }

      return false;
   }
   /**
    * Accessor for unit testing.
    * 
    * @return <code>true</code> if secure login is required,
    *         <code>false</code> if not.
    */
   boolean isSecureLogin()
   {
      return m_forceSecureLogin;
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.Filter#destroy()
    */
   public void destroy()
   {
       // Nothing to do here but can be overridden
   }

   /**
    * Get the configured entries from the system and user configuration files.
    * 
    * @return the configuredEntries, never <code>null</code>, and not normally
    * empty.
    */
   public List<SecurityEntry> getConfiguredEntries()
   {
      return m_configuredEntries;
   }

   /**
    * Checks to make sure that the supplied Host header is valid.  Valid host names include
    * 127.0.0.1, localhost, publicCMSHostName from server.properties, allowedOrigins, in server.properties.
    * If allowedOrigins is set to *, all hostnames are allowed.
    *
    * @param request The current request
    * @return When true, the Host header is valid.  When false, the hostHeader is invalid and the request should be rejected.
    */
   public boolean isValidHostHeader(HttpServletRequest request){
      boolean ret = false; //assume failure

      List<String> hosts = Collections.list(request.getHeaders("Host"));

      if( hosts.isEmpty() || allowedOrigins.isEmpty()) {
         ret = true; //no Host header so is valid
      }
     else{
         for(String h : hosts){
            if(allowedOrigins.contains(h)) {
               ret = true;
               break;
            }
         }
      }
      return ret;
   }

   /**
    * Set when the filter is initialized to the configured allowedOrigins for this
    * CMS service
    */
   protected List<String> allowedOrigins = new ArrayList<>();

   /**
    * Gets the configured allowed origins and public cms hostname.  These are used to prevent
    * Host header injection.
    *
    * @return The list of allowed hosts
    */
   protected List<String> getAllowedOriginHosts(){
      List<String> ret = new ArrayList<>();

      String origins = PSServer.getProperty(IPSConstants.SERVER_PROP_ALLOWED_ORIGINS,
              IPSConstants.SERVER_PROP_ALLOWED_ORIGINS_DEFAULT);

      if(origins.equalsIgnoreCase( IPSConstants.SERVER_PROP_ALLOWED_ORIGINS_DEFAULT) || origins.isEmpty()){
         //
      }else if(origins.contains(",")){
         String[] splitOrigins = origins.split(",");
         for(String s : splitOrigins){
            s = s.trim();
            if(s.startsWith("http")){
               String t= getHostFromURLString(s);
               if(!t.isEmpty()){
                  ret.add(t);
               }
            }else{
               if(!ret.isEmpty()) {
                  ret.add(s);
               }
            }
         }
      }else{
         if(origins.startsWith("http")){
            String t = getHostFromURLString(origins);
            if(!t.isEmpty())
               ret.add(t);
         }
      }

      //we also need to allow the public cms hostname if that is configured.
      String publicHost = PSServer.getProperty(IPSConstants.SERVER_PROP_PUBLIC_CMS_HOSTNAME,"");

      if(!publicHost.isEmpty())
         ret.add(publicHost);


      return ret;
   }

   /**
    * Gets the host portion of the url
    * @param s A url string (http(s)://host:port/
    * @return the host portion of the url or ""
    */
   private String getHostFromURLString(String s){
      if(s == null || s.isEmpty()){
         return "";
      }
      String ret = "";
      if(s.startsWith("http")){
         try {
            URI uri = new URI(s);
            ret = uri.getHost();
         } catch (URISyntaxException e) {
            //handle bad uri
            ms_log.warn("Invalid url detected in {} server.property. Skipping origin {}. Error: {}",
                    IPSConstants.SERVER_PROP_ALLOWED_ORIGINS,
                    s,
                    PSExceptionUtils.getMessageForLog(e));
         }
      }
      return ret;
   }
}

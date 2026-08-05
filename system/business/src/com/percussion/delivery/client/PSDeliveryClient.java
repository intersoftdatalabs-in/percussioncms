/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.delivery.client;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.proxyconfig.data.PSProxyConfig;
import com.percussion.proxyconfig.service.IPSProxyConfigService;
import com.percussion.proxyconfig.service.PSProxyConfigServiceLocator;
import com.percussion.server.PSServer;
import com.percussion.error.PSMissingBeanConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Default {@link IPSDeliveryClient} implementation that performs HTTP requests against the
 * delivery tier, honoring proxy configuration and license-override headers.
 *
 * @author wesleyhirsch
 */
public class PSDeliveryClient implements IPSDeliveryClient
{

   public static final String PERC_VERSION_HEADER="perc-version";
   public static final String TOMCAT_USER="tomcat-user";
   public static final String TOMCAT_PASSWORD="tomcat-password";
    public static final String LICENSE_OVERRIDE_HEADER = "licenseOverride";

    private String licenseOverride = "";

    /**
     * Logger for this service.
     */
    public static final Logger log = LogManager.getLogger(PSDeliveryClient.class);

    /**
        * The number of times a request will be retried after I/O failures.
     */
    private int retryCount = 3;

    /**
     * Offline Detection
     *
     * Stop pinging the services if it appears offline.
     */
    private boolean offline; //indicates if the service is considered to be offline or not.
    /**
    * @return the offline
    */
   public boolean isOffline()
   {
      return offline;
   }

   /**
    * @param offline the offline to set
    */
   public void setOffline(boolean offline)
   {
      this.offline = offline;
   }

   /**
    * @return the failureCount
    */
   public int getFailureCount()
   {
      return failureCount;
   }

   /**
    * @param failureCount the failureCount to set
    */
   public void setFailureCount(int failureCount)
   {
      this.failureCount = failureCount;
   }

   private int failureCount;  //the number of failures reported.
    private static final int MAX_FAILURES=30; //The maximum number of failures before we assume the DTS is offline.

    /**
     * Sets the timeout until a connection is established in milli-seconds.
     */
    private int connectionTimeout = 300000;

    /**
     * Sets the socket timeout (for each HTTP method called) in milli-seconds.
     */
    private int operationTimeout = 300000;

    /**
     * This HTTP codes are treated as successful when returned by the delivery
     * server.
     */
    private static final List<Integer> successfulHttpStatusCodes = new ArrayList<Integer>()
    {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        {
            add(200);
            add(204);
        }
    };

    private HttpMethodType requestType;

    private String userName ;

    private String password ;

    private Object requestMessageBody;

    private String requestUrl;

    private String responseMessageBodyContentType = MediaType.APPLICATION_JSON;

    private IPSProxyConfigService proxyConfigService;

    private PSProxyConfig proxyConfig = null;

    private boolean allowSelfSignedCertificate;

    private boolean sslEnabled;

    /**
     * Creates an instance with default options for connection and operation
     * timeouts.
     * <p>
     * The instance has to be closed when it's no longer necessary. See {@link
     * PSDeliveryClient#close} for more information.
     */
    public PSDeliveryClient()
    {
    }

    /**
      * Creates an instance with retry count, connection and operation timeout
      * parameters.
     * <p>
     * The instance has to be closed when it's no longer necessary. See {@link
     * PSDeliveryClient#close} for more information.
     *
     * @param retryCount the number of retry if failure, must not be less than
     *            zero.
     * @param connectionTimeout the timeout until a connection is established in
     *            milli-seconds. Must not be less than zero.
     * @param operationTimeout the socket timeout for each operation in
     *            milli-seconds. Must not be less than zero.
     */
     public PSDeliveryClient(int retryCount,
            int connectionTimeout, int operationTimeout)
    {
       if (retryCount < 0)
          throw new IllegalArgumentException("retryCount must not be < 0.");
       if (connectionTimeout < 0)
          throw new IllegalArgumentException("connectionTimeout must not be < 0.");
       if (operationTimeout < 0)
          throw new IllegalArgumentException("operationTimeout must not be < 0.");

       this.retryCount = retryCount;
       this.connectionTimeout = connectionTimeout;
       this.operationTimeout = operationTimeout;
    }

    /**
     * Closes resources used by this client.
     * <p>
     * There are currently no explicit resources to release for the JDK client
     * implementation, so this method is intentionally a no-op.
     */
    public void close()
    {
        // no-op
    }

    /**
     * Parses JSON string response into a generic Object (Map or List).
     * @return A parsed JSON object or array as Object. Never null.
     */
    private Object parseJson()
    {
        String jsonStr = pushOrGet(MediaType.APPLICATION_JSON);
        if (isBlank(jsonStr)) {
            return new java.util.HashMap<String, Object>();
        }
        try {
            var mapper = JsonMapper.builder().build();
            return mapper.readValue(jsonStr, Object.class);
        }
        catch (Exception ex) {
            log.error("Error parsing JSON response: {}", ex.getMessage());
            throw new PSDeliveryClientException("Error parsing JSON response: " + ex.getMessage(), ex);
        }
    }

    /**
     * A low level function that simply sends some content using the corresponding
     * HTTP method (according to <code>requestType</code> value), gets the response
     * and returns it.
     *
     * @param requestMessageBodyContentType Content Type
     * @return A string returned when the HTTP method is executed.
     */
    private String pushOrGet(String requestMessageBodyContentType)
    {
        String response;
        this.responseMessageBodyContentType = MediaType.APPLICATION_JSON;

        switch (this.requestType)
        {
        case GET:
            response = executeGetMethod();
            break;
        case DELETE:
            response = executeDeleteMethod();
            break;
        case POST:
            if (this.requestMessageBody == null)
                log.warn("Executing Post Method with null body.  This is probably not what you intended.");
            response = executePostMethod(requestMessageBodyContentType);
            break;
        case PUT:
            if (this.requestMessageBody == null)
                log.warn("Executing Put Method with null body.  This is probably not what you intended.");
            response = executePutMethod(requestMessageBodyContentType);
            break;
        default:
            throw new PSDeliveryClientException("Method " + this.requestType + " not implemented.");
        }

        if (isBlank(response))
            response = StringUtils.EMPTY;

        return response;
    }

    /**
     * Requests a JSON Object from a delivery server. Requires that
     * <code>this.url</code> is already set.
     *
     * @return An Object representing the JSON response from the server. If there is no data
     *         returned, returns an empty map. Will never be
     *         <code>null</code>, may be empty.
     * @throws PSDeliveryClientException if the remote server did not return
     *             JSON of the expected type.
     *
     */
    private Object getJsonObject()
    {
        try
        {
            Object obj = parseJson();
            if (obj == null) {
                return new java.util.HashMap<String, Object>();
            }
            if (!(obj instanceof Map<?, ?>)) {
                throw new PSDeliveryClientException("Expected JSON object (Map), got " + obj.getClass().toString());
            }
            return obj;
        }
        catch (PSDeliveryClientException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new PSDeliveryClientException("Error in executing the HTTP method: " + ex.getMessage(), ex);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.delivery.client.IPSDeliveryClient#getJsonObject(com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions)
     */
    public Object getJsonObject(PSDeliveryActionOptions actionOptions)
    {
        prepare(actionOptions, null);
        return getJsonObject();
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.delivery.client.IPSDeliveryClient#getJsonObject(com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions, java.lang.Object)
     */
    public Object getJsonObject(PSDeliveryActionOptions actionOptions,Object requestMessageBody)
    {
        prepare(actionOptions, requestMessageBody);
        return getJsonObject();
    }

    /**
     * Requests a JSON Array from a delivery server. Requires that
     * <code>this.url</code> is already set.
     *
     * @return An Object representing the JSON array response from the server. If there is no data
     *         returned, returns an empty list. Will never be
     *         <code>null</code>, may be empty.
     * @throws PSDeliveryClientException if the remote server did not return
     *             JSON of the expected type.
     *
     */
    private Object getJsonArray()
    {
        try
        {
            Object obj = parseJson();
            if (obj == null) {
                return new java.util.ArrayList<Object>();
            }
            if (!(obj instanceof List<?>)) {
                throw new PSDeliveryClientException("Expected JSON array (List), got " + obj.getClass().toString());
            }
            return obj;
        }
        catch (PSDeliveryClientException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new PSDeliveryClientException("Error in executing the HTTP method: " + ex.getMessage(), ex);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.delivery.client.IPSDeliveryClient#getJsonArray(com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions)
     */
    public Object getJsonArray(PSDeliveryActionOptions actionOptions)
    {
        prepare(actionOptions, null);
        return getJsonArray();
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.delivery.client.IPSDeliveryClient#getJsonArray(com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions, java.lang.Object)
     */
    public Object getJsonArray(PSDeliveryActionOptions actionOptions, Object requestMessageBody)
    {
        prepare(actionOptions, requestMessageBody);
        return getJsonArray();
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.delivery.client.IPSDeliveryClient#push(com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions, java.lang.String, java.lang.Object)
     */
    public void push(PSDeliveryActionOptions actionOptions, String requestMessageBodyContentType,
            Object requestMessageBody)
    {
        prepare(actionOptions, requestMessageBody);

        String mediaType = StringUtils.isNotBlank(requestMessageBodyContentType) ? requestMessageBodyContentType :
            MediaType.APPLICATION_JSON;

        pushOrGet(mediaType);
    }

    public void push(PSDeliveryActionOptions actionOptions, Object requestMessageBody)
    {
       push(actionOptions, null, requestMessageBody);
    }


    /*
     * (non-Javadoc)
     * @see com.percussion.delivery.client.IPSDeliveryClient#getString(com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions)
     */
	public String getString(PSDeliveryActionOptions actionOptions) {
        prepare(actionOptions, null);
       return pushOrGet(MediaType.APPLICATION_JSON);
	}

    /**
     * Prepares everything to run the query against the remote delivery server
     * service. It extracts data from the PSDeliveryActionOptions object and
     * saves the request message body value to use it when executing an entity
     * enclosing method.
     *
     * @param actionOptions The PSDeliveryActionOptions object.
     * @param requestMessageBody The request message body
     */
	private void prepare(PSDeliveryActionOptions actionOptions, Object requestMessageBody)
    {
       if(actionOptions.getDeliveryInfo() == null)
       {
           String error = "Error getting info from delivery config file";
          log.error(error);
          throw new PSDeliveryClientException(error);
       }

          sslEnabled = isSslEnabled(actionOptions);
          allowSelfSignedCertificate = sslEnabled
                     && actionOptions.getDeliveryInfo().getAllowSelfSignedCertificate().orElse(false);
        PSDeliveryInfo server = actionOptions.getDeliveryInfo();
        URI uri;
        String protocol;

        try {
            String adminUrl = server.getAdminUrl().orElseThrow(() -> new PSDeliveryClientException("Error getting info from delivery config file"));
            uri = new URI(adminUrl);
            if (!sslEnabled) {
                // Parse delivery server url to get the protocol and port
                uri = new URI(server.getUrl());
            }
        }
        catch (URISyntaxException e)
        {
            log.error("Error getting info from delivery config file");
            throw new PSDeliveryClientException("Error getting info from delivery config file", e);
        }

        protocol = uri.getScheme();

        this.requestType = actionOptions.getHttpMethod();

        if (actionOptions.getSuccessfullHttpStatusCodes() != null &&
                !actionOptions.getSuccessfullHttpStatusCodes().isEmpty())
            successfulHttpStatusCodes.addAll(actionOptions.getSuccessfullHttpStatusCodes());

        // Request information
        if (this.requestType.equals(HttpMethodType.GET) && requestMessageBody != null)
            throw new IllegalArgumentException("Attempting to execute GET method with message body.  Body is: " + requestMessageBody);

        this.requestUrl = processUrl(actionOptions);

        // Authentication information
        String userName = actionOptions.getDeliveryInfo().getUsername().orElse(null);
        String password = actionOptions.getDeliveryInfo().getPassword().orElse(null);
        this.userName = userName;
        this.password = password;

        if (isNotBlank(userName) && isBlank(password))
            log.warn("Executing HTTP request with username but blank password.  This is probably not what you intended.");

        if (proxyConfig == null)
        {
           if (this.proxyConfigService == null)
              this.proxyConfigService = getProxyConfigService();
           if (this.proxyConfigService != null)
              this.proxyConfig = proxyConfigService
                 .findByProtocol(protocol).orElse(new PSProxyConfig());
           else
              this.proxyConfig = new PSProxyConfig();
        }

    }
    public java.net.http.HttpHeaders getCsrfToken(PSDeliveryActionOptions actionOptions) throws IOException {
        prepare(actionOptions, null);

        HttpRequest request =
                createRequestBuilder(this.requestUrl)
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(Duration.ofMillis(operationTimeout))
                        .build();

        try
        {
            HttpResponse<Void> response =
                    createJdkHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
            return response.headers();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while retrieving CSRF token headers", e);
        }
    }

	/**
	 * Process the delivery server host URL, and returns the appropriate
	 * one according to the type of service (admin or non-admin) in the
	 * delivery server.
	 *
	 * @param actionOptions The PSDeliveryActionOptions object
	 * @return The processed URL of the delivery service.
	 */
    private String processUrl(PSDeliveryActionOptions actionOptions)
    {
        PSDeliveryInfo server = actionOptions.getDeliveryInfo();

        String actionUrl = actionOptions.getActionUrl();
        String finalUrl = "";

        URI uri;

        try
        {
            String protocol;
            String deliveryHost;
            String port;

            if (actionOptions.isAdminOperation())
            {
                // Parse delivery server url to get the host
                String adminUrl = server.getAdminUrl().orElseThrow(() -> new PSDeliveryClientException("Error getting info from delivery config file"));
                uri = new URI(adminUrl);
            }
            else
            {
                // Parse delivery server url to get the host
                uri = new URI(server.getUrl());
            }
            deliveryHost = uri.getHost();
            protocol = uri.getScheme();
            port = uri.getPort() <= 0 ? "" : Integer.toString(uri.getPort());
            if("-1".equalsIgnoreCase(port))
                port = "";

            //Add the slash for the port
            port = port.length() == 0 ? "" : ":" + port;

            // Make final URL
            finalUrl =
                protocol + "://" +
                deliveryHost + port +
                // Add slash to the url if necessary
                (actionUrl.startsWith("/") ? StringUtils.EMPTY : "/") + actionUrl;

            uri = new URI(finalUrl);
        }
        catch (URISyntaxException e)
        {
            log.error("Error parsing URL: {}" , finalUrl);
            throw new PSDeliveryClientException("Error parsing URL: " + finalUrl, e);
        }

        return uri.toASCIIString();
    }

    /**
     * Executes a GET request against the given URL using authentication.
     *
     * @return A string containing the entire contents of the body of the
     *         response. May return <code>null</code> if there is an error
     *         processing the given url.
     */
    private String executeGetMethod()
    {
        if (isBlank(this.responseMessageBodyContentType))
            this.responseMessageBodyContentType = MediaType.APPLICATION_JSON;

        HttpRequest.Builder builder = createRequestBuilder(this.requestUrl);
        if (isNotBlank(this.responseMessageBodyContentType))
            builder.header(HttpHeaders.CONTENT_TYPE, this.responseMessageBodyContentType);

        return this.executeHttpRequest(builder.GET().build(), "GET", this.requestUrl);
    }

    private String executeDeleteMethod()
    {
        if (isBlank(this.responseMessageBodyContentType))
            this.responseMessageBodyContentType = MediaType.APPLICATION_JSON;

        String deleteUrl = this.requestUrl;
        if (requestMessageBody instanceof Map<?, ?>)
            deleteUrl = appendQueryParams(this.requestUrl, (Map<?, ?>) requestMessageBody);

        HttpRequest.Builder builder = createRequestBuilder(deleteUrl);
        if (isNotBlank(this.responseMessageBodyContentType))
            builder.header(HttpHeaders.CONTENT_TYPE, this.responseMessageBodyContentType);

        return this.executeHttpRequest(
                builder.method("DELETE", HttpRequest.BodyPublishers.noBody()).build(),
                "DELETE",
                deleteUrl);
    }

    /**
     * Executes a PUT method against the given URL using authentication.
     *
     * @param requestMessageBodyContentType Content Type
     * @return A string containing the entire contents of the body of the
     *         response. May return <code>null</code> if there is an error
     *         processing the given url.
     */
    private String executePutMethod(String requestMessageBodyContentType)
    {
        return this.executeEntityEnclosingMethod(HttpMethodType.PUT, requestMessageBodyContentType);
    }

    /**
     * Executes a POST request against the given URL using authentication.
     *
     * @param requestMessageBodyContentType Content Type
     * @return A string containing the entire contents of the body of the
     *         response. May return <code>null</code> if there is an error
     *         processing the given url.
     */
    private String executePostMethod(String requestMessageBodyContentType)
    {
        return this.executeEntityEnclosingMethod(HttpMethodType.POST, requestMessageBodyContentType);
    }

    /**
     * Executes an entity enclosing method (POST or PUT). This methods contains
     * the shared logic between POST and PUT.
     *
     * @param httpMethod The HTTP method object to execute.
     * @param requestMessageBodyContentType Content Type
     * @return A string containing the entire contents of the body of the
     *         response. May return <code>null</code> if there is an error
     *         processing the given url.
     */
    private String executeEntityEnclosingMethod(HttpMethodType methodType,
                                                String requestMessageBodyContentType)
    {
        try
        {
            HttpRequest.Builder builder = createRequestBuilder(this.requestUrl);
            HttpRequest.BodyPublisher publisher;
            if (isNotBlank(requestMessageBodyContentType))
                builder.header(HttpHeaders.CONTENT_TYPE, requestMessageBodyContentType);
            publisher = createBodyPublisher(this.requestMessageBody);

            HttpRequest request = builder.method(methodType.name(), publisher).build();
            return this.executeHttpRequest(request, methodType.name(), this.requestUrl);
        }
        catch (Exception e)
        {
            String errorMessage = "Error in trying to set the request body for the HTTP method";

            log.error(errorMessage, e);
            throw new PSDeliveryClientException(errorMessage);
        }

    }

    /**
     * Low level HTTP request function.
     *
        * @param request A fully configured JDK {@link HttpRequest}.
        *            <strong>Note:</strong> May attempt the same request multiple
        *            times. Make sure the request passed into here is idempotent.
        * @param methodLabel The HTTP method label used for error reporting.
        * @param url The target URL used for error reporting.
     *
     * @return A string containing the body of the returned page. May be
     *         <code>null</code> if something went wrong.
     */
    private String executeHttpRequest(HttpRequest request, String methodLabel, String url)
    {
        int statusCode = -1;
        String responseData = "";
        try {
            for (int attempt = 0; attempt <= retryCount; attempt++) {
                try {
                    HttpResponse<InputStream> response =
                            createJdkHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
                    statusCode = response.statusCode();
                    try (InputStream responseDataStream = response.body()) {
                        responseData = responseDataStream == null
                                ? ""
                                : IOUtils.toString(responseDataStream, StandardCharsets.UTF_8);
                    }
                    break;
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while executing " + methodLabel + " request", e);
                }
                catch (IOException ex) {
                    if (attempt >= retryCount)
                        throw ex;
                }
            }

            if (!successfulHttpStatusCodes.contains(statusCode)) {
                failureCount = 0;
                offline = false;
                String msg;
                if (statusCode == 401) {
                    msg = String.format("Authentication error. Check user and password for this delivery server. HTTP status: %s",
                            statusCode);
                } else {
                    msg = String.format("Error when executing method : %s %s : %s", methodLabel, url, responseData);
                }
                log.error(msg);
                throw new PSDeliveryClientException(msg);
            } else {
                if (statusCode == 204)
                    return "";
                else
                    return responseData;
            }
        }
        catch (IOException ex)
        {
           failureCount++;
           if(failureCount > MAX_FAILURES){
              offline = true;
              log.info("Delivery Services are unavailable.  Suppressing further messages.");
           }
           if(!offline)
              log.error("Fatal transport error: {}" , ex.getMessage());

           String reqUrl = this.requestUrl;
           try
           {
              URL parsedUrl = new URL(reqUrl);
              reqUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost() + ":" + parsedUrl.getPort();
           }
           catch (MalformedURLException e)
           {
              if(!offline)
                 log.error(e.getLocalizedMessage());
           }
           if(!offline)
              throw new PSDeliveryClientException("Unable to connect to delivery server at: {}." + reqUrl + ".");
           return null;
        }
    }

        private HttpClient createJdkHttpClient()
    {
        HttpClient.Builder builder =
            HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectionTimeout));

        if (sslEnabled && allowSelfSignedCertificate)
            configureAllowSelfSignedTls(builder);

        if (proxyConfig != null && proxyConfig.getHost() != null && proxyConfig.getPort() != null)
        {
            builder.proxy(ProxySelector.of(
                    new InetSocketAddress(proxyConfig.getHost(), Integer.parseInt(proxyConfig.getPort()))));

            if (proxyConfig.getUser() != null && proxyConfig.getPassword().isPresent())
            {
                String proxyUser = proxyConfig.getUser();
                String proxyPassword = proxyConfig.getPassword().orElse("");
                builder.authenticator(new java.net.Authenticator()
                {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication()
                    {
                        if (getRequestorType() == RequestorType.PROXY)
                            return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                        return null;
                    }
                });
            }
        }

        return builder.build();
    }

    private void configureAllowSelfSignedTls(HttpClient.Builder builder)
    {
        try
        {
            // CodeQL java/insecure-trustmanager (alert #1068): previously used an all-trusting
            // X509TrustManager that returned new X509Certificate[0] from getAcceptedIssuers
            // and accepted any chain. Replace with the JVM's default trust managers, which
            // validate against the system trust store (cacerts / JSSE cacerts). Operators
            // who need to import a private CA / self-signed cert should add it to the JVM
            // trust store via `keytool -importcert -alias <name> -file <cert> -cacerts`.
            TrustManager[] trustManagers = createDefaultTrustManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            builder.sslContext(sslContext);
            SSLParameters sslParameters = new SSLParameters();
            // Endpoint identification (SNI/hostname verification) is enabled by default
            // when HttpsURLConnection is in play. HttpClient's setEndpointIdentificationAlgorithm
            // (HTTPS) was added in JDK 20; for JDK 21 (per AGENTS.md) the algorithm stays as
            // "HTTPS" so cert hostname still gets verified against the URL host.
            sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
            builder.sslParameters(sslParameters);
        }
        catch (GeneralSecurityException e)
        {
            throw new PSDeliveryClientException("Unable to configure TLS for self-signed certificates", e);
        }
    }

    /**
     * Returns the JVM's default {@link TrustManager}s, which validate TLS server certificates
     * against the system trust store ({@code $JAVA_HOME/lib/security/cacerts} plus any
     * {@code -Djavax.net.ssl.trustStore=...} override). Replaces the previous
     * all-trusting X509TrustManager that was flagged by CodeQL {@code java/insecure-trustmanager}
     * (alert #1068).
     *
     * @return the trust managers, never null or empty
     * @throws GeneralSecurityException if the platform does not provide a default
     *     {@code PKIX} or {@code SunX509} {@link javax.net.ssl.TrustManagerFactory}
     */
    static TrustManager[] createDefaultTrustManagers() throws GeneralSecurityException
    {
        // Prefer the algorithm-agnostic default algorithm (PKIX on modern JDKs).
        String algorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
        // Passing null to init() loads the default trust store (cacerts), per
        // TrustManagerFactory#init(KeyStore) contract.
        tmf.init((KeyStore) null);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        if (trustManagers == null || trustManagers.length == 0)
        {
            throw new GeneralSecurityException(
                    "Default TrustManagerFactory returned no trust managers (algorithm="
                            + algorithm
                            + ")");
        }
        return trustManagers;
    }

    private HttpRequest.Builder createRequestBuilder(String targetUrl)
    {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(targetUrl)).timeout(Duration.ofMillis(operationTimeout));

        builder.header(PERC_VERSION_HEADER, PSServer.getVersion());
        if (isNotBlank(this.userName))
            builder.header(TOMCAT_USER, this.userName);
        if (this.password != null)
            builder.header(TOMCAT_PASSWORD, this.password);
        if (isNotBlank(this.userName) && this.password != null)
        {
            String token = Base64.getEncoder().encodeToString(
                    (this.userName + ":" + this.password).getBytes(StandardCharsets.UTF_8));
            builder.header(HttpHeaders.AUTHORIZATION, "Basic " + token);
        }
        if (isNotBlank(this.licenseOverride))
            builder.header(LICENSE_OVERRIDE_HEADER, this.licenseOverride);
        return builder;
    }

        private HttpRequest.BodyPublisher createBodyPublisher(Object body)
            throws IOException
    {
        if (body == null)
            return HttpRequest.BodyPublishers.noBody();

            if (body instanceof HttpRequest.BodyPublisher)
                return (HttpRequest.BodyPublisher) body;

            if (body instanceof Map<?, ?>)
                return HttpRequest.BodyPublishers.ofString(formEncode((Map<?, ?>) body),
                    StandardCharsets.UTF_8);

        if (body instanceof Collection<?>)
        {
            try
            {
                    String encoded = formEncodeCollection((Collection<?>) body);
                    if (isNotBlank(encoded))
                        return HttpRequest.BodyPublishers.ofString(encoded, StandardCharsets.UTF_8);
                    return HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8);
            }
            catch (Exception ex)
            {
                throw new PSDeliveryClientException("Error in trying to set the request body for the HTTP method");
            }
        }

        if (body instanceof InputStream)
            return HttpRequest.BodyPublishers.ofByteArray(((InputStream) body).readAllBytes());

        if (body instanceof String)
            return HttpRequest.BodyPublishers.ofString((String) body, StandardCharsets.UTF_8);

        return HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8);
    }

    private String appendQueryParams(String baseUrl, Map<?, ?> params)
    {
        String query = formEncode(params);
        if (isBlank(query))
            return baseUrl;
        return baseUrl + (baseUrl.contains("?") ? "&" : "?") + query;
    }

    private String formEncode(Map<?, ?> params)
    {
        if (params == null || params.isEmpty())
            return "";
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<?, ?> entry : params.entrySet())
        {
            if (builder.length() > 0)
                builder.append('&');
            String name = entry.getKey() == null ? "" : entry.getKey().toString();
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            builder.append(URLEncoder.encode(name, StandardCharsets.UTF_8));
            builder.append('=');
            builder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private String formEncodeCollection(Collection<?> values)
    {
        if (values == null || values.isEmpty())
            return "";

        StringBuilder builder = new StringBuilder();
        for (Object value : values)
        {
            if (!(value instanceof Map.Entry<?, ?>))
                return "";

            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) value;
            if (builder.length() > 0)
                builder.append('&');
            String name = entry.getKey() == null ? "" : entry.getKey().toString();
            String mappedValue = entry.getValue() == null ? "" : entry.getValue().toString();
            builder.append(URLEncoder.encode(name, StandardCharsets.UTF_8));
            builder.append('=');
            builder.append(URLEncoder.encode(mappedValue, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private boolean isSslEnabled(PSDeliveryActionOptions actionOptions)
    {
        boolean sslEnabled = false;

        PSDeliveryInfo server = actionOptions.getDeliveryInfo();

        URI uri;
        String protocol;

        try
        {
            String adminUrl = server.getAdminUrl().orElseThrow(() -> new PSDeliveryClientException("Error getting info from delivery config file"));
            uri = new URI(adminUrl);
            protocol = uri.getScheme();
            if (protocol.equals("https"))
            {
                sslEnabled = true;
            }


        }
        catch (URISyntaxException e)
        {
            log.error("Error getting info from delivery config file");
            throw new PSDeliveryClientException("Error getting info from delivery config file", e);
        }
        return sslEnabled;
    }


   /**
    * When set, requests send to the delivery tier will use
    * the supplied license number instead of the primary
    * instance license id.
    *
    * This property will be automatically cleared after method
    * execution to prevent accidental override.
    *
    * @param licenseOverride the licenseOverride to set
    */
   public void setLicenseOverride(String licenseOverride)
   {
      this.licenseOverride = licenseOverride;
   }


   /**
    * Gets the corresponding proxy config bean for the service
    * @return ProxyConfigService bean. May be <code>null</code> if bean is not found
    */
   private IPSProxyConfigService getProxyConfigService()
   {
      try
      {
         return PSProxyConfigServiceLocator.getProxyConfigService();
      }
      catch (PSMissingBeanConfigurationException e)
      {
         return null;
      }
   }

   public void setProxyConfig (PSProxyConfig proxyConfig)
   {
      this.proxyConfig = proxyConfig;
   }

   public PSProxyConfig getProxyConfig ()
   {
      return this.proxyConfig;
   }
}

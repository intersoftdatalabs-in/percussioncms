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
package com.percussion.share.test;

import com.percussion.delivery.client.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.removeEnd;
import static org.apache.commons.lang.StringUtils.removeStart;
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

/**
 * A Wrapper around Commons HTTP client for REST services.
 * Most of the methods are protected as this class should be
 * extended to provide more specific behavior.
 * @author adamgent
 */
public class PSRestClient {
    private String url;
    private Map<String, String> requestHeaders = new HashMap<>();
    private String postContentType = "text/xml";
    private boolean sslSocketFactoryRegistered = false;
    private HttpClient client = new HttpClient();

    {
        client.getParams().setContentCharset("UTF-8");
    }

    public List<String> parseAcceptHeader(String acceptHeader) {
        return new ArrayList<>(asList(acceptHeader.split(",")));
    }

    public String outputAcceptHeader(List<String> accepts) {
        return StringUtils.join(accepts, ",");
    }

    protected void addAccept(String mime) {
        var accepts = parseAcceptHeader(getAcceptHeader());
        accepts.add(mime);
        setAcceptHeader(outputAcceptHeader(accepts));
    }

    protected String getAcceptHeader() {
        var accept = getRequestHeaders().get("Accept");
        return accept == null ? "" : accept;
    }

    protected void setAcceptHeader(String header) {
        notNull(header, "header");
        getRequestHeaders().put("Accept", header);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    protected HttpClient getClient() {
        return client;
    }

    protected String GET(String path, Collection<Entry<String, String>> params) {
        var method = getMethod();
        setPathMethod(path, method);
        var nvps = nameValuePairs(params);
        method.setQueryString(nvps);
        return executeMethod(method);
    }

    protected String GET(String path) {
        var method = getMethod();
        setPathMethod(path, method);
        return executeMethod(method);
    }

    protected InputStream GET_BINARY(String path) {
        var method = getMethod();
        setPathMethod(path, method);
        return executeBinaryMethod(method);
    }

    private InputStream executeBinaryMethod(HttpMethod method) throws RestClientException {
        registerSslProtocol();
        try {
            int stat = client.executeMethod(method);
            var body = method.getResponseBodyAsStream();
            var uri = method.getURI().getURI();
            var name = method.getName();
            log.trace("HTTP return code: " + stat);
            if (log.isDebugEnabled())
                log.debug(format("{0} {1}  HTTP Stat:{2}", name, uri, "" + stat));
            if (log.isTraceEnabled())
                log.trace("Response: " + body);
            if (!(200 <= stat && stat < 305)) {
                var error = "URI: " + uri + " HTTP Error: " + stat + " Response: \n" + body;
                log.error(error);
                throw new RestClientException(stat, uri, body);
            }
            return body;
        } catch (HttpException | IOException e) {
            throw new RestClientException(e);
        }
    }

    protected String POST(String path, String body) {
        return POST(path, body, getPostContentType());
    }

    protected String POST(String path, String body, String contentType) {
        var method = postMethod();
        var ct = contentType + "; charset=UTF-8";
        setPathMethod(path, method);
        try {
            if (log.isTraceEnabled() && body != null) {
                log.trace("POST Body: " + body);
            }
            if (body != null) {
                var sre = new StringRequestEntity(body, ct, "UTF-8");
                method.setRequestEntity(sre);
            }
            return executeMethod(method);
        } catch (UnsupportedEncodingException e) {
            throw new RestClientException(e);
        }
    }

    protected String PUT(String path, String body) {
        return PUT(path, body, getPostContentType());
    }

    protected String PUT(String path, String body, String contentType) {
        var method = putMethod();
        var ct = contentType + "; charset=UTF-8";
        setPathMethod(path, method);
        try {
            if (log.isTraceEnabled()) {
                log.trace("PUT Body: " + body);
            }
            var sre = new StringRequestEntity(body, ct, "UTF-8");
            method.setRequestEntity(sre);
            return executeMethod(method);
        } catch (UnsupportedEncodingException e) {
            throw new RestClientException(e);
        }
    }

    protected String POST(String path, Collection<Entry<String, String>> params) {
        var method = postMethod();
        setPathMethod(path, method);
        var nvps = nameValuePairs(params);
        method.setRequestBody(nvps);
        return executeMethod(method);
    }

    protected String DELETE(String path) {
        var method = deleteMethod();
        setPathMethod(path, method);
        return executeMethod(method);
    }

    protected void setPathMethod(String path, HttpMethod method) {
        notNull(path);
        notNull(method);
        var uri = getUri(path);
        try {
            method.setURI(uri);
        } catch (URIException e) {
            throw new RestClientException("Bad url " + getUrl() + path, e);
        }
    }

    protected URI getUri(String relativePath) {
        try {
            return new URI(new URI(getUrl(), false), new URI(relativePath, false));
        } catch (URIException | NullPointerException e) {
            throw new RestClientException("Bad url " + getUrl() + relativePath, e);
        }
    }

    private NameValuePair[] nameValuePairs(Collection<Entry<String, String>> params) {
        var list = new ArrayList<NameValuePair>();
        for (var e : params) {
            var nvp = new NameValuePair(e.getKey(), e.getValue());
            list.add(nvp);
        }
        return list.toArray(new NameValuePair[0]);
    }

    private void setRequestHeaders(HttpMethod method) {
        for (var e : getRequestHeaders().entrySet()) {
            method.setRequestHeader(e.getKey(), e.getValue());
        }
    }

    public String concatPath(String start, String... end) {
        isTrue(isNotBlank(start), "start cannot be blank");
        notEmpty(end, "Must have end paths.");
        var path = start;
        for (var p : end) {
            path = removeEnd(path, "/") + "/" + removeStart(p, "/");
        }
        return path;
    }

    public String escapePath(String path) {
        try {
            return URLEncoder.encode(path, CharEncoding.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Should never happen", e);
        }
    }

    public String getRequestContentType() {
        return getRequestHeaders().get("Content-Type");
    }

    public void setRequestContentType(String contentType) {
        getRequestHeaders().put("Content-Type", contentType);
    }

    private GetMethod getMethod() {
        var method = new GetMethod(getUrl());
        setRequestHeaders(method);
        return method;
    }

    private PostMethod postMethod() {
        var method = new PostMethod(getUrl());
        setRequestHeaders(method);
        return method;
    }

    private PutMethod putMethod() {
        var method = new PutMethod(getUrl());
        setRequestHeaders(method);
        return method;
    }

    private DeleteMethod deleteMethod() {
        var method = new DeleteMethod(getUrl());
        setRequestHeaders(method);
        return method;
    }

    private String executeMethod(HttpMethod method) throws RestClientException {
        registerSslProtocol();
        try {
            int stat = client.executeMethod(method);
            var body = method.getResponseBodyAsString();
            var uri = method.getURI().getURI();
            var name = method.getName();
            log.trace("HTTP return code: " + stat);
            if (log.isDebugEnabled())
                log.debug(format("{0} {1}  HTTP Stat:{2}", name, uri, "" + stat));
            if (log.isTraceEnabled())
                log.trace("Response: " + body);
            if (!(200 <= stat && stat < 305)) {
                var error = "URI: " + uri + " HTTP Error: " + stat + " Response: \n" + body;
                log.error(error);
                throw new RestClientException(stat, uri, body);
            }
            return body;
        } catch (IOException e) {
            throw new RestClientException(e);
        }
    }

    private void registerSslProtocol() {
        if (sslSocketFactoryRegistered)
            return;
        ProtocolSocketFactory socketFactory = new EasySSLProtocolSocketFactory();
        Protocol.registerProtocol("https", new Protocol("https", socketFactory, 443));
        sslSocketFactoryRegistered = true;
    }

    /**
     * Base exception for a REST failure.
     * @author adamgent
     */
    public static class RestClientException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private int status = 0;
        private String uri = "";
        private String responseBody;
        private String message = null;

        public RestClientException(String message) {
            super(message);
        }

        public RestClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public RestClientException(Throwable cause) {
            super(cause);
        }

        public RestClientException() {
        }

        public RestClientException(RestClientException cause) {
            setStatus(cause.getStatus());
            setUri(cause.getUri());
            setResponseBody(cause.getResponseBody());
            setMessage(cause.getMessage());
        }

        public RestClientException(int status, String uri, InputStream responseBody) {
            init(status, uri, null);
            setMessage(getRestErrorMessage());
            fillInStackTrace();
        }

        public RestClientException(int status, String uri, String responseBody) {
            init(status, uri, responseBody);
            setMessage(getRestErrorMessage());
            fillInStackTrace();
        }

        @Override
        public String getMessage() {
            if (message != null) {
                return this.message;
            }
            return super.getMessage();
        }

        protected void setMessage(String message) {
            this.message = message;
        }

        protected String getRestErrorMessage() {
            return format("HTTP Error code: {0}\nURI: {1}\nResponse: {2}", getStatus(), getUri(), getResponseBody());
        }

        protected void init(int status, String uri, String responseBody) {
            this.status = status;
            this.uri = uri;
            this.responseBody = responseBody;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public void setResponseBody(String responseBody) {
            this.responseBody = responseBody;
        }
    }

    protected static final Logger log = LogManager.getLogger(PSRestClient.class);

    public String getPostContentType() {
        return postContentType;
    }

    public void setPostContentType(String postContentType) {
        this.postContentType = postContentType;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }
}

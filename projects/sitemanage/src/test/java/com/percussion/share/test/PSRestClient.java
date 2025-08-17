/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

import com.percussion.utils.http.PSModernHttpClient;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.delivery.client.EasySSLProtocolSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A Wrapper around Commons HTTP client for REST services. Most of the methods are protected as this
 * class should be extended to provide more specific behavior.
 *
 * @author adamgent
 */
public class PSRestClient {
    private String url;
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private String postContentType= "text/xml";
    
    private PSModernHttpClient client;
    
    public List<String> parseAcceptHeader(String acceptHeader) {
        return new ArrayList<String>(asList(acceptHeader.split(",")));
    }
    
    public String outputAcceptHeader(List<String> accepts) {
        return StringUtils.join(accepts, ",");
    }
    
    protected void addAccept(String mime) {
        List<String> accepts = parseAcceptHeader(getAcceptHeader());
        accepts.add(mime);
        setAcceptHeader(outputAcceptHeader(accepts));
    }
    protected String getAcceptHeader() {
        String accept = getRequestHeaders().get("Accept");
        if (accept == null)
            return "";
        return accept;
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
        // Initialize the modern HTTP client with the base URL
        if (url != null) {
            this.client = new PSModernHttpClient(url, requestHeaders);
        }
    }

    

    protected synchronized PSModernHttpClient getClient() {
        if (client == null && url != null) {
            client = new PSModernHttpClient(url, requestHeaders);
        }
        return client;
    }

    /**
     * Performs an HTTP GET.
     * @param path relative or fully qualified.
     * @param params never <code>null</code>.
     * @return never <code>null</code>.
     */
    protected String GET(String path, Collection<Entry<String, String>> params) {
        try {
            Map<String, String> paramMap = new HashMap<>();
            for (Entry<String, String> entry : params) {
                paramMap.put(entry.getKey(), entry.getValue());
            }
            return getClient().get(path, paramMap);
        } catch (IOException e) {
            throw new RestClientException("GET request failed", e);
        }
    }
    
    protected String GET(String path) {
        try {
            return getClient().get(path);
        } catch (IOException e) {
            throw new RestClientException("GET request failed", e);
        }
    }
    
    protected InputStream GET_BINARY(String path) {
        try {
            return getClient().getBinary(path);
        } catch (IOException e) {
            throw new RestClientException("GET binary request failed", e);
        }
    }
    
    protected String POST(String path, String body) {
        return POST(path, body, getPostContentType());
    }
    
    protected String POST(String path, String body, String contentType) {
        try {
            if (log.isTraceEnabled() && body != null) {
                log.trace("POST Body: " + body);
            }
            if (body == null) {
                body = "";
            }
            return getClient().post(path, body, contentType);
        } catch (IOException e) {
            throw new RestClientException("POST request failed", e);
        }
    }
    
    protected String PUT(String path, String body) {
        return PUT(path, body, getPostContentType());
    }
    
    protected String PUT(String path, String body, String contentType) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("PUT Body: " + body);
            }
            return getClient().put(path, body, contentType);
        } catch (IOException e) {
            throw new RestClientException("PUT request failed", e);
        }
    }
    
    protected String POST(String path, Collection<Entry<String, String>> params) {
        try {
            Map<String, String> paramMap = new HashMap<>();
            for (Entry<String, String> entry : params) {
                paramMap.put(entry.getKey(), entry.getValue());
            }
            return getClient().postForm(path, paramMap);
        } catch (IOException e) {
            throw new RestClientException("POST form request failed", e);
        }
    }
    
    protected String DELETE(String path) {
        try {
            return getClient().delete(path);
        } catch (IOException e) {
            throw new RestClientException("DELETE request failed", e);
        }
    }
    
    public String concatPath(String start, String ... end) {
        isTrue(isNotBlank(start), "start cannot be blank");
        notEmpty(end, "Must have end paths.");
        String path = start;
        for (String p : end ) {
            path = removeEnd(path, "/") + "/" + removeStart(p, "/");
        }
        return path;
    }
    
    public String escapePath(String path) {
        try
        {
            return URLEncoder.encode(path, CharEncoding.UTF_8);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("Should never happen", e);
        }
    }
    
    public String getRequestContentType() {
        return getRequestHeaders().get("Content-Type");
    }
    public void setRequestContentType(String contentType) {
        getRequestHeaders().put("Content-Type", contentType);
    }
    
    /**
     * 
     * Base exception for a REST failure.
     * 
     * @author adamgent
     *
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

    public RestClientException() {}

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
      return format(
          "HTTP Error code: {0}\nURI: {1}\nResponse: {2}",
          getStatus(), getUri(), getResponseBody());
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

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
package com.percussion.delivery.client;

import com.percussion.delivery.data.PSDeliveryInfo;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.MediaType;


/**
 * Client contract for issuing requests against the delivery tier from the CMS server, including
 * delivery-info lookups and proxy-aware REST calls.
 *
 * @author wesleyhirsch
 */
public interface IPSDeliveryClient
{
    /**
     * Base request method to get something from the delivery tier. It does not
     * support sending an enclosed entity.
     *
     * @param actionOptions An object specifying the delivery server, action url,
     * http method to use, etc. Must not be <code>null</code>.
     * @return Returns the plain string object sent by the delivery tier. Never
     * <code>null</code>, maybe empty.
     */
    public String getString(PSDeliveryActionOptions actionOptions);

    /**
     * Requests a JSON Object from a delivery server.
     *
     * @param actionOptions An object specifying the delivery server, action url,
     * http method to use, etc. Must not be <code>null</code>.
     * @return An Object representing the JSON response from the server. If there is no data
     * returned, returns an empty object. Will never be <code>null</code>,
     * may be empty.
     */
    public Object getJsonObject(PSDeliveryActionOptions actionOptions);

    /**
     * Requests a JSON Object from a delivery server, with the possibility to
     * specify an enclosed entity to send to the server (needs a POST or PUT
     * method).
     *
     * @param actionOptions An object specifying the delivery server, action url,
     * http method to use, etc. Must not be <code>null</code>.
     * @param requestMessageBody An object to be passed to the remote server. It
     * may be a String or a list of NameValuePair objects (in the case you want to
     * send data as an HTML form element). Must be pre-escaped, and in the correct
     * format. Maybe be <code>null</code>, in that case it runs in the same way as
     * if you were using getJsonObject(PSDeliveryActionOptions actionOptions).
     * @return An Object representing the JSON response from the server. If there is no data
     * returned, returns an empty object. Will never be <code>null</code>,
     * may be empty.
     *
     */
    public Object getJsonObject(PSDeliveryActionOptions actionOptions, Object requestMessageBody);

    /**
     * Requests a JSON Array from a delivery server.
     *
     * @param actionOptions An object specifying the delivery server, action url,
     * http method to use, etc. Must not be <code>null</code>.
     * @return An Object representing the JSON array response from the server. If there is no data
     * returned, returns an empty object. Will never be <code>null</code>,
     * may be empty.
     *
     */
    public Object getJsonArray(PSDeliveryActionOptions actionOptions);

    /**
     * Requests a JSON Array from a delivery server, with the possibility to
     * specify an enclosed entity to send to the server (needs a POST or PUT
     * method).
     *
     * @param actionOptions An object specifying the delivery server, action url,
     * http method to use, etc. Must not be <code>null</code>.
     * @param requestMessageBody An object to be passed to the remote server. It
     * may be a String or a list of NameValuePair objects (in the case you want to
     * send data as an HTML form element). Must be pre-escaped, and in the correct
     * format. Maybe be <code>null</code>, in that case it runs in the same way as
     * if you were using getJsonObject(PSDeliveryActionOptions actionOptions).
     * @return An Object representing the JSON array response from the server. If there is no data
     * returned, returns an empty object. Will never be <code>null</code>,
     * may be empty.
     *
     */
    public Object getJsonArray(PSDeliveryActionOptions actionOptions, Object requestMessageBody);

    /**
     * Base method to send something to the delivery server, where you don't expect
     * nothing back.
     *
     * @param actionOptions An object specifying the delivery server, action url,
     * http method to use, etc. Must not be <code>null</code>.
     * @param requestMessageBodyContentType the media type {@link MediaType} of the request
     * message body.  Defaults to {@link MediaType#APPLICATION_JSON} if blank.
     * @param requestMessageBody An object to be passed to the remote server. It
     * may be a String or a list of NameValuePair objects (in the case you want to
     * send data as an HTML form element). Must be pre-escaped, and in the correct
     * format, as specified in the request message type. Maybe be <code>null</code>,
     * in that case it runs in the same way as if you were using
     * getJsonObject(PSDeliveryActionOptions actionOptions).
     */
    public void push(PSDeliveryActionOptions actionOptions, String requestMessageBodyContentType,
            Object requestMessageBody);

    /**
     * Convenience method, call {@link #push(PSDeliveryActionOptions, String, Object) push(PSDeliveryActionOptions, null, Object)}
     * @param actionOptions An object specifying the delivery server, action url,
     * http method to use, etc. Must not be <code>null</code>.
     * @param requestMessageBody An object to be passed to the remote server. It
     * may be a String or a list of NameValuePair objects (in the case you want to
     * send data as an HTML form element). Must be pre-escaped, and in the correct
     * format, as specified in the request message type. Maybe be <code>null</code>,
     * in that case it runs in the same way as if you were using
     * getJsonObject(PSDeliveryActionOptions actionOptions).
     */
    public void push(PSDeliveryActionOptions actionOptions, Object requestMessageBody);

    public enum HttpMethodType
    {
    	// These methods are nullipotent, i.e., for N >= 0 requests, the result is always the same.  In other words, they don't change anything.
    	GET,
    	HEAD,

    	// These methods are idempotent, i.e., for N > 0 requests, the result is always the same.  In other words, they may change something, but what they change it to is always the same.
    	PUT,
    	DELETE,

    	// This method is volatile, i.e., for N requests, the result may be different every time, and/or, they may change things every time.  Be careful when using this.
    	POST,

    	// These methods are little known and not often used.  Look up how HTTP works to understand these.
    	OPTIONS,
    	TRACE,
    	CONNECT
    }

    /**
     * Represent a set of settings when executing the remote method in the
     * delivery tier. You can specify the delivery server, action URL, http
     * method to use, and whether the service is and "admin" one or not.
     *
     * @author miltonpividori
     *
     */
    public class PSDeliveryActionOptions
    {
        /**
         * The delivery server information object. Should not be <code>null</code>,
         * and it should have valid delivery server information.
         */
        private PSDeliveryInfo deliveryInfo;

        /**
         * Action URL of the remove service. For example "/perc-comments-services/comment".
         * It will be concatenated to the delivery host URL. Should not be <code>null</code>.
         */
        private String actionUrl;

        /**
         * Http method to use (i.e. GET, POST, PUT, etc).
         */
        private HttpMethodType httpMethod;

        /**
         * An "admin" call means that the operation is intented for admin users,
         * and it's secured in the delivery tier.
         */
        private boolean isAdminOperation;

        /**
         * Stores a list of "successful" HTTP status codes. If the delivery
         * server returns one of these, the delivery client will accept it
         * as a correct status code.
         */
        private Set<Integer> successfullHttpStatusCodes;

        public PSDeliveryActionOptions()
        {
            successfullHttpStatusCodes = new HashSet<>();
        }

       /**
        * Setup the options to use for the request to the DTS.
        *
        * @param deliveryInfo The DTS info, not <code>null</code>.
        * @param actionUrl The url to request
        * @param httpMethod The HTTP method to use, not <code>null</code>
        * @param isAdminOperation <code>true</code> to use HTTPS, <code>false</code> otherwise, as a rule <code>true</code> should always
        * be passed to allow for HTTPS-only communications from CM1.
        */
        public PSDeliveryActionOptions(PSDeliveryInfo deliveryInfo, String actionUrl, HttpMethodType httpMethod, boolean isAdminOperation)
        {
            this();

            this.deliveryInfo = deliveryInfo;
            this.actionUrl = actionUrl;
            this.httpMethod = httpMethod;
            this.isAdminOperation = isAdminOperation;
        }

        public PSDeliveryActionOptions(PSDeliveryInfo deliveryInfo, String actionUrl, HttpMethodType httpMethod)
        {
            this(deliveryInfo, actionUrl, httpMethod, true);
        }

        public PSDeliveryActionOptions(PSDeliveryInfo deliveryInfo, String actionUrl, boolean isAdminOperation)
        {
            this(deliveryInfo, actionUrl, HttpMethodType.GET, isAdminOperation);
        }

        public PSDeliveryActionOptions(PSDeliveryInfo deliveryInfo, String actionUrl)
        {
            this(deliveryInfo, actionUrl, HttpMethodType.GET);
        }

        public PSDeliveryInfo getDeliveryInfo()
        {
            return deliveryInfo;
        }

        public PSDeliveryActionOptions setDeliveryInfo(PSDeliveryInfo deliveryInfo)
        {
            this.deliveryInfo = deliveryInfo;
            return this;
        }

        public String getActionUrl()
        {
            return actionUrl;
        }

        public PSDeliveryActionOptions setActionUrl(String actionUrl)
        {
            this.actionUrl = actionUrl;
            return this;
        }

        public HttpMethodType getHttpMethod()
        {
            return httpMethod;
        }

        public PSDeliveryActionOptions setHttpMethod(HttpMethodType httpMethod)
        {
            this.httpMethod = httpMethod;
            return this;
        }

        public boolean isAdminOperation()
        {
            return isAdminOperation;
        }

        public PSDeliveryActionOptions setAdminOperation(boolean isAdminOperation)
        {
            this.isAdminOperation = isAdminOperation;
            return this;
        }

        public Set<Integer> getSuccessfullHttpStatusCodes()
        {
            return successfullHttpStatusCodes;
        }

        public PSDeliveryActionOptions setSuccessfullHttpStatusCodes(Set<Integer> successfullHttpStatusCodes)
        {
            this.successfullHttpStatusCodes = successfullHttpStatusCodes;
            return this;
        }

        public PSDeliveryActionOptions addSuccessfullHttpStatusCode(Integer successfullHttpStatusCode)
        {
            this.successfullHttpStatusCodes.add(successfullHttpStatusCode);
            return this;
        }
    }

    public static class PSDeliveryClientException extends RuntimeException
    {

        /**
         *
         */
        private static final long serialVersionUID = -7552921159505062022L;

        /** HTTP status when known; {@code -1} if not an HTTP response failure. */
        private final int statusCode;

        /** HTTP method label (e.g. PUT); may be {@code null}. */
        private final String httpMethod;

        /** Full request URL when known; may be {@code null}. */
        private final String requestUrl;

        /**
         * Short truncated response snippet (not multi-KB HTML); may be {@code null}.
         */
        private final String responseSnippet;

        /**
         *
         */
        public PSDeliveryClientException() {
            this(null, null, -1, null, null, null);
        }

        /**
         * @param message
         * @param cause
         */
        public PSDeliveryClientException(String message, Throwable cause) {
            this(message, cause, -1, null, null, null);
        }

        /**
         * @param message
         */
        public PSDeliveryClientException(String message) {
            this(message, null, -1, null, null, null);
        }

        /**
         * @param cause
         */
        public PSDeliveryClientException(Throwable cause) {
            this(cause != null ? cause.getMessage() : null, cause, -1, null, null, null);
        }

        /**
         * Structured HTTP failure for operators and callers that need status/URL without parsing
         * the message.
         *
         * @param message log/exception message (already truncated if body was included)
         * @param statusCode HTTP status, or {@code -1} when unknown
         * @param httpMethod method label; may be {@code null}
         * @param requestUrl full URL; may be {@code null}
         * @param responseSnippet short body snippet; may be {@code null}
         */
        public PSDeliveryClientException(
                String message,
                int statusCode,
                String httpMethod,
                String requestUrl,
                String responseSnippet) {
            this(message, null, statusCode, httpMethod, requestUrl, responseSnippet);
        }

        private PSDeliveryClientException(
                String message,
                Throwable cause,
                int statusCode,
                String httpMethod,
                String requestUrl,
                String responseSnippet) {
            super(message, cause);
            this.statusCode = statusCode;
            this.httpMethod = httpMethod;
            this.requestUrl = requestUrl;
            this.responseSnippet = responseSnippet;
        }

        /** @return HTTP status when known; {@code -1} otherwise */
        public int getStatusCode() {
            return statusCode;
        }

        /** @return HTTP method label, or {@code null} */
        public String getHttpMethod() {
            return httpMethod;
        }

        /** @return full request URL when known, or {@code null} */
        public String getRequestUrl() {
            return requestUrl;
        }

        /** @return short response snippet when known, or {@code null} */
        public String getResponseSnippet() {
            return responseSnippet;
        }
    }
}

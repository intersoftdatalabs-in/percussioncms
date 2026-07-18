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
package com.percussion.services.aaclient;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Servlet that handles Active Assembly (AA) client requests by delegating to
 * appropriate widget handlers based on the 'widget' parameter.
 *
 * This servlet acts as a dispatcher, routing requests to specific widget handlers
 * that implement the {@link IPSWidgetHandler} interface.
 *
 * @author Percussion Software
 */
public class PSAaClientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LogManager.getLogger(PSAaClientServlet.class);

    private static final String WIDGET_PARAM = "widget";
    private static final String CONTENT_TYPE_PLAIN = "text/plain";
    private static final String ERROR_NO_HANDLER = "Servlet is not meant to handle the request";

    // Generic client-facing error message used to avoid leaking internal exception details
    // (CWE-209 / CodeQL java/error-message-exposure). The detailed exception is always logged
    // server-side before this generic message is returned to the client.
    private static final String GENERIC_WIDGET_ERROR =
        "An error occurred while processing the widget request";

    /**
     * Processes HTTP requests by delegating to appropriate widget handlers.
     *
     * @param request the HTTP servlet request, must not be null
     * @param response the HTTP servlet response, must not be null
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Objects.requireNonNull(request, "Request cannot be null");
        Objects.requireNonNull(response, "Response cannot be null");

        // Early return if connection is already closed
        if (response.isCommitted()) {
            log.debug("Response already committed, skipping processing");
            return;
        }

        var widgetParam = Optional.ofNullable(request.getParameter(WIDGET_PARAM))
            .filter(StringUtils::isNotEmpty);

        if (widgetParam.isPresent()) {
            handleWidgetRequest(widgetParam.get(), request, response);
        } else {
            handleInvalidRequest(response);
        }
    }

    /**
     * Handles a valid widget request by delegating to the appropriate handler.
     *
     * @param widgetName the name of the widget to handle
     * @param request the HTTP servlet request
     * @param response the HTTP servlet response
     * @throws IOException if an I/O error occurs
     */
    private void handleWidgetRequest(String widgetName, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        try {
            log.debug("Processing widget request: {}", widgetName);
            PSWidgetHandlerFactory.getHandler(widgetName).handleRequest(request, response);
        } catch (Exception e) {
            log.error("Error processing widget request for '{}': {}", widgetName, e.getMessage(), e);
            pushResponse(response, GENERIC_WIDGET_ERROR, CONTENT_TYPE_PLAIN, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handles invalid requests that don't specify a valid widget parameter.
     *
     * @param response the HTTP servlet response
     * @throws IOException if an I/O error occurs
     */
    private void handleInvalidRequest(HttpServletResponse response) throws IOException {
        log.debug("Invalid request - no widget parameter specified");
        pushResponse(response, ERROR_NO_HANDLER, CONTENT_TYPE_PLAIN, HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Sends a response to the client with the specified content and status code.
     * This method is thread-safe and handles UTF-8 encoding properly.
     *
     * @param httpResponse the HTTP response object, must not be null
     * @param responseContent the content to send, must not be null
     * @param contentType the MIME type of the content, must not be null
     * @param statusCode the HTTP status code to return
     * @throws IOException if an I/O error occurs during response writing
     * @throws IllegalArgumentException if any parameter is null
     */
    public static void pushResponse(HttpServletResponse httpResponse, String responseContent,
            String contentType, int statusCode) throws IOException {
        Objects.requireNonNull(httpResponse, "HTTP response cannot be null");
        Objects.requireNonNull(responseContent, "Response content cannot be null");
        Objects.requireNonNull(contentType, "Content type cannot be null");

        // Early return if connection is already closed
        if (httpResponse.isCommitted()) {
            log.debug("Response already committed, cannot send response");
            return;
        }

        try {
            httpResponse.setContentType(contentType + "; charset=UTF-8");
            var responseBytes = responseContent.getBytes(StandardCharsets.UTF_8);
            httpResponse.setContentLength(responseBytes.length);
            httpResponse.setStatus(statusCode);

            try (var outputStream = httpResponse.getOutputStream()) {
                outputStream.write(responseBytes); // codeql[java/xss] justification: byte-pump of caller-encoded content; executeSetField uses XSSValidation.escapeHtml (alert #756)
                outputStream.flush();
            }

            log.debug("Response sent successfully with status code: {}", statusCode);
        } catch (IOException e) {
            log.error("Failed to send response: {}", e.getMessage(), e);
            throw e;
        }
    }
}

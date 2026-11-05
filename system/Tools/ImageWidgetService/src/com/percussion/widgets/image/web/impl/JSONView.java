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

package com.percussion.widgets.image.web.impl;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Spring MVC view for rendering JSON responses with optional HTML wrapping.
 * Supports debug formatting, jQuery form file uploads, and AJAX requests.
 * This view provides flexible JSON output formatting based on request parameters.
 *
 * @since Java 11
 */
public class JSONView extends AbstractView implements View {

    private static final Logger log = LogManager.getLogger(JSONView.class);

    /** Default content type for JSON responses */
    private static final String DEFAULT_JSON_CONTENT_TYPE = "application/json";

    /** Content type for plain text responses */
    private static final String PLAIN_TEXT_CONTENT_TYPE = "text/plain";

    /** Content type for HTML responses */
    private static final String HTML_CONTENT_TYPE = "text/html";

    /** Parameter name for debug mode */
    private static final String DEBUG_PARAM = "debug";

    /** Parameter name for HTML forcing */
    private static final String FORCE_HTML_PARAM = "forceHTML";

    /** Parameter name for jQuery form file uploads */
    private static final String JQUERY_FORM_FILE_PARAM = "jQueryFormFile";

    /** Header name for AJAX requests */
    private static final String AJAX_HEADER = "X-Requested-With";

    /** Debug JSON indentation level */
    private static final int DEBUG_INDENT_LEVEL = 3;

    private String contentType = DEFAULT_JSON_CONTENT_TYPE;
    private String htmlContentType = HTML_CONTENT_TYPE;
    private String modelObjectName;

    /**
     * Default constructor initializing with default content types.
     */
    public JSONView() {
        setContentType(DEFAULT_JSON_CONTENT_TYPE);
    }

    /**
     * Constructor with custom model object name.
     *
     * @param modelObjectName the name of the model object containing JSON data
     */
    public JSONView(String modelObjectName) {
        this();
        setModelObjectName(modelObjectName);
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {

        var renderContext = createRenderContext(request);

        try (var writer = response.getWriter()) {
            configureResponse(response, renderContext);
            renderJsonContent(model, writer, renderContext);

            log.debug("Successfully rendered JSON view for model object: {}",
                Optional.ofNullable(modelObjectName).orElse("default"));

        } catch (IOException e) {
            log.error("Error rendering JSON view", e);
            throw e;
        }
    }

    /**
     * Creates a render context from the HTTP request.
     *
     * @param request the HTTP request
     * @return render context with formatting options
     */
    private RenderContext createRenderContext(HttpServletRequest request) {
        var debug = WebUtils.hasSubmitParameter(request, DEBUG_PARAM);
        var forceHtml = WebUtils.hasSubmitParameter(request, FORCE_HTML_PARAM);
        var jQueryFormFile = WebUtils.hasSubmitParameter(request, JQUERY_FORM_FILE_PARAM);

        var requestedWith = Optional.ofNullable(request.getHeader(AJAX_HEADER))
            .filter(StringUtils::isNotBlank);

        var isAjax = requestedWith.isPresent();

        return new RenderContext(debug, forceHtml, jQueryFormFile, isAjax);
    }

    /**
     * Configures the HTTP response based on render context.
     *
     * @param response the HTTP response
     * @param context the render context
     */
    private void configureResponse(HttpServletResponse response, RenderContext context) {
        var responseContentType = determineContentType(context);
        response.setContentType(responseContentType);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // Add security headers for JSON responses
        if (!context.forceHtml() && !context.jQueryFormFile()) {
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        }
    }

    /**
     * Determines the appropriate content type based on render context.
     *
     * @param context the render context
     * @return the content type string
     */
    private String determineContentType(RenderContext context) {
        if (context.forceHtml() || context.jQueryFormFile()) {
            return htmlContentType;
        }
        return contentType;
    }

    /**
     * Renders the JSON content to the writer.
     *
     * @param model the model map
     * @param writer the response writer
     * @param context the render context
     * @throws IOException if writing fails
     */
    private void renderJsonContent(Map<String, Object> model, PrintWriter writer,
                                 RenderContext context) throws IOException {

        // Write HTML wrapper if needed
        if (context.forceHtml()) {
            writer.write("<html><head><title>ImageResult</title></head>");
            writer.write("<body><textarea rows=\"33\" cols=\"100\">");
        } else if (context.jQueryFormFile()) {
            writer.write("<textarea>");
        }

        // Write JSON content
        var jsonContent = extractJsonContent(model, context);
        writer.write(jsonContent);

        // Close HTML wrapper if needed
        if (context.forceHtml()) {
            writer.write("</textarea></body></html>");
        } else if (context.jQueryFormFile()) {
            writer.write("</textarea>");
        }

        writer.flush();
    }

    /**
     * Extracts JSON content from the model.
     *
     * @param model the model map
     * @param context the render context
     * @return the JSON string
     */
    private String extractJsonContent(Map<String, Object> model, RenderContext context) {
        if (StringUtils.isNotBlank(modelObjectName)) {
            return extractNamedJsonObject(model, context);
        } else {
            return extractFirstJsonObject(model, context);
        }
    }

    /**
     * Extracts JSON object by name from the model.
     *
     * @param model the model map
     * @param context the render context
     * @return the JSON string
     */
    private String extractNamedJsonObject(Map<String, Object> model, RenderContext context) {
        var obj = model.get(modelObjectName);
        if (obj == null) {
            return "{}";
        }
        return convertToJson(obj, context.debug());
    }

    /**
     * Extracts the first JSON-serializable object found in the model.
     *
     * @param model the model map
     * @param context the render context
     * @return the JSON string
     */
    private String extractFirstJsonObject(Map<String, Object> model, RenderContext context) {
        return model.values().stream()
            .filter(Objects::nonNull)
            .findFirst()
            .map(obj -> convertToJson(obj, context.debug()))
            .orElse("{}");
    }

    /**
     * Converts an object to JSON string using Jackson.
     *
     * @param obj the object to convert
     * @param debug whether to use debug formatting
     * @return the JSON string
     */
    private String convertToJson(Object obj, boolean debug) {
        try {
            var builder = JsonMapper.builder();
            if (debug) {
                builder.enable(SerializationFeature.INDENT_OUTPUT);
            }
            return builder.build().writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Error converting object to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    // Getters and setters with validation

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = StringUtils.isNotBlank(contentType)
            ? contentType.trim()
            : DEFAULT_JSON_CONTENT_TYPE;
    }

    public String getHtmlContentType() {
        return htmlContentType;
    }

    public void setHtmlContentType(String htmlContentType) {
        this.htmlContentType = StringUtils.isNotBlank(htmlContentType)
            ? htmlContentType.trim()
            : HTML_CONTENT_TYPE;
    }

    public String getModelObjectName() {
        return modelObjectName;
    }

    public void setModelObjectName(String modelObjectName) {
        this.modelObjectName = StringUtils.isBlank(modelObjectName)
            ? null
            : modelObjectName.trim();
    }

    /**
     * Gets the model object name as an Optional.
     *
     * @return Optional containing the model object name, or empty if not set
     */
    public Optional<String> getModelObjectNameOptional() {
        return Optional.ofNullable(modelObjectName)
            .filter(StringUtils::isNotBlank);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        var that = (JSONView) obj;
        return Objects.equals(contentType, that.contentType) &&
               Objects.equals(htmlContentType, that.htmlContentType) &&
               Objects.equals(modelObjectName, that.modelObjectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentType, htmlContentType, modelObjectName);
    }

    @Override
    public String toString() {
        return String.format("JSONView{contentType='%s', htmlContentType='%s', modelObjectName='%s'}",
            contentType, htmlContentType, modelObjectName);
    }

    /**
     * POJO representing the render context for JSON view rendering.
     */
    private static class RenderContext {
        private final boolean debug;
        private final boolean forceHtml;
        private final boolean jQueryFormFile;
        private final boolean isAjax;

        public RenderContext(boolean debug, boolean forceHtml, boolean jQueryFormFile, boolean isAjax) {
            this.debug = debug;
            this.forceHtml = forceHtml;
            this.jQueryFormFile = jQueryFormFile;
            this.isAjax = isAjax;
        }

        public boolean debug() { return debug; }
        public boolean forceHtml() { return forceHtml; }
        public boolean jQueryFormFile() { return jQueryFormFile; }
        public boolean isAjax() { return isAjax; }
    }
}

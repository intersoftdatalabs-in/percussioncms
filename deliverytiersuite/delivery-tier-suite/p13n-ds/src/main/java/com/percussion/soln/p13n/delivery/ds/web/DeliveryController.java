// REFACTORED: CP-JAVA11
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

package com.percussion.soln.p13n.delivery.ds.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.percussion.soln.p13n.delivery.DeliveryException;
import com.percussion.soln.p13n.delivery.DeliveryRequest;
import com.percussion.soln.p13n.delivery.DeliveryResponse;
import com.percussion.soln.p13n.delivery.IDeliveryService;
import com.percussion.soln.p13n.delivery.web.DeliveryWebUtils;
import com.percussion.soln.p13n.tracking.VisitorProfile;
import com.percussion.soln.p13n.tracking.web.IVisitorTrackingHttpService;

/**
 * Controller for handling delivery requests.
 * Sunny Sal says: "Controller like a boss, debug like a hero!"
 */
@Controller
public class DeliveryController {

    public static final String JSONP_CALLBACK_PARAM = "jsoncallback";
    private static final Log log = LogFactory.getLog(DeliveryController.class);
    private IDeliveryService deliveryService;
    private IVisitorTrackingHttpService visitorTrackingHttpService;

    public DeliveryController() {
        // Default constructor
    }

    protected ModelAndView handle(HttpServletRequest request,
            HttpServletResponse response, Object obj, BindException bindException)
            throws Exception {
        var sw = log.isDebugEnabled() ? new StopWatch() : null;
        if (sw != null) {
            log.debug("Handling delivery request command : " + obj);
            sw.start();
        }
        var deliveryRequest = getDeliveryRequest(request, obj, bindException);
        var jsonp = new JSONP();
        jsonp.setCallback(request.getParameter(JSONP_CALLBACK_PARAM));
        ModelAndView mv;
        if (bindException.hasErrors()) {
            mv = outputError(bindException, jsonp, response);
        } else {
            mv = handle(deliveryRequest, jsonp, response);
        }
        if (sw != null) {
            sw.stop();
            log.debug("Delivery Controller took: " + sw.getTime() + "ms");
        }
        return mv;
    }

    public ModelAndView handle(DeliveryRequest deliveryRequest,
            JSONP json,
            HttpServletResponse response)
            throws Exception {
        try {
            var deliveryResponse = deliveryService.deliver(deliveryRequest);
            return outputJSON(json, deliveryResponse, response);
        } catch (DeliveryException de) {
            return outputError(de, json, response);
        }
    }

    protected ModelAndView outputError(BindException e, JSONP json, HttpServletResponse response)
            throws IOException {
        DeliveryResponse deliveryResponse;
        log.debug("Bad delivery request. Failed validation: ", e);
        var fe = e.getFieldError();
        if (fe != null) {
            deliveryResponse = new DeliveryResponse(fe.getCode(), fe.getDefaultMessage());
        } else if (e.getGlobalError() != null) {
            var error = e.getGlobalError();
            deliveryResponse = new DeliveryResponse(error.getCode(), error.getDefaultMessage());
        } else {
            deliveryResponse = new DeliveryResponse(e);
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return outputJSON(json, deliveryResponse, response);
    }

    protected ModelAndView outputError(Exception e, JSONP json, HttpServletResponse response)
            throws IOException {
        log.warn("Bad delivery request: ", e);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        var deliveryResponse = new DeliveryResponse(e);
        return outputJSON(json, deliveryResponse, response);
    }

    protected ModelAndView outputJSON(
            JSONP jsonp,
            DeliveryResponse deliveryResponse,
            HttpServletResponse response) throws IOException {
        var json = DeliveryWebUtils.responseToJson(deliveryResponse);
        jsonp.setJson(json);
        return outputJSON(jsonp, response);
    }

    protected ModelAndView outputJSON(JSONP obj,
            HttpServletResponse response) throws IOException {
        var writer = response.getWriter();
        writer.print(obj.toString());
        return null;
    }

    protected DeliveryRequest getDeliveryRequest(HttpServletRequest request, Object command, BindException errors) {
        var deliveryRequest = (DeliveryRequest) command;
        var profile = getVisitorProfile(request, errors);

        if (deliveryRequest != null) {
            deliveryRequest.setVisitorProfile(profile);
        }

        log.debug(request.getMethod());
        log.debug(request.getContentType());
        if (deliveryRequest instanceof BadJSONDeliveryRequest) {
            errors.addError(new ObjectError("json.error", "Failed to unmarshal JSON POST"));
        } else if (deliveryRequest != null && deliveryRequest.getListItem() != null) {
            log.debug("List Item is in request");
        } else {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "listItemId", "field.required", "listItemId is required");
        }

        return deliveryRequest;
    }

    protected VisitorProfile getVisitorProfile(HttpServletRequest request, BindException errors) {
        var profile = getVisitorProfile(request);
        if (profile == null) {
            errors.reject("profile.not_found", "Profile not found.");
        }
        return profile;
    }

    private VisitorProfile getVisitorProfile(HttpServletRequest request) {
        return getVisitorTrackingHttpService().resolveProfile(null, request);
    }

    @RequestMapping
    public void handleRequest(MockHttpServletRequest request, MockHttpServletResponse response) {
        // No-op for mock requests
    }

    protected static class BadJSONDeliveryRequest extends DeliveryRequest {
        // Marker for bad JSON requests
    }

    protected Object getCommand(HttpServletRequest request) throws Exception {
        if (isJsonRestRequest(request)) {
            DeliveryRequest target;
            try {
                var payload = inputStreamAsString(request.getInputStream());
                log.debug("JSON request payload - " + payload);
                target = jsonToDeliveryRequest(payload);
            } catch (Exception e) {
                log.error("Failed to serialize JSON POST request to DeliveryRequest: ", e);
                target = new BadJSONDeliveryRequest();
            }
            return target;
        } else {
            return null;
        }
    }

    protected String inputStreamAsString(InputStream stream) throws IOException {
        try (var br = new BufferedReader(new InputStreamReader(stream))) {
            var sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    protected DeliveryRequest jsonToDeliveryRequest(String json) {
        return DeliveryWebUtils.jsonToRequest(json);
    }

    protected boolean suppressBinding(HttpServletRequest request) {
        return isJsonRestRequest(request);
    }

    protected boolean isJsonRestRequest(HttpServletRequest request) {
        var contentType = request.getContentType();
        return contentType != null &&
                contentType.startsWith("application/json") &&
                "POST".equals(request.getMethod());
    }

    public IDeliveryService getDeliveryService() {
        return deliveryService;
    }

    public void setDeliveryService(IDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    public IVisitorTrackingHttpService getVisitorTrackingHttpService() {
        return visitorTrackingHttpService;
    }

    public void setVisitorTrackingHttpService(
            IVisitorTrackingHttpService visitorTrackingWebMediator) {
        this.visitorTrackingHttpService = visitorTrackingWebMediator;
    }
}

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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interface for handling widget requests in the AA client system.
 * Implementations of this interface process HTTP requests and generate
 * appropriate responses for specific widget types.
 *
 * @author Percussion Software
 */
@FunctionalInterface
public interface IPSWidgetHandler {

    /**
     * Handles an HTTP request for a specific widget type.
     *
     * @param request the HTTP servlet request, must not be null
     * @param response the HTTP servlet response, must not be null
     * @throws Exception if an error occurs during request processing
     */
    void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception;
}

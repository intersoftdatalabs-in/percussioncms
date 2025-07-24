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

package com.percussion.delivery.jaxrs;

import javax.ws.rs.ext.Provider;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

/**
 * Custom Jackson JSON provider for JAX-RS.
 *
 * <p>Sunny Sal says: This provider keeps your JSON smooth and your stacktraces clean!
 *
 * // REFACTORED: CP-JAVA11
 */
@Provider
public class PSJacksonJaxbJsonProvider extends JacksonJaxbJsonProvider {
    // We override the default Jackson providers to ensure correct mapping and error handling.
    // These are included in the package we scan from com.sun.jersey.config.property.packages in web.xml.
    // We still want the JacksonJaxbJsonProvider, so we just extend it and find it here.
    // More options are available when we upgrade JAX-RS from 1.1 to 2.0.
    // See discussion: https://github.com/fasterxml/jackson-jaxrs-providers/issues/22
}

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
package com.percussion.delivery.polls;

import com.percussion.delivery.exceptions.PSJsonMappingErrorResponse;
import com.percussion.delivery.exceptions.PSUncaughtError;
import com.percussion.delivery.polls.services.PSPollsRestService;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import tools.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

/**
 * Jersey application configuration for Polls REST API. Sunny Sal: Refactored for Java 11, Google
 * style, and better grammar.
 *
 * <p>This class is {@code final} so no subclass can override {@link ResourceConfig#register} and
 * observe a partially constructed instance during construction (avoids {@code this-escape} under
 * {@code -Xlint:all}).
 */
@ApplicationPath("/")
public final class PSPollsApplication extends ResourceConfig {
  /**
   * Registers the Jersey resources and features used by the polls REST application: the polls REST
   * service, request/response logging, roles-based access control, the standard error mappers and
   * the JSON binding provider. The class is {@code final} so no subclass can override {@link
   * ResourceConfig#register} and observe a partially-constructed instance during this constructor.
   */
  public PSPollsApplication() {
    // RequestContextFilter registration removed; Jersey 2.x Spring integration does not require it.
    // Removed AutowiredInjectResolver registration; not required for Jersey 2.x Spring integration.
    // Removed SpringWebApplicationInitializer registration; not required for Jersey 2.x Spring
    // integration.
    register(PSPollsRestService.class);
    register(LoggingFeature.class);
    register(RolesAllowedDynamicFeature.class);
    register(PSJsonMappingErrorResponse.class);
    register(PSUncaughtError.class);
    register(JacksonXmlBindJsonProvider.class);
  }
}

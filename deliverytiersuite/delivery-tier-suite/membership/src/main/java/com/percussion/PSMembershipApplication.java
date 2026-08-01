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

package com.percussion;

// REFACTORED: CP-JAVA11

import com.percussion.delivery.exceptions.PSJsonMappingErrorResponse;
import com.percussion.delivery.exceptions.PSUncaughtError;
import com.percussion.generickey.utils.services.impl.PSGenericKeyRestService;
import com.percussion.membership.services.impl.PSMembershipRestService;
import jakarta.ws.rs.ApplicationPath;
import java.util.List;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.spring.AutowiredInjectResolver;
import org.glassfish.jersey.server.spring.SpringComponentProvider;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.SpringWebApplicationInitializer;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import tools.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

/**
 * JAX-RS Application configuration for Percussion Membership Services. Configures Jersey with
 * Spring integration and REST services using Java 11 features.
 *
 * @author Percussion Software
 * @since 8.1.6
 */
@ApplicationPath("/")
public class PSMembershipApplication extends ResourceConfig {

  /**
   * Initialize the JAX-RS application with required components. Sets up Spring integration, REST
   * services, and exception handling using modern Java patterns.
   */
  @SuppressWarnings("this-escape")
  public PSMembershipApplication() {
    registerSpringComponents();
    registerRestServices();
    registerExceptionHandlers();
    registerProviders();
  }

  /** Register Spring integration components using Java 11 features. */
  private void registerSpringComponents() {
    var springComponents =
        List.of(
            RequestContextFilter.class,
            SpringComponentProvider.class,
            AutowiredInjectResolver.class,
            SpringLifecycleListener.class,
            SpringWebApplicationInitializer.class);

    springComponents.forEach(this::register);
  }

  /** Register REST service classes. */
  private void registerRestServices() {
    var restServices = List.of(PSMembershipRestService.class, PSGenericKeyRestService.class);

    restServices.forEach(this::register);
  }

  /** Register exception handling components. */
  private void registerExceptionHandlers() {
    var exceptionHandlers = List.of(PSJsonMappingErrorResponse.class, PSUncaughtError.class);

    exceptionHandlers.forEach(this::register);
  }

  /** Register JAX-RS providers and features. */
  private void registerProviders() {
    var providers =
        List.of(
            LoggingFeature.class,
            RolesAllowedDynamicFeature.class,
            JacksonXmlBindJsonProvider.class);

    providers.forEach(this::register);
  }
}

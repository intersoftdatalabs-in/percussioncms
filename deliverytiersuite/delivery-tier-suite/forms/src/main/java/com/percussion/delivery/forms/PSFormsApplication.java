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

package com.percussion.delivery.forms;

import com.percussion.delivery.exceptions.PSJsonMappingErrorResponse;
import com.percussion.delivery.exceptions.PSUncaughtError;
import com.percussion.delivery.forms.impl.PSFormRestService;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.spring.AutowiredInjectResolver;
import org.glassfish.jersey.server.spring.SpringComponentProvider;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.SpringWebApplicationInitializer;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

/**
 * The JAX-RS application class for the Forms delivery tier service. Registers all REST resource
 * classes, providers, and Jersey features required to expose the {@link PSFormRestService}
 * endpoints, including Spring integration, JSON marshalling, CSRF support, and exception mapping.
 *
 * <p>The application is mounted at the root context path so all form-related URIs are served by the
 * same Jersey container.
 */
@ApplicationPath("/")
public class PSFormsApplication extends ResourceConfig {
  /**
   * Registers the Jersey features and resource / provider classes that make up the Forms
   * delivery-tier service. The {@code register} invocations follow the Jersey-initializer pattern
   * documented by the {@code ResourceConfig} API; they intentionally run before the subclass is
   * fully constructed.
   */
  @SuppressWarnings("this-escape")
  public PSFormsApplication() {
    register(RequestContextFilter.class);
    register(SpringComponentProvider.class);
    register(AutowiredInjectResolver.class);
    register(SpringLifecycleListener.class);
    register(SpringWebApplicationInitializer.class);
    register(PSFormRestService.class);
    register(LoggingFeature.class);
    register(RolesAllowedDynamicFeature.class);
    register(PSJsonMappingErrorResponse.class);
    register(PSUncaughtError.class);
    register(JacksonJsonProvider.class);
  }
}

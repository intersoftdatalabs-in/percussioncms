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

package com.percussion.delivery;

import com.percussion.delivery.comments.services.PSCommentsRestService;
import com.percussion.delivery.exceptions.PSJsonMappingErrorResponse;
import com.percussion.delivery.exceptions.PSUncaughtError;
import com.percussion.delivery.likes.services.PSLikesRestService;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.spring.AutowiredInjectResolver;
import org.glassfish.jersey.server.spring.SpringComponentProvider;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import tools.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

/**
 * Jersey application configuration for Percussion CMS REST services. Registers REST resources,
 * filters, and providers.
 *
 * @author Sunny Sal
 */
@ApplicationPath("/")
public class PSCommentsApplication extends ResourceConfig {

  /**
   * Registers Jersey/Spring components, REST resources, features, and providers for the comments
   * and likes REST APIs. The {@link ResourceConfig#register} methods invoked here are overridable
   * on this subclass, which is why this constructor carries a targeted {@code this-escape}
   * suppression: subclasses (if any) are not expected to be deserialized, and Jersey instantiates
   * this class exactly once during application bootstrap, before any subclass overrides could
   * become visible.
   */
  @SuppressWarnings("this-escape")
  public PSCommentsApplication() {
    // Register Jersey and Spring integration components
    register(RequestContextFilter.class);
    register(SpringComponentProvider.class);
    register(AutowiredInjectResolver.class);
    register(SpringLifecycleListener.class);

    // Register REST services
    register(PSLikesRestService.class);
    register(PSCommentsRestService.class);

    // Register features and error handlers
    register(LoggingFeature.class);
    register(RolesAllowedDynamicFeature.class);
    register(PSJsonMappingErrorResponse.class);
    register(PSUncaughtError.class);

    // Register Jackson JSON provider
    register(JacksonXmlBindJsonProvider.class);
  }
}

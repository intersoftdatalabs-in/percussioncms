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

package com.percussion.delivery.feeds;

import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;
import com.percussion.delivery.exceptions.PSJsonMappingErrorResponse;
import com.percussion.delivery.exceptions.PSUncaughtError;
import com.percussion.delivery.feeds.services.PSFeedService;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.spring.AutowiredInjectResolver;
import org.glassfish.jersey.server.spring.SpringComponentProvider;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.SpringWebApplicationInitializer;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;

/**
 * Jersey application configuration for the feeds REST endpoints. Mounted at the application root
 * and registers the Spring/Jersey integration, JSON providers, exception mappers, and the {@link
 * PSFeedService} resource.
 */
@ApplicationPath("/")
public class PSFeedsApplication extends ResourceConfig {

  /**
   * Registers the request-scoped filter, Spring component provider, lifecycle listeners, the feed
   * REST service, JSON binding provider, and exception mappers used by the feeds application.
   */
  public PSFeedsApplication() {
    register(RequestContextFilter.class);
    register(SpringComponentProvider.class);
    register(AutowiredInjectResolver.class);
    register(SpringLifecycleListener.class);
    register(SpringWebApplicationInitializer.class);
    register(PSFeedService.class);
    register(LoggingFeature.class);
    register(RolesAllowedDynamicFeature.class);
    register(PSJsonMappingErrorResponse.class);
    register(PSUncaughtError.class);
    register(JacksonXmlBindJsonProvider.class);
  }
}

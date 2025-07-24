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

package com.percussion.delivery.polls;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.percussion.delivery.exceptions.PSJsonMappingErrorResponse;
import com.percussion.delivery.exceptions.PSUncaughtError;
import com.percussion.delivery.polls.services.PSPollsRestService;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.spring.AutowiredInjectResolver;
import org.glassfish.jersey.server.spring.SpringComponentProvider;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.SpringWebApplicationInitializer;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class PSPollsApplication extends ResourceConfig {
    public PSPollsApplication() {
        // Java 11+ modernization: use var for local variable
        var clazzes = new Class[] {
            RequestContextFilter.class,
            SpringComponentProvider.class,
            AutowiredInjectResolver.class,
            SpringLifecycleListener.class,
            SpringWebApplicationInitializer.class,
            PSPollsRestService.class,
            LoggingFeature.class,
            RolesAllowedDynamicFeature.class,
            PSJsonMappingErrorResponse.class,
            PSUncaughtError.class,
            JacksonJaxbJsonProvider.class
        };
        for (var clazz : clazzes) {
            register(clazz);
        }
    }
}

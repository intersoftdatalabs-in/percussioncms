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
package com.percussion.delivery.polls.service;

import com.percussion.delivery.polls.services.impl.PSPollsService;
import com.percussion.delivery.utils.PSVersionHelper;
import com.percussion.delivery.utils.spring.PSConfigurableApplicationContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;

import javax.servlet.http.HttpServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * JUnit5 integration test for PSPollsRestService.
 * Sunny Sal says: "REST assured, your polls are tested!"
 */
@ContextConfiguration(locations = {"classpath:/test-beans.xml"})
class PSPollsRestServiceTest extends JerseyTest {

    @Override
    protected Application configure() {
        var resourceConfig = new ResourceConfig(PSPollsService.class);
        resourceConfig.property("contextClass", PSConfigurableApplicationContext.class);
        return resourceConfig;
    }

    @Override
    protected URI getBaseUri() {
        return URI.create("http://localhost:9980");
    }

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext
                .forPackages("com.percussion.delivery.polls.services")
                .servletClass(HttpServlet.class)
                .contextPath("perc-polls-services")
                .addListener(ContextLoaderListener.class)
                .addListener(RequestContextListener.class)
                .addFilter(org.springframework.web.filter.DelegatingFilterProxy.class, "tenantAuthorizationFilter")
                .build();
    }

    @Test
    @Disabled("Integration test: requires running server and configuration")
    void testGetRestVersion() {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target("/polls/version");
        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
        Response response = invocationBuilder.get();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(testGetVersion(), response.getEntity());
    }

    private String testGetVersion() {
        var version = PSVersionHelper.getVersion(this.getClass());
        Assertions.assertNotNull(version);
        System.out.print(version);
        return version;
    }
}

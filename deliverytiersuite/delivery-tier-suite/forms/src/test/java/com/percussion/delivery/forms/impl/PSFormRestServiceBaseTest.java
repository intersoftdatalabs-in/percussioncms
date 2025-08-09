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
package com.percussion.delivery.forms.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.delivery.utils.PSVersionHelper;
import com.percussion.delivery.utils.spring.PSConfigurableApplicationContext;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import javax.servlet.http.HttpServlet;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;

/**
 * @author natechadwick
 */
public class PSFormRestServiceBaseTest extends JerseyTest {

  public PSFormRestServiceBaseTest() {}

  /***
   * Takes the context file as an arg and spins up grizzly to
   * test rest methods.
   */
  @Override
  protected Application configure() {
    ResourceConfig resourceConfig = new ResourceConfig(PSFormRestService.class);
    resourceConfig.property("contextConfig", PSConfigurableApplicationContext.class);
    return resourceConfig;
  }

  @Override
  protected DeploymentContext configureDeployment() {
    return ServletDeploymentContext.forPackages("com.percussion.delivery.forms.impl")
        .servletClass(HttpServlet.class)
        .contextPath("perc-form-processor")
        .addListener(ContextLoaderListener.class)
        .addListener(RequestContextListener.class)
        .addFilter(
            org.springframework.web.filter.DelegatingFilterProxy.class, "tenantAuthorizationFilter")
        .build();
  }

  @Test
  @Disabled
  public void testGetRestVersion() {
    Client client = ClientBuilder.newClient();
    WebTarget webTarget = client.target("/forms/version");
    Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
    Response response = invocationBuilder.get();
    assertNotNull(response);
    Assertions.assertEquals(200, response.getStatus());
    Assertions.assertEquals(testGetVersion(), response.getEntity());
  }

  private String testGetVersion() {
    String version = PSVersionHelper.getVersion(this.getClass());
    assertNotNull(version);
    System.out.print(version);
    return version;
  }

  @Test
  @Disabled("Integration tests are failing")
  public void testCSRF() {
    Client client = ClientBuilder.newClient();
    WebTarget webTarget = client.target("/forms/csrf");
    Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
    Response response = invocationBuilder.get();
    System.out.println(response.getEntity());
  }
}

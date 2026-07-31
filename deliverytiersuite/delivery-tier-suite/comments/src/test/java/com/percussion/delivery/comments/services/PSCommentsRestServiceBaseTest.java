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
package com.percussion.delivery.comments.services;

import static org.junit.jupiter.api.Assertions.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.multitenant.PSTenantSecurityFilter;
import com.percussion.delivery.test.utils.FakeRegistrant;
import com.percussion.delivery.test.utils.PSFakeDataGenerator;
import com.percussion.delivery.test.utils.spring.PSConfigurableApplicationContext;
import com.percussion.delivery.utils.PSVersionHelper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/***
 *
 * @author natechadwick
 *
 */
public abstract class PSCommentsRestServiceBaseTest extends JerseyTest {

  private static final Logger log = LogManager.getLogger(PSCommentsRestServiceBaseTest.class);

  private static String PERCUSSION_LIC = "2012-07-04-12344";
  private static int NUM_TENANTS = 10;
  private List<FakeRegistrant> tenants;
  private String _appContext;

  /***
   * Takes the context file as an arg and spins up grizzly to
   * test rest methods.
   *
   * @param appContext
   */
  @Override
  protected Application configure() {
    ResourceConfig resourceConfig = new ResourceConfig(PSCommentsService.class);
    resourceConfig.property("contextConfig", PSConfigurableApplicationContext.class);
    return resourceConfig;
  }

  @Override
  protected DeploymentContext configureDeployment() {
    return DeploymentContext.builder(new ResourceConfig(PSCommentsService.class))
        .contextPath("perc-comments-services")
        .build();
  }

  public PSCommentsRestServiceBaseTest() {}

  @org.junit.jupiter.api.BeforeEach
  public void setup() throws Exception {
    super.setUp();
    this.tenants = PSFakeDataGenerator.getFakeRegistrations(NUM_TENANTS);
  }

  @org.junit.jupiter.api.AfterEach
  public void teardown() {
    // add tear down code here.
  }

  private static String MOD_STATE_PUT_URL = "/comment/moderation/defaultModerationState";
  private static String MOD_STATE_GET_URL = "/comment/defaultModerationState/";

  @org.junit.jupiter.api.Disabled
  @Test
  public void testModerationState() throws Exception {

    Client client = ClientBuilder.newClient();

    WebTarget webTarget = client.target(MOD_STATE_PUT_URL);
    Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

    ObjectMapper mapper = JsonMapper.builder().build();
    ObjectNode setState = mapper.createObjectNode();
    setState.put("site", "testsite");
    setState.put("state", "REJECTED");

    Response response = invocationBuilder.put(Entity.json(mapper.writeValueAsString(setState)));
    assertNotNull(response);
    assertEquals(200, response.getStatus());

    Client client2 = ClientBuilder.newClient();

    WebTarget webTarget2 = client.target(MOD_STATE_GET_URL + "testsite");
    Invocation.Builder invocationBuilder2 = webTarget2.request(MediaType.APPLICATION_JSON);
    Response response2 = invocationBuilder2.get();

    assertNotNull(response2);
    assertEquals(200, response2.getStatus());

    String ret = response.readEntity(String.class);

    Assertions.assertEquals(ret, "REJECTED");
  }

  @Test
  @org.junit.jupiter.api.Disabled
  public void testComment() throws Exception {

    // Load up 1 comment per tenant.
    int i = 1;
    ObjectMapper mapper = JsonMapper.builder().build();
    for (FakeRegistrant p : this.tenants) {

      Client client = ClientBuilder.newClient();
      WebTarget webTarget = client.target("/comment");
      Invocation.Builder invocationBuilder =
          webTarget
              .request(MediaType.APPLICATION_FORM_URLENCODED)
              .header(PSTenantSecurityFilter.TENANTID_PARAM_NAME, p.getGUID());

      webTarget.queryParam(IPSCommentRestService.FORM_PARAM_EMAIL, p.getEmailAddress());
      webTarget.queryParam(IPSCommentRestService.FORM_PARAM_PAGEPATH, "/index");
      webTarget.queryParam(IPSCommentRestService.FORM_PARAM_SITE, "www." + p.getDomain());
      webTarget.queryParam(IPSCommentRestService.FORM_PARAM_TAGS, "sametag");
      webTarget.queryParam(
          IPSCommentRestService.FORM_PARAM_TEXT,
          "Tenant " + p.getGUID() + " was born " + p.getBirthday());
      webTarget.queryParam(IPSCommentRestService.FORM_PARAM_TITLE, "Tenant " + i + "tests");
      webTarget.queryParam(
          IPSCommentRestService.FORM_PARAM_URL, "http://www." + p.getDomain() + "/index");
      webTarget.queryParam(IPSCommentRestService.FORM_PARAM_USERNAME, p.getUsername());
      log.info("Tenant ID: {}", p.getGUID());

      Response response = invocationBuilder.get();
      assertNotNull(response);

      log.info(response.getEntity());
      // Assertions.assertEquals(200, response.getStatus());

      PSCommentCriteria crit = new PSCommentCriteria();

      ObjectNode postJson = mapper.createObjectNode();
      postJson.put("site", "www." + p.getDomain());
      postJson.put("pagepath", "/index");

      //	      //Now we want to Approve the comments.
      //	      response =
      // webResource.path("/comment/moderation/asmoderator").entity(mapper.writeValueAsString(postJson)).type("application/json").accept("application/json").header(PSTenantSecurityFilter.TENANTID_PARAM_NAME, p.getGUID()).post(ClientResponse.class);
      //
      //	      Assert.assertNotNull(response);
      /// Assertions.assertEquals(200,response.getStatus());
      // TODO: Fix me - this is not working.@see PSCommentService in site manage and figure out how
      // they are doing it.

      i++;
    }

    // Now that each tenant has some comments in the db
    // we want to try and access any of the other guys content
    // when logged in as a different tenant.
    // TODO: Finish me

  }

  public void testAddCommentWithGoodLicense() {

    FakeRegistrant p = tenants.get(0);

    Client client = ClientBuilder.newClient();
    WebTarget webTarget = client.target("/comment");
    Invocation.Builder invocationBuilder =
        webTarget
            .request(MediaType.APPLICATION_FORM_URLENCODED)
            .header(PSTenantSecurityFilter.TENANTID_PARAM_NAME, PERCUSSION_LIC);

    webTarget.queryParam(IPSCommentRestService.FORM_PARAM_EMAIL, p.getEmailAddress());
    webTarget.queryParam(IPSCommentRestService.FORM_PARAM_PAGEPATH, "/index");
    webTarget.queryParam(IPSCommentRestService.FORM_PARAM_SITE, "www." + p.getDomain());
    webTarget.queryParam(IPSCommentRestService.FORM_PARAM_TAGS, "sametag");
    webTarget.queryParam(
        IPSCommentRestService.FORM_PARAM_TEXT,
        "Tenant " + p.getGUID() + " was born " + p.getBirthday());
    webTarget.queryParam(IPSCommentRestService.FORM_PARAM_TITLE, "Good license tests");
    webTarget.queryParam(
        IPSCommentRestService.FORM_PARAM_URL, "http://www." + p.getDomain() + "/index");
    webTarget.queryParam(IPSCommentRestService.FORM_PARAM_USERNAME, p.getUsername());
    log.info("Tenant ID: {}", PERCUSSION_LIC);
    Response response = invocationBuilder.get();
    // response =
    // webResource.path("/comment").entity(queryParams).type("application/x-www-form-urlencoded").header(PSTenantSecurityFilter.TENANTID_PARAM_NAME, PERCUSSION_LIC).post(ClientResponse.class);

    assertNotNull(response);
  }

  @Test
  @org.junit.jupiter.api.Disabled
  public void testGetRestVersion() {

    Client client = ClientBuilder.newClient();
    WebTarget webTarget = client.target("/comment/version");
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

  @Override
  protected TestContainerFactory getTestContainerFactory() {
    return new GrizzlyWebTestContainerFactory();
  }
}

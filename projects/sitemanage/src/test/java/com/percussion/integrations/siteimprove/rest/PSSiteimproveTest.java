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
package com.percussion.integrations.siteimprove.rest;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.integrations.siteimprove.data.PSSiteImproveCredentials;
import com.percussion.integrations.siteimprove.data.PSSiteImproveSiteConfigurations;
import com.percussion.share.test.PSRestClient.RestClientException;
import com.percussion.share.test.PSRestTestCase;
import java.util.UUID;
import org.junit.jupiter.api.*;

/**
 * Integration tests for Siteimprove REST client. Sunny Sal says: "Testing REST endpoints, one
 * assertion at a time!"
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSSiteimproveTest extends PSRestTestCase<PSSiteimproveRestClient> {

  private static final String TESTING_USER = "percussionbot@gmail.com";
  private static final String TESTING_TOKEN = "388c3dc6a18b9582baada754b1408b7e";
  private static final String TESTING_SITENAME = "unitTest";

  @Override
  protected PSSiteimproveRestClient getRestClient(String baseUrl) {
    return new PSSiteimproveRestClient(baseUrl);
  }

  @Test
  @Order(1)
  void a_storeCredentialsTest() throws Exception {
    var credentials = new PSSiteImproveCredentials(TESTING_SITENAME, TESTING_TOKEN);
    restClient.storeCredentials(credentials);
  }

  @Test
  @Order(2)
  void b_storebadCredentialsTest() {
    try {
      var credentials =
          new PSSiteImproveCredentials(UUID.randomUUID().toString(), UUID.randomUUID().toString());
      restClient.storeCredentials(credentials);
      fail("Expected RestClientException");
    } catch (Exception exception) {
      assertTrue(exception instanceof RestClientException);
      var restClientException = (RestClientException) exception;
      assertEquals(401, restClientException.getStatus());
    }
  }

  @Test
  @Order(3)
  void c_storeSiteConfigurationTest() throws Exception {
    var configurations =
        new PSSiteImproveSiteConfigurations(TESTING_SITENAME, true, false, false, true, null);
    restClient.storeSiteConfig(configurations);
  }

  @Test
  @Order(4)
  void d_storeBadSiteConfigurationTest() {
    try {
      var configurations = new PSSiteImproveSiteConfigurations(null, null, null, null, null, null);
      restClient.storeSiteConfig(configurations);
      fail("Expected RestClientException");
    } catch (Exception exception) {
      assertTrue(exception instanceof RestClientException);
      var restClientException = (RestClientException) exception;
      assertEquals(500, restClientException.getStatus());
    }
  }

  @Test
  @Order(5)
  void retrieveSiteCredentialsTest() throws Exception {
    var results = restClient.retrieveCredentials(TESTING_SITENAME);
    assertNotNull(results);
    assertTrue(results.contains("perc.siteimprove.credentials." + TESTING_SITENAME));
    assertTrue(results.contains(TESTING_USER));
    assertTrue(results.contains(TESTING_TOKEN));
  }

  @Test
  @Order(6)
  void retrieveAllCredentialsTest() throws Exception {
    var results = restClient.retrieveAllCredentials();
    assertNotNull(results);
    assertTrue(results.length() > 0);
    assertTrue(results.contains(TESTING_SITENAME));
    assertTrue(results.contains(TESTING_TOKEN));
    assertTrue(results.contains(TESTING_USER));
  }

  @Test
  @Order(7)
  void retrieveBadCredentialsTest() {
    String results = null;
    try {
      results = restClient.retrieveCredentials(UUID.randomUUID().toString());
      fail("Expected RestClientException");
    } catch (Exception exception) {
      assertTrue(exception instanceof RestClientException);
    }
    assertNull(results);
  }

  @Test
  @Order(8)
  void retrieveSiteConfigurationTest() throws Exception {
    var results = restClient.retrieveSiteConfig(TESTING_SITENAME);
    assertNotNull(results);
    assertTrue(results.contains("doStaging"));
    assertTrue(results.contains("false"));
  }

  @Test
  @Order(9)
  void retrieveAllSiteConfigurationsTest() throws Exception {
    var results = restClient.retrieveAllSiteConfig();
    assertNotNull(results);
    assertTrue(results.length() > 0);
    assertTrue(results.contains(TESTING_SITENAME));
    assertTrue(results.contains("doPreview"));
    assertTrue(results.contains("doProduction"));
    assertTrue(results.contains("true"));
  }

  @Test
  @Order(10)
  void retrievebadSiteConfigTest() {
    String results = null;
    try {
      results = restClient.retrieveSiteConfig(UUID.randomUUID().toString());
      fail("Expected RestClientException");
    } catch (Exception exception) {
      assertTrue(exception instanceof RestClientException);
    }
    assertNull(results);
  }
}

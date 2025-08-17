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
package com.percussion.delivery.client;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.delivery.client.IPSDeliveryClient.HttpMethodType;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.impl.PSDeliveryInfoLoaderTest;
import com.percussion.proxyconfig.data.PSProxyConfig;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author natechadwick
 * @author federicoromanelli
 */
@Tag("IntegrationTest")
public class PSDeliveryClientTests {

  private static final String NETSUITE_METHOD_URL =
      "/" + PSDeliveryInfo.SERVICE_THIRDPARTY + "/netsuite/method";

  private static final String PROXY_HOST_NO_AUTH = "10.10.10.70";
  private static final String PROXY_HOST_AUTH = "10.10.10.133";
  private static final String PROXY_HOST_INCORRECT = "10.10.10.155";
  PSDeliveryInfo info;
  PSDeliveryInfo info2;

  @BeforeEach
  public void setup() {
    var loadUtil = new PSDeliveryInfoLoaderTest();
    var loader = loadUtil.getDeliveryInfoLoader("PercussionDeliveryServerConfigTest.xml");
    info = loader.getDeliveryServers().get(0);
    info2 = loader.getDeliveryServers().get(1);
    assertNotNull(info);
    assertNotNull(info2);
  }

  @Disabled
  @Test
  public void testSSLwithTLS() {
    var c = new PSDeliveryClient();
    var opt = new PSDeliveryActionOptions(info, "/perc-metadata-services/application.wadl", true);
    c.push(opt, null);
  }

  @Test
  public void testNoProxyConfig() {
    var c = new PSDeliveryClient();
    var proxyConfig = new PSProxyConfig();

    c.setProxyConfig(proxyConfig);
    c.setLicenseOverride("-1");
    var result =
        c.getJsonArray(
            new PSDeliveryActionOptions(info, NETSUITE_METHOD_URL, HttpMethodType.GET, true));
    assertNotNull(result);
  }

  @Test
  public void testNoProxyConfigBeanAvailable() {
    var c = new PSDeliveryClient();
    c.setLicenseOverride("-1");
    var result =
        c.getJsonArray(
            new PSDeliveryActionOptions(info, NETSUITE_METHOD_URL, HttpMethodType.GET, true));
    assertNotNull(result);
  }

  @Test
  public void testProxyConfig() {
    var c = new PSDeliveryClient();
    var proxyConfig = new PSProxyConfig();
    proxyConfig.setHost(PROXY_HOST_NO_AUTH);
    proxyConfig.setPort("3128");
    proxyConfig.setProtocols(new ArrayList<>(asList("http", "https")));

    c.setProxyConfig(proxyConfig);
    c.setLicenseOverride("-1");
    var result =
        c.getJsonArray(
            new PSDeliveryActionOptions(info2, NETSUITE_METHOD_URL, HttpMethodType.GET, true));
    assertNotNull(result);
  }

  @Test
  public void testProxyConfigInvalidServer() {
    var c = new PSDeliveryClient();
    var proxyConfig = new PSProxyConfig();
    proxyConfig.setHost(PROXY_HOST_INCORRECT);
    proxyConfig.setPort("3128");
    proxyConfig.setProtocols(new ArrayList<>(asList("http", "https")));

    c.setProxyConfig(proxyConfig);
    c.setLicenseOverride("-1");
    try {
      var result =
          c.getJsonArray(
              new PSDeliveryActionOptions(info, NETSUITE_METHOD_URL, HttpMethodType.GET, true));
      fail("Shouldn't get to this point");
    } catch (Exception e) {
      assertTrue(StringUtils.contains(e.getMessage(), "Unable to connect to delivery server"));
    }
  }

  @Test
  public void testProxyConfigUserAndPassword() {
    var c = new PSDeliveryClient();
    var proxyConfig = new PSProxyConfig();
    proxyConfig.setHost(PROXY_HOST_AUTH);
    proxyConfig.setPort("3128");
    proxyConfig.setUser("admin");
    proxyConfig.setPassword("demo");
    proxyConfig.setProtocols(new ArrayList<>(asList("http", "https")));

    c.setProxyConfig(proxyConfig);
    c.setLicenseOverride("-1");
    var result =
        c.getJsonArray(
            new PSDeliveryActionOptions(info, NETSUITE_METHOD_URL, HttpMethodType.GET, true));
    assertNotNull(result);
  }

  @Test
  public void testProxyConfigUserAndPasswordInvalidServer() {
    var c = new PSDeliveryClient();
    var proxyConfig = new PSProxyConfig();
    proxyConfig.setHost(PROXY_HOST_AUTH);
    proxyConfig.setPort("3128");
    proxyConfig.setUser("admin");
    proxyConfig.setPassword("demo1");
    proxyConfig.setProtocols(new ArrayList<>(asList("http", "https")));

    c.setProxyConfig(proxyConfig);
    c.setLicenseOverride("-1");
    try {
      var result =
          c.getJsonArray(
              new PSDeliveryActionOptions(info, NETSUITE_METHOD_URL, HttpMethodType.GET, true));
      fail("Shouldn't get to this point");
    } catch (Exception e) {
      assertTrue(StringUtils.contains(e.getMessage(), "Unable to connect to delivery server"));
    }
  }
}

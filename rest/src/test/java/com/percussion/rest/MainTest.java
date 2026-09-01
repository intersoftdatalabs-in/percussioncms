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

package com.percussion.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.GuidListJsonReader;
import com.percussion.rest.acls.AclListJsonReader;
import com.percussion.rest.communities.CommunityListJsonReader;
import com.percussion.rest.contentexplorer.folders.AddFolderRequestJsonReader;
import com.percussion.rest.searches.SearchExecuteRequestJsonReader;
import com.percussion.rest.errors.RestExceptionMapper;
import com.percussion.rest.errors.WebApplicationExceptionMapper;
import com.percussion.utils.testing.PSTestNetUtils;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.spring.SpringResourceFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class MainTest {

  public static final String ENDPOINT_HOST = "http://127.0.0.1";
  public static final String ENDPOINT_PATH = "/rest";

  @Autowired private ApplicationContext applicationContext;

  public WebTarget target(String address) {

    ClientBuilder builder = ClientBuilder.newBuilder();

    String endpoint = MainTest.ENDPOINT_HOST + ":" + ContextConfiguration.port + ENDPOINT_PATH;
    WebTarget target =
        builder
            .build()
            .register(tools.jackson.jakarta.rs.json.JacksonJsonProvider.class)
            .target(endpoint)
            .path(address);

    return target;
  }

  @BeforeAll
  public static void initialize() throws Exception {}

  @Test
  public void testContextLoaded() {
    assertNotNull(applicationContext);
    assertTrue(true);
  }

  /**
   * Regression: {@link MainTestJacksonBeansTest.StaticJacksonSpringConfig} must not be
   * component-scanned into this context (duplicate JacksonJsonProvider breaks @Autowired by type).
   */
  @Test
  public void testUniqueJacksonBeans() {
    assertEquals(
        1,
        applicationContext.getBeanNamesForType(JacksonJsonProvider.class).length,
        "exactly one JacksonJsonProvider bean expected");
    assertEquals(
        1,
        applicationContext.getBeanNamesForType(JacksonContextResolver.class).length,
        "exactly one JacksonContextResolver bean expected");
  }

  @Configuration
  @ImportResource({"classpath:META-INF/cxf/cxf.xml"})
  // Exclude MainTestJacksonBeansTest nested @Configuration: component-scan would otherwise
  // register a second JacksonJsonProvider / JacksonContextResolver and fail @Autowired by type.
  @ComponentScan(
      basePackages = {"com.percussion.rest"},
      excludeFilters =
          @ComponentScan.Filter(
              type = FilterType.REGEX,
              pattern = ".*\\.MainTestJacksonBeansTest(\\$.*)?"))
  public static class ContextConfiguration {

    public static int port;

    @Autowired private ApplicationContext ctx;

    @Autowired private JacksonJsonProvider jacksonProvider;

    @Autowired private JacksonContextResolver contextResolver;

    /**
     * Static {@code @Bean} methods avoid early-init issues in Spring Framework 6+ configuration
     * processing (v8.1.7 PR #574 Jackson compatibility residue).
     */
    @Bean
    public static JacksonJsonProvider jacksonJsonProvider() {
      return new JacksonJsonProvider();
    }

    @Bean
    public static JacksonContextResolver jacksonContextResolver() {
      return new JacksonContextResolver();
    }

    public Server getServer() {

      LinkedList<ResourceProvider> resourceProviders = new LinkedList<>();
      for (String beanName : ctx.getBeanDefinitionNames()) {
        if (ctx.findAnnotationOnBean(beanName, Path.class) != null) {
          SpringResourceFactory factory = new SpringResourceFactory(beanName);
          factory.setApplicationContext(ctx);
          resourceProviders.add(factory);
        }
      }
      Map<Object, Object> extensionMap = new HashMap<>();
      extensionMap.put("json", "application/json");
      extensionMap.put("xml", "application/xml");

      final JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
      port = PSTestNetUtils.findFreePort();
      RestExceptionMapper exceptionMapper = new RestExceptionMapper();
      WebApplicationExceptionMapper waeMapper = new WebApplicationExceptionMapper();
      String endpoint = MainTest.ENDPOINT_HOST + ":" + port + ENDPOINT_PATH;
      factory.setExtensionMappings(extensionMap);
      factory.setBus(ctx.getBean(SpringBus.class));
      factory.setProviders(
          Arrays.asList(
              exceptionMapper,
              waeMapper,
              new AclListJsonReader(),
              new CommunityListJsonReader(),
              new GuidListJsonReader(),
              new AddFolderRequestJsonReader(),
              new SearchExecuteRequestJsonReader(),
              jacksonProvider,
              contextResolver));
      factory.setResourceProviders(resourceProviders);
      factory.setAddress(endpoint);
      try {
        return factory.create();
      } catch (Exception ex) {
        return null;
      }
    }
  }
}

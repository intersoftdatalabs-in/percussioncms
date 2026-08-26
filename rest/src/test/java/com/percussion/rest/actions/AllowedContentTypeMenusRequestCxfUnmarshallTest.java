/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.rest.actions;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.rest.JacksonContextResolver;
import com.percussion.rest.errors.WebApplicationExceptionMapper;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

/**
 * CXF pipeline for POST {@code /actions/find/types} (#3855).
 *
 * <p>Production JAXB / UNWRAP_ROOT_VALUE rejects a bare {@code contentIds} root. When {@link
 * AllowedContentTypeMenusRequestJsonReader} is registered, wrapped, flat, and GUID-token bodies
 * bind as HTTP 200.
 */
@Tag("UnitTest")
public class AllowedContentTypeMenusRequestCxfUnmarshallTest {

  private static final String WRAPPED =
      "{\"AllowedContentTypeMenusRequest\":{\"contentIds\":[551]}}";
  private static final String FLAT = "{\"contentIds\":[551]}";
  private static final String GUID = "{\"contentIds\":[\"16777215-101-551\"]}";

  private Server server;

  @AfterEach
  public void stopServer() {
    if (server != null) {
      server.stop();
      server.destroy();
      server = null;
    }
  }

  @Test
  public void cxfFactorySelectsAllowedContentTypeMenusRequestJsonReader() throws Exception {
    ServerProviderFactory factory = ServerProviderFactory.createInstance(null);
    AllowedContentTypeMenusRequestJsonReader custom =
        new AllowedContentTypeMenusRequestJsonReader();
    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(
        (tools.jackson.databind.json.JsonMapper)
            new JacksonContextResolver().getContext(AllowedContentTypeMenusRequest.class));
    factory.setUserProviders(Arrays.asList(custom, jackson, new JacksonContextResolver()));

    MessageImpl message = new MessageImpl();
    MessageBodyReader<AllowedContentTypeMenusRequest> selected =
        factory.createMessageBodyReader(
            AllowedContentTypeMenusRequest.class,
            AllowedContentTypeMenusRequest.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            message);
    assertNotNull(
        selected, "CXF must find a MessageBodyReader for AllowedContentTypeMenusRequest");
    assertTrue(
        selected.isReadable(
            AllowedContentTypeMenusRequest.class,
            AllowedContentTypeMenusRequest.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE),
        selected.getClass().getName());

    AllowedContentTypeMenusRequest req =
        selected.readFrom(
            AllowedContentTypeMenusRequest.class,
            AllowedContentTypeMenusRequest.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(FLAT.getBytes(StandardCharsets.UTF_8)));
    assertArrayEquals(new int[] {551}, req.getContentIds());
  }

  @Test
  public void cxfPostWrappedEnvelopeIsHttp200() throws Exception {
    assertPostBinds(WRAPPED, 551, "local://issue-3855-find-types-wrap");
  }

  @Test
  public void cxfPostFlatSpaBodyIsHttp200() throws Exception {
    assertPostBinds(FLAT, 551, "local://issue-3855-find-types-flat");
  }

  @Test
  public void cxfPostGuidTokenIsHttp200() throws Exception {
    assertPostBinds(GUID, 551, "local://issue-3855-find-types-guid");
  }

  private void assertPostBinds(String json, int expectedId, String address) throws Exception {
    AtomicReference<Integer[]> captured = new AtomicReference<>();
    IActionMenuAdaptor adaptor = mock(IActionMenuAdaptor.class);
    when(adaptor.findAllowedContentTypes(any()))
        .thenAnswer(
            inv -> {
              captured.set(inv.getArgument(0));
              ActionMenu menu = new ActionMenu();
              menu.setName("rffHome");
              return List.of(menu);
            });

    ActionMenuResource resource = new ActionMenuResource();
    var field = ActionMenuResource.class.getDeclaredField("adaptor");
    field.setAccessible(true);
    field.set(resource, adaptor);

    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(
        (tools.jackson.databind.json.JsonMapper)
            new JacksonContextResolver().getContext(AllowedContentTypeMenusRequest.class));

    JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
    sf.setAddress(address);
    sf.setServiceBean(resource);
    sf.setProviders(
        List.of(
            new AllowedContentTypeMenusRequestJsonReader(),
            jackson,
            new JacksonContextResolver(),
            new WebApplicationExceptionMapper()));
    server = sf.create();
    assertNotNull(server, "CXF local server must start");

    WebClient client =
        WebClient.create(address)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON)
            .path("/actions/find/types");
    Response response = client.post(json);
    assertEquals(
        200,
        response.getStatus(),
        "POST must be HTTP 200; body=" + response.readEntity(String.class));
    Integer[] saved = captured.get();
    assertNotNull(saved, "adaptor.findAllowedContentTypes must be invoked");
    assertArrayEquals(new Integer[] {expectedId}, saved);
  }
}

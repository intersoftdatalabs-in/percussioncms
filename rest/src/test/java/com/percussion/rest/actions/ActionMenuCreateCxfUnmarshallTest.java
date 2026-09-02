/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

/**
 * CXF pipeline for Admin {@code POST /actions} (#4123).
 *
 * <p>JAXB / a stale collection POST used {@code AllowedWorkflowTransitionsRequest} and rejected
 * {@code ActionMenu}. With {@link ActionMenuJsonReader}, wrapped and flat create bodies bind
 * and call {@code createActionMenu}.
 */
@Tag("UnitTest")
public class ActionMenuCreateCxfUnmarshallTest {

  private static final String WRAPPED =
      "{\"ActionMenu\":{\"name\":\"cxfN1\",\"label\":\"Create me\",\"menuType\":\"MENUITEM\"}}";
  private static final String FLAT = "{\"name\":\"cxfFlat\",\"label\":\"Flat\"}";

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
  public void cxfFactorySelectsActionMenuJsonReader() throws Exception {
    ServerProviderFactory factory = ServerProviderFactory.createInstance(null);
    ActionMenuJsonReader custom = new ActionMenuJsonReader();
    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(
        (tools.jackson.databind.json.JsonMapper)
            new JacksonContextResolver().getContext(ActionMenu.class));
    factory.setUserProviders(
        Arrays.asList(
            custom, jackson, new JacksonContextResolver(), new JAXBElementProvider<>()));

    MessageImpl message = new MessageImpl();
    MessageBodyReader<ActionMenu> selected =
        factory.createMessageBodyReader(
            ActionMenu.class,
            ActionMenu.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            message);
    assertNotNull(selected, "CXF must find a MessageBodyReader for ActionMenu");
    assertTrue(
        selected.isReadable(
            ActionMenu.class,
            ActionMenu.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE),
        selected.getClass().getName());

    ActionMenu menu =
        selected.readFrom(
            ActionMenu.class,
            ActionMenu.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(WRAPPED.getBytes(StandardCharsets.UTF_8)));
    assertEquals("cxfN1", menu.getName());
  }

  @Test
  public void cxfPostWrappedActionMenuInvokesCreate() throws Exception {
    assertCreatePost(WRAPPED, "cxfN1", "local://issue-4123-actions-wrap");
  }

  @Test
  public void cxfPostFlatActionMenuInvokesCreate() throws Exception {
    assertCreatePost(FLAT, "cxfFlat", "local://issue-4123-actions-flat");
  }

  @Test
  public void cxfPostWrappedActionMenuWithJaxbProviderStillInvokesCreate() throws Exception {
    assertCreatePost(WRAPPED, "cxfN1", "local://issue-4171-actions-jaxb");
  }

  @Test
  public void jacksonBarrierBindsWrappedActionMenuWithoutCustomReader() {
    ActionMenu menu =
        new JacksonContextResolver()
            .getContext(ActionMenu.class)
            .readValue(WRAPPED, ActionMenu.class);
    assertEquals("cxfN1", menu.getName());
  }

  private void assertCreatePost(String json, String expectedName, String address) throws Exception {
    AtomicReference<ActionMenu> captured = new AtomicReference<>();
    IActionMenuAdaptor adaptor = mock(IActionMenuAdaptor.class);
    when(adaptor.createActionMenu(any()))
        .thenAnswer(
            inv -> {
              ActionMenu in = inv.getArgument(0);
              captured.set(in);
              ActionMenu created = new ActionMenu();
              created.setName(in.getName());
              created.setLabel(in.getLabel());
              created.setId(42);
              return created;
            });

    ActionMenuResource resource = new ActionMenuResource();
    var field = ActionMenuResource.class.getDeclaredField("adaptor");
    field.setAccessible(true);
    field.set(resource, adaptor);

    server = startServer(resource, address);

    WebClient client =
        WebClient.create(address)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON)
            .path("/actions");
    Response response = client.post(json);
    assertEquals(
        200, response.getStatus(), () -> "POST /actions must be HTTP 200 (status=" + response.getStatus() + ")");
    ActionMenu saved = captured.get();
    assertNotNull(saved, "adaptor.createActionMenu must be invoked");
    assertEquals(expectedName, saved.getName());
    verify(adaptor).createActionMenu(any());
  }

  private static Server startServer(ActionMenuResource resource, String address) {
    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(
        (tools.jackson.databind.json.JsonMapper)
            new JacksonContextResolver().getContext(ActionMenu.class));
    JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
    sf.setAddress(address);
    sf.setServiceBean(resource);
    sf.setProviders(
        List.of(
            new ActionMenuJsonReader(),
            new AllowedContentTypeMenusRequestJsonReader(),
            jackson,
            new JacksonContextResolver(),
            new JAXBElementProvider<>(),
            new WebApplicationExceptionMapper()));
    Server created = sf.create();
    assertNotNull(created, "CXF local server must start");
    return created;
  }
}

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

package com.percussion.rest.contentexplorer.folders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.percussion.rest.JacksonContextResolver;
import com.percussion.rest.errors.WebApplicationExceptionMapper;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
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
 * CXF pipeline for POST {@code /content-explorer/folders} (#3360).
 *
 * <p>Production JAXB / Jettison rejects a bare {@code name} root. When {@link
 * AddFolderRequestJsonReader} is registered (rest-jax-rs providers), both the preferred wrap and
 * the live SPA flat body bind.
 */
@Tag("UnitTest")
public class AddFolderRequestCxfUnmarshallTest {

  private static final String WRAPPED =
      "{\"AddFolderRequest\":{\"name\":\"qa3360\",\"parentPath\":\"/Folders\"}}";
  private static final String FLAT = "{\"name\":\"qa3360_flat\",\"parentPath\":\"/Folders\"}";

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
  public void cxfFactorySelectsAddFolderRequestJsonReader() throws Exception {
    ServerProviderFactory factory = ServerProviderFactory.createInstance(null);
    AddFolderRequestJsonReader custom = new AddFolderRequestJsonReader();
    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(
        (tools.jackson.databind.json.JsonMapper)
            new JacksonContextResolver().getContext(AddFolderRequest.class));
    factory.setUserProviders(Arrays.asList(custom, jackson, new JacksonContextResolver()));

    MessageImpl message = new MessageImpl();
    MessageBodyReader<AddFolderRequest> selected =
        factory.createMessageBodyReader(
            AddFolderRequest.class,
            AddFolderRequest.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            message);
    assertNotNull(selected, "CXF must find a MessageBodyReader for AddFolderRequest");
    assertTrue(
        selected.isReadable(
            AddFolderRequest.class,
            AddFolderRequest.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE),
        selected.getClass().getName());

    AddFolderRequest req =
        selected.readFrom(
            AddFolderRequest.class,
            AddFolderRequest.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(FLAT.getBytes(StandardCharsets.UTF_8)));
    assertEquals("qa3360_flat", req.getName());
    assertEquals("/Folders", req.getParentPath());
  }

  @Test
  public void cxfPostWrappedEnvelopeIsHttp200() throws Exception {
    assertPostBinds(WRAPPED, "qa3360", "local://issue-3360-add-folder-wrap");
  }

  @Test
  public void cxfPostFlatSpaBodyIsHttp200() throws Exception {
    assertPostBinds(FLAT, "qa3360_flat", "local://issue-3360-add-folder-flat");
  }

  private void assertPostBinds(String json, String expectedName, String address) throws Exception {
    AtomicReference<AddFolderRequest> captured = new AtomicReference<>();
    IContentExplorerFolderAdaptor adaptor = mock(IContentExplorerFolderAdaptor.class);
    doAnswer(
            inv -> {
              AddFolderRequest arg = inv.getArgument(1);
              captured.set(arg);
              RxFolder created = new RxFolder();
              created.setName(arg.getName());
              created.setPath(arg.getParentPath() + "/" + arg.getName());
              created.setId("1-101-9");
              return created;
            })
        .when(adaptor)
        .addFolder(any(), any());

    ContentExplorerFoldersResource resource = new ContentExplorerFoldersResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    org.mockito.Mockito.lenient()
        .when(uriInfo.getBaseUri())
        .thenReturn(URI.create("http://localhost/rest"));
    resource.setUriInfo(uriInfo);

    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(
        (tools.jackson.databind.json.JsonMapper)
            new JacksonContextResolver().getContext(AddFolderRequest.class));

    JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
    sf.setAddress(address);
    sf.setServiceBean(resource);
    sf.setProviders(
        List.of(
            new AddFolderRequestJsonReader(),
            jackson,
            new JacksonContextResolver(),
            new WebApplicationExceptionMapper()));
    server = sf.create();
    assertNotNull(server, "CXF local server must start");

    WebClient client =
        WebClient.create(address)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON)
            .path("/content-explorer/folders");
    Response response = client.post(json);
    assertEquals(
        200,
        response.getStatus(),
        "POST must be HTTP 200; body=" + response.readEntity(String.class));
    AddFolderRequest saved = captured.get();
    assertNotNull(saved, "adaptor.addFolder must be invoked");
    assertEquals(expectedName, saved.getName());
    assertEquals("/Folders", saved.getParentPath());
  }
}

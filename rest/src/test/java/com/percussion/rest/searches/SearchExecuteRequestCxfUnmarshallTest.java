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

package com.percussion.rest.searches;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

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
 * CXF pipeline for POST {@code /searches/{id}/execute} (#3517).
 *
 * <p>Production JAXB / UNWRAP_ROOT_VALUE rejects a bare {@code startIndex} / {@code folderPath}
 * root. When {@link SearchExecuteRequestJsonReader} is registered (rest-jax-rs providers), both
 * the preferred wrap and a flat paging body bind.
 */
@Tag("UnitTest")
public class SearchExecuteRequestCxfUnmarshallTest {

  private static final String WRAPPED =
      "{\"SearchExecuteRequest\":{\"folderPath\":\"//Sites\",\"startIndex\":1,\"maxResults\":25}}";
  private static final String FLAT = "{\"startIndex\":2,\"maxResults\":10}";

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
  public void cxfFactorySelectsSearchExecuteRequestJsonReader() throws Exception {
    ServerProviderFactory factory = ServerProviderFactory.createInstance(null);
    SearchExecuteRequestJsonReader custom = new SearchExecuteRequestJsonReader();
    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(
        (tools.jackson.databind.json.JsonMapper)
            new JacksonContextResolver().getContext(SearchExecuteRequest.class));
    factory.setUserProviders(Arrays.asList(custom, jackson, new JacksonContextResolver()));

    MessageImpl message = new MessageImpl();
    MessageBodyReader<SearchExecuteRequest> selected =
        factory.createMessageBodyReader(
            SearchExecuteRequest.class,
            SearchExecuteRequest.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            message);
    assertNotNull(selected, "CXF must find a MessageBodyReader for SearchExecuteRequest");
    assertTrue(
        selected.isReadable(
            SearchExecuteRequest.class,
            SearchExecuteRequest.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE),
        selected.getClass().getName());

    SearchExecuteRequest req =
        selected.readFrom(
            SearchExecuteRequest.class,
            SearchExecuteRequest.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(FLAT.getBytes(StandardCharsets.UTF_8)));
    assertEquals(2, req.getStartIndex());
    assertEquals(10, req.getMaxResults());
    assertNull(req.getFolderPath());
  }

  @Test
  public void cxfPostWrappedEnvelopeIsHttp200() throws Exception {
    SearchExecuteRequest captured = assertPostBinds(WRAPPED, "local://issue-3517-search-exec-wrap");
    assertEquals("//Sites", captured.getFolderPath());
    assertEquals(1, captured.getStartIndex());
    assertEquals(25, captured.getMaxResults());
  }

  @Test
  public void cxfPostFlatStartIndexBodyIsHttp200() throws Exception {
    SearchExecuteRequest captured = assertPostBinds(FLAT, "local://issue-3517-search-exec-flat");
    assertNull(captured.getFolderPath());
    assertEquals(2, captured.getStartIndex());
    assertEquals(10, captured.getMaxResults());
  }

  private SearchExecuteRequest assertPostBinds(String json, String address) throws Exception {
    AtomicReference<SearchExecuteRequest> captured = new AtomicReference<>();
    ISearchAdaptor adaptor = mock(ISearchAdaptor.class);
    doAnswer(
            inv -> {
              SearchExecuteRequest arg = inv.getArgument(1);
              captured.set(arg);
              SearchExecuteResult result = new SearchExecuteResult();
              result.setSearchName("View_All");
              result.setChildren(List.of());
              result.setTotalCount(0);
              result.setStartIndex(arg != null && arg.getStartIndex() != null ? arg.getStartIndex() : 1);
              return result;
            })
        .when(adaptor)
        .executeSearch(eq("View_All"), any());

    SearchResource resource = new SearchResource(adaptor);

    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(
        (tools.jackson.databind.json.JsonMapper)
            new JacksonContextResolver().getContext(SearchExecuteRequest.class));

    JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
    sf.setAddress(address);
    sf.setServiceBean(resource);
    sf.setProviders(
        List.of(
            new SearchExecuteRequestJsonReader(),
            jackson,
            new JacksonContextResolver(),
            new WebApplicationExceptionMapper()));
    server = sf.create();
    assertNotNull(server, "CXF local server must start");

    WebClient client =
        WebClient.create(address)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON)
            .path("/searches/View_All/execute");
    Response response = client.post(json);
    assertEquals(
        200,
        response.getStatus(),
        "POST must be HTTP 200; body=" + response.readEntity(String.class));
    SearchExecuteRequest saved = captured.get();
    assertNotNull(saved, "adaptor.executeSearch must be invoked");
    return saved;
  }
}

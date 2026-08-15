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
package com.percussion.rest.acls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.ArrayList;
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
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

/**
 * CXF / JAX-RS pipeline for PUT {@code /acls/bulk} (#3391).
 *
 * <p>{@link AclListJsonReaderTest} only covers {@link AclListJsonReader#parse(String)}. The live
 * 400 was {@code ClassCastException: Cannot cast java.util.ArrayList to AclList} after CXF selected
 * {@link JacksonJsonProvider}. This class exercises the real save envelope through:
 *
 * <ul>
 *   <li>Jackson {@code JavaType} (what {@code JacksonJsonProvider} constructs)
 *   <li>{@link JacksonJsonProvider#readFrom}
 *   <li>CXF {@link ServerProviderFactory#createMessageBodyReader}
 *   <li>an in-process CXF server PUT
 * </ul>
 */
@Tag("UnitTest")
public class AclListCxfUnmarshallTest {

  static final String DF_SAVE =
      "{\"AclList\":[{"
          + "\"id\":7,"
          + "\"name\":\"By_Author ACL\","
          + "\"objectId\":5,"
          + "\"objectType\":31,"
          + "\"objectGuid\":{\"stringValue\":\"0-31-5\"},"
          + "\"aclEntries\":["
          + "{\"name\":\"Default\",\"principal\":{\"name\":\"Default\"},"
          + "\"type\":{\"name\":\"Default\",\"type\":\"USER\"},"
          + "\"permissions\":[{\"permission\":\"READ\"}]},"
          + "{\"name\":\"AnyCommunity\",\"principal\":{\"name\":\"AnyCommunity\"},"
          + "\"type\":{\"name\":\"AnyCommunity\",\"type\":\"COMMUNITY\"},"
          + "\"permissions\":[{\"permission\":\"RUNTIME_VISIBLE\"}]},"
          + "{\"name\":\"Admin\",\"principal\":{\"name\":\"Admin\"},"
          + "\"type\":{\"name\":\"Admin\",\"type\":\"USER\"},"
          + "\"permissions\":[{\"permission\":\"READ\"}]}"
          + "]}]}";

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
  public void productionJavaTypeUnmarshalsEnvelopeToAclListNotArrayList() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(AclList.class);
    JavaType javaType = mapper.getTypeFactory().constructType(AclList.class);
    Object raw = mapper.readValue(DF_SAVE, javaType);
    assertInstanceOf(AclList.class, raw, "JavaType path must not return raw ArrayList: " + raw);
    AclList list = (AclList) raw;
    assertEquals(1, list.size());
    assertEquals(
        3, list.get(0).getAclEntries() == null ? 0 : list.get(0).getAclEntries().size());
  }

  @Test
  public void jacksonJsonProviderReadFromReturnsAclList() throws Exception {
    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(
        (tools.jackson.databind.json.JsonMapper)
            new JacksonContextResolver().getContext(AclList.class));
    @SuppressWarnings("unchecked")
    Class<Object> declared = (Class<Object>) (Class<?>) AclList.class;
    Object raw =
        jackson.readFrom(
            declared,
            AclList.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(DF_SAVE.getBytes(StandardCharsets.UTF_8)));
    assertInstanceOf(
        AclList.class, raw, "JacksonJsonProvider.readFrom must return AclList, not " + typeOf(raw));
    assertEquals(1, ((AclList) raw).size());
  }

  @Test
  public void cxfFactorySelectsReadableProviderAndReturnsAclList() throws Exception {
    ServerProviderFactory factory = ServerProviderFactory.createInstance(null);
    AclListJsonReader custom = new AclListJsonReader();
    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(
        (tools.jackson.databind.json.JsonMapper)
            new JacksonContextResolver().getContext(AclList.class));
    factory.setUserProviders(Arrays.asList(custom, jackson, new JacksonContextResolver()));

    MessageImpl message = new MessageImpl();
    MessageBodyReader<AclList> selected =
        factory.createMessageBodyReader(
            AclList.class,
            AclList.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            message);
    assertNotNull(selected, "CXF must find a MessageBodyReader for AclList");
    assertTrue(
        selected.isReadable(
            AclList.class, AclList.class, new Annotation[0], MediaType.APPLICATION_JSON_TYPE),
        selected.getClass().getName());

    AclList list =
        selected.readFrom(
            AclList.class,
            AclList.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(DF_SAVE.getBytes(StandardCharsets.UTF_8)));
    assertInstanceOf(AclList.class, list);
    assertEquals(1, list.size());
    assertEquals("By_Author ACL", list.get(0).getName());
    assertEquals(
        3, list.get(0).getAclEntries() == null ? 0 : list.get(0).getAclEntries().size());
  }

  @Test
  public void cxfPutBulkSaveEnvelopeIsHttp200AndAclList() throws Exception {
    AtomicReference<AclList> captured = new AtomicReference<>();
    IAclAdaptor adaptor = mock(IAclAdaptor.class);
    doAnswer(
            inv -> {
              Object arg = inv.getArgument(0);
              assertInstanceOf(
                  AclList.class, arg, "saveAcls must receive AclList, not " + typeOf(arg));
              captured.set((AclList) arg);
              return null;
            })
        .when(adaptor)
        .saveAcls(any());

    AclResource resource = new AclResource();
    var field = AclResource.class.getDeclaredField("adaptor");
    field.setAccessible(true);
    field.set(resource, adaptor);

    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(
        (tools.jackson.databind.json.JsonMapper)
            new JacksonContextResolver().getContext(AclList.class));

    JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
    sf.setAddress("local://issue-3391-acl-bulk");
    sf.setServiceBean(resource);
    sf.setProviders(
        List.of(
            new AclListJsonReader(),
            jackson,
            new JacksonContextResolver(),
            new WebApplicationExceptionMapper()));
    server = sf.create();
    assertNotNull(server, "CXF local server must start");

    WebClient client =
        WebClient.create("local://issue-3391-acl-bulk")
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON)
            .path("/acls/bulk");
    Response response = client.put(DF_SAVE);
    assertEquals(
        200,
        response.getStatus(),
        "PUT {\"AclList\":[...]} must be HTTP 200; body=" + response.readEntity(String.class));
    AclList saved = captured.get();
    assertNotNull(saved, "adaptor.saveAcls must be invoked");
    assertEquals(1, saved.size());
    assertEquals(31, saved.get(0).getObjectType());
    assertEquals(5, saved.get(0).getObjectId());
    assertEquals(
        3, saved.get(0).getAclEntries() == null ? 0 : saved.get(0).getAclEntries().size());
    assertEquals("Default", saved.get(0).getAclEntries().get(0).getName());
    assertEquals("AnyCommunity", saved.get(0).getAclEntries().get(1).getName());
    assertEquals("Admin", saved.get(0).getAclEntries().get(2).getName());
  }

  @Test
  public void cxfPutBareArrayIsHttp200AndAclList() throws Exception {
    AtomicReference<AclList> captured = new AtomicReference<>();
    IAclAdaptor adaptor = mock(IAclAdaptor.class);
    doAnswer(
            inv -> {
              Object arg = inv.getArgument(0);
              assertInstanceOf(AclList.class, arg, "saveAcls must receive AclList, not " + typeOf(arg));
              captured.set((AclList) arg);
              return null;
            })
        .when(adaptor)
        .saveAcls(any());

    AclResource resource = new AclResource();
    var field = AclResource.class.getDeclaredField("adaptor");
    field.setAccessible(true);
    field.set(resource, adaptor);

    JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
    sf.setAddress("local://issue-3391-acl-bulk-bare");
    sf.setServiceBean(resource);
    sf.setProviders(
        List.of(
            new AclListJsonReader(),
            new JacksonJsonProvider(),
            new JacksonContextResolver(),
            new WebApplicationExceptionMapper()));
    server = sf.create();

    WebClient client =
        WebClient.create("local://issue-3391-acl-bulk-bare")
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON)
            .path("/acls/bulk");
    Response response =
        client.put("[{\"name\":\"By_Author ACL\",\"objectType\":31,\"objectId\":5}]");
    assertEquals(200, response.getStatus(), response.readEntity(String.class));
    AclList saved = captured.get();
    assertNotNull(saved);
    assertEquals(1, saved.size());
    assertEquals("By_Author ACL", saved.get(0).getName());
    assertEquals(31, saved.get(0).getObjectType());
  }

  private static String typeOf(Object raw) {
    if (raw == null) {
      return "null";
    }
    return raw.getClass().getName() + (raw instanceof ArrayList && !(raw instanceof AclList)
        ? " (raw ArrayList)"
        : "");
  }
}

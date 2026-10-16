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
package com.percussion.pubserver.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.junit.jupiter.api.Test;

/** CXF JSON reads must be able to replace the property list on a publish server body. */
class PSPublishServerInfoJsonReadTest {

  @Test
  void propertyListAcceptsClearAndAdd() {
    PSPublishServerInfo info = new PSPublishServerInfo();
    assertDoesNotThrow(() -> info.getProperties().clear());
    PSPublishServerProperty property = new PSPublishServerProperty();
    property.setKey("serverip");
    property.setValue("ftp.example");
    info.getProperties().add(property);
    assertEquals("ftp.example", info.findProperty("serverip"));
  }

  @Test
  void cxfJsonProviderReadsHostOnlyBody() throws Exception {
    String json =
        "{\"serverInfo\":{\"properties\":[{\"key\":\"serverip\",\"value\":\"ftp.new.example\"}]}}";
    JSONProvider<PSPublishServerInfo> provider = new JSONProvider<>();
    PSPublishServerInfo info =
        provider.readFrom(
            PSPublishServerInfo.class,
            PSPublishServerInfo.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            new MultivaluedHashMap<>(),
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    assertEquals("ftp.new.example", info.findProperty("serverip"));
    assertEquals(1, info.getProperties().size());
  }
}

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

package com.percussion.delivery.feeds.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

/**
 * Slice 8 (#6341): the fixture seeder's {@code PUT /rss/descriptors} must
 * deserialize. Reproduces the live cell failure (feeds.log:
 * {@code Cannot construct instance of IPSFeedDescriptor}) seen after the
 * Jackson 3 migration.
 */
class PSFeedDescriptorsDeserializationTest {

  private static final String BODY = loadFixtureBody();

  private static String loadFixtureBody() {
    try (InputStream stream =
        PSFeedDescriptorsDeserializationTest.class.getResourceAsStream(
            "/feed-descriptors-fixture.json")) {
      assertNotNull(stream, "test resource feed-descriptors-fixture.json must exist");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new AssertionError("cannot load feed-descriptors-fixture.json", e);
    }
  }

  @Test
  void descriptorListDeserializesToConcreteType() throws Exception {
    PSFeedDescriptors descriptors =
        new ObjectMapper().readValue(BODY, PSFeedDescriptors.class);
    assertEquals(1, descriptors.getDescriptors().size());
    assertInstanceOf(
        PSFeedDescriptor.class, descriptors.getDescriptors().get(0));
    assertEquals("qasurface-fixture", descriptors.getDescriptors().get(0).getName());
  }

  @Test
  void descriptorListDeserializesWithJaxbProviderMapper() throws Exception {
    ObjectMapper mapper =
        new JacksonXmlBindJsonProvider()
            .locateMapper(PSFeedDescriptors.class, MediaType.APPLICATION_JSON_TYPE);
    PSFeedDescriptors descriptors = mapper.readValue(BODY, PSFeedDescriptors.class);
    assertEquals(1, descriptors.getDescriptors().size());
    assertInstanceOf(
        PSFeedDescriptor.class, descriptors.getDescriptors().get(0));
  }
}

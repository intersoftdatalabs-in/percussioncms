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
package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.keywords.KeywordSummary;
import com.percussion.sitemanage.json.JacksonContextResolver;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

/** Production keyword JSON uses the sitemanage mapper, which honors JAXB root names. */
@Tag("UnitTest")
class KeywordSummaryWireChoicesTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(KeywordSummary.class);

  @Test
  void omittedChoicesAreNotSpecified() {
    KeywordSummary body =
        mapper.readValue(
            "{\"Keyword\":{\"label\":\"Priority\",\"description\":\"note\",\"sequence\":4}}",
            KeywordSummary.class);

    assertFalse(body.isChoicesSpecified());
    assertTrue(body.getChoices().isEmpty());
    assertEquals("note", body.getDescription());
    assertEquals(4, body.getSequence());
  }

  @Test
  void emptyChoicesArrayIsSpecified() {
    KeywordSummary body =
        mapper.readValue(
            "{\"Keyword\":{\"label\":\"Priority\",\"choices\":[]}}", KeywordSummary.class);

    assertTrue(body.isChoicesSpecified());
    assertTrue(body.getChoices().isEmpty());
  }

  @Test
  void presentChoicesAreSpecified() {
    KeywordSummary body =
        mapper.readValue(
            "{\"Keyword\":{\"label\":\"Priority\",\"description\":\"note\",\"choices\":[{\"label\":\"Low\",\"value\":\"low\",\"description\":\"bottom\",\"sequence\":2}]}}",
            KeywordSummary.class);

    assertTrue(body.isChoicesSpecified());
    assertEquals(1, body.getChoices().size());
    assertEquals("bottom", body.getChoices().get(0).getDescription());
    assertEquals("note", body.getDescription());
  }

  @Test
  void liveChoiceDescriptionBodyKeepsSpecifiedDescription() {
    KeywordSummary body =
        mapper.readValue(
            "{\"Keyword\":{\"label\":\"NdKwDesc\",\"description\":\"note\",\"sequence\":4,\"choices\":[{\"label\":\"High\",\"value\":\"high\",\"sequence\":1},{\"label\":\"Low\",\"value\":\"low\",\"description\":\"bottom\",\"sequence\":2}]}}",
            KeywordSummary.class);

    assertTrue(body.isChoicesSpecified());
    assertEquals(2, body.getChoices().size());
    assertEquals(null, body.getChoices().get(0).getDescription());
    assertEquals("bottom", body.getChoices().get(1).getDescription());
  }

  @Test
  void jacksonProviderReadFromSpecifiesChoiceDescription() throws Exception {
    JacksonJsonProvider provider = new JacksonJsonProvider();
    provider.setMapper((JsonMapper) mapper);
    String json =
        "{\"Keyword\":{\"label\":\"NdKwDesc\",\"description\":\"note\",\"sequence\":4,\"choices\":[{\"label\":\"High\",\"value\":\"high\",\"sequence\":1},{\"label\":\"Low\",\"value\":\"low\",\"description\":\"bottom\",\"sequence\":2}]}}";
    @SuppressWarnings("unchecked")
    MessageBodyReader<KeywordSummary> reader = (MessageBodyReader<KeywordSummary>) (MessageBodyReader<?>) provider;
    KeywordSummary body =
        reader.readFrom(
            KeywordSummary.class,
            KeywordSummary.class,
            new java.lang.annotation.Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

    assertTrue(body.isChoicesSpecified());
    assertEquals("bottom", body.getChoices().get(1).getDescription());
  }
}

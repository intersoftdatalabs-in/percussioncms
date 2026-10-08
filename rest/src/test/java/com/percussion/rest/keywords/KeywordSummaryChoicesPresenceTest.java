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

package com.percussion.rest.keywords;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Omitted keyword choices must not look like an empty list that clears stored choices. */
class KeywordSummaryChoicesPresenceTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(KeywordSummary.class);

  @Test
  void omittedChoicesAreNotSpecifiedAndBlankDescriptionStaysBlank() throws Exception {
    KeywordSummary body =
        mapper.readValue(
            "{\"Keyword\":{\"label\":\"Priority\",\"description\":\"\",\"sequence\":4}}",
            KeywordSummary.class);

    assertFalse(body.isChoicesSpecified());
    assertTrue(body.getChoices().isEmpty());
    assertEquals("Priority", body.getLabel());
    assertEquals("", body.getDescription());
    assertEquals(4, body.getSequence());
  }

  @Test
  void nullChoicesAreNotSpecified() throws Exception {
    KeywordSummary body =
        mapper.readValue(
            "{\"Keyword\":{\"label\":\"Priority\",\"description\":\"note\",\"choices\":null}}",
            KeywordSummary.class);

    assertFalse(body.isChoicesSpecified());
    assertTrue(body.getChoices().isEmpty());
    assertEquals("note", body.getDescription());
  }

  @Test
  void emptyChoicesArrayIsSpecified() throws Exception {
    KeywordSummary body =
        mapper.readValue(
            "{\"Keyword\":{\"label\":\"Priority\",\"choices\":[]}}", KeywordSummary.class);

    assertTrue(body.isChoicesSpecified());
    assertTrue(body.getChoices().isEmpty());
  }

  @Test
  void presentChoicesAreSpecified() {
    KeywordSummary body = new KeywordSummary();
    KeywordChoiceSummary choice = new KeywordChoiceSummary();
    choice.setLabel("High");
    choice.setValue("high");
    body.setChoices(List.of(choice));

    assertTrue(body.isChoicesSpecified());
    assertEquals(1, body.getChoices().size());
    assertEquals("High", body.getChoices().get(0).getLabel());
  }

  @Test
  void setterNullClearsTheSpecifiedFlag() {
    KeywordSummary body = new KeywordSummary();
    body.setChoices(List.of(new KeywordChoiceSummary()));
    body.setChoices(null);

    assertFalse(body.isChoicesSpecified());
    assertTrue(body.getChoices().isEmpty());
  }

  @Test
  void omittedFieldIsNotSpecified() {
    KeywordSummary body = new KeywordSummary();

    assertFalse(body.isChoicesSpecified());
    assertTrue(body.getChoices().isEmpty());
  }

  @Test
  void getterMutationWithoutSetterStillCountsAsSpecified() {
    KeywordSummary body = new KeywordSummary();
    assertFalse(body.isChoicesSpecified());

    KeywordChoiceSummary choice = new KeywordChoiceSummary();
    choice.setLabel("Low");
    choice.setDescription("bottom");
    body.getChoices().add(choice);

    assertTrue(body.isChoicesSpecified());
    assertEquals("bottom", body.getChoices().get(0).getDescription());
  }

  @Test
  void emptyListWithoutSetterIsNotSpecified() throws Exception {
    KeywordSummary body = new KeywordSummary();
    Field field = KeywordSummary.class.getDeclaredField("choices");
    field.setAccessible(true);
    field.set(body, new ArrayList<KeywordChoiceSummary>());

    assertFalse(body.isChoicesSpecified());
    assertTrue(body.getChoices().isEmpty());
  }
}

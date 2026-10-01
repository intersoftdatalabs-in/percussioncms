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
package com.percussion.sitemanage.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.sitemanage.json.JacksonContextResolver;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** GET section must include {@code externalLinkUrl} as a string (#4985). */
class PSSiteSectionExternalLinkUrlJsonTest {

  @Test
  void externalLinkUrlSerializesAsString() throws Exception {
    PSSiteSection section = new PSSiteSection();
    section.setId("1");
    section.setTitle("Partner");
    section.setSectionType(PSSiteSection.PSSectionTypeEnum.externallink);
    section.setExternalLinkUrl("https://example.com/edited");

    ObjectMapper mapper = new JacksonContextResolver().getContext(PSSiteSection.class);
    String json = mapper.writeValueAsString(section);

    assertTrue(json.contains("\"externalLinkUrl\""), json);
    assertTrue(json.contains("https://example.com/edited"), json);
    assertEquals(
        "https://example.com/edited",
        mapper.readValue(json, PSSiteSection.class).getExternalLinkUrl());
  }
}

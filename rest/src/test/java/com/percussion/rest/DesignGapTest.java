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

package com.percussion.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

@Tag("UnitTest")
public class DesignGapTest {

  @Test
  public void ofSetsCodeAndMessage() {
    DesignGap g = DesignGap.of("CT_ITEM_EXITS", "Item-level pre/post exits not exposed");
    assertEquals("CT_ITEM_EXITS", g.getCode());
    assertEquals("Item-level pre/post exits not exposed", g.getMessage());
  }

  @Test
  public void contentTypeDetailSerializesStructuredDesignGaps() {
    ContentTypeDetail d = new ContentTypeDetail();
    d.setName("percPage");
    d.setDesignGaps(List.of(DesignGap.of("CT_ITEM_EXITS", "Item-level pre/post exits not exposed")));

    ObjectMapper mapper = new JacksonContextResolver().getContext(ContentTypeDetail.class);
    String json = mapper.writeValueAsString(d);

    assertTrue(json.contains("\"code\""), json);
    assertTrue(json.contains("CT_ITEM_EXITS"), json);
    assertTrue(json.contains("\"message\""), json);
    assertTrue(json.contains("Item-level pre/post exits not exposed"), json);
    // Must not serialize designGaps as bare string array entries — require object array wire shape
    assertTrue(
        json.contains("\"designGaps\":[{\"code\":\"CT_ITEM_EXITS\""),
        () -> "expected structured designGaps objects, got: " + json);
    assertTrue(
        !json.contains("\"designGaps\":[\"") && !json.contains("\"designGaps\":[ \"CT_"),
        () -> "must not serialize designGaps as bare strings: " + json);
  }

  @Test
  public void equalsAndHashCodeByCodeAndMessage() {
    DesignGap a = DesignGap.of("A", "m");
    DesignGap b = DesignGap.of("A", "m");
    DesignGap c = DesignGap.of("B", "m");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertTrue(!a.equals(c));
  }
}

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

package com.percussion.rest.contenttypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class ContentTypeFieldRuleExpressionsJsonReaderTest {

  private static final String FLAT =
      "{"
          + "\"fieldName\":\"sys_title\","
          + "\"validation\":[{\"type\":\"conditional\",\"conditionals\":"
          + "[{\"variable\":\"sys_title\",\"operator\":\"<>\",\"value\":\"#3896\"}]}],"
          + "\"visibility\":[],"
          + "\"inputTranslation\":[],"
          + "\"outputTranslation\":[]"
          + "}";

  private final ContentTypeFieldRuleExpressionsJsonReader reader =
      new ContentTypeFieldRuleExpressionsJsonReader();

  @Test
  public void parseAcceptsWrappedEnvelope() {
    ContentTypeFieldRuleExpressions out =
        ContentTypeFieldRuleExpressionsJsonReader.parse(
            "{\"ContentTypeFieldRuleExpressions\":" + FLAT + "}");
    assertEquals("sys_title", out.getFieldName());
    assertNotNull(out.getValidation());
    assertEquals(1, out.getValidation().size());
    assertEquals("conditional", out.getValidation().get(0).getType());
    assertEquals("#3896", out.getValidation().get(0).getConditionals().get(0).getValue());
    assertNotNull(out.getVisibility());
    assertTrue(out.getVisibility().isEmpty());
    assertNotNull(out.getInputTranslation());
    assertNotNull(out.getOutputTranslation());
  }

  @Test
  public void parseAcceptsFlatBody() {
    ContentTypeFieldRuleExpressions out = ContentTypeFieldRuleExpressionsJsonReader.parse(FLAT);
    assertEquals("sys_title", out.getFieldName());
    assertEquals(1, out.getValidation().size());
    assertTrue(out.getVisibility().isEmpty());
  }

  @Test
  public void parseAcceptsCamelCaseRootAlias() {
    ContentTypeFieldRuleExpressions out =
        ContentTypeFieldRuleExpressionsJsonReader.parse(
            "{\"contentTypeFieldRuleExpressions\":" + FLAT + "}");
    assertEquals("sys_title", out.getFieldName());
  }

  @Test
  public void parseEmptyIsEmptyEnvelope() {
    ContentTypeFieldRuleExpressions empty = ContentTypeFieldRuleExpressionsJsonReader.parse("  ");
    assertTrue(empty.getValidation() == null || empty.getValidation().isEmpty());
  }

  @Test
  public void parseRejectsNonObject() {
    assertThrows(
        WebApplicationException.class, () -> ContentTypeFieldRuleExpressionsJsonReader.parse("[1]"));
  }

  @Test
  public void readFromReadsUtf8Body() throws Exception {
    byte[] raw = FLAT.getBytes(StandardCharsets.UTF_8);
    ContentTypeFieldRuleExpressions out =
        reader.readFrom(
            ContentTypeFieldRuleExpressions.class,
            ContentTypeFieldRuleExpressions.class,
            new java.lang.annotation.Annotation[0],
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(raw));
    assertEquals("sys_title", out.getFieldName());
    assertTrue(reader.isReadable(ContentTypeFieldRuleExpressions.class, null, null, null));
  }
}

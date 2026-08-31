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

package com.percussion.rest.locales;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class AutoTranslationRowsJsonReaderTest {

  @Test
  public void parseBareArray() {
    List<AutoTranslationRow> rows =
        AutoTranslationRowsJsonReader.parse(
            "[{\"locale\":\"fr-fr\",\"contentTypeName\":\"percPage\"}]");
    assertEquals(1, rows.size());
    assertEquals("fr-fr", rows.get(0).getLocale());
    assertEquals("percPage", rows.get(0).getContentTypeName());
  }

  @Test
  public void parseWrappedArrayIncludingEmptyClear() {
    assertTrue(AutoTranslationRowsJsonReader.parse("{\"AutoTranslationRow\":[]}").isEmpty());
    List<AutoTranslationRow> rows =
        AutoTranslationRowsJsonReader.parse(
            "{\"AutoTranslationRow\":[{\"locale\":\"en-us\",\"contentTypeName\":\"percPage\","
                + "\"workflowName\":\"Default Workflow\",\"communityName\":\"Default\"}]}");
    assertEquals(1, rows.size());
    assertEquals("en-us", rows.get(0).getLocale());
    assertEquals("Default Workflow", rows.get(0).getWorkflowName());
  }

  @Test
  public void parseWrappedSingleObject() {
    List<AutoTranslationRow> rows =
        AutoTranslationRowsJsonReader.parse(
            "{\"AutoTranslationRow\":{\"locale\":\"ar\",\"contentTypeName\":\"percPage\"}}");
    assertEquals(1, rows.size());
    assertEquals("ar", rows.get(0).getLocale());
  }

  @Test
  public void parseEmptyAndBlank() {
    assertTrue(AutoTranslationRowsJsonReader.parse("[]").isEmpty());
    assertTrue(AutoTranslationRowsJsonReader.parse("  ").isEmpty());
    assertTrue(AutoTranslationRowsJsonReader.parse(null).isEmpty());
  }

  @Test
  public void parseRejectsScalarWrappedProperty() {
    WebApplicationException rowEx =
        assertThrows(
            WebApplicationException.class,
            () -> AutoTranslationRowsJsonReader.parse("{\"AutoTranslationRow\":\"x\"}"));
    assertEquals(400, rowEx.getResponse().getStatus());
    WebApplicationException camelEx =
        assertThrows(
            WebApplicationException.class,
            () -> AutoTranslationRowsJsonReader.parse("{\"autoTranslationRow\":\"x\"}"));
    assertEquals(400, camelEx.getResponse().getStatus());
    WebApplicationException wrappedEx =
        assertThrows(
            WebApplicationException.class,
            () -> AutoTranslationRowsJsonReader.parse("{\"AutoTranslations\":\"x\"}"));
    assertEquals(400, wrappedEx.getResponse().getStatus());
    WebApplicationException camelWrappedEx =
        assertThrows(
            WebApplicationException.class,
            () -> AutoTranslationRowsJsonReader.parse("{\"autoTranslations\":\"x\"}"));
    assertEquals(400, camelWrappedEx.getResponse().getStatus());
  }

  @Test
  public void readBoundedRejectsOversizedBody() throws Exception {
    byte[] big = new byte[5];
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                AutoTranslationRowsJsonReader.readBounded(new ByteArrayInputStream(big), 4));
    assertEquals(413, ex.getResponse().getStatus());
    byte[] ok = new byte[4];
    assertEquals(
        4, AutoTranslationRowsJsonReader.readBounded(new ByteArrayInputStream(ok), 4).length);
  }

  @Test
  public void isReadableOnlyForAutoTranslationRowList() throws Exception {
    Method m =
        AutoTranslationsResource.class.getMethod("saveAutoTranslations", List.class);
    Type generic = m.getGenericParameterTypes()[0];
    assertTrue(AutoTranslationRowsJsonReader.isAutoTranslationRowList(List.class, generic));
    assertTrue(
        !AutoTranslationRowsJsonReader.isAutoTranslationRowList(List.class, String.class));
  }
}

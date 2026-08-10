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
package test.percussion.pso.restservice.model;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.restservice.model.Field;
import com.percussion.pso.restservice.model.StringValue;
import com.percussion.pso.restservice.model.XhtmlValue;
import org.junit.jupiter.api.Test;

/** Behavioral coverage for {@link Field#Field(String, String)} string-value routing. */
public class FieldConstructorTest {

  @Test
  void shortStringBecomesStringValue() {
    Field f = new Field("title", "hello");
    assertEquals("title", f.getName());
    assertEquals("hello", f.getStringValue());
    assertInstanceOf(StringValue.class, f.getValue());
  }

  @Test
  void longStringUsesValueAttribute() {
    String longVal = "x".repeat(51);
    Field f = new Field("body", longVal);
    assertEquals("body", f.getName());
    assertEquals(longVal, f.getStringValue());
    assertEquals(longVal, f.getValueAtt());
    assertNull(f.getValue());
  }

  @Test
  void rxbodyfieldMarkupBecomesXhtmlValue() {
    String html = "<div class=\"rxbodyfield\"><p>hi</p></div>";
    Field f = new Field("rich", html);
    assertEquals("rich", f.getName());
    assertInstanceOf(XhtmlValue.class, f.getValue());
    assertEquals(html, f.getStringValue());
  }

  @Test
  void setStringValueMatchesConstructorRouting() {
    Field f = new Field();
    f.setName("n");
    f.setStringValue("short");
    assertEquals("short", f.getStringValue());
    assertInstanceOf(StringValue.class, f.getValue());
  }
}

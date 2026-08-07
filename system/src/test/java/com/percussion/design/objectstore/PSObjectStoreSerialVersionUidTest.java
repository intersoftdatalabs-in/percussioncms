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
package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Date;
import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.jupiter.api.Test;

/**
 * Java serialization checks for design.objectstore types that received {@code serialVersionUID} in
 * the #2313 D–M batch (parent #2022).
 */
public class PSObjectStoreSerialVersionUidTest {

  @Test
  public void testDateLiteralAndHtmlParameterSerialization() throws Exception {
    Date now = new Date(1_700_000_000_000L);
    FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd");
    PSDateLiteral dateLit = new PSDateLiteral(now, format);
    dateLit.setId(7);

    PSHtmlParameter htmlParam = new PSHtmlParameter("sys_contentid");
    PSDisplayTextLiteral displayLit = new PSDisplayTextLiteral("label", "value");

    byte[] bytes;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(dateLit);
      oos.writeObject(htmlParam);
      oos.writeObject(displayLit);
      bytes = bos.toByteArray();
    }

    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      PSDateLiteral serDate = (PSDateLiteral) ois.readObject();
      PSHtmlParameter serHtml = (PSHtmlParameter) ois.readObject();
      PSDisplayTextLiteral serDisplay = (PSDisplayTextLiteral) ois.readObject();

      assertEquals(dateLit, serDate);
      assertEquals(7, serDate.getId());
      assertEquals(now, serDate.getDate());
      assertEquals(htmlParam.getName(), serHtml.getName());
      assertEquals("sys_contentid", serHtml.getName());
      assertEquals(displayLit, serDisplay);
    }

    assertEquals(1L, readSerialVersionUid(PSDateLiteral.class));
    assertEquals(1L, readSerialVersionUid(PSHtmlParameter.class));
    assertEquals(1L, readSerialVersionUid(PSDisplayTextLiteral.class));
    assertEquals(1L, readSerialVersionUid(PSExtensionCall.class));
    assertEquals(1L, readSerialVersionUid(PSFieldSet.class));
  }

  private static long readSerialVersionUid(Class<?> type) throws Exception {
    Field f = type.getDeclaredField("serialVersionUID");
    f.setAccessible(true);
    return f.getLong(null);
  }
}
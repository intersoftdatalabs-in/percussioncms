/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.HTTPClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * Regression for CodeQL {@code java/implicit-cast-in-compound-assignment} #639: {@link
 * RespInputStream#skip(long)} advances {@code offset} with an explicit int cast after clamping to
 * remaining buffered bytes (no implicit long→int compound assignment).
 */
class RespInputStreamSkipTest {

  @Test
  void skipWithinLocalBufferUsesExplicitIntAdvance() throws Exception {
    RespInputStream in = new RespInputStream(null, null);
    byte[] buf = new byte[20];
    for (int i = 0; i < buf.length; i++) {
      buf[i] = (byte) (i + 1);
    }
    setField(in, "buffer", buf);
    setField(in, "offset", 0);
    setField(in, "end", 20);
    setField(in, "interrupted", false);
    setField(in, "closed", false);

    long skipped = in.skip(7L);
    assertEquals(7L, skipped);
    assertEquals(7, getIntField(in, "offset"));

    // Skip more than remaining — clamped to left
    skipped = in.skip(100L);
    assertEquals(13L, skipped);
    assertEquals(20, getIntField(in, "offset"));
  }

  @Test
  void skipWhenClosedReturnsZero() throws Exception {
    RespInputStream in = new RespInputStream(null, null);
    setField(in, "closed", true);
    assertEquals(0L, in.skip(5L));
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field f = RespInputStream.class.getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  private static int getIntField(Object target, String name) throws Exception {
    Field f = RespInputStream.class.getDeclaredField(name);
    f.setAccessible(true);
    return f.getInt(target);
  }
}

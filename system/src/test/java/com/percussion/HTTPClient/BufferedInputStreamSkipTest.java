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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.HTTPClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Regression for CodeQL {@code java/implicit-cast-in-compound-assignment} #638: {@link
 * BufferedInputStream#skip(long)} must not use an implicit long→int compound assignment on {@code
 * pos}. Explicit cast after clamping to remaining buffer bytes is required.
 */
class BufferedInputStreamSkipTest {

  @Test
  void skipWithinBufferUsesExplicitIntAdvance() throws IOException {
    byte[] data = new byte[100];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) i;
    }
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(data));
    // Prime the internal buffer
    assertEquals(0, in.read());

    long skipped = in.skip(10L);
    assertEquals(10L, skipped);

    // Next byte should be original index 11 (read one, skipped ten)
    assertEquals(11, in.read());
  }

  @Test
  void skipZeroAndNegativeReturnZero() throws IOException {
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(new byte[8]));
    assertEquals(0L, in.skip(0L));
    assertEquals(0L, in.skip(-5L));
  }
}

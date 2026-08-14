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
package com.percussion.HTTPClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Regression for CodeQL {@code java/tainted-numeric-cast} #1980: HTTP chunk sizes are
 * user-controlled longs and must not be narrowed to {@code int} without a range guard (CWE-197 /
 * CWE-681).
 */
class ChunkedInputStreamNumericCastTest {

  @Test
  void saturateAvailableCapsAtIntegerMaxWhenChunkExceedsInt() {
    assertEquals(Integer.MAX_VALUE, Codecs.saturateAvailable(Integer.MAX_VALUE + 1L, 0));
    assertEquals(Integer.MAX_VALUE, Codecs.saturateAvailable(Long.MAX_VALUE, 10));
  }

  @Test
  void saturateAvailableDoesNotWrapWhenSumExceedsInt() {
    assertEquals(
        Integer.MAX_VALUE, Codecs.saturateAvailable(Integer.MAX_VALUE - 5L, 20));
    assertEquals(42, Codecs.saturateAvailable(10L, 32));
    assertEquals(7, Codecs.saturateAvailable(-1L, 7));
    assertEquals(0, Codecs.saturateAvailable(0L, 0));
  }

  @Test
  void clampToChunkLengthNeverNarrowsOversizedChunk() {
    assertEquals(1024, Codecs.clampToChunkLength(1024, Integer.MAX_VALUE + 100L));
    assertEquals(5, Codecs.clampToChunkLength(1024, 5L));
    assertEquals(0, Codecs.clampToChunkLength(1024, 0L));
    assertEquals(0, Codecs.clampToChunkLength(0, 50L));
  }

  @Test
  void getChunkLengthRejectsValuesLargerThanIntegerMax() {
    // 0x80000000 = 2147483648 > Integer.MAX_VALUE
    byte[] header = "80000000\r\n".getBytes(StandardCharsets.US_ASCII);
    assertThrows(
        ParseException.class, () -> Codecs.getChunkLength(new ByteArrayInputStream(header)));
  }

  @Test
  void getChunkLengthAcceptsIntegerMax() throws Exception {
    byte[] header = "7FFFFFFF\r\n".getBytes(StandardCharsets.US_ASCII);
    assertEquals(Integer.MAX_VALUE, Codecs.getChunkLength(new ByteArrayInputStream(header)));
  }

  @Test
  void availableAfterSmallChunkDoesNotTruncate() throws Exception {
    // chunk of 5 bytes "hello"
    byte[] wire = "5\r\nhello\r\n0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
    ChunkedInputStream in = new ChunkedInputStream(new ByteArrayInputStream(wire));
    assertEquals('h', in.read());
    int avail = in.available();
    assertTrue(avail >= 4, "remaining chunk bytes should be visible, got " + avail);
  }

  @Test
  void availableWithHugeReflectedChunkLenDoesNotReturnNegative() throws Exception {
    ByteArrayInputStream raw = new ByteArrayInputStream(new byte[8]);
    ChunkedInputStream in = new ChunkedInputStream(raw);
    Field f = ChunkedInputStream.class.getDeclaredField("chunk_len");
    f.setAccessible(true);
    f.setLong(in, Integer.MAX_VALUE + 12345L);
    int avail = in.available();
    assertTrue(avail >= 0, "truncated cast must not go negative, got " + avail);
    assertEquals(Integer.MAX_VALUE, avail);
  }

  @Test
  void skipHugeCountDoesNotAllocateUserSizedBuffer() throws IOException {
    byte[] wire = "5\r\nhello\r\n0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
    ChunkedInputStream in = new ChunkedInputStream(new ByteArrayInputStream(wire));
    long skipped = in.skip(Integer.MAX_VALUE + 100L);
    assertEquals(5L, skipped);
    assertEquals(-1, in.read());
  }

  @Test
  void skipZeroAndNegativeReturnZero() throws IOException {
    byte[] wire = "5\r\nhello\r\n0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
    ChunkedInputStream in = new ChunkedInputStream(new ByteArrayInputStream(wire));
    assertEquals(0L, in.skip(0L));
    assertEquals(0L, in.skip(-3L));
    assertEquals('h', in.read());
  }
}

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
package com.percussion.utils.jsr170;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.jcr.RepositoryException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class PSBinaryTest {

  @Test
  public void buffersStreamAndReportsSize() throws Exception {
    byte[] data = new byte[] {10, 20, 30};
    PSBinary binary = new PSBinary(new ByteArrayInputStream(data));
    assertEquals(3L, binary.getSize());
    try (InputStream in = binary.getStream()) {
      assertEquals(10, in.read());
      assertEquals(20, in.read());
      assertEquals(30, in.read());
      assertEquals(-1, in.read());
    }
  }

  @Test
  public void readAtPosition() throws Exception {
    PSBinary binary = new PSBinary(new byte[] {1, 2, 3, 4, 5});
    byte[] buf = new byte[2];
    assertEquals(2, binary.read(buf, 2));
    assertEquals(3, buf[0]);
    assertEquals(4, buf[1]);
    assertEquals(-1, binary.read(buf, 100));
  }

  @Test
  public void disposeBlocksFurtherAccess() throws Exception {
    PSBinary binary = new PSBinary(new byte[] {1});
    binary.dispose();
    assertThrows(RepositoryException.class, binary::getSize);
    assertThrows(RepositoryException.class, binary::getStream);
  }

  @Test
  public void rejectsNullStream() {
    assertThrows(IllegalArgumentException.class, () -> new PSBinary((InputStream) null));
  }

  @Test
  public void rejectsNullByteArray() {
    assertThrows(IllegalArgumentException.class, () -> new PSBinary((byte[]) null));
  }

  @Test
  public void secondGetStreamStillWorks() throws Exception {
    PSBinary binary = new PSBinary(new byte[] {7, 8});
    try (InputStream a = binary.getStream();
        InputStream b = binary.getStream()) {
      assertEquals(7, a.read());
      assertEquals(7, b.read());
    }
    assertTrue(binary.getSize() > 0);
  }
}

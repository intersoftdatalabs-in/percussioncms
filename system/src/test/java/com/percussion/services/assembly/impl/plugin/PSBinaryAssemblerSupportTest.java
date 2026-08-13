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
package com.percussion.services.assembly.impl.plugin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.assembly.impl.plugin.PSBinaryAssemblerSupport.ResolvedSys;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Binding resolution for the binary assembler without Spring (#3280). */
@Tag("UnitTest")
class PSBinaryAssemblerSupportTest {

  @Test
  @DisplayName("resolveSys accepts string mimetype and byte[] binary")
  void successByteArray() {
    Map<String, Object> sys = new HashMap<>();
    sys.put("mimetype", "image/png");
    sys.put("binary", new byte[] {9, 8});
    Map<String, Object> bindings = new HashMap<>();
    bindings.put("$sys", sys);

    ResolvedSys resolved = PSBinaryAssemblerSupport.resolveSys(bindings);
    assertTrue(resolved.success());
    assertEquals("image/png", resolved.mimetype());
    assertArrayEquals(new byte[] {9, 8}, (byte[]) resolved.data());
  }

  @Test
  @DisplayName("resolveSys fails when $sys is missing or not a map")
  void sysNotMap() {
    assertFalse(PSBinaryAssemblerSupport.resolveSys(null).success());
    assertEquals(
        PSBinaryAssemblerSupport.ERR_SYS_NOT_MAP,
        PSBinaryAssemblerSupport.resolveSys(Map.of()).error());
    assertEquals(
        PSBinaryAssemblerSupport.ERR_SYS_NOT_MAP,
        PSBinaryAssemblerSupport.resolveSys(Map.of("$sys", "nope")).error());
  }

  @Test
  @DisplayName("resolveSys fails when mimetype or binary missing or wrong type")
  void missingOrWrongTypes() {
    Map<String, Object> noMime = new HashMap<>();
    noMime.put("binary", new byte[] {1});
    assertEquals(
        PSBinaryAssemblerSupport.ERR_MIMETYPE_UNBOUND,
        PSBinaryAssemblerSupport.resolveSys(Map.of("$sys", noMime)).error());

    Map<String, Object> noBin = new HashMap<>();
    noBin.put("mimetype", "a/b");
    assertEquals(
        PSBinaryAssemblerSupport.ERR_BINARY_UNBOUND,
        PSBinaryAssemblerSupport.resolveSys(Map.of("$sys", noBin)).error());

    Map<String, Object> badMime = new HashMap<>();
    badMime.put("mimetype", 12);
    badMime.put("binary", new byte[] {1});
    assertEquals(
        PSBinaryAssemblerSupport.ERR_MIMETYPE_TYPE,
        PSBinaryAssemblerSupport.resolveSys(Map.of("$sys", badMime)).error());

    Map<String, Object> badBin = new HashMap<>();
    badBin.put("mimetype", "a/b");
    badBin.put("binary", "nope");
    assertEquals(
        PSBinaryAssemblerSupport.ERR_BINARY_TYPE,
        PSBinaryAssemblerSupport.resolveSys(Map.of("$sys", badBin)).error());
  }
}

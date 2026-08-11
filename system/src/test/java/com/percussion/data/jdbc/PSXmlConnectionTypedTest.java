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
package com.percussion.data.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Signature/type tests for {@link PSXmlConnection} JDBC type-map methods after rawtypes cleanup.
 */
@Tag("UnitTest")
class PSXmlConnectionTypedTest {

  @Test
  void getTypeMapReturnsTypedMapSignature() throws Exception {
    Method m = PSXmlConnection.class.getMethod("getTypeMap");
    assertEquals(Map.class, m.getReturnType());
    // Generic return type must be Map<String, Class<?>>
    String generic = m.getGenericReturnType().getTypeName();
    assertEquals("java.util.Map<java.lang.String, java.lang.Class<?>>", generic);
  }

  @Test
  void setTypeMapAcceptsTypedMapSignature() throws Exception {
    Method m = PSXmlConnection.class.getMethod("setTypeMap", Map.class);
    String param = m.getGenericParameterTypes()[0].getTypeName();
    assertEquals("java.util.Map<java.lang.String, java.lang.Class<?>>", param);
  }

  @Test
  void fileSystemConnectionTypeMapSharesTypedSignature() throws Exception {
    Method get = PSFileSystemConnection.class.getMethod("getTypeMap");
    assertEquals(
        "java.util.Map<java.lang.String, java.lang.Class<?>>",
        get.getGenericReturnType().getTypeName());
  }
}

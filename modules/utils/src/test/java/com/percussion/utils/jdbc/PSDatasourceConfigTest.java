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
package com.percussion.utils.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class PSDatasourceConfigTest {

  @Test
  public void membersCtorTrimsOriginAndDatabase() {
    PSDatasourceConfig cfg = new PSDatasourceConfig("cfg", "jdbc/rx", "  dbo  ", "  rxdb  ");
    assertEquals("cfg", cfg.getName());
    assertEquals("jdbc/rx", cfg.getDataSource());
    assertEquals("dbo", cfg.getOrigin());
    assertEquals("rxdb", cfg.getDatabase());
  }

  @Test
  public void copyCtorShallowCopiesFields() {
    PSDatasourceConfig source = new PSDatasourceConfig("a", "ds", "o", "db");
    PSDatasourceConfig copy = new PSDatasourceConfig(source);
    assertEquals(source, copy);
    copy.setName("b");
    assertNotEquals(source.getName(), copy.getName());
  }

  @Test
  public void membersCtorRejectsBlankName() {
    assertThrows(
        IllegalArgumentException.class, () -> new PSDatasourceConfig("", "ds", null, null));
  }

  @Test
  public void setOriginNullBecomesEmpty() {
    PSDatasourceConfig cfg = new PSDatasourceConfig("a", "ds", "o", "db");
    cfg.setOrigin(null);
    assertEquals("", cfg.getOrigin());
  }
}

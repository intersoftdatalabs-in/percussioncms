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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import java.lang.reflect.Field;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * PSX_RXCONFIGURATIONS.CONFIGURATION is TEXT/CLOB across backends. {@code @Lob} on PostgreSQL maps
 * to OID and yields "Bad value for type long" when loading relationship XML; LONGVARCHAR keeps TEXT
 * semantics on PG/H2/MySQL/SQL Server.
 */
@Tag("UnitTest")
class PSConfigHibernateMappingTest {

  @Test
  void configurationUsesLongVarcharNotLob() throws Exception {
    Field f = PSConfig.class.getDeclaredField("m_configString");
    assertNotNull(f.getAnnotation(Column.class));
    assertEquals("CONFIGURATION", f.getAnnotation(Column.class).name());

    assertFalse(
        f.isAnnotationPresent(Lob.class),
        "@Lob must not be used — PostgreSQL maps String @Lob to OID");

    JdbcTypeCode jdbcType = f.getAnnotation(JdbcTypeCode.class);
    assertNotNull(jdbcType, "CONFIGURATION needs @JdbcTypeCode for portable TEXT/CLOB");
    assertEquals(SqlTypes.LONGVARCHAR, jdbcType.value());
  }

  @Test
  void entityStillMapsToRxConfigurationsTable() {
    jakarta.persistence.Table table = PSConfig.class.getAnnotation(jakarta.persistence.Table.class);
    assertNotNull(table);
    assertTrue(
        "PSX_RXCONFIGURATIONS".equalsIgnoreCase(table.name()),
        "table name must remain PSX_RXCONFIGURATIONS");
  }
}

/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.services.schedule.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Quartz driver-delegate resolution (#548 H2, #1500 PostgreSQL).
 *
 * <p>Matrix install smoke failed with {@code Unrecognized database driver: "h2:file"} when the
 * repository URL was {@code jdbc:h2:file:...} and the scheduler bean only knew derby/mysql/etc.
 */
@Tag("UnitTest")
class PSSchedulerBeanDriverDelegateTest {

  private static final String STD = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
  private static final String MSSQL = "org.quartz.impl.jdbcjobstore.MSSQLDelegate";
  private static final String PG = "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate";

  @Test
  void h2AndH2FileFormsUseStdDelegate() {
    assertEquals(STD, PSSchedulerBean.resolveDriverDelegateClass("h2"));
    assertEquals(STD, PSSchedulerBean.resolveDriverDelegateClass("H2"));
    assertEquals(STD, PSSchedulerBean.resolveDriverDelegateClass("h2:file"));
    assertEquals(STD, PSSchedulerBean.resolveDriverDelegateClass("h2:mem"));
    assertEquals(STD, PSSchedulerBean.resolveDriverDelegateClass("h2:tcp"));
  }

  @Test
  void postgresqlUsesPostgreSQLDelegate() {
    assertEquals(PG, PSSchedulerBean.resolveDriverDelegateClass("postgresql"));
    assertEquals(PG, PSSchedulerBean.resolveDriverDelegateClass("postgres"));
    assertEquals(PG, PSSchedulerBean.resolveDriverDelegateClass("POSTGRESQL"));
  }

  @Test
  void legacyDriversStillMapped() {
    assertEquals(MSSQL, PSSchedulerBean.resolveDriverDelegateClass("sqlserver"));
    assertEquals(MSSQL, PSSchedulerBean.resolveDriverDelegateClass("jtds:sqlserver"));
    assertEquals(STD, PSSchedulerBean.resolveDriverDelegateClass("derby"));
    assertEquals(STD, PSSchedulerBean.resolveDriverDelegateClass("mysql"));
    assertNotNull(PSSchedulerBean.resolveDriverDelegateClass("oracle:thin"));
  }

  @Test
  void blankAndUnknownReturnNull() {
    assertNull(PSSchedulerBean.resolveDriverDelegateClass(null));
    assertNull(PSSchedulerBean.resolveDriverDelegateClass(""));
    assertNull(PSSchedulerBean.resolveDriverDelegateClass("   "));
    assertNull(PSSchedulerBean.resolveDriverDelegateClass("cockroach"));
  }
}

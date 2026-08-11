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
package com.percussion.utils.container.jboss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.utils.container.IPSJndiDatasource;
import com.percussion.utils.container.PSJndiDatasourceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for the BeanUtils-free copy constructor (this-escape redesign, issue #2969).
 */
@Tag("UnitTest")
public class PSJBossJndiDatasourceCopyTest {

  @Test
  public void copyFromInterfaceCopiesSharedProperties() {
    IPSJndiDatasource source =
        new PSJndiDatasourceImpl(
            "jdbc/rx", "jtds:sqlserver", "net.sourceforge.jtds.jdbc.Driver", "host:1433", "u", "p");
    source.setMinConnections(3);
    source.setMaxConnections(30);
    source.setIdleTimeout(1000);
    source.setSecurityDomain("rx.realm");
    source.setId(7);

    PSJBossJndiDatasource copy = new PSJBossJndiDatasource(source);

    assertEquals("jdbc/rx", copy.getName());
    assertEquals("jtds:sqlserver", copy.getDriverName());
    assertEquals("net.sourceforge.jtds.jdbc.Driver", copy.getDriverClassName());
    assertEquals("host:1433", copy.getServer());
    assertEquals("u", copy.getUserId());
    assertEquals("p", copy.getPassword());
    assertEquals(3, copy.getMinConnections());
    assertEquals(30, copy.getMaxConnections());
    assertEquals(1000, copy.getIdleTimeout());
    assertEquals("rx.realm", copy.getSecurityDomain());
    assertEquals(7, copy.getId());
  }

  @Test
  public void copyFromJBossCopiesRoundTripFieldsIndependently() {
    PSJBossJndiDatasource source =
        new PSJBossJndiDatasource(
            "jdbc/oracle",
            "oracle:thin",
            "oracle.jdbc.OracleDriver",
            "host:1521:orcl",
            "scott",
            "tiger");
    source.setMinConnections(5);
    source.setMaxConnections(50);

    PSJBossJndiDatasource copy = new PSJBossJndiDatasource(source);

    assertEquals(source, copy);
    assertNotSame(source, copy);
    // Mutating the copy must not affect the source pool sizes
    copy.setMinConnections(1);
    assertEquals(5, source.getMinConnections());
    assertEquals(1, copy.getMinConnections());
  }

  @Test
  public void copyCtorRejectsNullSource() {
    assertThrows(NullPointerException.class, () -> new PSJBossJndiDatasource((IPSJndiDatasource) null));
  }
}

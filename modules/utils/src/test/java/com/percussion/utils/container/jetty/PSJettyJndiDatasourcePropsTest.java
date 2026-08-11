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
package com.percussion.utils.container.jetty;

import static com.percussion.utils.container.IPSJdbcDbmsDefConstants.DB_CONNECTION_TEST_QUERY;
import static com.percussion.utils.container.IPSJdbcDbmsDefConstants.DB_DRIVER_CLASS_NAME_PROPERTY;
import static com.percussion.utils.container.IPSJdbcDbmsDefConstants.DB_DRIVER_NAME_PROPERTY;
import static com.percussion.utils.container.IPSJdbcDbmsDefConstants.DB_NAME_PROPERTY;
import static com.percussion.utils.container.IPSJdbcDbmsDefConstants.DB_RESOURCE_NAME;
import static com.percussion.utils.container.IPSJdbcDbmsDefConstants.DB_SERVER_PROPERTY;
import static com.percussion.utils.container.IPSJdbcDbmsDefConstants.PWD_ENCRYPTED_PROPERTY;
import static com.percussion.utils.container.IPSJdbcDbmsDefConstants.PWD_PROPERTY;
import static com.percussion.utils.container.IPSJdbcDbmsDefConstants.UID_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for the final Properties constructor (this-escape redesign, issue #2969).
 */
@Tag("UnitTest")
public class PSJettyJndiDatasourcePropsTest {

  @Test
  public void propertiesCtorSeedsNamePasswordAndConnectionTest() {
    Properties props = new Properties();
    props.setProperty(DB_NAME_PROPERTY, "jdbc/default");
    props.setProperty(DB_RESOURCE_NAME, "jdbc/rxdefault");
    props.setProperty(DB_DRIVER_NAME_PROPERTY, "jtds:sqlserver");
    props.setProperty(DB_DRIVER_CLASS_NAME_PROPERTY, "net.sourceforge.jtds.jdbc.Driver");
    props.setProperty(DB_SERVER_PROPERTY, "localhost:1433");
    props.setProperty(UID_PROPERTY, "sa");
    props.setProperty(PWD_PROPERTY, "plain");
    props.setProperty(PWD_ENCRYPTED_PROPERTY, "N");
    props.setProperty(DB_CONNECTION_TEST_QUERY, "SELECT 1");

    PSJettyJndiDatasource ds = new PSJettyJndiDatasource(props);

    assertEquals("jdbc/rxdefault", ds.getName());
    assertEquals("jtds:sqlserver", ds.getDriverName());
    assertEquals("net.sourceforge.jtds.jdbc.Driver", ds.getDriverClassName());
    assertEquals("localhost:1433", ds.getServer());
    assertEquals("sa", ds.getUserId());
    assertEquals("plain", ds.getPassword());
    assertEquals("SELECT 1", ds.getConnectionTestQuery());
    assertFalse(ds.isEncrypted());
  }
}

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
package com.percussion.rx.delivery.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.pubserver.data.PSDatabasePubServer;
import com.percussion.services.pubserver.data.PSDatabasePubServer.DriverType;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.util.PSSqlHelper;
import com.percussion.utils.jdbc.PSJdbcUtils;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Oracle schema vs connect-UID origin resolution used by database / page-database
 * publishing (#953 / #2245).
 *
 * <p>No live Oracle — pure mapping and SQL qualification assertions only.
 */
public class PSDatabaseDeliveryHandlerOracleOriginTest {

  @Test
  void oracleSchemaDifferingFromUid_usesConnectUserAsOrigin() {
    var db = new PSDatabasePubServer();
    db.setDriverType(DriverType.ORACLE);
    db.setUserName("SYSTEM");
    db.setOwner("ORAPROD"); // configured schema property value

    assertEquals(
        "SYSTEM",
        PSDatabaseDeliveryHandler.resolveDbmsOrigin(db),
        "Oracle TableFactory origin must be connect UID when schema ≠ UID (ORA-01918)");
    // Configured schema remains on the pub-server view for display / datasource config.
    assertEquals("ORAPROD", db.getOwner());
    assertEquals("SYSTEM", db.getUserName());
  }

  @Test
  void oracleSchemaMatchingUid_usesConnectUserAsOrigin() {
    var db = new PSDatabasePubServer();
    db.setDriverType(DriverType.ORACLE);
    db.setUserName("ORAPROD");
    db.setOwner("ORAPROD");

    assertEquals("ORAPROD", PSDatabaseDeliveryHandler.resolveDbmsOrigin(db));
  }

  @Test
  void oracleEmptyUser_fallsBackToSchemaOwner() {
    var db = new PSDatabasePubServer();
    db.setDriverType(DriverType.ORACLE);
    db.setUserName("");
    db.setOwner("ORAPROD");

    assertEquals("ORAPROD", PSDatabaseDeliveryHandler.resolveDbmsOrigin(db));
  }

  @Test
  void oracleNullUser_fallsBackToSchemaOwner() {
    var db = new PSDatabasePubServer();
    db.setDriverType(DriverType.ORACLE);
    db.setUserName(null);
    db.setOwner("ORAPROD");

    assertEquals("ORAPROD", PSDatabaseDeliveryHandler.resolveDbmsOrigin(db));
  }

  @Test
  void oracleFromPubServerProperties_schemaDefaultsToUserWhenMissing() {
    var pub = new PSPubServer();
    pub.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "ORACLE");
    pub.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "SYSTEM");
    pub.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, "secret");
    pub.addProperty(IPSPubServerDao.PUBLISH_SID_PROPERTY, "ORCL");
    pub.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "dbhost");
    pub.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "1521");
    // no schema property → PSDatabasePubServer defaults owner to userName

    var db = new PSDatabasePubServer(pub);
    assertEquals("SYSTEM", db.getUserName());
    assertEquals("SYSTEM", db.getOwner());
    assertEquals("SYSTEM", PSDatabaseDeliveryHandler.resolveDbmsOrigin(db));
  }

  @Test
  void oracleFromPubServerProperties_schemaOraprodUidSystem_originIsSystem() {
    var pub = new PSPubServer();
    pub.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "ORACLE");
    pub.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "SYSTEM");
    pub.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, "secret");
    pub.addProperty(IPSPubServerDao.PUBLISH_SCHEMA_PROPERTY, "ORAPROD");
    pub.addProperty(IPSPubServerDao.PUBLISH_SID_PROPERTY, "ORCL");
    pub.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "PERCHCLDOMINO");
    pub.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "1521");

    var db = new PSDatabasePubServer(pub);
    assertEquals("SYSTEM", db.getUserName());
    assertEquals("ORAPROD", db.getOwner(), "schema property still stored on owner field");
    assertEquals(
        "SYSTEM",
        PSDatabaseDeliveryHandler.resolveDbmsOrigin(db),
        "TableFactory origin must not treat schema ORAPROD as Oracle user");
  }

  @Test
  void mssqlOwnerPreserved_connectUserNotUsedAsOrigin() {
    var db = new PSDatabasePubServer();
    db.setDriverType(DriverType.MSSQL);
    db.setUserName("sa");
    db.setOwner("dbo");

    assertEquals(
        "dbo",
        PSDatabaseDeliveryHandler.resolveDbmsOrigin(db),
        "MSSQL owner semantics must be preserved");
  }

  @Test
  void mssqlFromPubServerProperties_ownerAndUserSeparate() {
    var pub = new PSPubServer();
    pub.addProperty(IPSPubServerDao.PUBLISH_DRIVER_PROPERTY, "MSSQL");
    pub.addProperty(IPSPubServerDao.PUBLISH_USER_ID_PROPERTY, "sa");
    pub.addProperty(IPSPubServerDao.PUBLISH_PASSWORD_PROPERTY, "secret");
    pub.addProperty(IPSPubServerDao.PUBLISH_OWNER_PROPERTY, "dbo");
    pub.addProperty(IPSPubServerDao.PUBLISH_DATABASE_NAME_PROPERTY, "cmlite_db");
    pub.addProperty(IPSPubServerDao.PUBLISH_DATABASE_SERVER_NAME, "sqlhost");
    pub.addProperty(IPSPubServerDao.PUBLISH_PORT_PROPERTY, "1433");

    var db = new PSDatabasePubServer(pub);
    assertEquals("sa", db.getUserName());
    assertEquals("dbo", db.getOwner());
    assertEquals("dbo", PSDatabaseDeliveryHandler.resolveDbmsOrigin(db));
  }

  @Test
  void mysqlOriginIsOwnerUnchanged() {
    var db = new PSDatabasePubServer();
    db.setDriverType(DriverType.MYSQL);
    db.setUserName("root");
    db.setOwner(null);

    assertNull(PSDatabaseDeliveryHandler.resolveDbmsOrigin(db));
  }

  @Test
  void oracleQualifiedTableUsesResolvedOriginNotSchema() {
    var db = new PSDatabasePubServer();
    db.setDriverType(DriverType.ORACLE);
    db.setUserName("SYSTEM");
    db.setOwner("ORAPROD");

    String origin = PSDatabaseDeliveryHandler.resolveDbmsOrigin(db);
    String qualified =
        PSSqlHelper.qualifyTableName("PERC_EXPORT_PAGE", null, origin, PSJdbcUtils.ORACLE_DRIVER);

    assertEquals("SYSTEM.PERC_EXPORT_PAGE", qualified);
    // Unfixed path would have been ORAPROD.PERC_EXPORT_PAGE → ORA-01918 if user missing.
    assertEquals(
        "ORAPROD.PERC_EXPORT_PAGE",
        PSSqlHelper.qualifyTableName(
            "PERC_EXPORT_PAGE", null, "ORAPROD", PSJdbcUtils.ORACLE_DRIVER));
  }
}

/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.tablefactory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.security.error.PSExceptionUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.derby.drda.NetworkServerControl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test adding data to db. Assumes a database with specific credentials, change credentials or add
 * an appropriate db to run the test.
 *
 * @author dougrand
 */
@Tag("UnitTest")
public class PSTablefactoryLoadTest {

  private static final Logger log = LogManager.getLogger(PSTablefactoryLoadTest.class);

  @TempDir public Path temporaryFolder;
  private String rxdeploydir;
  protected String baseDir;
  private NetworkServerControl server;

  public PSTablefactoryLoadTest() {}

  @BeforeEach
  public void setup() throws IOException {

    rxdeploydir = System.getProperty("rxdeploydir");
    System.setProperty("rxdeploydir", temporaryFolder.toAbsolutePath().toString());

    baseDir = temporaryFolder.toAbsolutePath().toString();

    File ao_data = new File(baseDir, "ao_data.xml");
    ao_data.deleteOnExit();
    InputStream is =
        PSTablefactoryLoadTest.class.getResourceAsStream(
            "/com/percussion/tablefactory/ao_data.xml");
    FileUtils.copyInputStreamToFile(is, ao_data);

    File ao_def = new File(baseDir, "ao_def.xml");
    ao_def.deleteOnExit();
    is =
        PSTablefactoryLoadTest.class.getResourceAsStream("/com/percussion/tablefactory/ao_def.xml");
    FileUtils.copyInputStreamToFile(is, ao_def);

    File db = new File(baseDir, "db.properties");
    db.deleteOnExit();
    is =
        PSTablefactoryLoadTest.class.getResourceAsStream(
            "/com/percussion/tablefactory/db.properties");
    FileUtils.copyInputStreamToFile(is, db);

    File mc = new File(baseDir, "multichild.xml");
    mc.deleteOnExit();
    is =
        PSTablefactoryLoadTest.class.getResourceAsStream(
            "/com/percussion/tablefactory/multichild.xml");
    FileUtils.copyInputStreamToFile(is, mc);

    File networkedDerbyDB = new File(baseDir, "derby-networked_rxrepository.properties");
    networkedDerbyDB.deleteOnExit();
    is =
        PSTablefactoryLoadTest.class.getResourceAsStream(
            "/com/percussion/tablefactory/derby-networked_rxrepository.properties");
    FileUtils.copyInputStreamToFile(is, networkedDerbyDB);

    File mco = new File(baseDir, "multichild_out.xml");
    mco.deleteOnExit();
    is =
        PSTablefactoryLoadTest.class.getResourceAsStream(
            "/com/percussion/tablefactory/multichild_out.xml");
    FileUtils.copyInputStreamToFile(is, mco);

    try {
      server = new NetworkServerControl(InetAddress.getByName("localhost"), 1529);

      server.start(null);
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  @AfterEach
  public void teardown() {
    try {
      // Reset the deploy dir property if it was set prior to test
      if (rxdeploydir != null) System.setProperty("rxdeploydir", rxdeploydir);

      server.shutdown();
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  @Test
  public void testAOCase() throws Exception {
    String args[] =
        new String[] {
          baseDir + "/db.properties",
          "dummy.xml",
          baseDir + "/ao_def.xml",
          baseDir + "/ao_data.xml",
          "-mld"
        };

    PSJdbcTableFactory.main(args);
  }

  @Test
  public void testNetworkedDerby() {
    try {
      String args[] =
          new String[] {
            baseDir + "/derby-networked_rxrepository.properties",
            "dummy.xml",
            baseDir + "/ao_def.xml",
            baseDir + "/ao_data.xml",
            "-mld"
          };

      PSJdbcTableFactory.main(args);
      assertTrue(server != null);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testMultiChildCase() throws Exception {
    String args[] =
        new String[] {
          baseDir + "/db.properties", "dummy.xml", baseDir + "/multichild.xml", "-mldp"
        };

    PSJdbcTableFactory.main(args);
  }
}

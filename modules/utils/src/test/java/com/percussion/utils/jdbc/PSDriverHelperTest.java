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

import static com.percussion.util.PSResourceUtils.getResourcePath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Driver;
import org.junit.jupiter.api.Test;

/** Test case for the {@link PSDriverHelper} class */
public class PSDriverHelperTest {
  /** Constant for the driver 1 class. */
  public static final String TEST_DRIVER1_CLASS = "net.sourceforge.jtds.jdbc.Driver";

  /** Constant for the non-driver class. */
  public static final String TEST_CLASS = "net.sourceforge.jtds.util.Logger";

  /** Constant for the driver 1 file. */
  public String TEST_DRIVER1_FILE;

  /** Constant for the driver 2 class. */
  public static final String TEST_DRIVER2_CLASS = "oracle.jdbc.OracleDriver";

  /** Constant for the driver 2 file. */
  public String TEST_DRIVER2_FILE;

  @org.junit.jupiter.api.BeforeEach
  public void setUp() {
    try {
      TEST_DRIVER1_FILE =
          getResourcePath(PSDriverHelperTest.class, "/com/percussion/utils/jdbc/jtds.jar");
    } catch (Exception e) {
      TEST_DRIVER1_FILE = null;
    }
    try {
      TEST_DRIVER2_FILE =
          getResourcePath(PSDriverHelperTest.class, "/com/percussion/utils/jdbc/ojdbc6.jar");
    } catch (Exception e) {
      TEST_DRIVER2_FILE = null;
    }
  }

  /**
   * Test loading drivers
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testGetDriver() throws Exception {
    // If test resources not available, skip this test
    if (TEST_DRIVER1_FILE == null || TEST_DRIVER2_FILE == null) return;

    // Load a driver
    Driver driver = PSDriverHelper.getDriver(TEST_DRIVER1_CLASS, TEST_DRIVER1_FILE);
    assertNotNull(driver);
    String driverName = driver.getClass().getName();

    // Re-load the same driver
    assertEquals(
        driverName,
        PSDriverHelper.getDriver(TEST_DRIVER1_CLASS, TEST_DRIVER1_FILE).getClass().getName());

    // Load a different driver
    assertFalse(
        PSDriverHelper.getDriver(TEST_DRIVER2_CLASS, TEST_DRIVER2_FILE)
            .getClass()
            .getName()
            .equals(driverName));

    // Load a regular class
    try {
      PSDriverHelper.getDriver(TEST_CLASS, TEST_DRIVER1_FILE);
      fail("driver should not be found");
    } catch (Exception e) {
      // expected
    }

    // Load a non-existent class
    try {
      PSDriverHelper.getDriver("foo", TEST_DRIVER1_FILE);
      fail("driver should not be found");
    } catch (Exception e) {
      // expected
    }

    // Load from a non-existent file
    try {
      PSDriverHelper.getDriver("foo", "foo.jar");
      fail("driver should not be found");
    } catch (Exception e) {
      // expected
    }
  }
}

/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.utils.container;

import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.utils.io.PathUtils;
import com.percussion.utils.jdbc.PSJdbcUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Static container utilities.
 */
public class PSStaticContainerUtils {
  private static final Logger ms_log = LogManager.getLogger(PSStaticContainerUtils.class);

  /** Creates a static container utilities instance. All methods are static. */
  public PSStaticContainerUtils() {}

  /**
   * Loads properties from a file.
   *
   * @param f the properties file, never <code>null</code>
   * @return the loaded properties, never <code>null</code>
   */
  public static Properties getProperties(File f) {

    Properties prop = new Properties();

    if (f.exists()) {
      try (FileReader reader = new FileReader(f)) {
        // load a properties file
        prop.load(reader);
      } catch (IOException ex) {
        throw new RuntimeException("Failed to load properties file " + f.getAbsolutePath());
      }
    }
    return prop;
  }

  /**
   * Decrypts a string when encryption is enabled.
   *
   * @param str the string to decrypt, never <code>null</code>
   * @param encrypted "Y" to decrypt, anything else returns the string unchanged
   * @return the decrypted string, never <code>null</code>
   */
  public static String decrypt(String str, String encrypted) {
    if (StringUtils.equalsIgnoreCase(encrypted, "Y")) {
      try {
        str =
            PSEncryptor.decryptString(
                PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR), str);
      } catch (PSEncryptionException e) {
        str =
            PSLegacyEncrypter.getInstance(
                    PathUtils.getRxPath()
                        .toAbsolutePath()
                        .toString()
                        .concat(PSEncryptor.SECURE_DIR))
                .decrypt(
                    str,
                    PSLegacyEncrypter.getInstance(
                            PathUtils.getRxPath()
                                .toAbsolutePath()
                                .toString()
                                .concat(PSEncryptor.SECURE_DIR))
                        .getPartOneKey(),
                    null);
      }
    }
    return str;
  }

  /**
   * Returns the connection test query for the supplied driver.
   *
   * @param driver the driver name, never <code>null</code>
   * @param override an overriding query, may be <code>null</code>
   * @return the test query, or <code>null</code> when unknown
   */
  public static String connectionTestQuery(String driver, String override) {
    if (driver.equalsIgnoreCase(PSJdbcUtils.JTDS_DRIVER)) {
      return "select 1";
    }

    return null;
  }

  /**
   * Saves properties to a file.
   *
   * @param props the properties to save, never <code>null</code>
   * @param propertyFile the destination file, never <code>null</code>
   * @throws IOException on I/O errors
   */
  public static void saveProperties(Properties props, File propertyFile) throws IOException {

    File tempFile = PSStaticContainerUtils.getTempFile(propertyFile);
    boolean saved = false;
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
      // save properties to project root folder
      props.store(writer, null);
      saved = true;
    } catch (IOException e) {
      ms_log.error("error saving properties file propertyFile", e);
    }
    if (saved) FileUtils.copyFile(tempFile, propertyFile);
    Files.deleteIfExists(tempFile.toPath());
  }

  /**
   * Builds a JDBC connection URL from database properties.
   *
   * @param dbProps the database properties, never <code>null</code>
   * @return the JDBC URL, never <code>null</code>
   */
  public static String buildConnectionUrl(Properties dbProps) {
    return buildConnectionUrl(
        dbProps.getProperty(IPSJdbcDbmsDefConstants.DB_DRIVER_NAME_PROPERTY),
        dbProps.getProperty(IPSJdbcDbmsDefConstants.DB_SERVER_PROPERTY));
  }

  /**
   * Builds a JDBC connection URL from driver name and server properties.
   *
   * <p>Both fragments are trimmed: Jetty {@code Property} values and installer-written
   * properties may carry surrounding whitespace/newlines, and an untrimmed fragment produces a
   * URL the JDBC driver rejects at boot (issue #5656).
   *
   * @param driverName the database driver name (e.g., "derby", "postgresql")
   * @param server the server/database specification
   * @return the complete JDBC URL (e.g., "jdbc:derby:CMDB")
   */
  public static String buildConnectionUrl(String driverName, String server) {
    String driver = driverName == null ? "" : driverName.trim();
    String srv = server == null ? "" : server.trim();
    return "jdbc:" + driver + ":" + srv;
  }

  /**
   * Creates a temporary file next to the supplied property file.
   *
   * @param propertyFile the property file, never <code>null</code>
   * @return the temporary file, never <code>null</code>
   * @throws IOException on I/O errors
   */
  public static File getTempFile(File propertyFile) throws IOException {
    File tempFile =
        File.createTempFile(
            FilenameUtils.removeExtension(propertyFile.getAbsolutePath().toString()), ".tmp");
    tempFile.deleteOnExit();
    return tempFile;
  }
}

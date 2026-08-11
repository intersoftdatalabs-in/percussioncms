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

import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.utils.container.jboss.PSJBossJndiDatasource;
import com.percussion.utils.io.PathUtils;
import com.percussion.utils.xml.PSInvalidXmlException;
import java.util.Properties;
import org.w3c.dom.Element;

/**
 * Represents a Jetty specific JNDI datasource file.
 *
 * <p>Final so the {@link Properties} constructor may assign name / connection-test / password
 * without {@code this-escape} diagnostics under {@code -Xlint}.
 *
 * @author natechadwick
 */
public final class PSJettyJndiDatasource extends PSJBossJndiDatasource {
  // additional fields for jetty only
  private String resourceName;

  private String connectionTestQuery;

  private boolean encrypted = false;

  public PSJettyJndiDatasource(Element source) throws PSInvalidXmlException {
    super(source);
  }

  /**
   * Build from Jetty install properties. Uses direct field assignment after the multi-arg super
   * constructor so construction does not call overridable setters ({@code this-escape}).
   *
   * @param props install properties, may not be {@code null}
   */
  public PSJettyJndiDatasource(Properties props) {
    super(
        props.getProperty(DB_NAME_PROPERTY),
        props.getProperty(DB_DRIVER_NAME_PROPERTY),
        props.getProperty(DB_DRIVER_CLASS_NAME_PROPERTY),
        props.getProperty(DB_SERVER_PROPERTY),
        props.getProperty(UID_PROPERTY),
        props.getProperty(PWD_PROPERTY));

    // Jetty resource name + connection test: assign protected / local fields directly
    this.name = props.getProperty(DB_RESOURCE_NAME);
    this.resourceName = this.name;
    this.connectionTestQuery = props.getProperty(DB_CONNECTION_TEST_QUERY);

    String pwd = props.getProperty(PWD_PROPERTY);
    String encryptedFlag = props.getProperty(PWD_ENCRYPTED_PROPERTY);
    if (encryptedFlag != null && encryptedFlag.equalsIgnoreCase("Y")) {
      this.encrypted = true;
      try {
        pwd =
            PSEncryptor.decryptString(
                PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR), pwd);
      } catch (PSEncryptionException e) {
        pwd =
            PSLegacyEncrypter.getInstance(
                    PathUtils.getRxPath()
                        .toAbsolutePath()
                        .toString()
                        .concat(PSEncryptor.SECURE_DIR))
                .decrypt(
                    pwd,
                    PSLegacyEncrypter.getInstance(
                            PathUtils.getRxPath()
                                .toAbsolutePath()
                                .toString()
                                .concat(PSEncryptor.SECURE_DIR))
                        .getPartOneKey(),
                    null);
      }
    }
    this.password = pwd;
  }

  public PSJettyJndiDatasource(
      String name,
      String driverName,
      String driverClassName,
      String server,
      String userId,
      String password) {
    super(name, driverName, driverClassName, server, userId, password);
  }

  @Override
  public String getConnectionTestQuery() {
    return connectionTestQuery;
  }

  @Override
  public void setConnectionTestQuery(String connectionTestQuery) {
    this.connectionTestQuery = connectionTestQuery;
  }

  public boolean isEncrypted() {
    return encrypted;
  }

  public void setEncrypted(boolean encrypted) {
    this.encrypted = encrypted;
  }
}

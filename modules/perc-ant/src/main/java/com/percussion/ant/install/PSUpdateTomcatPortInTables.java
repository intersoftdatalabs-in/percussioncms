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

package com.percussion.ant.install;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Subclass of {@link PSExecSQLStmt} that substitutes the configured Tomcat port into the SQL
 * statement before it is executed. The literal token {@code CATALINA_PORT} in the SQL is replaced
 * with the value of the {@code tomcatPort} attribute.
 */
public class PSUpdateTomcatPortInTables extends PSExecSQLStmt {
  /** Creates a new Tomcat port update task. */
  public PSUpdateTomcatPortInTables() {
    super();
  }

  // see base class
  @Override
  public void execute() {
    String sqlStr = getSql();
    String patternStr = "CATALINA_PORT";
    Pattern pattern = Pattern.compile(patternStr);
    Matcher matcher = pattern.matcher(sqlStr);
    sqlStr = matcher.replaceAll(tomcatPort);
    setSql(sqlStr);
    super.execute();
  }

  /**
   * Returns the Tomcat port that will be substituted into the SQL statement.
   *
   * @return the Tomcat port, never <code>null</code>
   */
  public String getTomcatPort() {
    return tomcatPort;
  }

  /**
   * Sets the Tomcat port that will be substituted into the SQL statement.
   *
   * @param token the Tomcat port (Ant attribute {@code tomcatPort}) to substitute for {@code
   *     CATALINA_PORT}
   */
  public void setTomcatPort(String token) {
    this.tomcatPort = token;
  }

  /** Tomcat port from the tomcat panel */
  protected String tomcatPort = "9992";
}

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

package com.percussion.utils.container;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Constants for JDBC Jetty DBMS definitions.
 */
public interface IPSJdbcJettyDbmsDefConstants {

  /** Constant. */
  public static final String JETTY_DS_PREFIX = "perc.ds";
  /** Constant. */
  public static final String JETTY_NAME_SUFFIX = "name";
  /** Constant. */
  public static final String JETTY_UID_SUFFIX = "uid";
  /** Constant. */
  public static final String JETTY_PWD_SUFFIX = "pwd";
  /** Constant. */
  public static final String JETTY_IDLE_MS = "idle.ms";
  /** Constant. */
  public static final String JETTY_CONNECTIONS_MIN = "connections.min";
  /** Constant. */
  public static final String JETTY_CONNECTIONS_MAX = "connections.max";
  /** Constant. */
  public static final String JETTY_PWD_ENCRYPTED_SUFFIX = "pwd.encrypted";
  /** Constant. */
  public static final String JETTY_BACKEND_NAME_SUFFIX = "backend.name";
  /** Constant. */
  public static final String JETTY_DRIVER_NAME_SUFFIX = "driver.name";
  /** Constant. */
  public static final String JETTY_DRIVER_CLASS_SUFFIX = "driver.class";
  /** Constant. */
  public static final String JETTY_CONNECTIONTEST_SUFFIX = "connectiontest";
  /** Constant. */
  public static final String JETTY_SERVER_SUFFIX = "server";

  /** Constant. */
  public static final List<String> JETTY_DS_PARAMS =
      Stream.of(
              JETTY_DS_PREFIX,
              JETTY_NAME_SUFFIX,
              JETTY_UID_SUFFIX,
              JETTY_PWD_SUFFIX,
              JETTY_CONNECTIONS_MIN,
              JETTY_CONNECTIONS_MAX,
              JETTY_BACKEND_NAME_SUFFIX,
              JETTY_BACKEND_NAME_SUFFIX,
              JETTY_DRIVER_CLASS_SUFFIX,
              JETTY_CONNECTIONTEST_SUFFIX,
              JETTY_SERVER_SUFFIX)
          .collect(Collectors.toList());

  /** Constant. */
  public static final String JETTY_CONN_PREFIX = "perc.conn";
  /** Constant. */
  public static final String JETTY_CONN_NAME_SUFFIX = "name";
  /** Constant. */
  public static final String JETTY_CONN_SCHEMA_SUFFIX = "schema";
  /** Constant. */
  public static final String JETTY_CONN_DS_SUFFIX = "ds";
  /** Constant. */
  public static final String JETTY_CONN_DB_SUFFIX = "db";
  /** Constant. */
  public static final String JETTY_CONN_DEFAULT_SUFFIX = "default";

  /** Constant. */
  public static final List<String> JETTY_CONN_PARAMS =
      Stream.of(
              JETTY_CONN_NAME_SUFFIX,
              JETTY_CONN_SCHEMA_SUFFIX,
              JETTY_CONN_DS_SUFFIX,
              JETTY_CONN_DB_SUFFIX,
              JETTY_CONN_DEFAULT_SUFFIX)
          .collect(Collectors.toList());
}

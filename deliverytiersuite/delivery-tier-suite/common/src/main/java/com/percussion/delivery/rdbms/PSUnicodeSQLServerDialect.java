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

package com.percussion.delivery.rdbms;

/**
 * Custom Dialect Class used to redefine native types for hibernate using MSSql Server 2008
 *
 * @author federicoromanelli
 *
 */
public class PSUnicodeSQLServerDialect extends org.hibernate.dialect.SQLServerDialect {
  /**
   * Initializes a new instance of the {@link org.hibernate.dialect.SQLServerDialect} class.
   *
   * Note: the mapping for the values used in registerColumnType method are the same
   * as the ones described in the following file:
   * "\system\Tools\TableFactory\src\com\percussion\tablefactory\PSJdbcDataTypeMaps.xml"
   */
  public PSUnicodeSQLServerDialect() {
    super();
  }
}

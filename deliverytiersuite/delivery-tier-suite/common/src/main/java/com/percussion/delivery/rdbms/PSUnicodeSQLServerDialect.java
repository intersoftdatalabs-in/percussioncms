/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.delivery.rdbms;

import org.hibernate.dialect.SQLServer2012Dialect;

import java.sql.Types;

/**
 * Custom Dialect Class used to redefine native types for hibernate using MSSql Server 2008
 * 
 * @author federicoromanelli
 *
 */
public class PSUnicodeSQLServerDialect extends SQLServer2012Dialect
{
        /**
         * Initializes a new instance of the {@link SQLServer2012Dialect} class.
         * 
         * Note: the mapping for the values used in registerColumnType method are the same
         * as the ones described in the following file:
         * "\system\Tools\TableFactory\src\com\percussion\tablefactory\PSJdbcDataTypeMaps.xml"
         */
        public PSUnicodeSQLServerDialect() {
            super();
            
            // Register the native data types to use by Hibernate
            // Make sure to use the values included in PSJdbcDataTypeMaps.xml
            registerColumnType(Types.BIT, "BIT");
            registerColumnType(Types.TINYINT, "TINYINT");
            registerColumnType(Types.SMALLINT, "SMALLINT");            
            registerColumnType(Types.INTEGER, "INT");
            registerColumnType(Types.BIGINT, "BIGINT");
            registerColumnType(Types.FLOAT, "REAL");
            registerColumnType(Types.REAL, "REAL");
            registerColumnType(Types.DOUBLE, "FLOAT(53)");
            registerColumnType(Types.NUMERIC, "NUMERIC(18,0)");
            registerColumnType(Types.DECIMAL, "DECIMAL(18,0)");
            registerColumnType(Types.CHAR, "NCHAR($l)");
            registerColumnType(Types.VARCHAR, "NVARCHAR($l)");
            registerColumnType(Types.LONGVARCHAR, "NTEXT");
            registerColumnType(Types.DATE, "DATETIME");
            registerColumnType(Types.TIME, "DATETIME");
            registerColumnType(Types.TIMESTAMP, "DATETIME");
            registerColumnType(Types.BINARY, "BINARY");
            registerColumnType(Types.VARBINARY, "VARBINARY");
            registerColumnType(Types.LONGVARBINARY, "IMAGE");
            registerColumnType(Types.BLOB, "IMAGE");
            registerColumnType(Types.CLOB, "NTEXT");
        }
}

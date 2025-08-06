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

package com.percussion.services.datasource;

import org.hibernate.dialect.DerbyTenSevenDialect;

import java.sql.Types;

/**
 * Hibernate dialect for Derby database version 10.14 with modern Java 11 enhancements.
 *
 * <p>This dialect extends DerbyTenSevenDialect and provides specific type mappings
 * for Derby 10.14, including proper handling of nationalized types which are
 * unsupported by Derby database engine.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
public class DerbyTenOneFourDialect extends DerbyTenSevenDialect {

    /**
     * Default constructor that configures Derby-specific type mappings.
     *
     * <p>This constructor remaps nationalized types to regular types since
     * Derby database does not support nationalized character types.</p>
     */
    public DerbyTenOneFourDialect() {
        super();

        // Remap nationalized types as they are unsupported by Derby
        registerColumnType(Types.NCHAR, "char($l)");
        registerColumnType(Types.NVARCHAR, "varchar($l)");
        registerColumnType(Types.LONGNVARCHAR, "long varchar($l)");
        registerColumnType(Types.NCLOB, "clob($l)");
    }

    /**
     * Get the cross join separator for Derby database.
     *
     * @return The cross join separator string
     */
    @Override
    public String getCrossJoinSeparator() {
        return ", ";
    }

    /**
     * Indicates whether this dialect supports nationalized types.
     *
     * <p>Derby database does not support nationalized character types,
     * so this method returns false to ensure proper type mapping.</p>
     *
     * @return false, as Derby does not support nationalized types
     */
    @Override
    public boolean supportsNationalizedTypes() {
        return false;
    }
}

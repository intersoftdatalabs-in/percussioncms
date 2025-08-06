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

package com.percussion.services.datasource.impl;

import oracle.jdbc.OracleConnectionWrapper;
import oracle.jdbc.driver.OracleConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Wrapper;
import java.util.Objects;

/**
 * Oracle-specific connection wrapper that provides enhanced Oracle database functionality
 * with modern Java 11 validation patterns.
 *
 * <p>This wrapper extends Oracle's OracleConnectionWrapper to provide consistent
 * connection management while maintaining Oracle-specific features and ensuring
 * proper resource cleanup.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
public class PSOracleConnectionWrapper extends OracleConnectionWrapper {

    /**
     * The delegate connection for proper resource management.
     */
    private final Connection delegate;

    /**
     * Constructs a new Oracle connection wrapper with enhanced validation.
     *
     * @param delegate The wrapper containing the Oracle connection, may not be null
     * @throws SQLException if the Oracle connection cannot be unwrapped
     * @throws IllegalArgumentException if delegate is null
     */
    public PSOracleConnectionWrapper(Wrapper delegate) throws SQLException {
        super(unwrapOracleConnection(delegate));
        this.delegate = (Connection) Objects.requireNonNull(delegate,
            "delegate may not be null");
    }

    /**
     * Safely unwrap the Oracle connection with enhanced error handling.
     *
     * @param wrapper The wrapper to unwrap
     * @return The unwrapped Oracle connection
     * @throws SQLException if unwrapping fails
     * @throws IllegalArgumentException if wrapper is null
     */
    private static OracleConnection unwrapOracleConnection(Wrapper wrapper) throws SQLException {
        Objects.requireNonNull(wrapper, "wrapper may not be null");

        try {
            return wrapper.unwrap(OracleConnection.class);
        } catch (SQLException e) {
            throw new SQLException("Failed to unwrap Oracle connection", e);
        }
    }

    /**
     * Closes the delegate connection ensuring proper resource cleanup.
     *
     * @throws SQLException if closing the connection fails
     */
    @Override
    public void close() throws SQLException {
        try {
            delegate.close();
        } catch (SQLException e) {
            throw new SQLException("Failed to close Oracle connection", e);
        }
    }

    @Override
    public String toString() {
        return String.format("PSOracleConnectionWrapper{delegate=%s}",
            delegate != null ? delegate.getClass().getSimpleName() : "null");
    }
}
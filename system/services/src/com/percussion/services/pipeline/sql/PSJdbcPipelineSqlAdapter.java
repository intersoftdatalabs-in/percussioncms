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

package com.percussion.services.pipeline.sql;

import com.percussion.services.pipeline.PSPipelineIrException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.sql.DataSource;

/**
 * JDBC implementation of {@link IPSPipelineSqlAdapter} using prepared statements only.
 *
 * <p>Connections are obtained per call from the supplied {@link DataSource} (or supplier) and
 * closed after use. Suitable for H2 unit tests and production datasources.
 */
public class PSJdbcPipelineSqlAdapter implements IPSPipelineSqlAdapter {

  private final Supplier<Connection> connectionSupplier;

  public PSJdbcPipelineSqlAdapter(DataSource dataSource) {
    Objects.requireNonNull(dataSource, "dataSource");
    this.connectionSupplier =
        () -> {
          try {
            return dataSource.getConnection();
          } catch (SQLException e) {
            throw new IllegalStateException("Failed to obtain JDBC connection", e);
          }
        };
  }

  /**
   * @param connectionSupplier must return a new open connection each call; adapter closes it
   */
  public PSJdbcPipelineSqlAdapter(Supplier<Connection> connectionSupplier) {
    this.connectionSupplier = Objects.requireNonNull(connectionSupplier, "connectionSupplier");
  }

  @Override
  public List<Map<String, Object>> query(PSPipelineSqlPlan plan) throws PSPipelineIrException {
    Objects.requireNonNull(plan, "plan");
    if (plan.getKind() != PSPipelineSqlPlan.Kind.QUERY) {
      throw new PSPipelineIrException("SQL plan is not a QUERY: " + plan.getKind());
    }
    try (Connection conn = open();
        PreparedStatement ps = conn.prepareStatement(plan.getSql())) {
      bind(ps, plan.getParameters());
      try (ResultSet rs = ps.executeQuery()) {
        return mapRows(rs);
      }
    } catch (SQLException e) {
      throw new PSPipelineIrException("Pipeline SQL query failed: " + plan.getDescription(), e);
    } catch (RuntimeException e) {
      throw new PSPipelineIrException("Pipeline SQL query failed: " + plan.getDescription(), e);
    }
  }

  @Override
  public int update(PSPipelineSqlPlan plan) throws PSPipelineIrException {
    Objects.requireNonNull(plan, "plan");
    if (plan.getKind() != PSPipelineSqlPlan.Kind.UPDATE) {
      throw new PSPipelineIrException("SQL plan is not an UPDATE: " + plan.getKind());
    }
    try (Connection conn = open();
        PreparedStatement ps = conn.prepareStatement(plan.getSql())) {
      bind(ps, plan.getParameters());
      return ps.executeUpdate();
    } catch (SQLException e) {
      throw new PSPipelineIrException("Pipeline SQL update failed: " + plan.getDescription(), e);
    } catch (RuntimeException e) {
      throw new PSPipelineIrException("Pipeline SQL update failed: " + plan.getDescription(), e);
    }
  }

  private Connection open() throws SQLException {
    try {
      Connection conn = connectionSupplier.get();
      if (conn == null) {
        throw new SQLException("Connection supplier returned null");
      }
      return conn;
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      if (cause instanceof SQLException sqlEx) {
        throw sqlEx;
      }
      throw e;
    }
  }

  private static void bind(PreparedStatement ps, List<Object> parameters) throws SQLException {
    if (parameters == null) {
      return;
    }
    for (int i = 0; i < parameters.size(); i++) {
      ps.setObject(i + 1, parameters.get(i));
    }
  }

  private static List<Map<String, Object>> mapRows(ResultSet rs) throws SQLException {
    ResultSetMetaData meta = rs.getMetaData();
    int cols = meta.getColumnCount();
    List<Map<String, Object>> rows = new ArrayList<>();
    while (rs.next()) {
      Map<String, Object> row = new LinkedHashMap<>();
      for (int c = 1; c <= cols; c++) {
        String label = meta.getColumnLabel(c);
        if (label == null || label.isBlank()) {
          label = meta.getColumnName(c);
        }
        Object value = rs.getObject(c);
        row.put(label, value);
      }
      rows.add(row);
    }
    return rows;
  }
}

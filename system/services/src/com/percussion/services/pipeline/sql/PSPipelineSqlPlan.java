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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Parameterized SQL plan: JDBC SQL with {@code ?} placeholders and ordered bind values. */
public final class PSPipelineSqlPlan {

  public enum Kind {
    QUERY,
    UPDATE
  }

  private final Kind kind;
  private final String sql;
  private final List<Object> parameters;
  private final String description;

  public PSPipelineSqlPlan(Kind kind, String sql, List<Object> parameters, String description) {
    this.kind = Objects.requireNonNull(kind, "kind");
    this.sql = Objects.requireNonNull(sql, "sql");
    this.parameters =
        parameters != null
            ? Collections.unmodifiableList(new ArrayList<>(parameters))
            : List.of();
    this.description = description != null ? description : kind.name();
  }

  public Kind getKind() {
    return kind;
  }

  public String getSql() {
    return sql;
  }

  public List<Object> getParameters() {
    return parameters;
  }

  public String getDescription() {
    return description;
  }
}

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
package com.percussion.services.datasource;

import java.util.Set;
import org.hibernate.LockOptions;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.internal.PessimisticLockKind;
import org.hibernate.sql.ast.internal.StandardLockingClauseStrategy;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;

/**
 * Custom Derby dialect that fixes a bug in Hibernate 7.2.x community
 * {@link DerbyDialect} where the FOR UPDATE locking clause is rendered as
 * {@code for update with rs with rs} due to both the lock string methods
 * and {@code DerbyLockingClauseStrategy.renderResultSetOptions()} appending
 * {@code " with rs"}.
 *
 * <p>This dialect overrides the locking clause strategy to suppress the
 * duplicate {@code with rs} suffix. Derby's correct syntax is:
 * <ul>
 *   <li>{@code SELECT ... FOR UPDATE WITH RS} (pessimistic write lock)</li>
 *   <li>{@code SELECT ... FOR READ ONLY WITH RS} (pessimistic read lock)</li>
 * </ul>
 *
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-XXXXX">Hibernate issue tracker</a>
 */
public class PSCommunityDerbyDialect extends DerbyDialect {

  /**
   * Overrides the locking clause strategy to use a version that does not
   * append the redundant {@code " with rs"} suffix. The parent dialect's
   * {@code getForUpdateString()} and {@code getWriteLockString()} methods
   * already include {@code " for update with rs"}, so the result set option
   * rendering must be a no-op.
   */
  @Override
  protected LockingClauseStrategy buildLockingClauseStrategy(
      PessimisticLockKind lockKind,
      RowLockStrategy rowLockStrategy,
      LockOptions lockOptions,
      Set<NavigablePath> navigablePaths) {
    return new PSFixedDerbyLockingClauseStrategy(
        this, lockKind, rowLockStrategy, lockOptions, navigablePaths);
  }

  /**
   * Locking clause strategy for Derby that renders the result set options
   * as empty to avoid the double {@code with rs} problem.
   */
  private static class PSFixedDerbyLockingClauseStrategy
      extends StandardLockingClauseStrategy {

    PSFixedDerbyLockingClauseStrategy(
        PSCommunityDerbyDialect dialect,
        PessimisticLockKind lockKind,
        RowLockStrategy rowLockStrategy,
        LockOptions lockOptions,
        Set<NavigablePath> navigablePaths) {
      super(dialect, lockKind, rowLockStrategy, lockOptions, navigablePaths);
    }

    @Override
    protected void renderResultSetOptions(SqlAppender sqlAppender) {
      // No-op: the lock string from the dialect already includes "with rs"
    }
  }
}

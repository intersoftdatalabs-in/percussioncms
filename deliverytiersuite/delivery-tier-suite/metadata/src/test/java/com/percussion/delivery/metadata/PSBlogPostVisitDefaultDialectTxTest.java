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
package com.percussion.delivery.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.delivery.metadata.data.PSBlogPostVisit;
import com.percussion.delivery.metadata.rdbms.impl.PSDbBlogPostVisit;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Issue #6375 (parent #6237): the {@code --with-dts} cell's
 * {@code metadata.log} showed {@code PSBlogPostVisitService.saveVisits} failing every scheduler
 * tick with {@code DefaultJpaDialect does not support custom isolation levels}. Root cause: {@code
 * PSBlogPostVisitDao} demanded {@code Isolation.READ_UNCOMMITTED} on every method, and the shipped
 * {@code JpaTransactionManager} (default JPA dialect, no {@code HibernateJpaDialect} like the
 * regular test context) refuses to begin a transaction with a custom isolation level — the same
 * root cause as #6343 / #6374 on the query path. Tracked visits were silently dropped while the
 * surface spec stayed green on the result-list shape.
 *
 * <p>This test boots the production-parity context ({@code test-beans-default-dialect.xml}: {@code
 * JpaTransactionManager} without a dialect, as shipped) and drives {@code save} +
 * {@code getTopVisitedPages} through the transactional proxy, so it fails with the cell's exact
 * exception before the fix and passes after.
 */
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-beans-default-dialect.xml"})
public class PSBlogPostVisitDefaultDialectTxTest {

  @Autowired public IPSBlogPostVisitDao visitsDao;

  @Autowired private SessionFactory sessionFactory;

  private final List<String> createdPagepaths = new ArrayList<>();

  /**
   * The module suite shares one H2 across test classes ({@code PSMostReadServiceTest} asserts
   * exact least-hit ordering), so every saved row is removed here — the DAO {@code delete}
   * overloads are unimplemented ({@code UnsupportedOperationException}).
   */
  @AfterEach
  public void cleanUp() {
    if (createdPagepaths.isEmpty()) {
      return;
    }
    try (Session session = sessionFactory.openSession()) {
      Transaction tx = session.beginTransaction();
      session
          .createMutationQuery("delete from PSDbBlogPostVisit v where v.pagepath in (:paths)")
          .setParameterList("paths", createdPagepaths)
          .executeUpdate();
      tx.commit();
    }
    createdPagepaths.clear();
  }

  @Test
  public void saveAndQueryBeginWithoutCustomIsolation() {
    String pagepath = "/qacell/surface-" + System.currentTimeMillis() + ".html";
    createdPagepaths.add(pagepath);
    List<IPSBlogPostVisit> visits =
        List.of(new PSBlogPostVisit(pagepath, LocalDate.now(), BigInteger.ONE));

    // Pre-fix this throws InvalidIsolationLevelException from the
    // default-dialect transaction manager (the cell's saveVisits failure).
    visitsDao.save(visits);

    List<PSDbBlogPostVisit> found = visitsDao.findBlogPostVisit(pagepath);
    assertNotNull(found);
    assertEquals(1, found.size());

    // Pre-fix this also throws: the read methods carried the same custom
    // isolation (the REST layer swallowed it into an empty top-list).
    List<String> top = visitsDao.getTopVisitedPages("/qacell/", 7, 10, "desc");
    assertNotNull(top);
    assertTrue(
        top.contains(pagepath),
        "the saved visit must be visible to the top-visited query");
  }
}

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
package com.percussion.delivery.likes.service.rdbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.delivery.likes.data.IPSLikes;
import com.percussion.delivery.likes.services.IPSLikesDao;
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
 * Issue #6670 (parent #6237): {@code PSLikesDao} carried a class-level {@code
 * Transactional(REQUIRED, READ_COMMITTED)} annotation, and the shipped DTS {@code
 * JpaTransactionManager} uses the default JPA dialect, which refuses to begin a transaction with
 * any custom isolation level ({@code InvalidIsolationLevelException} — the same root cause as
 * #6343 / #6375). Any call that starts (rather than joins) a transaction through this DAO 500s;
 * the comments surface specs stayed green only because the exercised paths join an outer
 * transaction.
 *
 * <p>This test boots the production-parity context ({@code test-beans-default-dialect.xml}: {@code
 * JpaTransactionManager} without a dialect, as shipped) and drives {@code create} + {@code save}
 * + {@code find} + {@code incrementTotal} through the transactional proxy with no outer
 * transaction, so it fails with the exact {@code InvalidIsolationLevelException} before the fix
 * and passes after.
 */
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-beans-default-dialect.xml"})
public class PSLikesDefaultDialectTxTest {

  @Autowired public IPSLikesDao likesDao;

  @Autowired private SessionFactory sessionFactory;

  private String createdSite;

  /**
   * The module suite shares one H2 across test classes, so every saved row is removed here (the
   * DAO {@code delete} overload takes generated ids, which the save path does not return).
   */
  @AfterEach
  public void cleanUp() {
    if (createdSite == null) {
      return;
    }
    try (Session session = sessionFactory.openSession()) {
      Transaction tx = session.beginTransaction();
      session
          .createMutationQuery("delete from PSLikes l where l.site = :site")
          .setParameter("site", createdSite)
          .executeUpdate();
      tx.commit();
    }
    createdSite = null;
  }

  @Test
  public void saveFindAndIncrementBeginWithoutCustomIsolation() throws Exception {
    createdSite = "qacell-site-" + System.currentTimeMillis();
    String likeId = "like-" + System.currentTimeMillis();
    String type = IPSLikes.Type.comment.toString();

    // Pre-fix each proxied call below throws InvalidIsolationLevelException from the
    // default-dialect transaction manager (the production likes failure).
    IPSLikes like = likesDao.create(createdSite, likeId, type);
    assertNotNull(like);
    likesDao.save(like);

    List<IPSLikes> found = likesDao.find(createdSite, likeId, type);
    assertNotNull(found);
    assertEquals(1, found.size());

    int total = likesDao.incrementTotal(createdSite, likeId, type);
    assertEquals(1, total);
  }
}

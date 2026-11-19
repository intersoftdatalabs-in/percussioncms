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
package com.percussion.delivery.forms.impl.rdbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.delivery.forms.IPSFormDao;
import com.percussion.delivery.forms.data.IPSFormData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issue #6670 (parent #6237): {@code PSFormDao} carried a class-level {@code Transactional(REQUIRED,
 * READ_COMMITTED)} annotation. The shipped DTS {@code JpaTransactionManager} is dialect-less in
 * {@code WEB-INF/beans.xml} and would reject any explicit isolation level with {@code
 * InvalidIsolationLevelException} (same root cause as #6343 / #6375) — in practice the shipped
 * {@code LocalContainerEntityManagerFactoryBean} + {@code HibernateJpaVendorAdapter} lets the
 * transaction manager auto-detect a {@code HibernateJpaDialect} that tolerates the custom level,
 * which is why this surface never 500s today. The explicit level is still a landmine (any EMF /
 * adapter change re-arms it), so the annotation drops it and this test locks the contract: the
 * DAO must request the default isolation, and {@code createFormData} + {@code save} + {@code
 * findFormsByName} must round-trip through the transactional proxy with no outer transaction.
 */
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-beans.xml"})
public class PSFormDefaultDialectTxTest {

  @Autowired public IPSFormDao formDao;

  private String createdFormName;

  /** The module suite shares one H2 across test classes, so every saved row is removed here. */
  @AfterEach
  public void cleanUp() {
    if (createdFormName == null) {
      return;
    }
    List<IPSFormData> leftovers = formDao.findFormsByName(createdFormName);
    for (IPSFormData form : leftovers) {
      formDao.delete(form);
    }
    createdFormName = null;
  }

  @Test
  public void daoRequestsDefaultIsolation() {
    Transactional tx =
        com.percussion.delivery.forms.impl.rdbms.PSFormDao.class.getAnnotation(
            Transactional.class);
    assertNotNull(tx, "PSFormDao must stay a Spring-managed transactional bean");
    assertEquals(
        Isolation.DEFAULT,
        tx.isolation(),
        "PSFormDao must not request a custom isolation level "
            + "(the shipped dialect-less JpaTransactionManager rejects it)");
  }

  @Test
  public void saveAndFindBeginWithoutCustomIsolation() {
    createdFormName = "qacell-form-" + System.currentTimeMillis();
    Map<String, String[]> data = new HashMap<>();
    data.put("field1", new String[] {"value1"});

    // Each proxied call below must begin its own transaction with the default isolation level.
    IPSFormData form = formDao.createFormData(createdFormName, data);
    assertNotNull(form);
    formDao.save(form);

    List<IPSFormData> found = formDao.findFormsByName(createdFormName);
    assertNotNull(found);
    assertEquals(1, found.size());
    assertEquals(createdFormName, found.get(0).getName());
  }
}

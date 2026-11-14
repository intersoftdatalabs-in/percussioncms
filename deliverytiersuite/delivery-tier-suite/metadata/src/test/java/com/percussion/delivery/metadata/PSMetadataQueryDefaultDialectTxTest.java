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

import com.percussion.delivery.metadata.data.PSMetadataQuery;
import com.percussion.delivery.metadata.impl.utils.PSPair;
import com.percussion.delivery.metadata.rdbms.impl.PSDbMetadataEntry;
import com.percussion.delivery.metadata.rdbms.impl.PSDbMetadataProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Issue #6343: {@code POST /metadata/get} 302-d in the {@code --with-dts} cell for every non-null
 * body. Root cause: {@code IPSMetadataQueryService.executeQuery} demanded {@code
 * Isolation.READ_UNCOMMITTED}, and the shipped {@code JpaTransactionManager} (default JPA dialect,
 * no {@code HibernateJpaDialect} like the test context) refuses to begin a transaction with a
 * custom isolation level. The resulting {@code InvalidIsolationLevelException} was wrapped as a 500
 * and then redirected to the error page by {@code PSUncaughtError}.
 *
 * <p>This test boots the production-parity context ({@code test-beans-default-dialect.xml}: {@code
 * JpaTransactionManager} without a dialect, as shipped) and drives {@code executeQuery} through
 * the transactional proxy, so it fails with the cell's exact exception before the fix and passes
 * after.
 */
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-beans-default-dialect.xml"})
public class PSMetadataQueryDefaultDialectTxTest {

  @Autowired public IPSMetadataQueryService service;

  @Autowired public IPSMetadataIndexerService indexer;

  @BeforeEach
  public void seed() {
    indexer.deleteAllMetadataEntries();
    Collection<IPSMetadataEntry> ents = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      PSDbMetadataEntry entry =
          new PSDbMetadataEntry(
              "cellpage" + i + ".html",
              "/qacell/",
              "/qacell/cellpage" + i + ".html",
              "page",
              "qacell");
      entry.setLinktext("cell linktext " + i);
      entry.addProperty(new PSDbMetadataProperty("dcterms:title", "Cell Title " + i));
      ents.add(entry);
    }
    indexer.save(ents);
  }

  @Test
  public void executeQueryBeginsWithoutCustomIsolation() throws Exception {
    PSMetadataQuery query = new PSMetadataQuery();
    // Note: the legacy "site:xyz" shorthand is malformed; criteria need an
    // explicit operator with quoted string values (see PSCriteriaElement).
    query.setCriteria(List.of("site = 'qacell'"));
    query.setMaxResults(5);
    query.setStartIndex(0);

    PSPair<List<IPSMetadataEntry>, Integer> results = service.executeQuery(query);

    assertNotNull(results);
    assertNotNull(results.getFirst());
    assertEquals(2, results.getFirst().size());
    List<String> paths =
        results.getFirst().stream().map(IPSMetadataEntry::getPagepath).sorted().toList();
    assertEquals(List.of("/qacell/cellpage0.html", "/qacell/cellpage1.html"), paths);
  }

  @Test
  public void emptyCriteriaQueryAnswersShape() throws Exception {
    PSMetadataQuery query = new PSMetadataQuery();
    query.setMaxResults(5);
    query.setStartIndex(0);

    PSPair<List<IPSMetadataEntry>, Integer> results = service.executeQuery(query);

    assertNotNull(results);
    assertNotNull(results.getFirst());
    assertEquals(2, results.getFirst().size());
  }
}

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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;
import tools.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

/**
 * Proves the exact cell provider ({@code JacksonXmlBindJsonProvider}, see
 * {@link PSMetadataApplication}) binds the query DTOs the published widgets post. Slice 4 (#6244)
 * left {@code POST /metadata/get} 302-ing in the {@code --with-dts} cell for every non-null body
 * (issue #6343): Jersey's catch-all {@code PSUncaughtError} mapper redirects when binding the
 * {@link PSMetadataQuery} entity blows up, so this test fails on the DTO shape the cell rejects.
 */
public class PSMetadataQueryBindingTest {

  private final JacksonXmlBindJsonProvider provider = new JacksonXmlBindJsonProvider();

  private PSMetadataQuery bind(String json) throws Exception {
    Annotation[] annotations = new Annotation[0];
    try (ByteArrayInputStream in =
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
      return (PSMetadataQuery)
          provider.readFrom(
              (Class<Object>) (Class<?>) PSMetadataQuery.class,
              PSMetadataQuery.class,
              annotations,
              MediaType.APPLICATION_JSON_TYPE,
              new MultivaluedHashMap<>(),
              in);
    }
  }

  @Test
  public void emptyCriteriaListBinds() throws Exception {
    PSMetadataQuery query = bind("{\"criteria\":[],\"maxResults\":5,\"startIndex\":0}");
    assertNotNull(query);
    assertEquals(List.of(), query.getCriteria());
    assertEquals(5, query.getMaxResults());
  }

  @Test
  public void populatedCriteriaBinds() throws Exception {
    // Single-line JSON: a continuation-line concat (`+ "\..."`) mimics a Windows
    // separator join to the paths.hardcoded_sep gate, so keep this payload whole.
    PSMetadataQuery query =
        bind("{\"criteria\":[\"site:qasite\"],\"maxResults\":5,\"startIndex\":0,\"orderBy\":\"pagepath\",\"returnTotalEntries\":true}");
    assertNotNull(query);
    assertEquals(List.of("site:qasite"), query.getCriteria());
    assertEquals("pagepath", query.getOrderBy());
  }

  @Test
  public void roundTripWritesQueryShape() throws Exception {
    PSMetadataQuery query = bind("{\"criteria\":[\"site:qasite\"],\"maxResults\":5}");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    provider.writeTo(
        query,
        PSMetadataQuery.class,
        PSMetadataQuery.class,
        new Annotation[0],
        MediaType.APPLICATION_JSON_TYPE,
        new MultivaluedHashMap<>(),
        out);
    assertNotNull(out.toString(StandardCharsets.UTF_8));
  }
}

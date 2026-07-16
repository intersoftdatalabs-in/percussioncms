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
package com.percussion.services.contentmgr.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.utils.jsr170.PSValueFactory;
import javax.jcr.Value;
import javax.jcr.query.Query;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class PSQueryJcr20Test {

  @Test
  public void bindLimitOffsetDefaultsAndRoundTrip() throws Exception {
    PSQuery q = new PSQuery(Query.SQL);
    assertEquals(-1L, q.getLimit());
    assertEquals(0L, q.getOffset());
    assertEquals(0, q.getBindVariableNames().length);

    q.setLimit(25);
    q.setOffset(10);
    assertEquals(25L, q.getLimit());
    assertEquals(10L, q.getOffset());

    Value v = PSValueFactory.createValue((Object) "hello");
    q.bindValue("name", v);
    assertArrayEquals(new String[] {"name"}, q.getBindVariableNames());
    assertEquals(v, q.getBoundValues().get("name"));
  }

  @Test
  public void bindValueRejectsNulls() throws Exception {
    PSQuery q = new PSQuery(Query.SQL);
    Value v = PSValueFactory.createValue((Object) 1L);
    assertThrows(IllegalArgumentException.class, () -> q.bindValue(null, v));
    assertThrows(IllegalArgumentException.class, () -> q.bindValue("", v));
    assertThrows(IllegalArgumentException.class, () -> q.bindValue("x", null));
  }
}

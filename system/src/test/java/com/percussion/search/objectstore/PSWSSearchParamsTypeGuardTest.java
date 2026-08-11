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
package com.percussion.search.objectstore;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Regression: typed setters must still reject wrong element types from raw collections with {@link
 * IllegalArgumentException} (not ClassCastException), matching pre-generics defensive contracts
 * (#2386 review).
 */
public class PSWSSearchParamsTypeGuardTest {

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void setSearchFieldsRejectsNonPsWsSearchField() {
    PSWSSearchParams params = new PSWSSearchParams();
    List raw = new ArrayList();
    raw.add("not-a-search-field");
    assertThrows(IllegalArgumentException.class, () -> params.setSearchFields(raw));
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void setResultFieldsRejectsNonString() {
    PSWSSearchParams params = new PSWSSearchParams();
    Collection raw = new ArrayList();
    raw.add(Integer.valueOf(42));
    assertThrows(IllegalArgumentException.class, () -> params.setResultFields(raw));
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void setPropertiesRejectsNonStringKeyOrValue() {
    PSWSSearchParams params = new PSWSSearchParams();
    Map rawKey = new HashMap();
    rawKey.put(Integer.valueOf(1), "v");
    assertThrows(IllegalArgumentException.class, () -> params.setProperties(rawKey));

    Map rawVal = new HashMap();
    rawVal.put("k", Integer.valueOf(2));
    assertThrows(IllegalArgumentException.class, () -> params.setProperties(rawVal));
  }
}

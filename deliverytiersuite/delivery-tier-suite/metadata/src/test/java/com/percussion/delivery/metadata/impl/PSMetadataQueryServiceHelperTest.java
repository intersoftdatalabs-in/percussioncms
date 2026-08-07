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
package com.percussion.delivery.metadata.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.percussion.delivery.metadata.IPSMetadataProperty.VALUETYPE;
import com.percussion.delivery.metadata.utils.PSHashCalculator;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** Covers {@link PSMetadataQueryServiceHelper#parseToList} number parsing without deprecated APIs. */
public class PSMetadataQueryServiceHelperTest {

  @Test
  public void parseToListParsesNumberCsv() throws Exception {
    Properties props = new Properties();
    props.setProperty("propValue", "NUMBER");
    PSPropertyDatatypeMappings mappings = new PSPropertyDatatypeMappings();
    mappings.setDatatypeMappings(props);

    List<Object> values =
        PSMetadataQueryServiceHelper.parseToList(
            "propValue", "1.5,2,3.25", mappings, new PSHashCalculator());

    assertEquals(3, values.size());
    assertInstanceOf(Double.class, values.get(0));
    assertEquals(1.5d, (Double) values.get(0), 0.0001d);
    assertEquals(2.0d, (Double) values.get(1), 0.0001d);
    assertEquals(3.25d, (Double) values.get(2), 0.0001d);
  }
}

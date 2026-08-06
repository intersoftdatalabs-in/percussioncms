/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.delivery.test.utils.properties;

import com.percussion.delivery.utils.properties.PSPropertyDefinition;
import com.percussion.delivery.utils.properties.PSPropertyGroupDefinition;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author natechadwick
 */
public class PSPropertyGroupTest {

  @Test
  public void testJSON() {
    PSPropertyGroupDefinition basic = new PSPropertyGroupDefinition();
    basic.setDisplayName("Basic");
    basic.setName("basic");
    basic.setExpanded(true);
    basic.setHelpText("Standard options for the Amakai Cache Manager Plugin.");

    PSPropertyDefinition p1 = new PSPropertyDefinition();
    p1.setDatatype("string");
    p1.setDefaultValue("test default");
    p1.setDisplayName("P1 Test");
    p1.setName("p1");
    basic.getProperties().add(p1);

    PSPropertyDefinition p2 = new PSPropertyDefinition();
    p2.setDatatype("string");
    p2.setDefaultValue("test default");
    p2.setDisplayName("P2 Test");
    p2.setName("p2");
    basic.getProperties().add(p2);

    var mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
    System.out.print(mapper.writeValueAsString(basic));
  }
}

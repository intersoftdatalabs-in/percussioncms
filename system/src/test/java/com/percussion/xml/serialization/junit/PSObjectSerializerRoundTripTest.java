/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.xml.serialization.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.assembly.IPSAssemblyTemplate.AAType;
import com.percussion.services.assembly.IPSAssemblyTemplate.GlobalTemplateUsage;
import com.percussion.services.assembly.IPSAssemblyTemplate.OutputFormat;
import com.percussion.services.assembly.IPSAssemblyTemplate.PublishWhen;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.assembly.data.PSTemplateBinding;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.xml.serialization.PSObjectSerializer;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Offline round-trip coverage for design objects through {@link PSObjectSerializer} under the
 * Jackson-backed helper (issue #1893 / parent #1823 / epic #505).
 *
 * <p><strong>No live CMS</strong> — earlier versions loaded real slots via {@code
 * PSAssemblyServiceLocator}; that required a running server and was disabled. This suite builds
 * in-memory templates only.
 *
 * <p>Domain-specific golden / package-fixture coverage for assembly lives in #1891 (and peers).
 * This class only asserts behavioral equality and the approved no-graph-id wire deviation.
 *
 * <p><strong>Approved deviations:</strong> no Betwixt {@code id="…"} graph-identity attributes;
 * unannotated property dumps may include derived {@code *-optional} / catalog alias fields until
 * domain annotation slices land.
 *
 * @author dougrand
 */
public class PSObjectSerializerRoundTripTest {

  private static final Pattern BETWIXT_GRAPH_ID_ATTR =
      Pattern.compile("\\sid\\s*=\\s*\"\\d+\"", Pattern.CASE_INSENSITIVE);

  @BeforeAll
  static void registerTypes() {
    // Jackson is the sole XML engine after #2062 (Betwixt purge).
    PSXmlSerializationHelper.addType("assembly-template", PSAssemblyTemplate.class);
  }

  /**
   * Round trip an assembly template with every scalar field and bindings set. Does not attach live
   * slots (offline).
   *
   * @throws Exception on serialize/deserialize failure
   */
  @Test
  public void testRoundTripTemplate() throws Exception {
    PSAssemblyTemplate template = setupTemplate();

    PSObjectSerializer ser = PSObjectSerializer.getInstance();
    String str = ser.toXmlString(template);

    assertTrue(str.contains("<assembly-template") || str.contains("<assembly-template>"), str);
    assertFalse(
        BETWIXT_GRAPH_ID_ATTR.matcher(str).find(),
        "Jackson must not emit Betwixt graph-identity id attributes: " + str);
    assertFalse(str.trim().startsWith("<null"), str);

    PSAssemblyTemplate restore = (PSAssemblyTemplate) ser.fromXmlString(str);

    assertEquals(template, restore);
    assertEquals(template.getName(), restore.getName());
    assertEquals(template.getLabel(), restore.getLabel());
    assertEquals(template.getAssembler(), restore.getAssembler());
    assertEquals(template.getActiveAssemblyType(), restore.getActiveAssemblyType());
    assertEquals(template.getOutputFormat(), restore.getOutputFormat());
    assertEquals(template.getPublishWhen(), restore.getPublishWhen());
    assertEquals(template.getGlobalTemplateUsage(), restore.getGlobalTemplateUsage());
    assertEquals(template.getGUID().toString(), restore.getGUID().toString());
    assertEquals(2, restore.getBindings().size());
    assertEquals("a", restore.getBindings().get(0).getVariable());
    assertEquals("1+2", restore.getBindings().get(0).getExpression());
  }

  /** Create a fully populated in-memory template for the test (no assembly service). */
  private PSAssemblyTemplate setupTemplate() {
    PSAssemblyTemplate template = new PSAssemblyTemplate();
    template.setName("test_template_0");
    template.setLabel("test template 0");
    template.setDescription("desc for tt0");
    template.setActiveAssemblyType(AAType.NonHtml);
    template.setAssembler("invalid assembler 0");
    template.setAssemblyUrl("../assembler/random_0");
    template.setCharset("invalid_charset");
    template.setGlobalTemplate(new PSGuid(PSTypeEnum.TEMPLATE, 1101));
    template.setGlobalTemplateUsage(GlobalTemplateUsage.Defined);
    template.setGUID(new PSGuid(PSTypeEnum.TEMPLATE, 1102));
    template.setLocationPrefix("foo_");
    template.setLocationSuffix("_bar");
    template.setOutputFormat(OutputFormat.Page);
    template.setPublishWhen(PublishWhen.Never);
    template.setStyleSheetPath("some invalid stylesheet path");
    template.addBinding(new PSTemplateBinding(1, "a", "1+2"));
    template.addBinding(new PSTemplateBinding(2, "b", "2*2"));
    return template;
  }
}

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

package com.percussion.services.utils.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Facade regression for {@link PSXmlSerializationHelper} after the Jackson-only cutover (#1887 /
 * #2062 / epic #505). Covers legacy {@code <null>} root rewrite, golden pilot shapes, round-trip,
 * and suppress annotation.
 */
class PSXmlSerializationHelperTest {

  @BeforeAll
  static void registerChoiceTypes() {
    PSXmlSerializationHelper.addType(
        "choice", PSJacksonXmlSerializationHelperTest.SampleChoice.class);
    PSXmlSerializationHelper.addType(
        "sample-choice", PSJacksonXmlSerializationHelperTest.SampleChoice.class);
    PSXmlSerializationHelper.addType(PSJacksonXmlSerializationHelperTest.SampleKeyword.class);
  }

  /** Minimal unannotated bean for rewrite-only cases (no nested collection assert). */
  public static class SampleKeyword {
    private long id;
    private String label;
    private String value;
    private List<SampleChoice> choices = new ArrayList<>();

    public long getId() {
      return id;
    }

    public void setId(long id) {
      this.id = id;
    }

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public List<SampleChoice> getChoices() {
      return choices;
    }

    public void setChoices(List<SampleChoice> choices) {
      this.choices = choices;
    }
  }

  public static class SampleChoice {
    private long id;
    private String label;
    private String value;
    private int sequence;

    public long getId() {
      return id;
    }

    public void setId(long id) {
      this.id = id;
    }

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public int getSequence() {
      return sequence;
    }

    public void setSequence(int sequence) {
      this.sequence = sequence;
    }
  }

  @Test
  void rewriteLegacyNullRootMapsToTypeElementName() {
    String xml =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null id="1">
          <label>Time_Zones</label>
        </null>
        """;
    String rewritten = PSXmlSerializationHelper.rewriteLegacyNullRoot(xml, SampleKeyword.class);
    assertTrue(rewritten.contains("<sample-keyword"), "open tag should be mapped type name");
    assertTrue(rewritten.contains("</sample-keyword>"), "close tag should be mapped type name");
    assertTrue(!rewritten.contains("<null"), "legacy null root open tag removed");
  }

  /**
   * Shipping package keyword files put the XML declaration and {@code <null>} root on one line with
   * intervening spaces (see perc.Baseline Time_Zones.keyword).
   */
  @Test
  void rewriteLegacyNullRootHandlesSameLineDeclarationAndRoot() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>  <null id=\"1\">\n"
            + "    <label>Time_Zones</label>\n"
            + "    <value>401</value>\n"
            + "  </null>\n";
    String rewritten = PSXmlSerializationHelper.rewriteLegacyNullRoot(xml, SampleKeyword.class);
    assertTrue(rewritten.contains("<sample-keyword id=\"1\">"), rewritten);
    assertTrue(rewritten.contains("</sample-keyword>"), rewritten);
    assertTrue(!rewritten.contains("<null"), rewritten);
  }

  @Test
  void rewriteLegacyNullRootLeavesNonNullRootUnchanged() {
    String xml =
        "<?xml version=\"1.0\"?><sample-keyword id=\"1\"><label>X</label></sample-keyword>";
    assertEquals(xml, PSXmlSerializationHelper.rewriteLegacyNullRoot(xml, SampleKeyword.class));
  }

  @Test
  void rewriteLegacyNullRootIsCaseSensitive() {
    String xml = "<?xml version=\"1.0\"?><Null id=\"1\"><label>X</label></Null>";
    assertEquals(xml, PSXmlSerializationHelper.rewriteLegacyNullRoot(xml, SampleKeyword.class));
  }

  @Test
  void rewriteLegacyNullRootAllowsTrailingCommentAfterClose() {
    String xml =
        "<?xml version=\"1.0\"?><null id=\"1\"><label>Time_Zones</label></null><!-- trailing -->\n";
    String rewritten = PSXmlSerializationHelper.rewriteLegacyNullRoot(xml, SampleKeyword.class);
    assertTrue(rewritten.contains("<sample-keyword"), rewritten);
    assertTrue(rewritten.contains("</sample-keyword>"), rewritten);
    assertTrue(rewritten.contains("<!-- trailing -->"), rewritten);
    assertTrue(!rewritten.contains("</null>"), rewritten);
  }

  @Test
  void readFromXmlAcceptsLegacyNullRootElement() throws Exception {
    // Mirrors Time_Zones.keyword / other packaged keywords (root element is literally "null")
    // Register nested choice element name used in package XML (collection singular "choice").
    PSXmlSerializationHelper.addType("choice", SampleChoice.class);

    String xml =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null id="1">
          <choices>
            <choice id="2">
              <label>ACT</label>
              <sequence>1</sequence>
              <value>ACT</value>
            </choice>
          </choices>
          <label>Time_Zones</label>
          <value>401</value>
        </null>
        """;

    SampleKeyword target = new SampleKeyword();
    Object restored = PSXmlSerializationHelper.readFromXML(xml, target);
    assertNotNull(restored);
    assertTrue(restored instanceof SampleKeyword);
    SampleKeyword keyword = (SampleKeyword) restored;
    assertEquals("Time_Zones", keyword.getLabel());
    assertEquals("401", keyword.getValue());
    // Root bind is the regression under test; nested choices need type map (as above).
    assertNotNull(keyword.getChoices());
  }

  @Test
  void readFromXmlStillAcceptsMappedTypeRootElement() throws Exception {
    // Round-trip write produces a mapped root (not "null"); must still parse
    SampleKeyword original = new SampleKeyword();
    original.setId(7);
    original.setLabel("Demo");
    original.setValue("7");

    String xml = PSXmlSerializationHelper.writeToXml(original);
    assertNotNull(xml);
    assertTrue(!xml.trim().startsWith("<null"), "write path should not emit legacy null root");

    SampleKeyword target = new SampleKeyword();
    SampleKeyword restored = (SampleKeyword) PSXmlSerializationHelper.readFromXML(xml, target);
    assertEquals("Demo", restored.getLabel());
    assertEquals("7", restored.getValue());
  }

  @Test
  void facadeWriteMatchesGoldenFixture() throws Exception {
    // Use Jackson pilot DTO (simple name SampleKeyword → root sample-keyword).
    PSJacksonXmlSerializationHelperTest.SampleKeyword original = jacksonPilotKeyword();
    String facadeXml = PSXmlSerializationHelper.writeToXml(original);
    String golden = loadResource("com/percussion/services/utils/xml/sample-keyword-golden.xml");
    PSJacksonXmlSerializationHelperTest.assertLogicalXmlParity(golden, facadeXml);
  }

  @Test
  void facadeJacksonRoundTripRestoresNestedChoices() throws Exception {
    PSJacksonXmlSerializationHelperTest.SampleKeyword original = jacksonPilotKeyword();
    String xml = PSXmlSerializationHelper.writeToXml(original);
    assertTrue(xml.contains("sample-keyword"), xml);
    assertTrue(xml.contains("sample-choice"), xml);

    PSJacksonXmlSerializationHelperTest.SampleKeyword target =
        new PSJacksonXmlSerializationHelperTest.SampleKeyword();
    PSJacksonXmlSerializationHelperTest.SampleKeyword restored =
        (PSJacksonXmlSerializationHelperTest.SampleKeyword)
            PSXmlSerializationHelper.readFromXML(xml, target);
    assertSame(target, restored);
    assertEquals(original.getId(), restored.getId());
    assertEquals(original.getLabel(), restored.getLabel());
    assertEquals(original.getValue(), restored.getValue());
    assertEquals(1, restored.getChoices().size());
    assertEquals("ACT", restored.getChoices().get(0).getLabel());
  }

  @Test
  void facadeReadAcceptsLegacyNullRootWithSampleChoiceItems() throws Exception {
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <choices>
            <sample-choice>
              <id>2</id>
              <label>ACT</label>
              <sequence>1</sequence>
              <value>ACT</value>
            </sample-choice>
          </choices>
          <id>1</id>
          <label>Time_Zones</label>
          <value>401</value>
        </null>
        """;

    PSJacksonXmlSerializationHelperTest.SampleKeyword restored =
        (PSJacksonXmlSerializationHelperTest.SampleKeyword)
            PSXmlSerializationHelper.readFromXML(
                legacy, new PSJacksonXmlSerializationHelperTest.SampleKeyword());
    assertEquals("Time_Zones", restored.getLabel());
    assertEquals("401", restored.getValue());
    assertEquals(1L, restored.getId());
    assertNotNull(restored.getChoices());
    assertEquals(1, restored.getChoices().size());
    assertEquals("ACT", restored.getChoices().get(0).getLabel());
  }

  @Test
  void facadeHonorsIpsXmlSerializationSuppress() throws Exception {
    PSJacksonXmlSerializationHelperTest.SampleKeyword original = jacksonPilotKeyword();
    original.setInternalOnly("secret-must-not-appear");
    String xml = PSXmlSerializationHelper.writeToXml(original);
    assertFalse(xml.contains("secret-must-not-appear"), xml);
    assertFalse(xml.contains("internal-only"), xml);
    assertFalse(xml.contains("internalOnly"), xml);
  }

  @Test
  void addTypeRegistersOnJacksonHelper() {
    class LocalMarker {
      // marker type for registry only
    }
    String name = "local-marker-" + System.nanoTime();
    PSXmlSerializationHelper.addType(name, LocalMarker.class);
    assertEquals(LocalMarker.class, PSJacksonXmlSerializationHelper.typeMapView().get(name));
  }

  private static PSJacksonXmlSerializationHelperTest.SampleKeyword jacksonPilotKeyword() {
    PSJacksonXmlSerializationHelperTest.SampleKeyword k =
        new PSJacksonXmlSerializationHelperTest.SampleKeyword();
    k.setId(1L);
    k.setLabel("Time_Zones");
    k.setValue("401");
    PSJacksonXmlSerializationHelperTest.SampleChoice c =
        new PSJacksonXmlSerializationHelperTest.SampleChoice();
    c.setId(2L);
    c.setLabel("ACT");
    c.setValue("ACT");
    c.setSequence(1);
    k.getChoices().add(c);
    return k;
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSXmlSerializationHelperTest.class.getClassLoader().getResourceAsStream(classpath)) {
      assertNotNull(in, "missing test resource: " + classpath);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}

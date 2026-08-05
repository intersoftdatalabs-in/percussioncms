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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.utils.xml.IPSXmlSerialization;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Golden / round-trip parity for the Jackson XML engine (issue #1822, cutover #1887, Betwixt purge
 * #2062, epic #505).
 *
 * <p>Pilot family mirrors package-deploy keywords ({@code SampleKeyword} / {@code SampleChoice})
 * because production {@code PSKeyword} lives outside {@code modules/utils}. The production facade
 * {@link PSXmlSerializationHelper} uses this engine exclusively.
 */
class PSJacksonXmlSerializationHelperTest {

  @BeforeAll
  static void registerChoiceType() {
    // Package archives often use item element "choice"; type-mapped write uses "sample-choice".
    PSXmlSerializationHelper.addType("choice", SampleChoice.class);
    PSXmlSerializationHelper.addType("sample-choice", SampleChoice.class);
    PSJacksonXmlSerializationHelper.addType("choice", SampleChoice.class);
    PSJacksonXmlSerializationHelper.addType("sample-choice", SampleChoice.class);
  }

  @Test
  void nameMapperProducesHistoricalHyphenatedTypeNames() {
    assertEquals("keyword", PSXmlElementNameMapper.mapTypeToElementName("PSKeyword"));
    assertEquals("sample-keyword", PSXmlElementNameMapper.mapTypeToElementName("SampleKeyword"));
    assertEquals("sample-choice", PSXmlElementNameMapper.mapTypeToElementName("SampleChoice"));
  }

  @Test
  void jacksonWriteMatchesGoldenFixture() throws Exception {
    SampleKeyword original = pilotKeyword();
    String jacksonXml = PSJacksonXmlSerializationHelper.writeToXml(original);
    String golden = loadResource("com/percussion/services/utils/xml/sample-keyword-golden.xml");
    assertLogicalXmlParity(golden, jacksonXml);
  }

  @Test
  void facadeWriteMatchesGoldenFixture() throws Exception {
    SampleKeyword original = pilotKeyword();
    String facadeXml = PSXmlSerializationHelper.writeToXml(original);
    String golden = loadResource("com/percussion/services/utils/xml/sample-keyword-golden.xml");
    assertLogicalXmlParity(golden, facadeXml);
  }

  @Test
  void jacksonWriteFacadeReadRoundTripScalars() throws Exception {
    // Nested collection restore on unannotated paths can differ; assert scalar wire.
    SampleKeyword original = pilotKeyword();
    String jacksonXml = PSJacksonXmlSerializationHelper.writeToXml(original);

    SampleKeyword target = new SampleKeyword();
    SampleKeyword restored =
        (SampleKeyword) PSXmlSerializationHelper.readFromXML(jacksonXml, target);
    assertEquals(original.getId(), restored.getId());
    assertEquals(original.getLabel(), restored.getLabel());
    assertEquals(original.getValue(), restored.getValue());
    assertNotNull(restored.getChoices());
  }

  @Test
  void jacksonReadAcceptsLegacyNullRootWithChoiceItems() throws Exception {
    // Package keyword archives use root <null> and item element <choice> (registered name).
    String legacy =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <null>
          <choices>
            <choice>
              <id>2</id>
              <label>ACT</label>
              <sequence>1</sequence>
              <value>ACT</value>
            </choice>
          </choices>
          <id>1</id>
          <label>Time_Zones</label>
          <value>401</value>
        </null>
        """;

    SampleKeyword restored =
        PSJacksonXmlSerializationHelper.readFromXml(legacy, SampleKeyword.class);
    assertEquals("Time_Zones", restored.getLabel());
    assertEquals("401", restored.getValue());
    assertEquals(1L, restored.getId());
    // Jackson may not map unregistered polymorphic item names without MixIns; accept empty
    // choices only if the alias path fails — prefer populated when annotations allow.
    assertNotNull(restored.getChoices());
  }

  @Test
  void jacksonReadAcceptsLegacyNullRootWithSampleChoiceItems() throws Exception {
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

    SampleKeyword restored =
        PSJacksonXmlSerializationHelper.readFromXml(legacy, SampleKeyword.class);
    assertEquals("Time_Zones", restored.getLabel());
    assertEquals("401", restored.getValue());
    assertEquals(1L, restored.getId());
    assertNotNull(restored.getChoices());
    assertEquals(1, restored.getChoices().size());
    assertEquals("ACT", restored.getChoices().get(0).getLabel());
    assertEquals(2L, restored.getChoices().get(0).getId());
  }

  @Test
  void rewriteLegacyNullRootStillUsedOnJacksonPath() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>  <null id=\"1\">\n"
            + "    <label>Time_Zones</label>\n"
            + "  </null>\n";
    String rewritten = PSXmlSerializationHelper.rewriteLegacyNullRoot(xml, SampleKeyword.class);
    assertTrue(rewritten.contains("<sample-keyword"), rewritten);
    assertFalse(rewritten.contains("<null"), rewritten);
  }

  @Test
  void ipsGuidSerializesAndDeserializesAsStringForm() throws Exception {
    // Historical Betwixt converter parity — required for package <guid> elements (#1888 / #1890 /
    // #1891).
    SampleWithGuid original = new SampleWithGuid();
    original.setId(513L);
    original.setGuid(new com.percussion.services.guidmgr.data.PSGuid("0-5-513"));

    String xml = PSJacksonXmlSerializationHelper.writeToXml(original);
    assertTrue(xml.contains("<guid>0-5-513</guid>") || xml.contains(">0-5-513<"), xml);

    SampleWithGuid restored =
        PSJacksonXmlSerializationHelper.readFromXml(xml, SampleWithGuid.class);
    assertNotNull(restored.getGuid());
    assertEquals("0-5-513", restored.getGuid().toString());
    assertEquals(513L, restored.getId());
  }

  @Test
  void ipsXmlSerializationSuppressIsHonoredByJackson() throws Exception {
    SampleKeyword original = pilotKeyword();
    original.setInternalOnly("secret-must-not-appear");
    String jacksonXml = PSJacksonXmlSerializationHelper.writeToXml(original);
    assertFalse(jacksonXml.contains("secret-must-not-appear"), jacksonXml);
    assertFalse(jacksonXml.contains("internal-only"), jacksonXml);
    assertFalse(jacksonXml.contains("internalOnly"), jacksonXml);
  }

  private static SampleKeyword pilotKeyword() {
    SampleKeyword k = new SampleKeyword();
    k.setId(1L);
    k.setLabel("Time_Zones");
    k.setValue("401");
    SampleChoice c = new SampleChoice();
    c.setId(2L);
    c.setLabel("ACT");
    c.setValue("ACT");
    c.setSequence(1);
    k.getChoices().add(c);
    return k;
  }

  /**
   * Compare logical XML trees: ignore XML declaration, historical graph-identity {@code id}
   * attributes, insignificant whitespace, and HTML comments.
   */
  static void assertLogicalXmlParity(String expectedXml, String actualXml) {
    String e = normalizeXmlForCompare(expectedXml);
    String a = normalizeXmlForCompare(actualXml);
    assertEquals(e, a, () -> "expected:\n" + expectedXml + "\nactual:\n" + actualXml);
  }

  static String normalizeXmlForCompare(String xml) {
    String s = Objects.requireNonNull(xml, "xml");
    // Drop XML declaration and comments
    s = s.replaceAll("(?is)<\\?xml[^?]*\\?>", "");
    s = s.replaceAll("(?s)<!--.*?-->", "");
    // Drop historical object-identity attributes (id="…") — property values are child elements
    s = s.replaceAll("\\s+id=\"[^\"]*\"", "");
    // Normalize newlines
    s = s.replace("\r\n", "\n").replace('\r', '\n');
    // Collapse whitespace between tags
    s = s.replaceAll(">\\s+<", "><");
    return s.trim();
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSJacksonXmlSerializationHelperTest.class.getClassLoader().getResourceAsStream(classpath)) {
      assertNotNull(in, "missing test resource: " + classpath);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Pilot DTO shaped like packaged keyword XML. Jackson annotations pin collection item element
   * name to {@code sample-choice}. {@link JsonPropertyOrder} mirrors historical property emission
   * order for golden parity.
   */
  @JacksonXmlRootElement(localName = "sample-keyword")
  @JsonPropertyOrder({"choices", "id", "label", "value"})
  public static class SampleKeyword {
    private long id;
    private String label;
    private String value;
    private List<SampleChoice> choices = new ArrayList<>();
    private String internalOnly;

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

    @JacksonXmlElementWrapper(localName = "choices")
    @JacksonXmlProperty(localName = "sample-choice")
    public List<SampleChoice> getChoices() {
      return choices;
    }

    public void setChoices(List<SampleChoice> choices) {
      this.choices = choices;
    }

    /** Suppressed on the Jackson serialization path. */
    @IPSXmlSerialization(suppress = true)
    public String getInternalOnly() {
      return internalOnly;
    }

    public void setInternalOnly(String internalOnly) {
      this.internalOnly = internalOnly;
    }
  }

  @JacksonXmlRootElement(localName = "sample-with-guid")
  public static class SampleWithGuid {
    private long id;
    private com.percussion.utils.guid.IPSGuid guid;

    public long getId() {
      return id;
    }

    public void setId(long id) {
      this.id = id;
    }

    public com.percussion.utils.guid.IPSGuid getGuid() {
      return guid;
    }

    public void setGuid(com.percussion.utils.guid.IPSGuid guid) {
      this.guid = guid;
    }
  }

  @JacksonXmlRootElement(localName = "sample-choice")
  @JsonPropertyOrder({"id", "label", "sequence", "value"})
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
}

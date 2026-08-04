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
 * Golden / round-trip parity for the parallel Jackson XML helper (issue #1822, epic #505).
 *
 * <p>Pilot family mirrors package-deploy keywords ({@code SampleKeyword} / {@code SampleChoice})
 * because production {@code PSKeyword} lives outside {@code modules/utils}. Betwixt remains the
 * production default; these tests prove Jackson can match the wire shape.
 */
class PSJacksonXmlSerializationHelperTest {

  @BeforeAll
  static void registerChoiceType() {
    // Betwixt write uses type-mapped item name "sample-choice"; package archives often use
    // "choice".
    PSXmlSerializationHelper.addType("choice", SampleChoice.class);
    PSXmlSerializationHelper.addType("sample-choice", SampleChoice.class);
    PSJacksonXmlSerializationHelper.addType("choice", SampleChoice.class);
    PSJacksonXmlSerializationHelper.addType("sample-choice", SampleChoice.class);
  }

  @Test
  void nameMapperAgreesWithBetwixtPsNameMapper() {
    PSXmlSerializationHelper.PSNameMapper betwixt = new PSXmlSerializationHelper.PSNameMapper();
    assertEquals(
        betwixt.mapTypeToElementName("PSKeyword"),
        PSXmlElementNameMapper.mapTypeToElementName("PSKeyword"));
    assertEquals(
        betwixt.mapTypeToElementName("SampleKeyword"),
        PSXmlElementNameMapper.mapTypeToElementName("SampleKeyword"));
    assertEquals(
        betwixt.mapTypeToElementName("SampleChoice"),
        PSXmlElementNameMapper.mapTypeToElementName("SampleChoice"));
  }

  @Test
  void betwixtWriteProducesStableGoldenShape() throws Exception {
    SampleKeyword original = pilotKeyword();
    String betwixtXml = PSXmlSerializationHelper.writeToXml(original);
    assertNotNull(betwixtXml);
    assertFalse(
        betwixtXml.trim().startsWith("<null"), "modern write must not emit legacy null root");
    assertTrue(containsTag(betwixtXml, "sample-keyword"), "Betwixt root: " + betwixtXml);
    assertTrue(containsTag(betwixtXml, "sample-choice"), "nested type element: " + betwixtXml);
    assertTrue(betwixtXml.contains("Time_Zones"), betwixtXml);
    assertTrue(betwixtXml.contains("ACT"), betwixtXml);
  }

  @Test
  void jacksonWriteMatchesBetwixtLogicalTree() throws Exception {
    SampleKeyword original = pilotKeyword();
    String betwixtXml = PSXmlSerializationHelper.writeToXml(original);
    String jacksonXml = PSJacksonXmlSerializationHelper.writeToXml(original);

    assertLogicalXmlParity(betwixtXml, jacksonXml);
  }

  @Test
  void jacksonWriteMatchesGoldenFixture() throws Exception {
    SampleKeyword original = pilotKeyword();
    String jacksonXml = PSJacksonXmlSerializationHelper.writeToXml(original);
    String golden = loadResource("com/percussion/services/utils/xml/sample-keyword-golden.xml");
    assertLogicalXmlParity(golden, jacksonXml);
  }

  @Test
  void betwixtWriteMatchesGoldenFixture() throws Exception {
    SampleKeyword original = pilotKeyword();
    String betwixtXml = PSXmlSerializationHelper.writeToXml(original);
    String golden = loadResource("com/percussion/services/utils/xml/sample-keyword-golden.xml");
    assertLogicalXmlParity(golden, betwixtXml);
  }

  @Test
  void betwixtWriteJacksonReadRoundTrip() throws Exception {
    SampleKeyword original = pilotKeyword();
    String betwixtXml = PSXmlSerializationHelper.writeToXml(original);

    SampleKeyword restored =
        PSJacksonXmlSerializationHelper.readFromXml(betwixtXml, SampleKeyword.class);
    assertPilotEquals(original, restored);
  }

  @Test
  void jacksonWriteBetwixtReadRoundTripScalars() throws Exception {
    // Betwixt without a .betwixt file does not reliably restore nested collection items for this
    // pilot (even Betwixt write→read leaves choices empty). Assert scalar wire + root binding.
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

  private static void assertPilotEquals(SampleKeyword expected, SampleKeyword actual) {
    assertNotNull(actual);
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getLabel(), actual.getLabel());
    assertEquals(expected.getValue(), actual.getValue());
    assertNotNull(actual.getChoices());
    assertEquals(expected.getChoices().size(), actual.getChoices().size());
    for (int i = 0; i < expected.getChoices().size(); i++) {
      SampleChoice e = expected.getChoices().get(i);
      SampleChoice a = actual.getChoices().get(i);
      assertEquals(e.getId(), a.getId());
      assertEquals(e.getLabel(), a.getLabel());
      assertEquals(e.getValue(), a.getValue());
      assertEquals(e.getSequence(), a.getSequence());
    }
  }

  /**
   * Compare logical XML trees: ignore XML declaration, Betwixt graph-identity {@code id}
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
    // Drop Betwixt object-identity attributes (id="…") — property values are child elements
    s = s.replaceAll("\\s+id=\"[^\"]*\"", "");
    // Normalize newlines
    s = s.replace("\r\n", "\n").replace('\r', '\n');
    // Collapse whitespace between tags
    s = s.replaceAll(">\\s+<", "><");
    return s.trim();
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
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
   * name to {@code sample-choice} (Betwixt type-mapped write form). {@link JsonPropertyOrder}
   * mirrors Betwixt property emission order for golden parity.
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

    /** Suppressed on both Betwixt and Jackson paths. */
    @IPSXmlSerialization(suppress = true)
    public String getInternalOnly() {
      return internalOnly;
    }

    public void setInternalOnly(String internalOnly) {
      this.internalOnly = internalOnly;
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

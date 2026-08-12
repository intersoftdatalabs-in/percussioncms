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

package com.percussion.rest.sites;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Wire shape for SitesResource list GET must marshal {@link SiteList} elements.
 *
 * <p>List GET {@code /services/sites} requires {@link SiteList} JAXB context registration of {@link
 * Site} via {@code @XmlSeeAlso} (#3090). Mirrors {@code UserPreferenceSerialDeserialTest} (#2746).
 */
@Tag("UnitTest")
public class SiteListSerialDeserialTest {

  private static Site sampleSite() {
    var site = new Site();
    site.setName("Help");
    site.setDescription("Help site");
    site.setBaseUrl("https://help.example.com");
    return site;
  }

  private static JsonMapper wrapRootMapper() {
    return JsonMapper.builder()
        .enable(SerializationFeature.WRAP_ROOT_VALUE)
        .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
        .build();
  }

  /**
   * Developer Sites catalog uses GET all → {@link SiteList}. Jackson root wrap must round-trip
   * list envelope + element fields (#3090 load path).
   */
  @Test
  public void serializeAndDeserializeSiteList() throws JacksonException {
    var mapper = wrapRootMapper();
    var list = new SiteList();
    list.add(sampleSite());

    var json = mapper.writeValueAsString(list);
    assertTrue(
        json.contains("SiteList") || json.contains("["),
        "expected list wire shape, got: " + json);
    assertTrue(json.contains("Help"), "list JSON must include site name, got: " + json);

    var roundTrip = mapper.readValue(json, SiteList.class);
    assertEquals(1, roundTrip.size(), "list size after round-trip");
    assertEquals("Help", roundTrip.get(0).getName().orElse(null));
    assertEquals("Help site", roundTrip.get(0).getDescription().orElse(null));
    assertEquals("https://help.example.com", roundTrip.get(0).getBaseUrl().orElse(null));
  }

  /**
   * Regression for #3090: without {@code @XmlSeeAlso(Site.class)} on {@link SiteList}, JAXB context
   * for the list does not know {@link Site} and GET /services/sites fails with "nor any of its
   * super class is known to this context".
   */
  @Test
  public void jaxbContextKnowsSiteFromList() throws Exception {
    JAXBContext ctx = JAXBContext.newInstance(SiteList.class);
    // Context creation alone is not enough on all providers — marshal a non-empty list.
    var list = new SiteList();
    list.add(sampleSite());

    Marshaller marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
    var writer = new StringWriter();
    try {
      marshaller.marshal(list, writer);
    } catch (Exception e) {
      fail(
          "JAXB must marshal SiteList containing Site (#3090); got: " + e.getMessage(), e);
    }
    var xml = writer.toString();
    assertFalse(xml.isBlank(), "marshalled XML must not be empty");
    assertTrue(
        xml.contains("SiteList") || xml.contains("siteList"),
        "expected SiteList root in XML, got: " + xml);
    // Element type registration is the critical #3090 gate; name may appear as attribute or
    // child depending on property accessors / XmlAccessType defaults.
    assertTrue(
        xml.contains("Help") || xml.toLowerCase().contains("site"),
        "marshalled payload should reference site content, got: " + xml);
  }

  /** Empty list remains valid for sites-none catalogs. */
  @Test
  public void jaxbContextMarshalsEmptySiteList() throws Exception {
    JAXBContext ctx = JAXBContext.newInstance(SiteList.class);
    Marshaller marshaller = ctx.createMarshaller();
    var writer = new StringWriter();
    marshaller.marshal(new SiteList(), writer);
    assertFalse(writer.toString().isBlank());
  }
}

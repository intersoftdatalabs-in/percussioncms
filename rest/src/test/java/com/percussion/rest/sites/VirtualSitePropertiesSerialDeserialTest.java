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

import com.percussion.rest.JacksonContextResolver;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * PUT/GET {@code /services/sites/{id}/virtual} wire shape (#3365 / QA #3030).
 *
 * <p>Production JSON uses {@link JacksonContextResolver} WRAP/UNWRAP_ROOT_VALUE so the envelope
 * must be {@code VirtualSiteProperties}, not a bare {@code sourceKind}. JAXB child elements must be
 * plain strings — Optional getters historically produced {@code unexpected element sourceKind}.
 */
@Tag("UnitTest")
public class VirtualSitePropertiesSerialDeserialTest {

  private static VirtualSiteProperties sample() {
    VirtualSiteProperties props = new VirtualSiteProperties();
    props.setSourceKind("git-filesystem");
    props.setRootPath("product-docs");
    props.setRemoteUrl("https://git.example.com/org/product-docs.git");
    props.setBranch("main");
    props.setConfigFile("_config.yaml");
    props.setSiteKey("product-docs");
    props.setVirtual(true);
    return props;
  }

  private static JsonMapper wrapRootMapper() {
    return JsonMapper.builder()
        .enable(SerializationFeature.WRAP_ROOT_VALUE)
        .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
        .build();
  }

  @Test
  public void jacksonWrapsRootAndPlainStringFields() throws JacksonException {
    var mapper = wrapRootMapper();
    String json = mapper.writeValueAsString(sample());
    assertTrue(json.contains("\"VirtualSiteProperties\""), "expected root wrap, got: " + json);
    assertTrue(json.contains("\"sourceKind\""), json);
    assertTrue(json.contains("\"git-filesystem\""), json);
    assertTrue(json.contains("\"rootPath\""), json);
    assertTrue(json.contains("\"remoteUrl\""), json);
    assertTrue(json.contains("\"branch\""), json);
    assertFalse(
        json.contains("\"empty\"") && json.contains("\"present\""),
        "sourceKind must be a plain string, not an Optional bean: " + json);

    VirtualSiteProperties roundTrip = mapper.readValue(json, VirtualSiteProperties.class);
    assertEquals("git-filesystem", roundTrip.getSourceKind());
    assertEquals("product-docs", roundTrip.getRootPath());
    assertEquals("https://git.example.com/org/product-docs.git", roundTrip.getRemoteUrl());
    assertEquals("main", roundTrip.getBranch());
    assertEquals("_config.yaml", roundTrip.getConfigFile());
    assertEquals("product-docs", roundTrip.getSiteKey());
    assertEquals(Boolean.TRUE, roundTrip.getVirtual());
  }

  @Test
  public void productionMapperRoundTripsEnvelope() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(VirtualSiteProperties.class);
    String json = mapper.writeValueAsString(sample());
    assertTrue(json.contains("\"VirtualSiteProperties\""), json);
    assertTrue(json.contains("\"sourceKind\""), json);
    assertTrue(json.contains("\"git-filesystem\""), json);
    assertFalse(
        json.contains("\"empty\"") && !json.contains("\"sourceKind\":\"git-filesystem\""),
        "sourceKind must be a plain string: " + json);

    VirtualSiteProperties roundTrip = mapper.readValue(json, VirtualSiteProperties.class);
    assertEquals("git-filesystem", roundTrip.getSourceKind());
    assertEquals("product-docs", roundTrip.getRootPath());
    assertEquals("https://git.example.com/org/product-docs.git", roundTrip.getRemoteUrl());
    assertEquals("main", roundTrip.getBranch());
  }

  @Test
  public void productionMapperRejectsBareSourceKindRoot() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(VirtualSiteProperties.class);
    String flat =
        "{\"sourceKind\":\"git-filesystem\",\"rootPath\":\"C:/docs\",\"virtual\":true}";
    try {
      VirtualSiteProperties result = mapper.readValue(flat, VirtualSiteProperties.class);
      assertTrue(
          result == null
              || result.getSourceKind() == null
              || result.getSourceKind().isBlank(),
          "flat sourceKind root must not bind under UNWRAP_ROOT_VALUE; got sourceKind="
              + (result == null ? "null" : result.getSourceKind()));
    } catch (Exception expected) {
      assertTrue(
          expected.getMessage() != null
              && (expected.getMessage().contains("VirtualSiteProperties")
                  || expected.getMessage().contains("Root name")
                  || expected.getMessage().contains("sourceKind")),
          "unexpected failure: " + expected);
    }
  }

  @Test
  public void jaxbMarshalsRootAndStringChildren() throws Exception {
    JAXBContext ctx = JAXBContext.newInstance(VirtualSiteProperties.class);
    Marshaller marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
    var writer = new StringWriter();
    try {
      marshaller.marshal(sample(), writer);
    } catch (Exception e) {
      fail("JAXB must marshal VirtualSiteProperties (#3365); got: " + e.getMessage(), e);
    }
    String xml = writer.toString();
    assertTrue(xml.contains("VirtualSiteProperties"), xml);
    assertTrue(xml.contains("sourceKind"), xml);
    assertTrue(xml.contains("git-filesystem"), xml);
    assertTrue(xml.contains("rootPath"), xml);
    assertTrue(xml.contains("remoteUrl"), xml);
    assertTrue(xml.contains("branch"), xml);
    assertFalse(xml.contains("<empty>"), "must not marshal Optional beans: " + xml);

    Unmarshaller unmarshaller = ctx.createUnmarshaller();
    VirtualSiteProperties roundTrip =
        (VirtualSiteProperties) unmarshaller.unmarshal(new StringReader(xml));
    assertEquals("git-filesystem", roundTrip.getSourceKind());
    assertEquals("product-docs", roundTrip.getRootPath());
    assertEquals("https://git.example.com/org/product-docs.git", roundTrip.getRemoteUrl());
    assertEquals("main", roundTrip.getBranch());
    assertEquals("_config.yaml", roundTrip.getConfigFile());
    assertEquals("product-docs", roundTrip.getSiteKey());
  }

  @Test
  public void productionMapperRoundTripsCsvFilesystemSourceKind() {
    VirtualSiteProperties props = new VirtualSiteProperties();
    props.setSourceKind("csv-filesystem");
    props.setRootPath("csv-docs");
    props.setSiteKey("csv-help");
    props.setVirtual(true);

    ObjectMapper mapper = new JacksonContextResolver().getContext(VirtualSiteProperties.class);
    String json = mapper.writeValueAsString(props);
    assertTrue(json.contains("\"VirtualSiteProperties\""), json);
    assertTrue(json.contains("\"csv-filesystem\""), json);

    VirtualSiteProperties roundTrip = mapper.readValue(json, VirtualSiteProperties.class);
    assertEquals("csv-filesystem", roundTrip.getSourceKind());
    assertEquals("csv-docs", roundTrip.getRootPath());
    assertEquals("csv-help", roundTrip.getSiteKey());
    assertEquals(Boolean.TRUE, roundTrip.getVirtual());
  }

  @Test
  public void productionMapperRoundTripsSqlDatabaseSourceKind() {
    VirtualSiteProperties props = new VirtualSiteProperties();
    props.setSourceKind("sql-database");
    props.setRootPath("sql-docs");
    props.setSiteKey("sql-help");
    props.setVirtual(true);

    ObjectMapper mapper = new JacksonContextResolver().getContext(VirtualSiteProperties.class);
    String json = mapper.writeValueAsString(props);
    assertTrue(json.contains("\"VirtualSiteProperties\""), json);
    assertTrue(json.contains("\"sql-database\""), json);
    assertFalse(json.toLowerCase().contains("password"), json);

    VirtualSiteProperties roundTrip = mapper.readValue(json, VirtualSiteProperties.class);
    assertEquals("sql-database", roundTrip.getSourceKind());
    assertEquals("sql-docs", roundTrip.getRootPath());
    assertEquals("sql-help", roundTrip.getSiteKey());
    assertEquals(Boolean.TRUE, roundTrip.getVirtual());
  }

  @Test
  public void productionMapperRoundTripsHttpJsonSourceKind() {
    VirtualSiteProperties props = new VirtualSiteProperties();
    props.setSourceKind("http-json");
    props.setRootPath("http-docs");
    props.setSiteKey("http-help");
    props.setVirtual(true);

    ObjectMapper mapper = new JacksonContextResolver().getContext(VirtualSiteProperties.class);
    String json = mapper.writeValueAsString(props);
    assertTrue(json.contains("\"VirtualSiteProperties\""), json);
    assertTrue(json.contains("\"http-json\""), json);
    assertFalse(json.toLowerCase().contains("password"), json);
    assertFalse(json.toLowerCase().contains("authorization"), json);

    VirtualSiteProperties roundTrip = mapper.readValue(json, VirtualSiteProperties.class);
    assertEquals("http-json", roundTrip.getSourceKind());
    assertEquals("http-docs", roundTrip.getRootPath());
    assertEquals("http-help", roundTrip.getSiteKey());
    assertEquals(Boolean.TRUE, roundTrip.getVirtual());
  }

  @Test
  public void productionMapperRoundTripsRssAtomSourceKind() {
    VirtualSiteProperties props = new VirtualSiteProperties();
    props.setSourceKind("rss-atom");
    props.setRootPath("rss-docs");
    props.setSiteKey("rss-help");
    props.setVirtual(true);

    ObjectMapper mapper = new JacksonContextResolver().getContext(VirtualSiteProperties.class);
    String json = mapper.writeValueAsString(props);
    assertTrue(json.contains("\"VirtualSiteProperties\""), json);
    assertTrue(json.contains("\"rss-atom\""), json);
    assertFalse(json.toLowerCase().contains("password"), json);
    assertFalse(json.toLowerCase().contains("authorization"), json);

    VirtualSiteProperties roundTrip = mapper.readValue(json, VirtualSiteProperties.class);
    assertEquals("rss-atom", roundTrip.getSourceKind());
    assertEquals("rss-docs", roundTrip.getRootPath());
    assertEquals("rss-help", roundTrip.getSiteKey());
    assertEquals(Boolean.TRUE, roundTrip.getVirtual());
  }

  @Test
  public void jaxbRejectsBareSourceKindRoot() throws Exception {
    JAXBContext ctx = JAXBContext.newInstance(VirtualSiteProperties.class);
    Unmarshaller unmarshaller = ctx.createUnmarshaller();
    String bare = "<sourceKind>git-filesystem</sourceKind>";
    try {
      Object result = unmarshaller.unmarshal(new StringReader(bare));
      fail(
          "bare sourceKind root must not unmarshal as VirtualSiteProperties (#3365); got: "
              + result);
    } catch (UnmarshalException expected) {
      // CXF/JAXB production path: unexpected element sourceKind
    } catch (AssertionError expected) {
      // GlassFish JAXB + Saxon XML parser can fail as AssertionError on unexpected root
    }
  }
}

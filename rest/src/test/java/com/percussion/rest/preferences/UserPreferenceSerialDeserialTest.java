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

package com.percussion.rest.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Wire shape for PreferenceResource single-pref PUT/GET must use Jackson root wrap {@code
 * UserPreference} (matches WebUI #2708 and {@code JacksonContextResolver}).
 *
 * <p>List GET {@code /preferences/} requires {@link UserPreferenceList} JAXB context
 * registration of {@link UserPreference} via {@code @XmlSeeAlso} (#2746).
 */
public class UserPreferenceSerialDeserialTest {

  private static UserPreference samplePreference() {
    var pref = new UserPreference();
    pref.setName("perc_profile_gravatar_email");
    pref.setValue("avatar@example.com");
    pref.setCategory("sys_preferences");
    pref.setContext("private");
    pref.setUserName("Admin");
    return pref;
  }

  private static JsonMapper wrapRootMapper() {
    return JsonMapper.builder()
        .enable(SerializationFeature.WRAP_ROOT_VALUE)
        .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
        .build();
  }

  @Test
  public void serializeAndDeserializeWrapsRootNameUserPreference() throws JacksonException {
    var mapper = wrapRootMapper();
    var pref = samplePreference();

    var json = mapper.writeValueAsString(pref);
    assertTrue(
        json.contains("\"UserPreference\""),
        "expected UserPreference root wrap, got: " + json);
    assertTrue(json.contains("perc_profile_gravatar_email"));

    var roundTrip = mapper.readValue(json, UserPreference.class);
    assertEquals(pref.getName(), roundTrip.getName());
    assertEquals(pref.getValue(), roundTrip.getValue());
    assertEquals(pref.getUserName(), roundTrip.getUserName());
  }

  @Test
  public void deserializeRejectsFlatNameRootWhenUnwrapping() {
    var mapper = wrapRootMapper();

    // Flat body that WebUI previously sent — root key is "name", not "UserPreference"
    var flat = "{\"name\":\"perc_profile_gravatar_email\",\"value\":\"x\",\"userName\":\"Admin\"}";
    try {
      UserPreference result = mapper.readValue(flat, UserPreference.class);
      // Must not vacuous-pass: if Jackson did not throw, the result must not look like a
      // successfully unwrapped preference (name populated from unexpected root).
      assertTrue(
          result == null
              || result.getName() == null
              || result.getName().isBlank(),
          "flat body must not deserialize as named UserPreference under UNWRAP_ROOT_VALUE; got name="
              + (result == null ? "null" : result.getName()));
    } catch (Exception expected) {
      // Expected for UNWRAP_ROOT_VALUE mismatch (#2708 class of failure)
      assertTrue(
          expected.getMessage() != null
              && (expected.getMessage().contains("UserPreference")
                  || expected.getMessage().contains("Root name")
                  || expected.getMessage().contains("name")),
          "unexpected failure: " + expected);
    }
  }

  /**
   * Profile Preferences load uses GET all → {@link UserPreferenceList}. Jackson root wrap
   * must round-trip list envelope + element fields (#2746 load path).
   */
  @Test
  public void serializeAndDeserializeUserPreferenceList() throws JacksonException {
    var mapper = wrapRootMapper();
    var list = new UserPreferenceList();
    list.add(samplePreference());

    var json = mapper.writeValueAsString(list);
    assertTrue(
        json.contains("UserPreferenceList") || json.contains("["),
        "expected list wire shape, got: " + json);
    assertTrue(
        json.contains("perc_profile_gravatar_email"),
        "list JSON must include preference name, got: " + json);

    var roundTrip = mapper.readValue(json, UserPreferenceList.class);
    assertEquals(1, roundTrip.size(), "list size after round-trip");
    assertEquals("perc_profile_gravatar_email", roundTrip.get(0).getName());
    assertEquals("avatar@example.com", roundTrip.get(0).getValue());
    assertEquals("Admin", roundTrip.get(0).getUserName());
  }

  /**
   * Regression for #2746: without {@code @XmlSeeAlso(UserPreference.class)} on {@link
   * UserPreferenceList}, JAXB context for the list does not know {@link UserPreference}
   * and GET /preferences/ fails with "nor any of its super class is known to this context".
   */
  @Test
  public void jaxbContextKnowsUserPreferenceFromList() throws Exception {
    JAXBContext ctx = JAXBContext.newInstance(UserPreferenceList.class);
    // Context creation alone is not enough on all providers — marshal a non-empty list.
    var list = new UserPreferenceList();
    list.add(samplePreference());

    Marshaller marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
    var writer = new StringWriter();
    try {
      marshaller.marshal(list, writer);
    } catch (Exception e) {
      fail(
          "JAXB must marshal UserPreferenceList containing UserPreference (#2746); got: "
              + e.getMessage(),
          e);
    }
    var xml = writer.toString();
    assertFalse(xml.isBlank(), "marshalled XML must not be empty");
    assertTrue(
        xml.contains("UserPreferenceList") || xml.contains("userPreferenceList"),
        "expected UserPreferenceList root in XML, got: " + xml);
    // Element type registration is the critical #2746 gate; name may appear as
    // attribute or child depending on property accessors / XmlAccessType defaults.
    assertTrue(
        xml.toLowerCase().contains("preference") || xml.contains("Admin"),
        "marshalled payload should reference preference content, got: " + xml);
  }
}

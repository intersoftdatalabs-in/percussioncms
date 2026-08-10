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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Wire shape for PreferenceResource single-pref PUT/GET must use Jackson root wrap {@code
 * UserPreference} (matches WebUI #2708 and {@code JacksonContextResolver}).
 */
public class UserPreferenceSerialDeserialTest {

  @Test
  public void serializeAndDeserializeWrapsRootNameUserPreference() throws JacksonException {
    var mapper =
        JsonMapper.builder()
            .enable(SerializationFeature.WRAP_ROOT_VALUE)
            .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .build();

    var pref = new UserPreference();
    pref.setName("perc_profile_gravatar_email");
    pref.setValue("avatar@example.com");
    pref.setCategory("sys_preferences");
    pref.setContext("private");
    pref.setUserName("Admin");

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
    var mapper =
        JsonMapper.builder()
            .enable(SerializationFeature.WRAP_ROOT_VALUE)
            .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .build();

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
}

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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.preferences.UserPreference;
import com.percussion.server.PSPersistentProperty;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral unit tests for {@link ApiUtils} user-preference conversion used by {@link
 * PreferencesAdaptor#loadPreference} / save (#2948).
 *
 * <p>Regression: {@code convertUserProperty} historically omitted {@code value}, so GET
 * {@code /preferences/{name}} returned empty payloads and Developer default ACL {@code
 * RUNTIME_VISIBLE} did not survive reload.
 */
@Tag("UnitTest")
public class ApiUtilsUserPreferenceConvertTest {

  /** Sample default ACL template JSON with RUNTIME_VISIBLE on Default (issue #2948). */
  private static final String DEFAULT_ACL_TEMPLATE_WITH_RUNTIME =
      "{\"version\":1,\"entries\":["
          + "{\"name\":\"Default\",\"type\":\"USER\",\"permissions\":"
          + "[\"READ\",\"UPDATE\",\"DELETE\",\"OWNER\",\"RUNTIME_VISIBLE\"]},"
          + "{\"name\":\"AnyCommunity\",\"type\":\"COMMUNITY\",\"permissions\":[\"RUNTIME_VISIBLE\"]}"
          + "]}";

  @Test
  public void convertUserPropertyPreservesValue() {
    PSPersistentProperty prop =
        new PSPersistentProperty(
            "admin",
            "developer.defaultObjectAclTemplate",
            "sys_preferences",
            "private",
            DEFAULT_ACL_TEMPLATE_WITH_RUNTIME);

    UserPreference up = ApiUtils.convertUserProperty(prop);

    assertNotNull(up);
    assertEquals("developer.defaultObjectAclTemplate", up.getName());
    assertEquals("admin", up.getUserName());
    assertEquals("sys_preferences", up.getCategory());
    assertEquals("private", up.getContext());
    assertEquals(DEFAULT_ACL_TEMPLATE_WITH_RUNTIME, up.getValue());
    assertTrue(
        up.getValue().contains("RUNTIME_VISIBLE"),
        "preference value must retain RUNTIME_VISIBLE for Default ACL template reload");
  }

  @Test
  public void convertUserPropertyMatchesConvertPSPersistentProperty() {
    PSPersistentProperty prop =
        new PSPersistentProperty(
            "editor",
            "some.pref",
            "sys_preferences",
            "private",
            "{\"flag\":true,\"nested\":\"value\"}");

    UserPreference viaLoad = ApiUtils.convertUserProperty(prop);
    UserPreference viaSave = ApiUtils.convertPSPersistentProperty(prop);

    assertEquals(viaSave.getName(), viaLoad.getName());
    assertEquals(viaSave.getUserName(), viaLoad.getUserName());
    assertEquals(viaSave.getCategory(), viaLoad.getCategory());
    assertEquals(viaSave.getContext(), viaLoad.getContext());
    assertEquals(viaSave.getValue(), viaLoad.getValue());
    assertEquals(viaSave.getExtraParam(), viaLoad.getExtraParam());
  }

  @Test
  public void convertUserPropertyEmptyValueIsEmptyStringNotNull() {
    PSPersistentProperty prop =
        new PSPersistentProperty("admin", "empty.pref", "sys_preferences", "private", null);

    UserPreference up = ApiUtils.convertUserProperty(prop);

    assertNotNull(up.getValue());
    assertEquals("", up.getValue());
  }
}

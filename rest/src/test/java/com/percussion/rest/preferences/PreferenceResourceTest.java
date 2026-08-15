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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** PreferenceResource missing-pref HTTP contract (#3458 / parent #2745). */
@Tag("UnitTest")
public class PreferenceResourceTest {

  private IPreferenceAdaptor adaptor;
  private PreferenceResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IPreferenceAdaptor.class);
    resource = new PreferenceResource(adaptor);
  }

  @Test
  public void getAllReturnsAdaptorList() {
    UserPreference pref = new UserPreference();
    pref.setName("developer.defaultObjectAclTemplate");
    pref.setValue("{}");
    UserPreferenceList list = new UserPreferenceList();
    list.add(pref);
    when(adaptor.getAllUserPreferences()).thenReturn(list);

    UserPreferenceList out = resource.getAllUserPreferences();
    assertEquals(1, out.size());
    assertEquals("developer.defaultObjectAclTemplate", out.get(0).getName());
    verify(adaptor).getAllUserPreferences();
  }

  @Test
  public void getAllReturnsEmptyListWhenAdaptorHasNone() {
    when(adaptor.getAllUserPreferences()).thenReturn(null);
    UserPreferenceList out = resource.getAllUserPreferences();
    assertNotNull(out);
    assertTrue(out.isEmpty());
  }

  @Test
  public void getAllReturnsEmptyListInsteadOf404() {
    when(adaptor.getAllUserPreferences()).thenThrow(new NotFoundException());
    UserPreferenceList out = resource.getAllUserPreferences();
    assertNotNull(out);
    assertTrue(out.isEmpty());
  }

  @Test
  public void getAllWrapsUnexpectedFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.getAllUserPreferences()).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getAllUserPreferences());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void loadPreferenceDelegatesWhenPresent() {
    UserPreference pref = new UserPreference();
    pref.setName("perc_profile_gravatar_email");
    pref.setValue("avatar@example.com");
    when(adaptor.loadPreference(eq("perc_profile_gravatar_email"))).thenReturn(pref);

    UserPreference out = resource.loadPreference("perc_profile_gravatar_email");
    assertEquals("avatar@example.com", out.getValue());
    verify(adaptor).loadPreference("perc_profile_gravatar_email");
  }

  @Test
  public void loadPreferenceReturnsEmptyValueInsteadOf404() {
    when(adaptor.loadPreference(eq("perc_profile_gravatar_email")))
        .thenThrow(new NotFoundException());

    UserPreference out = resource.loadPreference("perc_profile_gravatar_email");
    assertEquals("perc_profile_gravatar_email", out.getName());
    assertEquals("", out.getValue());
  }

  @Test
  public void loadPreferenceReturnsEmptyValueWhenAdaptorReturnsNull() {
    when(adaptor.loadPreference(eq("missing"))).thenReturn(null);
    UserPreference out = resource.loadPreference("missing");
    assertEquals("missing", out.getName());
    assertEquals("", out.getValue());
  }

  @Test
  public void emptyPreferenceUsesBlankNameForNull() {
    UserPreference out = PreferenceResource.emptyPreference(null);
    assertEquals("", out.getName());
    assertEquals("", out.getValue());
  }
}

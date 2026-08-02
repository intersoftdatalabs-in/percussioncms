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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.apibridge.mkd.MkdGcmCorrectionService;
import com.percussion.apibridge.mkd.MkdLanguageConfig;
import com.percussion.rest.i18n.I18nCorrectionResult;
import com.percussion.rest.i18n.I18nCorrectionSubmission;
import com.percussion.server.PSServer;
import com.percussion.util.PSProperties;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Gates and happy-path mapping with mocked GCM. Role membership is injectable so unit tests do not
 * need a live session.
 */
@Tag("UnitTest")
public class I18nCorrectionsAdaptorImplTest {

  private PSProperties previousProps;
  private Field propsField;

  @BeforeEach
  public void captureServerProps() throws Exception {
    propsField = PSServer.class.getDeclaredField("ms_serverProps");
    propsField.setAccessible(true);
    previousProps = (PSProperties) propsField.get(null);
    // Field type is PSProperties (extends java.util.Properties); plain Properties cannot be set.
    propsField.set(null, new PSProperties());
  }

  @AfterEach
  public void restoreServerProps() throws Exception {
    propsField.set(null, previousProps);
  }

  private void setProp(String key, String value) throws Exception {
    PSProperties p = (PSProperties) propsField.get(null);
    if (value == null) {
      p.remove(key);
    } else {
      p.setProperty(key, value);
    }
  }

  @Test
  public void rejectsMissingEmail() {
    MkdGcmCorrectionService gcm = mock(MkdGcmCorrectionService.class);
    I18nCorrectionsAdaptorImpl adaptor = new I18nCorrectionsAdaptorImpl(gcm);
    I18nCorrectionSubmission body = new I18nCorrectionSubmission();
    body.setLocale("en-us");
    assertThrows(IllegalArgumentException.class, () -> adaptor.submit(body));
  }

  @Test
  public void rejectsWhenDisabled() throws Exception {
    setProp(MkdLanguageConfig.PROP_ENABLED, "false");
    setProp(MkdLanguageConfig.PROP_ROLES, "*");
    MkdGcmCorrectionService gcm = mock(MkdGcmCorrectionService.class);
    I18nCorrectionsAdaptorImpl adaptor = new I18nCorrectionsAdaptorImpl(gcm);
    I18nCorrectionSubmission body = sampleBody();
    SecurityException ex = assertThrows(SecurityException.class, () -> adaptor.submit(body));
    assertTrue(ex.getMessage().toLowerCase().contains("disabled"));
  }

  @Test
  public void rejectsWhenRolesEmptyEvenIfEnabled() throws Exception {
    setProp(MkdLanguageConfig.PROP_ENABLED, "true");
    setProp(MkdLanguageConfig.PROP_ROLES, "");
    MkdGcmCorrectionService gcm = mock(MkdGcmCorrectionService.class);
    I18nCorrectionsAdaptorImpl adaptor = new I18nCorrectionsAdaptorImpl(gcm);
    assertThrows(SecurityException.class, () -> adaptor.submit(sampleBody()));
  }

  @Test
  public void configRoleGateHelpers() throws Exception {
    setProp(MkdLanguageConfig.PROP_ROLES, "");
    assertTrue(MkdLanguageConfig.rolesEmpty());
    setProp(MkdLanguageConfig.PROP_ROLES, "*");
    assertTrue(MkdLanguageConfig.rolesAllowAll());
    assertTrue(MkdLanguageConfig.userInAllowedRoles(List.of("Admin")));
    setProp(MkdLanguageConfig.PROP_ROLES, "Translations_Team, Admin");
    assertTrue(MkdLanguageConfig.userInAllowedRoles(List.of("Admin")));
    assertTrue(!MkdLanguageConfig.userInAllowedRoles(List.of("Editor")));
  }

  @Test
  public void mapsToOkWhenGcmReturns() throws Exception {
    MkdGcmCorrectionService gcm = mock(MkdGcmCorrectionService.class);
    when(gcm.postCorrection(any())).thenReturn("mid-xyz");
    setProp(MkdLanguageConfig.PROP_ENABLED, "true");
    setProp(MkdLanguageConfig.PROP_ROLES, "*");
    I18nCorrectionSubmission body = sampleBody();
    I18nCorrectionsAdaptorImpl adaptor =
        new I18nCorrectionsAdaptorImpl(gcm, () -> List.of("Admin"));

    I18nCorrectionResult r = adaptor.submit(body);
    assertEquals("ok", r.getStatus());
    assertEquals("mid-xyz", r.getGcmMessageId());
    verify(gcm).postCorrection(body);
  }

  @Test
  public void rejectsWhenUserNotInAllowedRoles() throws Exception {
    MkdGcmCorrectionService gcm = mock(MkdGcmCorrectionService.class);
    setProp(MkdLanguageConfig.PROP_ENABLED, "true");
    setProp(MkdLanguageConfig.PROP_ROLES, "Translations_Team");
    I18nCorrectionsAdaptorImpl adaptor =
        new I18nCorrectionsAdaptorImpl(gcm, () -> List.of("Editor"));
    SecurityException ex =
        assertThrows(SecurityException.class, () -> adaptor.submit(sampleBody()));
    assertTrue(ex.getMessage().toLowerCase().contains("role"));
  }

  @Test
  public void invalidGcmPortFallsBackToDefault() throws Exception {
    setProp(MkdLanguageConfig.PROP_GCM_PORT, "11199o");
    assertEquals(MkdLanguageConfig.DEFAULT_GCM_PORT, MkdLanguageConfig.gcmPort());
  }

  private static I18nCorrectionSubmission sampleBody() {
    I18nCorrectionSubmission body = new I18nCorrectionSubmission();
    body.setEmail("u@example.com");
    body.setLocale("en-us");
    body.setCurrentText("Hello");
    body.setProposedText("Hallo");
    body.setMessageId("perc.ui.test@Hello");
    return body;
  }
}

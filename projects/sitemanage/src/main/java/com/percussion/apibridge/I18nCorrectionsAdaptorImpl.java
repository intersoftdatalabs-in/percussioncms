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

import static com.percussion.webservices.PSWebserviceUtils.getUserRoles;

import com.percussion.apibridge.mkd.MkdGcmCorrectionService;
import com.percussion.apibridge.mkd.MkdLanguageConfig;
import com.percussion.rest.i18n.I18nCorrectionResult;
import com.percussion.rest.i18n.I18nCorrectionSubmission;
import com.percussion.rest.i18n.I18nCorrectionsAdaptor;
import com.percussion.system.utils.PSSiteManageBean;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Forwards browser language corrections to GCM when configured and role-gated. */
@PSSiteManageBean
public class I18nCorrectionsAdaptorImpl implements I18nCorrectionsAdaptor {

  private static final Logger log = LogManager.getLogger(I18nCorrectionsAdaptorImpl.class);

  private final MkdGcmCorrectionService gcmService;
  private boolean warnedEmptyRoles;

  public I18nCorrectionsAdaptorImpl() {
    this(new MkdGcmCorrectionService());
  }

  /** Test / DI constructor. */
  public I18nCorrectionsAdaptorImpl(MkdGcmCorrectionService gcmService) {
    this.gcmService = gcmService;
  }

  @Override
  public I18nCorrectionResult submit(I18nCorrectionSubmission submission) {
    if (submission == null) {
      throw new IllegalArgumentException("submission is required");
    }
    if (StringUtils.isBlank(submission.getEmail())) {
      throw new IllegalArgumentException("email is required");
    }
    if (StringUtils.isBlank(submission.getLocale())) {
      throw new IllegalArgumentException("locale is required");
    }

    if (!MkdLanguageConfig.isEnabled()) {
      throw new SecurityException("i18n corrections are disabled on this server");
    }

    if (MkdLanguageConfig.rolesEmpty()) {
      if (!warnedEmptyRoles) {
        MkdLanguageConfig.warnIfEnabledWithoutRoles();
        warnedEmptyRoles = true;
      }
      throw new SecurityException(
          "i18n corrections require perc.mkd.language.roles (e.g. Translations_Team or *)");
    }

    List<String> userRoles;
    try {
      userRoles = getUserRoles();
    } catch (Exception e) {
      log.debug("Could not resolve user roles for i18n correction gate", e);
      throw new SecurityException("Unable to resolve user roles for i18n corrections");
    }

    if (!MkdLanguageConfig.userInAllowedRoles(userRoles)) {
      throw new SecurityException("User is not in an allowed role for i18n corrections");
    }

    String mid = gcmService.postCorrection(submission);
    return I18nCorrectionResult.ok(mid);
  }
}

/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11

package com.percussion.apibridge;

import com.percussion.rest.errors.UnexpectedException;
import com.percussion.rest.preferences.IPreferenceAdaptor;
import com.percussion.rest.preferences.UserPreference;
import com.percussion.rest.preferences.UserPreferenceList;
import com.percussion.server.PSRequest;
import com.percussion.server.PSUserSession;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.system.utils.PSSiteManageBean;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/** Adaptor for user preferences management in Percussion CMS. */
@PSSiteManageBean
public class PreferencesAdaptor implements IPreferenceAdaptor {

  private static final Logger log = LogManager.getLogger(PreferencesAdaptor.class);

  @Autowired private IPSCmsObjectMgr objectMgr;

  @Override
  public UserPreferenceList getAllUserPreferences() {
    var session = getSession();
    var userPrefs =
        objectMgr.findPersistentPropertiesByName(session.getRealAuthenticatedUserEntry());
    return ApiUtils.convertUserProperties(userPrefs);
  }

  @Override
  public UserPreferenceList saveAllUserPreferences(UserPreferenceList prefs) {
    var session = getSession();
    try {
      for (var pref : prefs) {
        objectMgr.savePersistentPropertyMeta(ApiUtils.convertUserPreferenceToMeta(pref));
        objectMgr.savePersistentProperty(ApiUtils.convertUserPreference(pref));
      }
      return this.getAllUserPreferences();
    } catch (Exception e) {
      log.error("An error occurred when updating user preferences.", e);
      throw new UnexpectedException();
    }
  }

  @Override
  public UserPreference loadPreference(String preference) {
    var session = getSession();
    var userPrefs =
        objectMgr.findPersistentPropertiesByName(session.getRealAuthenticatedUserEntry());
    // convertPSPersistentProperty (via convertUserProperty) must copy PROPERTYVALUE —
    // omitting value made GET /preferences/{name} return empty payloads and dropped
    // Developer default ACL RUNTIME_VISIBLE on reload (#2948).
    return userPrefs.stream()
        .filter(p -> p.getName().equalsIgnoreCase(preference))
        .findFirst()
        .map(ApiUtils::convertUserProperty)
        .orElseThrow(NotFoundException::new);
  }

  @Override
  public UserPreference savePreference(UserPreference pref) {
    var session = getSession();
    var p = ApiUtils.convertUserPreference(pref);
    var pm = ApiUtils.convertUserPreferenceToMeta(pref);

    objectMgr.savePersistentPropertyMeta(pm);
    objectMgr.savePersistentProperty(p);

    return ApiUtils.convertPSPersistentProperty(p);
  }

  @Override
  public void deletePreference(UserPreference pref) {
    var session = getSession();
    if (session.getUserProperties().contains(pref)) {
      objectMgr.deletePersistentProperty(ApiUtils.convertUserPreference(pref));
    } else {
      throw new NotFoundException();
    }
  }

  private PSUserSession getSession() {
    PSRequest req = PSSecurityFilter.getCurrentRequest();
    return req.getUserSession();
  }
}

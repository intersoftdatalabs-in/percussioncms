/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.rest.preferences;

/**
 * Adaptor interface for user preference operations.
 * Sunny Sal: "Preferences ka adaptor, user ka comfort factor!"
 */
public interface IPreferenceAdaptor {

    /**
     * Gets all user preferences for the current user.
     *
     * @return list of user preferences
     */
    UserPreferenceList getAllUserPreferences();

    /**
     * Saves all user preferences for the current user.
     *
     * @param prefs preferences to save
     * @return updated list of user preferences
     */
    UserPreferenceList saveAllUserPreferences(UserPreferenceList prefs);

    /**
     * Loads a specific user preference by name.
     *
     * @param preference the preference name
     * @return the user preference
     */
    UserPreference loadPreference(String preference);

    /**
     * Saves a specific user preference.
     *
     * @param pref the preference to save
     * @return the saved preference
     */
    UserPreference savePreference(UserPreference pref);

    /**
     * Deletes a specific user preference.
     *
     * @param pref the preference to delete
     */
    void deletePreference(UserPreference pref);
}

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

/**
 * User profile hub catalog keys (slice 1 shell — #2393 / parent #2374).
 * English after {@code @} is the source fallback when TMX is not loaded.
 */
export const PROFILE_MSG = {
  MENU_MY_PROFILE: "perc.ui.profile.modern@My profile",
  TITLE: "perc.ui.profile.modern@My profile",
  INTRO:
    "perc.ui.profile.modern@View and manage your account settings. Sections below will open as they become available.",
  SECTIONS_NAV: "perc.ui.profile.modern@Profile sections",
  SECTION_ACCOUNT: "perc.ui.profile.modern@Account",
  SECTION_ACCOUNT_BODY:
    "perc.ui.profile.modern@Your name, login id, email, and role summary will appear here.",
  SECTION_SECURITY: "perc.ui.profile.modern@Security",
  SECTION_SECURITY_BODY:
    "perc.ui.profile.modern@Password and authentication options for your account will appear here.",
  SECTION_PREFERENCES: "perc.ui.profile.modern@Preferences",
  SECTION_PREFERENCES_BODY:
    "perc.ui.profile.modern@Choose your default CMS landing page and view preferences stored for your account.",
  SECTION_AVATAR: "perc.ui.profile.modern@Avatar",
  SECTION_AVATAR_BODY:
    "perc.ui.profile.modern@Avatar and Gravatar email settings will appear here.",
  COMING_SOON: "perc.ui.profile.modern@Coming soon",
  // Preferences section (#2396)
  PREF_LOADING: "perc.ui.profile.modern@Loading preferences…",
  PREF_LOAD_ERROR: "perc.ui.profile.modern@Could not load preferences.",
  PREF_RETRY: "perc.ui.profile.modern@Try again",
  PREF_LANDING_LABEL: "perc.ui.profile.modern@Default landing page",
  PREF_LANDING_HINT:
    "perc.ui.profile.modern@Where you land after sign-in. Leave as role default to use your role homepage.",
  PREF_STACK_NOTE:
    "perc.ui.profile.modern@Personal preferences use the product preference services already available for your account.",
  PREF_STORED_COUNT:
    "perc.ui.profile.modern@Stored preference entries: {0}",
  PREF_SAVE: "perc.ui.profile.modern@Save preferences",
  PREF_SAVING: "perc.ui.profile.modern@Saving…",
  PREF_SAVE_SUCCESS: "perc.ui.profile.modern@Preferences saved.",
  PREF_SAVE_ERROR: "perc.ui.profile.modern@Could not save preferences.",
} as const;

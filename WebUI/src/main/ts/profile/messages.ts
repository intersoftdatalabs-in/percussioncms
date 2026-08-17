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
 * User profile hub catalog keys (#2393 shell + #2395 account + #2396 preferences
 * + #2394 security — parent #2374).
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
    "perc.ui.profile.modern@Your login id, email, provider, and role summary for this account.",
  SECTION_SECURITY: "perc.ui.profile.modern@Security",
  SECTION_SECURITY_BODY:
    "perc.ui.profile.modern@Change your password when your account uses local authentication, or learn how directory and SSO accounts manage credentials.",
  SECTION_PREFERENCES: "perc.ui.profile.modern@Preferences",
  SECTION_PREFERENCES_BODY:
    "perc.ui.profile.modern@Choose your default CMS landing page and view preferences stored for your account.",
  SECTION_AVATAR: "perc.ui.profile.modern@Avatar",
  SECTION_AVATAR_BODY:
    "perc.ui.profile.modern@Choose the email used for your Gravatar avatar and preview how it appears in the header.",
  COMING_SOON: "perc.ui.profile.modern@Coming soon",

  // Security / password section (#2394)
  SECURITY_LOADING: "perc.ui.profile.modern@Loading security settings…",
  SECURITY_LOAD_ERROR:
    "perc.ui.profile.modern@Could not load security settings.",
  SECURITY_RETRY: "perc.ui.profile.modern@Try again",
  PW_NEW_LABEL: "perc.ui.profile.modern@New password",
  PW_CONFIRM_LABEL: "perc.ui.profile.modern@Confirm new password",
  PW_LENGTH_HINT:
    "perc.ui.profile.modern@Use at least {0} characters. Server password rules still apply.",
  PW_REQUIRED: "perc.ui.profile.modern@Enter a password.",
  PW_TOO_SHORT:
    "perc.ui.profile.modern@Password must be at least {0} characters.",
  PW_MISMATCH: "perc.ui.profile.modern@Passwords do not match.",
  PW_SUBMIT: "perc.ui.profile.modern@Change password",
  PW_SAVING: "perc.ui.profile.modern@Updating password…",
  PW_SAVE_SUCCESS: "perc.ui.profile.modern@Your password was changed.",
  PW_SAVE_ERROR:
    "perc.ui.profile.modern@Unable to change your password. Check the requirements and try again.",
  PW_EXTERNAL_TITLE:
    "perc.ui.profile.modern@Password managed outside Percussion",
  PW_EXTERNAL_BODY:
    "perc.ui.profile.modern@Your account uses directory or single sign-on authentication. Change your password with your identity provider or IT administrator — it cannot be updated from this profile.",

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

  // Account section (#2395)
  ACCOUNT_LOADING: "perc.ui.profile.modern@Loading account information…",
  ACCOUNT_LOAD_ERROR:
    "perc.ui.profile.modern@Unable to load your account information. Try again later.",
  ACCOUNT_RETRY: "perc.ui.profile.modern@Retry",
  FIELD_LOGIN_ID: "perc.ui.profile.modern@Login ID",
  FIELD_EMAIL: "perc.ui.profile.modern@Email",
  FIELD_PROVIDER: "perc.ui.profile.modern@Account type",
  FIELD_ROLES: "perc.ui.profile.modern@Roles",
  FIELD_COMMUNITIES: "perc.ui.profile.modern@Communities",
  FIELD_CURRENT_COMMUNITY: "perc.ui.profile.modern@Current community",
  FIELD_DEFAULT_COMMUNITY: "perc.ui.profile.modern@Default community",
  DEFAULT_COMMUNITY_HINT:
    "perc.ui.profile.modern@Used at the next sign-in when Remember last community is not selected. Only communities you can access are listed.",
  DEFAULT_COMMUNITY_NONE:
    "perc.ui.profile.modern@Use role default",
  DEFAULT_COMMUNITY_UNAVAILABLE:
    "perc.ui.profile.modern@No communities are assigned to your account.",
  SAVE_DEFAULT_COMMUNITY: "perc.ui.profile.modern@Save default community",
  DEFAULT_COMMUNITY_SAVE_SUCCESS:
    "perc.ui.profile.modern@Your default community was saved.",
  DEFAULT_COMMUNITY_SAVE_ERROR:
    "perc.ui.profile.modern@Unable to save your default community. Choose a community you can access.",
  DEFAULT_COMMUNITY_INVALID:
    "perc.ui.profile.modern@Choose a community from the list.",
  PROVIDER_INTERNAL: "perc.ui.profile.modern@Internal",
  PROVIDER_DIRECTORY: "perc.ui.profile.modern@Directory",
  READONLY_HINT:
    "perc.ui.profile.modern@This field is managed by your directory and cannot be changed here.",
  LOGIN_READONLY_HINT:
    "perc.ui.profile.modern@Your login ID cannot be changed from this page.",
  ROLES_READONLY_HINT:
    "perc.ui.profile.modern@Roles are assigned by an administrator and cannot be changed here.",
  COMMUNITIES_READONLY_HINT:
    "perc.ui.profile.modern@Communities are determined by your roles and cannot be changed here.",
  EMAIL_HELP_EDITABLE:
    "perc.ui.profile.modern@This email is stored with your CMS account and used for notifications.",
  EMAIL_HELP_DIRECTORY:
    "perc.ui.profile.modern@Email for directory accounts is managed by your organization.",
  SAVE_EMAIL: "perc.ui.profile.modern@Save email",
  SAVING: "perc.ui.profile.modern@Saving…",
  SAVE_SUCCESS: "perc.ui.profile.modern@Your email was saved.",
  SAVE_ERROR:
    "perc.ui.profile.modern@Unable to save your email. Check the address and try again.",
  EMAIL_INVALID: "perc.ui.profile.modern@Enter a valid email address.",
  EMPTY_VALUE: "perc.ui.profile.modern@Not set",
  NONE_LISTED: "perc.ui.profile.modern@None",

  // Avatar / Gravatar section (#2397)
  AVATAR_LOADING: "perc.ui.profile.modern@Loading avatar settings…",
  AVATAR_LOAD_ERROR: "perc.ui.profile.modern@Could not load avatar settings.",
  AVATAR_RETRY: "perc.ui.profile.modern@Try again",
  AVATAR_PREVIEW_LABEL: "perc.ui.profile.modern@Avatar preview",
  AVATAR_EMAIL_LABEL: "perc.ui.profile.modern@Gravatar email",
  AVATAR_EMAIL_HINT:
    "perc.ui.profile.modern@Used only to look up your public Gravatar image. Leave the box checked to use your primary account email.",
  AVATAR_USE_PRIMARY:
    "perc.ui.profile.modern@Use primary account email for Gravatar",
  AVATAR_PRIMARY_EMAIL: "perc.ui.profile.modern@Primary email: {0}",
  AVATAR_PRIMARY_MISSING:
    "perc.ui.profile.modern@No primary account email is stored yet. Enter a Gravatar email below or set your account email when available.",
  AVATAR_NO_EMAIL:
    "perc.ui.profile.modern@No email selected — initials will be shown.",
  AVATAR_PRIVACY_NOTE:
    "perc.ui.profile.modern@Privacy: the browser loads the avatar image from Gravatar using a one-way hash of the email. Your email is not shown to other users in the header.",
  AVATAR_EXTERNAL_DISABLED:
    "perc.ui.profile.modern@External avatar images are disabled on this server. Your initials are shown instead.",
  AVATAR_SSO_NOTE:
    "perc.ui.profile.modern@If you sign in with directory/SSO, your primary email may be managed by your identity provider. You can still set a separate Gravatar email here.",
  AVATAR_SAVE: "perc.ui.profile.modern@Save avatar settings",
  AVATAR_SAVING: "perc.ui.profile.modern@Saving…",
  AVATAR_SAVE_SUCCESS: "perc.ui.profile.modern@Avatar settings saved.",
  AVATAR_SAVE_ERROR: "perc.ui.profile.modern@Could not save avatar settings.",
  AVATAR_EMAIL_INVALID:
    "perc.ui.profile.modern@Enter a valid email address, or use your primary account email.",
  AVATAR_ARIA: "perc.ui.profile.modern@Avatar for {0}",
  AVATAR_DEFAULT_NAME: "perc.ui.profile.modern@user",
} as const;

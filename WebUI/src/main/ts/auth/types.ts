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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export type AuthView = "verify-email" | "forgot-username";

export interface AuthBootstrap {
  view: AuthView;
  token: string;
  authCsrfToken: string;
  csrfTokenName: string;
  csrfTokenValue: string;
  contextPath: string;
  locale: string;
  textDir: "ltr" | "rtl";
}

export interface VerifyEmailResponse {
  verified: boolean;
  status: string;
  message: string;
}

export interface ForgotUsernameResponse {
  accepted: boolean;
  message: string;
}

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

import {
  formatApiError,
  isApiError,
} from "../api/client";
import { message, MSG } from "../i18n/message";

/**
 * Map edition create/update failures to operator-visible text.
 * HTTP 400 → invalid name or missing site; HTTP 403 → forbidden; HTTP 409 → duplicate name.
 * Plain {@link ApiError} objects are not {@code Error} instances — do not use
 * {@code e.message} or the shell shows a generic failure (or nothing useful).
 */
export function mapEditionSaveError(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 409) {
      return formatApiError(err, message(MSG.PUBLISH_EDITION_NAME_CONFLICT));
    }
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_ERROR));
    }
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}

/**
 * Map edition copy failures to operator-visible text.
 * HTTP 400 → bad request; HTTP 403 → forbidden; HTTP 409 → duplicate name.
 * Plain {@link ApiError} objects are not {@code Error} instances — do not use
 * {@code e.message} or the shell shows a generic failure (or nothing useful).
 */
export function mapEditionCopyError(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 409) {
      return formatApiError(err, message(MSG.PUBLISH_EDITION_NAME_CONFLICT));
    }
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_ERROR));
    }
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}

/**
 * Map edition delete failures to operator-visible text.
 * HTTP 400 → bad edition id; HTTP 403 → forbidden; HTTP 409 → edition in use
 * (a publish job is still running). Plain {@link ApiError} objects are not
 * {@code Error} instances — do not use {@code e.message}.
 */
export function mapEditionDeleteError(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 409) {
      return formatApiError(err, "Edition is in use");
    }
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_ERROR));
    }
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}

/**
 * Map edition content-list association failures to operator-visible text.
 * HTTP 400 → missing ids; HTTP 403 → forbidden; HTTP 409 → already associated.
 * Plain {@link ApiError} objects are not {@code Error} instances — do not use
 * {@code e.message} or a failed associate looks like a generic miss.
 */
export function mapEditionContentListAssociateError(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 409) {
      return formatApiError(
        err,
        message(MSG.PUBLISH.DESIGN.EDITIONS.ASSOCIATE_CONFLICT),
      );
    }
    if (err.status === 400) {
      return formatApiError(
        err,
        message(MSG.PUBLISH.DESIGN.EDITIONS.ASSOCIATE_FAILED),
      );
    }
  }
  return formatApiError(
    err,
    message(MSG.PUBLISH.DESIGN.EDITIONS.ASSOCIATE_FAILED),
  );
}

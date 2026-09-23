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
package com.percussion.sitemanage.service.impl;

import com.percussion.share.service.exception.PSValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Locale;

/**
 * Maps site-copy failures onto HTTP 400, 403, or 409 for the Explorer wizard.
 * Operator-facing text is fixed; raw exception text is not copied into the response.
 */
public final class PSSiteCopyHttpStatus {

  private PSSiteCopyHttpStatus() {}

  public static Response.Status statusFor(Throwable error) {
    if (error == null) {
      return Response.Status.BAD_REQUEST;
    }
    String text = collect(error);
    if (isForbidden(text)) {
      return Response.Status.FORBIDDEN;
    }
    if (isConflict(text)) {
      return Response.Status.CONFLICT;
    }
    if (error instanceof PSValidationException || isBadRequest(text)) {
      return Response.Status.BAD_REQUEST;
    }
    return Response.Status.INTERNAL_SERVER_ERROR;
  }

  public static WebApplicationException toException(Throwable error) {
    Response.Status status = statusFor(error);
    return new WebApplicationException(messageFor(status), status);
  }

  static String messageFor(Response.Status status) {
    if (status == Response.Status.FORBIDDEN) {
      return "You are not authorized to copy a site.";
    }
    if (status == Response.Status.CONFLICT) {
      return "Site copy conflicts with an existing site or a copy in progress.";
    }
    if (status == Response.Status.BAD_REQUEST) {
      return "Site copy request was rejected.";
    }
    return "Site copy failed.";
  }

  private static boolean isForbidden(String text) {
    return text.contains("not authorized")
        || text.contains("do not have permission")
        || text.contains("forbidden")
        || text.contains("access denied");
  }

  private static boolean isConflict(String text) {
    return text.contains("already exists")
        || text.contains("being copied")
        || text.contains("currently being created")
        || text.contains("in progress");
  }

  private static boolean isBadRequest(String text) {
    return text.contains("unable to copy")
        || text.contains("failed to find")
        || text.contains("invalid")
        || text.contains("required")
        || text.contains("cannot be null")
        || text.contains("rejected");
  }

  private static String collect(Throwable error) {
    StringBuilder buf = new StringBuilder();
    Throwable cur = error;
    int depth = 0;
    while (cur != null && depth < 8) {
      if (cur.getMessage() != null) {
        buf.append(' ').append(cur.getMessage());
      }
      cur = cur.getCause();
      depth++;
    }
    return buf.toString().toLowerCase(Locale.ROOT);
  }
}

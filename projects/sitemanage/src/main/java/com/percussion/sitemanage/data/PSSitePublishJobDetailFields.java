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
package com.percussion.sitemanage.data;

import java.util.Locale;

/**
 * Optional Status detail fields copied onto {@link PSSitePublishJob}. Missing values stay null so
 * clients render empty text instead of failing.
 */
public final class PSSitePublishJobDetailFields {

  private PSSitePublishJobDetailFields() {}

  /** True when the job status text describes a failure (including completed-with-failures). */
  public static boolean isFailedJobStatus(String status) {
    if (status == null || status.isBlank()) {
      return false;
    }
    return status.toLowerCase(Locale.ROOT).contains("fail");
  }

  /** Blank edition names are omitted. */
  public static String editionNameOrNull(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    return name;
  }

  /**
   * {@code incremental} when the edition name is an incremental publish edition; {@code full}
   * otherwise. Blank names are omitted so the client can default.
   */
  public static String publishKindOrNull(String editionName) {
    if (editionName == null || editionName.isBlank()) {
      return null;
    }
    if (editionName.toUpperCase(Locale.ROOT).contains("INCREMENTAL")) {
      return "incremental";
    }
    return "full";
  }

  /**
   * Error text is exposed only for a failed job, and only when the publisher supplied a message.
   */
  public static String errorTextForFailedJob(String status, String message) {
    if (!isFailedJobStatus(status)) {
      return null;
    }
    if (message == null || message.isBlank()) {
      return null;
    }
    return message;
  }
}

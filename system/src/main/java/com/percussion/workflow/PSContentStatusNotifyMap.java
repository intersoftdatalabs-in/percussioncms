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
package com.percussion.workflow;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the 15-column CONTENTSTATUS change map used by {@link
 * PSContentStatusContext#commit()} for item-summary cache notify events.
 *
 * <p>Kept free of JDBC / Spring / {@link PSWorkFlowUtils} static initializers so unit
 * tests can pin the map shape without a live server or rxdeploydir.
 */
final class PSContentStatusNotifyMap {

  static final String CONTENTID = "CONTENTID";
  static final String CONTENTSTATEID = "CONTENTSTATEID";
  static final String CONTENTCHECKOUTUSERNAME = "CONTENTCHECKOUTUSERNAME";
  static final String CURRENTREVISION = "CURRENTREVISION";
  static final String EDITREVISION = "EDITREVISION";
  static final String TIPREVISION = "TIPREVISION";
  static final String REVISIONLOCK = "REVISIONLOCK";
  static final String LASTTRANSITIONDATE = "LASTTRANSITIONDATE";
  static final String STATEENTEREDDATE = "STATEENTEREDDATE";
  static final String NEXTAGINGTRANSITION = "NEXTAGINGTRANSITION";
  static final String NEXTAGINGDATE = "NEXTAGINGDATE";
  static final String CONTENTSTARTDATE = "CONTENTSTARTDATE";
  static final String CONTENTEXPIRYDATE = "CONTENTEXPIRYDATE";
  static final String REMINDERDATE = "REMINDERDATE";
  static final String REPEATEDAGINGTRANSSTARTDATE = "REPEATEDAGINGTRANSSTARTDATE";

  private PSContentStatusNotifyMap() {}

  /**
   * Same 15 columns the legacy {@code PSContentStatusContext.commit(Connection)} path put
   * into {@code notifyUpdateItem} via setInt/setString/setDate.
   *
   * @return unmodifiable ordered map, never {@code null}
   */
  static Map<String, String> build(
      int contentId,
      int stateId,
      String checkOutUserName,
      int currentRevision,
      int editRevision,
      int tipRevision,
      boolean revisionLocked,
      Date lastTransitionDate,
      Date stateEnteredDate,
      int nextAgingTransition,
      Date nextAgingDate,
      Date startDate,
      Date expiryDate,
      Date reminderDate,
      Date repeatedAgingStartDate) {
    Map<String, String> columns = new LinkedHashMap<>();
    String user = checkOutUserName == null ? "" : checkOutUserName;
    columns.put(CONTENTSTATEID, Integer.toString(stateId));
    columns.put(CONTENTCHECKOUTUSERNAME, user);
    columns.put(CURRENTREVISION, Integer.toString(currentRevision));
    columns.put(EDITREVISION, Integer.toString(editRevision));
    columns.put(TIPREVISION, Integer.toString(tipRevision));
    columns.put(REVISIONLOCK, revisionLocked ? "Y" : "N");
    columns.put(LASTTRANSITIONDATE, formatDate(lastTransitionDate));
    columns.put(STATEENTEREDDATE, formatDate(stateEnteredDate));
    columns.put(NEXTAGINGTRANSITION, Integer.toString(nextAgingTransition));
    columns.put(NEXTAGINGDATE, formatDate(nextAgingDate));
    columns.put(CONTENTSTARTDATE, formatDate(startDate));
    columns.put(CONTENTEXPIRYDATE, formatDate(expiryDate));
    columns.put(REMINDERDATE, formatDate(reminderDate));
    columns.put(REPEATEDAGINGTRANSSTARTDATE, formatDate(repeatedAgingStartDate));
    columns.put(CONTENTID, Integer.toString(contentId));
    return Collections.unmodifiableMap(columns);
  }

  /**
   * Legacy {@code mm/dd/yyyy hh:mm:ss:milli} shape (same as {@link
   * PSWorkFlowUtils#DateString(Date)} for non-null dates). Null dates stay {@code null} so
   * the map matches legacy {@code setDate} (which does not call DateString for null).
   */
  static String formatDate(Date date) {
    if (date == null) {
      return null;
    }
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    StringBuilder buf = new StringBuilder();
    buf.append((calendar.get(Calendar.MONTH) + 1)).append('/');
    buf.append(calendar.get(Calendar.DAY_OF_MONTH)).append('/');
    buf.append(calendar.get(Calendar.YEAR)).append(' ');
    buf.append(calendar.get(Calendar.HOUR)).append(':');
    buf.append(calendar.get(Calendar.MINUTE)).append(':');
    buf.append(calendar.get(Calendar.SECOND)).append(':');
    buf.append(calendar.get(Calendar.MILLISECOND));
    return buf.toString();
  }
}

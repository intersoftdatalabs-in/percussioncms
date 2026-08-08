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
package com.percussion.delivery.metadata.impl;

import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.data.PSMetadataDatedEntries;
import com.percussion.delivery.metadata.data.PSMetadataDatedEvent;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * This class is responsible for process the dates of the page and return the JSONObject with the
 * entries with their properties.
 *
 * @author rafaelsalis
 */
public class PSDatedEntriesHelper {

  /** No-arg constructor; the helper is stateless aside from its formatter instance. */
  public PSDatedEntriesHelper() {}

  /** Constants names for the page properties. */
  private static final String SUMMARY_PROPERTY_NAME = "dcterms:abstract";

  private static final String START_DATE_PROPERTY_NAME = "perc:start_date";
  private static final String END_DATE_PROPERTY_NAME = "perc:end_date";

  /** Date formatter used to render the start / end timestamps of the dated event payload. */
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  /**
   * Returns the dated-entries payload assembled from the supplied metadata index results:
   *
   * <ul>
   *   <li>page title
   *   <li>page summary
   *   <li>page start date
   *   <li>page end date
   *   <li>page url
   * </ul>
   *
   * @param results the metadata entries to process; assumed not {@code null}.
   * @return a populated {@link PSMetadataDatedEntries} object.
   * @throws Exception if the processing fails or the supplied results are invalid.
   */
  public PSMetadataDatedEntries getDatedEntries(List<IPSMetadataEntry> results) throws Exception {
    if (results == null) throw new IllegalArgumentException("Results can not be null");

    PSMetadataDatedEntries datedListResults = new PSMetadataDatedEntries();

    try {
      for (IPSMetadataEntry entryPage : results) {
        PSMetadataDatedEvent event = new PSMetadataDatedEvent();
        event.setTitle(entryPage.getLinktext());

        // Strip the site from the url
        String[] paths = entryPage.getPagepath().split("/");
        String pageUrl = StringUtils.EMPTY;
        for (int i = 2; i < paths.length; i++) {
          pageUrl = pageUrl + "/" + paths[i];
        }
        event.setUrl(pageUrl);

        for (IPSMetadataProperty prop : entryPage.getProperties()) {
          if (SUMMARY_PROPERTY_NAME.equals(prop.getName()) && !prop.getStringvalue().isEmpty()) {
            event.setSummary(prop.getStringvalue());
          }

          if (START_DATE_PROPERTY_NAME.equals(prop.getName()) && prop.getDatevalue() != null) {
            ZonedDateTime start =
                prop.getDatevalue().atZone(ZoneId.systemDefault());
            event.setStart(FORMATTER.format(start));
          }

          if (END_DATE_PROPERTY_NAME.equals(prop.getName()) && prop.getDatevalue() != null) {
            ZonedDateTime end = prop.getDatevalue().atZone(ZoneId.systemDefault());
            event.setEnd(FORMATTER.format(end));
          }
        }

        datedListResults.add(event);
      }
      return datedListResults;
    } catch (Exception e) {
      throw new Exception("Cannot get the list of entries within a specific range of dates.");
    }
  }
}

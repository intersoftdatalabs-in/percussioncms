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

package com.percussion.delivery.metadata.impl;

import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.data.PSMetadataBlogEntry;
import com.percussion.delivery.metadata.data.PSMetadataBlogMonth;
import com.percussion.delivery.metadata.data.PSMetadataBlogYear;
import com.percussion.delivery.metadata.data.PSMetadataRestBlogList;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Processes metadata entries to produce a list of blog posts organized by year and month.
 * @author leonardohildt
 */
public class PSBlogsHelper {

    public static final String BLOG_PROPERTY_NAME = "dcterms:created";

    /**
     * Returns a list of blog posts organized by year and month.
     * @param results entries containing the collection of metadata entries.
     * @return PSMetadataRestBlogList contains the list organized by year and month.
     */
    public PSMetadataRestBlogList getProcessedBlogs(List<IPSMetadataEntry> results) throws Exception {
        if (results == null) {
            throw new IllegalArgumentException("Results cannot be null");
        }

        var blogs = new PSMetadataBlogEntry();

        try {
            for (var entryPage : results) {
                for (var prop : entryPage.getProperties()) {
                    if (BLOG_PROPERTY_NAME.equals(prop.getName()) && !prop.getStringvalue().isEmpty()) {
                        var cal = Calendar.getInstance();
                        var currentDate = cal.getTime();
                        cal.setTime(prop.getDatevalue());
                        var pageDate = cal.getTime();

                        // Skip future-dated pages
                        if (pageDate.after(currentDate)) {
                            break;
                        }

                        var currentPostYear = cal.get(Calendar.YEAR);
                        var currentPostMonth = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());

                        var selectedYear = blogs.getYears().stream()
                                .filter(year -> year.getYear().equals(currentPostYear))
                                .findFirst()
                                .orElseGet(() -> {
                                    var newYear = new PSMetadataBlogYear(currentPostYear);
                                    blogs.getYears().add(newYear);
                                    return newYear;
                                });

                        var selectedMonth = selectedYear.getMonths().stream()
                                .filter(month -> month.getMonth().equals(currentPostMonth))
                                .findFirst()
                                .orElse(null);

                        if (selectedMonth != null) {
                            selectedYear.setYearCount(selectedYear.getYearCount() + 1);
                            selectedMonth.setCount(selectedMonth.getCount() + 1);
                        }
                    }
                }
            }

            var blogYearsList = new ArrayList<>(blogs.getYears());
            blogYearsList.sort(Comparator.comparing(PSMetadataBlogYear::getYear).reversed());

            var blogListResults = new PSMetadataRestBlogList();
            blogListResults.setYears(blogYearsList);
            return blogListResults;
        } catch (Exception e) {
            throw new Exception("Cannot get the list of blogs organized by year and months.", e);
        }
    }
}

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

package com.percussion.delivery.metadata.data;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang.ObjectUtils;

/**
 * Represents a year and the list of months with the number of posts for each month.
 * Each year has the total of posts for the given year.
 */
public class PSMetadataBlogYear {

    private Integer year;
    private Integer yearCount;
    private List<PSMetadataBlogMonth> months;

    public PSMetadataBlogYear(Integer year) {
        this.year = year;
        this.yearCount = 0;

        var cal = Calendar.getInstance();
        var currentYear = cal.get(Calendar.YEAR);
        var currentMonth = cal.get(Calendar.MONTH);

        var emptyMonths = new ArrayList<PSMetadataBlogMonth>();
        var localeMonths = new DateFormatSymbols(Locale.getDefault()).getMonths();
        var indexMonth = localeMonths.length - 2;
        if (currentYear.equals(year)) {
            indexMonth = currentMonth;
        }

        for (int i = indexMonth; i >= 0; i--) {
            emptyMonths.add(new PSMetadataBlogMonth(localeMonths[i], 0));
        }
        this.months = emptyMonths;
    }

    class MonthOrderBlogsComparator implements Comparator<PSMetadataBlogMonth> {
        @Override
        public int compare(PSMetadataBlogMonth o1, PSMetadataBlogMonth o2) {
            return o1.getMonth().compareTo(o2.getMonth());
        }
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setYearCount(Integer yearCount) {
        this.yearCount = yearCount;
    }

    public Integer getYearCount() {
        return yearCount;
    }

    public List<PSMetadataBlogMonth> getMonths() {
        return months;
    }

    public void setMonths(List<PSMetadataBlogMonth> months) {
        this.months = months;
    }

    public void addMonth(PSMetadataBlogMonth month) {
        this.months.add(month);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PSMetadataBlogYear)) {
            return false;
        }
        return ObjectUtils.equals(((PSMetadataBlogYear) obj).year, this.year);
    }
}

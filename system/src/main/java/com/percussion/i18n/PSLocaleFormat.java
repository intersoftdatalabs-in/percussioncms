/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.i18n;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Objects;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Locale format profile keyed by BCP-47 language string (not {@code LOCALEID}).
 *
 * <p>Customers may invent locales; format rows are optional. Resolution falls back
 * regional → language-only → {@code en-us} via {@link PSLocaleFormatResolver}.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSLocaleFormat")
@Table(name = "RXLOCALEFORMAT")
public class PSLocaleFormat {

  public static final String TEXT_DIR_LTR = "ltr";
  public static final String TEXT_DIR_RTL = "rtl";

  public static final String MEASUREMENT_US = "us";
  public static final String MEASUREMENT_UK = "uk";
  public static final String MEASUREMENT_METRIC = "metric";

  /** ISO day-of-week: 1 = Monday … 7 = Sunday. */
  public static final int FIRST_DAY_MONDAY = 1;

  public static final int FIRST_DAY_SUNDAY = 7;

  public PSLocaleFormat() {}

  public PSLocaleFormat(String languageString) {
    setLanguageString(languageString);
  }

  @Id
  @Column(name = "LANGUAGESTRING", length = 16)
  private String m_languageString;

  @Basic
  @Column(name = "TEXTDIR", length = 8)
  private String m_textDir;

  @Basic
  @Column(name = "DATEPATTERN", length = 64)
  private String m_datePattern;

  @Basic
  @Column(name = "TIMEPATTERN", length = 64)
  private String m_timePattern;

  @Basic
  @Column(name = "DATETIMEPATTERN", length = 128)
  private String m_dateTimePattern;

  @Basic
  @Column(name = "DECIMALSEP", length = 4)
  private String m_decimalSep;

  @Basic
  @Column(name = "GROUPINGSEP", length = 4)
  private String m_groupingSep;

  @Basic
  @Column(name = "CURRENCYCODE", length = 3)
  private String m_currencyCode;

  @Basic
  @Column(name = "CURRENCYPATTERN", length = 32)
  private String m_currencyPattern;

  /** 1=Monday … 7=Sunday; may be null to inherit. */
  @Basic
  @Column(name = "FIRSTDAYOFWEEK")
  private Integer m_firstDayOfWeek;

  @Basic
  @Column(name = "MEASUREMENTSYSTEM", length = 16)
  private String m_measurementSystem;

  @Basic
  @Column(name = "DEFAULTTZ", length = 64)
  private String m_defaultTz;

  @Basic
  @Column(name = "NUMBERINGSYSTEM", length = 16)
  private String m_numberingSystem;

  @Basic
  @Column(name = "CALENDAR", length = 32)
  private String m_calendar;

  @Version
  @Column(name = "VERSION")
  private Integer m_version = 0;

  public String getLanguageString() {
    return m_languageString;
  }

  public void setLanguageString(String languageString) {
    m_languageString =
        languageString == null
            ? null
            : languageString.trim().toLowerCase().replace('_', '-');
  }

  public String getTextDir() {
    return m_textDir;
  }

  public void setTextDir(String textDir) {
    m_textDir = emptyToNull(textDir);
  }

  public String getDatePattern() {
    return m_datePattern;
  }

  public void setDatePattern(String datePattern) {
    m_datePattern = emptyToNull(datePattern);
  }

  public String getTimePattern() {
    return m_timePattern;
  }

  public void setTimePattern(String timePattern) {
    m_timePattern = emptyToNull(timePattern);
  }

  public String getDateTimePattern() {
    return m_dateTimePattern;
  }

  public void setDateTimePattern(String dateTimePattern) {
    m_dateTimePattern = emptyToNull(dateTimePattern);
  }

  public String getDecimalSep() {
    return m_decimalSep;
  }

  public void setDecimalSep(String decimalSep) {
    m_decimalSep = emptyToNull(decimalSep);
  }

  public String getGroupingSep() {
    return m_groupingSep;
  }

  public void setGroupingSep(String groupingSep) {
    m_groupingSep = emptyToNull(groupingSep);
  }

  public String getCurrencyCode() {
    return m_currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    m_currencyCode = emptyToNull(currencyCode);
  }

  public String getCurrencyPattern() {
    return m_currencyPattern;
  }

  public void setCurrencyPattern(String currencyPattern) {
    m_currencyPattern = emptyToNull(currencyPattern);
  }

  public Integer getFirstDayOfWeek() {
    return m_firstDayOfWeek;
  }

  public void setFirstDayOfWeek(Integer firstDayOfWeek) {
    m_firstDayOfWeek = firstDayOfWeek;
  }

  public String getMeasurementSystem() {
    return m_measurementSystem;
  }

  public void setMeasurementSystem(String measurementSystem) {
    m_measurementSystem = emptyToNull(measurementSystem);
  }

  public String getDefaultTz() {
    return m_defaultTz;
  }

  public void setDefaultTz(String defaultTz) {
    m_defaultTz = emptyToNull(defaultTz);
  }

  public String getNumberingSystem() {
    return m_numberingSystem;
  }

  public void setNumberingSystem(String numberingSystem) {
    m_numberingSystem = emptyToNull(numberingSystem);
  }

  public String getCalendar() {
    return m_calendar;
  }

  public void setCalendar(String calendar) {
    m_calendar = emptyToNull(calendar);
  }

  public Integer getVersion() {
    return m_version;
  }

  public void setVersion(Integer version) {
    m_version = version;
  }

  /**
   * Copy into a JSON-friendly map of non-null fields (for login / SPA bootstrap).
   */
  public java.util.Map<String, Object> toBootstrapMap() {
    java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>();
    m.put("languageString", m_languageString);
    putIfPresent(m, "textDir", m_textDir);
    putIfPresent(m, "datePattern", m_datePattern);
    putIfPresent(m, "timePattern", m_timePattern);
    putIfPresent(m, "dateTimePattern", m_dateTimePattern);
    putIfPresent(m, "decimalSep", m_decimalSep);
    putIfPresent(m, "groupingSep", m_groupingSep);
    putIfPresent(m, "currencyCode", m_currencyCode);
    putIfPresent(m, "currencyPattern", m_currencyPattern);
    if (m_firstDayOfWeek != null) {
      m.put("firstDayOfWeek", m_firstDayOfWeek);
    }
    putIfPresent(m, "measurementSystem", m_measurementSystem);
    putIfPresent(m, "defaultTz", m_defaultTz);
    putIfPresent(m, "numberingSystem", m_numberingSystem);
    putIfPresent(m, "calendar", m_calendar);
    return m;
  }

  private static void putIfPresent(java.util.Map<String, Object> m, String k, String v) {
    if (v != null && !v.isEmpty()) {
      m.put(k, v);
    }
  }

  private static String emptyToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PSLocaleFormat)) {
      return false;
    }
    PSLocaleFormat that = (PSLocaleFormat) o;
    return Objects.equals(m_languageString, that.m_languageString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_languageString);
  }
}

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

package com.percussion.rest.locales;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional RXLOCALEFORMAT profile row keyed by BCP-47 language string (not LOCALEID).
 *
 * <p>Absent when no format row exists; runtime UI may still resolve via regional → base → en-us
 * defaults ({@code PSLocaleFormatResolver}).
 */
@XmlRootElement(name = "LocaleFormat")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Locale format profile (RXLOCALEFORMAT)")
public class LocaleFormatSummary {

  private String languageString;
  private String textDir;
  private String datePattern;
  private String timePattern;
  private String dateTimePattern;
  private String decimalSep;
  private String groupingSep;
  private String currencyCode;
  private String currencyPattern;
  private Integer firstDayOfWeek;
  /** True when JSON included {@code firstDayOfWeek} (including JSON null / blank to clear). */
  @JsonIgnore
  private boolean firstDayOfWeekPresent;
  private String measurementSystem;
  private String defaultTz;
  private String numberingSystem;
  private String calendar;

  /**
   * Ordered per-locale fallback chain (language strings). {@code null} when the client omits the
   * property; an empty list clears the stored chain. Stored on RXLOCALEFORMAT.FALLBACKCHAIN as
   * comma-separated codes.
   */
  private List<String> fallbackChain;

  public LocaleFormatSummary() {}

  public String getLanguageString() {
    return languageString;
  }

  public void setLanguageString(String languageString) {
    this.languageString = languageString;
  }

  public String getTextDir() {
    return textDir;
  }

  public void setTextDir(String textDir) {
    this.textDir = textDir;
  }

  public String getDatePattern() {
    return datePattern;
  }

  public void setDatePattern(String datePattern) {
    this.datePattern = datePattern;
  }

  public String getTimePattern() {
    return timePattern;
  }

  public void setTimePattern(String timePattern) {
    this.timePattern = timePattern;
  }

  public String getDateTimePattern() {
    return dateTimePattern;
  }

  public void setDateTimePattern(String dateTimePattern) {
    this.dateTimePattern = dateTimePattern;
  }

  public String getDecimalSep() {
    return decimalSep;
  }

  public void setDecimalSep(String decimalSep) {
    this.decimalSep = decimalSep;
  }

  public String getGroupingSep() {
    return groupingSep;
  }

  public void setGroupingSep(String groupingSep) {
    this.groupingSep = groupingSep;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public String getCurrencyPattern() {
    return currencyPattern;
  }

  public void setCurrencyPattern(String currencyPattern) {
    this.currencyPattern = currencyPattern;
  }

  @JsonProperty("firstDayOfWeek")
  public Integer getFirstDayOfWeek() {
    return firstDayOfWeek;
  }

  public void setFirstDayOfWeek(Integer firstDayOfWeek) {
    this.firstDayOfWeek = firstDayOfWeek;
    this.firstDayOfWeekPresent = true;
  }

  /**
   * JSON {@code firstDayOfWeek}: a number 1–7, JSON {@code null}, or a blank string to clear.
   * Omitted properties leave {@link #isFirstDayOfWeekPresent()} false.
   */
  @JsonSetter("firstDayOfWeek")
  public void assignFirstDayOfWeek(Object raw) {
    if (raw == null) {
      setFirstDayOfWeek(null);
      return;
    }
    if (raw instanceof Number number) {
      int n = number.intValue();
      // JAXB JSON maps JSON null onto 0 for numeric properties.
      setFirstDayOfWeek(n == 0 ? null : Integer.valueOf(n));
      return;
    }
    if (raw instanceof String text) {
      if (text.isBlank()) {
        setFirstDayOfWeek(null);
        return;
      }
      try {
        setFirstDayOfWeek(Integer.valueOf(text.trim()));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("invalid firstDayOfWeek: " + text, e);
      }
      return;
    }
    throw new IllegalArgumentException("invalid firstDayOfWeek: " + raw);
  }

  @JsonIgnore
  public boolean isFirstDayOfWeekPresent() {
    return firstDayOfWeekPresent;
  }

  public String getMeasurementSystem() {
    return measurementSystem;
  }

  public void setMeasurementSystem(String measurementSystem) {
    this.measurementSystem = measurementSystem;
  }

  public String getDefaultTz() {
    return defaultTz;
  }

  public void setDefaultTz(String defaultTz) {
    this.defaultTz = defaultTz;
  }

  public String getNumberingSystem() {
    return numberingSystem;
  }

  public void setNumberingSystem(String numberingSystem) {
    this.numberingSystem = numberingSystem;
  }

  public String getCalendar() {
    return calendar;
  }

  public void setCalendar(String calendar) {
    this.calendar = calendar;
  }

  public List<String> getFallbackChain() {
    return fallbackChain;
  }

  public void setFallbackChain(List<String> fallbackChain) {
    this.fallbackChain = fallbackChain;
  }

  /**
   * JSON {@code fallbackChain}: an array of language strings, a single string (JAXB JSON may
   * unwrap a one-element list to a scalar), JSON {@code null}, or a blank-only list to clear
   * (nested empty arrays do not survive every JSON provider, so clears are sent blank-only
   * and normalized back to empty server-side). Omitted properties leave {@link
   * #getFallbackChain()} null.
   */
  @JsonSetter("fallbackChain")
  public void assignFallbackChain(Object raw) {
    if (raw == null) {
      setFallbackChain(null);
      return;
    }
    if (raw instanceof List<?> list) {
      List<String> out = new ArrayList<>();
      for (Object item : list) {
        if (item != null) {
          out.add(String.valueOf(item));
        }
      }
      setFallbackChain(out);
      return;
    }
    if (raw instanceof String text) {
      setFallbackChain(List.of(text));
      return;
    }
    throw new IllegalArgumentException("invalid fallbackChain: " + raw);
  }
}

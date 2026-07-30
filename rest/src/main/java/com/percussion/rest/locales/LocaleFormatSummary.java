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

package com.percussion.rest.locales;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

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
  private String measurementSystem;
  private String defaultTz;
  private String numberingSystem;
  private String calendar;

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

  public Integer getFirstDayOfWeek() {
    return firstDayOfWeek;
  }

  public void setFirstDayOfWeek(Integer firstDayOfWeek) {
    this.firstDayOfWeek = firstDayOfWeek;
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
}

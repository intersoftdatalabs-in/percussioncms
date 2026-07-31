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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Product-shipped locale format profiles. Mirrors {@code RXLOCALEFORMAT} seed data so UI and unit
 * tests can resolve formats even when the DB catalog is unavailable.
 */
public final class PSLocaleFormatDefaults {

  private static final Map<String, PSLocaleFormat> SHIPPED;

  static {
    Map<String, PSLocaleFormat> m = new LinkedHashMap<>();
    put(
        m,
        fmt(
            "en-us",
            PSLocaleFormat.TEXT_DIR_LTR,
            "MM/dd/yyyy",
            "h:mm a",
            ".",
            ",",
            "USD",
            PSLocaleFormat.FIRST_DAY_SUNDAY,
            PSLocaleFormat.MEASUREMENT_US,
            "America/New_York"));
    put(
        m,
        fmt(
            "en-gb",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ".",
            ",",
            "GBP",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_UK,
            "Europe/London"));
    put(
        m,
        fmt(
            "ar",
            PSLocaleFormat.TEXT_DIR_RTL,
            "dd/MM/yyyy",
            "HH:mm",
            ".",
            ",",
            null,
            6,
            PSLocaleFormat.MEASUREMENT_METRIC,
            null));
    put(
        m,
        fmt(
            "bn",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "h:mm a",
            ".",
            ",",
            "BDT",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Asia/Dhaka"));
    put(
        m,
        fmt(
            "de",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd.MM.yyyy",
            "HH:mm",
            ",",
            ".",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            null));
    put(
        m,
        fmt(
            "de-de",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd.MM.yyyy",
            "HH:mm",
            ",",
            ".",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Berlin"));
    put(
        m,
        fmt(
            "de-at",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd.MM.yyyy",
            "HH:mm",
            ",",
            ".",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Vienna"));
    put(
        m,
        fmt(
            "de-ch",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd.MM.yyyy",
            "HH:mm",
            ",",
            ".",
            "CHF",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Zurich"));
    put(
        m,
        fmt(
            "de-li",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd.MM.yyyy",
            "HH:mm",
            ",",
            ".",
            "CHF",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Vaduz"));
    put(
        m,
        fmt(
            "de-lu",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd.MM.yyyy",
            "HH:mm",
            ",",
            ".",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Luxembourg"));
    put(
        m,
        fmt(
            "es",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            null));
    put(
        m,
        fmt(
            "es-es",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Madrid"));
    put(
        m,
        fmt(
            "es-mx",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "h:mm a",
            ".",
            ",",
            "MXN",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Mexico_City"));
    put(
        m,
        fmt(
            "es-cl",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd-MM-yyyy",
            "HH:mm",
            ",",
            ".",
            "CLP",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Santiago"));
    put(
        m,
        fmt(
            "es-co",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "h:mm a",
            ",",
            ".",
            "COP",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Bogota"));
    put(
        m,
        fmt(
            "es-ar",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            "ARS",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Argentina/Buenos_Aires"));
    put(
        m,
        fmt(
            "es-ve",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "h:mm a",
            ",",
            ".",
            "VES",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Caracas"));
    put(
        m,
        fmt(
            "es-pe",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ".",
            ",",
            "PEN",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Lima"));
    put(
        m,
        fmt(
            "es-ec",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            "USD",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Guayaquil"));
    put(
        m,
        fmt(
            "es-cr",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            "CRC",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Costa_Rica"));
    put(
        m,
        fmt(
            "es-pa",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "h:mm a",
            ".",
            ",",
            "PAB",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Panama"));
    put(
        m,
        fmt(
            "es-uy",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            "UYU",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Montevideo"));
    put(
        m,
        fmt(
            "es-bo",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            "BOB",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/La_Paz"));
    put(
        m,
        fmt(
            "es-sv",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "h:mm a",
            ".",
            ",",
            "USD",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/El_Salvador"));
    put(
        m,
        fmt(
            "es-hn",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "h:mm a",
            ".",
            ",",
            "HNL",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Tegucigalpa"));
    put(
        m,
        fmt(
            "es-ni",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "h:mm a",
            ".",
            ",",
            "NIO",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Managua"));
    put(
        m,
        fmt(
            "es-pr",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "h:mm a",
            ".",
            ",",
            "USD",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Puerto_Rico"));
    put(
        m,
        fmt(
            "fr",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            " ",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            null));
    put(
        m,
        fmt(
            "fr-fr",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            " ",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Paris"));
    put(
        m,
        fmt(
            "fr-ca",
            PSLocaleFormat.TEXT_DIR_LTR,
            "yyyy-MM-dd",
            "HH:mm",
            ",",
            " ",
            "CAD",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Toronto"));
    put(
        m,
        fmt(
            "fr-ch",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd.MM.yyyy",
            "HH:mm",
            ".",
            "'",
            "CHF",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Zurich"));
    put(
        m,
        fmt(
            "fr-lu",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            " ",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Luxembourg"));
    put(
        m,
        fmt(
            "fr-be",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            " ",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Brussels"));
    put(
        m,
        fmt(
            "fr-us",
            PSLocaleFormat.TEXT_DIR_LTR,
            "MM/dd/yyyy",
            "h:mm a",
            ".",
            ",",
            "USD",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/New_York"));
    put(
        m,
        fmt(
            "hi",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "h:mm a",
            ".",
            ",",
            "INR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            null));
    put(
        m,
        fmt(
            "hi-in",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "h:mm a",
            ".",
            ",",
            "INR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Asia/Kolkata"));
    put(
        m,
        fmt(
            "it",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            null));
    put(
        m,
        fmt(
            "it-it",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Rome"));
    put(
        m,
        fmt(
            "it-ch",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd.MM.yyyy",
            "HH:mm",
            ".",
            "'",
            "CHF",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Zurich"));
    put(
        m,
        fmt(
            "ja-jp",
            PSLocaleFormat.TEXT_DIR_LTR,
            "yyyy/MM/dd",
            "HH:mm",
            ".",
            ",",
            "JPY",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Asia/Tokyo"));
    put(
        m,
        fmt(
            "nl",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd-MM-yyyy",
            "HH:mm",
            ",",
            ".",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            null));
    put(
        m,
        fmt(
            "nl-nl",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd-MM-yyyy",
            "HH:mm",
            ",",
            ".",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Amsterdam"));
    put(
        m,
        fmt(
            "nl-be",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Brussels"));
    put(
        m,
        fmt(
            "pt",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            null,
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            null));
    put(
        m,
        fmt(
            "pt-br",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            "BRL",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "America/Sao_Paulo"));
    put(
        m,
        fmt(
            "pt-pt",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "HH:mm",
            ",",
            ".",
            "EUR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Lisbon"));
    put(
        m,
        fmt(
            "pl",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd.MM.yyyy",
            "HH:mm",
            ",",
            " ",
            "PLN",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            null));
    put(
        m,
        fmt(
            "ru",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd.MM.yyyy",
            "HH:mm",
            ",",
            " ",
            "RUB",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            null));
    put(
        m,
        fmt(
            "sv",
            PSLocaleFormat.TEXT_DIR_LTR,
            "yyyy-MM-dd",
            "HH:mm",
            ",",
            " ",
            "SEK",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            null));
    put(
        m,
        fmt(
            "tr",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd.MM.yyyy",
            "HH:mm",
            ",",
            ".",
            "TRY",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            null));
    put(
        m,
        fmt(
            "tr-tr",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd.MM.yyyy",
            "HH:mm",
            ",",
            ".",
            "TRY",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Europe/Istanbul"));
    put(
        m,
        fmt(
            "te",
            PSLocaleFormat.TEXT_DIR_LTR,
            "dd/MM/yyyy",
            "h:mm a",
            ".",
            ",",
            "INR",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Asia/Kolkata"));
    put(
        m,
        fmt(
            "zh-cn",
            PSLocaleFormat.TEXT_DIR_LTR,
            "yyyy/MM/dd",
            "HH:mm",
            ".",
            ",",
            "CNY",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Asia/Shanghai"));
    put(
        m,
        fmt(
            "zh-tw",
            PSLocaleFormat.TEXT_DIR_LTR,
            "yyyy/MM/dd",
            "HH:mm",
            ".",
            ",",
            "TWD",
            PSLocaleFormat.FIRST_DAY_MONDAY,
            PSLocaleFormat.MEASUREMENT_METRIC,
            "Asia/Taipei"));

    SHIPPED = Collections.unmodifiableMap(m);
  }

  private PSLocaleFormatDefaults() {}

  /** Immutable map of product-shipped format rows keyed by language string. */
  public static Map<String, PSLocaleFormat> shipped() {
    return SHIPPED;
  }

  /**
   * Absolute floor used when no catalog row supplies a field (same as {@code en-us} product
   * defaults).
   */
  public static PSLocaleFormat productFloor() {
    PSLocaleFormat f = SHIPPED.get("en-us");
    if (f != null) {
      return copyOf(f);
    }
    return fmt(
        "en-us",
        PSLocaleFormat.TEXT_DIR_LTR,
        "MM/dd/yyyy",
        "h:mm a",
        ".",
        ",",
        "USD",
        PSLocaleFormat.FIRST_DAY_SUNDAY,
        PSLocaleFormat.MEASUREMENT_US,
        "America/New_York");
  }

  private static void put(Map<String, PSLocaleFormat> m, PSLocaleFormat f) {
    m.put(f.getLanguageString(), f);
  }

  private static PSLocaleFormat copyOf(PSLocaleFormat src) {
    PSLocaleFormat f = new PSLocaleFormat(src.getLanguageString());
    f.setTextDir(src.getTextDir());
    f.setDatePattern(src.getDatePattern());
    f.setTimePattern(src.getTimePattern());
    f.setDateTimePattern(src.getDateTimePattern());
    f.setDecimalSep(src.getDecimalSep());
    f.setGroupingSep(src.getGroupingSep());
    f.setCurrencyCode(src.getCurrencyCode());
    f.setCurrencyPattern(src.getCurrencyPattern());
    f.setFirstDayOfWeek(src.getFirstDayOfWeek());
    f.setMeasurementSystem(src.getMeasurementSystem());
    f.setDefaultTz(src.getDefaultTz());
    f.setNumberingSystem(src.getNumberingSystem());
    f.setCalendar(src.getCalendar());
    return f;
  }

  private static PSLocaleFormat fmt(
      String code,
      String dir,
      String date,
      String time,
      String dec,
      String grp,
      String currency,
      int firstDay,
      String meas,
      String tz) {
    PSLocaleFormat f = new PSLocaleFormat(code);
    f.setTextDir(dir);
    f.setDatePattern(date);
    f.setTimePattern(time);
    f.setDecimalSep(dec);
    f.setGroupingSep(grp);
    f.setCurrencyCode(currency);
    f.setFirstDayOfWeek(firstDay);
    f.setMeasurementSystem(meas);
    f.setDefaultTz(tz);
    f.setNumberingSystem("latn");
    f.setCalendar("gregory");
    return f;
  }
}

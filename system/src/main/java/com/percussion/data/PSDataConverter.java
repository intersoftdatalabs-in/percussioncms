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
package com.percussion.data;

import com.percussion.design.objectstore.PSDateLiteral;
import com.percussion.design.objectstore.PSLiteral;
import com.percussion.design.objectstore.PSLiteralSet;
import com.percussion.design.objectstore.PSNumericLiteral;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.server.PSConsole;
import com.percussion.util.PSDataTypeConverter;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * The PSDataConverter class is used to convert potential comparable data from one certain type to
 * another. When using this class, two kinds of data with different type will be able to be compared
 * with each other.
 *
 * <p>For example, suppose we have the following data: {@code java.math.BigDecimal num = 10;} {@code
 * String oneNumberInString = "20";} If we were asked to determine mathematically whether a number
 * 10 is less than the other number 20, then what we can do here is to convert oneNumberInString
 * from type String to type java.math.BigDecimal. Once this is done, since both num and
 * oneNumberInString are of the same type, mathematical operation can be applied.
 *
 * <p>Currently, the convertible data types are BigDecimal, PSNumericLiteral, Date, PSDateLiteral,
 * String, PSTextLiteral, and File.
 *
 * @author Tas Giakouminakis
 * @version 1.0
 * @since 1.0
 */
public final class PSDataConverter {
  /**
   * Convert an object to one specified data type, if possible. However, this method will not
   * convert type PSLiteralSet so that it won't be changed. To handle type PSLiteralSet which is
   * quite complicated, in class com.percussion.data.PSConditionalEvaluator, method makeComparable2,
   * the data in the set is type determined and comparing operation performed.
   *
   * <p>If dateFormat is null, then our default formats will be activated one by one. This only
   * happens when trying to convert a type of String or PSTextLiteral to a type of Date. A typical
   * case is when data object is a String/PSTextLiteral, srcType is DATATYPE_TEXT, and dstType is
   * DATATYPE_DATE. If data is not in a supported date format pattern, then a
   * IllegalArgumentException is thrown.
   *
   * <p>With default format being activated, here are some working cases:
   *
   * <ol>
   *   <li>"1999-08-12 00:00:00.123"
   *   <li>"1999.08.12"
   *   <li>"1999.08.12 AD"
   *   <li>"1999.08.12 AD at 14:04:24"
   *   <li>"1999.08.12 at 01:01:01 PDT"
   * </ol>
   *
   * @param data the object to convert, may be {@code null}
   * @param dstType the data type to convert to
   * @param dateFormat the dateFormat of a string representing a date, may be {@code null}
   * @return the converted object, may be {@code null}
   * @throws IllegalArgumentException if conversion is not possible
   */
  public static Object convert(Object data, int dstType, FastDateFormat dateFormat) {
    if (data == null) {
      return null;
    }

    var srcType = getDataType(data);

    switch (srcType) {
      case DATATYPE_NUMERIC:
        return convertFromNumeric(data, dstType);
      case DATATYPE_DOUBLE:
        return convertFromDouble(data, dstType);
      case DATATYPE_LONG:
        return convertFromLong(data, dstType);
      case DATATYPE_INT:
        return convertFromInteger(data, dstType);
      case DATATYPE_DATE:
        return convertFromDate(data, dstType);
      case DATATYPE_NULLTEXT:
      case DATATYPE_TEXT:
        return convertFromText(data, dstType, dateFormat);
      case DATATYPE_FILE:
        return convertFromFile(data, dstType);
      default:
        return data;
    }
  }

  /**
   * Convert a text object delimited by commas or a List of objects to PSLiteralSet of either
   * PSTextLiteral or PSNumericLiteral or PSDateLiteral.
   *
   * @param data the object to convert, must not be {@code null}
   * @return the converted object, never {@code null}
   * @throws IllegalArgumentException if data is {@code null}
   */
  public static Object convertToSet(Object data) {
    Objects.requireNonNull(data, "data to convert cannot be null");

    if (data instanceof PSLiteralSet) {
      return data; // already a literal set
    }

    if (getDataType(data) != DATATYPE_TEXT) {
      return data; // can't convert non-text to set
    }

    var valueSet = parseCommaSeparatedValues(data.toString());

    if (valueSet.isEmpty()) {
      valueSet.add(data.toString()); // a set can have only one item
    }

    // Try to convert to date literals first
    return tryConvertToDateLiterals(valueSet)
        .orElseGet(
            () ->
                tryConvertToNumericLiterals(valueSet)
                    .orElseGet(() -> convertToTextLiterals(valueSet)));
  }

  /**
   * Get the data type of the specified object.
   *
   * @param data the object to get the type for, may be {@code null}
   * @return the data type constant
   */
  public static int getDataType(Object data) {
    if (data == null) {
      return DATATYPE_NULL;
    }

    if (data instanceof BigDecimal || data instanceof PSNumericLiteral) {
      return DATATYPE_NUMERIC;
    }
    if (data instanceof Double) {
      return DATATYPE_DOUBLE;
    }
    if (data instanceof Long) {
      return DATATYPE_LONG;
    }
    if (data instanceof Integer) {
      return DATATYPE_INT;
    }
    if (data instanceof Date || data instanceof PSDateLiteral) {
      return DATATYPE_DATE;
    }
    if (data instanceof String || data instanceof PSTextLiteral) {
      var text = data.toString();
      return text.isEmpty() ? DATATYPE_NULLTEXT : DATATYPE_TEXT;
    }
    if (data instanceof File) {
      return DATATYPE_FILE;
    }

    return DATATYPE_UNKNOWN;
  }

  // Private helper methods for conversion

  private static Object convertFromNumeric(Object data, int dstType) {
    var number =
        (data instanceof PSNumericLiteral) ? ((PSNumericLiteral) data).getNumber() : (Number) data;

    switch (dstType) {
      case DATATYPE_DATE:
        return number != null ? new Date(number.longValue()) : null;
      case DATATYPE_TEXT:
        return number != null ? number.toString() : null;
      case DATATYPE_INT:
      case DATATYPE_LONG:
      case DATATYPE_DOUBLE:
        throw new IllegalArgumentException(
            "Invalid conversion from "
                + getTypeString(DATATYPE_NUMERIC)
                + " to "
                + getTypeString(dstType));
      default:
        return data;
    }
  }

  private static Object convertFromDouble(Object data, int dstType) {
    var number = (Number) data;

    switch (dstType) {
      case DATATYPE_NUMERIC:
        return BigDecimal.valueOf(number.doubleValue());
      case DATATYPE_DATE:
        return new Date(number.longValue());
      case DATATYPE_TEXT:
        return number.toString();
      case DATATYPE_INT:
      case DATATYPE_LONG:
        throw new IllegalArgumentException(
            "Invalid conversion from "
                + getTypeString(DATATYPE_DOUBLE)
                + " to "
                + getTypeString(dstType));
      default:
        return data;
    }
  }

  private static Object convertFromLong(Object data, int dstType) {
    var number = (Number) data;

    switch (dstType) {
      case DATATYPE_NUMERIC:
        return BigDecimal.valueOf(number.longValue());
      case DATATYPE_DATE:
        return new Date(number.longValue());
      case DATATYPE_TEXT:
        return number.toString();
      case DATATYPE_INT:
      case DATATYPE_DOUBLE:
        throw new IllegalArgumentException(
            "Invalid conversion from "
                + getTypeString(DATATYPE_LONG)
                + " to "
                + getTypeString(dstType));
      default:
        return data;
    }
  }

  private static Object convertFromInteger(Object data, int dstType) {
    var number = (Number) data;

    switch (dstType) {
      case DATATYPE_NUMERIC:
        return BigDecimal.valueOf(number.longValue());
      case DATATYPE_DATE:
        return new Date(number.longValue());
      case DATATYPE_TEXT:
        return number.toString();
      case DATATYPE_LONG:
        return number.longValue();
      case DATATYPE_DOUBLE:
        return number.doubleValue();
      default:
        return data;
    }
  }

  private static Object convertFromDate(Object data, int dstType) {
    var date = (data instanceof PSDateLiteral) ? ((PSDateLiteral) data).getDate() : (Date) data;

    switch (dstType) {
      case DATATYPE_NUMERIC:
        return BigDecimal.valueOf(date.getTime());
      case DATATYPE_LONG:
        return date.getTime();
      case DATATYPE_DATE:
        return date;
      case DATATYPE_TEXT:
        return date.toString();
      case DATATYPE_DOUBLE:
      case DATATYPE_INT:
        throw new IllegalArgumentException(
            "Invalid conversion from "
                + getTypeString(DATATYPE_DATE)
                + " to "
                + getTypeString(dstType));
      default:
        return data;
    }
  }

  private static Object convertFromText(Object data, int dstType, FastDateFormat dateFormat) {
    var text = data.toString();

    switch (dstType) {
      case DATATYPE_NUMERIC:
        try {
          return new BigDecimal(text);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Cannot convert text to numeric: " + e.getMessage());
        }
      case DATATYPE_LONG:
        try {
          return Long.valueOf(text);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Cannot convert text to long: " + e.getMessage());
        }
      case DATATYPE_INT:
        try {
          return Integer.valueOf(text);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Cannot convert text to integer: " + e.getMessage());
        }
      case DATATYPE_DOUBLE:
        try {
          return Double.valueOf(text);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Cannot convert text to double: " + e.getMessage());
        }
      case DATATYPE_DATE:
        return convertTextToDate(text, dateFormat);
      case DATATYPE_TEXT:
        return data;
      default:
        return data;
    }
  }

  private static Object convertFromFile(Object data, int dstType) {
    var file = (File) data;

    switch (dstType) {
      case DATATYPE_NULL:
      case DATATYPE_NULLTEXT:
        // Return either null (file is empty) or data (file is not empty)
        return file.length() > 1 ? data : null;
      default:
        throw new IllegalArgumentException("Cannot convert file to " + getTypeString(dstType));
    }
  }

  private static Date convertTextToDate(String text, FastDateFormat dateFormat) {
    if (dateFormat != null) {
      try {
        return dateFormat.parse(text);
      } catch (Exception e) {
        PSConsole.printMsg(
            "DataConverter", "Not in specified format, trying to check all valid formats");
      }
    }

    try {
      return parseStringToDate(text);
    } catch (Exception e) {
      PSConsole.printMsg(
          "DataConverter",
          "One recommended text pattern is yyyy-MM-dd, such as 2000-03-30 19:04:45");
      throw new IllegalArgumentException("Cannot parse date from text: " + text);
    }
  }

  private static List<String> parseCommaSeparatedValues(String text) {
    return Arrays.asList(text.split(","));
  }

  private static Optional<PSLiteralSet> tryConvertToDateLiterals(List<String> valueSet) {
    var literalSet = new PSLiteralSet(PSLiteral.class);

    for (var value : valueSet) {
      try {
        var formatBuf = new StringBuilder();
        var date = PSDataTypeConverter.parseStringToDate(value, formatBuf);

        if (date != null) {
          var dateLiteral =
              new PSDateLiteral(date, FastDateFormat.getInstance(formatBuf.toString()));
          literalSet.add(dateLiteral);
        } else {
          return Optional.empty();
        }
      } catch (IllegalArgumentException e) {
        return Optional.empty();
      }
    }

    return Optional.of(literalSet);
  }

  private static Optional<PSLiteralSet> tryConvertToNumericLiterals(List<String> valueSet) {
    var literalSet = new PSLiteralSet(PSLiteral.class);

    for (var value : valueSet) {
      try {
        var numVal = Integer.parseInt(value);
        literalSet.add(new PSNumericLiteral(numVal, new DecimalFormat()));
      } catch (NumberFormatException e) {
        return Optional.empty();
      }
    }

    return Optional.of(literalSet);
  }

  private static PSLiteralSet convertToTextLiterals(List<String> valueSet) {
    var literalSet = new PSLiteralSet(PSLiteral.class);

    valueSet.stream().map(PSTextLiteral::new).forEach(literalSet::add);

    return literalSet;
  }

  private static String getTypeString(int type) {
    switch (type) {
      case DATATYPE_NUMERIC:
        return "NUMERIC";
      case DATATYPE_DOUBLE:
        return "DOUBLE";
      case DATATYPE_LONG:
        return "LONG";
      case DATATYPE_INT:
        return "INT";
      case DATATYPE_DATE:
        return "DATE";
      case DATATYPE_TEXT:
        return "TEXT";
      case DATATYPE_NULLTEXT:
        return "NULLTEXT";
      case DATATYPE_FILE:
        return "FILE";
      case DATATYPE_NULL:
        return "NULL";
      default:
        return "UNKNOWN";
    }
  }

  private static Date parseStringToDate(String text) {
    return PSDataTypeConverter.parseStringToDate(text, new StringBuilder());
  }

  // Data type constants
  public static final int DATATYPE_UNKNOWN = 0;
  public static final int DATATYPE_NUMERIC = 1;
  public static final int DATATYPE_DOUBLE = 2;
  public static final int DATATYPE_LONG = 3;
  public static final int DATATYPE_INT = 4;
  public static final int DATATYPE_DATE = 5;
  public static final int DATATYPE_TEXT = 6;
  public static final int DATATYPE_NULLTEXT = 7;
  public static final int DATATYPE_FILE = 8;
  public static final int DATATYPE_NULL = 9;

  // Private constructor to prevent instantiation
  private PSDataConverter() {
    // Utility class
  }
}

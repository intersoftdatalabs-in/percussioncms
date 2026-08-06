/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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

package com.percussion.system.utils;

import com.percussion.design.objectstore.PSNumericLiteral;
import com.percussion.design.objectstore.PSTextLiteral;
import java.util.Calendar;
import java.util.Optional;

/**
 * The PSCalculation class performs basic mathematical operation and computation for number related
 * Objects. These objects include java.lang.Number, all its subclasses, String,
 * com.percussion.design.objectstore.PSNumericLiteral, and
 * com.percussion.design.objectstore.PSTextLiteral. Moreover, it also adjusts calendar and time.
 *
 * @author Jian Huang
 * @version 2.0
 * @since 1.0
 * @deprecated Consider using BigDecimal and modern Java math libraries for new code
 */
@Deprecated(since = "Java 11 refactoring", forRemoval = false)
public final class PSCalculation {
  // Private constructor to prevent instantiation
  private PSCalculation() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * Make sure the input object is really a number related object. The returned object is either
   * java.lang.Number or one of its subclasses. This method is called by add, subtract, multiply,
   * and divide.
   *
   * @param o an input object
   * @return either java.lang.Number or one of its subclasses' object
   * @throws IllegalArgumentException if the object cannot be converted to a number
   */
  public static Number numberVerify(Object o) {
    if (o == null) {
      throw new IllegalArgumentException("numberVerify exception: null is not a number object");
    }

    // Check if it's already a Number (BigDecimal and BigInteger are subclasses of Number)
    if (o instanceof Number) {
      return (Number) o;
    }

    if (o instanceof String) {
      try {
        return Double.valueOf((String) o);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "NumberFormatException: numberVerify exception - cannot parse string: " + o, e);
      }
    }

    if (o instanceof PSNumericLiteral) {
      return ((PSNumericLiteral) o).getNumber();
    }

    if (o instanceof PSTextLiteral) {
      var text = ((PSTextLiteral) o).getText();
      try {
        return Double.valueOf(text);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "NumberFormatException: numberVerify exception - cannot parse PSTextLiteral: " + text,
            e);
      }
    }

    // The rest of data types are not numbers, even Date and Boolean
    throw new IllegalArgumentException(
        "numberVerify exception: input is not a number related object: "
            + o.getClass().getSimpleName());
  }

  /**
   * Sum of (Obj1 + Obj2), where Obj1 and Obj2 are number related objects. Since null and zero are
   * different, only addition of both nulls are allowed, which returns a null. However, if only one
   * of the two input objects is null, then a IllegalArgumentException will be thrown. Note: calling
   * numberVerify method in advance is not needed, add method does it automatically.
   *
   * @param o1 first operand
   * @param o2 second operand
   * @return the sum which is a number related object, or null if both Obj1 and Obj2 are null
   */
  public static Double add(Object o1, Object o2) {
    return performBinaryOperation(o1, o2, Double::sum, "add");
  }

  /**
   * The result of (Obj1 - Obj2), where Obj1 and Obj2 are number related objects. Since null and
   * zero are different, only subtraction of both nulls are allowed, which returns a null. However,
   * if only one of the two input objects is null, then a IllegalArgumentException will be thrown.
   * Note: calling numberVerify method in advance is not needed, subtract method does it
   * automatically.
   *
   * @param o1 first operand
   * @param o2 second operand
   * @return the difference which is a number related object, or null if both Obj1 and Obj2 are null
   */
  public static Double subtract(Object o1, Object o2) {
    return performBinaryOperation(o1, o2, (a, b) -> a - b, "subtract");
  }

  /**
   * The result of (Obj1 * Obj2), where Obj1 and Obj2 are number related objects Since null and zero
   * are different, only multiplication of both nulls are allowed, which returns a null. However, if
   * only one of the two input objects is null, then a IllegalArgumentException will be thrown.
   * Note: calling numberVerify method in advance is not needed, multiply method does it
   * automatically.
   *
   * @param o1 first operand
   * @param o2 second operand
   * @return the production which is a number related object, or null if both Obj1 and Obj2 are null
   */
  public static Double multiply(Object o1, Object o2) {
    return performBinaryOperation(o1, o2, (a, b) -> a * b, "multiply");
  }

  /**
   * The result of (Obj1 / Obj2), where Obj1 and Obj2 are number related objects. Since null and
   * zero are different, only division of both nulls are allowed, which returns a null. However, if
   * only one of the two input objects is null, then a IllegalArgumentException will be thrown.
   * Note: calling numberVerify method in advance is not needed, divide method does it
   * automatically.
   *
   * @param o1 first operand
   * @param o2 second operand
   * @return the division which is a number related object, or null if both Obj1 and Obj2 are null
   */
  public static Double divide(Object o1, Object o2) {
    return performBinaryOperation(
        o1,
        o2,
        (a, b) -> {
          if (b == 0.0) {
            throw new ArithmeticException("Division by zero");
          }
          return a / b;
        },
        "divide");
  }

  /**
   * Helper method to perform binary operations with consistent null handling and error reporting.
   *
   * @param o1 first operand
   * @param o2 second operand
   * @param operation the operation to perform
   * @param operationName the name of the operation for error messages
   * @return the result of the operation or null if both operands are null
   */
  private static Double performBinaryOperation(
      Object o1,
      Object o2,
      java.util.function.BinaryOperator<Double> operation,
      String operationName) {
    if (o1 == null && o2 == null) {
      return null;
    }

    if (o1 == null || o2 == null) {
      throw new IllegalArgumentException(
          "PSCalculation/" + operationName + " exception: null parameter(s)");
    }

    try {
      var num1 = numberVerify(o1);
      var num2 = numberVerify(o2);
      return operation.apply(num1.doubleValue(), num2.doubleValue());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "PSCalculation/" + operationName + " exception: " + e.getMessage(), e);
    }
  }

  /**
   * Adjust a given calendar date by updating year, month, date, hour, minute, and second.
   *
   * <p>Note 1 (by Jian Huang): com.percussion.util.PSDate, rather than java.util.Calendar, is
   * adopted as the return-type to prevent developers not familiar with the subject from using
   * java.util.Calendar incorrectly. For instance, as of JDK 1.2, get(Calendar.MONTH) returns an
   * integer from 0 to 11 instead of 1 to 12. Also, get(Calendar.HOUR_OF_DAY) uses 24-hour clock
   * while get(Calendar.HOUR) uses 12-hour clock which needs indication from get(Calendar.AM_PM).
   * Furthermore, java.util.Calendar's toString() method is only for debugging purpose, not for real
   * usage.
   *
   * <p>Note 2 (by Jian Huang): if you really want to use Calendar object, the information stored in
   * com.percussion.util.PSDate is enough to create a Calendar object.
   *
   * @param dateOld the initial date to be adjusted
   * @param numYear the number of year to adjust
   * @param numMonth the number of month to adjust
   * @param numDate the number of day to adjust
   * @param numHour the number of hour to adjust
   * @param numMin the number of minute to adjust
   * @param numSec the number of second to adjust
   * @return a PSDate representation of the updated calendar, or null if dateOld is null
   */
  public static PSDate dateAdjust(
      Calendar dateOld,
      int numYear,
      int numMonth,
      int numDate,
      int numHour,
      int numMin,
      int numSec) {
    return Optional.ofNullable(dateOld)
        .map(
            calendar -> {
              // Create a copy to avoid modifying the original
              var workingCalendar = (Calendar) calendar.clone();

              workingCalendar.add(Calendar.SECOND, numSec);
              workingCalendar.add(Calendar.MINUTE, numMin);
              workingCalendar.add(Calendar.HOUR_OF_DAY, numHour); // 24 hour clock
              workingCalendar.add(Calendar.DATE, numDate);
              workingCalendar.add(Calendar.MONTH, numMonth);
              workingCalendar.add(Calendar.YEAR, numYear);

              var hr = workingCalendar.get(Calendar.HOUR_OF_DAY); // 24 hour clock
              var mi = workingCalendar.get(Calendar.MINUTE);
              var se = workingCalendar.get(Calendar.SECOND);

              var da = workingCalendar.get(Calendar.DATE);
              // Add one to make sure that months are integers from 1 to 12
              var mo = workingCalendar.get(Calendar.MONTH) + 1;
              var yr = workingCalendar.get(Calendar.YEAR);

              // Warning: do NOT use Java Calendar's toString() method !!!
              // Warning: do NOT return Calendar object to prevent from "month" confusion
              // Warning: do NOT create java.util.Date object by using deprecated
              // constructors/methods, it is not safe in the long run
              return new PSDate(yr, mo, da, hr, mi, se);
            })
        .orElse(null);
  }
}

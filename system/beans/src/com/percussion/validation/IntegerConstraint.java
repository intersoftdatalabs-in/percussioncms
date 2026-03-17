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

package com.percussion.validation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.swing.JComboBox;
import javax.swing.JTextField;

/** 
 * Handles integer validation including the component value to be a valid integer
 * and within the range if the bounds are provided.
 *
 * @see java.lang.Integer
 */
public class IntegerConstraint implements ComponentValidationConstraint {

   /** 
    * Constructs a basic {@code IntegerConstraint} object that handles only
    * valid integers, no range checking.
    */
   public IntegerConstraint() {
      this.rangeMin = null;
      this.rangeMax = null;
   }

   /** 
    * Constructs this object that handles valid integers and checks the 
    * specified range. Enter {@code Integer.MAX_VALUE} for max if no
    * maximum boundary is required, and enter {@code Integer.MIN_VALUE} for
    * min if no minimum boundary is desired.
    *
    * @param min input for the range minimum. Must be &lt; max.
    * @param max input for the range maximum. Must be &gt; min.
    * 
    * @throws IllegalArgumentException if max is less than or equal to min.
    */
   public IntegerConstraint(int min, int max) {
      if (min >= max) {
         throw new IllegalArgumentException(
            "Maximum cannot be less than or equal to minimum");
      }

      this.rangeMax = (max < Integer.MAX_VALUE) ? max : null;
      this.rangeMin = (min > Integer.MIN_VALUE) ? min : null;
   }

   @Override
   public String getErrorText() {
      return getErrorText(null);
   }

   @Override
   public String getErrorText(String label) {
      var effectiveLabel = Optional.ofNullable(label)
         .filter(l -> !l.trim().isEmpty())
         .map(l -> l.endsWith(":") ? l : l + ":")
         .orElse("");

      var args = new ArrayList<Object>();
      args.add(effectiveLabel);

      String key = determineErrorKey(args);
      return MessageFormat.format(RESOURCE_BUNDLE.getString(key), args.toArray());
   }

   /**
    * Determines the appropriate error key based on validation state.
    *
    * @param args the arguments list to populate
    * @return the error key
    */
   private String determineErrorKey(List<Object> args) {
      if (invalidNumber != null) {
         if (!invalidNumber.trim().isEmpty()) {
            args.add(invalidNumber);
            return "notInteger";
         } else {
            return "missingInteger";
         }
      }

      // Range validation errors
      if (rangeMin != null && rangeMax == null) {
         args.add(rangeMin);
         return "lessThanMin";
      } else if (rangeMax != null && rangeMin == null) {
         args.add(rangeMax);
         return "moreThanMax";
      } else {
         return "notInRange";
      }
   }

   @Override
   public void checkComponent(Object suspect) throws ValidationException {
      invalidNumber = null;
      var enteredText = extractTextFromComponent(suspect);

      try {
         var value = Integer.parseInt(enteredText);
         validateRange(value);
      } catch (NumberFormatException e) {
         invalidNumber = enteredText;
         throw new ValidationException("Invalid integer format", e);
      }
   }

   /**
    * Extracts text from the given component.
    *
    * @param component the component to extract text from
    * @return the text content
    * @throws IllegalArgumentException if component is not supported
    */
   private String extractTextFromComponent(Object component) {
      if (component instanceof JTextField) {
         return ((JTextField) component).getText();
      } else if (component instanceof JComboBox) {
         var comboBox = (JComboBox<?>) component;
         return Optional.ofNullable(comboBox.getEditor().getItem())
            .map(Object::toString)
            .orElse("");
      } else {
         throw new IllegalArgumentException(
            "Component must be an instance of JTextField or JComboBox, but was: " +
            (component != null ? component.getClass().getSimpleName() : "null"));
      }
   }

   /**
    * Validates that the value is within the specified range.
    *
    * @param value the value to validate
    * @throws ValidationException if value is out of range
    */
   private void validateRange(int value) throws ValidationException {
      if ((rangeMin != null && value < rangeMin) ||
          (rangeMax != null && value > rangeMax)) {
         throw new ValidationException("Value out of range: " + value);
      }
   }

   /**
    * The integer representing the allowed maximum value, initialized to {@code
    * null} and set with maximum value to be checked if it is
    * constructed using {@link #IntegerConstraint(int, int)}. If not
    * {@code null}, its value will not be {@code Integer.MAX_VALUE}.
    */
   private final Integer rangeMax;

   /**
    * The integer representing the allowed minimum value, initialized to {@code
    * null} and set with minimum value to be checked if it is
    * constructed using {@link #IntegerConstraint(int, int)}. If not
    * {@code null}, its value will not be {@code Integer.MIN_VALUE}.
    */
   private final Integer rangeMin;

   /**
    * Used to pass data between the validation method and the error message
    * generator method. If the user entered a non-integer, it will be placed
    * in this field by the validation method.
    */
   private String invalidNumber;

   /**
    * The static resource bundle to provide the error messages, never {@code
    * null}.
    */
   private static final ResourceBundle RESOURCE_BUNDLE = createResourceBundle();

   /**
    * Creates the resource bundle with proper error handling.
    *
    * @return the resource bundle, never {@code null}
    */
   private static ResourceBundle createResourceBundle() {
      try {
         return ResourceBundle.getBundle(
            "com.percussion.validation.ValidationResources",
            Locale.getDefault());
      } catch (MissingResourceException e) {
         System.err.println("Warning: Could not load validation resources: " + e.getMessage());
         // Return a fallback empty resource bundle
         return ResourceBundle.getBundle("java.util.ListResourceBundle", Locale.getDefault());
      }
   }
}

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
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

/**
 * Constraint for validating String-based component values.
 * @see ValidationConstraint
 */
public class StringLengthConstraint implements ComponentValidationConstraint {

   /**
    * Constructs a new maximum length constraint.
    * 
    * @param maxLength the maximum length to be enforced, must be > 0.
    * @throws IllegalArgumentException if maxLength is less than 1
    */
   public StringLengthConstraint(int maxLength) {
      if (maxLength < 1) {
         throw new IllegalArgumentException("Maximum length must be greater than 0");
      }
      this.maxLength = maxLength;
   }

   @Override
   public String getErrorText() {
      return getErrorText(null);
   }

   @Override
   public String getErrorText(String label) {
      var effectiveLabel = Optional.ofNullable(label)
         .filter(l -> !l.trim().isEmpty())
         .map(String::trim)
         .map(l -> "<" + l + ">")
         .orElse("<?>");

      var args = new Object[] { effectiveLabel, String.valueOf(maxLength) };
      return MessageFormat.format(
         RESOURCE_BUNDLE.getString("stringlengthconstraint.exceeds"), args);
   }

   @Override
   public void checkComponent(Object suspect) throws ValidationException {
      var text = extractTextFromComponent(suspect);

      if (text.length() > maxLength) {
         throw new ValidationException();
      }
   }

   /**
    * Extracts text from the given component.
    *
    * @param component the component to extract text from
    * @return the text content, never {@code null}
    * @throws IllegalArgumentException if component is not supported
    */
   private String extractTextFromComponent(Object component) {
      if (component instanceof JTextComponent) {
         var textComponent = (JTextComponent) component;
         return Optional.ofNullable(textComponent.getDocument())
            .map(doc -> textComponent.getText())
            .orElse("");
      } else if (component instanceof JComboBox) {
         var comboBox = (JComboBox<?>) component;
         return Optional.ofNullable(comboBox.getSelectedItem())
            .map(Object::toString)
            .orElse("");
      } else {
         throw new IllegalArgumentException(
            "Component must be a text field or combo box, but was: " +
            (component != null ? component.getClass().getSimpleName() : "null"));
      }
   }

   /**
    * The maximum allowed length, initialized in constructor and never changed
    * after that. Always > 0.
    */
   private final int maxLength;

   /**
    * The validation framework resource bundle. Initialized statically,
    * never changed after that.
    */
   private static final ResourceBundle RESOURCE_BUNDLE =
      ResourceBundle.getBundle(ValidationFramework.VALIDATION_RESOURCES, Locale.getDefault());
}

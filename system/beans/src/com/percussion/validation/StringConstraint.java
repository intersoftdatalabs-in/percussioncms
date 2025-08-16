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
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

/** 
 * Constraint for validating String-based component values. Validates empty 
 * strings and invalid characters if the invalid characters are specified.
 *
 * @see ValidationConstraint
 */
public class StringConstraint implements ComponentValidationConstraint {

   /**
    * Constructs the object to do basic validation for checking empty component
    * value.
    */
   public StringConstraint() {
      this.invalidCharacters = null;
   }

   /** 
    * Constructs this object to validate all the invalid characters in the 
    * supplied string do not present in the component value in addition to empty
    * string validation.
    * 
    * @param invalidChars the string with invalid characters, may not be {@code null}
    * or empty.
    * @throws IllegalArgumentException if invalidChars is null or empty
    */
   public StringConstraint(String invalidChars) {
      if (invalidChars == null || invalidChars.trim().isEmpty()) {
         throw new IllegalArgumentException("invalidChars may not be null or empty.");
      }
      this.invalidCharacters = invalidChars;
   }

   @Override
   public String getErrorText(String label) {
      var effectiveLabel = Optional.ofNullable(label)
         .filter(l -> !l.trim().isEmpty())
         .map(l -> l.endsWith(":") ? l.substring(0, l.length() - 1) : l)
         .orElse("");

      String key;
      List<Object> args;
      if (lastErrorCharacter == null) {
         key = "emptyField";
         args = List.of(effectiveLabel);
      } else {
         key = "invalidChar";
         args = List.of(effectiveLabel, lastErrorCharacter, invalidCharacters);
      }

      return MessageFormat.format(RESOURCE_BUNDLE.getString(key), args.toArray());
   }
   
   @Override
   public String getErrorText() {
      return getErrorText(null);
   }

   @Override
   public void checkComponent(Object suspect) throws ValidationException {
      var data = extractTextFromComponent(suspect);

      // Check for invalid characters
      if (invalidCharacters != null) {
         var invalidChar = findInvalidCharacter(data);
         if (invalidChar.isPresent()) {
            lastErrorCharacter = String.valueOf(invalidChar.get());
            throw new ValidationException();
         }
      }
      
      // Check if component is empty
      if (data.trim().isEmpty()) {
         lastErrorCharacter = null;
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
    * Finds the first invalid character in the given text.
    *
    * @param text the text to check
    * @return the first invalid character found, or empty if none found
    */
   private Optional<Character> findInvalidCharacter(String text) {
      if (invalidCharacters == null) {
         return Optional.empty();
      }

      return text.chars()
         .filter(ch -> invalidCharacters.indexOf(ch) >= 0)
         .mapToObj(ch -> (char) ch)
         .findFirst();
   }

   /**
    * The string representing not allowed characters in the component value. 
    * Initialized to {@code null} and set with a value if it is
    * constructed using {@link #StringConstraint(String)}.
    */
   private final String invalidCharacters;

   /**
    * The last error character that caused validation to fail.
    * Used for error message generation.
    */
   private String lastErrorCharacter;

   /** 
    * A string of invalid characters that is typically not used in the normal
    * identifier convention.
    */
   public static final String NO_SPECIAL_CHAR = 
      " ~!@#$%^&*()+`-=[]{}|;':,.<>/?";
   
   /** A string of characters that is not accepted as a part of a class name. */
   public static final String CLASS_NAME_CHAR_ONLY = 
      " ~!@#%^&*()+`-=[]{}|;':,<>/?";

   /**
    * The static resource bundle to provide the error messages.
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

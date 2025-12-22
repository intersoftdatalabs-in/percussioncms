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
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

/**
 * Constraint for validating String-based component values to validate that the
 * component value is not one of the supplied list of values.
 * 
 * @see ValidationConstraint
 */
public class ListMemberConstraint implements ValidationConstraint {

   /**
    * Default constructor for this type of constraint.
    *
    * @param existing A collection of names against which to validate. The
    * control's text cannot appear in this list. The objects in the collection
    * are converted to strings using {@code toString()}. {@code null}
    * entries are ignored.
    *
    * @param caseSensitive If {@code true}, the check is performed using
    * case sensitive comparison.
    */
   public ListMemberConstraint(Collection<?> existing, boolean caseSensitive) {
      this.caseSensitive = caseSensitive;
      this.existingElements = Optional.ofNullable(existing)
         .orElse(Collections.emptyList());
   }

   /**
    * A convenience method. Equivalent to {@code this(existing, false)}.
    * See {@link #ListMemberConstraint(Collection, boolean)} for a
    * description.
    *
    * @param existing A collection of names against which to validate
    */
   public ListMemberConstraint(Collection<?> existing) {
      this(existing, false);
   }

   @Override
   public String getErrorText() {
      if (errorMessage == null) {
         var existingNames = existingElements.stream()
            .filter(elem -> elem != null)
            .map(Object::toString)
            .collect(Collectors.joining(", "));

         var args = new String[] { existingNames };
         var pattern = RESOURCE_BUNDLE.getString("uniqueListConstraintError");
         errorMessage = MessageFormat.format(pattern, (Object[]) args);
      }
      return errorMessage;
   }

   @Override
   public void checkComponent(Object suspect) throws ValidationException {
      var text = extractTextFromComponent(suspect);

      if (text.trim().isEmpty()) {
         return; // Empty text is allowed
      }

      var isDuplicate = existingElements.stream()
         .filter(elem -> elem != null)
         .map(Object::toString)
         .anyMatch(existingName ->
            caseSensitive ? text.equals(existingName) : text.equalsIgnoreCase(existingName));

      if (isDuplicate) {
         throw new ValidationException("Value already exists in the list");
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
         return Optional.ofNullable(textComponent.getText()).orElse("");
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
    * Indicates whether the comparison should be case sensitive.
    */
   private final boolean caseSensitive;

   /**
    * The collection of existing elements to check against. Never {@code null}.
    */
   private final Collection<?> existingElements;

   /**
    * The cached error message. Lazy-loaded when first requested.
    */
   private String errorMessage;

   /**
    * The static resource bundle to provide error messages.
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

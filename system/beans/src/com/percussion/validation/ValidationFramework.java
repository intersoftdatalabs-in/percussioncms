/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.text.JTextComponent;

/**
 * The generic "validation" framework for checking specified components within
 * a container. This class is the interface between actual components that
 * need to be checked and the constraints to be checked against specified by
 * the programmer.
 * <P>
 * {@code ValidationException} is thrown to signify an invalid value within
 * a component. {@code JOptionPane} is used to quickly pop up a warning
 * message directing the user to the invalid component. Then the focus is given
 * to invalid component for user's convenience.
 *
 * @see ValidationException
 * @see ValidationConstraint
 */
public class ValidationFramework {

   /**
    * Default constructor. All components and constraints to be validated will
    * be {@code null}. The parent window is also {@code null}.
    */
   public ValidationFramework() {
      // Default constructor
   }
   
   /** 
    * Constructs a basic framework with specified component-constraint pairs and
    * a parent window.
    *
    * @param parent the parent window to be used for error dialogs, may not be 
    * {@code null}
    * @param components the array of components to be checked, may not be {@code
    * null} and its length must be equal to constraints length and the
    * components in the array must not be {@code null}
    * @param constraints the array of constraints to use for validation, may not
    * be {@code null} and its length must be equal to components length and
    * the constraints in the array must not be {@code null}
    *
    * @throws IllegalArgumentException if any parameter is invalid.
    */
   public ValidationFramework(Window parent, Object[] components, 
      ValidationConstraint[] constraints) {
      validateConstructorParameters(parent, components, constraints);
      setFramework(parent, components, constraints);
   }

   /**
    * Validates constructor parameters.
    *
    * @param parent the parent window
    * @param components the array of components
    * @param constraints the array of constraints
    * @throws IllegalArgumentException if any parameter is invalid
    */
   private void validateConstructorParameters(Window parent, Object[] components,
      ValidationConstraint[] constraints) {
      if (parent == null) {
         throw new IllegalArgumentException("the parent window may not be null.");
      }

      if (components == null) {
         throw new IllegalArgumentException("components may not be null.");
      }

      if (constraints == null) {
         throw new IllegalArgumentException("constraints may not be null.");
      }

      if (components.length != constraints.length) {
         throw new IllegalArgumentException(
            "components and constraints array lengths must match");
      }

      for (var component : components) {
         if (component == null) {
            throw new IllegalArgumentException(
               "component in the components array may not be null.");
         }
      }
      
      for (var constraint : constraints) {
         if (constraint == null) {
            throw new IllegalArgumentException(
               "constraint in the constraints array may not be null.");
         }
      }
   }

   /** 
    * Reinitializes the framework with the specified components and constraints.
    * 
    * @param components the array of components to be checked, assumes not 
    * {@code null} and its length is equal to constraints length and the
    * components in the array are not {@code null}
    * @param constraints the array of constraints to use for validation, assumes
    * not {@code null} and its length is equal to components length and
    * the constraints in the array are not {@code null}
    */
   private void setFramework(Object[] components, 
      ValidationConstraint[] constraints) {
      this.componentList = List.of(components);
      this.constraintList = List.of(constraints);
   }

   /**
    * Resets the framework with the parent window, components and constraints.
    *
    * @param parent the parent window to be used for error dialogs, may not be 
    * {@code null}
    * @param components the array of components to be checked, may not be {@code
    * null} and its length must be equal to constraints length and the
    * components in the array must not be {@code null}
    * @param constraints the array of constraints to use for validation, may not
    * be {@code null} and its length must be equal to components length and
    * the constraints in the array must not be {@code null}
    *
    * @throws IllegalArgumentException if any parameter is invalid.
    */
   public void setFramework(Window parent, Object[] components,
      ValidationConstraint[] constraints) {
      validateConstructorParameters(parent, components, constraints);
      this.parentWindow = parent;
      setFramework(components, constraints);
   }

   /** 
    * Loops through all the components that need validation and checks them
    * against their appropriate constraints. If the constraint throws a {@code
    * ValidationException}, the exception is caught and displays a warning
    * dialog and the component with the incorrect value is highlighted and
    * focused. If the lists are {@code null}, then it always returns {@code
    * true}
    *
    * @return {@code true} if validation succeeds, otherwise {@code false}
    * @see ValidationConstraint
    */
   public boolean checkValidity() {
      if (componentList == null || constraintList == null) {
         return true;
      }

      for (int i = 0; i < componentList.size(); i++) {
         try {
            constraintList.get(i).checkComponent(componentList.get(i));
         } catch (ValidationException e) {
            handleValidationError(i);
            return false;
         }
      }
      return true;
   }

   /**
    * Handles validation error for the component at the specified index.
    *
    * @param index the index of the component that failed validation
    */
   private void handleValidationError(int index) {
      var component = componentList.get(index);
      var constraint = constraintList.get(index);

      String label = null;
      if (component instanceof Component) {
         label = getLabelTextForComponent((Component) component);
      }

      String message;
      if (constraint instanceof ComponentValidationConstraint) {
         message = ((ComponentValidationConstraint) constraint).getErrorText(label);
      } else {
         message = constraint.getErrorText();
      }

      JOptionPane.showMessageDialog(parentWindow, message,
         RESOURCE_BUNDLE.getString("error"), JOptionPane.ERROR_MESSAGE);

      if (component instanceof JTextComponent) {
         var textComponent = (JTextComponent) component;
         textComponent.selectAll();
         ((Component) component).requestFocus();

         // if the error is at the password field, clear it.
         if (component instanceof JPasswordField) {
            ((JPasswordField) component).setText(null);
         }
      } else if (component instanceof JComboBox) {
         var comboBox = (JComboBox<?>) component;
         var editor = comboBox.getEditor().getEditorComponent();

         if (editor instanceof JTextComponent) {
            ((JTextComponent) editor).selectAll();
         }

         editor.requestFocus();
      }
   }
   
   /**
    * Gets the label text for the supplied component. 
    * 
    * @param comp the component to check for label, assumed not {@code null}
    *
    * @return the label text, may be {@code null} if the container of this
    * component does not contain a label that refers to this component. May be
    * empty if the label text is empty.
    */
   private String getLabelTextForComponent(Component comp) {
      return Optional.ofNullable(comp.getParent())
         .map(Container::getComponents)
         .map(Arrays::stream)
         .orElse(Arrays.stream(new Component[0]))
         .filter(JLabel.class::isInstance)
         .map(JLabel.class::cast)
         .filter(label -> label.getLabelFor() == comp)
         .map(JLabel::getText)
         .findFirst()
         .orElse(null);
   }

   /**
    * The list of components to be validated. Initialized in the
    * constructor and may be modified through {@code
    * setFramework(Window, Object[], ValidationConstraint[])}. Never
    * {@code null} after initialization.
    */
   private List<Object> componentList;

   /**
    * The list of constraints used for validation. Initialized in the
    * constructor and may be modified through {@code
    * setFramework(Window, Object[], ValidationConstraint[])}. Never
    * {@code null} after initialization.
    */
   private List<ValidationConstraint> constraintList;

   /**
    * The parent window to be used to display error dialogs. Initialized in the
    * constructor and may be modified through {@code
    * setFramework(Window, Object[], ValidationConstraint[])}. Never
    * {@code null} after initialization.
    */
   private Window parentWindow;

   /**
    * The fully qualified resource file name used for validations.
    */
   public static final String VALIDATION_RESOURCES = 
      "com.percussion.validation.ValidationResources";

   /**
    * The static resource bundle to provide the error messages, never {@code
    * null}
    */
   private static final ResourceBundle RESOURCE_BUNDLE =
      ResourceBundle.getBundle(VALIDATION_RESOURCES, Locale.getDefault());
}

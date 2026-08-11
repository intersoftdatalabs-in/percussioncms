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

package com.percussion.soln.jcr;

// REFACTORED: CP-JAVA11
import java.util.Calendar;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Convenience base that implements JCR property accessors via {@link #getValue()}.
 */
public abstract class AbstractSimplyProperty {

  /**
   * Creates a property helper.
   */
  protected AbstractSimplyProperty() {
    // default
  }

  // REFACTORED: CP-JAVA11
  /**
   * Returns this property as a boolean.
   *
   * @return the boolean value
   * @throws ValueFormatException if conversion fails
   * @throws RepositoryException if a repository error occurs
   */
  public boolean getBoolean() throws ValueFormatException, RepositoryException {
    return getValue().getBoolean();
  }

  /**
   * Returns this property as a date.
   *
   * @return the calendar value
   * @throws ValueFormatException if conversion fails
   * @throws RepositoryException if a repository error occurs
   */
  public Calendar getDate() throws ValueFormatException, RepositoryException {
    return getValue().getDate();
  }

  /**
   * Returns this property as a double.
   *
   * @return the double value
   * @throws ValueFormatException if conversion fails
   * @throws RepositoryException if a repository error occurs
   */
  public double getDouble() throws ValueFormatException, RepositoryException {
    return getValue().getDouble();
  }

  /**
   * Returns this property as a long.
   *
   * @return the long value
   * @throws ValueFormatException if conversion fails
   * @throws RepositoryException if a repository error occurs
   */
  public long getLong() throws ValueFormatException, RepositoryException {
    return getValue().getLong();
  }

  /**
   * Returns this property as a string.
   *
   * @return the string value
   * @throws ValueFormatException if conversion fails
   * @throws RepositoryException if a repository error occurs
   */
  public String getString() throws ValueFormatException, RepositoryException {
    return getValue().getString();
  }

  /**
   * Returns the JCR property type of this property.
   *
   * @return the property type constant
   * @throws RepositoryException if a repository error occurs
   */
  public int getType() throws RepositoryException {
    return getValue().getType();
  }

  /**
   * Returns the single value for this property.
   *
   * @return the value
   * @throws ValueFormatException if conversion fails
   * @throws RepositoryException if a repository error occurs
   */
  public abstract Value getValue() throws ValueFormatException, RepositoryException;

  /**
   * Returns all values for this multi-valued property.
   *
   * @return the values
   * @throws ValueFormatException if conversion fails
   * @throws RepositoryException if a repository error occurs
   */
  public abstract Value[] getValues() throws ValueFormatException, RepositoryException;
}

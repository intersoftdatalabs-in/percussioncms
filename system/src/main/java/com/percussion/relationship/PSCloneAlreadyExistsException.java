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
package com.percussion.relationship;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import java.util.Objects;

/**
 * Runtime exception that is thrown when someone requests to create a clone of
 * an item based on a relationship type and the same clone was created
 * earlier and still exists in the system.
 *
 * @author RammohanVangapalli
 */
public final class PSCloneAlreadyExistsException extends RuntimeException {
  /**
   * Constructor that takes the existing relationships owner, dependent and
   * relationship name that can be accessible later when generating the
   * message.
   *
   * @param owner owner of the existing relationship, must not be
   * {@code null}.
   * @param dependent dependent of the existing relationship, must not be
   * {@code null}.
   * @param relType name of the relationship, must not be {@code null}
   * or empty.
   * @throws IllegalArgumentException if any parameter is {@code null} or relType is empty
   */
  public PSCloneAlreadyExistsException(PSLocator owner, PSLocator dependent, String relType) {
    super();
    m_owner = Objects.requireNonNull(owner, "owner cannot be null");
    m_dependent = Objects.requireNonNull(dependent, "dependent cannot be null");

    Objects.requireNonNull(relType, "relType cannot be null");
    if (relType.trim().isEmpty()) {
      throw new IllegalArgumentException("relType cannot be empty");
    }
    m_relType = relType.trim();
  }

  /**
   * Constructor that takes the existing relationship that can be
   * accessible later when generating the message.
   *
   * @param relationship must not be {@code null}.
   * @throws IllegalArgumentException if relationship is {@code null}
   */
  public PSCloneAlreadyExistsException(PSRelationship relationship) {
    super();
    m_relationship = Objects.requireNonNull(relationship, "relationship cannot be null");
  }

  /**
   * Default constructor.
   */
  public PSCloneAlreadyExistsException() {
    super();
  }

  /**
   * Constructor with message.
   *
   * @param message the detail message
   */
  public PSCloneAlreadyExistsException(String message) {
    super(message);
  }

  /**
   * Constructor with message and cause.
   *
   * @param message the detail message
   * @param cause the cause of this exception
   */
  public PSCloneAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructor with cause.
   *
   * @param cause the cause of this exception
   */
  public PSCloneAlreadyExistsException(Throwable cause) {
    super(cause);
  }

  /**
   * Access method for the relationship type name.
   *
   * @return name of the existing relationship, never {@code null}.
   */
  public String getRelationshipType() {
    if (m_relationship != null) {
      return m_relationship.getConfig().getName();
    }
    return m_relType;
  }

  /**
   * Access method for the owner.
   *
   * @return owner of the existing relationship, never {@code null}.
   */
  public PSLocator getOwner() {
    if (m_relationship != null) {
      return m_relationship.getOwner();
    }
    return m_owner;
  }

  /**
   * Access method for the dependent.
   *
   * @return dependent of the existing relationship, never {@code null}.
   */
  public PSLocator getDependent() {
    if (m_relationship != null) {
      return m_relationship.getDependent();
    }
    return m_dependent;
  }

  /**
   * Access method for the relationship.
   *
   * @return the existing relationship, may be {@code null}.
   */
  public PSRelationship getRelationship() {
    return m_relationship;
  }

  /**
   * Owner of the existing relationship, may be {@code null}.
   */
  private final PSLocator m_owner;

  /**
   * Dependent of the existing relationship, may be {@code null}.
   */
  private final PSLocator m_dependent;

  /**
   * Name of the relationship, may be {@code null}.
   */
  private final String m_relType;

  /**
   * The existing relationship, may be {@code null}.
   */
  private final PSRelationship m_relationship;
}

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
// REFACTORED: CP-JAVA11
package com.percussion.delivery.polls.data;

import java.util.Set;

/**
 * Represents a poll with a name, question, and possible answers. Sunny Sal: Refactored for Java 11,
 * Google style, and better grammar.
 */
public interface IPSPoll {
  /**
   * Gets the poll's id as a string (typically the string form of the persistence id).
   *
   * @return the id, never {@code null}.
   */
  String getId();

  /**
   * Sets the poll's id from its string form.
   *
   * @param id the id, not {@code null}.
   */
  void setId(String id);

  /**
   * Gets the poll's name.
   *
   * @return the poll name, never {@code null}.
   */
  String getPollName();

  /**
   * Sets the poll's name.
   *
   * @param pollName the poll name, not {@code null}.
   */
  void setPollName(String pollName);

  /**
   * Gets the poll's question text.
   *
   * @return the poll question, never {@code null}.
   */
  String getPollQuestion();

  /**
   * Sets the poll's question text.
   *
   * @param pollQuestion the poll question, not {@code null}.
   */
  void setPollQuestion(String pollQuestion);

  /**
   * Gets the poll's possible answers.
   *
   * @return the set of poll answers; may be empty but never {@code null}.
   */
  Set<IPSPollAnswer> getPollAnswers();

  /**
   * Sets the poll's possible answers.
   *
   * @param pollAnswers the set of poll answers, not {@code null}.
   */
  void setPollAnswers(Set<IPSPollAnswer> pollAnswers);
}

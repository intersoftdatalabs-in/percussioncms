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

package com.percussion.delivery.polls.services;

import com.percussion.delivery.polls.data.IPSPoll;
import com.percussion.delivery.polls.data.IPSPollAnswer;

// REFACTORED: CP-JAVA11
/**
 * Data access object contract for the polls feature. Implementations are responsible for reading
 * and writing polls to a backing store (typically a relational database via JPA).
 */
public interface IPSPollsDao {
  /**
   * Finds the poll with the supplied poll name.
   *
   * @param pollName the poll name to look up, not {@code null}.
   * @return the matching {@link IPSPoll}, or {@code null} when no poll with that name exists.
   */
  IPSPoll find(String pollName);

  /**
   * Finds the poll whose question matches the supplied value.
   *
   * @param pollQuestion the poll question to look up, not {@code null}.
   * @return the matching {@link IPSPoll}, or {@code null} when no poll with that question exists.
   */
  IPSPoll findByQuestion(String pollQuestion);

  /**
   * Creates a new empty {@link IPSPoll} instance ready for population.
   *
   * @return a new empty poll, never {@code null}.
   */
  IPSPoll createEmptyPoll();

  /**
   * Creates a new empty {@link IPSPollAnswer} instance ready for population.
   *
   * @return a new empty poll answer, never {@code null}.
   */
  IPSPollAnswer createEmptyAnswer();

  /**
   * Saves the supplied poll (insert or update).
   *
   * @param poll the poll to persist, not {@code null}.
   */
  void save(IPSPoll poll);
}

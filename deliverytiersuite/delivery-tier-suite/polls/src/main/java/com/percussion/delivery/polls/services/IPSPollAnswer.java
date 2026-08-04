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

// REFACTORED: CP-JAVA11
/**
 * Lightweight poll answer contract used by service interfaces. Concrete implementations are
 * typically the JPA-backed {@code PSPollAnswer} entity.
 */
public interface IPSPollAnswer {
  /**
   * Gets the answer's persistence id.
   *
   * @return the id.
   */
  long getId();

  /**
   * Sets the answer's persistence id.
   *
   * @param id the id.
   */
  void setId(long id);

  /**
   * Gets the answer text.
   *
   * @return the answer text, never {@code null}.
   */
  String getAnswer();

  /**
   * Sets the answer text.
   *
   * @param answer the answer text, not {@code null}.
   */
  void setAnswer(String answer);

  /**
   * Gets the number of times this answer has been selected.
   *
   * @return the count, zero or higher.
   */
  int getCount();

  /**
   * Sets the number of times this answer has been selected.
   *
   * @param count the count, zero or higher.
   */
  void setCount(int count);
}

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

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 * Represents a REST poll with its name, question, results, and session restriction. Sunny Sal:
 * Refactored for Java 11, Google style, and better grammar.
 */
@XmlRootElement(name = "poll")
public class PSRestPoll {
  private String pollName;
  private String pollQuestion;
  private Map<String, Integer> pollResults;
  private int totalVotes;
  private Map<String, Boolean> pollSubmits;
  private boolean restrictBySession;

  /** Default constructor required for JAX-RS binding frameworks. */
  public PSRestPoll() {}

  /**
   * Gets the poll name.
   *
   * @return the poll name, may be {@code null} when not yet set.
   */
  public String getPollName() {
    return pollName;
  }

  /**
   * Sets the poll name.
   *
   * @param pollName the poll name, not {@code null}.
   */
  public void setPollName(String pollName) {
    this.pollName = pollName;
  }

  /**
   * Gets the poll question.
   *
   * @return the poll question, may be {@code null} when not yet set.
   */
  public String getPollQuestion() {
    return pollQuestion;
  }

  /**
   * Sets the poll question.
   *
   * @param pollQuestion the poll question, not {@code null}.
   */
  public void setPollQuestion(String pollQuestion) {
    this.pollQuestion = pollQuestion;
  }

  /**
   * Gets the poll results: a map of answer text to current vote count.
   *
   * @return the poll results map, may be {@code null} when not yet set.
   */
  public Map<String, Integer> getPollResults() {
    return pollResults;
  }

  /**
   * Sets the poll results.
   *
   * @param pollResults the poll results map, not {@code null}.
   */
  public void setPollResults(Map<String, Integer> pollResults) {
    this.pollResults = pollResults;
  }

  /**
   * Gets the per-session submission map for the poll (key=question, value=whether the user has
   * already voted in the current session).
   *
   * @return the map of session submissions, may be {@code null} when not yet set.
   */
  public Map<String, Boolean> getPollSubmits() {
    return pollSubmits;
  }

  /**
   * Sets the per-session submission map for the poll.
   *
   * @param pollSubmits the map of session submissions, not {@code null}.
   */
  public void setPollSubmits(Map<String, Boolean> pollSubmits) {
    this.pollSubmits = pollSubmits;
  }

  /**
   * Gets the total number of votes cast for this poll.
   *
   * @return the total votes, zero or higher.
   */
  public int getTotalVotes() {
    return totalVotes;
  }

  /**
   * Sets the total number of votes cast for this poll.
   *
   * @param totalVotes the total votes, zero or higher.
   */
  public void setTotalVotes(int totalVotes) {
    this.totalVotes = totalVotes;
  }

  /**
   * Returns whether the poll is restricted to one submission per session.
   *
   * @return {@code true} if restricted by session, {@code false} otherwise.
   */
  public boolean isRestrictBySession() {
    return restrictBySession;
  }

  /**
   * Sets whether the poll is restricted to one submission per session.
   *
   * @param restrictBySession {@code true} to restrict by session, {@code false} otherwise.
   */
  public void setRestrictBySession(boolean restrictBySession) {
    this.restrictBySession = restrictBySession;
  }
}

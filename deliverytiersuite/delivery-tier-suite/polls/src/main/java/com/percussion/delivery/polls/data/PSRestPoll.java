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

import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;

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

  public PSRestPoll() {}

  public String getPollName() {
    return pollName;
  }

  public void setPollName(String pollName) {
    this.pollName = pollName;
  }

  public String getPollQuestion() {
    return pollQuestion;
  }

  public void setPollQuestion(String pollQuestion) {
    this.pollQuestion = pollQuestion;
  }

  public Map<String, Integer> getPollResults() {
    return pollResults;
  }

  public void setPollResults(Map<String, Integer> pollResults) {
    this.pollResults = pollResults;
  }

  public Map<String, Boolean> getPollSubmits() {
    return pollSubmits;
  }

  public void setPollSubmits(Map<String, Boolean> pollSubmits) {
    this.pollSubmits = pollSubmits;
  }

  public int getTotalVotes() {
    return totalVotes;
  }

  public void setTotalVotes(int totalVotes) {
    this.totalVotes = totalVotes;
  }

  public boolean isRestrictBySession() {
    return restrictBySession;
  }

  public void setRestrictBySession(boolean restrictBySession) {
    this.restrictBySession = restrictBySession;
  }
}

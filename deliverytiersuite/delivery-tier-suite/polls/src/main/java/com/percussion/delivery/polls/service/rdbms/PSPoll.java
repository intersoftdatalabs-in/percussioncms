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

package com.percussion.delivery.polls.service.rdbms;

import com.percussion.delivery.polls.data.IPSPoll;
import com.percussion.delivery.polls.data.IPSPollAnswer;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Set;

// REFACTORED: CP-JAVA11
@Entity
@Table(name = "PERC_POLLS")
public class PSPoll implements IPSPoll, Serializable {
  @Id
  @GeneratedValue
  @Column(name = "POLL_ID")
  private long pollId;

  @Column(name = "POLL_NAME", nullable = false, length = 256)
  private String pollName;

  @Column(name = "POLL_QUESTION", nullable = false, length = 4000)
  private String pollQuestion;

  @OneToMany(
      cascade = CascadeType.ALL,
      fetch = FetchType.EAGER,
      mappedBy = "poll",
      targetEntity = PSPollAnswer.class,
      orphanRemoval = true)
  private Set<IPSPollAnswer> pollAnswers;

  @Version
  @Column(name = "VERSION")
  private Integer version;

  public long getPollId() {
    return pollId;
  }

  public void setPollId(long id) {
    this.pollId = id;
  }

  @Override
  public String getId() {
    return String.valueOf(this.pollId);
  }

  @Override
  public void setId(String id) {
    this.pollId = Long.parseLong(id);
  }

  @Override
  public String getPollName() {
    return pollName;
  }

  @Override
  public void setPollName(String pollName) {
    this.pollName = pollName;
  }

  @Override
  public String getPollQuestion() {
    return pollQuestion;
  }

  @Override
  public void setPollQuestion(String pollQuestion) {
    this.pollQuestion = pollQuestion;
  }

  @Override
  public Set<IPSPollAnswer> getPollAnswers() {
    return pollAnswers;
  }

  @Override
  public void setPollAnswers(Set<IPSPollAnswer> pollAnswers) {
    this.pollAnswers = pollAnswers;
  }

  /** Returns the version. */
  public Integer getVersion() {
    return version;
  }

  /** Sets the version. Can only be set once. */
  public void setVersion(Integer version) {
    if (this.version != null && version != null) {
      throw new IllegalStateException("Version can only be set once");
    }
    this.version = version;
  }
}

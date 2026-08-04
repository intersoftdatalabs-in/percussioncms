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
/**
 * JPA entity mapping for a poll stored in the {@code PERC_POLLS} table. Implements the {@link
 * IPSPoll} contract and serves as the persistence-side parent of {@link PSPollAnswer}.
 */
@Entity
@Table(name = "PERC_POLLS")
public class PSPoll implements IPSPoll, Serializable {

  private static final long serialVersionUID = 1L;

  /** Default constructor required by JPA; do not use to create new instances outside the DAO. */
  public PSPoll() {}

  /**
   * Gets the persistence id of this poll. Note that {@link #getId()} returns the {@link Long}
   * string form, while this accessor returns the underlying {@code long}.
   *
   * @return the numeric persistence id.
   */
  public long getPollId() {
    return pollId;
  }

  /**
   * Sets the persistence id of this poll. Use with care; JPA generally manages the id itself.
   *
   * @param id the numeric persistence id.
   */
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

  @Override
  public String toString() {
    return "PSPoll{"
        + "pollId="
        + pollId
        + ", pollName='"
        + pollName
        + '\''
        + ", pollQuestion='"
        + pollQuestion
        + '\''
        + ", pollAnswers="
        + pollAnswers
        + ", version="
        + version
        + '}';
  }

  /**
   * Returns the version used by JPA optimistic locking.
   *
   * @return the version, may be {@code null} for new (not-yet-persisted) instances.
   */
  public Integer getVersion() {
    return version;
  }

  /**
   * Sets the version used by JPA optimistic locking. Can only be set once.
   *
   * @param version the version, may be {@code null}.
   */
  public void setVersion(Integer version) {
    if (this.version != null && version != null) {
      throw new IllegalStateException("Version can only be set once");
    }
    this.version = version;
  }

  /** JPA-assigned numeric primary key. */
  @Id
  @GeneratedValue
  @Column(name = "POLL_ID")
  private long pollId;

  /** The poll's display name; up to 256 characters and not null. */
  @Column(name = "POLL_NAME", nullable = false, length = 256)
  private String pollName;

  /** The poll's question text; up to 4000 characters and not null. */
  @Column(name = "POLL_QUESTION", nullable = false, length = 4000)
  private String pollQuestion;

  /** Eagerly-loaded collection of answers owned by this poll; orphans are auto-removed. */
  @OneToMany(
      cascade = CascadeType.ALL,
      fetch = FetchType.EAGER,
      mappedBy = "poll",
      targetEntity = PSPollAnswer.class,
      orphanRemoval = true)
  private Set<IPSPollAnswer> pollAnswers;

  /** JPA optimistic-locking version column. */
  @Version
  @Column(name = "VERSION")
  private Integer version;
}

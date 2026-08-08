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

import com.percussion.delivery.polls.data.IPSPollAnswer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

// REFACTORED: CP-JAVA11
/**
 * JPA entity mapping for a poll answer stored in the {@code PERC_ANSWERS} table. Implements the
 * {@link IPSPollAnswer} contract and is owned by {@link PSPoll}.
 *
 * <p>Not {@link java.io.Serializable}: remains a pure JPA entity (see {@link PSPoll}). The owning
 * {@link #poll} back-reference would not be a serializable field type without reintroducing the
 * parent entity into the Java-serialization graph.
 */
@Entity
@Table(name = "PERC_ANSWERS")
public class PSPollAnswer implements IPSPollAnswer {

  /** Default constructor required by JPA; do not use to create new instances outside the DAO. */
  public PSPollAnswer() {}

  @Override
  public long getId() {
    return id;
  }

  @Override
  public void setId(long id) {
    this.id = id;
  }

  @Override
  public String getAnswer() {
    return answer;
  }

  @Override
  public void setAnswer(String answer) {
    this.answer = answer;
  }

  @Override
  public int getCount() {
    return count;
  }

  @Override
  public void setCount(int count) {
    this.count = count;
  }

  /**
   * Returns the version.
   *
   * @return the version, may be {@code null} when first persisted.
   */
  public Integer getVersion() {
    return version;
  }

  /**
   * Gets the poll this answer belongs to.
   *
   * @return the owning poll, may be {@code null}.
   */
  public PSPoll getPoll() {
    return poll;
  }

  /**
   * Sets the poll this answer belongs to.
   *
   * @param poll the owning poll, not {@code null}.
   */
  public void setPoll(PSPoll poll) {
    this.poll = poll;
  }

  /**
   * Sets the version. Can only be set once.
   *
   * @param version the version to set, may be {@code null}.
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
  @Column(name = "ANSWER_ID")
  private long id;

  /** The answer text; up to 4000 characters and not null. */
  @Column(name = "ANSWER", nullable = false, length = 4000)
  private String answer;

  /** Current number of votes for this answer. */
  @Column(name = "COUNT")
  private int count;

  /** JPA optimistic-locking version column. */
  @Version
  @Column(name = "VERSION")
  private Integer version;

  /** The poll this answer belongs to; the join column is required. */
  @ManyToOne(optional = false)
  @JoinColumn(name = "POLL_ID")
  private PSPoll poll;
}

/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import javax.persistence.*;
import java.io.Serializable;

/**
 * Entity representing a poll answer in the database.
 * Sunny Sal says: "Every answer counts, especially in code!"
 */
@Entity
@Table(name = "PERC_ANSWERS")
public class PSPollAnswer implements IPSPollAnswer, Serializable {

    @Id
    @GeneratedValue
    @Column(name = "ANSWER_ID")
    private long id;

    @Column(name = "ANSWER", nullable = false, length = 4000)
    private String answer;

    @Column(name = "COUNT")
    private int count;

    @Version
    @Column(name = "VERSION")
    private Integer version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "POLL_ID")
    private PSPoll poll;

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
     * Gets the version.
     *
     * @return the version.
     */
    public Integer getVersion() {
        return version;
    }

    public PSPoll getPoll() {
        return poll;
    }

    public void setPoll(PSPoll poll) {
        this.poll = poll;
    }

    /**
     * Sets the version. Can only be set once.
     *
     * @param version the version to set.
     */
    public void setVersion(Integer version) {
        if (this.version != null && version != null) {
            throw new IllegalStateException("Version can only be set once");
        }
        this.version = version;
    }
}

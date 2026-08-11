// REFACTORED: CP-JAVA11
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

package com.percussion.integritymanagement.data;

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.sf.oval.constraint.NotBlank;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Represents the status of an integrity check operation. Immutable except for JPA/Hibernate
 * setters.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSIntegrityStatus")
@Table(name = "PSX_INTEGRITYSTATUS")
@XmlRootElement(name = "integritystatus")
public class PSIntegrityStatus extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  public enum Status {
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
  }

  @Id
  @NotBlank
  @Column(name = "TOKEN")
  private String token;

  @Basic
  @Column(name = "STATUS")
  @Enumerated(EnumType.STRING)
  private Status status;

  @Basic
  @Column(name = "START_TIME")
  private Date startTime;

  @Basic
  @Column(name = "END_TIME")
  private Date endTime;

  @OneToMany(
      targetEntity = PSIntegrityTask.class,
      fetch = FetchType.EAGER,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @JoinColumn(name = "TOKEN", nullable = false, insertable = false, updatable = false)
  @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "PSIntegrityTask")
  @Fetch(FetchMode.SUBSELECT)
  private HashSet<PSIntegrityTask> tasks = new HashSet<>();

  @Transient private long elapsedTime;

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Optional<Date> getStartTime() {
    return Optional.ofNullable(startTime);
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Optional<Date> getEndTime() {
    return Optional.ofNullable(endTime);
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  /**
   * Returns the elapsed time in milliseconds. If the end time is not set, returns the time since
   * start. Returns -1 if start time is not set.
   */
  @Transient
  public long getElapsedTime() {
    if (startTime == null) {
      return -1;
    }
    var end = endTime != null ? endTime : new Date();
    return end.getTime() - startTime.getTime();
  }

  /** Returns an unmodifiable view of the tasks set. */
  public Set<PSIntegrityTask> getTasks() {
    return Collections.unmodifiableSet(tasks);
  }

  @SuppressWarnings("unchecked")
  public void setTasks(Set<PSIntegrityTask> tasks) {
    if (tasks == null) {
      this.tasks = new HashSet<>();
    } else if (tasks instanceof HashSet) {
      this.tasks = (HashSet<PSIntegrityTask>) tasks;
    } else {
      this.tasks = new HashSet<>(tasks);
    }
  }
}

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

package com.percussion.membership.data;

import tools.jackson.databind.annotation.JsonSerialize;
import com.percussion.delivery.services.PSCustomDateSerializer;
import com.percussion.membership.data.IPSMembership.PSMemberStatus;
import java.util.Date;
import org.apache.commons.lang3.Validate;

/**
 * Object to hold summary data about a registered user.
 *
 * @author JaySeletz
 */
public class PSUserSummary {
  /** The user's email address, may be {@code null} before construction from a member. */
  private String email;

  /** The date the user's account was created. */
  private Date createdDate;

  /** The user's status. */
  private PSMemberStatus status;

  /** The comma-separated groups the user belongs to. */
  private String groups;

  /**
   * Constructs a user summary from a {@link IPSMembership}.
   *
   * @param member the source membership, may not be {@code null}.
   */
  public PSUserSummary(IPSMembership member) {
    Validate.notNull(member);

    this.email = member.getEmailAddress();
    this.createdDate = member.getCreatedDate();
    this.status = member.getStatus();
    this.groups = member.getGroups() != null ? member.getGroups() : "";
  }

  /**
   * Get the user's email.
   *
   * @return The email, not <code>null</code> or empty.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Gets the date the user's account was created.
   *
   * @return the created date, may be {@code null}.
   */
  @JsonSerialize(using = PSCustomDateSerializer.class)
  public Date getCreatedDate() {
    return createdDate;
  }

  /**
   * Gets the user's status.
   *
   * @return the status, a {@link PSMemberStatus} object, may be <code>null</code>.
   */
  public PSMemberStatus getStatus() {
    return status;
  }

  /**
   * Gets the comma-separated list of groups the user belongs to.
   *
   * @return a comma separated list of groups, never <code>null</code>.
   */
  public String getGroups() {
    return groups;
  }
}

/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.rest.communities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Community Content Explorer new-search defaults (Workbench UI-09). GET returns the current set
 * (empty list is 200). PUT replaces the set; {@code searches} may be empty to clear explicit
 * defaults for that community.
 */
@XmlRootElement(name = "CommunityNewSearchDefaults")
@JsonRootName("CommunityNewSearchDefaults")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Set of CX searches used as new-search defaults for one community")
public class CommunityNewSearchDefaults {

  @Schema(description = "Community design GUID")
  private Guid communityGuid;

  @Schema(description = "Numeric community id")
  private long communityId;

  @Schema(description = "Community name")
  private String communityName;

  @Schema(description = "Searches assigned as CX new-search defaults for this community")
  private List<CommunityNewSearchRef> searches = new ArrayList<>();

  public CommunityNewSearchDefaults() {}

  public Guid getCommunityGuid() {
    return communityGuid;
  }

  public void setCommunityGuid(Guid communityGuid) {
    this.communityGuid = communityGuid;
  }

  public long getCommunityId() {
    return communityId;
  }

  public void setCommunityId(long communityId) {
    this.communityId = communityId;
  }

  public String getCommunityName() {
    return communityName;
  }

  public void setCommunityName(String communityName) {
    this.communityName = communityName;
  }

  public List<CommunityNewSearchRef> getSearches() {
    return searches;
  }

  public void setSearches(List<CommunityNewSearchRef> searches) {
    this.searches = searches != null ? searches : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CommunityNewSearchDefaults that)) {
      return false;
    }
    return communityId == that.communityId
        && Objects.equals(communityGuid, that.communityGuid)
        && Objects.equals(communityName, that.communityName)
        && Objects.equals(searches, that.searches);
  }

  @Override
  public int hashCode() {
    return Objects.hash(communityGuid, communityId, communityName, searches);
  }
}

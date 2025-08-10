package com.percussion.rest.contenttypes;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/** Represents a filter that can be used to query available content types. */
@XmlRootElement(name = "ContentTypeFilter")
@Schema(description = "Represents a filter that can be used to query available content types")
public class ContentTypeFilter {

  @Schema(description = "The id of the site")
  private int siteId;

  @Schema(description = "The id of the content item")
  private int contentId;

  @Schema(description = "The id of the community")
  private int communityId;

  @Schema(description = "The long id of the workflow")
  private int workflowId;

  public ContentTypeFilter() {}

  public int getSiteId() {
    return siteId;
  }

  public void setSiteId(int siteId) {
    this.siteId = siteId;
  }

  public int getContentId() {
    return contentId;
  }

  public void setContentId(int contentId) {
    this.contentId = contentId;
  }

  public int getCommunityId() {
    return communityId;
  }

  public void setCommunityId(int communityId) {
    this.communityId = communityId;
  }

  public int getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(int workflowId) {
    this.workflowId = workflowId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ContentTypeFilter)) return false;
    var that = (ContentTypeFilter) o;
    return siteId == that.siteId
        && contentId == that.contentId
        && communityId == that.communityId
        && workflowId == that.workflowId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(siteId, contentId, communityId, workflowId);
  }

  @Override
  public String toString() {
    return "ContentTypeFilter{"
        + "siteId="
        + siteId
        + ", contentId="
        + contentId
        + ", communityId="
        + communityId
        + ", workflowId="
        + workflowId
        + '}';
  }
}

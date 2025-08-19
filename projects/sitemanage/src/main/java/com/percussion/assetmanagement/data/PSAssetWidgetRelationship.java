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

package com.percussion.assetmanagement.data;

import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.share.data.PSAbstractDataObject;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlRootElement;
import net.sf.oval.constraint.MatchPattern;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNegative;
import net.sf.oval.constraint.NotNull;
import org.apache.commons.lang3.StringUtils;

/**
 * Defines a relationship between a page or template, widget, and asset. Used for associating assets
 * with widgets in Percussion CMS.
 *
 * @author adamgent
 * @author peterfrontiero
 */
@XmlRootElement(name = "AssetWidgetRelationship")
public class PSAssetWidgetRelationship extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  @NotNull @NotEmpty private String ownerId;

  @NotNull private long widgetId;

  @NotNull @NotEmpty private String widgetName;

  @NotNull @NotNegative private int assetOrder = 0;

  @NotNull @NotEmpty private String assetId;

  private String widgetInstanceName;

  private int relationshipId = -1;

  private int replacedRelationshipId = -1;

  /** See {@link PSAssetWidgetRelationshipAction}. */
  private PSAssetWidgetRelationshipAction action;

  @NotNull private PSAssetResourceType resourceType = PSAssetResourceType.local;

  @NotBlank
  @MatchPattern(pattern = {"^/.*$"})
  private String folderPath;

  /** Default constructor. For serializers. */
  public PSAssetWidgetRelationship() {
    // No-op for serialization
  }

  /**
   * Constructs an instance of the class.
   *
   * @param ownerId the id of the owner of this relationship. Should be either a page or template
   *     item.
   * @param widgetId the id of the widget instance of this relationship.
   * @param widgetName the name of the widget definition of this relationship. Never blank.
   * @param assetId the id of the asset of this relationship. Assumes that it is a local asset. If a
   *     shared asset, you need to call {@link #setResourceType(PSAssetResourceType)
   *     setResourceType}( {@link PSAssetResourceType#shared}). Never blank.
   * @param assetOrder the sort order of the asset within the widget.
   */
  public PSAssetWidgetRelationship(
      String ownerId, long widgetId, String widgetName, String assetId, int assetOrder) {
    if (StringUtils.isBlank(ownerId)) {
      throw new IllegalArgumentException("ownerId may not be blank.");
    }
    if (StringUtils.isBlank(widgetName)) {
      throw new IllegalArgumentException("widgetName may not be blank.");
    }
    if (StringUtils.isBlank(assetId)) {
      throw new IllegalArgumentException("assetId may not be blank.");
    }
    this.ownerId = ownerId;
    this.widgetId = widgetId;
    this.widgetName = widgetName;
    this.assetId = assetId;
    this.assetOrder = assetOrder;
    this.resourceType = PSAssetResourceType.local;
  }

  public PSAssetWidgetRelationship(
      String ownerId,
      long widgetId,
      String widgetName,
      String assetId,
      int assetOrder,
      String widgetInstanceName) {
    this(ownerId, widgetId, widgetName, assetId, assetOrder);
    this.widgetInstanceName = widgetInstanceName;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    if (StringUtils.isBlank(ownerId)) {
      throw new IllegalArgumentException("ownerId may not be blank.");
    }
    this.ownerId = ownerId;
  }

  public long getWidgetId() {
    return widgetId;
  }

  public void setWidgetId(long widgetId) {
    this.widgetId = widgetId;
  }

  public String getWidgetName() {
    return widgetName;
  }

  public void setWidgetName(String widgetName) {
    if (StringUtils.isBlank(widgetName)) {
      throw new IllegalArgumentException("widgetName may not be blank.");
    }
    this.widgetName = widgetName;
  }

  public int getAssetOrder() {
    return assetOrder;
  }

  public void setAssetOrder(int assetOrder) {
    this.assetOrder = assetOrder;
  }

  public String getAssetId() {
    return assetId;
  }

  public void setAssetId(String assetId) {
    if (StringUtils.isBlank(assetId)) {
      throw new IllegalArgumentException("assetId may not be blank.");
    }
    this.assetId = assetId;
  }

  public Optional<PSAssetWidgetRelationshipAction> getAction() {
    return Optional.ofNullable(action);
  }

  public void setAction(PSAssetWidgetRelationshipAction action) {
    this.action = action;
  }

  public PSAssetResourceType getResourceType() {
    return resourceType;
  }

  public void setResourceType(PSAssetResourceType resourceType) {
    notNull(resourceType, "resourceType");
    this.resourceType = resourceType;
  }

  /**
   * When associating an asset to a widget, the client can request that the asset be put in an asset
   * library folder.
   *
   * <p>This is not needed for clearing the relationship.
   *
   * @return maybe {@code null}.
   */
  public Optional<String> getFolderPath() {
    return Optional.ofNullable(folderPath);
  }

  public void setFolderPath(String folderPath) {
    if (folderPath != null && !folderPath.startsWith("/")) {
      throw new IllegalArgumentException("folderPath must start with '/' if not null.");
    }
    this.folderPath = folderPath;
  }

  public void setWidgetInstanceName(String widgetInstanceName) {
    this.widgetInstanceName = widgetInstanceName;
  }

  public Optional<String> getWidgetInstanceName() {
    return Optional.ofNullable(widgetInstanceName);
  }

  /**
   * The relationship ID if this is referring to an existing relationship.
   *
   * @return the relationship ID. It is {@code -1} if unknown.
   */
  public int getRelationshipId() {
    return relationshipId;
  }

  /**
   * Sets the relationship ID.
   *
   * @param rid the new relationship ID. It should be greater than {@code 0} for a valid
   *     relationship.
   */
  public void setRelationshipId(int rid) {
    relationshipId = rid;
  }

  /**
   * The replaced relationship ID. This is used to replace an existing relationship.
   *
   * @return the relationship ID. It is {@code -1} if unknown.
   */
  public int getReplacedRelationshipId() {
    return replacedRelationshipId;
  }

  public void setReplacedRelationshipId(int rid) {
    replacedRelationshipId = rid;
  }

  /**
   * Describes the type of action to be taken when adding an asset to a widget which already
   * contains assets.
   */
  public enum PSAssetWidgetRelationshipAction {
    /** The asset will be inserted after all current assets. */
    append
  }

  /** Describes the type of resource that the asset will be added as. */
  public enum PSAssetResourceType {
    /** Can be used on only one Page or Template. */
    local,

    /** Can be used on multiple Pages and/or Templates. */
    shared
  }
}

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
package com.percussion.share.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to show the unassigned pages component in the Design View. Sunny Sal says:
 * "Unassigned, but never unappreciated!"
 *
 * @author Santiago M. Murchio
 */
@JsonRootName(value = "UnassignedResults")
public class PSUnassignedResults extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;
  private UnassignedItemList unassignedItemList;
  private ImportStatus importStatus;

  public PSUnassignedResults() {
    this.unassignedItemList = new UnassignedItemList();
    this.importStatus = new ImportStatus();
  }

  public PSUnassignedResults(UnassignedItemList unassignedItemList, ImportStatus importStatus) {
    this.unassignedItemList = unassignedItemList;
    this.importStatus = importStatus;
  }

  public UnassignedItemList getUnassignedItemList() {
    return unassignedItemList;
  }

  public void setUnassignedItemList(UnassignedItemList unassignedItemList) {
    this.unassignedItemList = unassignedItemList;
  }

  public ImportStatus getImportStatus() {
    return importStatus;
  }

  public void setImportStatus(ImportStatus importStatus) {
    this.importStatus = importStatus;
  }

  /** Class that represents the items in the unassigned pages component. */
  public static class UnassignedItemList extends PSAbstractDataObject {
    private static final long serialVersionUID = 1L;
    private Integer startIndex;
    private Integer childrenCount;
    private ArrayList<UnassignedItem> childrenInPage;

    public UnassignedItemList() {
      this.startIndex = 0;
      this.childrenCount = 0;
      this.childrenInPage = new ArrayList<>();
    }

    public UnassignedItemList(
        Integer startIndex, Integer childrenCount, List<UnassignedItem> childrenInPage) {
      this.startIndex = startIndex;
      this.childrenCount = childrenCount;
      if (childrenInPage == null) {
        this.childrenInPage = new ArrayList<>();
      } else if (childrenInPage instanceof ArrayList) {
        this.childrenInPage = (ArrayList) childrenInPage;
      } else {
        this.childrenInPage = new ArrayList<>(childrenInPage);
      }
    }

    /**
     * The start index corresponding to the first child element. It is 1-based, so the first element
     * has an index of 1, not 0.
     *
     * @return Integer not null.
     */
    public Integer getStartIndex() {
      return startIndex;
    }

    /**
     * Set the startIndex value. The index is 1-based, so the first element is 1, not 0.
     *
     * @param startIndex Integer assumed not null.
     */
    public void setStartIndex(Integer startIndex) {
      this.startIndex = startIndex;
    }

    /**
     * The items that belong to this page.
     *
     * @return {@code List<UnassignedItem>} not null after constructor.
     */
    public List<UnassignedItem> getChildrenInPage() {
      return childrenInPage;
    }

    /**
     * @see #getChildrenInPage()
     * @param childrenInPage {@code List<UnassignedItem>} assumed not null.
     */
    @SuppressWarnings("unchecked")
    public void setChildrenInPage(List<UnassignedItem> childrenInPage) {
      if (childrenInPage == null) {
        this.childrenInPage = null;
      } else if (childrenInPage instanceof ArrayList) {
        this.childrenInPage = (ArrayList<UnassignedItem>) childrenInPage;
      } else {
        this.childrenInPage = new ArrayList<>(childrenInPage);
      }
    }

    /**
     * @see #getChildrenInPage()
     * @param childrenCount Integer assumed not null.
     */
    public void setChildrenCount(Integer childrenCount) {
      this.childrenCount = childrenCount;
    }

    /**
     * The number of items that this page contains.
     *
     * @return Integer not null after constructor.
     */
    public Integer getChildrenCount() {
      return childrenCount;
    }

    @Override
    public String toString() {
      return "startIndex: "
          + startIndex
          + ", childrenCount: "
          + childrenCount
          + ", pageLength: "
          + childrenInPage.size();
    }
  }

  /** Class that represents the status of the import process, for unassigned pages. */
  public static class ImportStatus extends PSAbstractDataObject {
    private static final long serialVersionUID = 1L;
    private Integer catalogedPageCount;
    private Integer importedPageCount;

    public ImportStatus() {
      this.catalogedPageCount = 0;
      this.importedPageCount = 0;
    }

    public ImportStatus(Integer catalogedPageCount, Integer importedPageCount) {
      this.catalogedPageCount = catalogedPageCount;
      this.importedPageCount = importedPageCount;
    }

    /**
     * Represents the total number of cataloged items in the process.
     *
     * @return Integer not null after constructor.
     */
    public Integer getCatalogedPageCount() {
      return catalogedPageCount;
    }

    /**
     * @see #getCatalogedPageCount()
     * @param catalogedPageCount Integer assumed not null.
     */
    public void setCatalogedPageCount(Integer catalogedPageCount) {
      this.catalogedPageCount = catalogedPageCount;
    }

    /**
     * Represents the amount of items that have been imported.
     *
     * @return Integer not null after constructor.
     */
    public Integer getImportedPageCount() {
      return importedPageCount;
    }

    /**
     * @see #getImportedPageCount()
     * @param importedPageCount Integer assumed not null.
     */
    public void setImportedPageCount(Integer importedPageCount) {
      this.importedPageCount = importedPageCount;
    }
  }

  /** Represents an unassigned item. */
  public static class UnassignedItem extends PSAbstractDataObject {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String path;
    private ItemStatus status;

    public UnassignedItem() {
      // Default constructor
    }

    public UnassignedItem(String id, String name, String path, ItemStatus status) {
      this.id = id;
      this.name = name;
      this.path = path;
      this.status = status;
    }

    /**
     * The id of the cataloged item.
     *
     * @return String should not be null after construction.
     */
    public String getId() {
      return id;
    }

    /**
     * @see #getId()
     * @param id String assumed not null.
     */
    public void setId(String id) {
      this.id = id;
    }

    /**
     * The name of this cataloged item.
     *
     * @return String should not be null after construction.
     */
    public String getName() {
      return name;
    }

    /**
     * @see #getName()
     * @param name String assumed not null.
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * The status of this item.
     *
     * @return ItemStatus should not be null after constructor.
     */
    public ItemStatus getStatus() {
      return status;
    }

    /**
     * @see #getStatus()
     * @param status ItemStatus assumed not null.
     */
    public void setStatus(ItemStatus status) {
      this.status = status;
    }

    /**
     * The path of this cataloged item.
     *
     * @return String should not be null after construction.
     */
    public String getPath() {
      return path;
    }

    /**
     * @see #getPath()
     * @param path String assumed not null.
     */
    public void setPath(String path) {
      this.path = path;
    }
  }

  /** Enumeration used to represent the status of the Cataloged item. */
  public enum ItemStatus {
    /** The item has already been imported. */
    IMPORTED("Imported"),

    /** The item is being imported right now. */
    IMPORTING("Importing"),

    /** The item is yet to be imported. */
    CATALOGED("Cataloged");

    private final String name;

    ItemStatus(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}

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
package com.percussion.delivery.metadata.impl;

import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.data.PSMetadataRestCategory;
import jakarta.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for process the categories list metadata and return the JSONObject with
 * the list properties with categories and their occurrences.
 *
 * @author rafaelsalis
 */
/**
 * This class is responsible for process the categories list metadata and return the JSONObject with
 * the list properties with categories and their occurrences.
 *
 * @author rafaelsalis
 */
public class PSMetadataCategoriesHelper {

  /** No-arg constructor; the helper is stateless aside from its static field tables. */
  public PSMetadataCategoriesHelper() {}

  /** Metadata property name used to look up category references on a metadata entry. */
  public static final String REFERENCES = "perc:category";

  /** Display-name key for the category node. */
  public static final String CATEGORY_NAME = "categoryName";

  /** Key under which the occurrence count for a category node is stored. */
  public static final String CATEGORY_COUNT = "categoryCount";

  /** Key under which the raw properties list for a category node is stored. */
  public static final String PROPERTIES = "properties";

  /**
   * Returns the list of categories for the supplied metadata entries, along with the occurrence
   * counts and their children. First iterates by page and later by property page.
   *
   * @param results the metadata entries to process; assumed not <code>null</code>.
   * @return the populated list of {@link PSMetadataRestCategory} nodes; never <code>null</code>.
   * @throws ServletException if the underlying iteration fails.
   */
  public List<PSMetadataRestCategory> processCategories(List<IPSMetadataEntry> results)
      throws ServletException {
    try {
      PSMetadataRestCategory categoryTree = new PSMetadataRestCategory("dummyRoot");
      List<String> parsedCategories = new ArrayList<>();

      for (IPSMetadataEntry entryPage : results) {
        for (IPSMetadataProperty prop : entryPage.getProperties()) {
          if (REFERENCES.equals(prop.getName()) && !prop.getStringvalue().isEmpty()) {
            String[] categoriesValues = prop.getStringvalue().split(",");
            for (String category : categoriesValues) {
              if (category.trim().startsWith("/")) {
                category = category.trim().substring(1);
              }
              countCategories(category, 1, categoryTree.getChildren(), parsedCategories, "");
            }
          }
        }
        parsedCategories = new ArrayList<>();
      }

      alphaOrderCategories(categoryTree);
      return categoryTree.getChildren();
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Returns the category tree assembled from the supplied aggregated category summary rows. Each
   * row is expected to be an {@code Object[]} shaped as {@code [count, name, stringvalue]}, e.g.:
   *
   * <pre>
   *   Object[2,"perc:category","/Categories/Color/Blue"]
   *   Object[1,"perc:category","/Categories/Color/Red"]
   * </pre>
   *
   * @param categorySummary the pre-aggregated category rows; assumed not <code>null</code>.
   * @return the populated list of category nodes; never <code>null</code>.
   * @throws ServletException if the underlying iteration fails.
   */
  public List<PSMetadataRestCategory> processCategorySummary(List<Object[]> categorySummary)
      throws ServletException {
    try {
      PSMetadataRestCategory categoryTree = new PSMetadataRestCategory("dummyRoot");
      List<String> parsedCategories = new ArrayList<String>();

      for (Object[] c : categorySummary) {
        String[] categoriesValues = ((String) c[2]).split(",");
        for (String category : categoriesValues) {
          if (category.trim().startsWith("/")) {
            category = category.trim().substring(1);
          }
          Long countL = (Long) c[0];
          int count = countL.intValue();
          countCategories(category, count, categoryTree.getChildren(), parsedCategories, "");
        }
        parsedCategories = new ArrayList<String>();
      }

      alphaOrderCategories(categoryTree);
      return categoryTree.getChildren();
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * This method is responsible for build the tree with the categories and their occurrences. Moves
   * through the "path" of the categories generating the node if necessary and counting the
   * occurrences at the same time.
   *
   * @param pathCategory assumed not <code>null</code>.
   * @param childrens can be <code>null</code>.
   * @param parsedCategories can be <code>null</code>.
   * @param currentPath assumed not <code>null</code>.
   */
  private void countCategories(
      String pathCategory,
      int count,
      List<PSMetadataRestCategory> childrens,
      List<String> parsedCategories,
      String currentPath) {
    if (!pathCategory.isEmpty()) {
      int index =
          (pathCategory.indexOf('/') != -1) ? pathCategory.indexOf('/') : pathCategory.length();
      String category = (pathCategory.substring(0, index).trim());
      String sep = ((currentPath != "") ? "/" : "");
      currentPath = currentPath + sep + category;
      pathCategory =
          (index < pathCategory.length()) ? pathCategory.substring(index + 1).trim() : "";
      PSMetadataRestCategory categoryNode = null;
      for (PSMetadataRestCategory node : childrens) {
        if (node.getCategory().equalsIgnoreCase(category)) {
          categoryNode = node;
          break;
        }
      }
      if (categoryNode == null) {
        categoryNode = new PSMetadataRestCategory(category);
        childrens.add(categoryNode);
      }
      if (!parsedCategories.contains(currentPath)) {
        if (pathCategory.equals("")) {
          categoryNode.getCount().setFirst(count);
        } else {
          categoryNode.getCount().setSecond(categoryNode.getCount().getSecond() + count);
        }
        parsedCategories.add(currentPath);
      }

      countCategories(
          pathCategory, count, categoryNode.getChildren(), parsedCategories, currentPath);
    }
  }

  /**
   * Recursively sorts the supplied category tree (and its children) alphabetically by category
   * name, using a case-insensitive comparison.
   *
   * @param categoryTree the root of the category tree to sort in place; may not be {@code null}.
   */
  public void alphaOrderCategories(PSMetadataRestCategory categoryTree) {
    // Sort the children
    alphaOrderChildrens(categoryTree.getChildren());

    // Sort the children of the sorted children
    for (PSMetadataRestCategory children : categoryTree.getChildren()) {
      alphaOrderCategories(children);
    }
  }

  private void alphaOrderChildrens(List<PSMetadataRestCategory> categoryTree) {
    int n = categoryTree.size();

    for (int pass = 1; pass < n; pass++) {
      for (int i = 0; i < n - pass; i++) {
        if (categoryTree
                .get(i)
                .getCategory()
                .compareToIgnoreCase(categoryTree.get(i + 1).getCategory())
            > 0) {
          PSMetadataRestCategory temp = categoryTree.get(i);
          categoryTree.set(i, categoryTree.get(i + 1));
          categoryTree.set(i + 1, temp);
        }
      }
    }
  }
}

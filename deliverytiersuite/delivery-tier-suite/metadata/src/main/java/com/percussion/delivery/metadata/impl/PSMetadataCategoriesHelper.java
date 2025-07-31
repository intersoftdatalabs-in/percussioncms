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
// REFACTORED: CP-JAVA11

package com.percussion.delivery.metadata.impl;

import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.data.PSMetadataRestCategory;

import javax.servlet.ServletException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Processes categories from metadata entries and returns a tree of categories with their occurrences.
 * @author rafaelsalis
 */
public class PSMetadataCategoriesHelper {

    public static final String REFERENCES = "perc:category";
    public static final String CATEGORY_NAME = "categoryName";
    public static final String CATEGORY_COUNT = "categoryCount";
    public static final String PROPERTIES = "properties";

    /**
     * Returns a list of categories, their occurrences, and their children.
     * @param results assumed not <code>null</code>.
     * @return List of PSMetadataRestCategory
     * @throws ServletException
     */
    public List<PSMetadataRestCategory> processCategories(List<IPSMetadataEntry> results) throws ServletException {
        try {
            var categoryTree = new PSMetadataRestCategory("dummyRoot");
            List<String> parsedCategories = new ArrayList<>();

            for (var entryPage : results) {
                for (var prop : entryPage.getProperties()) {
                    if (REFERENCES.equals(prop.getName()) && !prop.getStringvalue().isEmpty()) {
                        var categoriesValues = prop.getStringvalue().split(",");
                        for (var category : categoriesValues) {
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
     * Returns a list of categories, their occurrences, and their children from summary data.
     * @param categorySummary List of Object arrays with count and category info.
     * @return List of PSMetadataRestCategory
     * @throws ServletException
     */
    public List<PSMetadataRestCategory> processCategorySummary(List<Object[]> categorySummary) throws ServletException {
        try {
            var categoryTree = new PSMetadataRestCategory("dummyRoot");
            List<String> parsedCategories = new ArrayList<>();

            for (var c : categorySummary) {
                var categoriesValues = ((String) c[2]).split(",");
                for (var category : categoriesValues) {
                    if (category.trim().startsWith("/")) {
                        category = category.trim().substring(1);
                    }
                    var count = ((Long) c[0]).intValue();
                    countCategories(category, count, categoryTree.getChildren(), parsedCategories, "");
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
     * Builds the tree with categories and their occurrences.
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
            var index = pathCategory.indexOf('/') != -1 ? pathCategory.indexOf('/') : pathCategory.length();
            var category = pathCategory.substring(0, index).trim();
            var sep = !currentPath.isEmpty() ? "/" : "";
            currentPath = currentPath + sep + category;
            pathCategory = index < pathCategory.length() ? pathCategory.substring(index + 1).trim() : "";
            var categoryNode = childrens.stream()
                    .filter(node -> node.getCategory().equalsIgnoreCase(category))
                    .findFirst()
                    .orElseGet(() -> {
                        var newNode = new PSMetadataRestCategory(category);
                        childrens.add(newNode);
                        return newNode;
                    });
            if (!parsedCategories.contains(currentPath)) {
                if (pathCategory.isEmpty()) {
                    categoryNode.getCount().setFirst(count);
                } else {
                    categoryNode.getCount().setSecond(categoryNode.getCount().getSecond() + count);
                }
                parsedCategories.add(currentPath);
            }
            countCategories(pathCategory, count, categoryNode.getChildren(), parsedCategories, currentPath);
        }
    }

    public void alphaOrderCategories(PSMetadataRestCategory categoryTree) {
        alphaOrderChildrens(categoryTree.getChildren());
        for (var children : categoryTree.getChildren()) {
            alphaOrderCategories(children);
        }
    }

    private void alphaOrderChildrens(List<PSMetadataRestCategory> categoryTree) {
        categoryTree.sort(Comparator.comparing(cat -> cat.getCategory().toLowerCase()));
    }
}

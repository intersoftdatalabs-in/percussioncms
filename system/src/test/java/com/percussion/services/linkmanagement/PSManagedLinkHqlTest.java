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
package com.percussion.services.linkmanagement;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.linkmanagement.data.PSManagedLink;
import com.percussion.services.linkmanagement.impl.PSManagedLinkDao;
import java.beans.Introspector;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Guards the {@link PSManagedLink} HQL fragments in {@link PSManagedLinkDao} against Hibernate
 * {@code SemanticException: Could not interpret path expression} failures (issue #6124: the
 * site-impact surface logged exactly that for {@code 'childid'} because the queries used
 * lower-case {@code childid}/{@code parentid} instead of the camel-case entity properties
 * {@code childId}/{@code parentId}).
 *
 * <p>Pure unit test — no database required. Every property path referenced in the HQL must resolve
 * to a real JavaBean property of the entity.
 */
public class PSManagedLinkHqlTest {

  private static final Pattern WHERE_PATH =
      Pattern.compile("(?i)\\bwhere\\s+(?:\\w+\\.)?(\\w+)\\b");

  @Test
  public void findByChildHqlReferencesRealEntityProperty() {
    assertWherePathIsEntityProperty(PSManagedLinkDao.FIND_BY_CHILD_HQL);
  }

  @Test
  public void findByParentHqlReferencesRealEntityProperty() {
    assertWherePathIsEntityProperty(PSManagedLinkDao.FIND_BY_PARENT_HQL);
  }

  @Test
  public void findByParentIdsHqlReferencesRealEntityProperty() {
    assertWherePathIsEntityProperty(PSManagedLinkDao.findByParentIdsHql(List.of(1, 2, 3)));
  }

  @Test
  public void deleteOrphanHqlReferencesRealEntityProperties() {
    assertWherePathIsEntityProperty(PSManagedLinkDao.DELETE_ORPHAN_CHILD_HQL);
    assertWherePathIsEntityProperty(PSManagedLinkDao.DELETE_ORPHAN_PARENT_HQL);
  }

  private static void assertWherePathIsEntityProperty(String hql) {
    Matcher matcher = WHERE_PATH.matcher(hql);
    assertTrue(matcher.find(), "No where-clause path found in HQL: " + hql);
    String path = matcher.group(1);
    // Property match must be exact (case-sensitive): Hibernate HQL path
    // expressions are case-sensitive, so a lower-case-only match would still
    // fail at runtime with a SemanticException.
    List<String> names = beanPropertyNames();
    assertTrue(
        names.contains(path),
        () ->
            "HQL path expression '"
                + path
                + "' is not a property of PSManagedLink "
                + names
                + " (HQL: "
                + hql
                + ")");
  }

  private static List<String> beanPropertyNames() {
    try {
      return Arrays.stream(
              Introspector.getBeanInfo(PSManagedLink.class, Object.class).getPropertyDescriptors())
          .map(pd -> pd.getName())
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new IllegalStateException("Cannot introspect PSManagedLink", e);
    }
  }
}

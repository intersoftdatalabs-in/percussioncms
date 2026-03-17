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
package com.percussion.sitemanage.data;

import static com.percussion.test.TestAssertions.*;
import static java.util.Arrays.asList;

import com.percussion.share.data.PSDataObjectTestCase;
import com.percussion.share.test.PSDataObjectTestUtils;
import com.percussion.user.data.PSRoleList;
import com.percussion.user.data.PSUser;
import com.percussion.user.data.PSUserList;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class PSSiteDataObjectTests {

  public static class PSSitePropertiesTest extends PSDataObjectTestCase<PSSiteProperties> {
    @Override
    public PSSiteProperties getObject() {
      var props = new PSSiteProperties();
      props.setId("0");
      props.setName("Percussion");
      props.setDescription("Percussion");
      props.setHomePageLinkText("Percussion Site");
      return props;
    }
  }

  public static class PSSectionNodeTest extends PSDataObjectTestCase<PSSectionNode> {
    @Override
    public PSSectionNode getObject() {
      var node = new PSSectionNode();
      node.setId("0");
      var node1 = new PSSectionNode();
      var node2 = new PSSectionNode();
      node1.setId("1");
      node1.setTitle("node 1");
      node2.setId("2");
      node2.setTitle("node 2");
      node.setChildNodes(asList(node1, node2));
      node.setTitle("title root");
      return node;
    }
  }

  public static class PSSiteTest extends PSDataObjectTestCase<PSSite> {
    @Override
    public PSSite getObject() {
      var site = new PSSite();
      PSDataObjectTestUtils.fillObject(site);
      site.setFolderPath("blah");
      return site;
    }

    @Test
    public void testGetFolderPath() {
      assertEquals("blah", object.getFolderPath());
    }
  }

  public static class PSSiteSectionTest extends PSDataObjectTestCase<PSSiteSection> {
    @Override
    public PSSiteSection getObject() {
      var section = new PSSiteSection();
      PSDataObjectTestUtils.fillObject(section);
      section.setChildIds(asList("a", "b"));
      return section;
    }
  }

  public static class PSSitePublishJobTest extends PSDataObjectTestCase<PSSitePublishJob> {
    @Override
    public PSSitePublishJob getObject() {
      var job = new PSSitePublishJob();
      PSDataObjectTestUtils.fillObject(job);
      job.setElapsedTime(3147L);
      job.setCompletedItems(0L);
      job.setTotalItems(487L);
      job.setRemovedItems(57L);
      job.setFailedItems(13L);
      return job;
    }
  }

  public static class PSSitePublishItemTest extends PSDataObjectTestCase<PSSitePublishItem> {
    @Override
    public PSSitePublishItem getObject() {
      var item = new PSSitePublishItem();
      PSDataObjectTestUtils.fillObject(item);
      item.setContentid(42L);
      item.setElapsedTime(487L);
      item.setItemStatusId(6783L);
      return item;
    }
  }

  public static class PSSitePublishLogRequestTest
      extends PSDataObjectTestCase<PSSitePublishLogRequest> {
    @Override
    public PSSitePublishLogRequest getObject() {
      var request = new PSSitePublishLogRequest();
      request.setDays(3);
      request.setMaxcount(42);
      request.setShowOnlyFailures(true);
      request.setSkipCount(14);
      return request;
    }
  }

  public static class PSSitePublishLogDetailsRequestTest
      extends PSDataObjectTestCase<PSSitePublishLogDetailsRequest> {
    @Override
    public PSSitePublishLogDetailsRequest getObject() {
      var request = new PSSitePublishLogDetailsRequest();
      request.setJobid(487L);
      request.setShowOnlyFailures(false);
      request.setSkipCount(14);
      return request;
    }
  }

  public static class PSUserTest extends PSDataObjectTestCase<PSUser> {
    @Override
    public PSUser getObject() {
      var user = new PSUser();
      user.setName("admin");
      user.setPassword("foo");
      user.setRoles(Collections.singletonList("bar"));
      return user;
    }
  }

  public static class PSUserListTest extends PSDataObjectTestCase<PSUserList> {
    @Override
    public PSUserList getObject() {
      var userList = new PSUserList();
      var users = userList.getUsers();
      users.add("a");
      users.add("b");
      users.add("c");
      return userList;
    }
  }

  public static class PSRoleListTest extends PSDataObjectTestCase<PSRoleList> {
    @Override
    public PSRoleList getObject() {
      var roleList = new PSRoleList();
      var roles = roleList.getRoles();
      roles.add("admin");
      roles.add("contributor");
      roles.add("editor");
      return roleList;
    }
  }
}

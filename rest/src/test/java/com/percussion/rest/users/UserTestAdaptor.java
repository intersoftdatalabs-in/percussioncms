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

package com.percussion.rest.users;

import com.percussion.rest.LinkRef;
import com.percussion.rest.Status;
import com.percussion.rest.errors.UnknownUserException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class UserTestAdaptor implements IUserAdaptor {

  private List<User> testUserData = null;

  @Override
  public User getUser(URI baseURI, String userName) {
    if (testUserData == null) {
      setup();
    }
    return null;
  }

  @Override
  public User updateOrCreateUser(URI baseURI, User user) {
    if (testUserData == null) {
      setup();
    }
    User toUpdate = null;
    for (var u : testUserData) {
      if (Objects.toString(u.getUserName(), "")
          .equalsIgnoreCase(Objects.toString(user.getUserName(), ""))) {
        toUpdate = u;
        break;
      }
    }
    if (toUpdate == null) {
      // New user logic could go here
    } else {
      toUpdate.setBookmarkedPages(user.getBookmarkedPages());
      if (user.getEmailAddress() != null) {
        toUpdate.setEmailAddress(user.getEmailAddress());
      }
      if (user.getFirstName() != null) {
        toUpdate.setFirstName(user.getFirstName());
      }
      if (user.getLastName() != null) {
        toUpdate.setLastName(user.getLastName());
      }
    }
    return null;
  }

  @Override
  public void deleteUser(URI baseURI, String userName) {
    if (testUserData == null) {
      setup();
    }
    User toDelete = null;
    for (var u : testUserData) {
      if (Objects.toString(u.getUserName(), "").equalsIgnoreCase(userName)) {
        toDelete = u;
        break;
      }
    }
    if (toDelete != null) {
      testUserData.remove(toDelete);
    } else {
      throw new UnknownUserException();
    }
  }

  @Override
  public List<String> findUsers(URI baseURI, String pattern) {
    return null; // TODO: Implement user search
  }

  private void setup() {
    var roles = new ArrayList<String>();
    roles.add("Editor");
    roles.add("Contributor");

    var a = new User();
    a.setUserName("a.user");
    a.setEmailAddress("a.email");
    a.setFirstName("a.first");
    a.setLastName("a.last");
    a.setRoles(roles);

    var aref = new LinkRef();
    aref.setName("a.userpage");
    aref.setHref("#");
    a.setPersonalPage(aref);

    var aPersonAsset = new LinkRef();
    var apAssets = new ArrayList<LinkRef>();
    apAssets.add(aPersonAsset);
    a.setPersonAssets(apAssets);

    var b = new User();
    b.setUserName("b.user");
    b.setEmailAddress("b.email");
    b.setFirstName("b.first");
    b.setLastName("b.last");
    b.setRoles(roles);

    var bref = new LinkRef();
    bref.setName("b.userpage");
    bref.setHref("#");
    b.setPersonalPage(bref);

    var bPersonAsset = new LinkRef();
    var bpAssets = new ArrayList<LinkRef>();
    bpAssets.add(bPersonAsset);
    b.setPersonAssets(bpAssets);

    this.testUserData = new ArrayList<>();
    this.testUserData.add(a);
    this.testUserData.add(b);
  }

  @Override
  public Status checkDirectoryStatus() {
    return null;
  }

  @Override
  public List<String> searchDirectory(String pattern) {
    return null;
  }
}

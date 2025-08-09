package com.percussion.security.shim.acl.impl;

import com.percussion.security.shim.acl.Group;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the Group interface for legacy compatibility.
 */
public class GroupImpl implements Group {
  private final String name;
  private final Set<Principal> members = new HashSet<>();

  public GroupImpl(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean addMember(Principal user) {
    return members.add(user);
  }

  @Override
  public boolean removeMember(Principal user) {
    return members.remove(user);
  }

  @Override
  public boolean isMember(Principal member) {
    return members.contains(member);
  }

  @Override
  public Enumeration<? extends Principal> members() {
    return Collections.enumeration(members);
  }

  @Override
  public String toString() {
    return "GroupImpl{" + "name='" + name + '\'' + ", members=" + members + '}';
  }
}

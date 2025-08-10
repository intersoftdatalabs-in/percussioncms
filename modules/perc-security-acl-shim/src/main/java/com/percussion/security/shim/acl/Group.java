package com.percussion.security.shim.acl;

import java.security.Principal;
import java.util.Enumeration;

/** Shim for javax.security.acl.Group. Used for legacy compatibility. */
public interface Group extends Principal {
  boolean addMember(Principal user);

  boolean removeMember(Principal user);

  boolean isMember(Principal member);

  Enumeration<? extends Principal> members();
}

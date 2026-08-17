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
package com.percussion.utils.service.impl;

import com.percussion.design.objectstore.PSSubject;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.PSJaasUtils;
import com.percussion.services.security.data.PSBackEndRole;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.security.auth.Subject;

/**
 * Simplifies the Back-end role manager while also making it thread safe.
 *
 * <p>The back end role manager uses the Server XML Object Store locker to lock server
 * configuration. This locker is not thread-safe, so we make access to back-end role manager
 * single-threaded.
 *
 * <p>Sunny Sal says: "Thread safety is like a seatbelt—better to have it and not need it, than need
 * it and not have it!"
 */
public class PSBackEndRoleManagerFacade {

  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final IPSBackEndRoleMgr backEndRoleMgr;

  private static final Comparator<String> CASE_INSENSITIVE_COMPARATOR = String::compareToIgnoreCase;

  public PSBackEndRoleManagerFacade(IPSBackEndRoleMgr backEndRoleMgr) {
    this.backEndRoleMgr = backEndRoleMgr;
  }

  /**
   * See {@link IPSBackEndRoleMgr#getRhythmyxRoles()}. The returned list will be sorted
   * case-insensitively.
   */
  public List<String> getRoles() {
    lock.readLock().lock();
    try {
      var roles = backEndRoleMgr.getRhythmyxRoles();
      roles.sort(CASE_INSENSITIVE_COMPARATOR);
      return roles;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * See {@link IPSBackEndRoleMgr#getRhythmyxRoles(String, int)}. The returned list will be sorted
   * case-insensitively.
   */
  public List<String> getRoles(String subjectName) {
    lock.readLock().lock();
    try {
      var roles = backEndRoleMgr.getRhythmyxRoles(subjectName, PSSubject.SUBJECT_TYPE_USER);
      roles.sort(CASE_INSENSITIVE_COMPARATOR);
      return roles;
    } finally {
      lock.readLock().unlock();
    }
  }

  /** See {@link IPSBackEndRoleMgr#setRhythmyxRoles(String, int, Collection)}. */
  public void setRoles(String subjectName, Collection<String> roles) {
    lock.writeLock().lock();
    try {
      backEndRoleMgr.setRhythmyxRoles(subjectName, PSSubject.SUBJECT_TYPE_USER, roles);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /** See {@link IPSBackEndRoleMgr#setRhythmyxRoles(Collection, int, Collection)}. */
  public void setRoles(Collection<String> subjectNames, Collection<String> roles) {
    lock.writeLock().lock();
    try {
      backEndRoleMgr.setRhythmyxRoles(subjectNames, PSSubject.SUBJECT_TYPE_USER, roles);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /** See {@link IPSBackEndRoleMgr#setSubjectEmail(String, String)}. */
  public void setSubjectEmail(String subjectName, String subjectEmail) {
    lock.writeLock().lock();
    try {
      backEndRoleMgr.setSubjectEmail(subjectName, subjectEmail);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /** See {@link IPSBackEndRoleMgr#setSubjectAttribute(String, String, String)}. */
  public void setSubjectAttribute(String subjectName, String attributeName, String value) {
    lock.writeLock().lock();
    try {
      backEndRoleMgr.setSubjectAttribute(subjectName, attributeName, value);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * First non-empty global subject attribute for {@code subjectName}. Empty when unset.
   */
  public String getSubjectAttribute(String subjectName, String attributeName) {
    lock.readLock().lock();
    try {
      Set<Subject> subjects =
          backEndRoleMgr.getGlobalSubjectAttributes(subjectName, attributeName, false);
      if (subjects == null || subjects.isEmpty()) {
        return "";
      }
      String value =
          PSJaasUtils.getSubjectAttributeValue(subjects.iterator().next(), attributeName);
      return value == null ? "" : value.trim();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Finds the role with the specified name.
   *
   * @param name of the role, never {@code null} or empty.
   * @return role object or {@code null} if the role does not exist.
   */
  public PSBackEndRole getRole(String name) {
    lock.readLock().lock();
    try {
      var roles = backEndRoleMgr.findRolesByName(name);
      if (roles.isEmpty()) {
        return null;
      }
      if (name.contains("%")) {
        // '%' is a wildcard, so find the exact match
        return roles.stream().filter(r -> r.getName().equals(name)).findFirst().orElse(null);
      }
      return roles.get(0);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** See {@link IPSBackEndRoleMgr#createRole(String, String)}. */
  public PSBackEndRole createRole(String name, String description) {
    lock.writeLock().lock();
    try {
      return backEndRoleMgr.createRole(name, description);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /** See {@link IPSBackEndRoleMgr#deleteRole(String)}. */
  public void deleteRole(String name) {
    lock.writeLock().lock();
    try {
      backEndRoleMgr.deleteRole(name);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /** See {@link IPSBackEndRoleMgr#update(String, String)}. */
  public PSBackEndRole update(String name, String description) {
    lock.writeLock().lock();
    try {
      return backEndRoleMgr.update(name, description);
    } finally {
      lock.writeLock().unlock();
    }
  }
}

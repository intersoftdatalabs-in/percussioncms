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
package com.percussion.security.shim.acl;

import java.security.Principal;
import javax.security.auth.Subject;
import java.util.Enumeration;

/**
 * Minimal compatibility interface mirroring java.security.acl.Acl used by legacy code.
 * Semantics: deny overrides allow where applicable; owners control modifications.
 */
public interface Acl extends Owner {

    String getName();

    void setName(Principal caller, String name) throws NotOwnerException;

    boolean addEntry(Principal caller, AclEntry entry) throws NotOwnerException;

    boolean removeEntry(Principal caller, AclEntry entry) throws NotOwnerException;

    Enumeration<AclEntry> entries();

    boolean checkPermission(Subject subject, Permission permission);
}
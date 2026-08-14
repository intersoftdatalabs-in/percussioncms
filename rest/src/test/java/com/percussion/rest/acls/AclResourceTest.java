/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
package com.percussion.rest.acls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.Guid;
import com.percussion.rest.Status;
import com.percussion.security.IPSTypedPrincipal;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class AclResourceTest {

  private IAclAdaptor adaptor;
  private AclResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IAclAdaptor.class);
    resource = new AclResource();
    try {
      var f = AclResource.class.getDeclaredField("adaptor");
      f.setAccessible(true);
      f.set(resource, adaptor);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void saveAclsDelegatesAndReturnsOk() throws Exception {
    AclList list = new AclList();
    Acl acl = new Acl();
    acl.setName("By_Author ACL");
    list.add(acl);

    Status status = resource.saveAcls(list);
    assertEquals(200, status.getStatusCode());
    verify(adaptor).saveAcls(eq(list));
  }

  @Test
  public void saveAclsWrapsAdaptorFailure() throws Exception {
    AclList list = new AclList();
    doThrow(new RuntimeException("denied")).when(adaptor).saveAcls(any());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.saveAcls(list));
    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  public void loadAclForObjectDelegates() {
    Guid guid = new Guid();
    guid.setStringValue("0-31-5");
    Acl acl = new Acl();
    acl.setName("By_Author ACL");
    when(adaptor.loadAclForObject(any(Guid.class))).thenReturn(acl);

    Acl out = resource.loadAclForObject("0-31-5");
    assertSame(acl, out);
    verify(adaptor).loadAclForObject(any(Guid.class));
  }

  @Test
  public void createAclDelegates() {
    CreateAclRequest request = new CreateAclRequest();
    Guid guid = new Guid();
    guid.setStringValue("0-31-5");
    request.setObjectGuid(guid);
    request.setOwner(new TypedPrincipal("Admin", IPSTypedPrincipal.PrincipalTypes.USER));
    Acl created = new Acl();
    created.setName("new");
    when(adaptor.createAcl(any(Guid.class), any(TypedPrincipal.class))).thenReturn(created);

    assertSame(created, resource.createAcl(request));
    verify(adaptor).createAcl(eq(guid), eq(request.getOwner()));
  }
}

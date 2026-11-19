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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.rest.test.apibridge;

import com.percussion.rest.auth.IAuthProfileAdaptor;
import com.percussion.rest.auth.ProfileEmailActionResult;
import com.percussion.rest.auth.ProfileEmailChangeRequest;
import com.percussion.rest.auth.ProfileEmailStatus;
import org.springframework.stereotype.Component;

/** Spring test stub for {@link IAuthProfileAdaptor} (MainTest / shared rest context). */
@Component
public class TestAuthProfileAdaptor implements IAuthProfileAdaptor {

  @Override
  public ProfileEmailStatus getMyEmailStatus() {
    ProfileEmailStatus status = new ProfileEmailStatus();
    status.setUserName("Admin");
    status.setEmail("admin@example.com");
    status.setEmailVerified(true);
    status.setEmailEditable(true);
    status.setEmailRequired(true);
    status.setProviderType("INTERNAL");
    return status;
  }

  @Override
  public ProfileEmailActionResult requestMyEmailChange(ProfileEmailChangeRequest request) {
    String email = request != null ? request.getEmail() : "";
    return new ProfileEmailActionResult(
        true, false, "admin@example.com", email, true, "Verification sent");
  }

  @Override
  public ProfileEmailActionResult resendMyEmailVerification() {
    return new ProfileEmailActionResult(
        true, false, "admin@example.com", "pending@example.com", false, "Verification sent");
  }
}

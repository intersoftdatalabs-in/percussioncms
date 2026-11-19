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

import com.percussion.rest.auth.AdminRecoveryRequest;
import com.percussion.rest.auth.AdminRecoveryResponse;
import com.percussion.rest.auth.AuthAccountReadinessSummary;
import com.percussion.rest.auth.AuthMailTestRequest;
import com.percussion.rest.auth.AuthMailTestResult;
import com.percussion.rest.auth.AuthReadinessReport;
import com.percussion.rest.auth.IAuthReadinessAdaptor;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/** Spring test stub for {@link IAuthReadinessAdaptor} (MainTest / shared rest context). */
@Component
public class TestAuthReadinessAdaptor implements IAuthReadinessAdaptor {

  @Override
  public AuthReadinessReport getReadinessReport() {
    AuthReadinessReport report = new AuthReadinessReport();
    report.setGeneratedAt(Instant.now());
    report.setMfaMode("optional");
    report.setPasswordMinLength(12);
    report.setMailConfigured(false);
    report.setMailSource("UNCONFIGURED");
    report.setOverallStatus("WARN");
    report.setInfraChecks(List.of());
    report.setAccountSummary(new AuthAccountReadinessSummary());
    report.setAccountIssues(List.of());
    return report;
  }

  @Override
  public AuthMailTestResult sendTestEmail(AuthMailTestRequest request) {
    String recipient = request != null ? request.getRecipient() : "";
    return new AuthMailTestResult(true, recipient, "TEST_STUB", "Test stub OK");
  }

  @Override
  public AdminRecoveryResponse initiateAdminRecovery(AdminRecoveryRequest request) {
    String target = request != null ? request.getTargetUserName() : "";
    boolean immediate = request != null && request.isResetImmediately();
    return new AdminRecoveryResponse(
        true,
        target,
        immediate ? "IMMEDIATE_MFA_RESET" : "RECOVERY_LINK",
        immediate ? "DIRECT" : "EMAIL",
        "",
        "Test stub OK");
  }
}

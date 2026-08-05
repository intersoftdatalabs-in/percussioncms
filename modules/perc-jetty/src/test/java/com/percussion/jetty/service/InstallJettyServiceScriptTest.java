/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.jetty.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * GH-962: installer script contracts (systemd preferred, init.d fallback, no dual-register).
 *
 * <p>Structural tests that verify the shipped {@code install-jetty-service.sh} honors the
 * systemd-first contract: when systemd is available the installer registers a native unit and skips
 * SysV / chkconfig registration; the {@code --initd} flag forces the legacy path. The uninstall
 * path tracks which registration mechanism was used so cleanup is symmetric with install. Tests are
 * read-only on the script source; they do not actually install or uninstall services.
 */
class InstallJettyServiceScriptTest {

  private static final Path INSTALL_SCRIPT =
      Path.of("src", "main", "jetty", "service", "install-jetty-service.sh");

  private static String script;

  @BeforeAll
  static void load() throws Exception {
    assertTrue(
        Files.isRegularFile(INSTALL_SCRIPT), () -> "missing " + INSTALL_SCRIPT.toAbsolutePath());
    script = Files.readString(INSTALL_SCRIPT, StandardCharsets.UTF_8);
  }

  @Test
  void script_detectsSystemdAndInstallsUnit() {
    assertTrue(script.contains("is_systemd_available"), "systemd detection helper");
    assertTrue(script.contains("/run/systemd/system"), "systemd runtime dir check");
    assertTrue(script.contains("installSystemdUnit"), "native unit install");
    assertTrue(script.contains("percussion-cms.service.in"), "unit template name");
    assertTrue(script.contains("substitute_unit_template"), "safe placeholder substitution");
    assertTrue(script.contains("validate_service_name"), "service name validation");
    assertTrue(script.contains("systemctl enable"), "enable unit");
    assertTrue(script.contains("systemctl daemon-reload"), "daemon-reload");
  }

  @Test
  void script_uninstallTracksHadSystemdAndInitd() {
    assertTrue(script.contains("had_systemd=true") || script.contains("had_systemd=false"));
    assertTrue(script.contains("had_initd"));
    assertTrue(script.contains("had_systemd") && script.contains("had_initd"));
  }

  @Test
  void script_skipsSysVEnableOnSystemdPath() {
    // On systemd path we must not call enableSysV / chkconfig on
    assertTrue(script.contains("enableSysV"), "SysV helper exists for fallback");
    assertTrue(script.contains("use_systemd_install"), "selection helper");
    // Ensure systemd branch message documents skipping SysV boot registration
    assertTrue(
        script.contains("SysV boot registration skipped")
            || script.contains("SysV boot registration"),
        "document no dual-register");
  }

  @Test
  void script_supportsInitdForceAndUninstallSystemd() {
    assertTrue(script.contains("--initd"), "force init.d flag");
    assertTrue(script.contains("--systemd"), "force systemd flag");
    assertTrue(script.contains("removeSystemdUnit"), "uninstall systemd unit");
    assertTrue(
        script.contains("disable --now") || script.contains("systemctl disable"), "disable unit");
  }

  @Test
  void script_requiresRoot_noDryRunFlag() {
    // GH-1977: install is root-only; no product --dry-run (docs cover offline review)
    assertTrue(script.contains("id -u"), "root check via id -u");
    assertTrue(
        script.contains("must be run with sudo or as root")
            || script.contains("must be run as root"),
        "root error message");
    assertFalse(script.contains("--dry-run"), "no --dry-run installer flag");
  }

  @Test
  void defaultsFile_doesNotEmbedShellCommands() {
    // Historical bug: mkdir/chown were written into /etc/default and broke EnvironmentFile
    assertFalse(
        script.contains("cat <<-EOF > /etc/default")
            && script.contains("mkdir -p ${JETTY_RUN}\n    chown"),
        "defaults heredoc must not include mkdir/chown shell lines");
    assertTrue(
        script.contains("cat > \"/etc/default/${SERVICE_NAME}\"")
            || script.contains("cat > \"/etc/default/"),
        "writes defaults file");
  }
}

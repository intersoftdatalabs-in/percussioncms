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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.ant.install;

import com.percussion.install.PSGeneratedPasswords;
import com.percussion.install.PSGeneratedPasswords.GeneratedEntry;
import com.percussion.install.PSLogger;
import com.percussion.security.error.PSExceptionUtils;
import java.nio.file.Path;
import org.apache.tools.ant.BuildException;

/**
 * ANT task that persists a CMS repository database password to
 * {@code <installRoot>/var/config/generated/passwords} under the {@link
 * PSGeneratedPasswords#KEY_CMDB cmdb} key.
 *
 * <p>The task has two modes:
 *
 * <ul>
 *   <li>{@code random="true"} (default): generate a cryptographically random password. Used by
 *       silent / unattended installs where there is no operator to prompt.
 *   <li>{@code random="false"}: store an explicit value supplied via {@code value}. Reserved for
 *       system-supplied credentials (CI / automation / upstream secret manager). Operator-typed
 *       secrets must NOT flow through this task — they belong in
 *       {@code rxconfig/Installer/rxrepository.properties} only.
 * </ul>
 *
 * <p>The task exposes the resulting password via ANT properties so downstream targets (notably
 * the H2 branch of {@code installRepository.xml}) can reference it without re-reading the file:
 *
 * <pre>{@code
 *   <PSGenerateRepositoryPassword rootDir="${install.dir}"/>
 *   <property name="cmdb.password" value="${cmdb.password}"/>
 * }</pre>
 *
 * <p>Example Usage:
 *
 * <pre>{@code
 *   <taskdef name="generateRepositoryPassword"
 *            class="com.percussion.ant.PSGenerateRepositoryPassword"
 *            classpathref="INSTALL.CLASSPATH"/>
 *
 *   <generateRepositoryPassword rootDir="${install.dir}" random="true"/>
 * }</pre>
 */
public class PSGenerateRepositoryPassword extends PSAction {

  /** Creates a new task instance. */
  public PSGenerateRepositoryPassword() {}

  /**
   * Instance-level install root attribute setter. ANT auto-binds this when {@code rootDir=} is
   * declared on the task. Per-instance state avoids leaking the inherited {@link
   * PSAction#ms_rootDir} across tasks running in the same JVM.
   *
   * @param rootDir absolute path to the CMS install root, never {@code null}.
   */
  public void setRootDir(String rootDir) {
    super.setRootDir(rootDir);
    m_rootDir = rootDir;
  }

  /**
   * Whether to generate a random password. Default is {@code true}.
   *
   * @param random {@code true} to generate a random password; {@code false} to use {@link
   *     #setValue(String)}.
   */
  public void setRandom(boolean random) {
    m_random = random;
  }

  /**
   * Explicit password value, used only when {@code random="false"}. Must not be blank when set.
   *
   * @param value password supplied by the interactive installer, never {@code null}.
   */
  public void setValue(String value) {
    m_value = value;
  }

  @Override
  public void execute() {
    if (m_rootDir == null || m_rootDir.trim().isEmpty()) {
      throw new BuildException("rootDir must be set");
    }
    try {
      Path installRoot = java.nio.file.Paths.get(m_rootDir.trim());
      GeneratedEntry entry;
      if (m_random) {
        entry = PSGeneratedPasswords.generateAndStoreCmdb(installRoot);
      } else {
        if (m_value == null) {
          throw new BuildException("value must be set when random=\"false\"");
        }
        Path file =
            PSGeneratedPasswords.write(
                installRoot, PSGeneratedPasswords.KEY_CMDB, m_value);
        entry = new GeneratedEntry(m_value, file);
      }
      getProject().setProperty(CMDB_PASSWORD_PROPERTY, entry.password());
      PSLogger.logInfo("Persisted CMS repository password to " + entry.file().toString());
    } catch (Exception e) {
      throw new BuildException(
          "Failed to persist CMS repository password: " + PSExceptionUtils.getMessageForLog(e), e);
    }
  }

  /** ANT property name that exposes the persisted password to subsequent targets. */
  public static final String CMDB_PASSWORD_PROPERTY = "cmdb.password";

  private boolean m_random = true;
  private String m_value;
  private String m_rootDir;
}
